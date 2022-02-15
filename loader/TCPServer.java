package com.enals.loader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

import com.enals.loader.LanguagePack.modelName;
import com.enals.loader.LanguagePack.systemStatus;

public class TCPServer extends Thread{
	private final long maskL = 5278580800269405267L;//the end of the data mask
	private final int maskH = 827212108;//the mask for the header
	private boolean stopped = false;//The Thread is running or stopped
	private boolean isReceivingRunning = false;
	private boolean isSendingRunning = false;
	private boolean isLogon = false;
	private final int outPort,inPort;
	
	private int sendingHeaderCount = 0;
	
	private ioBuffer ioHeader = null;	

	private ioBuffer ioIn = null;
	private Charset cset = Charset.forName("UTF-8");
	private BlockingQueue<ioBuffer> outQueue = new ArrayBlockingQueue<ioBuffer>(8);//
	private BlockingQueue<ioBuffer> ioQueue = new ArrayBlockingQueue<ioBuffer>(4);
	private Map<Integer,ioBuffer> waitingQueue = new HashMap<Integer,ioBuffer>();//
	private Map<String,Map<String,String>> pci;
	private String[] ipList;
	private String macaddress = "";
	private dataDeal dD;
	private JProgressBar noticeBar = null;

	private byte[] hback = new byte[64];
	
	private LanguagePack Lpack = null;
	private byte[] databB;
	private Loader loader;
	private paramLib pb;
	
	public TCPServer (int outp,int inp,String[] serverl,Map<String,Map<String,String>> pci,Loader ld) {
		this.outPort = outp;
		this.inPort = inp;		
		this.ipList = serverl;	
		this.pci = pci;
		this.loader = ld;
	}
	
	protected BlockingQueue<ioBuffer> getOutQueue(BlockingQueue<ioBuffer> inq,JProgressBar jp){
		if (this.isLogon) return null;
		this.noticeBar = jp;
		return this.outQueue;
		
	}
	
	public void setlanguagePack(LanguagePack lp) {
		this.Lpack = lp;
	}
	
	private boolean initIoBuffer() {
		try {
			databB = new byte[(1 << 20) * (pb.getIO_Body_Buffer_Length() + 1)];
			int t2 = 0;
			
			if (ioHeader == null) {
				ioHeader = new ioBuffer(t2,(1 << pb.getIO_Header_Buffer_Length()),databB,true,null);//16KB
				ioHeader.clearHeader(0, 64);
				ioHeader.getHeader().putLong(pb.getServerID());
				ioHeader.getHeader().putLong(pb.getHostID());				
				t2 += (1 << pb.getIO_Header_Buffer_Length());
			}
			
			ioBuffer next = ioHeader;
			ioBuffer iob = ioHeader;
			ioQueue.offer(ioHeader);
			
			for (int i = 0;i < 3;i++){
				next = new ioBuffer(t2,(1 << pb.getIO_Header_Buffer_Length()),databB,false,null);//16KB
				next.clearHeader(0, 64);
				next.getHeader().putLong(pb.getServerID());		
				next.getHeader().putLong(pb.getHostID());
				iob.setNext(next);
				iob = next;
				ioQueue.offer(next);
				t2 += (1 << pb.getIO_Header_Buffer_Length());
			}
			return true;
		} catch (Exception e) {//if pconn is null			
			return false;
			
		}
	}
	
	protected void clear() {
		this.dD = null;
		ioHeader = null;
		ioIn = null;
		cset = null;
		outQueue = null;//
		waitingQueue = null;//
		pci = null;
		ipList = null;
		macaddress = "";
		noticeBar = null;
		hback = null;
		Lpack = null;
		databB = null;
		loader = null;
	}
	
	public void stopIT() {
		this.stopped = true;
	}
	
	public boolean isStopped() {
		return this.isReceivingRunning && this.isSendingRunning;
	}
	
	/*
	 * init the scout channel
	 */
	protected boolean initScoutChannel(ioBuffer io) {
		try{
			String s =((InetSocketAddress) (pb.get_scIn_Channel().socket().getRemoteSocketAddress())).getAddress().getHostAddress();
			pb.set_scOut_Channel(SocketChannel.open());
			
			if (!pb.get_scOut_Channel().connect(new InetSocketAddress(s,outPort)))
			{
				JOptionPane.showMessageDialog(null, "Could not connect to the server now");
				System.exit(0);
			}
			io.setHeaderPosition(0);
			while (io.getHeader().hasRemaining()) {
				try {
					pb.get_scOut_Channel().write(io.getHeader());
				}catch(Exception er) {}
			}			
			return true;
		}catch(Exception e){			
			return false;
		}
	}
	
	public void run() {
		try{
			isReceivingRunning = true;
			byte[] hb = new byte[64];
			ByteBuffer header = ByteBuffer.wrap(hb);
			
			for (int i = 0;i < 64;i++)
				hback[i] = 0;
			ByteBuffer.wrap(hback).putLong(0, pb.getServerID());
			ByteBuffer.wrap(hback).putLong(8, pb.getHostID());
			while (!this.stopped){
				try{
					if (pb.get_scIn_Selector().select(1000)==0) {//will be blocked for a long time
						continue;
		            }				
	            // Get an iterator over the set of selected keys  
	            Iterator<SelectionKey> itCtrl=pb.get_scIn_Selector().selectedKeys().iterator();
	            // Look at each key in the selected set	            
	            while (itCtrl.hasNext() && !this.stopped) {
	            	SelectionKey sKey = (SelectionKey) itCtrl.next(); 	            	
	        			//Is a new connection coming in?  
		            try {  
			            if (sKey.isReadable())//this is reading from server					            {		
			           	{
			            	/*
			            	 * 32 byte used as the header of each data package								 
							 * 0 - 7,is the local host ID
							 * 8 - 15 is the serverID or the destination ID 
							 * 16 - 19 is the header index
							 * 22 - 21 is the command
							 * 20 is the fun
							 * 23 is the errorcode
							 * 24      is the data type 0 - no data,1 - the data is in the header,2 the data is in the body
							 * 25      is the security level 0 - 3
							 * 26 - 27 is the offset of the security key
							 * 28 - 31 is the data len 
							 * 32 - 63 bytes are reserverd
			            	 */			            		
							try{								
								header.position(0);
								while (header.hasRemaining() && !this.stopped){
									if (pb.get_scIn_Channel().read(header) == -1) {										
										throw new Exception("disconnected");
									}//this maybe throw a exceptino if the connection is closed											
								}
								
								switch (header.get(22)) {
								case (byte) 10://this is the command from server to check the client status
									ioIn = getIO(ioIn,this);
									header.putLong(24, 0L);
									System.arraycopy(hb, 0, ioIn.getHeaderByteBuffer(), ioIn.getHeaderPosition(0),64);									
									ioIn.clearHeader(0, 64);
									outQueue.offer(ioIn);
									ioIn = null;
									break;
									default:
										try {
											ioIn = waitingQueue.get(header.getInt(16));		
											System.out.println("comm:" + ioIn.getHeaderByte(22) + " and fun:" + ioIn.getHeaderShort(20));
											if (ioIn == null) break;//we must be waiting the iobuffer to get the data
											System.arraycopy(hb, 0, ioIn.getHeaderByteBuffer(), ioIn.getHeaderPosition(0), 64);											
											switch (ioIn.getHeaderByte(24)) {
											case (byte) 0://no body data,and no more data need to read									
												break;
											case (byte) 1://more data need to read in the getHeader(),ioIn.getHeader().getInt(ioIn.getHeaderPostion() + 28) is the bytes need to read.((1 << iobufferlen) - 64)						
												ioIn.clearHeader(64, 64 + ioIn.getHeaderInt(28));											
												while (ioIn.getHeader().hasRemaining() && !this.stopped){
													if (pb.get_scIn_Channel().read(ioIn.getHeader()) == -1) {													
														throw new Exception("disconnected");
													}//this maybe throw a exceptino if the connection is closed											
												}//this maybe throw a exceptino if the connection is closed																				
												break;
											case (byte) 2:
												ioIn.clearBody(0, ioIn.getHeaderInt(28));	
												while (ioIn.getBody().hasRemaining() && !this.stopped){
													if (pb.get_scIn_Channel().read(ioIn.getBody()) == -1) {													
														throw new Exception("disconnected");
													}//this maybe throw a exceptino if the connection is closed											
												}
												break;
												default://reading the body data	
													throw new Exception("readerror");										
											}
											loader.addIO(ioIn);
										}catch(Exception er) {
											er.printStackTrace();
										}
										break;
								}							
								
							}catch(Exception e) {
								JOptionPane.showMessageDialog(null, systemStatus.SERVERDISCONNECTED);
								this.stopped = true;
							}finally {								
							}
		            	}
		            }catch(Exception e){
		            	
		            	}
		            itCtrl.remove();
		            }
				}catch(Exception e){
					
				}
			}
		}catch(Exception e){
						
		}finally{
			isReceivingRunning = false;
		}
	}
	/*
	 * this is the data sending out to server thread
	 */
	private final class dataDeal extends Thread{//all the send & receive 
		public void run(){
			try{
				ioBuffer io;
				isSendingRunning = true;
				while (!stopped){
					try{	
						
						io = outQueue.poll(1000, TimeUnit.MILLISECONDS);
						if (io == null) continue;
						try {
							if (stopped) return;
							io.setHeaderPosition(0);						
							while (io.getHeader().hasRemaining() && !stopped) {
								pb.get_scOut_Channel().write(io.getHeader());									
							}
							
							switch (io.getHeaderByte(22)) {
							case (byte) 10://the server check								
								break;
								default:
									waitingQueue.put(io.getHeaderInt(16), io);//16 - 19 are the sending count									
									break;
							}														
						}catch(Exception e) {															
						}finally {
							if (!ioQueue.contains(io))
								ioQueue.offer(io);
						}
					}catch(Exception er) {
						
					}
				}
			}catch(Exception e){						
			}finally{
				isSendingRunning = false;
			}
		}
	}
	
	protected void setIsLogon(boolean isl) {
		this.isLogon = isl;
	}
	
	public byte[] connect(int osType,long ickdate,byte[] iakey,SocketChannel si,paramLib pblib) {
		try{	
			if (isLogon) return null;
			this.pb = pblib;
			byte[] bb = randomCode(768);	
			try{
				//System.out.println("begin:" + System.currentTimeMillis());
				String lsip = "";
				try {
					Enumeration<NetworkInterface> netl = NetworkInterface.getNetworkInterfaces();
		    		while (netl.hasMoreElements())//if the server's ip address is in the same segment with the current computer's internal ip address
		    		{
		    			NetworkInterface n1 = netl.nextElement();	    			
		    			if (n1!=null){	    				
		    				Enumeration<InetAddress> al = n1.getInetAddresses();
		    				while (al.hasMoreElements()){
		    					InetAddress a1 = al.nextElement();	
		    					byte[] ab = a1.getAddress();
		    					for (String s:ipList){
		    						byte[] lab = InetAddress.getByName(s).getAddress();//判断服务器地址是否和本机地址在同一个地址段，如果是将优先连接该服务器地址
		    						if (ab[0] == lab[0] && ab[1] == lab[1] && ab[2] == lab[2])
		    						{
		    							lsip = s;
		    						}
		    					}
		    				}
		    			}			    			
		    		}
				}catch(Exception er) {
					lsip = "";
				}
								
				ByteBuffer rp = ByteBuffer.wrap(new byte[32]);
				if (si == null) {					
					for (String s: ipList){
						try{						
							pb.set_scIn_Channel(SocketChannel.open());	
							if (!pb.get_scIn_Channel().connect(new InetSocketAddress(lsip == "" ? s : lsip,inPort)))
							{
								pb.set_scIn_Channel(null);
							}else
							{
								lsip = s;
								long ct = System.currentTimeMillis();
								rp.limit(4);
								rp.position(0);
								while (rp.hasRemaining()) {
									if (pb.get_scIn_Channel().read(rp) == -1) {
										pb.set_scIn_Channel(null);									
									}
									if (System.currentTimeMillis() - ct > 1000 * 5) {//5 seconds waiting
										pb.set_scIn_Channel(null);		
									}
								}
								if (rp.getInt(0) == 827212108) {//如果连接成功，并验证成功
									rp.clear();
									rp.putLong(5278580800269405267L);
									rp.clear();
									pb.get_scIn_Channel().write(rp);
									if (rp.hasRemaining()) {
										pb.get_scIn_Channel().write(rp);
										if (rp.hasRemaining()) {
											pb.set_scIn_Channel(null);
										}
									}									
								}
								break;
							}
						}catch(Exception e){																	
						}
					}
				}else if (si.isConnected()) {
					pb.set_scIn_Channel(si);
				}else {
					for (String s: ipList){
						try{	
							pb.set_scIn_Channel(SocketChannel.open());
							if (!pb.get_scIn_Channel().connect(new InetSocketAddress(lsip == "" ? s : lsip,inPort)))
							{
								pb.set_scIn_Channel(null);
							}else
							{
								lsip = s;
								long ct = System.currentTimeMillis();
								rp.limit(4);
								rp.position(0);
								while (rp.hasRemaining()) {
									if (pb.get_scIn_Channel().read(rp) == -1) {
										pb.set_scIn_Channel(null);										
									}
									if (System.currentTimeMillis() - ct > 1000 * 5) {//5 seconds waiting
										pb.set_scIn_Channel(null);	
									}
								}
								if (rp.getInt(0) == 827212108) {//如果连接成功，并验证成功
									rp.clear();
									rp.putLong(5278580800269405267L);
									rp.clear();
									pb.get_scIn_Channel().write(rp);
									if (rp.hasRemaining()) {
										pb.get_scIn_Channel().write(rp);
										if (rp.hasRemaining()) {
											pb.set_scIn_Channel(null);	
										}
									}									
								}
								break;
							}
						}catch(Exception e){																	
						}
					}
				}
			}catch(Exception e){								
				
			}							
					
			if (pb.get_scIn_Channel() == null) {
				JOptionPane.showMessageDialog(null, systemStatus.SERVERDISCONNECTED);
				System.exit(0);
			}
			
			if (!pb.get_scIn_Channel().isConnected()) {
				JOptionPane.showMessageDialog(null, systemStatus.SERVERDISCONNECTED);
				System.exit(0);
			}
			
			
			pb.get_scIn_Channel().configureBlocking(false);
			
			try {
				try{
					long maclong = 0L;
		    		Enumeration<NetworkInterface> netl = NetworkInterface.getNetworkInterfaces();
		    		String s1 = "";
		    		ByteBuffer r1 = ByteBuffer.wrap(bb);
					r1.putLong(128 * 3, 0L);//zero 					
					int index = 1;
		    		Stopl:
		    		while (netl.hasMoreElements())//get the hardware address
		    		{
		    			NetworkInterface n1 = netl.nextElement();
		    			if (n1!=null){						    				
		    				Enumeration<InetAddress> al = n1.getInetAddresses();
		    				String str1 = "0123456789ABCDEF";			    				
		    				while (al.hasMoreElements()){
		    					InetAddress a1 = al.nextElement();	
		    					//System.out.println(a1.toString());						    					
	    						byte[] mac1 = n1.getHardwareAddress();	
	    						s1 = "";
	    						int n = 0;
	    						if (mac1!=null){
	    							for (byte b:mac1){
	    								if (b < 0) {
	    									n = b & 0x0FF;
	    								}else
	    									n = b;
	    								if (s1.equals(""))
	    									s1 = s1 + str1.substring(n / 16, n / 16 + 1) + str1.substring(n % 16,n % 16 + 1);
	    								else
	    									s1 = s1 + ":" + str1.substring(n / 16, n / 16 + 1) + str1.substring(n % 16,n % 16 + 1);
	    							}	    							
	    							s1 = s1.replace(":", "");
	    							r1.putLong(128 * 3, Long.parseLong(s1, 16)); 
	    							pci.put("connectedNIC" + index, new HashMap<String,String>());
	    							pci.get("connectedNIC" + index++).put("MACADDRESS", s1);
	    							//System.arraycopy(mac1, 0, bb, 128 * 3 + 8 - mac1.length, mac1.length);		    							
	    						}	
	    						if (a1.getHostAddress().equals(((SocketChannel) pb.get_scIn_Channel()).socket().getLocalAddress().getHostAddress()))
	    							break Stopl;
		    				}
		    			}			    			
		    		}	
					this.macaddress = s1;
					maclong = r1.getLong(128 * 3);									
		    		byte[] ma = s1.getBytes();
		    		System.arraycopy(ma, 0, bb, 576, ma.length);//ip address
		    		bb[575] = (byte) ma.length;//the ip's lenght
					byte[] ipa = ((byte[])(((InetSocketAddress)((SocketChannel) pb.get_scIn_Channel()).getLocalAddress()).getAddress().getAddress())).clone();//the ip address
		    		System.arraycopy(ipa, 0, bb, 480, ipa.length);//ip address
		    		bb[479] = (byte) ipa.length;//the ip's lenght
		    		bb[478] = (byte) osType;					
					
		    		System.arraycopy(iakey, 0, bb, 0, 128);
		    		
					r1.putLong(752, maclong);
					r1.putLong(760, ickdate);
					
					int t1 = 0;
					long timec = 0;
					//send the key data to server
					for (int i = 0;i < 6;i++) {
						r1.limit(t1 + 128);//128 bytes each time
						r1.position(t1);						
						pb.get_scIn_Channel().write(r1);//128 bytes will be write to the channel
						if (r1.hasRemaining()) {
							pb.get_scIn_Channel().write(r1);//128 bytes will be write to the channel
							if (r1.hasRemaining()) {
								JOptionPane.showMessageDialog(null, systemStatus.SENDDATAERRORTOSERVER);
								System.exit(0);
							}
						}
						r1.limit(8);
						r1.position(0);
						if (pb.get_scIn_Channel().read(r1) == -1) {
							JOptionPane.showMessageDialog(null, systemStatus.SERVERDATAERROR);
							System.exit(0);
						}
						timec = System.currentTimeMillis();
						while (r1.hasRemaining()) {
							if (pb.get_scIn_Channel().read(r1) == -1) 
							{
								if (r1.hasRemaining()) {
									JOptionPane.showMessageDialog(null, systemStatus.SERVERDATAERROR);
									System.exit(0);										
								}
							}
							if (System.currentTimeMillis() - timec > 1000 * 60)
								if (r1.hasRemaining()) {
									JOptionPane.showMessageDialog(null, systemStatus.SERVERDATAERROR);
									System.exit(0);										
								}
						}
						t1 += 128;
					}
					
					r1.limit(8);
					r1.position(0);
					
					pb.get_scIn_Channel().write(r1);//128 bytes will be write to the channel
					if (r1.hasRemaining()) {
						pb.get_scIn_Channel().write(r1);//128 bytes will be write to the channel
						if (r1.hasRemaining()) {
							JOptionPane.showMessageDialog(null, systemStatus.SENDDATAERRORTOSERVER);
							System.exit(0);
						}
					}
					
					r1.limit(64);
					r1.position(0);
					
					while (r1.hasRemaining() && !this.stopped) 
					{
						if (pb.get_scIn_Channel().read(r1) == -1) {
							JOptionPane.showMessageDialog(null, "reading Data error from the server,now will quit");
							System.exit(0);
						}
						try {
							Thread.sleep(10);
						}catch(Exception er) {}						
					}	
					
					pb.set_scOut_Channel(pb.get_scIn_Channel());
					dD = new dataDeal();
	    			dD.start();						
					initSecKey(bb,0);
	    			initIoBuffer();	    			
		    	}catch(Exception e){
		    		e.printStackTrace();
		    		JOptionPane.showMessageDialog(null, "Error when Connect to the server,now will quit");
					return null;
		    	}								
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				JOptionPane.showMessageDialog(null, "Error when Connect to the server,now will quit");
				System.exit(0);
			}		
			try {
				pb.set_scIn_Selector(Selector.open());
			} catch (IOException e) {
				// TODO Auto-generated catch block			
			}
			pb.get_scIn_Channel().register(pb.get_scIn_Selector(),SelectionKey.OP_READ, this);
			//System.out.println("done:" + System.currentTimeMillis());
			return bb;
		}catch(Exception er){
			return null;
		}
	}
	/*
	 * 32 byte used as the header of each data package
	 * 0 - 7,is the local host ID
	 * 8 - 15 is the serverID or the destination ID 
	 * 16 - 19 is the header index
	 * 20 - 21 is the command
	 * 22 is the fun
	 * 23 is the errorcode
	 * 24      is the data type 0 - no data,1 - the data is in the header,2 the data is in the body
	 * 25      is the security level 0 - 3
	 * 26 - 27 is the offset of the security key
	 * 28 - 31 is the data len 
	 * 32 - 63 bytes are reserverd
	 */
	public void sendingIO(ioBuffer io,int command,int function,int errorcode,boolean hasdata) {
		try {
			if (this.isLogon) return;
			io.putHeaderShort(errorcode, (short) 0);
			io.putHeaderShort(20, (short) function);
			io.putHeaderByte(22, (byte) command);//function 0 - 127
			io.putHeaderByte(23, (byte) errorcode);			
			if (hasdata) {
				switch (io.getHeaderByte(24)) {//0 no more data,1 data is in the header after byte 64,2 data is in the body
				case (byte) 0://no more data
					io.putHeaderByte(24, (byte) 0);	
					io.setHeaderPosition(0);
					io.setHeaderLimit(64);
					io.setBodyLimit(0);
					break;
				case (byte) 1://data is in the header after 64 byte	
					switch (io.getHeaderByte(25)) {//encryption level
					case (byte) 0:
						break;
					case (byte)1:
					case (byte)2:
						try {
							if (stringEncode(io.getHeaderByteBuffer(),io.getHeaderByteBuffer(),io.getHeaderPosition( 64),io.getHeaderPosition( 64 + io.getHeaderInt(28)),io.getHeaderPosition( 64),io.getHeaderLimit(),(int) io.getHeaderByte(25)) == -1) throw new Exception("encryption error");
							io.putHeaderShort(26, (short) (io.getHeaderPosition(64) & 0x1ff));//key offset
							io.setHeaderPosition(0);
							io.setHeaderLimit(64 + io.getHeaderInt(28));
						}catch(Exception er) {}
						break;
					case (byte) 3:
						try {
							System.arraycopy(io.getHeaderByteBuffer(), io.getHeaderPosition( 64), io.getHeaderByteBuffer(), io.getHeaderPosition(pb.getIO_Header_3_1_Position()), io.getHeaderInt(28));
							if ((command = stringEncode(io.getHeaderByteBuffer(),io.getHeaderByteBuffer(),io.getHeaderPosition(pb.getIO_Header_3_1_Position()),io.getHeaderPosition(pb.getIO_Header_3_1_Position() + io.getHeaderInt(28)),io.getHeaderPosition( 64),io.getHeaderLimit(),3)) == -1) throw new Exception("encryption error");
							io.putHeaderShort(26, (short) (io.getHeaderPosition(pb.getIO_Header_3_1_Position()) & 0x1ff));//key offset
							io.putHeaderInt(28, command);//the data len	
							io.setHeaderPosition(0);
							io.setHeaderLimit(64 + command);
						}catch(Exception er) {}
						break;
					}
					break;
				case (byte) 2://data is in the body
					switch (io.getHeaderByte(25)) {
					case (byte) 0:
						break;
					case (byte)1:
					case (byte)2:
						try {
							if (stringEncode(io.getBodyByteBuffer(),io.getBodyByteBuffer(),io.getBodyPosition(0),io.getHeaderInt(28) + io.getBodyPosition(0),io.getBodyPosition(0),io.getBodyLimit(),(int) io.getHeaderByte(25)) == -1) throw new Exception("encryption error");
							io.putHeaderShort(26, (short) ((io.getBodyPosition(0)) & 0x1ff));//key offset
							io.setBodyPosition(0);
							io.setBodyLimit(io.getHeaderInt(28));
							io.setHeaderPosition(0);
							io.setHeaderLimit(64);						
						}catch(Exception er) {}
						break;
					case (byte) 3:
						try {
							
						}catch(Exception er) {}
						break;
					}
					break;
					default://error
						
						break;
				}
			}else {
				io.putHeaderLong(24, 0);
				io.setHeaderPosition(0);
				io.setHeaderLimit(64);
			}
			
			command = 0;
			while (!outQueue.offer(io) && !this.stopped)
			{
				try {
					Thread.sleep(10);
				}catch(Exception er) {}
				if (++command > 100) {
					try {
						noticeBar.setString("Could not add IO to the sending queue in sendingIO()");
						return;
					}catch(Exception er) {}
				}
			}
		}catch(Exception er) {}
	}
	
	public Charset getCharSet() {
		return this.cset;
	}	
	/*
	 * get the macaddress
	 */
	public String getMACAddress() {
		if (this.isLogon) return null;
		return this.macaddress;
	}
	//初始化密钥
	public void initSecKey(byte[] bkey,int index){
		try {
			if (this.isLogon) return;
			byte[] secKey1 = new byte[512];
			byte[] secKey2 = new byte[64];
			byte[] secKey3 = new byte[32];
			switch (index) {
			case 0:
				System.arraycopy(bkey, 128, secKey1, 0, 512);
				System.arraycopy(bkey, 512 + 128, secKey2, 0, 64);
				System.arraycopy(bkey, 576 + 128, secKey3, 0, 32);
				
				pb.setSecKey(secKey1, 1);
				pb.setSecKey(secKey2, 2);
				pb.setSecKey(secKey3, 3);
				pb.setCharset(Charset.forName(new String(bkey,0,bkey[62])));
				pb.setBufferSize(ByteBuffer.wrap(bkey).getInt(48));
				pb.setIO_Body_Buffer_Length(ByteBuffer.wrap(bkey).getShort(60));
				pb.setIO_Header_3_1_Position(ByteBuffer.wrap(bkey).getInt(40));
				pb.setIO_Body_3_1_Position(ByteBuffer.wrap(bkey).getInt(44));
				pb.setServerID(ByteBuffer.wrap(bkey).getInt(52));				
				break;
			case 1:				
				System.arraycopy(bkey, 0, secKey1, 0, 512);
				pb.setSecKey(secKey1, 1);
				break;
			case 2:				
				System.arraycopy(bkey, 0, secKey2, 0, 64);
				pb.setSecKey(secKey2, 2);
				break;
			case 3:				
				System.arraycopy(bkey, 576 + 128, secKey3, 0, 32);
				pb.setSecKey(secKey3, 3);
				break;
			}				
		} catch (Exception e) {		
			// TODO Auto-generated catch block				
		}
	}	
	/*
	 * get a IO
	 */
	public synchronized ioBuffer getIO(ioBuffer io,Object parent) {
		try {			
			if (io != null) {
				return io;
			}			
			return io = ioQueue.poll();
		}catch(Exception er) {
			return null;
		}
		finally {
			if (io != null) {
				io.clearHeader(0, 64);
				io.clearBody(0, 0);
				io.getHeader().put(hback);				
				io.putHeaderInt(16, this.sendingHeaderCount++);
			}
		}		
	}
	//对从服务器接收到的ioBuffer进行解密
	public int decodeIO(ioBuffer io) {
		if (this.isLogon) return -1;
		switch (io.getHeaderByte(24)) {//0 no more data,1 in the header,2 in the body
		case (byte) 1://the data is in the header
			try {
				switch (io.getHeaderByte(25)) {//encryption level
				case (byte) 0:
					return 0;
				case (byte) 1://the data is in the header					
				case (byte) 2://the data is in the body
					try {
						return stringDecode(io.getHeaderShort( 26), io.getHeaderByteBuffer(), io.getHeaderByteBuffer(), io.getHeaderPosition(64), io.getHeaderPosition(64 + io.getHeaderInt(28)), io.getHeaderPosition(64), io.getHeaderLimit(), io.getHeaderByte(25));
					}catch(Exception er) {}
					break;
				case (byte) 3://the data is in the body
					try {
						int len = stringDecode(io.getHeaderShort( 26), io.getHeaderByteBuffer(), io.getHeaderByteBuffer(), io.getHeaderPosition( 64), io.getHeaderPosition( 64 + io.getHeaderInt(28)), io.getHeaderPosition(pb.getIO_Header_3_1_Position()), io.getHeaderLimit(), io.getHeaderByte(25));
						System.arraycopy(io.getHeaderByteBuffer(), io.getHeaderPosition(pb.getIO_Header_3_1_Position()), io.getHeaderByteBuffer(), io.getHeaderPosition( 64), len);
						return len;						
					}catch(Exception er) {}
					break;
				}
			}catch(Exception er) {}
			break;
		case (byte) 2://the data is in the body
			try {
				switch (io.getHeaderByte(25)) {//encryption level
				case (byte) 0:
					return 0;
				case (byte) 1://the data is in the header					
				case (byte) 2://the data is in the body
					try {
						return stringDecode(io.getHeaderShort( 26), io.getBodyByteBuffer(), io.getBodyByteBuffer(), io.getBodyPosition(0), io.getBodyPosition(0) + io.getHeaderInt(28), io.getBodyPosition(0), io.getBodyLimit(), io.getHeaderByte(25));
					}catch(Exception er) {}
					break;
				case (byte) 3://the data is in the body
					try {
						int len = stringDecode(io.getHeaderShort( 26), io.getBodyByteBuffer(), io.getBodyByteBuffer(), io.getBodyPosition(0), io.getBodyPosition(0) + io.getHeaderInt(28), io.getBodyPosition(0) + pb.getIO_Body_3_1_Position(), io.getBodyLimit(), io.getHeaderByte(25));
						System.arraycopy(io.getBodyByteBuffer(), io.getBodyPosition(0) + pb.getIO_Body_3_1_Position(), io.getBodyByteBuffer(), io.getBodyPosition(0), len);
						return len;
					}catch(Exception er) {}
					break;
				}
			}catch(Exception er) {}
			break;
		}
		return -1;		
	}
	/*
	 * 数据加密
	 * if level less than 3,then bstr must be use bodyOutB
	 * if level equal 3 then bstr must not be bodyOutB
	 */
	private int stringEncode(byte[] sourceByte,byte[] destinationByte,int sp,int ep,int dpostion,int dlimit,int level){//security the information,value of sLayer(true for 2 times,false for 1 tiems)
		try{				
			switch (level) {
			case 1:
				for (;sp < ep;sp++){//sp is not a constant var,different sp will have different result
					destinationByte[sp] ^= pb.getSecKey(1)[(sp) & 0x1FF]; // % 512						
				}
				return 0;					
			case 2:
				for (;sp < ep;sp++){//sp is not a constant var,different sp will have different result
					destinationByte[sp] ^= pb.getSecKey(1)[(sp) & 0x1FF]; // % 512
					destinationByte[sp] ^= pb.getSecKey(2)[(sp) & 0x3F];//64
				}
				return 0;					
			case 3:
				level =  dpostion;
				for (;sp < ep;sp++){//sp is not a constant var,different sp will have different result
					sourceByte[sp] ^= pb.getSecKey(1)[(sp) & 0x1FF]; // % 512
					sourceByte[sp] ^= pb.getSecKey(2)[(sp) & 0x3F];
					dpostion += (((pb.getSecKey(3)[(sp) & 0x1F]) & 0x1) > 0 ? 1 : -((pb.getSecKey(3)[(sp) & 0x1F]) & 0x1));
					destinationByte[dpostion++] = (byte)(sourceByte[sp] ^ pb.getSecKey(3)[sp & 0x1F]);
				}					
				return dpostion - level;					
			}
			return -1;
		}catch(Exception e){			
		}
		return -1;
	}
	/*	 
	 * 对数据解密
	 */
	private int stringDecode(int beg,byte[] sourceByte,byte[] destinationByte,int sp,int ep,int dpostion,int dlimit,int level){//security the information,value of sLayer(true for 2 times,false for 1 tiems)
		try{
			switch (level) {
			case 1:					
				for (;sp < ep;sp++){//XOR 
					destinationByte[sp] ^= pb.getSecKey(1)[(beg++) & 0x1FF];
				}
				return 0;
			case 2:
				for (;sp < ep;sp++){//XOR 
					destinationByte[sp] ^= pb.getSecKey(2)[(beg) & 0x3F];
					destinationByte[sp] ^= pb.getSecKey(1)[(beg++) & 0x1FF];
				}
				return 0;
			case 3:
				level = dpostion;
				for (;sp < ep;sp++){//XOR 
					sp += (((pb.getSecKey(3)[(beg) & 0x1F]) & 0x1) > 0 ? 1 : -((pb.getSecKey(3)[(beg) & 0x1F]) & 0x1));
					destinationByte[dpostion] =(byte)(sourceByte[sp] ^ pb.getSecKey(3)[beg & 0x1F]);
					destinationByte[dpostion] ^= pb.getSecKey(2)[(beg) & 0x3F];
					destinationByte[dpostion++] ^= pb.getSecKey(1)[(beg++) & 0x1FF];
				}
				return dpostion - level;
			}
			return -1;				
		}catch(Exception e){			
			return -1;
		}			
	}
	//随机字节数
	protected byte[] randomCode(int codeLen){
		byte[] codetemp= new byte[codeLen];
		Random rand= new Random();
		rand.nextBytes(codetemp);
		int i=0;
		while (i<codeLen){
			if (codetemp[i]==34)//delete " from the string
				codetemp[i]=(byte)rand.nextInt(33);
			i++;
		}
		return codetemp;
	}
}

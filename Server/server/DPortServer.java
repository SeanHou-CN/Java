package com.enals.server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.Iterator;
import javax.swing.JOptionPane;


public class DPortServer implements Runnable{
	private final int OUT_PORT_NUMBER = 8189;
	private final int IN_PORT_NUMBER = 8181;	
	
	private int serverID = 1;
	private final byte EOT = 0x04;//EOT byte,flag	
	
	private static String fileRoot = File.separator + "var" + File.separator;
	
	protected static Charset charSet = Charset.forName("UTF-8");
	private ServerStatus myServerStatus = ServerStatus.isNew;
	private ServerStatus ServerRunningStatus = ServerStatus.isRunning;	
	private boolean isRunning = true;
	
	private static DPortServer cPort8189=null;
	
	private TCPPort tcpPort = new TCPPort(OUT_PORT_NUMBER,IN_PORT_NUMBER);
	private ServerIOSource myServerSource;
	
	protected enum ServerStatus{//server status
		isStopped(0),//server or client thread is stopped
		isTest(1),//testing mode
		isNew(2),//new server,the first user will be the super user	
		isAddingNewClosed(4),//new user is not allowed	
		isRunning(8);//is running
		
		private int index;
		private  ServerStatus(int index) {
			this.index = index;
		}
		
		protected boolean getRunningStatus() {
			return this.index != 0;
					
		}
	}
	
	protected enum OSType{//operating system type
		WINDOWS,
		APPLE,
		LINUX;
	}
	protected enum SKeyOptioin{//used to security the data
		PRODUCTID,
		DIGITALPRODUCTID,
		IDENTIFYINGNUMBER,
		PROCESSORID,
		SERIALNUMBER,
		MACADDRESS,
		PARTNUMBER,
		PRODUCTNAME;
	}
	protected enum SKeySList{//security key search list,search the first one,then the second one
		OS,
		CPU,
		MEMORYCHIP,
		BASEBOARD,
		NIC,
		DISKDRIVE,
		SMBIOS;
	}
	protected enum clientStatus{//client status
		isDISCONNECTED(0),
		isTHREADSTOPPED(1),
		isGETTINGDATA(2),
		isWAITINGINIT(3),
		isINTERACTIONCHECK(4),
		isCHECKINGUSERLOGIN(5),
		isCHECKINGLOGINDATE(6),
		isLOGIN(7),		
		isDEALINGERROR(8),
		isSENDINGORGINFO(9),
		isSENDINGDECODEKEY(10),
		isREADYFORINCHANNEL(11),
		isSLEEPING(13);
		
		private int index;
		private  clientStatus(int index) {
			this.index = index;
		}
		
		protected boolean getRunningStatus() {
			return this.index > 1;
					
		}
	}
	
	public enum iostatus{//io status
		isReading,
		isDealing,
		isSending,
		isIdel, ReadingDone,
	}
	
	public static void main(String[] args){
    	try{//Load mysql driver
    		System.setProperty("jdbc.drivers", "com.mysql.jdbc.Driver"); //load the jdbc driver
    		new Thread(cPort8189 = new DPortServer()).start();
    	}catch(SecurityException se){        		
    		JOptionPane.showMessageDialog(null,"Server can not load SQL Driver!!!");
    	}catch(Exception dd) {
    		dd.printStackTrace();
    	}    	
	}
	
	public void run(){ 	
		try{			
			myServerSource = new ServerIOSource(serverID,
					20,//private int clientInstance = 20;//max client instance
					14,//private int clientIOBufferSize = 14;//16K bytes used for the ioBuffer
					16,//private int clientMem = 16;//16MB Memory used by each client instance		
					1024,//private int Max_fileHandler = 1024;//1024 files will be accessible at the same time
					"jdbc:mysql://localhost/SEnals?useUnicode=true&anp;characterEncoding=utf8mb4",//private String DBurl = "jdbc:mysql://localhost/SEnals?useUnicode=true&anp;characterEncoding=utf8mb4"; //MySQL
					"logonly",
					"Passw0rd",	
					cPort8189,
					tcpPort.InitTCPPort(IN_PORT_NUMBER),
					1000 * 60 * 30);//lock the user 30 minuters	
			
			myServerSource.initIOBuffer();//init the ioBuffer
         myServerSource.setCharSet("UTF-8");
            
         try{
        	 	Files.deleteIfExists(new File(System.getProperty("user.dir") + File.separator + "ServerRunning.inf").toPath());
				Files.createFile(new File(System.getProperty("user.dir") + File.separator + "ServerRunning.inf").toPath());
				Files.write(new File(System.getProperty("user.dir") + File.separator + "ServerRunning.inf").toPath(), writeObjectTobyte(new Date()), StandardOpenOption.WRITE);							
	    	}catch(Exception dd) {
	    		dd.printStackTrace();
	    	}
            
	        SocketChannel channel=null;           
            SelectionKey sKey; 
            Selector OUT_Port_Selector = tcpPort.InitTCPPort(OUT_PORT_NUMBER);
            ByteBuffer rp = ByteBuffer.wrap(new byte[36]);
            rp.putInt(827212108);      
            ClientInstanceBody ci;            
	        while (this.getServerRunningStatus()) {
	        	try{
                	//Do something here,Like server clean
	        		
	        	}catch(Exception e){
            	   
	        	}
	        	try{	  
	        		//waiting for connection
		            if ((OUT_Port_Selector.select(60 * 1000))==0) {//this will be blocked until there are at least one key is selected or timeout
		        			
		            	continue;
			            }
		            
		            try{
			            //Get an iterator over the set of selected keys  
			            Iterator<SelectionKey> itCtrl=OUT_Port_Selector.selectedKeys().iterator();
			            //Look at each key in the selected set
			            while (this.getServerRunningStatus() && itCtrl.hasNext()) {  			            	
		            		sKey = (SelectionKey) itCtrl.next(); 	            
		            		//Is a new connection coming in?  
				            try {  
					            if (sKey.isAcceptable()) {
					            	try{					            		
					                	channel= ((ServerSocketChannel) sKey.channel()).accept(); 
					                	channel.configureBlocking(false);
					                	rp.limit(4);
					                	rp.position(0);
					                	channel.write(rp);		
					                	channel.register(OUT_Port_Selector, SelectionKey.OP_READ);
					               	}catch(Exception e){					            	   
						            	try{((SocketChannel) sKey.channel()).close();}catch(Exception er){}
						            	continue;
					               	}		              
					            }else if (sKey.isReadable())
				            		{	
					            	channel = (SocketChannel) sKey.channel();
					            	ci = (ClientInstanceBody) sKey.attachment();
					            	if (ci == null)
					            		{
					            		rp.limit(36);
					            		rp.position(4);
					            		if (channel.read(rp) == -1) throw new Exception("Attacking");
					            		if (rp.hasRemaining()) throw new Exception("Attacking");
					            		if ((rp.getLong(4) ^ 5278580800269405267L) == 0)
					            			ci = myServerSource.getClientInstanceBody(channel,sKey);
					            		if (ci == null) throw new Exception("Attacking");
					            		
					            		sKey.attach(ci);					            		
					            		}					            	
					            	//if reading done
					            	if (ci.readekey(channel)) {
					            		if (ci.getClientStatus() == clientStatus.isDISCONNECTED)
					            			{
					            			if (!myServerSource.offerDisconnectedQueue(ci))
					            				throw new Exception("Attacking");
					            			continue;
					            			}					            		
					            	}else {
					            		try{
						            		switch (ci.getClientStatus()) {
						            		case isGETTINGDATA:						            			
						            			if (ci.setByteBufferpl(0, 8, 0)) {
						            				if (!ci.sendingData(channel)) 
						            					{
															if (!myServerSource.offerDisconnectedQueue(ci))
									            				throw new Exception("Thread stopped or Disconnected");
															continue;
														}							            			
						            				}
						            			ci.setByteBufferpl(ci.getTsp() + 128, ci.getTsp() + 256, 128);
						            			if (ci.getTsp() == 768) {
						            				ci.setStatus(clientStatus.isWAITINGINIT);
						            				ci.setByteBufferpl(0, 8, 0);
						            				}
						            			break;
						            		case isWAITINGINIT:
						            			ci.getICByteBuffere().clear();
						            			if (!myServerSource.interactionCheck(ci)) {//if it attacking then add it to the queue
							            			if (!myServerSource.offerAttackingQueue(ci,channel,sKey))
							            				throw new Exception("Attacking");
							            			continue;
							            			}
						            			if (!myServerSource.sendSITClient(ci, channel))
						            				{
														if (!myServerSource.offerDisconnectedQueue(ci))
								            				throw new Exception("Thread stopped or Disconnected");
														continue;
						            				}
						            			
													if (!ci.checkInteractionStatus())
													{
														if (!myServerSource.offerAttackingQueue(ci,channel,sKey))
								            				throw new Exception("Attacking");
														continue;
													}
													ci.setStatus(clientStatus.isSENDINGDECODEKEY);	
													ci.setByteBufferpl(0, 64, 0);
													break;
							            	case isSENDINGDECODEKEY:
							            		if (!ci.dealingDecodeKey()) {
							            			ci.sendingData(channel);
							            			if (!myServerSource.offerDisconnectedQueue(ci))
							            				throw new Exception("Thread stopped or Disconnected");
														continue;
							            		}else	if (!ci.sendingData(channel))
							            			{
							            			if (!ci.sendingData(channel)) {
							            				if (!myServerSource.offerDisconnectedQueue(ci))
								            				throw new Exception("Thread stopped or Disconnected");
															continue;
							            				}
							            			}
							            		ci.setStatus(clientStatus.isSENDINGORGINFO);	
							            		ci.setByteBufferpl(0, 64, 0);
							            		break;
												case isSENDINGORGINFO:
													ci.dealingOrgInfo();
													if (!ci.sendingData(channel))
							            			{
							            			if (!ci.sendingData(channel)) {
							            				if (!myServerSource.offerDisconnectedQueue(ci))
								            				throw new Exception("Thread stopped or Disconnected");
															continue;
							            				}
							            			}
													ci.setByteBufferpl(0, 64, 0);
													break;
												case isCHECKINGUSERLOGIN:
													if (!ci.checkingUserLogin(channel)) {
														if (!myServerSource.offerDisconnectedQueue(ci))
								            				throw new Exception("Thread stopped or Disconnected");
														continue;
													}
													myServerSource.removeFromDealingQueue(ci);													
													break;
												default:
													if (!myServerSource.offerDisconnectedQueue(ci))
							            				throw new Exception("Thread stopped or Disconnected");
													break;
						            		
						            			}
					            		}catch(Exception er){
					            			this.printError(er);
					            			}
					            		finally {
					            			try{
					            				if (!ci.getICByteBuffere().hasRemaining())
					            					ci.setByteBufferpl(0, 64, 0);
					            			}catch(Exception er){}	
					            			}					            		
					            		}
				            		}
				            }catch(Exception e) {
				            	try{sKey.attach(null);}catch(Exception er){}				            	
				            	try{((SocketChannel) sKey.channel()).close();}catch(Exception er){}
				            	try{ci = null;}catch(Exception er){}
				            }finally{
				            	try{itCtrl.remove();}catch(Exception er){}
				            }
				        }       
		        	}catch(Exception r){
		        		this.printError(r);    	
		        	}
	        	}catch(Exception er){
	        		
	        	}	        	
	        }
        }catch (Exception e){
        	this.printError(e);    	
        	try{
        		ServerRunningStatus = ServerStatus.isStopped;
        		System.exit(0);
        	}catch(Exception er){}        	        	
        }finally {
        	try{
				Files.deleteIfExists(new File(System.getProperty("user.dir") + File.separator + "ServerRunning.inf").toPath());
    		}catch(Exception dd) {
	    		dd.printStackTrace();
	    	}
        }
    }
	//get server status
	protected ServerStatus getServerStatus() {
		return this.myServerStatus;
	}	
	
	protected boolean getServerRunningStatus() {
		return this.isRunning;
	}	
	
	protected void setServerStatus(ServerStatus ss) {
		this.myServerStatus = ss;
	}
	
	
	public static byte[] writeObjectTobyte(Object o) {
		try {
			ByteArrayOutputStream ba = new ByteArrayOutputStream();
			ObjectOutputStream outp = new ObjectOutputStream(ba);
			outp.writeObject(o);
			return ba.toByteArray();
		}catch(Exception er) {
			return null;}
	}
	
	protected void printError(Exception er) {
		if (myServerStatus == ServerStatus.isTest)
			try {
				System.out.println("isTest mode is on--------");
				ByteArrayOutputStream ba = new ByteArrayOutputStream();
				PrintStream ps = new PrintStream(ba);
				er.printStackTrace(ps);
				int bi = 0;
				for (int i = 0;i < ba.toByteArray().length;i++) {
					if (ba.toByteArray()[i] == (byte) 13 || ba.toByteArray()[i] == (byte) 10) {
						if (new String(ba.toByteArray(),bi,i - bi).toUpperCase().indexOf("DPORTSERVER.JAVA") > 0) {
							System.out.println(new String(ba.toByteArray(),bi,i - bi));
							return;
						}
						
						if (bi == 0) {
							System.out.println(new String(ba.toByteArray(),bi,i - bi));
						}						
						
						bi = i;
					}
				}
			}catch(Exception T) {}
	}
}

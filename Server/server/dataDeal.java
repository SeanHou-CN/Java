package com.enals.server;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.enals.server.DPortServer.clientStatus;

final class dataDeal extends Thread{
	private boolean isStoppedIT = false;
	private boolean isRunning = false;
	private ServerIOSource myServerSource;
	private Selector IN_Port_Selector;
	protected dataDeal(ServerIOSource myServerSource,Selector IN_Port_Selector) {
		this.myServerSource = myServerSource;
		this.IN_Port_Selector = IN_Port_Selector;
	}
	
	protected boolean StopIt() {
		this.isStoppedIT = true;
		return this.isRunning;
	}
	
	@Override
	public void run(){
		try{	        
    		try{
    			this.isRunning = true;
    			this.isStoppedIT = false;
    			SocketChannel channel=null;           
              SelectionKey sKey; 
		        ClientInstanceBody cr = null;
		        ByteBuffer iob = ByteBuffer.wrap(new byte[64]);			        
		        while (!this.isStoppedIT && myServerSource.getServerRunningStatus()){			        	
		        	try{
		        		if (IN_Port_Selector.select() == 0){}//this will block for a long time
			        	Iterator<SelectionKey> itC = IN_Port_Selector.selectedKeys().iterator();
			        	while (!this.isStoppedIT && itC.hasNext()){
			        		sKey = itC.next();
				        	try{
				        		if (sKey.isAcceptable()){
				        			try{
				        				cr = null;
					               channel= ((ServerSocketChannel) sKey.channel()).accept(); 
					               channel.configureBlocking(false);
					               channel.register(IN_Port_Selector, SelectionKey.OP_READ);
				        			}catch(Exception e){						            	   	
					                	try{channel.close();}catch(Exception ne){}					                
					                	itC.remove();// if now the channel is closed or ?
				            			continue;
				        			}				        			
				        		}else if (sKey.isReadable()){	
				        			if (((ClientInstanceBody) sKey.attachment()) == null) {
				        				channel = (SocketChannel) sKey.channel();
				        				iob.clear();
				        				if (channel.read(iob) == -1) throw new Exception("Disconnected");	
				        				if (iob.hasRemaining())	{
					                	if (channel.read(iob) == -1) throw new Exception("Disconnected");	
					                	}
					               if (iob.hasRemaining())	{
					                	throw new Exception("Disconnected");	
					                	}
					               if ((cr = this.myServerSource.getLiveClient(iob.getLong(0))) == null) throw new Exception("Attack");
					             
					               if (cr.getClientStatus(clientStatus.isREADYFORINCHANNEL) && cr.checkInPortConnection(iob.array()))					                	
			                			{
					            	   sKey.attach(cr);
					            	   cr.setStatus(clientStatus.isLOGIN);
					            	   cr.setInChannel(channel);
			                		}else
			                			throw new Exception("Disconnected");
				        			}else {
				        				((ClientInstanceBody) sKey.attachment()).addReadingIO();
				        				//System.out.println("datadeal:" + ((ClientInstanceBody) sKey.attachment()).toString());
				        			}			        				
				        		}
				        		itC.remove();// if now the channel is closed or ?
		            		continue;
				        	}catch(Exception er){				        		
				        		try{sKey.attach(null);}catch(Exception e){}				            	
				            try{((SocketChannel) sKey.channel()).close();}catch(Exception e){}
				            try{cr.setStatus(clientStatus.isDISCONNECTED);}catch(Exception e){}				        					        		
			               itC.remove();// if now the channel is closed or ?
				        	}				        	
			        	}
		        	}catch(Exception er){
		        		
		        	}
		        }			        
	        }catch(Exception e){
	        	e.printStackTrace();		        	
	        }
		}catch(Exception e){
			e.printStackTrace();
		}finally{	
			this.isRunning = false;
		}
	}
}

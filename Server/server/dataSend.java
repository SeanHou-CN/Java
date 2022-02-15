package com.enals.server;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.enals.server.DPortServer.iostatus;

final class dataSend extends Thread{
	private ServerIOSource myServerSource;
	private dataDeal datadeal;
	private final BlockingQueue<ClientInstanceBody> dw;		
	
	private boolean isStoppedIT = false;
	private boolean isRunning = false;
	
	public dataSend(BlockingQueue<ClientInstanceBody> d,BlockingQueue<ClientInstanceBody> d1,ServerIOSource myServerSource,dataDeal datadeal){
		this.dw = d;
		this.myServerSource = myServerSource;	
		this.datadeal = datadeal;
	}
	
	protected boolean StopIt() {
		this.isStoppedIT = true;
		return this.isRunning;
	}
	
	public void run(){
		try{
			ClientInstanceBody di = null;		
			ioBuffer io;
			this.isRunning = true;
			this.isStoppedIT = false;
			while (!this.isStoppedIT && myServerSource.getServerRunningStatus()){
				di = dw.poll(1000,TimeUnit.MILLISECONDS);
				//System.out.println("Send process wakeup and waiting:" + waitingTime);
				try{
					if (di == null)
					{						
						continue;
					}
					
					if (di.isThreadStopped() || di.getClientSource().getOutQueue().size() == 0)
						continue;
					
					try {
						io = di.getClientSource().getOutQueue().peek();
					}catch(Exception er) {
						continue;
					}
					
					try{
						if (io.isHeaderHasRemaining()){
							di.getOutChannel().write(io.getHeader());
							if (io.getHeader().hasRemaining()){
								di.getOutChannel().write(io.getHeader());
								if (io.getHeader().hasRemaining()){
									myServerSource.addSendingQueue(di,true,null);
									continue;
								}
							}									
						}	
						
						if (io.isBodyHasRemaining()) {
							di.getOutChannel().write(io.getBody());
							if (io.getBody().hasRemaining()) {
								di.getOutChannel().write(io.getBody());
								if (io.getBody().hasRemaining()) {
									myServerSource.addSendingQueue(di,true,null);
									continue;
								}
							}	
						}
						
						io.setIOStatus(iostatus.isIdel);
						if (di.addCommandFromSending(io))
							myServerSource.addSendingQueue(di,true,null);
					}catch(Exception er) {						
						io.setIOStatus(iostatus.isIdel);
						di.addCommandFromSending(io);
						continue;	
					}
				}catch(Exception e)
				{}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			this.isRunning = false;
		}
	}
}

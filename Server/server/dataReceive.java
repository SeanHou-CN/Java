package com.enals.server;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.enals.server.DPortServer.clientStatus;
import com.enals.server.DPortServer.iostatus;

final class dataReceive extends Thread{		
	private final BlockingQueue<ClientInstanceBody> dataWaitingQ,nextd;	
	private ServerIOSource myServerSource;
	Exception closedChannel = new Exception("-1 bytes is read");
	private dataDeal datadeal;
	
	private boolean isStoppedIT = false;
	private boolean isRunning = false;
	
	public dataReceive(BlockingQueue<ClientInstanceBody> d,BlockingQueue<ClientInstanceBody> n,ServerIOSource myServerSource,dataDeal datadeal){
		this.dataWaitingQ = d;
		this.nextd = n;
		this.myServerSource = myServerSource;
		this.datadeal = datadeal;
	}
	
	protected boolean StopIt() {
		this.isStoppedIT = true;
		return this.isRunning;
	}
	
	public void run(){
		try{
			ClientInstanceBody di = null,rd = null;
			ioBuffer io;
			this.isRunning = true;
			this.isStoppedIT = false;
			while (!this.isStoppedIT && myServerSource.getServerRunningStatus()){
				try{			
					di = dataWaitingQ.poll(1000,TimeUnit.MILLISECONDS);
			
					if (di == null)
						continue;
					
					try {
						if (di.isThreadStopped() || (io = di.getReadIO()) == null)
							continue;
					}catch(Exception er) {
						continue;
					}	
					
					try{					
						if (io.isHeaderHasRemaining()) {
							if (di.readIOHeader(io) == -1) throw closedChannel;
							if (io.isHeaderHasRemaining()){
								if (di.readIOHeader(io) == -1) throw closedChannel;//read again
								if (io.isHeaderHasRemaining()){	//why comming here,64 bytes									
									continue;
								}									
							}													
						}
						
						if (io.isBodyHasRemaining()) {
							if (di.readIOBody(io) == -1) throw closedChannel;
							if (io.isBodyHasRemaining()) {
								if (di.readIOBody(io) == -1) throw closedChannel;
								if (io.isBodyHasRemaining()) {
									continue;
								}
							}
						}
						
						switch (io.getHeaderByte(24)) {	
						case 3://forward this command to the other client							
						case 4://forward the data to another client												
						case 5://forward the data to another client
							if ((rd = this.myServerSource.getLiveClient(io.getHeaderLong(0))) != null)
							{
								if (myServerSource.addSendingQueue(rd, false, io))
								{
									di.removeReadIO();
								}else
									myServerSource.addReadingQueue(di);
							}
							else {
								if (di.addCommandFromReading()) {
									myServerSource.addReadingQueue(di);
								}
							}
							break;
							default:
								if (di.addCommandFromReading()) {
									myServerSource.addReadingQueue(di);
								}						
						}						
					}catch(Exception er) {
						if (er.equals(closedChannel)) {							
							di.setStatus(clientStatus.isDISCONNECTED);
						}								
					}
				}catch(Exception e){
					e.printStackTrace();
				}finally{										
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally {
			this.isRunning = false;
		}
	}
}

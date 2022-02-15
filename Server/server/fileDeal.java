package com.enals.server;

import java.io.File;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/*
 * file handler
 */
final class fileDeal extends Thread{
	private BlockingQueue<fileHandler> waitingQueue = new ArrayBlockingQueue<fileHandler>(Max_fileHandler);
	private Map<Long,fileHandler> filequeue = new LinkedHashMap<Long,fileHandler>();//this is with a order
	private String root = fileRoot + "clients" + File.separator;
	private int qindex = 0;
			
	private void addWaitingQueue(long filename,fileHandler fh){
		try{
			if (this.waitingQueue.offer(fh)) return;
			filequeue.put(filename,fh);				
		}catch(Exception er){				
		}			
	}
	public void run(){
		try{
			try{
				Files.createDirectories(new File(root).toPath());
				Files.createDirectories(new File(root + File.separator + "files" + File.separator).toPath());
			}catch(Exception er){
				er.printStackTrace();
			}
			
			fileHandler fh = null;
			long hid = 0L;
			
			while (ServerRunningStatus.getRunningStatus()){
				try{
					qindex = this.waitingQueue.size();
					while ((fh = this.waitingQueue.poll(10000, TimeUnit.MILLISECONDS)) != null)
					{
						if (fh.ch != null){
							if (!fh.ch.isThreadStopped()){								
								if (fh.actionType)//writing data
								{
									if (fh.iow.size() > 0)
									{
										fh.writingData(2);
									}
								}else
								{										
									if (fh.ior.size() > 0)
									{
										fh.readingData(readingFileCount);
									}
								}	
							}								
						}
						if (qindex > 0) qindex--;
					}
				}catch(Exception er){						
				}
				
				try{
					Iterator<Long> its = filequeue.keySet().iterator();
					while (ServerRunningStatus.getRunningStatus() && its.hasNext()){
						hid = its.next();
						its.remove();
						fh = filequeue.get(hid);
						try{
							if (fh.ch != null){
								if (!fh.ch.isThreadStopped()){								
									if (fh.actionType)//writing data
									{
										if (fh.iow.size() > 0)
										{
											fh.writingData(2);
										}
									}else
									{										
										if (fh.ior.size() > 0)
										{
											fh.readingData(readingFileCount);
										}
									}	
								}								
							}							
						}catch(Exception er){}								
					}
				}catch(Exception wr){}
			}
		}catch(Exception e){
			
		}
	}
	
	protected boolean freeFileHandler(fileHandler fh){		
		try{			
			if (fh != null){	
				if (fh.actionType)
					fh.closeWritingFile();
				else
					fh.closeReadingFile();
				filequeue.remove(fh.index);				
				if (!myServerSource.getFileHandler().contains(fh))
					myServerSource.getFileHandler().put(fh);								
			}
			return true;
		}catch(Exception er){			
			return false;
		}
	}		
}

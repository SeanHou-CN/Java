package com.enals.server;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.enals.server.ClientInstanceBody;

final class fileHandler{
	private long crc32;
	private RandomAccessFile f1;
	private long timestamp;
	private long cpoint = 0L;//the current pointer
	private long limit = 0L;
	private long currentPosition = 0L;		
	private long fileIndex = 0;
	private long index = 0L;
	
	private int errorCode = 0;
	private int keyIndex = -1;
	private int type = -1;
	
	private String fp = "";
	
	private boolean actionType = true;//true for write,false for read

	private ClientInstanceBody ch;
	private BlockingQueue<ioBuffer> iow = new ArrayBlockingQueue<ioBuffer>(2);
	private BlockingQueue<ioBuffer> ior = new ArrayBlockingQueue<ioBuffer>(readingFileCount - 1);		
	private BlockingQueue<ClientInstanceBody> usingClient = new ArrayBlockingQueue<ClientInstanceBody>(readingFileCount);//we allow 4 users to read this file at the same time
	private ioBuffer io;

	private boolean addData(ioBuffer io)
	{	
		if (this.iow.offer(io))
		{
			this.errorCode = 0;
			fd.addWaitingQueue(index,this);
			return true;
		}
		return false;
	}
	
	private boolean getData(ioBuffer i){//all the file are stored encrypted,so we read 1 block eack time
		if (this.ior.offer(i))
		{
			this.errorCode = 0;
			fd.addWaitingQueue(index,this);
			return true;
		}
		else
		{
			return false;
		}						
	}		
	private void closeReadingFile() {
		try{			
			if (f1 == null) return;
			f1.getChannel().force(true);
			f1.close();	
			f1 = null;
		}catch(Exception er){
			er.printStackTrace();
		}
	}
	
	private void closeWritingFile() {
		try{
			if (f1 == null) return;
			try{
				f1.seek(this.limit - 20);
				f1.writeLong(this.currentPosition);
				f1.writeLong(cpoint);
		      f1.writeInt(maskH);					
			}catch(Exception er){
				er.printStackTrace();
			}
			try{					
				f1.getChannel().force(true);
				f1.close();	
				f1 = null;
			}catch(Exception er){
				er.printStackTrace();
			}				
		}catch(Exception er){
			er.printStackTrace();
		}
	}
	
	private boolean writingData(int i){			
		try{
			io = iow.poll();
			if (io == null) return false;
			
			switch (io.body.getShort(io.bodypostion + 16)){				
			case (short) 6:
				f1 = new RandomAccessFile(fp + this.fileIndex,"rw");
				f1.write(io.bodymaster.getByteBuffer(), io.bodypostion + 16, io.readThisTime - 16);//the head of the file
				this.errorCode = 9;//write done
				return true;			
			case (short) 1:
			case (short) 2:
				if (f1 == null) {						
					if (Files.exists(new File(fp + this.fileIndex).toPath())){
						f1 = new RandomAccessFile(fp + this.fileIndex,"rw");							
												
						f1.seek(this.limit - 20);
				      this.currentPosition = f1.readLong();//the position in the file
				      this.cpoint = f1.readLong();//the position + 16 in the file
				      if ((this.cpoint ^ (this.limit - 20)) == 0L)
				      	{
				    	  	this.errorCode = 8;//file is already uploading done
				    	  	break;
				      	}	
				      
				      if (this.currentPosition > this.cpoint || this.cpoint > this.limit - 20) {
				    	  try {
								f1.getChannel().force(true);
								f1.close();	
							}catch(Exception er) {}
							try {
								Files.deleteIfExists(new File(fp + this.fileIndex).toPath());
							}catch(Exception er) {}
							f1 = new RandomAccessFile(fp + this.fileIndex,"rw");
							f1.setLength(limit);
					      f1.seek(this.limit - 20);
					      f1.writeLong(0L);
					    	f1.writeLong(0L);
					      f1.writeInt(maskH);	
					      this.cpoint = this.currentPosition = 0L;
				       }
					}else {
						f1 = new RandomAccessFile(fp + this.fileIndex,"rw");
						f1.setLength(limit);
				      f1.seek(this.limit - 20);
				      f1.writeLong(0L);
				    	f1.writeLong(0L);
				      f1.writeInt(maskH);	
				      this.cpoint = this.currentPosition = 0L;
					}
				}
				
				if ((io.body.getLong(io.bodypostion + 24) ^ this.currentPosition) == 0L) {
					f1.seek(cpoint);
					f1.write(io.bodymaster.getByteBuffer(), io.bodypostion + 16, io.readThisTime - 16);
					cpoint = f1.getFilePointer();
					this.currentPosition += io.readThisTime - 32;
					if ((this.cpoint ^ (this.limit - 20)) == 0L)
					{
						f1.writeLong(this.currentPosition);
						f1.writeLong(cpoint);
						f1.writeInt(maskH);
						this.errorCode = 8;
						break;
					}else if (this.cpoint > this.limit - 20) {
						try {
							f1.getChannel().force(true);
							f1.close();	
						}catch(Exception er) {}
						try {
							Files.deleteIfExists(new File(fp + this.fileIndex).toPath());
						}catch(Exception er) {}
						f1 = new RandomAccessFile(fp + this.fileIndex,"rw");
						f1.setLength(limit);
				      f1.seek(this.limit - 20);
				      f1.writeLong(0L);
				    	f1.writeLong(0L);
				      f1.writeInt(maskH);	
				      this.cpoint = this.currentPosition = 0L;							
					}
					io.header.putLong(io.headerpostion + 40, currentPosition);
					this.errorCode = 1;
				}else {
					io.header.putLong(io.headerpostion + 40, currentPosition);
					this.errorCode = 7;
				}
				return false;
				default:
					this.errorCode = 15;
					return true;
			}
		}catch(Exception er){	
			this.errorCode = 15;	
			try {
				f1.getChannel().force(true);
				f1.close();	
			}catch(Exception er1) {}
			try {
				f1 = null;
				Files.deleteIfExists(new File(fp + this.fileIndex).toPath());
			}catch(Exception er1) {}
			er.printStackTrace();
		}finally{
			try{
				if (io != null){
					switch (errorCode){//if we write done the file,then
					case 1:
						io.header.putShort(io.headerpostion + 20, (short) 2);							
						this.ch.addCommandFromReading(io);
						break;	
					case 7:
						if (this.keyIndex == -1) {
							io.header.putShort(io.headerpostion + 20, (short) 2);							
							this.ch.addCommandFromReading(io);
						}else {
							io.header.putInt(io.headerpostion + 36, this.keyIndex);	
							io.header.putShort(io.headerpostion + 20, (short) 7);																
							this.ch.addCommandFromReading(io);
						}							
						break;
					case 8://file is already uploading done												
						io.header.putShort(io.headerpostion + 20, (short) 9);							
						this.ch.addCommandFromReading(io);
						break;
					case 9://this file is writing done
						io.header.putShort(io.headerpostion + 20, (short) 10);							
						this.ch.addCommandFromReading(io);							
						break;						
						default:
							io.header.putShort(io.headerpostion + 20, (short) 11);								
							try{
								Files.deleteIfExists(new File(fp + this.fileIndex).toPath());	
							}catch(Exception er){
								er.printStackTrace();
							}
							this.ch.addCommandFromReading(io);
							break;
					}
					io = null;
				}					
			}catch(Exception er)
			{					
			}					
		}	
		return true;
	}

	private void readingData(int i){	//only allow one thread read this file at the same time
		try{
			if (io == null)				
				io = ior.poll();//io should contain the information about the file
			if (io == null) {
				this.errorCode = 15;
				return;
			}
			
			if (f1 == null) {
				f1 = new RandomAccessFile(fp + this.fileIndex,"r");
			}
			
			this.cpoint = io.header.getLong(io.headerpostion + 40);
			
			if ((this.cpoint ^ (this.limit - 20)) == 0L) {
				this.errorCode = 3;
				return;
			}				
			
			f1.seek(this.cpoint);//this is the cpoint
			this.keyIndex = f1.readInt();
			this.errorCode = f1.readInt();//this is the data len in this package
			this.currentPosition= f1.readLong();
			if ((this.currentPosition ^ io.header.getLong(io.headerpostion + 48)) != 0) {//this is the currentposition
				this.errorCode = 15;
				return;
			}
			if (io.initBody(errorCode + 16))
			{
				io.body.limit(io.bodylimit);
				f1.read(io.bodymaster.getByteBuffer(), io.bodypostion + 16, errorCode);					
				io.body.putInt(io.bodypostion, keyIndex);
				io.body.putInt(io.bodypostion + 4, errorCode);
				io.body.putLong(io.bodypostion , currentPosition);
				io.body.limit(io.bodypostion + this.errorCode + 16);
				this.errorCode = 1;
			}else {
				this.errorCode = 2;
			}
		}catch(Exception er) {
			this.errorCode = 15;
			
		}finally {
			try{
				if (io != null){
					switch (errorCode){//if we write done the file,then
					case 1:
						io.header.putShort(io.headerpostion + 20, (short) 13);							
						this.ch.addCommandFromReading(io);
						break;	
					case 2:
						io.header.putShort(io.headerpostion + 20, (short) 14);
						this.ch.addCommandFromReading(io);
						break;
					case 3:
						io.header.putShort(io.headerpostion + 20, (short) 16);
						this.ch.addCommandFromReading(io);
						break;
						default:
							io.header.putShort(io.headerpostion + 20, (short) 15);														
							this.ch.addCommandFromReading(io);
					}
					io = null;
				}					
			}catch(Exception er)
			{					
			}
		}
	}
	
	private void init(int tp,String fpn,long i,ClientInstanceBody chr,long cr,long times,long fileindex,long len,boolean isWrite,int filekey){
		this.fp = fpn;
		this.keyIndex = filekey;
		this.type = tp;
		this.index = i;
		this.ch = chr;
		this.cpoint = 0L;
		this.limit = len;
		this.crc32 = cr;
		this.actionType = isWrite;
		this.errorCode = 0;
		this.fileIndex = fileindex;
		this.currentPosition = 0L;
		this.timestamp = times;
		this.ior.clear();
		this.iow.clear();
		
		try{
			if (f1 != null)
			{
				f1.getChannel().force(true);
				f1.close();
				f1 = null;
			}				
		}catch(Exception er){}	
	}
	
	private int init(int tp,String fpn,ClientInstanceBody chr,long fi,boolean isWriting,long fileindex){			
		try{
			this.fp = fpn;
			this.type = tp;
			this.ch = chr;
			this.fileIndex = fileindex;
			this.index= fi;
			this.actionType = isWriting;
			this.errorCode = 0;
			this.currentPosition = 0L;
			if (Files.exists(new File(fp + this.fileIndex).toPath()))
				return 0;
		}catch(Exception er){}	
		return -1;
	}
}

package com.enals.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.enals.server.DPortServer.iostatus;

//client Source
final class clientSource{
	private clientSource next;
	
	private fileHandler ufile,dfile;
	
	private Connection myconn;
	
	private BlockingQueue<ioBuffer> ioQueue;//4

	private ioBuffer ioHeader;		
	private BlockingQueue<ioBuffer> readQueue;//
	private BlockingQueue<ioBuffer> headerQueue;//
	private BlockingQueue<ioBuffer> commandQueue;//
	private BlockingQueue<ioBuffer> outQueue;//
	private BlockingQueue<ioBuffer> externalQueue;//
	private Map<Integer,ioBuffer> sendingDone;
	
	private ServerIOSource myServerSource;
	private ClientInstanceBody master;
	
	protected clientSource(int clientInstance,ServerIOSource myServerSource,ClientInstanceBody m) {		
		this.myServerSource = myServerSource;
		this.master = m;
		
		externalQueue = new ArrayBlockingQueue<ioBuffer>(clientInstance);
		ioQueue = new ArrayBlockingQueue<ioBuffer>(4);//4
		readQueue = new ArrayBlockingQueue<ioBuffer>(1);//
		headerQueue = new ArrayBlockingQueue<ioBuffer>(1);//
		commandQueue = new ArrayBlockingQueue<ioBuffer>(5);//
		outQueue = new ArrayBlockingQueue<ioBuffer>(24);//
		sendingDone = new HashMap<Integer,ioBuffer>();
		this.initConnection();
		this.initIOBuffer();
	}
	
	protected void initMaster(ClientInstanceBody m) {
		this.master = m;
		this.initConnection();
		this.clear();		
		this.initIOBuffer();
	}
	
	//init the sql connection
		protected boolean initConnection() {
			try{
				myconn = DriverManager.getConnection(myServerSource.getDBURL(),myServerSource.getSQLUserName(),myServerSource.getSQLUserPassword()); //create the connection to the database server
				return true;
			}catch(Exception e){
				e.printStackTrace();
			}			
			return false;
		}
	
	protected boolean initIOBuffer() {
		try {
			ioBuffer io;
			
			if (this.ioHeader == null) {
				if ((this.ioHeader = this.myServerSource.getFreeIO()) == null) return false;//why ?,clientinstance is smaller
			}
			
			if (this.ioHeader.getIONext() == null) {
				ioBuffer iob = new ioBuffer(this.ioHeader.getHeaderPosition(0),(1 << this.myServerSource.getIOBufferSize()),this.ioHeader.getHeaderByteBuffer(),false,null);
				this.ioHeader.setNext(iob);
				int t1 = (1 << this.myServerSource.getIOBufferSize());
		      for (int i = 0;i < 3;i++) {	      		      	
		      	try {
		      		io = new ioBuffer(this.ioHeader.getHeaderPosition(0) + t1,(1 << this.myServerSource.getIOBufferSize()),this.ioHeader.getHeaderByteBuffer(),false,null);
		      		iob.setNext(io);
		      		iob = io;
		      		t1 += (1 << this.myServerSource.getIOBufferSize());
		    		}catch(Exception er) {}
		      	}
			}			
			this.initIOQueue(true);		
			return true;//already init done
		}catch(Exception er) {}
		return false;
	}
	
	protected boolean initIOBody() {
		try {
			byte[] bb = new byte[(1 << 20) * this.myServerSource.getClientInstanceMemory()];
			ioBuffer io = this.ioHeader.getIONext();
			int t1 = 0;
			while (io != null) {
				BodyBuffer ibb = new BodyBuffer(t1,((1 << 20) * (this.myServerSource.getClientInstanceMemory() >>> 2)),0,null,bb,false);
				io.initBody(ibb);
				io = io.getIONext();	
				t1 += ((1 << 20) * (this.myServerSource.getClientInstanceMemory() >>> 2));
			}
			return true;
		}catch(Exception er) {}
		return false;
	}
	
	private synchronized void initIOQueue(boolean isclear) {
		try {
			ioBuffer io = this.ioHeader;
	      ioQueue.clear();
			while ((io = io.getIONext()) != null) {				
				if (isclear) {
					io.setIOStatus(iostatus.isIdel);
					ioQueue.offer(io);
					io.setMaster(this.master);
				}else {
					if (io.getStatus(iostatus.isIdel)) {
						ioQueue.offer(io);
					}
				}			
			}
		}catch(Exception er) {}	      	
	}
	
	protected void clear() {
		try {
			ioQueue.clear();
			readQueue.clear();
			headerQueue.clear();
			commandQueue.clear();
			outQueue.clear();
			externalQueue.clear();
			sendingDone.clear();				
		}catch(Exception er) {}
	}
	//put the ioheader back to the server source
	protected boolean addIOTOServerSource() {		
		try {
			myconn.close();			
		}catch(Exception er) {}
		ioHeader.setMaster(null);
		this.master = null;
		this.myServerSource.offerIO(ioHeader);
		this.ioHeader = null;
		return  this.myServerSource.offerClientSource(this);
	}
	
	protected Connection getSQLConnection() {
		return this.myconn;
	}
	
	protected Statement getStatement() {
		try {
			if (myconn.isClosed())
				if (!this.initConnection()) return null;
			return this.myconn.createStatement();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			return null;
		}
	}
	
	protected Statement getStatement(int resultSetType, int resultSetConcurrency) {
		try {
			if (myconn.isClosed())
				if (!this.initConnection()) return null;
			return this.myconn.createStatement(resultSetType, resultSetConcurrency);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			return null;
		}
	}
	
	protected PreparedStatement getPreparedStatement_1(String s1) {
		try {
			if (myconn.isClosed())
				if (!this.initConnection()) return null;
			return myconn.prepareStatement(s1);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			return null;
		}
	}
	
	protected PreparedStatement getPreparedStatement_2(String s2) {
		try {
			if (myconn.isClosed())
				if (!this.initConnection()) return null;
			return myconn.prepareStatement(s2);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			return null;
		}
	}
	
	protected PreparedStatement getPreparedStatement_3(String s3) {
		try {
			if (myconn.isClosed())
				if (!this.initConnection()) return null;
			return myconn.prepareStatement(s3);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			return null;
		}
	}
	
	protected BlockingQueue<ioBuffer> getCommandQueue(){
		return this.commandQueue;
	}
	
	protected BlockingQueue<ioBuffer> getHeaderQueue(){
		return this.headerQueue;
	}
	
	protected BlockingQueue<ioBuffer> getIOQueue(){
		return this.ioQueue;
	}
	
	protected ioBuffer getioHeader(){
		return this.ioHeader;
	}
	
	protected BlockingQueue<ioBuffer> getReadQueue(){
		return this.readQueue;
	}
	
	protected BlockingQueue<ioBuffer> getOutQueue(){
		return this.outQueue;
	}
	
	protected BlockingQueue<ioBuffer> getExternalQueue(){
		return this.externalQueue;
	}
		
	protected clientSource getNext() {
		return this.next;
	}
	/*
	 * get a IO
	 */
	protected ioBuffer getIO() {
		ioBuffer io = ioQueue.poll();
		try {
			if (io == null)
				this.initIOQueue(false);
			io = ioQueue.poll();
			io.setIOStatus(iostatus.isReading);
			io.clearHeader(0, 64);
			io.putHeaderLong(20, 0);
			io.clearBody(0, 0);
		}catch(Exception er) {}
		return io;
	}
	
	protected void setNext(clientSource n) {
		this.next = n;
	}
	
	protected ioBuffer offerReadQueueIO() {
		ioBuffer io  = getIO();
		return io == null ? null : (this.readQueue.offer(io) ? io : (ioQueue.offer(io) ? null : null));		
	}	
	
	protected boolean offerCommandQueueIO() {
		return this.commandQueue.offer(ioHeader);		
	}
}

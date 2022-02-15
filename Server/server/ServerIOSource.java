package com.enals.server;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Stack;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import javax.swing.JOptionPane;

import com.enals.server.DPortServer.ServerStatus;
import com.enals.server.DPortServer.clientStatus;

//all the memory buffer used by the server
final class ServerIOSource{
	private final int serverID;		
	private final byte EOT = 0x04;//EOT byte,flag
	private final long maskL = 5278580800269405267L;//the end of the data mask
	private final int maskH = 827212108;//the mask for the header
	private byte[] ioDataBuffer;//1M memory for the iobuffer,each iobuffer use 4096 bytes	
	private byte[] ib = new byte[128];
	private int iobuffer_3_1;
	private int iobody_3_1;
	private long keyDate;		
	
	private long serverIDL;
	
	private long userLockTimes = -1L;
	
	private int clientThreadWaitingMins = 5;//5 minuters waiting
	private int clientInstance = 20;//max client instance
	private int clientIOBufferSize = 14;//16K bytes used for the ioBuffer
	private int clientMem = 16;//16MB Memory used by each client instance
	private int readingFileCount = 5;//5 users allowed to read the file at the same time
	private int Max_fileHandler = 1024;//1024 files will be accessible at the same time
	private int sumOfClientInstanceBody = 0;
	
	private String DBurl = "jdbc:mysql://localhost/SEnals?useUnicode=true&anp;characterEncoding=utf8mb4"; //MySQL
	private String sqlUserName = "logonly";
	private String sqlUserPassword= "Passw0rd";
	
	private ByteBuffer fbic = ByteBuffer.wrap(ib);//used for the interaction with the client before the client is connected
	private ioBuffer ioQueueHeader;//the io Header of the client instance used		
	
	private final Map<Long,ClientInstanceBody> ActiveQueue = new HashMap<Long,ClientInstanceBody>();//quick search the live client
	
	private final Map<Long,ClientInstanceBody> clientMACQueue = new HashMap<Long,ClientInstanceBody>();//quick search the live client
	
	private Stack<ioBuffer> clientIoHeader;
	
	private BlockingQueue<ClientInstanceBody> dealingQueue;
	
	private BlockingQueue<ClientInstanceBody> disconnectedQueue;
	private BlockingQueue<ClientInstanceBody> attackingQueue;
		
	private BlockingQueue<ClientInstanceBody> dataWaitingQ0;	
	//private BlockingQueue<ClientInstanceBody> dataWaitingQ1;
	private BlockingQueue<ClientInstanceBody> dataWritingQ0;
	//private BlockingQueue<ClientInstanceBody> dataWritingQ1;
	private ArrayList<ClientInstanceBody> readingQueue = new ArrayList<ClientInstanceBody>();
	private BlockingQueue<clientSource> clientSourceQueue;
	
	private BlockingQueue<fileHandler> afileHandler = null;
	
	private ClientInstanceBody clientHeader;
	private clientSource clientSourceHeader;
	
	private Connection msc;
	private int tk = -1;
	
	private DPortServer cPort8189;
	private dataDeal cPort8181;
	private ExecutorService cHandle;
	
	private dataReceive dataReceiver = null;	
	private dataSend dataSender = null;
	
	private byte[] orgInfo;
	
	private String clientFileName;
	private long clientFileVersion;	
	private long CRCValue = -1;
	private byte[] clientFileBody;	
	
	protected ServerIOSource(int sid,
			int clientInstance,
			int clientIOBufferSize,
			int clientMem,
			int Max_fileHandler,
			String dburl,
			String sqluser,
			String sqlpassword,
			DPortServer cPort8189,
			Selector sl,
			long ltimes) throws Exception{
		this.serverID = sid;
		this.serverIDL =sid;
		this.serverIDL = this.serverIDL << 32;
		this.ioDataBuffer = new byte[(clientInstance + 1) * (1 << clientIOBufferSize) * 4];//bytes for each client instance used for the ioBuffer
		this.clientInstance = clientInstance;
		this.clientIOBufferSize = clientIOBufferSize;
		this.clientMem = clientMem;
		this.Max_fileHandler = Max_fileHandler;
		this.DBurl = dburl;
		this.sqlUserName = sqluser;
		this.sqlUserPassword = sqlpassword;
		this.cPort8189 = cPort8189;	
		this.userLockTimes = ltimes;
		this.cPort8181 = new dataDeal(this,sl);
		this.cHandle =Executors.newFixedThreadPool(4);
		
		try{//clear all the user logon status
			if (!this.initConnection()) throw new Exception("Connect to the SQL Server error");
			if (!this.UpdateORGbyte()) throw new Exception("Create the orgInfo byte array error");
			if (!this.UpdateCFByte()) throw new Exception("Create the client file byte array error");
			java.sql.Statement st = msc.createStatement();
			st.clearBatch();
			st.addBatch("Update TUserInformation set isLogon = false,isLocked = false");
			st.addBatch("Update THostInformation set isLogon = false");
			st.addBatch("Delete from TLoginInfo");
			st.executeBatch();
			this.generateInteractionKey();
		}catch(Exception er){
			JOptionPane.showMessageDialog(null, "Can not clear the database,Please close any thread those are using the database");
			System.exit(0);
		} 
		
        int t = 256;
		while (t * 3 < (1 << clientIOBufferSize)) {
			t += 256;
		}    	

		this.iobuffer_3_1 = (1 << clientIOBufferSize) - (t - 256);
		
		while (t * 3 < (1 << 20) * (clientMem >>> 2)) {
			t += 256;
		}  
		
		this.iobody_3_1 = (1 << 20) * (clientMem >>> 2) - (t - 256);
		fbic.putInt(40, iobuffer_3_1);
		fbic.putInt(44, iobody_3_1);
		fbic.putInt(48, clientIOBufferSize);
		fbic.putInt(52, (int)serverID);
		fbic.putInt(56, 827212108);		
		fbic.putShort(60, (short) clientMem);
		
		afileHandler = new ArrayBlockingQueue<fileHandler>(Max_fileHandler);		
		clientIoHeader = new Stack<ioBuffer>();
		
		dealingQueue = new ArrayBlockingQueue<ClientInstanceBody>(clientInstance * 10);	
		
		disconnectedQueue = new ArrayBlockingQueue<ClientInstanceBody>(clientInstance * 2);		
		attackingQueue = new ArrayBlockingQueue<ClientInstanceBody>(clientInstance * 2);
		
		dataWaitingQ0 = new ArrayBlockingQueue<ClientInstanceBody>(clientInstance * 2);	
		//dataWaitingQ1 = new ArrayBlockingQueue<ClientInstanceBody>(clientInstance * 2);
		dataWritingQ0 = new ArrayBlockingQueue<ClientInstanceBody>(clientInstance * 2);
		//dataWritingQ1 = new ArrayBlockingQueue<ClientInstanceBody>(clientInstance * 2);
		clientSourceQueue = new ArrayBlockingQueue<clientSource>(clientInstance * 2);
		
		this.dataReceiver = new dataReceive(dataWaitingQ0,null,this,this.cPort8181);
		this.dataSender = new dataSend(dataWritingQ0,null,this,this.cPort8181);
		
		this.cPort8181.start();
		this.dataReceiver.start();
		this.dataSender.start();		
	}		
	//check the interaction key
	protected boolean interactionCheck(ClientInstanceBody ci) {
		try {//check the interaction key
			//request to download the client file and the server is closed for download client
			if (ci.getICByteBuffere().getShort(20) == (short) 0) {
				if(this.cPort8189.getServerStatus() == ServerStatus.isAddingNewClosed)
					return false;
				else
				{
					return true;
				}
			}
			
			try {
				long icsk = Long.parseLong(new SimpleDateFormat("YYYYMMDD").format(new Date()));
				if ((icsk ^ keyDate) != 0)
				{
					this.generateInteractionKey();					
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
			
			if ((ci.getICByteBuffere().getLong(760) ^ keyDate) == 0) {
				for (int i =64;i < 128;i++)
				{
					if (ib[i] != ci.getICByteBuffere().array()[i])//if the interaction is not the correct
						return false;//attacking
				}
			}else {
				if (msc.isClosed()) {
					if (!initConnection()) throw new Exception("Connect to the SQL Server error");
				}
				try {					
					ResultSet rread = msc.createStatement().executeQuery("Select interactionKey from TICSKey where createTime = " + ci.getICByteBuffere().getLong(760 ));
					if (rread.next()) {
						for (int i =64;i < 128;i++)
						{
							if (ib[i] != rread.getBytes(1)[i - 64])//if the interaction is not the correct
								return false;//attacking
						}							
					}
					
					ci.setLoginCheck(ci.getICByteBuffere().getLong(760));
				} catch (Exception e1) {						
					e1.printStackTrace();
				}
			}				
			return true;
		}catch(Exception er) {				
		}
		return false;
	}
	//send the serverinfo to the client
	protected boolean sendSITClient(ClientInstanceBody ci,SocketChannel sout) {
		try {
			System.arraycopy(ib,0,ci.getICByteBuffere().array(), 0,64);
			ci.getICByteBuffere().limit(64);
			ci.getICByteBuffere().position(0);
			sout.write(ci.getICByteBuffere());
			if (ci.getICByteBuffere().hasRemaining()) {
				sout.write(ci.getICByteBuffere());
				if (ci.getICByteBuffere().hasRemaining()) {
					return false;
				}
			}
			return true;
		}catch(Exception er) {}
		return false;
	}
	//init the sql connection
	protected boolean initConnection() {
		try{
			msc = DriverManager.getConnection(DBurl,sqlUserName,sqlUserPassword); //create the connection to the database server
			return true;
		}catch(Exception e){
			e.printStackTrace();
		}			
		return false;
	}	
	//create interaction key
	protected void generateInteractionKey() {	
		ResultSet ssread = null;
		PreparedStatement ps1 = null;
		try {
			if (msc.isClosed()) {
				if (!initConnection()) throw new Exception("Connect to the SQL Server error");
			}
			
			long icsk = Long.parseLong(new SimpleDateFormat("YYYYMMDD").format(new Date()));			
			ssread = msc.createStatement().executeQuery("Select interactionKey from TICSKey where createTime = " + icsk);
			byte[] iakey = new byte[64];
			if (ssread.next()) {
				ssread.getBinaryStream(1).read(iakey, 0, 64);
				initInteractionKey(icsk,iakey);
			}else {
				ps1 = msc.prepareStatement("Insert into TICSKey(createTime,interactionKey) values(?,?)");
				ps1.setLong(1, icsk);
				ps1.setBinaryStream(2, ((InputStream) new ByteArrayInputStream(initInteractionKey(icsk,randomCode(64)),0,64)),64);
				ps1.execute();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			try {
				ssread.close();
			} catch (Exception e1) {					
				e1.printStackTrace();
			}
			try {
				ps1.close();
			} catch (Exception e1) {					
				//e1.printStackTrace();
			}
		}
	}
	//init the interaction key
	protected byte[] initInteractionKey(long kdate,byte[] rc) {
		try {
			this.keyDate = kdate;
			System.arraycopy(rc, 0, ib, 64, 64);				
			return rc;
		}catch(Exception er) {				
		}
		return null;
	}
	//init the ioBuffer
	protected void initIOBuffer() {
		try {        
      	//init the iobuffer 
			ioBuffer io;
      	ioBuffer iob  = ioQueueHeader = new ioBuffer(0,(1 << clientIOBufferSize) * 4,ioDataBuffer,true,null);
      	int t1 = (1 << clientIOBufferSize) * 4;
      	while (t1 < (clientInstance + 1) * (1 << clientIOBufferSize) * 4){
      		try {
					io = new ioBuffer(t1,(1 << clientIOBufferSize) * 4,ioDataBuffer,true,null);	        				
					offerIO(io);
	    			io.setPrev(iob);
	    			iob = io;
	    			t1 += (1 << clientIOBufferSize) * 4;
				}catch(Exception er) {}    	
      		} 
      	ioQueueHeader.setPrev(iob);//a circle by prev
      }catch(Exception er) {}
	}
	
	protected void offerIO(ioBuffer io) {		
		this.clientIoHeader.push(io);
	}
	
	protected void setCharSet(String cs){
		String lcs = DPortServer.charSet.displayName();
		try{
			DPortServer.charSet = Charset.availableCharsets().get(cs);
			for (int i = 0;i < 40;i++) ib[i] = EOT;
			System.arraycopy(cs.getBytes(), 0, ib, 0, cs.getBytes().length);
			ib[62] = (byte) (cs.getBytes().length);
			return;
		}catch(Exception e){
			e.printStackTrace();
		}//if error then reset it to the default
		DPortServer.charSet = Charset.forName(lcs);
		for (int i = 0;i < 40;i++) ib[i] = EOT;
		System.arraycopy(cs.getBytes(), 0, ib, 0, cs.getBytes().length);
		ib[62] = (byte) (cs.getBytes().length);
	}
	protected synchronized boolean addReadingQueue(ClientInstanceBody cb){
		return this.dataWaitingQ0.contains(cb) ? true : this.dataWaitingQ0.offer(cb);		
	}
	
	protected synchronized boolean addSendingQueue(ClientInstanceBody cb,boolean isFromSending,ioBuffer io){
		return cb.getClientSource().getOutQueue().contains(io) ? (this.dataWritingQ0.contains(cb) ? true : this.dataWritingQ0.offer(cb)) : (cb.getClientSource().getOutQueue().offer(io) ? (this.dataWritingQ0.contains(cb) ? true : this.dataWritingQ0.offer(cb)) : false);
	}	
	
	protected boolean checkFileVersion(long fv) {
		return (this.clientFileVersion ^ fv) == 0;
	}
	
	//offer active queue
	protected boolean offerActiveQueue(ClientInstanceBody ci) {
		try {	
			if (this.ActiveQueue.get(ci.getHostID()) !=null) {
				if (this.ActiveQueue.get(ci.getHostID()).isThreadStopped())
				{
					this.disconnectedQueue.offer(this.ActiveQueue.get(ci.getHostID()));					
				}
				this.ActiveQueue.put(ci.getHostID(), ci);
				return true;			
			}
			try {					
				this.disconnectedQueue.remove(ci);
			}catch(Exception er) {}
			try {					
				this.attackingQueue.remove(ci);
			}catch(Exception er) {}	
			this.ActiveQueue.put(ci.getHostID(), ci);			
		}catch(Exception er) {			
		}
		return this.ActiveQueue.get(ci.getHostID()) !=null;
	}
	
	//offer ClientInstanceBody
	protected boolean offerAttackingQueue(ClientInstanceBody ci,SocketChannel sc,SelectionKey sk) {			
		try {
			sk.attach(null);
			ci.DropChannelInput(sc);	
			ci.setStatus(clientStatus.isDISCONNECTED);
			if (this.attackingQueue.contains(ci)) return true;
			try {					
				this.ActiveQueue.remove(ci.getHostID());
			}catch(Exception er) {}						
		}catch(Exception er) {}
		return this.attackingQueue.offer(ci);
	}	
	//offer disconnected 
	protected boolean offerDisconnectedQueue(ClientInstanceBody ci) {
		try {
			ci.setStatus(clientStatus.isDISCONNECTED);
			ci.releaseClientSource();//release all the source
			try {					
				this.ActiveQueue.remove(ci.getHostID());					
			}catch(Exception er) {}	
		}catch(Exception er) {}
		return this.disconnectedQueue.contains(ci) ? true : this.disconnectedQueue.offer(ci);
	}
	
	protected boolean offerClientSource(clientSource cs) {
		return this.clientSourceQueue.contains(cs) ? true : this.clientSourceQueue.offer(cs);
	}
	
	protected boolean offerDealingQueue(ClientInstanceBody ci) {
		return this.dealingQueue.contains(ci) ? true : this.dealingQueue.offer(ci);
	}
	
	protected byte[] getInteractionKey() {
		return this.ib;
	}
	
	protected long getInteractionKeyDate() {
		return this.keyDate;
	}
	
	protected int getIOBufferSize() {
		return this.clientIOBufferSize;
	}
	
	protected String getDBURL() {
		return this.DBurl;
	}
	
	protected String getSQLUserName() {
		return this.sqlUserName;
	}
	
	protected String getSQLUserPassword() {
		return this.sqlUserPassword;
	}
	
	protected Connection getSQLConnection() {
		return this.msc;
	}
	
	protected synchronized Statement getStatement() {
		try {
			return this.msc.createStatement();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			return null;
		}
	}
	
	protected synchronized Statement getStatement(int resultSetType, int resultSetConcurrency) {
		try {
			return this.msc.createStatement(resultSetType, resultSetConcurrency);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			return null;
		}
	}
	
	protected synchronized PreparedStatement getPreparedStatement_1(String s1) {
		try {
			return msc.prepareStatement(s1);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			return null;
		}
	}
	
	protected synchronized PreparedStatement getPreparedStatement_2(String s2) {
		try {
			return msc.prepareStatement(s2);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			return null;
		}
	}
	
	protected ExecutorService getExector() {
		return this.cHandle;
	}
	
	protected int getClientInstanceNumer() {
		return this.clientInstance;
	}
	
	protected long getServerIDL() {
		return this.serverIDL;
	}
	
	protected long getUserLockTimes() {
		return this.userLockTimes;
	}
	
	protected ClientInstanceBody getLiveClient(long claddr) {
		return this.ActiveQueue.get(claddr);
	}
	
	protected boolean getServerRunningStatus() {
		return this.cPort8189.getServerRunningStatus();
	}
	
	protected boolean getServerRunningModel(ServerStatus ss) {
		return this.cPort8189.getServerStatus() == ss;
	}
	
	protected DPortServer getMyServer() {
		return this.cPort8189;
	}
	
	protected dataDeal getDataDealServer() {
		return this.cPort8181;
	}
	
	protected BlockingQueue<ClientInstanceBody> getDataWaitingQueue0(){
		return this.dataWaitingQ0;
	}
	/*
	protected BlockingQueue<ClientInstanceBody> getDataWaitingQueue1(){
		return this.dataWaitingQ1;
	}
	*/
	protected BlockingQueue<ClientInstanceBody> getDataWritingQueue0(){
		return this.dataWritingQ0;
	}
	/*
	protected BlockingQueue<ClientInstanceBody> getDataWritingQueue1(){
		return this.dataWritingQ1;
	}
	*/
	protected ArrayList<ClientInstanceBody> getReadingQueue() {
		return this.readingQueue;
	}
	//get host login date
	protected synchronized boolean getHostLoginInfo(long hostid,long logindate) {
		try{ 
    		ResultSet rread= null;   
    		try{    		
        		if (msc.isClosed()) 
    				if (!initConnection()) throw new Exception("Connect to the SQL Server error");
        		rread= msc.createStatement().executeQuery("Select hostID from THostInformation where hostID = " + hostid +" and lastLogin = " + logindate);
        		if (rread.next()){	    			
        			return true;
        		}    		
    		}catch(Exception e){			
    		}finally{
    			try {
    				rread.close();
    			} catch (Exception e1) {					
    				e1.printStackTrace();
    			}
    		}
		}catch(Exception e){			
		}
		return false;
	}
	//get file handler
	protected BlockingQueue<fileHandler> getFileHandler(){
		return this.afileHandler;
	}
	//get the io_buffer_3-1
	protected int getIO_Header_buffer_3_1() {
		return this.iobuffer_3_1;
	}
	
	protected int getIO_Body_buffer_3_1() {
		return this.iobuffer_3_1;
	}
	
	protected int getClientInstanceMemory() {
		return this.clientMem;
	}
	
	//get the orginfo
	protected byte[] getOrgInfo() {
		return this.orgInfo;
	}
	//get client status
	protected synchronized boolean getClientLoginStatus(long hostid) {
		try {
			if (this.ActiveQueue.get(hostid) != null)
			{
				return !this.ActiveQueue.get(hostid).isThreadStopped();
			}
		}catch(Exception er) {}
		return false;
	}
	//get free clientInstanceBody
	protected synchronized ClientInstanceBody getClientInstanceBody(SocketChannel channel,SelectionKey sKey) {
		ClientInstanceBody ci = this.attackingQueue.poll();
		try {				
			if (ci == null)
			{
				ci = this.disconnectedQueue.poll();
				if (ci == null)
				{
					if (sumOfClientInstanceBody++ > this.clientInstance * 10)						
					{
						if ((ci = this.dealingQueue.poll()) != null) {
							ci.reconnect(channel, sKey);
							return ci;
						}
						return null;
					}
					
					ci = new ClientInstanceBody(channel,
							sKey,
							this,
							DBurl,
							sqlUserName,
							sqlUserPassword,
							this.cPort8189.getServerStatus(),
							this.cPort8181,
							clientThreadWaitingMins
							);	
					if (this.clientHeader == null) {
						this.clientHeader = ci;						
					}else {
						ClientInstanceBody cn = this.clientHeader;
						while (cn.getNext() != null) {
							cn = cn.getNext();
						}
						cn.setNext(ci);						
					}
				}
        		else
        			ci.reconnect(channel,sKey);	
			}
  		else
  			ci.reconnect(channel,sKey);		
		}catch(Exception er) {}
		return ci;
	}				
	
	protected clientSource getClientSource(ClientInstanceBody cb) {
		clientSource cs = this.clientSourceQueue.poll();
		try {
			if (cs == null) {
				cs = new clientSource(this.clientInstance,this,cb);
				if (this.clientSourceHeader == null) {
					this.clientSourceHeader = cs;				
				}else {
					clientSource cn = this.clientSourceHeader;
					while (cn.getNext() != null) {
						cn = cn.getNext();
					}
					cn.setNext(cs);
				}					
			}else
				cs.initMaster(cb);
		}catch(Exception er) {}		
		return cs;
	}
	
	protected ioBuffer getFreeIO() {
		try {
			ioBuffer io = clientIoHeader.pop();
			if (io == null) {
				io = this.ioQueueHeader.getIOPrev();
				while (io.getMaster() != null && !io.equals(ioQueueHeader)) {					
					io = io.getIOPrev();
				}
				return io;
			}else
				return io;				
		}catch(Exception er) {}
		return null;
	}
	
	protected synchronized int getFile(ioBuffer io,int filepoint){			
		try{	
			if (clientFileBody == null)
			{
				if (!this.UpdateCFByte()){
					JOptionPane.showMessageDialog(null, "Could not create the client file"); 
					return 0;
				}
			}
			
			if (filepoint == 0) 
			{
				io.putHeaderLong(8, this.clientFileVersion);
				io.putHeaderLong(32, CRCValue);
				io.putHeaderLong(40, this.clientFileBody.length);
			}				
			io.putHeaderLong(48, filepoint);			
			System.arraycopy(clientFileBody, filepoint, io.getHeaderByteBuffer(), io.getHeaderPosition(64), filepoint + (1 << this.clientIOBufferSize) - 64 > this.clientFileBody.length ? this.clientFileBody.length - filepoint : (1 << this.clientIOBufferSize) - 64);
		}catch(Exception e3){}
		return filepoint + (1 << this.clientIOBufferSize) - 64 > this.clientFileBody.length ? this.clientFileBody.length - filepoint : (1 << this.clientIOBufferSize) - 64;
	}
	
	protected synchronized boolean UpdateCFByte() {
		ResultSet rread = null;
		try{    		
			if (msc.isClosed()) {
				if (!initConnection()) throw new Exception("Connect to the SQL Server error");
			}	
			
    		rread = msc.createStatement().executeQuery("Select fileID,fileName,fileBody,fileVersion from TClassFile where fileType = 'CLIENT' and fileID = (Select MAX(fileID) from TClassFile)");
    		if (rread.next()){
				clientFileName = rread.getString("fileName");
				clientFileVersion = rread.getLong("fileVersion");
				clientFileBody = rread.getBytes("fileBody").clone();
				CRCValue = this.doCRC32(clientFileBody);
			}else {
				try {
					PreparedStatement ps1 = msc.prepareStatement("Insert into TClassFile(fileName,fileBody,fileVersion,fileType) values(?,?,?,?)");
					SimpleDateFormat mft=new SimpleDateFormat("yyyyMMddHHMM");
					String ver = mft.format(new Date());
					clientFileBody = Files.readAllBytes(new File(System.getProperty("user.dir") + File.separator + "clients.jar").toPath());
					ps1.setString(1, "starOA.jar");
					ps1.setBinaryStream(2, ((InputStream) new ByteArrayInputStream(clientFileBody,0,clientFileBody.length)),clientFileBody.length);
					ps1.setLong(3, clientFileVersion = Long.parseLong(ver));
					ps1.setString(4,"CLIENT");
					ps1.execute();
					ps1.close();				
					clientFileName = "starOA.jar";
					CRCValue = this.doCRC32(clientFileBody);
				}catch(Exception er) {
					JOptionPane.showMessageDialog(null, "Please put file clients.jar to clients/");
					System.exit(0);
				}
			}
    		return true;
		}catch(Exception e){
			return false;
		}finally {
			try {
				rread.close();
			} catch (Exception e1) {					
				e1.printStackTrace();
			}
		}
	}
	
	 /*
     * get the organization information
     */
    protected synchronized boolean UpdateORGbyte(){
    	try{    		
			orgInfo = null;	
			if (msc.isClosed()) {
				if (!initConnection()) throw new Exception("Connect to the SQL Server error");
			}				
    		ResultSet rread = null;
    		try {
    			rread =  msc.createStatement().executeQuery("Select concat(orgName,'\\n',pCode,'\\n',orgType,idCode,'\\n') from TOrgInformation where orgType in ('A','C','D') and NOT isEdit");
    			StringBuilder tempStr=new StringBuilder();
        		while (rread.next()){
        			String s;
        			s = new String(rread.getBytes(1),"UTF-8");
        			tempStr.append(s);
        		}
        		tempStr.append("FF5278580800269405267FF\n");
        		try {
    				rread.close();
    			} catch (Exception e1) {					
    				e1.printStackTrace();
    			}
        		rread=msc.createStatement().executeQuery("Select concat(titleName,'\\n',titleID,'\\n',departmentID,'\\n') from TTitle");
        		while (rread.next()){
        			String s;
        			s = new String(rread.getBytes(1),"UTF-8");
        			tempStr.append(s);
        		}
        		tempStr.append("FF5278580800269405267FF\n");
        		orgInfo = tempStr.toString().getBytes(); 
			} catch (Exception e1) {					
				e1.printStackTrace();
			}finally {
				try {
    				rread.close();
    			} catch (Exception e1) {					
    				e1.printStackTrace();
    			}
			}
    		return true;
		}catch(Exception e){
			return false;
		}
    }
	/*
	 * get a host ID for the computer
	 */
	protected synchronized long getHostID(String mac,ClientInstanceBody cr){
		ResultSet rread = null;   
		Statement st = null;
		try{
			if (msc.isClosed()) 
				if (!initConnection()) throw new Exception("Connect to the SQL Server error");
    		long tk;
    		st = msc.createStatement();
			rread= st.executeQuery("Select hostID from TComputerInformation where itemField = 'MACADDRESS' and itemValue ='" + mac +"' limit 0,1");
    		if (rread.next()){	    			
    			return rread.getLong("hostID");
    		}else
    		{
    			try{rread.close();}catch(Exception e){}
    			rread= st.executeQuery("Select hostID,isLogon,isNormal from THostInformation where MACAddress ='" + mac +"'");
    			if (rread.next()){	    				
    				tk = rread.getLong(1);
    				rread= st.executeQuery("Select hostID from TSecurityKey where MACAddress ='" + mac +"'");
    				if (rread.next()) {
    					if (rread.getLong(1) == tk) {
    						return tk;
    					}else {
    						st.execute("Update TSecurityKey set hostID = " + tk + " where MACAddress ='" + mac +"'");
    						st.execute("Update THostInformation set isNormal = true where hostID =" + tk);
    						return tk;
    					}
    				}else {
    					st.execute("Insert into TSecurityKey(hostID,MACAddress) values(" + tk + ",'" + mac + "')");
    					st.execute("Update THostInformation set isNormal = true where hostID =" + tk);
    					return tk;
    				}
    			}
    			try{
    				rread.close();
	    			st.execute("Insert into THostInformation(MACAddress,isNormal) values('" + mac + "',false)");//the hostID will be auto-generated
					rread = st.executeQuery("Select hostID from THostInformation where MACAddress = '" + mac + "'");
					if (rread.next()){	    				
	    				tk = rread.getLong(1);	    				
	    				st.execute("Insert into TSecurityKey(hostID,MACAddress,needUpdate) values(" + (tk  + this.serverIDL) + ",'" + mac + "',true)");
	    				st.execute("Update THostInformation set isNormal = true,hostID = hostID + " + this.serverIDL + " where hostID =" + tk);
						return tk + this.serverIDL;
					}
    			}catch(Exception e){}
    			finally {
    				st.execute("delete from THostInformation where NOT isNormal");
    			}
    		}
		}catch(Exception e){}
		finally {
			try {
				rread.close();
			}catch(Exception er) {}
			try {
				st.close();
			}catch(Exception er) {}
		}
		return -1;
	}
	
	protected void removeFromDealingQueue(ClientInstanceBody ci) {
		if (ci != null)
			this.dealingQueue.remove(ci);
	}
	
	protected void setServerStatus(ServerStatus ss) {
		this.cPort8189.setServerStatus(ss);
	}
	
	private long doCRC32(byte[] cb){//md5 
		try{
			CheckedInputStream cis = new CheckedInputStream(((InputStream) new ByteArrayInputStream(cb,0,cb.length)),new CRC32());
			byte[] buf = new byte[128];
			while (cis.read(buf) >= 0){}
			long rv = cis.getChecksum().getValue();
			cis.close();
			cis = null;
			return rv;
		}catch(Exception e){
			e.printStackTrace();
		}
		return -1;
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

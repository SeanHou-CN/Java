package com.enals.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.enals.server.DPortServer.ServerStatus;
import com.enals.server.DPortServer.clientStatus;
import com.enals.server.DPortServer.iostatus;

//client Thread is here
public class ClientInstanceBody implements Runnable{	
	private ClientInstanceBody next;
	
	private long macAddressL_0 = -1L;
	private long macAddressL_1 = -1L;
	private long macAddressL_2 = -1L;
	private long loginDate = 0L;
	private long loginTime = -1L;	
	
	private long userID= -1;
	private long hostID = -1;
	
	private long fileLimitSize = -1L;
	
	private clientStatus myStatus = clientStatus.isWAITINGINIT;
	
	private SelectionKey skeyIn,skeyOut;//selectionkey
	private SocketChannel scIn,scOut;//8189,8180 port		
	private String scIPAddress;//ipaddress
	private String scMacAddress;
	private int osType = -1;//os type
	private byte[] secKey1,secKey2,secKey3;
	private byte[] levelControl = new byte[2048];
	protected ByteBuffer ickr = ByteBuffer.wrap(new byte[4096]);
	
	private PreparedStatement ps1,ps2,ps3;
	private java.sql.Statement sSRead,sSUpdate,sClear;
	private ResultSet sKRS,sKRS2;
	
	private String userName="";
	
	private int ThreadWaitingMins = 1;
	private int keyID = -1;//the securityKey ID	
	private int tsp = 0,tep = 0;//temp 
	
	private int userAgency = 0;
	private int userCompany = 0;
	private int userDepartment = 0;
	
	private int errors = 0;
	
	private short userLevel = 100;
	
	private short funnum = -1;//function number
	private byte commnum = -1;//command number
	
	protected clientSource mySource;
	private ServerIOSource myServerSource;	
	
	private BlockingQueue<ioBuffer> readQueue;
	private BlockingQueue<ioBuffer> commandQueue;
	private BlockingQueue<ioBuffer> outQueue;
	private ioBuffer rio;
	//command and function progress
	private Map<Byte,Short> cfp = new HashMap<Byte,Short>();
	
	protected boolean isStopped = true;
	protected boolean isRunning = false;	
	
	protected ClientInstanceBody(SocketChannel outps,
			SelectionKey sk,
			ServerIOSource mss,
			String dburl,
			String sqluser,
			String sqlpassword,
			ServerStatus ss,
			dataDeal datadeal,
			int threadWaitingM) {
		this.skeyOut = sk;
		this.scOut = outps;
		this.myStatus = clientStatus.isGETTINGDATA;
		this.loginTime = System.currentTimeMillis();
		this.ickr.limit(128 );
		this.ickr.position(0);
		this.tsp = 0;
		this.ThreadWaitingMins = threadWaitingM;
		this.myServerSource = mss;
	}	
	
	//init the param
	protected void reconnect(SocketChannel outps,SelectionKey skey) {
		this.macAddressL_0 = 0L;
		this.loginDate = 0L;
		this.tsp = 0;
		this.tep = 0;
		
		closeChannel(this.scOut,skeyOut);
		closeChannel(this.scIn,skeyIn);
		
		this.scOut = outps;
		this.skeyOut = skey;
		this.loginTime = System.currentTimeMillis();
		this.myStatus = clientStatus.isGETTINGDATA;
		this.ickr.limit(128 );
		this.ickr.position(0);	
		this.errors = 0;
	}
	
	protected boolean StopIt() {
		this.isStopped = true;	
		this.mySource.offerCommandQueueIO();		
		return this.isRunning;
	}
	
	//if the client instance is not running,then run it
	protected synchronized void startIT(){				
		try{
			//System.out.println("startIT:" + this.toString());
			if (!this.isRunning) {
				this.myServerSource.getExector().execute(this);
			}
		}catch(Exception e){				
		}
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try{				
			this.isRunning = true;
			this.isStopped = false;
			ioBuffer io = null;
			while (!this.isStopped && this.myServerSource.getServerRunningStatus()) {
				try{
					//System.out.println("waiting.....");
					io = this.commandQueue.poll(1000 * 60 * this.ThreadWaitingMins,TimeUnit.MILLISECONDS);
					//System.out.println("waiting end *******" + this.toString());
					
					if (this.isStopped) return;
					
					if (io == null)//check the client is still online
					{
						this.setStatus(clientStatus.isSLEEPING);						
						this.dealingIO(mySource.getIO(), 10, 0, 0, 0, 0, 0);//check the client is still on line
						return;
					}					
					
					io.setIOStatus(iostatus.isDealing);
					
					if (this.myStatus == clientStatus.isLOGIN || this.myStatus == clientStatus.isSLEEPING) {
						switch (io.getHeaderByte(24)) {
						case 3://forward the command to the other client
						case 4:
						case 5:						
							break;
						case 0:
						case 1:
						case 2:
							this.commnum = io.getHeaderByte(22);
							this.funnum = io.getHeaderShort(20);
							this.myStatus = clientStatus.isLOGIN;
							System.out.println("commnum:" + this.commnum + " and funnum:" + this.funnum);
							switch (this.commnum) {
							case 3:
								dealing_IO_3(io);
								break;
							case 4:
								dealing_IO_4(io);
								break;
							case 5:
								dealing_IO_5(io);
								break;	
							case 6:
								dealing_IO_6(io);
								break;
							case 7:
								dealing_IO_7(io);
								break;
							case 10:
								this.setStatus(clientStatus.isSLEEPING);
								return;								
								default:
									this.setStatus(clientStatus.isDISCONNECTED);
									return;
							}
							break;
							default://error or attack
							/*
							 * deal error code here
							 */
						}
					}else {//why we coming here
						this.setStatus(clientStatus.isDISCONNECTED);
						return;
					}
				}catch(Exception e){				
				}finally {
					try {
						sKRS.close();				
					}catch(Exception er) {}
					try {
						sSRead.close();				
					}catch(Exception er) {}
					try {				
						sSUpdate.close();
					}catch(Exception er) {}
					try {				
						ps1.close();
					}catch(Exception er) {}
					try {				
						ps2.close();
					}catch(Exception er) {}
					try {				
						ps3.close();
					}catch(Exception er) {}
					try {				
						mySource.getStatement().execute("UNLOCK TABLES");
					}catch(Exception er) {}
					try {				
						if (io.getStatus(iostatus.isDealing))
							this.addIOTOpool(io);
					}catch(Exception er) {}
				}								
			}			
		}catch(Exception e){}
		finally {
			System.out.println("Quit..........");
			this.isRunning = false;
			this.isStopped = true;
			this.loginTime = System.currentTimeMillis();
		}
	}
	//
	private void dealing_IO_3(ioBuffer io) {
		try {
			if (cfp.get(commnum) != this.funnum) throw new Exception("Error command");
			sSRead = mySource.getStatement();
			switch (this.funnum) {
			case 0:				
				sKRS = sSRead.executeQuery("Select keyID,securityLocker,singleKey,singleComputer,hostID,needUpdate,disabled,toIDFile,secKey2,done,updateTimes,decodeKey,isLocked,lockTime from TSecurityKey where hostID = " + this.hostID);
				if (sKRS.next()) {
					
					if (sKRS.getBoolean(7) || ((sKRS.getBoolean(3) || sKRS.getBoolean(4))) || !sKRS.getBoolean(6)){//do not allow the user to get the new client before the admin to set the flag of needUpdate to true
						this.dealingIO(io, commnum, funnum, 0, 0, 0, 3);//disabled or ?
						break;
					}else
					{	
						if (sKRS.getBoolean("isLocked")){  
							if (new Date().getTime() - sKRS.getLong("lockTime") >= this.myServerSource.getUserLockTimes())
							{
								sKRS.updateBoolean("isLocked", false);
								sKRS.updateRow();
							}else
							{
								this.dealingIO(io, commnum, funnum, 0, 0, 0, 2);//locked	    	    						
	    						break;
							}
						}					
					}
					
					sSRead.clearBatch();
					sSRead.addBatch("delete from TComputerInformation where version <>0 and hostID = " + this.hostID);
					sSRead.addBatch("delete from TComputerDetail where version <> 0 and hostID = " + this.hostID);		
					sSRead.executeBatch();  
					cfp.put(this.commnum,(short) 1);
					this.dealingIO(io, commnum, funnum, 0, 0, 0, 1);
				}else {
					ResultSet getID = sSRead.executeQuery("Select hostID from TComputerInformation where itemField = 'MACAddress' and itemValue ='" + this.scMacAddress + "' limit 0,1");
					if (getID.next()){	    						
						sSRead.addBatch("delete from TComputerInformation where version <> 0 and hostID = " + this.hostID);
						sSRead.addBatch("delete from TComputerDetail where version <> 0 and hostID = " + this.hostID);	    						
						sSRead.executeBatch();
					}else
					{    						
						getID = sSRead.executeQuery("Select hostID from THostInformation where MACAddress ='" + this.scMacAddress +"' limit 0,1");
						if (getID.next()){	    						
    						sSRead.addBatch("delete from TComputerInformation where version <> 0 and hostID = " + this.hostID);
    						sSRead.addBatch("delete from TComputerDetail where version <> 0 and hostID = " + this.hostID);	    						
    						sSRead.executeBatch();
						}else
						{
							this.dealingIO(io, commnum, funnum, 0, 0, 0, 4);//could not find the hostid
							break;
						}
					}
					cfp.put(this.commnum,(short) 1);
					this.dealingIO(io, commnum, funnum, 0, 0, 0, 1);
				}
				break;
			case 1:
				if ((tsp = this.decodeIO(io)) == -1) throw new Exception("Decode Data Error");
				ByteArrayInputStream ba = new ByteArrayInputStream(io.getHeaderByteBuffer(),io.getHeaderPosition(this.myServerSource.getIO_Header_buffer_3_1()),tsp);
				ObjectInputStream inp = new ObjectInputStream(ba);
				Map<String,Map<String,String>> pcInfo = null;
				try{
					pcInfo = (Map<String,Map<String,String>>) inp.readObject();
				}catch(Exception er){
					throw new Exception("error get the pcInfo");
				}
				ps1 = mySource.getPreparedStatement_1("Insert into TComputerInformation(hostID,osType,itemName,itemField,itemValue) values(" + this.hostID + "," + this.osType +",?,?,?)");
				Iterator<String> itS = pcInfo.keySet().iterator();
				ps1.clearBatch();
				String s1="",s2 = "",s3 = "";
				boolean fm = false;
				while (!this.isStopped && itS.hasNext())
				{	
					String SQL = itS.next();					
					if (SQL.indexOf("OSINFORMATION") ==0){
						if (pcInfo.get(SQL).get("CSNAME") != null)
						{
							ps1.setString(1, SQL);
							ps1.setString(2, "CSNAME");
							ps1.setString(3, s2 = pcInfo.get(SQL).get("CSNAME"));
							ps1.addBatch();									
						}
					}else
					if (SQL.indexOf("OS") == 0)
					{
						if (pcInfo.get(SQL).get("DIGITALPRODUCTID") != null)
						{
							ps1.setString(1, SQL);
							ps1.setString(2, "DIGITALPRODUCTID");
							ps1.setString(3, s3 = pcInfo.get(SQL).get("DIGITALPRODUCTID"));
							ps1.addBatch();
						}
						if (pcInfo.get(SQL).get("PRODUCTNAME") != null)
						{
							ps1.setString(1, SQL);
							ps1.setString(2, "PRODUCTNAME");
							ps1.setString(3, s1 = pcInfo.get(SQL).get("PRODUCTNAME"));
							ps1.addBatch();									
						}
					}else
					if (pcInfo.get(SQL).get("MACADDRESS") != null)
					{
						fm = true;
						ps1.setString(1, SQL);
						ps1.setString(2, "MACADDRESS");
						ps1.setString(3, pcInfo.get(SQL).get("MACADDRESS"));
						ps1.addBatch();
					}
				}
				if (!fm) {
					ps1.setString(1, "NICC0");
					ps1.setString(2, "MACADDRESS");
					ps1.setString(3, this.scMacAddress);
					ps1.addBatch();
				}
				ps1.executeBatch();				
				ps2 = mySource.getPreparedStatement_2("Insert into TComputerDetail(detailInformation,hostID,osType) values(?,?,?)");    					
				ps2.setBinaryStream(1, ((InputStream)new ByteArrayInputStream(io.getHeaderByteBuffer(),io.getHeaderPosition(this.myServerSource.getIO_Header_buffer_3_1()),tsp)),tsp);
				ps2.setLong(2, this.hostID);
				ps2.setInt(3, this.osType);
				ps2.execute();
				
				sSRead.execute("Update THostInformation set OS = '" + s1 + "',computerName = '" + s2 + "' where hostID =" + this.hostID);
				if (!s3.equals("")) sSRead.execute("Update THostInformation set ProductID = '" + s3 + "' where hostID =" + this.hostID);
				
				tsp = this.myServerSource.getFile(io, 0);
				tep = tsp;
				cfp.put(this.commnum, (short) 2);
		    	this.dealingIO(io, commnum, funnum, tsp, 1, 2, 0);
				break;
			case 2:
				int len = (int) io.getHeaderLong(48);
	    		if (len != tep) {
	    			try{setStatus(clientStatus.isDISCONNECTED);}catch(Exception ne){}//attack
		    		return;
	    		}
	    		tsp = this.myServerSource.getFile(io, tep);
	    		tep += tsp;
	    		if (tsp > 0)
	    		{
	    			this.dealingIO(io, commnum, funnum, tsp, 1, 2, 0);
	    		}
	    		else {			    			
	    			try{
	    				io.clearHeader(0, this.myServerSource.getIO_Header_buffer_3_1() + 2048);
	    				long dk3 = System.currentTimeMillis();
	    				/*
	    				sSUpdate = mySource.getStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
	    				sKRS = sSUpdate.executeQuery("Select decodeKey,securityLocker,decodeKey2,keyID from TSecurityKey where hostID = " + (int)this.hostID + " for Update");
	    				sKRS.next();
	    				sKRS.updateBytes(1, randomCode(64));
	    				sKRS.updateBytes(2, randomCode(512));
	    				sKRS.updateLong(3, dk3);
	    				sKRS.updateRow();
	    				System.arraycopy(sKRS.getBytes(1), 0, io.getHeaderByteBuffer(), io.getHeaderPosition(64), 64);
	    				System.arraycopy(sKRS.getBytes(2), 0, io.getHeaderByteBuffer(), io.getHeaderPosition(128), 512);
	    				*/
	    				
	    				System.arraycopy(randomCode(64), 0, io.getHeaderByteBuffer(), io.getHeaderPosition(64), 64);
	    				System.arraycopy(randomCode(512), 0, io.getHeaderByteBuffer(), io.getHeaderPosition(128), 512);
	    				
	    				ps2 = mySource.getPreparedStatement_2("Update TSecurityKey set decodeKey = ?,securityLocker = ?,decodeKey2 = ? where hostID = " + this.hostID);
	    				ps2.setBinaryStream(1, ((InputStream)new ByteArrayInputStream(io.getHeaderByteBuffer(),io.getHeaderPosition(64), 64)),64);
	    				ps2.setBinaryStream(2, ((InputStream)new ByteArrayInputStream(io.getHeaderByteBuffer(),io.getHeaderPosition(128), 512)),512); 
	    				ps2.setLong(3, dk3);
	    				ps2.execute();
	    				
	    				io.putHeaderLong(this.myServerSource.getIO_Header_buffer_3_1() + 1024, dk3);
	    				
	    				int dp = 0;			
	    				//System.out.println();
	    				for (int index = 0;index < 512;index++){//XOR, 0x1F (% 32)
	    					dp += ((io.getHeaderByte(64 + (index & 0x3F)) & 0x1) > 0 ? 1 : -(io.getHeaderByte(64 + (index & 0x3F)) & 0x1));
	    					io.getHeaderByteBuffer()[io.getHeaderPosition(this.myServerSource.getIO_Header_buffer_3_1() + dp++)] = (byte)(io.getHeaderByte(128 + index) ^ io.getHeaderByte(64 + (index & 0x3F)));//store the secKey1. and this is encoded by 64 bytes		    					
	    					//System.out.print(io.getByteBuffer()[io.headerpostion + iobuffer_3_1 + dp++] + "*");
	    				}	    				
	    				
	    				cfp.put(this.commnum, (short) 3);
	    				this.dealingIO(io, commnum, 3, 1024 + 8, 1, 3, 0);
	    			}catch(Exception er){		    				
	    				throw new Exception("Error handle the securitykey");
	    			}
	    		}
				break;
			case 3:
				ps1 = mySource.getPreparedStatement_1("update TSecurityKey set secKey2 = ? where hostID = " + this.hostID);
				ps1.setBytes(1, String.valueOf(io.getHeaderLong(32)).getBytes());
				ps1.execute();
				ps1.close();
				ps1 = null;
				
				sSRead.clearBatch();
				sSRead.addBatch("Update TComputerInformation set status = (select count(version) from TComputerDetail where hostID =  " + this.hostID + " and osType = " + this.osType + "),version = (select count(version) from TComputerDetail where hostID = " + this.hostID + " and osType = " + this.osType + ") - 1 where version = -1 and hostID = "  + this.hostID + " and osType = " + this.osType);
				sSRead.addBatch("Update TComputerInformation set status = status - 1 where (select count(version) from TComputerDetail where hostID = "  + this.hostID + " and osType = " + this.osType + ") = 2 and hostID = "  + this.hostID + " and osType = " + this.osType);
				sSRead.executeBatch();
				sSRead.clearBatch();
				
				sSRead.addBatch("Update TComputerDetail set status = 0 where version = 0 and hostID = " + this.hostID + " and osType = " + this.osType);
				sSRead.addBatch("Update TComputerDetail set status = IFNULL((select status from TComputerInformation where version = 1 and hostID = " + this.hostID + " and osType = " + this.osType + " limit 0,1),1),version = IFNULL((select version from TComputerInformation where version = 1 and hostID = " + this.hostID + " and osType = " + this.osType + " limit 0,1),0) where version = -1 and hostID = "  + this.hostID + " and osType = " + this.osType);	    			
				sSRead.addBatch("Update THostInformation set done = TRUE where hostID = " + this.hostID);
    			sSRead.addBatch("Update TSecurityKey set done = TRUE,needUpdate = TRUE,updateTimes = updateTimes + 1 where hostID = " + this.hostID);
				sSRead.executeBatch();
				cfp.put(this.commnum, (short) 4);
				this.dealingIO(io, commnum, 4, 0, 0, 0, 0);		    		    	
		    	break;	
			case 4:				
				sKRS = sSRead.executeQuery("Select keyID,securityLocker,secKey2,decodeKey from TSecurityKey where hostID = " + this.hostID + " and MACAddress = '" + this.scMacAddress + "' and decodeKey2 = " + io.getHeaderLong(32));
				if (sKRS.next()) {
					tsp = sKRS.getBinaryStream("decodeKey").read(io.getHeaderByteBuffer(), io.getHeaderPosition(this.myServerSource.getIO_Header_buffer_3_1()), 1024);	    		
					
		    		this.dealingIO(io, commnum, 5, tsp, 1, 3, 0);	
		    		tep = sKRS.getBinaryStream(3).read(secKey2, 0, 64);	    	
					tep = sKRS.getBinaryStream(2).read(secKey1, 0, 512);
		    		this.keyID = sKRS.getInt(1);
		    		sSRead.execute("Update TSecurityKey set needUpdate = false where hostID = " + this.hostID);
				}
				break;			
			}
		}catch(Exception er) {
			this.printError(er);
			this.dealingIO(io, commnum, funnum, 0, 0, 0, 4);//errors 4
		}
	}
	
	private void dealing_IO_4(ioBuffer io) {//check the file version
		try {	
			switch (this.funnum) {
			case 0://check the file version
				if (this.myServerSource.checkFileVersion(io.getHeaderLong(32))) {
					this.dealingIO(io, commnum, 1, 0, 0, 0, 0);//errors 4
				}else {
					if (cfp.get((byte) 3) == (short) 0)
						mySource.getStatement().execute("Update TSecurityKey set needUpdate = true where hostID = " + this.hostID + " and MACAddress = '" + this.scMacAddress + "'");
					this.dealingIO(io, commnum, 0, 0, 0, 0, 0);//errors 4
				}
				break;	
			case 2:
				io.clearHeader(0, 2048);
				io.putHeaderLong(0, this.myServerSource.getServerIDL());
				io.putHeaderLong(8, this.hostID);
				io.putHeaderLong(64, userID);
				System.arraycopy(this.levelControl, 0, io.getHeaderByteBuffer(), io.getHeaderPosition(64 + 8), 2048);
				this.dealingIO(io, commnum, 2, 2048, 1, 2, 0);//
				break;
			}
						
		}catch(Exception er) {
			this.printError(er);
			this.dealingIO(io, commnum, funnum, 0, 0, 0, 4);//errors 4
		}
	}
	
	private void dealing_IO_5(ioBuffer io) {
		try {
			try {
				switch (this.funnum) {
				case 0:
					sSRead = mySource.getStatement();
					sKRS = sSRead.executeQuery("Select keyID,securityLocker,singleKey,singleComputer,hostID,needUpdate,disabled,toIDFile,secKey2,done,updateTimes,decodeKey,isLocked,lockTime from TSecurityKey where hostID = " + this.hostID);
					if (sKRS.next()) {
						if (this.userLevel == 1) {
							if (sKRS.getBoolean("isLocked")){  
    							if (new Date().getTime() - sKRS.getLong("lockTime") >= this.myServerSource.getUserLockTimes())
    							{
    								sKRS.updateBoolean("isLocked", false);
    								sKRS.updateRow();
    							}else
    							{
    								this.dealingIO(io, commnum, funnum, 0, 0, 0, 2);
    	    						sKRS.close();    
    	    						break;
    							}
    						}	    						
							sSRead.clearBatch();
	    					sSRead.addBatch("delete from computerInformation where version <>0 and hostID = " + this.hostID);
	    					sSRead.addBatch("delete from computerDetail where version <> 0 and hostID = " + this.hostID);		    						
	    					sSRead.executeBatch();  
	    					
						}else {
							
						}
					}else {
						
					}
					break;
				}
			}catch(Exception er) {}
		}catch(Exception er) {}
	}
	
	private void dealing_IO_6(ioBuffer io) {
		try {
			try {
				switch (this.funnum) {
				case 0:
					sSRead = mySource.getStatement();
					sKRS = sSRead.executeQuery("Select keyID,securityLocker,singleKey,singleComputer,hostID,needUpdate,disabled,toIDFile,secKey2,done,updateTimes,decodeKey,isLocked,lockTime from TSecurityKey where hostID = " + this.hostID);
					if (sKRS.next()) {
						if (this.userLevel == 1) {
							if (sKRS.getBoolean("isLocked")){  
    							if (new Date().getTime() - sKRS.getLong("lockTime") >= this.myServerSource.getUserLockTimes())
    							{
    								sKRS.updateBoolean("isLocked", false);
    								sKRS.updateRow();
    							}else
    							{
    								this.dealingIO(io, commnum, funnum, 0, 0, 0, 2);
    	    						sKRS.close();    
    	    						break;
    							}
    						}	    						
							sSRead.clearBatch();
	    					sSRead.addBatch("delete from computerInformation where version <>0 and hostID = " + this.hostID);
	    					sSRead.addBatch("delete from computerDetail where version <> 0 and hostID = " + this.hostID);		    						
	    					sSRead.executeBatch();  
	    					
						}else {
							
						}
					}else {
						
					}
					break;
				}
			}catch(Exception er) {}
		}catch(Exception er) {}
	}
	
	private void dealing_IO_7(ioBuffer io) {
		try {
			try {
				switch (this.funnum) {
				case 0:
					sSRead = mySource.getStatement();
					sKRS = sSRead.executeQuery("Select keyID,securityLocker,singleKey,singleComputer,hostID,needUpdate,disabled,toIDFile,secKey2,done,updateTimes,decodeKey,isLocked,lockTime from TSecurityKey where hostID = " + this.hostID);
					if (sKRS.next()) {
						if (this.userLevel == 1) {
							if (sKRS.getBoolean("isLocked")){  
    							if (new Date().getTime() - sKRS.getLong("lockTime") >= this.myServerSource.getUserLockTimes())
    							{
    								sKRS.updateBoolean("isLocked", false);
    								sKRS.updateRow();
    							}else
    							{
    								this.dealingIO(io, commnum, funnum, 0, 0, 0, 2);
    	    						sKRS.close();    
    	    						break;
    							}
    						}	    						
							sSRead.clearBatch();
	    					sSRead.addBatch("delete from computerInformation where version <>0 and hostID = " + this.hostID);
	    					sSRead.addBatch("delete from computerDetail where version <> 0 and hostID = " + this.hostID);		    						
	    					sSRead.executeBatch();  
	    					
						}else {
							
						}
					}else {
						
					}
					break;
				}
			}catch(Exception er) {}
		}catch(Exception er) {}
	}
	/*
	 * add reading io
	 */
	protected synchronized void addReadingIO() {
		try {
			if (this.isThreadStopped()) {
				ickr.clear();
				this.readekey(this.scIn);
				return;
			}			
			try {				
				if (this.readQueue.size() == 0) 
				{
					if ((rio = this.mySource.offerReadQueueIO()) == null) return;						
				}
				
				if (rio.getHeaderCurrentPositionLen() < 64)
				{
					if (rio.isHeaderHasRemaining()) {//64 bytes
						if (this.readIOHeader(rio) == -1) throw new Exception("Closed Channel");
						if (this.rio.isHeaderHasRemaining()) {
							if (this.readIOHeader(rio) == -1) throw new Exception("Closed Channel");
							if (rio.isHeaderHasRemaining()) //64 bytes,should be disconnected
								/*
								 * some code here to deal with the client,why here?
								 */
								return;
						}
					}
					switch (rio.getHeaderByte(24)) {
					case 0:
						//System.out.println("readio:" + this.toString());
						this.addCommandFromReading(this.readQueue.poll());
						return;						
					case 1://with more data in the header
					case 5://forward the data to another client
						rio.clearHeader(64, rio.getHeaderInt(28) + 64);	
						break;
					case 2://with more data in the body
					case 4://forward the data to another client
						if (rio.getBody() == null) {
							mySource.initIOBody();
						}
						rio.clearBody(0, rio.getHeaderInt(28));
						break;				
					}
				}
				//if there is more data need to read
				this.myServerSource.addReadingQueue(this);
			}catch(Exception er) {
				//printError(er);
				try{setStatus(clientStatus.isDISCONNECTED);}catch(Exception ne){}				
			}
		}catch(Exception e) {
			printError(e);				
		}
	}
	
	protected synchronized void addCommandFromReading(ioBuffer io) {
		try {
			this.startIT();
			this.commandQueue.offer(io);			
		}catch(Exception e) {
			printError(e);
		}
	}
	
	@SuppressWarnings("finally")
	protected synchronized boolean addCommandFromSending(ioBuffer io) {
		try {					
			if (addIOTOpool(io))
				this.outQueue.remove(io);
		}catch(Exception e) {	
			printError(e);
		}finally {
			return this.outQueue.size() > 0;
		}
	}
	
	@SuppressWarnings("finally")
	protected boolean addCommandFromReading() {
		try {	
			this.startIT();			
			this.commandQueue.offer(readQueue.poll());
		}catch(Exception e) {	
			printError(e);
		}finally {
			return readQueue.size() > 0;
		}
	}
	/*
	 * add IO to iopool
	 */
	protected boolean addIOTOpool(ioBuffer io) {
		try {
			io.setIOStatus(iostatus.isIdel);
			return mySource.getIOQueue().contains(io) ? true :mySource.getIOQueue().offer(io);			
		}catch(Exception er) {
			return false;
		}			
	}
	
	protected void addIOTOHeaderpool(ioBuffer io) {
		try {
			mySource.getHeaderQueue().offer(io);							
		}catch(Exception er) {}			
	}
	
	//
	protected boolean checkInteractionStatus() {
		try {
			ickr.clear();
			this.macAddressL_0 = ickr.getLong(752);
			this.scMacAddress = new String(ickr.array(),576 ,ickr.array()[575 ]);	
			if ((this.hostID = myServerSource.getHostID(scMacAddress, this)) == -1) 
				return false;//already connected
			
			if (myServerSource.getClientLoginStatus(this.hostID)) return false;//the user is login already
			
			if (this.myStatus == clientStatus.isCHECKINGLOGINDATE)		
			{
				if (!this.myServerSource.getHostLoginInfo(hostID, loginDate))
					return false;
			}
			
			this.osType = ickr.array()[478 ];
			
			byte[] ip4 = new byte[4],ip6 = new byte[16]; 
			if (ickr.array()[479 ] == 4)
			{
				System.arraycopy(ickr.array(), 480 , ip4, 0, 4);//IP4
				this.scIPAddress = InetAddress.getByAddress(ip4).getHostAddress();
			}else {
				System.arraycopy(ickr.array(), 480 , ip6, 0, 16);//IP6
				this.scIPAddress = InetAddress.getByAddress(ip6).getHostAddress();
			}
			if (secKey1 == null) secKey1 = new byte[512];
			if (secKey2 == null) secKey2 = new byte[64];
			if (secKey3 == null) secKey3 = new byte[32];
			
			System.arraycopy(ickr.array(), 128 , secKey1, 0, 512);
			System.arraycopy(ickr.array(), 512 + 128 , secKey2, 0, 64);
			System.arraycopy(ickr.array(), 576 + 128 , secKey3, 0, 32);
			
			tsp = 0;//reset the position	
			this.setByteBufferpl(0, 64, 0);
			return true;
		} catch (Exception e) {//if pconn is null	
			printError(e);
		}
		return false;
	}
	//check scIn channel
	protected boolean checkInPortConnection(byte[] ipb) {
		try {
			tsp = 64;
			while (ipb[tsp - 32] == ickr.array()[tsp] && ++tsp < 96) {}
			return tsp == 96;
		}catch(Exception er) {}
		return false;
	}
	/* use login check
	 * 	
	 * ickr.array,512 bytes agency code,512  
	 */
	protected boolean checkingUserLogin(SocketChannel channel) {
		try {
			if (errors > 3) return false;
			
			if (ickr.limit() == 64) {//get the remaining data
				tep = ickr.getInt(28);
				ickr.limit(64 + tep);
				return true;
			}
			if (this.stringDecode(ickr.getShort(26), ickr.array(), ickr.array(), 64, 64 + ickr.getInt(28), 512, 0, 3) == -1)
				return false;
			ickr.clear();
			ps2 = this.myServerSource.getPreparedStatement_2("Select userID,singleKey,hostID,isLocked,userLevel,loginTimes,lockTime,isLogon,singleLogon,softwareLevel,isDisabled,limitsize,expired,userName,(password(?) = userPassword) as passwordcheck,IFNULL(chineseName,'NO_CHINESENAME_ASIGNED') as chineseName,IFNULL(titleName,'NO_TITLE_DEFINED') as titleName from VUser where userName = ? and CompanyID = ? and AgencyID = ? and DepartmentID = ? and serverID = " +(int) (this.myServerSource.getServerIDL() >>> 32));
			ps2.setInt(3, this.userCompany = ickr.getInt(512 + 4));
			ps2.setInt(4, this.userAgency = ickr.getInt(512));
			ps2.setInt(5, this.userDepartment = ickr.getInt(512 + 8));			
			ps2.setBinaryStream(2, ((InputStream) new ByteArrayInputStream(ickr.array(),512 + 64 + 2,ickr.getShort(512 + 64))),ickr.getShort(512 + 64));
			ps2.setBinaryStream(1, ((InputStream) new ByteArrayInputStream(ickr.array(),512 + 64 + 64 + 2,ickr.getShort(512 + 64 + 64))),ickr.getShort(512 + 64 + 64));
								
			sKRS = ps2.executeQuery();	
			
			sSRead = this.myServerSource.getStatement();
			if (sKRS.next()){ 
				if (!sKRS.getBoolean("passwordcheck"))
				{
					sSRead.execute("Update TUserInformation set loginTimes = loginTimes + 1 where userID = " + this.userID);
					sSRead.execute("Update TUserInformation set isLocked = TRUE,lockTime = " + new Date().getTime() + " where loginTimes > 3 and userID = " + this.userID);
					this.userID = -1;
					this.userLevel = 100;				
					ickr.limit(64);
					ickr.putShort(20, (short) 0);//this is the function number
					ickr.put(22, (byte) 2);//this is the command number
					ickr.put(23, (byte) 2);
					ickr.put(24, (byte) 0);	
					ickr.put(25, (byte) 0);	
					ickr.putInt(28, 0);				
					ickr.position(0);						
					this.sendingData(channel);
					return true;	
				}
				
				this.userLevel = (short)sKRS.getInt("userLevel");
				this.userName = new String(sKRS.getBytes("userName"),"UTF-8");
				
				if (this.userLevel != 1){//can not disable the super admin
					if (sKRS.getBoolean("isDisabled"))
					{
						this.userID = -1;
						this.userLevel = 100;
						errors++;						
						ickr.limit(64);
						ickr.putShort(20, (short) 0);//this is the function number
						ickr.put(22, (byte) 2);//this is the command number
						ickr.put(23, (byte) 5);
						ickr.put(24, (byte) 0);	
						ickr.put(25, (byte) 0);	
						ickr.putInt(28, 0);				
						ickr.position(0);						
						this.sendingData(channel);
						setStatus(clientStatus.isDISCONNECTED);
						return true;
					}
					
					if (sKRS.getLong("expired") != 0L)
					{
						if (sKRS.getLong("expired") - new Date().getTime() <=0) {
							sSRead.execute("Update TUserInformation set isDisabled = true where userID = " + sKRS.getInt("userID"));							
							this.userID = -1;
							this.userLevel = 100;
							errors++;
							ickr.limit(64);
							ickr.putShort(20, (short) 0);//this is the function number
							ickr.put(22, (byte) 2);//this is the command number
							ickr.put(23, (byte) 5);
							ickr.put(24, (byte) 0);	
							ickr.put(25, (byte) 0);	
							ickr.putInt(28, 0);				
							ickr.position(0);						
							this.sendingData(channel);
							setStatus(clientStatus.isDISCONNECTED);
							return true;
						}
					}						
				}
				//multi logon disabled
				if (sKRS.getBoolean("isLogon") && sKRS.getBoolean("singleLogon")){
					this.userID = -1;
					this.userLevel = 100;
					errors++;
					ickr.limit(64);
					ickr.putShort(20, (short) 0);//this is the function number
					ickr.put(22, (byte) 2);//this is the command number
					ickr.put(23, (byte) 1);
					ickr.put(24, (byte) 0);	
					ickr.put(25, (byte) 0);	
					ickr.putInt(28, 0);				
					ickr.position(0);						
					this.sendingData(channel);
					setStatus(clientStatus.isDISCONNECTED);
					return true;
				}
				
				if (sKRS.getBoolean("isLocked")) {
					if (new Date().getTime() - sKRS.getLong("lockTime") >= this.myServerSource.getUserLockTimes())
					{
						sSRead.execute("Update TUserInformation set isLocked = false,loginTimes = 0 where userID = " + sKRS.getInt("userID"));
					}else
					{
						this.userID = -1;
						this.userLevel = 100;
						errors++;
						ickr.limit(64);
						ickr.putShort(20, (short) 0);//this is the function number
						ickr.put(22, (byte) 2);//this is the command number
						ickr.put(23, (byte) 4);
						ickr.put(24, (byte) 0);	
						ickr.put(25, (byte) 0);	
						ickr.putInt(28, 0);				
						ickr.position(0);						
						this.sendingData(channel);
						setStatus(clientStatus.isDISCONNECTED);
						return true;
					}
				}
				
				this.userID = sKRS.getLong("userID");				
				this.fileLimitSize = 1024 * 1024 * 1024;//1G,file limits on the server
				this.fileLimitSize = this.fileLimitSize * sKRS.getShort("limitsize");
				
				System.arraycopy(sKRS.getBytes("softwareLevel"), 0, this.levelControl, 0, 1024); 	
				if (this.userLevel < 21) {
					for (int k = 0;k < 1024;k++)
						this.levelControl[k] = 49;
				}
				
				if (this.userLevel > 50)
					this.levelControl[9] = 48;				
				
				tsp = sKRS.getBinaryStream("chineseName").read(ickr.array(), 96 + 2, 1024);
				ickr.putShort(96, (short) tsp);
				tep = sKRS.getBinaryStream("titleName").read(ickr.array(), 96 + 2 + tsp + 2, 1024);
				ickr.putShort(96 + 2 + tsp, (short) tep);
				
				if (sKRS.getBoolean("singleKey")){//do not allowed to logon from the other computer for this user,THostofUser store the host to user info,if the user allowed to logon from mult
					sKRS = sSRead.executeQuery("Select hostID from THostofUser where userID = " + this.userID + " and serverID = " + (int)(this.myServerSource.getServerIDL() >>> 32));
					if (!sKRS.next())//if this is the first time to logon
					{						
						sSRead.execute("insert into THostofUser(hostID,userID,serverID,serverofHost) values(" + this.hostID + "," + this.userID + "," + (int)(this.myServerSource.getServerIDL() >>> 32) + "," + (int)(this.myServerSource.getServerIDL() >>> 32) + ")");
					}else {
						if ((sKRS.getLong(1) ^ this.hostID) != 0) {							
							this.userID = -1;
							this.userLevel = 100;
							errors++;
							ickr.limit(64);
							ickr.putShort(20, (short) 0);//this is the function number
							ickr.put(22, (byte) 2);//this is the command number
							ickr.put(23, (byte) 1);
							ickr.put(24, (byte) 0);	
							ickr.put(25, (byte) 0);	
							ickr.putInt(28, 0);				
							ickr.position(0);						
							this.sendingData(channel);
							setStatus(clientStatus.isDISCONNECTED);
							return true;	
						}
					}
				}					
				
				sSRead.clearBatch();
				sSRead.addBatch("Update TUserInformation set isLogon = TRUE,loginTimes = 0,hostID = " + this.hostID + " where userID = " + this.userID);
				sSRead.addBatch("Insert into TLoginInfo(userID,hostID) values(" + this.userID + "," + this.hostID + ")");
				sSRead.addBatch("Update THostInformation set isLogon = TRUE,IPAddress = '" + this.scIPAddress + "',userName = '" + this.userName + "',AgencyID = '" + this.userAgency + "',CompanyID = '" + this.userCompany + "',DepartmentID ='" + this.userDepartment + "' where hostID = " + this.hostID);
				sSRead.executeBatch();
				sSRead.clearBatch();
				
				System.arraycopy(randomCode(32), 0, ickr.array(), 64, 32);
				if (this.stringEncode(ickr.array(), ickr.array(), 96, 64 + tsp + tep + 2 + 2 + 32, 96, 64 + tsp + tep + 2 + 2 + 32, 2) == -1) throw new Exception("Encrption error");
				
				ickr.limit(64 + tsp + tep + 2 + 2 + 32);
				ickr.putLong(0, this.hostID);
				ickr.putShort(20, (short) 0);//this is the function number
				ickr.put(22, (byte) 2);//this is the command number
				ickr.put(23, (byte) 0);
				ickr.put(24, (byte) 1);
				ickr.put(25, (byte) 2);
				ickr.putShort(26, (short) (64 & 0x1ff));				
				ickr.putInt(28, tsp + tep + 2 + 2 + 32);				
				ickr.position(0);
				
				if (!this.myServerSource.offerActiveQueue(this)) {					
					return false;
				}
				
				this.setStatus(clientStatus.isREADYFORINCHANNEL);
				if (this.mySource == null)
					this.mySource = this.myServerSource.getClientSource(this);
				if (!this.sendingData(channel)) {
					if (!this.sendingData(channel)) {
						return false;
					}
				}	
				
				//System.out.println("login:" + this.toString());		
			}else
			{
				if (this.myServerSource.getServerRunningModel(ServerStatus.isNew)) {//this is a new server
					sKRS = sSRead.executeQuery("Select userID from TUserInformation limit 1");
					if (!sKRS.next()) {	
						sSRead.clearBatch();
						sSRead.addBatch("delete from TUserInformation");
						sSRead.addBatch("alter table TUserInformation auto_increment = 1");
						sSRead.addBatch("LOCK TABLES `TUserInformation` WRITE");
						sSRead.executeBatch();
						
						ps2.close();
						
						ps2 = myServerSource.getPreparedStatement_2("insert into TUserInformation (userName,userPassword,serverID,isEdit,isNew) values(?,?," + (int) (this.myServerSource.getServerIDL() >>> 32) + ",false,false)");
						ps2.setBinaryStream(1, ((InputStream) new ByteArrayInputStream(ickr.array(),512 + 64 + 2,ickr.getShort(512 + 64))),ickr.getShort(512 + 64));
						ps2.setBinaryStream(2, ((InputStream) new ByteArrayInputStream(ickr.array(),512 + 64 + 64 + 2,ickr.getShort(512 + 64 + 64))),ickr.getShort(512 + 64 + 64));
						ps2.execute();						
						
						sSRead.clearBatch();				
						sSRead.addBatch("Update TUserInformation set userLevel = 1,userID = rowID + " + this.myServerSource.getServerIDL() + ",userPassword = password(userPassword) where rowID = 1");
						sSRead.addBatch("UNLOCK TABLES");
						sSRead.executeBatch();
						
						sKRS = sSRead.executeQuery("Select userName from TUserInformation where rowID = 1");
						if (sKRS.next()) {
							this.userName = new String(sKRS.getBytes(1),"UTF-8");
						}else {
							//why here
							throw new Exception("Create Super Admin ERROR");
						}
						
						this.userID = 1 + this.myServerSource.getServerIDL();
						this.userLevel = 1;
						
						sSRead.clearBatch();
						sSRead.addBatch("delete from TOrgInformation");
						sSRead.addBatch("alter table TOrgInformation auto_increment = 1");
						sSRead.addBatch("LOCK TABLES `TOrgInformation` WRITE");
						sSRead.executeBatch();
						
						ps2.close();
						
						ps2 = myServerSource.getPreparedStatement_2("Insert into TOrgInformation(orgName,orgType,aCode,pCode,isEdit,editorID,Owners) values(?,'A',1,1,false,1,1)");
						ps2.setBinaryStream(1, ((InputStream) new ByteArrayInputStream(ickr.array(),512 + 64 + 64 + 64 + 2,tsp = ickr.getShort(512 + 64 + 64 + 64))),ickr.getShort(512 + 64 + 64 + 64));
						ps2.execute();
						
						ps2.close();
						
						ps2 = myServerSource.getPreparedStatement_2("Insert into TOrgInformation(orgName,orgType,aCode,pCode,isEdit,editorID,Owners) values(?,'C',1,1,false,1,1)");
						ps2.setBinaryStream(1, ((InputStream) new ByteArrayInputStream(ickr.array(),512 + 64 + 64 + 64 + 2 + tsp + 2,tep = ickr.getShort(512 + 64 + 64 + 64 + 2 + tsp))),ickr.getShort(512 + 64 + 64 + 64 + 2 + tsp));
						ps2.execute();
						
						ps2.close();
						
						ps2 = myServerSource.getPreparedStatement_2("Insert into TOrgInformation(orgName,orgType,aCode,pCode,isEdit,editorID,Owners) values(?,'D',1,2,false,1,1)");
						ps2.setBinaryStream(1, ((InputStream) new ByteArrayInputStream(ickr.array(),512 + 64 + 64 + 64 + 2 + tsp + 2 + tep + 2,ickr.getShort(512 + 64 + 64 + 64 + 2 + tsp + 2 + tep))),ickr.getShort(512 + 64 + 64 + 64 + 2 + tsp + 2 + tep));
						ps2.execute();							
						
						sSRead.clearBatch();				
						
						sSRead.addBatch("Update TOrgInformation set idCode = rowID");
						sSRead.addBatch("UNLOCK TABLES");
						sSRead.executeBatch();
						
						this.userAgency = 1;
						this.userCompany = 2;
						this.userDepartment = 3;
						
						sSRead.clearBatch();
						sSRead.addBatch("delete from TGroup");
						sSRead.addBatch("alter table TGroup auto_increment = 1");
						sSRead.addBatch("LOCK TABLES `TGroup` WRITE");
						sSRead.executeBatch();
						
						ps2.close();
						
						ps2 = myServerSource.getPreparedStatement_2("Insert into TGroup(userID,DepartmentID,CompanyID,AgencyID,userName,userLevel,titleID,isEdit,editorID,Owners,ServerID) values(?,?,?,?,?,?,1,false,1,1,?)");    					
						ps2.setLong(1, userID);
						ps2.setInt(2, userDepartment);
						ps2.setInt(3, userCompany);
						ps2.setInt(4, userAgency);
						ps2.setBinaryStream(5, ((InputStream) new ByteArrayInputStream(this.userName.getBytes("UTF-8"),0,this.userName.getBytes("UTF-8").length)),this.userName.getBytes("UTF-8").length);
						ps2.setShort(6, userLevel);
						ps2.setInt(7, (int) (this.myServerSource.getServerIDL() >>> 32));
						ps2.execute();
						
						sSRead.clearBatch();
						sSRead.addBatch("UNLOCK TABLES");
						sSRead.executeBatch();
						
						ickr.limit(64);
						ickr.putShort(20, (short) 0);//this is the function number
						ickr.put(22, (byte) 2);//this is the command number
						ickr.put(23, (byte) 10);
						ickr.put(24, (byte) 0);	
						ickr.put(25, (byte) 0);	
						ickr.putInt(28, 0);				
						ickr.position(0);						
						this.sendingData(channel);
						this.myServerSource.setServerStatus(ServerStatus.isTest);
						this.myServerSource.UpdateORGbyte();
					}				
				}else {
					ickr.limit(64);
					ickr.putShort(20, (short) 0);//this is the function number
					ickr.put(22, (byte) 2);//this is the command number
					ickr.put(23, (byte) 7);
					ickr.put(24, (byte) 0);	
					ickr.put(25, (byte) 0);	
					ickr.putInt(28, 0);				
					ickr.position(0);						
					this.sendingData(channel);
				}
			}			
			return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			this.setStatus(clientStatus.isDISCONNECTED);	
		}finally {
			try {
				try {
					ps1.close();					
				}catch(Exception er) {}
				try {					
					ps2.close();
				}catch(Exception er) {}
				try {	
					sSRead.close();					
				}catch(Exception er) {}
				try {					
					sSUpdate.close();					
				}catch(Exception er) {}
				try {				
					sKRS.close();
				}catch(Exception er) {}
				ps1 = null;
				ps2 = null;				
				sSRead = null;
				sSUpdate = null;
				sKRS = null;
			}catch(Exception er) {}
		}
		return false;
	}
	private ioBuffer copyDataTOIO(byte[] sbyte,ioBuffer dsio,int datalen,int seclevel) {
		try {
			switch (seclevel) {
			case 1:				
			case 2:
				if (datalen <= dsio.getHeaderLimit() - dsio.getHeaderPosition(64)) {
					System.arraycopy(sbyte, 0, dsio.getHeaderByteBuffer(), dsio.getHeaderPosition(64), datalen);
				}else if (datalen < dsio.getBodyLimit() - dsio.getBodyPosition(0)) {
					System.arraycopy(sbyte, 0, dsio.getBodyByteBuffer(), dsio.getBodyPosition(0), datalen);
				}
				break;
			case 3:
				
				break;
			}
		}catch(Exception er) {}
		return dsio;
	}
	//close the channel
	protected void closeChannel(SocketChannel sch,SelectionKey skey) {
		try{					
			try {
				if (sch != null) {
					if (!sch.socket().isInputShutdown()) {
						sch.socket().shutdownInput();
					}
					
					if (!sch.socket().isOutputShutdown()) {
						sch.socket().shutdownOutput();
					}
					
					if (!sch.socket().isClosed()) {
						sch.socket().close();
					}
					
					if (sch != null)
						sch.close();
				}									
			} catch (Exception e) {//if pconn is null									
			}
			try {							
				sch = null;							
			} catch (Exception e) {//if pconn is null									
			}	
			try{skey.attach(null);}catch(Exception er){}				
			try{((SocketChannel) skey.channel()).close();}catch(Exception er){}				
		}catch(Exception er){}
	}
	
	private int decodeIO(ioBuffer io) {
		try {
			switch (io.getHeaderByte(24)) {//0 no more data,1 in the header,2 in the body
			case (byte) 0:
				return 0;				
			case (byte) 1://the data is in the header
				try {
					switch (io.getHeaderByte(25)) {//encryption level
					case (byte) 0:
						return io.getHeaderInt(28);
					case (byte) 1://the data is in the header					
					case (byte) 2://the data is in the body
						try {
							return stringDecode(io.getHeaderShort(26), io.getHeaderByteBuffer(), io.getHeaderByteBuffer(), io.getHeaderPosition(64), io.getHeaderPosition(64 + io.getHeaderInt(28)), io.getHeaderPosition(64), io.getHeaderLimit(), io.getHeaderByte(25)) == -1 ? -1 :io.getHeaderInt(28);
						}catch(Exception er) {}
						break;
					case (byte) 3://the data is in the body
						try {
							int len = stringDecode(io.getHeaderShort(26), io.getHeaderByteBuffer(), io.getHeaderByteBuffer(), io.getHeaderPosition(64), io.getHeaderPosition(64 + io.getHeaderInt(28)), io.getHeaderPosition(this.myServerSource.getIO_Header_buffer_3_1()), io.getHeaderLimit(), io.getHeaderByte(25));
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
						return io.getHeaderInt(28);						
					case (byte) 1://the data is in the header					
					case (byte) 2://the data is in the body
						try {
							return stringDecode(io.getHeaderShort(26), io.getBodyByteBuffer(), io.getBodyByteBuffer(), io.getBodyPosition(0), io.getBodyPosition(io.getHeaderInt(28)), io.getBodyPosition(0), io.getBodyLimit(), io.getHeaderByte(25));
						}catch(Exception er) {}
						break;
					case (byte) 3://the data is in the body
						try {
							int len = stringDecode(io.getHeaderShort(26), io.getBodyByteBuffer(), io.getBodyByteBuffer(), io.getBodyPosition(0), io.getBodyPosition(io.getHeaderInt(28)), io.getBodyPosition(this.myServerSource.getIO_Body_buffer_3_1()), io.getBodyLimit(), io.getHeaderByte(25));
							return len;
						}catch(Exception er) {}
						break;
					}
				}catch(Exception er) {}
				break;
			}
			return -1;	
			
		}catch(Exception er) {}
		return -1;
	}
	
	//check the decodeKey,if the user download the client software,so it should be has a decodekey,if no ,then it is a attack
	protected boolean dealingDecodeKey() {
		try {
			sSRead = this.myServerSource.getStatement();
			sKRS = sSRead.executeQuery("Select keyID,securityLocker,hostID,disabled,secKey2,decodeKey,isLocked,lockTime,loginTimes from TSecurityKey where hostID = " + this.hostID + " and MACAddress = '" + this.scMacAddress + "' and decodeKey2 = " + ickr.getLong(32));
			if (sKRS.next()) {
				if (sKRS.getBoolean(4)) {	
					ickr.limit(64);
					ickr.putShort(20, (short) 1);//this is the function number
					ickr.put(22, (byte) 1);//this is the command number
					ickr.put(23, (byte) 1);
					ickr.put(24, (byte) 0);	
					ickr.put(25, (byte) 0);	
					ickr.putInt(28, 0);				
					ickr.position(0);	
	    			return false;
	    		}
	    		if (sKRS.getBoolean("isLocked")){
					if (new Date().getTime() - sKRS.getLong("lockTime") >= this.myServerSource.getUserLockTimes())
					{
						sSRead.execute("Update securityKey set isLocked = FALSE,loginTimes = 0 where keyID = " + sKRS.getInt(1));
					}else
					{								
						ickr.limit(64);
						ickr.putShort(20, (short) 1);//this is the function number
						ickr.put(22, (byte) 1);//this is the command number
						ickr.put(23, (byte) 2);
						ickr.put(24, (byte) 0);	
						ickr.put(25, (byte) 0);	
						ickr.putInt(28, 0);				
						ickr.position(0);	
		    			return false;
					}
				}
	    		
	    		if (sKRS.getBytes(6) == null) {
	    			ickr.limit(64);
					ickr.putShort(20, (short) 1);//this is the function number
					ickr.put(22, (byte) 1);//this is the command number
					ickr.put(23, (byte) 3);//no decodekey is found
					ickr.put(24, (byte) 0);	
					ickr.put(25, (byte) 0);	
					ickr.putInt(28, 0);				
					ickr.position(0);	
	    		}else {
		    		tsp = sKRS.getBinaryStream("decodeKey").read(ickr.array(), 512, 1024);	    		
					if ((tsp = this.stringEncode(ickr.array(), ickr.array(), 512, 512 + 64, 64, 1024, 3)) == -1)
					{
						this.myStatus = clientStatus.isDEALINGERROR;
						return false;
					}			
					
					tep = sKRS.getBinaryStream(5).read(secKey2, 0, 64);	    	
					tep = sKRS.getBinaryStream(2).read(secKey1, 0, 512);	
									
		    		this.keyID = sKRS.getInt(1);
					
					ickr.limit(64 + tsp);	
					
					ickr.putShort(20, (short) 1);//this is the function number
					ickr.put(22, (byte) 1);//this is the command number
					ickr.put(23, (byte) 0);//no decodekey is found
					ickr.put(24, (byte) 1);
					ickr.put(25, (byte) 3);
					ickr.putShort(26, (short) (512 & 0x1ff));//(short) ((io.headerpostion + iobuffer_3_1) & 0x1ff));//key offset				
					ickr.putInt(28, tsp);				
					ickr.position(0);
	    		}	    		
			}else {
				ickr.limit(64);
				ickr.putShort(20, (short) 1);//this is the function number
				ickr.put(22, (byte) 1);//this is the command number
				ickr.put(23, (byte) 3);//no decodekey is found
				ickr.put(24, (byte) 0);	
				ickr.put(25, (byte) 0);	
				ickr.putInt(28, 0);				
				ickr.position(0);	    			
			}
			tsp = 0;
			tep = 0;
			return true;
		}catch(Exception er) {
			printError(er);
		}finally {
			try {				
				sKRS.close();
			}catch(Exception er) {}
			try {	
				sSRead.close();					
			}catch(Exception er) {}	
		}
		return false;
	}
	
	/*
	 * datainfo 0 - no data,1 - data in the io header,2 - data in the body
	 * default: datainfo(1),from 64 bytes;datainfo(2) from 0 bytes
	 */
	private void dealingIO(ioBuffer io,int commnumber,int funnumber,int datalen,int datainfo,int encryptionlevel,int errorcode) {
		try {
			io.putHeaderShort(20, (short) funnumber);
			io.putHeaderByte(22, (byte) commnumber);//function 0 - 127
			io.putHeaderByte(23, (byte) errorcode);	
			io.putHeaderByte(24, (byte) datainfo);
			io.putHeaderByte(25, (byte) encryptionlevel);
			switch (datainfo) {
			case 0:
				io.clearBody(0, 0);
				io.clearHeader(0, 64);				
				break;
			case 1:
				io.clearBody(0, 0);				
				switch (encryptionlevel) {
				case 0:
					io.clearHeader(0, 64 + datalen);
					break;
				case 1:
				case 2:
					if (io.getHeaderPosition(datalen + 64) > io.getHeaderLimit()) throw new Exception("Data is too long");
					if (stringEncode(io.getHeaderByteBuffer(),io.getHeaderByteBuffer(),io.getHeaderPosition(64),io.getHeaderPosition(64 + datalen),io.getHeaderPosition(64),io.getHeaderLimit(),encryptionlevel) == -1) throw new Exception("encryption error");
					io.putHeaderShort(26, (short) (io.getHeaderPosition(64) & 0x1ff));//key offset
					io.putHeaderInt(28, datalen);
					io.clearHeader(0, 64 + datalen);
					break;
				case 3:
					if (io.getHeaderPosition(this.myServerSource.getIO_Header_buffer_3_1() + datalen) > io.getHeaderLimit()) throw new Exception("Data is too long");
					if ((commnumber = stringEncode(io.getHeaderByteBuffer(),io.getHeaderByteBuffer(),io.getHeaderPosition(this.myServerSource.getIO_Header_buffer_3_1()),io.getHeaderPosition(this.myServerSource.getIO_Header_buffer_3_1() + datalen),io.getHeaderPosition(64),io.getHeaderLimit(),3)) == -1) throw new Exception("encryption error");
					io.putHeaderShort(26, (short) (io.getHeaderPosition(this.myServerSource.getIO_Header_buffer_3_1()) & 0x1ff));//key offset
					io.putHeaderInt(28, commnumber);//the data len	
					io.clearHeader(0, 64 + commnumber);
					break;
					default:
						throw new Exception("Encryption Level ERROR");
				}
				break;
			case 2:
				io.clearHeader(0, 64);				
				switch (encryptionlevel) {
				case 0:
					io.clearBody(0, datalen);
					io.putHeaderInt(28, datalen);
					break;
				case 1:
				case 2:
					if (io.getBodyPosition(datalen) > io.getBodyLimit()) throw new Exception("Data is too long");
					if (stringEncode(io.getBodyByteBuffer(),io.getBodyByteBuffer(),io.getBodyPosition(0),io.getBodyPosition(datalen),io.getBodyPosition(0),io.getBodyLimit(),encryptionlevel) == -1) throw new Exception("encryption error");
					io.putHeaderShort(26, (short) ((io.getBodyPosition(0)) & 0x1ff));//key offset
					io.putHeaderInt(28, datalen);
					io.clearBody(0, datalen);
					break;
				case 3:
					if (io.getBodyPosition(this.myServerSource.getIO_Body_buffer_3_1() + datalen) > io.getHeaderLimit()) throw new Exception("Data is too long");
					if ((commnumber = stringEncode(io.getBodyByteBuffer(),io.getBodyByteBuffer(),io.getBodyPosition(this.myServerSource.getIO_Body_buffer_3_1()),io.getBodyPosition(this.myServerSource.getIO_Body_buffer_3_1() + datalen),io.getBodyPosition(0),io.getBodyLimit(),encryptionlevel)) == -1) throw new Exception("encryption error");
					io.putHeaderShort(26, (short) ((io.getBodyPosition(this.myServerSource.getIO_Body_buffer_3_1())) & 0x1ff));//key offset
					io.putHeaderInt(28, commnumber);//the data len	
					io.clearBody(0, commnumber);
					break;
				}
				break;
			}
			io.setIOStatus(iostatus.isSending);			
		}catch(Exception er) {
			this.addIOTOpool(io);
			this.printError(er);			
		}finally {
			try {
				if (io.getStatus(iostatus.isSending)) {
					if (!this.myServerSource.addSendingQueue(this, false, io))
						this.addIOTOpool(io);
				}
			}catch(Exception er) {}
		}
	}
	
	//orgInfo 
	protected boolean dealingOrgInfo() {
		try {//each time we send 128bytes only
			if (myServerSource.getOrgInfo().length > tep + 256 - 64) 
				tep += 256 - 64;
			else
				tep = myServerSource.getOrgInfo().length;
			
			System.arraycopy(myServerSource.getOrgInfo(), tsp, ickr.array(), 64, tep - tsp);
			if (this.stringEncode(ickr.array(), ickr.array(), 64, tep - tsp + 64, 64, tep - tsp + 64, 2) == -1)
			{
				this.myStatus = clientStatus.isDEALINGERROR;
				return false;
			}			
			
			ickr.limit(64 + tep - tsp);	
			
			ickr.putShort(20, (short) 2);//this is the function number
			ickr.put(22, (byte) 1);//this is the command number
			ickr.put(24, (byte) 1);
			ickr.put(25, (byte) 2);
			ickr.putShort(26, (short) (64 & 0x1ff));
			if (tsp == 0) {
				ickr.putLong(40, myServerSource.getOrgInfo().length);
				ickr.putLong(48, 0);					
			}else {
				ickr.putLong(48, 1);
			}
			ickr.putInt(28, tep - tsp);				
			ickr.position(0);
			tsp = tep;
			if (tep == myServerSource.getOrgInfo().length) {
				this.myStatus = clientStatus.isCHECKINGUSERLOGIN;
			}
			return true;
		} catch (Exception e) {//if pconn is null									
		}
		return false;
	}
	//drop the channel input
	protected void DropChannelInput(SocketChannel sch) {
		try {
			if (!sch.socket().isInputShutdown()) {
				sch.socket().shutdownInput();
			}									
		} catch (Exception e) {//if pconn is null									
		}
	}
	protected ioBuffer getReadIO() {
		return this.readQueue.peek();
	}
	
	//get the hostid
	protected long getHostID() {
		return this.hostID;
	}
	//get client mac address
	protected long getMACAddress() {
		return this.macAddressL_0;
	}
	
	protected ClientInstanceBody getNext() {
		return this.next;
	}
	
	//get the byte[]
	protected ByteBuffer getICByteBuffere() {
		return this.ickr;
	}
	//get the status
	protected clientStatus getClientStatus() {
		return this.myStatus;
	}
	
	protected boolean getClientStatus(clientStatus cs) {
		return this.myStatus == cs;
	}

	protected SocketChannel getOutChannel() {
		return this.scOut;
	}
	
	protected SocketChannel getInChannel() {
		return this.scIn;
	}

	protected clientSource getClientSource() {
		return this.mySource;
	}
	
	protected int getTsp() {
		return this.tsp;
	}
	
	//
	protected boolean isThreadStopped() {
		return this.myStatus == clientStatus.isDISCONNECTED || this.myStatus == clientStatus.isTHREADSTOPPED;
	}
	//get the login status
	protected boolean isLogin() {
		return this.getClientStatus(clientStatus.isLOGIN);
	}
	
	protected void printError(Exception err) {
		if (this.myServerSource.getServerRunningModel(ServerStatus.isTest))
			try {
				System.out.println("isTest mode is on--------");
				ByteArrayOutputStream ba = new ByteArrayOutputStream();
				PrintStream ps = new PrintStream(ba);
				err.printStackTrace(ps);
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
	//release source
	protected void releaseClientSource() {
		try{
			
		}catch(Exception er){}
	}
	
	//read the temp key
	protected boolean readekey(SocketChannel sc) {
		try {
			if (sc.read(ickr) == -1) 
				throw new IOException("disconnected");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			this.setStatus(clientStatus.isDISCONNECTED);
			return false;
		}
		return ickr.hasRemaining();
	}
	
	protected int readIOHeader(ioBuffer io) throws IOException {
		return this.scIn.read(io.getHeader());
	}
	
	protected int readIOBody(ioBuffer io) throws IOException {
		return this.scIn.read(io.getBody());
	}
	
	protected void setNext(ClientInstanceBody n) {
		this.next = n;
	}	
	
	protected void removeReadIO() {
		this.readQueue.poll();
	}
	
	//set the potion and limit to the ickr
	protected boolean setByteBufferpl(int sp,int lt,int stsp) {
		try {
			ickr.limit(lt);
			ickr.position(sp);
			tsp += stsp;
			return true;
		}catch(Exception er) {}
		return false;
	}	
	
	//send data to the client
	protected boolean sendingData(SocketChannel outps) {
		try {				
			outps.write(ickr);
			if (ickr.hasRemaining()) {
				outps.write(ickr);
				if (ickr.hasRemaining()) {					
					return false;
				}	
			}				
			return true;
		}catch(Exception er) {}
		tsp = -1;
		return false;
	}
	
	//clear the bytebuffer status,so when the client is disconnected
	protected synchronized void setStatus(clientStatus cs) {	
		try {
			if (this.myStatus == cs) return;		
			switch (cs) {
			case isLOGIN:		
				break;
			case isINTERACTIONCHECK:
				break;
			case isCHECKINGLOGINDATE:
				break;
			case isSLEEPING:
				break;
			case isDISCONNECTED:	
			case isTHREADSTOPPED:			
				try {
					this.StopIt();					
					closeChannel(this.scOut,skeyOut);
					closeChannel(this.scIn,skeyIn);				
				}catch(Exception er) {}
						
				try {	
					if (this.getClientStatus(clientStatus.isLOGIN)) {
						sSRead = mySource.getStatement();
						sSRead.clearBatch();
						sSRead.addBatch("Update userInformation set isLogon = FALSE where userID = " + this.userID);
						sSRead.addBatch("Update hostInformation set isLogon = FALSE where hostID = " + this.hostID);
						sSRead.addBatch("Insert into loginInfo(userID,hostID,logon) values(" + this.userID + "," + this.hostID + ",false)");
						sSRead.addBatch("Delete from TLoginInfo where hostID = " + this.hostID);
						sSRead.executeBatch();
					}
				}catch(Exception er) {}
				finally {
					try {
						sKRS.close();				
					}catch(Exception er) {}
					try {
						sSRead.close();				
					}catch(Exception er) {}
					try {				
						sSUpdate.close();
					}catch(Exception er) {}
					try {				
						ps1.close();
					}catch(Exception er) {}
					try {				
						ps2.close();
					}catch(Exception er) {}
					try {				
						ps3.close();
					}catch(Exception er) {}				
				}
				
				try {				
					this.mySource.clear();
					this.mySource.addIOTOServerSource();
					rio = null;
				}catch(Exception er) {}	
				
				try {				
					this.mySource = null;
					this.readQueue = null;
					this.commandQueue = null;
					this.outQueue = null;
				}catch(Exception er) {}	
				
				break;
			case isWAITINGINIT:
				break;
			default:
				break;			
			}
		}catch(Exception er) {}
		finally {
			this.myStatus = cs;			
		}
	}	
	//will check the user last time login is the same with loginDate
	protected void setLoginCheck(long lt) {
		this.loginDate = lt;
		myStatus = clientStatus.isCHECKINGLOGINDATE;
	}
		
	protected void setInChannel(SocketChannel sc)
	{			
		try{				
			this.scIn = sc;
			ickr.limit(136);
			System.arraycopy(this.myServerSource.getInteractionKey(), 64, ickr.array(), 64 + 8, 64);
			ickr.putShort(20, (short) 0);//this is the function number
			ickr.put(22, (byte) 3);//this is the command number
			ickr.put(23, (byte) 0);
			ickr.put(24, (byte) 1);	
			
			ickr.putLong(64, this.myServerSource.getInteractionKeyDate());
			
			if (this.stringEncode(ickr.array(), ickr.array(), 64, 64 + 64 + 8, 64, 64 + 64 + 8, 2) == -1) throw new Exception("Encrption error");
			
			ickr.put(25, (byte) 2);
			ickr.putShort(26, (short) (64 & 0x1ff));				
			ickr.putInt(28, 64 + 8);
			
			ickr.position(0);				
			this.readQueue = this.mySource.getReadQueue();
			this.commandQueue = this.mySource.getCommandQueue();
			this.outQueue = this.mySource.getOutQueue();
			cfp.put((byte) 3, (short) 0);
			this.sendingData(this.scOut);			
		}catch(Exception e){}
	}		
	/*
	 * if level less than 3,then bstr must be use bodyOutB
	 * if level equal 3 then bstr must not be bodyOutB
	 */
	protected int stringEncode(byte[] sourceByte,byte[] destinationByte,int sp,int ep,int dpostion,int dlimit,int level){//security the information,value of sLayer(true for 2 times,false for 1 tiems)
		try{				
			switch (level) {
			case 1:
				for (;sp < ep;sp++){//sp is not a constant var,different sp will have different result
					destinationByte[sp] ^= secKey1[(sp) & 0x1FF]; // % 512						
				}
				return 0;					
			case 2:
				for (;sp < ep;sp++){//sp is not a constant var,different sp will have different result
					destinationByte[sp] ^= secKey1[(sp) & 0x1FF]; // % 512
					destinationByte[sp] ^= secKey2[(sp) & 0x3F];//64
				}
				return 0;					
			case 3:
				dlimit =  dpostion;
				for (;sp < ep;sp++){//sp is not a constant var,different sp will have different result
					sourceByte[sp] ^= secKey1[(sp) & 0x1FF]; // % 512
					sourceByte[sp] ^= secKey2[(sp) & 0x3F];
					dpostion += (((secKey3[(sp) & 0x1F]) & 0x1) > 0 ? 1 : -((secKey3[(sp) & 0x1F]) & 0x1));
					destinationByte[dpostion++] = (byte)(sourceByte[sp] ^ secKey3[sp & 0x1F]);
				}					
				return dpostion - dlimit;					
			}
			return -1;
		}catch(Exception e){}
		return -1;
	}
	
	/*
	 * beg is the startposition in the security key 
	 */
	protected int stringDecode(int beg,byte[] sourceByte,byte[] destinationByte,int sp,int ep,int dpostion,int dlimit,int level){//security the information,value of sLayer(true for 2 times,false for 1 tiems)
		try{
			switch (level) {
			case 1:					
				for (;sp < ep;sp++){//XOR 
					destinationByte[sp] ^= secKey1[(beg++) & 0x1FF];
				}
				return 0;
			case 2:
				for (;sp < ep;sp++){//XOR 
					destinationByte[sp] ^= secKey2[(beg) & 0x3F];
					destinationByte[sp] ^= secKey1[(beg++) & 0x1FF];
				}
				return 0;
			case 3:
				dlimit = dpostion;
				for (;sp < ep;sp++){//XOR 
					sp += (((secKey3[(beg) & 0x1F]) & 0x1) > 0 ? 1 : -((secKey3[(beg) & 0x1F]) & 0x1));
					destinationByte[dpostion] =(byte)(sourceByte[sp] ^ secKey3[beg & 0x1F]);
					destinationByte[dpostion] ^= secKey2[(beg) & 0x3F];
					destinationByte[dpostion++] ^= secKey1[(beg++) & 0x1FF];
				}
				return dpostion - dlimit;
				
			}
			return -1;				
		}catch(Exception e){
			return -1;
		}			
	}
}

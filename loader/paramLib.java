package com.enals.loader;

import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

import com.enals.publiclib.LanguagePack.languageType;

public class paramLib {	
	private String userName,chineseName,titleName,departmentName;
	
	private SocketChannel scIn,scOut;
	private Selector selector_scIn;	
	
	private byte[] levelControl;	
	private byte[] secKey1,secKey2,secKey3;
	
	private long serverID= 0L,hostID = 0L,userID = 0L;//
	private int bufSize = -1;
	private int osType = -1;	
	private int iobufferlen = 14;
	private int iobuffer_3_1 = 0;
	private int iobody_3_1 = 0;	
	private int clientMemory = 0;
	private int inPort = 8189;
	private int outPort = 8181;
	
	private Charset cset = Charset.forName("UTF-8");
	
	private languageType lt;
	
	public void destory() {
		userName = "";
		chineseName = "";
		titleName = "";
		departmentName = "";
		
		scIn = null;
		scOut = null;
		selector_scIn = null;	
		
		for (int i = 0;i < this.levelControl.length;i++)
		{
			levelControl[i] = 0;
			try {
				secKey1[i] = 0;
			}catch(Exception er) {}
			try {
				secKey2[i] = 0;
			}catch(Exception er) {}
			try {
				secKey3[i] = 0;
			}catch(Exception er) {}
		}
		
		serverID = 0L;
		hostID = 0L;
		userID = 0L;
		bufSize = -1;
		osType = -1;	
		iobufferlen = 14;
		iobuffer_3_1 = 0;
		iobody_3_1 = 0;	
		clientMemory = 0;
		inPort = 8189;
		outPort = 8181;
	}
	
	public languageType getLanguageType() {
		return this.lt;
	}
	
	public long getUserID() {
		return this.userID;
	}
	
	public int getIn_Port() {
		return this.inPort;
	}
		
	public int getOut_Port() {
		return this.outPort;
	}
	
	public byte[] getSecKey(int index) {
		switch (index) {
		case 1:
			return this.secKey1;
		case 2:
			return this.secKey2;
		case 3:
			return this.secKey3;
			default:
				return null;
		}
		
	}
	
	public Charset getCharset() {
		return this.cset;
	}
	
	public long getServerID() {
		return this.serverID;
	}
	
	public long getHostID() {
		return this.hostID;
	}
	
	public int getBufferSize() {
		return this.bufSize;
	}
	
	public int getIO_Header_Buffer_Length() {
		return this.iobufferlen;
	}
	
	public int getIO_Header_3_1_Position() {
		return this.iobuffer_3_1;
	}
	
	public int getIO_Body_Buffer_Length() {
		return this.clientMemory;
	}
	
	public int getIO_Body_3_1_Position() {
		return this.iobody_3_1;
	}
	
	public SocketChannel get_scIn_Channel() {
		return this.scIn;
	}
	
	public SocketChannel get_scOut_Channel() {
		return this.scOut;
	}
	
	public Selector get_scIn_Selector() {
		return this.selector_scIn;
	}
	
	public int getOSType() {
		return this.osType;
	}
	
	public String getUsername() {
		return this.userName;
	}
	
	public String getUserTitle() {
		return this.titleName;
	}
	
	public String getDepartment() {
		return this.departmentName;
	}
	
	public String getChineseName() {
		return this.chineseName;
	}
	
	public byte[] getLevelControl() {
		return this.levelControl;
	}
	
	public void setOSType(int os) {
		this.osType = os;
	}
	
	public void setUserName(String st) {
		this.userName = st;
	}
	
	public void setUserTitle(String st) {
		this.titleName = st;
	}
	
	public void setDepartmentName(String st) {
		this.departmentName = st;
	}
	
	public void setChineseName(String st) {
		this.chineseName = st;
	}
	
	public void setLevelControl(byte[] bt) {
		this.levelControl = bt.clone();
	}
	
	public void set_scIn_Channel(SocketChannel sc) {
		this.scIn = sc;
	}
	
	public void set_scOut_Channel(SocketChannel sc) {
		this.scOut = sc;
	}
	
	public void set_scIn_Selector(Selector st) {
		this.selector_scIn = st;
	}
	
	public void setSecKey(byte[] bb,int index) {
		switch (index) {
		case 1:
			this.secKey1 = bb.clone();
			return;
		case 2:
			this.secKey2 = bb.clone();
			return;
		case 3:
			this.secKey3 = bb.clone();
			return;
			
		}
	}
	
	public void setServerID(long sd) {
		this.serverID = sd;		
	}
	
	public void setHostID(long hid) {
		this.hostID = hid;
	}
	
	public void setBufferSize(int bs) {
		this.bufSize = bs;
	}
	
	public void setIO_Header_Buffer_Length(int bl) {
		this.iobufferlen = bl;
	}
	
	public void setIO_Header_3_1_Position(int i3) {
		this.iobuffer_3_1 = i3;
	}
	
	public void setIO_Body_Buffer_Length(int cm) {
		this.clientMemory = cm;
	}
	
	public void setIO_Body_3_1_Position(int bm) {
		this.iobody_3_1 = bm;
	}
	
	public void setCharset(Charset cs) {
		this.cset = cs;
	}
	
	public void setIn_Port(int ip) {
		this.inPort = ip;
	}
	
	public void setOut_Port(int op) {
		this.outPort = op;
	}
	
	public void setLanguageType(languageType lt) {
		this.lt = lt;
	}
	
	public void setUserID(long ud) {
		this.userID = ud;
	}
}

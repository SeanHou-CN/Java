package com.enals.server;

import java.nio.ByteBuffer;

import com.enals.server.DPortServer.iostatus;

final class ioBuffer{
	private final int headerpostion;
	private final int headerlimit;
	private final int len;
	private int bodypostion,bodylimit,bodylen;
	protected int readThisTime = -1;	
	long ioage;
	private ByteBuffer header;
	private final byte[] bB;
	private ByteBuffer body;
	private BodyBuffer bodymaster;
	private ClientInstanceBody master = null;
	protected Exception e = null;
	private iostatus status = iostatus.isIdel;
	private final boolean isHeader;
	private ioBuffer next,prev;//this will be a circle 
	
	protected ioBuffer(int hp,int len,byte[] bb,boolean ish,ioBuffer next){
		this.headerpostion = hp;
		this.isHeader = ish;
		this.len = len;
		this.headerlimit = hp + len;//actually we put 128 bytes here
		this.bB = bb;
		header = ByteBuffer.wrap(bB,hp,len);
		header.limit(headerlimit);
		this.next = next;
	}
	
	protected void initBody(BodyBuffer body) {
		this.bodymaster = body;
		this.bodypostion = body.getStartPosition();
		this.bodylen = body.getLength();
		this.bodylimit = this.bodypostion + this.bodylen;
		this.body = ByteBuffer.wrap(body.getByteBuffer(), this.bodypostion, this.bodylen);
	}
	
	protected boolean isHeader() {
		return this.isHeader;
	}
	
	protected boolean isIOStatus(iostatus is) {
		return this.status == is;
	}
	
	protected boolean isHeaderHasRemaining() {
		return this.header.hasRemaining();
	}
	
	protected boolean isBodyHasRemaining() {		
		return this.body == null ? false : this.body.hasRemaining();
	}
	
	protected void clearHeader(int sp,int limit) {
		try {
			this.header.limit(this.headerpostion + (limit < len ? (limit == -1 ? len : limit) : len));
			this.header.position(this.headerpostion + sp);	//exception if sp > limit
		}catch(Exception er) {}			
	}
	
	protected void clearBody(int sp,int limit) {
		try {
			this.body.limit(this.bodypostion + (limit < bodylen ? (limit == -1 ? len : limit) : bodylen));
			this.body.position(this.bodypostion + sp);		//exception if sp > limit
		}catch(Exception er) {}			
	}
	
	protected void putHeaderByte(int sp,byte val){
		this.header.put(headerpostion + sp, val);//exception if sp > limit
	}
	
	protected void putHeaderShort(int sp,short val) {
		this.header.putShort(headerpostion + sp, val);//exception if sp > limit
	}
	
	protected void putHeaderInt(int sp,int val) {		
		this.header.putInt(this.headerpostion + sp, val);//exception if sp > limit
	}
	
	protected void putHeaderLong(int sp,long val) {		
		this.header.putLong(this.headerpostion + sp, val);//exception if sp > limit
	}
	
	protected void putHeaderByte(byte val){
		this.header.put(val);//exception if sp > limit
	}
	
	protected void putHeaderShort(short val) {
		this.header.putShort(val);//exception if sp > limit
	}
	
	protected void putHeaderInt(int val) {		
		this.header.putInt(val);//exception if sp > limit
	}
	
	protected void putHeaderLong(long val) {		
		this.header.putLong(val);//exception if sp > limit
	}
	
	protected byte getHeaderByte(int postion) {
		return this.header.get(this.headerpostion + postion);//exception if sp > limit
	}
	
	protected short getHeaderShort(int postion) {
		return this.header.getShort(headerpostion + postion);//exception if sp > limit
	}
	
	protected int getHeaderInt(int postion) {
		return this.header.getInt(headerpostion + postion);//exception if sp > limit
	}
	
	protected long getHeaderLong(int postion) {
		return this.header.getLong(headerpostion + postion); //exception if sp > limit
	}
	
	protected byte getHeaderByte() {
		return this.header.get();//exception if sp > limit
	}
	
	protected short getHeaderShort() {
		return this.header.getShort();//exception if sp > limit
	}
	
	protected int getHeaderInt() {
		return this.header.getInt();//exception if sp > limit
	}
	
	protected long getHeaderLong() {
		return this.header.getLong(); //exception if sp > limit
	}
	
	//body 
	protected void putBodyByte(int sp,byte val){
		this.body.put(headerpostion + sp, val);//exception if sp > limit
	}
	
	protected void putBodyShort(int sp,short val) {
		this.body.putShort(headerpostion + sp, val);//exception if sp > limit
	}
	
	protected void putBodyInt(int sp,int val) {		
		this.body.putInt(this.headerpostion + sp, val);//exception if sp > limit
	}
	
	protected void putBodyLong(int sp,long val) {		
		this.body.putLong(this.headerpostion + sp, val);//exception if sp > limit
	}
	
	protected void putBodyByte(byte val){
		this.body.put(val);//exception if sp > limit
	}
	
	protected void putBodyShort(short val) {
		this.body.putShort(val);//exception if sp > limit
	}
	
	protected void putBodyInt(int val) {		
		this.body.putInt(val);//exception if sp > limit
	}
	
	protected void putBodyLong(long val) {		
		this.body.putLong(val);//exception if sp > limit
	}
	
	protected byte getBodyByte(int postion) {
		return this.body.get(this.headerpostion + postion);//exception if sp > limit
	}
	
	protected short getBodyShort(int postion) {
		return this.body.getShort(headerpostion + postion);//exception if sp > limit
	}
	
	protected int getBodyInt(int postion) {
		return this.body.getInt(headerpostion + postion);//exception if sp > limit
	}
	
	protected long getBodyLong(int postion) {
		return this.body.getLong(headerpostion + postion); //exception if sp > limit
	}
	
	protected byte getBodyByte() {
		return this.body.get();//exception if sp > limit
	}
	
	protected short getBodyShort() {
		return this.body.getShort();//exception if sp > limit
	}
	
	protected int getBodyInt() {
		return this.body.getInt();//exception if sp > limit
	}
	
	protected long getBodyLong() {
		return this.body.getLong(); //exception if sp > limit
	}
	
	/*
	 *  return the buffer's start position
	 */
	protected int getHeaderPosition(int val) {
		return this.headerpostion + val;
	}	
	
	protected int getHeaderLimit() {
		return this.headerlimit;
	}
	
	protected void setHeaderLimit(int nlimit) {
		this.header.limit(this.headerpostion + (nlimit <= this.headerlimit ? nlimit : this.headerlimit));
	}
	
	protected int getBodyPosition(int val) {
		return this.bodypostion + val;
	}
	
	protected int getBodyLimit() {
		return this.bodylimit;
	}
	
	protected void setBodyLimit(int nlimit) {
		this.body.limit(this.bodypostion + (nlimit <= this.bodylimit ? nlimit : this.bodylimit));
	}
	
	protected int getHeaderCurrentPositionLen() {
		return this.header.position() - this.headerpostion;
	}	
	
	protected int getBodyCurrentPositionLen() {
		return this.body.position() - this.bodypostion;
	}
	
	protected void setHeaderPosition(int val) {
		this.header.position(this.headerpostion + val);
	}
	
	protected void setBodyPosition(int val) {
		this.body.position(this.bodypostion + val);
	}
	
	protected ByteBuffer getHeader() {
		return this.header;
	}
	
	protected ByteBuffer getBody() {
		return this.body;
	}
	
	/*
	 *  return the background byte[] 
	 */
	protected byte[] getHeaderByteBuffer() {
		return this.bB;
	}
	
	protected byte[] getBodyByteBuffer() {
		return this.bodymaster.getByteBuffer();
	}
	
	protected ClientInstanceBody getMaster() {
		return this.master;
	}
	
	protected ioBuffer getIONext() {
		return this.next;
	}
	
	protected ioBuffer getIOPrev() {
		return this.prev;
	}
	
	protected boolean getStatus(iostatus is) {
		return this.status == is;
	}
	
	protected void setPrev(ioBuffer nt) {
		this.prev = nt;
	}
	
	protected void setNext(ioBuffer nt) {
		this.next = nt;
	}
	
	protected void setMaster(ClientInstanceBody cb) {
		this.master = cb;
	}
	
	protected void setIOStatus(iostatus is) {
		this.status = is;
	}	
}	

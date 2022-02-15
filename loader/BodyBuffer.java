package com.enals.loader;

public final class BodyBuffer{
	protected final int sP,bLength,bLimit,indexNo;
	protected BodyBuffer next;
	protected final boolean isHeader;//this is the first buffer
	protected final byte[] bB;//the background bytebuffer
	protected ioBuffer master = null;
	protected boolean masterIsSet = false;
	
	public BodyBuffer(int sp,int len,int index,BodyBuffer bq,byte[] bb,boolean ish){
		sP = sp;
		bLength = len;
		bLimit = sp + len;
		indexNo = index;
		next = bq;
		bB = bb;
		isHeader = ish;
	}	
	/*
	 *  return the buffer's start position
	 */
	protected int getStartPosition() {
		return this.sP;
	}
	/*
	 *  return the buffer's length
	 */
	protected int getLength() {
		return this.bLength;
	}
	/*
	 *  return the background byte[] 
	 */
	protected byte[] getByteBuffer() {
		return this.bB;
	}
}

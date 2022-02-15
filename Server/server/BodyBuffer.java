package com.enals.server;

final class BodyBuffer{
	private final int sP,bLength;
	private final byte[] bB;//the background bytebuffer		
	protected BodyBuffer(int sp,int len,int index,BodyBuffer bq,byte[] bb,boolean ish){
		sP = sp;
		bLength = len;
		bB = bb;
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

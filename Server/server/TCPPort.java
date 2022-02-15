package com.enals.server;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

import javax.swing.JOptionPane;

//init the TCP Port
final class TCPPort{
	private Selector OutPortSelector,InPortSelector;
	private int OUT_PORT_NUMBER;
	private int IN_PORT_NUMBER;
	//init the selector
	protected TCPPort(int OUT_PORT,int IN_PORT) {
		this.OUT_PORT_NUMBER = OUT_PORT;
		this.IN_PORT_NUMBER = IN_PORT;
	}
	
	protected Selector getOutPortSelector() {
		return this.OutPortSelector;
	}
	
	protected Selector getInPortSelector() {
		return this.InPortSelector;
	}
	
	protected Selector InitTCPPort(int port) {
		try{   
			Selector selCtrl=null;
    		ServerSocketChannel channelCtrl=null;
    		ServerSocket serverCtrl=null;			      
	        try{
		        channelCtrl = ServerSocketChannel.open();		        
		        serverCtrl = channelCtrl.socket();		        
		        // Set the port the server channel will listen to  
		        serverCtrl.bind(new InetSocketAddress(port));
		        // Set nonblocking mode for the listening socket  
		        channelCtrl.configureBlocking(false);
		        selCtrl = Selector.open();			       
		        // Register the ServerSocketChannel with the Selector  
		        channelCtrl.register(selCtrl, SelectionKey.OP_ACCEPT);
		        if (this.OUT_PORT_NUMBER == port)
		        	return OutPortSelector = selCtrl;		
		        else if (this.IN_PORT_NUMBER == port)
		        	return InPortSelector = selCtrl;
	        }catch(Exception e){	        	
	        	JOptionPane.showMessageDialog(null,"Open ServerSocketChannel Error!!!");
	        }
	}catch(Exception dd) {
    		dd.printStackTrace();
    	}
		return null;
	}		
}

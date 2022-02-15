package com.enals.loader;

import java.awt.Graphics;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

public class oMJPanelWithImage extends JPanel{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected ImageIcon thisicon;
	public int iconwidth,iconheight;
	public oMJPanelWithImage(ImageIcon i){
		super();
		this.thisicon = i;
		iconwidth = i.getIconWidth();
		iconheight = i.getIconHeight();
		setSize(iconwidth,iconheight);		
	}

	@Override
	protected void paintComponent(Graphics g) { //rewrite the paintComponent
		super.paintComponent(g);
		Image img = thisicon.getImage();
		g.drawImage(img, 0, 0,getParent());
	}
}

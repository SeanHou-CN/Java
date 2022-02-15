package com.enals.loader;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class oMJGBCLayout extends GridBagConstraints  
{  
   /**
	 * 
	 */
   private static final long serialVersionUID = 3247778237543257112L;

   //location(row,column) 
   public oMJGBCLayout(int row, int column)  
   {  
      this.gridx = column;  
      this.gridy = row;  
   }  
  
   //location(row,column),rows = gridheight,columns = gridwidth
   public oMJGBCLayout(int row, int column, int gridheight, int gridwidth)  
   {  
      this.gridx = column;  
      this.gridy = row;  
      this.gridwidth = gridwidth;  
      this.gridheight = gridheight;  
   } 
  
   //set the row height in pixels
   public oMJGBCLayout setRowHeight(GridBagLayout GBL,int rowindex,int rheight) {
	   if (GBL.rowHeights == null) {
		   GBL.rowHeights = new int[rowindex + 1];
	   }else if (GBL.rowHeights.length < rowindex + 1)
	   {
		   int[] bi = GBL.rowHeights.clone();
		   GBL.rowHeights = new int[rowindex + 1];
		   for (int i = 0;i < bi.length;i++)
		   {
			   GBL.rowHeights[i] = bi[i];
		   }
	   }
		   
	   GBL.rowHeights[rowindex] = rheight;
	   return this;
   }
   
   //set the row height in pixels
   public oMJGBCLayout setRowHeight(GridBagLayout GBL,int[] rheight) {
	   GBL.rowHeights = rheight;
	   return this;
   }
   
   //set the column widths in pixels
   public oMJGBCLayout setColumnWidth(GridBagLayout GBL,int columnindex,int cwidth) {
	   if (GBL.columnWidths == null) {
		   GBL.columnWidths = new int[columnindex + 1];
	   }else if (GBL.columnWidths.length < columnindex + 1) {
		   int[] bi = GBL.columnWidths.clone();
		   GBL.columnWidths = new int[columnindex + 1];
		   for (int i = 0;i < bi.length;i++) {
			   GBL.columnWidths[i] = bi[i];
		   }
	   }
	   
	   GBL.columnWidths[columnindex] = cwidth;	  
	   return this;
   }
   
   //set the column widths in pixels
   public oMJGBCLayout setColumnWidth(GridBagLayout GBL,int[] cwidth) {
	   GBL.columnWidths = cwidth;
	   return this;
   }
   
   //the direction of the component
   public oMJGBCLayout setAnchor(int anchor)  
   {  
      this.anchor = anchor;  
      return this;  
   }  
  
   //center,south,north,east,west,or BOTH
   public oMJGBCLayout setFill(int fill)  
   {  
      this.fill = fill;  
      return this;  
   }  
   // 
   public oMJGBCLayout setIpad(int ipadx, int ipady)  
   {  
      this.ipadx = ipadx;  
      this.ipady = ipady;  
      return this;  
   } 
   //
   public oMJGBCLayout setWeight(double weightx, double weighty)  
   {  
      this.weightx = weightx;  
      this.weighty = weighty;  
      return this;  
   }  
  
   public oMJGBCLayout setColumnWeights(GridBagLayout GBL,double[] columnweights) {
	   GBL.columnWeights = columnweights;
	   return this;
   }
   
   public oMJGBCLayout setRowWeights(GridBagLayout GBL,double[] rowweights) {
	   GBL.rowWeights = rowweights;
	   return this;
   }
   
   //fill the space between the component and the cell  
   public oMJGBCLayout setInsets(int distance)  
   {  
      this.insets = new Insets(distance, distance, distance, distance);  
      return this;  
   }  
  
   //fill the space between the component and the cell  
   public oMJGBCLayout setInsets(int top, int left, int bottom, int right)  
   {  
      this.insets = new Insets(top, left, bottom, right);  
      return this;  
   }
} 

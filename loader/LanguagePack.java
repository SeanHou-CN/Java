package com.enals.loader;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.enals.publiclib.LanguagePack.languageType;

public class LanguagePack {
	final ArrayList<JLabel> oMJLabel = new ArrayList<JLabel>();
	final ArrayList<JComboBox<String>> oMJComboBox = new ArrayList<JComboBox<String>>();
	final ArrayList<JTextField> oMJTextField = new ArrayList<JTextField>();
	final ArrayList<JButton> oMJButton = new ArrayList<JButton>();
	
	private static languageType language;
	private Loader loader;
	
	protected LanguagePack(Loader ld)
	{
		this.loader = ld;
	}
		
	public enum iostatus{//io status
		isReading,
		isDealing,
		isSending,
		isIdel, ReadingDone,
	}
	
	public languageType getlanguageType() {
		return this.language;
	}
	
	public boolean setLanguage(languageType index) {
		try {
			for (languageType lt:languageType.values()) {
				if (lt == index)
				{
					this.language = lt;
					return true;
				}
			}
			return false;
		}catch(Exception er) {
			return false;
		}	
	}
	
	public boolean setLanguage(int index) {
		try {
			for (languageType lt:languageType.values()) {
				if (lt.getIndex() == index)
				{
					this.language = lt;
					return true;
				}
			}
			return false;
		}catch(Exception er) {
			return false;
		}	
	}
	
	public boolean setLanguage(String sname) {
		try {
			for (languageType lt:languageType.values()) {
				if (lt.getLanguageName().equals(sname))
				{
					this.language = lt;
					return true;
				}
			}
			return false;
		}catch(Exception er) {
			return false;
		}
	}
	
	public enum systemStatus{
		EXITSYSTEM("",0),
		DECODEERROR("",1),
		DECODKEYEERROR("",2),
		USERDISABLED("",3),
		USERLOCKED("",4),
		UNKNOWNEERROR("",5),
		DISABLEDLOGON("",6),
		WRONGPASSWORD("",7),
		UNKNOWUSER("",8),	
		SERVERERROR("",9),	
		SERVERDATAERROR("",10),
		PASSWORDNOMATCH("",11),
		DATEFORMATERROR("",12),
		USERNAMENULL("",13),
		SERVERDISCONNECTED("",14),
		TRYCONNECTINGSERVER("",15),
		CONFIGFILENOTFOUND("",16),
		SENDDATAERRORTOSERVER("",17),
		SERVERCONNECTED("",18),
		ANENCYISNULL("",19),
		COMPANYISNULL("",20),
		DEPARTMENTISNULL("",21),
		NEWSERVERDETECTED("",22),
		NEWUSERISADDED("",23),
		SERVERIPREQUEST("",24),
		CHECKSERVERIP("",25),
		SERVERIPERROR("",26),
		;
		
		private String languageName;
		private int index;
		private systemStatus(String s,int i) {
			this.languageName = s;
			this.index = i;
		}
		
		public String getLanguageName() {
			switch (language) {
			case CHINESE_BIG5HK:
				break;
			case CHINESE_BIG5TW:
				break;
			case CHINESE_GB32:
				switch (index) {
				case 0:
					return "退出系统，确认？";
				case 1:
					return "数据解码错误";
				case 2:
					return "解码钥错误";
				case 3:
					return "用户已禁用";
				case 4:
					return "用户已锁定";
				case 5:
					return "未知错误";
				case 6:
					return "禁止登陆";
				case 7:
					return "密码错误";
				case 8:
					return "未知用户名";
				case 9:
					return "服务器错误";
				case 10:
					return "服务器数据错误";
				case 11:
					return "密码不一致";
				case 12:
					return "日期格式错误";
				case 13:
					return "用户名不能为空";
				case 14:
					return "服务器已经断开";
				case 15:
					return "尝试连接到服务器，请等待......";
				case 16:
					return "没有找到服务器配置文件";
				case 17:
					return "发送数据到服务器错误";
				case 18:
					return "服务器已连接";
				case 19:
					return "公司名称不能为空";
				case 20:
					return "分支名称不能为空";
				case 21:
					return "部门名称不能为空";
				case 22:
					return "检测到新的服务器，请确认您输入的公司信息与您的用户名和密码是否正确（该用户名将成为超级用户）？";
				case 23:
					return "恭喜您注册成功，请退出后重新登陆";
				case 24:
					return "请输入服务器IP地址:";
				case 25:
					return "请稍后正在验证服务器地址......";
				case 26:
					return "服务器地址不正确，或服务器没有相应";
					default:
						return "";
				}				
			case ENGLISH:
				switch (index) {
				
					default:
						return "";
				}				
			default:
				break;			
			}
			return this.languageName;
		}		
		
		public int getIndex() {
	        return index;
	    }
	}
	
	public enum oaKeyEvent{
		keyTyped,
		keyPressed,
		keyReleased,
	}
	
	public enum componentType{
		oMJLabel,	
		oMJButtonLabel,		
		oMJButton,
		oMJComboBox,
		oMJCheckBox,
		oMJMidBar,
		oMJRadioButton,
		oMJTextField,
		oMJTextArea,
		oMJPanel,
		oMJPopupMenu,
		oMJDialog,
		oMJTextPane,
		oMJDragTextPane,
		oMJTable,
		oMJDocument,		
		oMJOptionString,
		oMJErrorString, 
		oMJTableTitleString,	
		oMJTableColumnWidth,
		oMJTableColumnWeights,
		oMJTableColumnAlignment,
		oMJTableColumnBounds,
		oMJTableRowHeight,
		oMJTableRowWeights,
		oMJTableRowObjects,
		oMJWeekDay,
	}
	
	public enum oaMouseEvent{
		MOUSE_CLICKED,
		MOUSE_IN,
		MOUSE_EXIT,
		MOUSE_PRESS,
		MOUSE_RELEASE,
		MOUSE_DRAG,	
		MOUSE_MOVE,
		MOUSE_WHEEL_MOVE;
	}
	
	public enum OSType{
		WINDOWS,
		APPLE,
		LINUX;
	}
	
	public enum modelName{
		//the top menu		
		LOADER("LOADER",0,0,null,0),		
		HOME("HOME",1,0,null,100),		
		;
		private String languageName;
		private int menuID;
		private int startnumber;
		private modelName parent;
		private int oMJButtonIndex;
		private modelName(String s,int i,int menulevel,modelName pa,int oMJBIndex) {
			this.languageName = s;
			this.menuID = i;
			this.startnumber = menulevel;
			this.parent = pa;
			oMJButtonIndex = oMJBIndex;
		}
		
		public int getStartNumber() {
			return this.startnumber;
		}
		
		public modelName getParent() {
			return this.parent;
		}
		
		protected String getLanguageName() {
			return this.languageName;
		}		
		
		public int getIndex() {
			return this.oMJButtonIndex;
		}
		
		public int getMenuID() {
	        return menuID;
	    }
	}
	
	public URL getImageIconURL(String icn) {
		try {
			//System.out.println(getClass().getResource("/icons/" + icn));
			return getClass().getResource("/icons/" + icn);
		}catch(Exception er)
		{			
			return null;
		}
	}
	/*
	 * init the loader
	 */
	public boolean InitLoader() {
		try {
			if (oMJLabel.size() == 0) {
				ArrayList<JLabel> a1 = oMJLabel;
				a1.add(new JLabel(""));//0Company
				a1.add(new JLabel(""));//1Agency
				a1.add(new JLabel(""));//2Department
				a1.add(new JLabel(""));//3Name
				a1.add(new JLabel(""));//4Password
				a1.add(new JLabel(""));//5Language
				a1.add(new JLabel(""));//6Status bar
				
				ArrayList<JComboBox<String>> c1 = oMJComboBox;
				c1.add(new JComboBox<String>());//0,Company
				c1.add(new JComboBox<String>());//1,Agency
				c1.add(new JComboBox<String>());//2,Department
				c1.add(new JComboBox<String>());//3language
				
				for (int i = 0;i < c1.size();i++) {
					c1.get(i).addActionListener(new ActionListener() {
						
						@Override
						public void actionPerformed(ActionEvent e) {
							// TODO Auto-generated method stub
							loader.oMJActionListener(e.getSource(), componentType.oMJComboBox,e);
						}							
					});				
				}
				
				ArrayList<JTextField> t1 = oMJTextField;
				t1.add(new JTextField());//username
				t1.add(new JPasswordField());//password
				for (int i = 0;i < t1.size();i++)
				{
					t1.get(i).setName(i + "");
					t1.get(i).addKeyListener(new KeyListener() {
	
						@Override
						public void keyTyped(KeyEvent e) {
							// TODO Auto-generated method stub
							loader.oMJKeyListener(oaKeyEvent.keyTyped, componentType.oMJTextField,e);
						}
	
						@Override
						public void keyPressed(KeyEvent e) {
							// TODO Auto-generated method stub
							loader.oMJKeyListener(oaKeyEvent.keyPressed, componentType.oMJTextField,e);
						}
	
						@Override
						public void keyReleased(KeyEvent e) {
							// TODO Auto-generated method stub
							loader.oMJKeyListener(oaKeyEvent.keyReleased, componentType.oMJTextField,e);
						}								
					});						
				}
				ArrayList<JButton> b1 = oMJButton;
				b1.add(new JButton());
				b1.add(new JButton());			
				
				for (int i = 0;i < b1.size();i++) {
					b1.get(i).addActionListener(new ActionListener() {
						
						@Override
						public void actionPerformed(ActionEvent e) {
							// TODO Auto-generated method stub
							loader.oMJActionListener(e.getSource(), componentType.oMJButton,e);
						}							
					});
				}
			}			
			
			switch (language) {			
			case CHINESE_GB32:
				try {
					String[] labelS = new String[]{
							"公司名称：",
							"分支名称：",
							"部门名称：",
							"登陆用户：",
							"登陆密码：",
							"使用语言：",
							"状态：",						
					};
					for (JLabel jl :oMJLabel) {
						jl.setText(labelS[oMJLabel.indexOf(jl)]);
					}	
					
					String[] bS = new String[]{
							"确  定",
							"退  出",											
					};
					for (JButton jl :oMJButton) {
						jl.setText(bS[oMJButton.indexOf(jl)]);
					}					
				}catch(Exception er) {}
				break;
			case CHINESE_BIG5HK:
				break;
			case CHINESE_BIG5TW:
				break;
			case ENGLISH:
				break;
			default:
				break;
			
			}			
			return true;
		}catch(Exception er) {
			return false;
		}
	}
	
	public Map<Integer,String> getLanguageType(){
		try {
			Map<Integer,String> mlt = new HashMap<Integer,String>();
			for (languageType lt:languageType.values()) {
				mlt.put(lt.getIndex(), lt.getLanguageName());
			}
			return mlt;
		}catch(Exception er) {			
			return null;
		}
	}
	
	public Object getoMComponent(componentType ot,int index){
		try{
			switch (ot) {
			case oMJComboBox:
				return this.oMJComboBox.get(index);			
			case oMJLabel:
				return this.oMJLabel.get(index);			
			case oMJTextField:
				return this.oMJTextField.get(index);	
			case oMJButton:
				return this.oMJButton.get(index);
			default:
				break;
			
			}			
		}catch(Exception e)
		{}
		return null;
	}
}

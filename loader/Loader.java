package com.enals.loader;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import com.enals.loader.LanguagePack.OSType;
import com.enals.loader.LanguagePack.componentType;
import com.enals.loader.LanguagePack.oaKeyEvent;
import com.enals.loader.LanguagePack.oaMouseEvent;
import com.enals.loader.LanguagePack.systemStatus;
import com.enals.publiclib.LanguagePack.languageType;

public class Loader extends JFrame{
	private int osType = -1;//标记客户端的操作系统
	private String osString;//存放客户端操作系统名字
	private static String rootPath = "",sFile = "starOA.jar";//记录文件的启动目录，程序主体文件名字starOA.jar
	private String[] serverIP;//服务器地址
	private final int outPort = 8181,inPort = 8189;
	
	private static byte[] skb;
	private static ArrayList<byte[]> ickal = new ArrayList<byte[]>();
	
	private BlockingQueue<ioBuffer> ioQueue = new ArrayBlockingQueue<ioBuffer>(8);
	private ArrayList<ioBuffer> waitingIO = new ArrayList<ioBuffer>();
	private Charset charSet;
	
	//用来设置Agency，company，department列表
	private String companyID,agencyID,departmentID;
	private final ArrayList<String> idName = new ArrayList<String>();
	private final ArrayList<String> pCode = new ArrayList<String>();
	private final ArrayList<String> idCode = new ArrayList<String>();
	private final ArrayList<String[]> titleInfo = new ArrayList<String[]>();
	
	//用来收集电脑配置信息，用于公司电脑资产管理
	private Map<String,Map<String,String>> pcInfo = new HashMap<String,Map<String,String>>();
	private byte[] keyInfo = new byte[4096 * 16];//存储电脑配置信息
	private byte[] kb = null;//存储从服务器获取的信息	
	
	private byte[] iak = new byte[128];	
	private long icskdate = Long.parseLong(new SimpleDateFormat("YYYYMMDD").format(new Date()));

	SocketChannel scIn = null;
	
	private final boolean isTest = true;
	private boolean isRunning = true;
	private TCPServer tpserver;
	private LanguagePack Lpack = null;//语言包
	private languageType languagestr;
	
	//加载主体文件，并运行
	private static URLClassLoader loader;
	private static URL url;
	private static Object logonInstance = null;
	private static Class<?> starOACL;
	
	private long crc32Value = 0L,fileSize = 0L,cpoint = 0L,byteread = 0,bts =System.currentTimeMillis();
	private long fileVersion;
	private int index = 1;
	private paramLib pblib = new paramLib();
	//进度条
	final JProgressBar pg = new JProgressBar();
	
	
	public static void main(String[] args){//执行文件入口
		try{			
			rootPath = System.getProperty("user.dir") + File.separator + "Program" + File.separator;			
			//在文件启动目录下创建一个子文件夹Program，用来保存我们的程序主体文件			
			File starF = new File(rootPath +File.separator + sFile);
			if (starF.exists())
			{				
				FileSystem starFS = null;
				try{
					starFS = FileSystems.newFileSystem(Paths.get(rootPath + File.separator + sFile));
				}catch(Exception e2){
					starFS = null;
				}
				
				if (starFS != null){
					try{
						Files.walkFileTree(starFS.getPath("/"), new SimpleFileVisitor<Path>(){//查找是否有加密key文件spos.dat，如果有则读取内容并保存在数组中
							public FileVisitResult visitFile(Path file,BasicFileAttributes atts) throws IOException
							{
								if (file.endsWith("spos.dat")){
									byte[] fb = Files.readAllBytes(file);
									skb = fb;
									return FileVisitResult.TERMINATE;
								}
								return FileVisitResult.CONTINUE;
							}
						});
						
					}catch(Exception e){
						e.printStackTrace();
					}
				}	
				try{
					starFS.close();
				}catch(Exception er){}				
			}
			
			if (skb == null)
				new Loader(true);//说明主体文件存在
			else
				new Loader(false);//主体文件不存在，我们需要从服务器下载	
		}catch(Exception e){			
			e.printStackTrace();
		}
	}
	public Loader(boolean isgetFile) {
		try{
			//下面检查客户端操作系统的名字，并保存在相应的变量中
			if(System.getProperty("os.name").toLowerCase().startsWith("win")){
				for (OSType t:OSType.values()){
					if (t.name().equals("WINDOWS")){
						this.osType = t.ordinal();
						this.osString = t.name();						
						break;
					}
				}
			}

			if (System.getProperty("os.name").toLowerCase().startsWith("mac")){
				for (OSType t:OSType.values()){
					if (t.name().equals("APPLE")){
						this.osType = t.ordinal();
						this.osString = t.name();
						break;
					}
				}
			}
			
			if (System.getProperty("os.name").toLowerCase().startsWith("linux")){
				for (OSType t:OSType.values()){
					if (t.name().equals("LINUX")){
						this.osType = t.ordinal();
						this.osString = t.name();
						break;
					}
				}
			}
			this.Lpack = new LanguagePack(this);//初始化语言包		
			this.languagestr = languageType.CHINESE_GB32;//设定使用简体中文			
			Lpack.setLanguage(languagestr);//设定语言包使用简体中文
			Lpack.InitLoader();	//初始化Loader中的语言			
			
			pblib.setLanguageType(languagestr);
			
			int LoginWidth=365,LoginHeight=440;//设定登陆界面的宽和高
			
			Toolkit kit= Toolkit.getDefaultToolkit();
			Dimension screenSize=kit.getScreenSize();//获得客户端屏幕信息
			double screenWidth = screenSize.getWidth(); //get the width of the screen
			double screenHeight=screenSize.getHeight();		
			setIconImage(new ImageIcon(getClass().getResource("/icons/StareastEnalsLogin.gif")).getImage()); //set the icon of this window
			setContentPane(new oMJPanelWithImage(new ImageIcon(getClass().getResource("/images/StareastEnalsLogin.png"))));	//设定登陆界面使用的背景文件			
		
			GridBagLayout gbl = new GridBagLayout();//设定布局
			getContentPane().setLayout(gbl);
			//开始读取语言包中的对象，并添加到Loader中，getoMComponent函数在语言包中，componentType是我们定义的枚举类型变量，用来指示要加载的对象类型，同时设定被加载对象的布局
			//Label			
			getContentPane().add((JLabel) Lpack.getoMComponent( componentType.oMJLabel, 0),new oMJGBCLayout(1,1,1,1).setInsets(0, 5, 0, 0).setFill(oMJGBCLayout.WEST));
			getContentPane().add((JLabel) Lpack.getoMComponent( componentType.oMJLabel, 1),new oMJGBCLayout(2,1,1,1).setInsets(0, 5, 0, 0).setFill(oMJGBCLayout.WEST));
			getContentPane().add((JLabel) Lpack.getoMComponent( componentType.oMJLabel, 2),new oMJGBCLayout(3,1,1,1).setInsets(0, 5, 0, 0).setFill(oMJGBCLayout.WEST));
			getContentPane().add((JLabel) Lpack.getoMComponent( componentType.oMJLabel, 3),new oMJGBCLayout(4,1,1,1).setInsets(0, 5, 0, 0).setFill(oMJGBCLayout.WEST));
			getContentPane().add((JLabel) Lpack.getoMComponent( componentType.oMJLabel, 4),new oMJGBCLayout(5,1,1,1).setInsets(0, 5, 0, 0).setFill(oMJGBCLayout.WEST));
			//JComboBox
			getContentPane().add((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 0),new oMJGBCLayout(1,2,1,3).setInsets(0, 5, 0, 0).setFill(oMJGBCLayout.BOTH));
			getContentPane().add((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 1),new oMJGBCLayout(2,2,1,3).setInsets(0, 5, 0, 0).setFill(oMJGBCLayout.BOTH));
			getContentPane().add((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 2),new oMJGBCLayout(3,2,1,3).setInsets(0, 5, 0, 0).setFill(oMJGBCLayout.BOTH));
			//JTextField
			getContentPane().add((JTextField) Lpack.getoMComponent( componentType.oMJTextField, 0),new oMJGBCLayout(4,2,1,3).setInsets(0, 5, 3, 2).setFill(oMJGBCLayout.BOTH));
			getContentPane().add((JTextField) Lpack.getoMComponent( componentType.oMJTextField, 1),new oMJGBCLayout(5,2,1,3).setInsets(0, 5, 3, 2).setFill(oMJGBCLayout.BOTH));
			//JButton			
			getContentPane().add((JButton) Lpack.getoMComponent( componentType.oMJButton, 0),new oMJGBCLayout(6,3,1,1).setInsets(0, 5, 0, 2));
			getContentPane().add((JButton) Lpack.getoMComponent( componentType.oMJButton, 1),new oMJGBCLayout(6,4,1,1).setInsets(0, 5, 0, 6));
						
			getContentPane().add((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 3),new oMJGBCLayout(6,2,1,1).setInsets(0, 4, 0, 0).setFill(oMJGBCLayout.BOTH));			
			getContentPane().add((JLabel) Lpack.getoMComponent( componentType.oMJLabel, 5),new oMJGBCLayout(6,1,1,1).setInsets(0, 5, 0, 0).setColumnWidth(gbl, new int[] {1,70,120,70,70,5}).setRowHeight(gbl, new int[] {235,25,25,25,25,25,25,25}).setRowWeights(gbl, new double[] {0,0,0,0,0,0,0}).setColumnWeights(gbl, new double[] {0,0,100,0,0,0}).setFill(oMJGBCLayout.WEST));
			
			Map<Integer,String> lt = Lpack.getLanguageType();
			Iterator<Integer> it = lt.keySet().iterator();
			
			while (it.hasNext()) {
				((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 3)).addItem(lt.get(it.next()));
			}
			
			//添加状态栏
			final JPanel stats1 = new JPanel();
			getContentPane().add(stats1,new oMJGBCLayout(7,0,1,6).setInsets(0, 0, 0, 0).setFill(oMJGBCLayout.BOTH));			
			stats1.setBounds(0, 402, LoginWidth, 25);
			stats1.setBackground(Color.getHSBColor((float)57 / 100,(float) 20 / 100,(float) 83 / 100));
			stats1.setLayout(null);			
			pg.setBounds(280, 8, 80, 10);
			pg.setMinimum(0);
			pg.setMaximum(100);
			pg.setValue(0);		
			JPanel bb = new JPanel();
			JLabel bstatus = (JLabel) Lpack.getoMComponent( componentType.oMJLabel, 6);
			bstatus.setToolTipText(bstatus.getText());
			stats1.add(bstatus);
			stats1.add(bb);
			bb.setBounds(270, 4, 2, 18);
			bstatus.setBounds(10, 2, 300, 21);
			bstatus.setOpaque(false);
			stats1.add(pg);			
			
			setBounds(((int)screenWidth - LoginWidth)/2, ((int)screenHeight-LoginHeight)/2, 365,464);	
			this.setDefaultCloseOperation(EXIT_ON_CLOSE);
			this.setVisible(true);
			
			bstatus.setText(bstatus.getToolTipText() + systemStatus.TRYCONNECTINGSERVER.getLanguageName());
			
			try {//从本地配置文件中读取服务器地址，请把服务器地址保存在Loader.jar所在文件夹下面，格式为（SERVERIP:192.168.1.50;）
				if (Files.exists(new File(System.getProperty("user.dir") + File.separator + "config.inf").toPath())){
					byte[] fb = Files.readAllBytes(new File(System.getProperty("user.dir") + File.separator + "config.inf").toPath());
					String s = new String(fb);
					ArrayList<String> ipa = new ArrayList<String>();
					//查找服务器地址
					while (s.indexOf("SERVERIP:") >= 0) {
						s = s.substring(s.indexOf("SERVERIP:"));
						s = s.substring(s.indexOf(':') +1);
						ipa.add(s.substring(0, s.indexOf(';')));
					}
					//		
					serverIP = new String[ipa.size()];
					for (int i = 0;i < ipa.size();i++){
						serverIP[i] = ipa.get(i);
					}
					//查找最后一次登陆服务器日期
					s = new String(fb);				
					while (s.indexOf("ICSKEYDATE:") >= 0) {
						s = s.substring(s.indexOf("ICSKEYDATE:"));
						s = s.substring(s.indexOf(':') +1);
						icskdate = Long.parseLong(s.substring(0, s.indexOf(';')));
						break;
					}
					//查找最后一次登陆服务器时的握手密钥，服务器每天更换一次握手密钥（可以根据需要更换，还可以改成每小时更换密钥）
					int i = 0,t = 0;
					while (i < fb.length) {
						t = 0;
						while (fb[i++] == ("ICSKEYDATA:").getBytes("UTF-8")[t++]) {
							if (t == 11) 
								break;
						}
						if (t == ("ICSKEYDATA:").getBytes("UTF-8").length) {
							System.arraycopy(fb, i, iak, 64, 64);
							break;
						}
					}
				}
				if (serverIP == null) {//请用户输入服务器地址，并尝试连接
					JDialog sd = new JDialog(this,systemStatus.SERVERIPREQUEST.getLanguageName(),true);
					Container dc = sd.getContentPane();
					dc.setLayout(null);
					JLabel jl = new JLabel(systemStatus.SERVERIPREQUEST.getLanguageName());
					dc.add(jl);
					JTextField jt = new JTextField();
					dc.add(jt);
					JButton jb = new JButton(((JButton) Lpack.getoMComponent( componentType.oMJButton, 0)).getText());					
					dc.add(jb);
					jt.addKeyListener(new KeyListener() {

						@Override
						public void keyTyped(KeyEvent e) {
							// TODO Auto-generated method stub
							switch (e.getKeyChar()) {
							case '0':
							case '1':
							case '2':
							case '3':
							case '4':
							case '5':
							case '6':
							case '7':
							case '8':
							case '9':
							case '.':
								break;
								default:
									e.consume();
							}
						}

						@Override
						public void keyPressed(KeyEvent e) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void keyReleased(KeyEvent e) {
							// TODO Auto-generated method stub
							
						}						
					});
					
					jb.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							// TODO Auto-generated method stub
							try{	
								sd.setTitle(systemStatus.CHECKSERVERIP.getLanguageName());									
								ByteBuffer rp = ByteBuffer.wrap(new byte[32]);
								try{						
									scIn = SocketChannel.open();	
									if (!scIn.connect(new InetSocketAddress(jt.getText(),inPort)))
									{
										JOptionPane.showMessageDialog(sd, systemStatus.SERVERIPERROR.getLanguageName());
										try {scIn.close();}catch(Exception tt) {}
										return;
									}else {										
										long ct = System.currentTimeMillis();
										rp.limit(4);
										rp.position(0);
										while (rp.hasRemaining()) {
											if (scIn.read(rp) == -1) {
												JOptionPane.showMessageDialog(sd, systemStatus.SERVERDISCONNECTED.getLanguageName());
												try {scIn.close();}catch(Exception tt) {}
												return;
											}
											if (System.currentTimeMillis() - ct > 1000 * 5) {//5 seconds waiting
												JOptionPane.showMessageDialog(sd, systemStatus.SERVERDISCONNECTED.getLanguageName());
												try {scIn.close();}catch(Exception tt) {}
												return;
											}
										}
										
										if (rp.getInt(0) == 827212108) {//如果连接成功，并验证成功
											rp.clear();
											rp.putLong(5278580800269405267L);
											rp.clear();
											scIn.write(rp);
											if (rp.hasRemaining()) {
												scIn.write(rp);
												if (rp.hasRemaining()) {
													JOptionPane.showMessageDialog(sd, systemStatus.SERVERDISCONNECTED.getLanguageName());
													try {scIn.close();}catch(Exception tt) {}
													return;
												}
											}
											sd.setVisible(false);
											RandomAccessFile rac = new RandomAccessFile(System.getProperty("user.dir") + File.separator + "config.inf","rw");
											rac.seek(0);
											rac.writeUTF("SERVERIP:" +jt.getText().trim() + ";");											
											rac.setLength(rac.getFilePointer());
											try {
												rac.getChannel().force(true);
												rac.close();
											}catch(Exception er) {}
										}
									}									
								}catch(Exception er){
									er.printStackTrace();
								}finally {
									
								}
							}catch(Exception er){																	
							}
						}						
					});
					jb.setBounds(110,30,65,20);					
					jl.setBounds(5,5,120,20);
					jt.setBounds(130,5,150,20);
					sd.setBounds(((int)screenWidth - 285)/2, ((int)screenHeight - 80)/2,285,80);
					sd.setVisible(true);
				}
			}catch(Exception er) {
				JOptionPane.showMessageDialog(null, systemStatus.CONFIGFILENOTFOUND.getLanguageName());
			}
			
			if (isgetFile) {
				iak[20] = 0;
				iak[21] = 0;
			}else {
				iak[20] = 0;
				iak[21] = 1;
			}
			
			tpserver = new TCPServer(this.outPort,this.inPort,serverIP,pcInfo,this);
			kb = tpserver.connect(osType,icskdate,iak,scIn,this.pblib);
			pblib.setIn_Port(inPort);//可以随时更改设置
			pblib.setOut_Port(outPort);
			charSet = tpserver.getCharSet();
			tpserver.start();
			tpserver.setlanguagePack(Lpack);
			bstatus.setText(bstatus.getToolTipText() + systemStatus.SERVERCONNECTED.getLanguageName());
			cpoint = 0;
			ioBuffer io = tpserver.getIO(null, this);	//get the orginfo	
			if (skb != null)
				io.putHeaderLong(32, ByteBuffer.wrap(skb).getLong(1024));//decodekey2
			tpserver.sendingIO(io, 1, 1, 0,false);//
			
			this.validate();
			ickal.clear();
			InteractiveWithServer(isgetFile);
		}catch(Exception er){}		
	}
	
	//加载主体文件
	private void loadMainBody(){
		try{			
			if (starOACL == null) {				
				try{
					url = new URL("file:" + rootPath + File.separator + sFile);
					loader = new URLClassLoader(new URL[]{url});
					starOACL = loader.loadClass("com.enals.mainmenu.starOA");				
				}catch(Exception e){
					e.printStackTrace();
				}				
			}				
			
			try{
				tpserver.stopIT();
				while (tpserver.isStopped()) {
					try {
						Thread.sleep(10);
					}catch(Exception er) {}
				}
				//加载主体文件
				starOACL.getDeclaredConstructors()[0].newInstance(pblib);				
			}catch(Exception e){
		    	e.printStackTrace();
			}
			
			pblib = null;
			tpserver.clear();
			kb = null;	
			tpserver = null;			
			Lpack = null;
			starOACL = null;
			loader = null;
			pcInfo = null;
			url = null;
			keyInfo = null;
			this.setDefaultCloseOperation(EXIT_ON_CLOSE);
			this.dispose();
			this.setVisible(false);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	//add IObuffer to queue 
	public void addIO(ioBuffer io) {
		try {
			if (!this.ioQueue.offer(io))
			{
				this.waitingIO.add(io);
			}
		}catch(Exception er) {}		
	}	

	//与服务器进行会话
	private void InteractiveWithServer(boolean isDownloadingFile) {
		try {
			ioBuffer io;
			while (this.isRunning) {
				try {
					io = this.ioQueue.poll(1000, TimeUnit.MILLISECONDS);
					if (io == null) {
						if (this.waitingIO.size() > 0)
						{
							io = this.waitingIO.get(0);
							this.waitingIO.remove(0);
						}
					}
					if (io == null) continue;
					switch (io.getHeaderByte(22)) {
					case (byte) 1:
						try {
							switch (io.getHeaderShort(20)) {
							case (short) 1://this is the request command
								try {
									switch (io.getHeaderByte(23)) {//this is the error code
									case (byte) 0:																										
										int len = 0;
										if ((len = tpserver.decodeIO(io)) == -1) throw new Exception(systemStatus.DECODEERROR.getLanguageName());
										if (len != 64) {
											JOptionPane.showMessageDialog(null, systemStatus.DECODKEYEERROR.getLanguageName());
											System.exit(0);
										}
										byte[] tb = new byte[len];
										System.arraycopy(io.getHeaderByteBuffer(), io.getHeaderPosition(64),tb, 0, 64);
										int dp = 0;		
										byte[] sk1 = new byte[512];
					    				for (int index = 0;index < 512;index++){//XOR, 0x1F (% 32)
					    					dp += (((tb[index & 0x3F]) & 0x1) > 0 ? 1 : -((tb[index & 0x3F]) & 0x1));
					    					sk1[index] = (byte)(skb[dp++] ^ (tb[index & 0x3F]));//store the secKey1. and this is encoded by 64 bytes							    					
					    				}										
					    				tpserver.initSecKey(sk1, 1);
					    				System.arraycopy(pblib.getSecKey(2), 0, tb, 0, 64);
					    				sk1 = String.valueOf(Files.readAttributes(new File(rootPath +File.separator + sFile).toPath(), BasicFileAttributes.class).creationTime().toMillis()).getBytes().clone();
					    				System.arraycopy(sk1, 0, tb, 0, sk1.length);
					    				tpserver.initSecKey(tb, 2);
					    				index = 0;
					    				fileSize = 0L;
					    				io.putHeaderLong(32, 0);
					    				io.putHeaderLong(40, 0);
					    				io.putHeaderLong(48, 0);
					    				tpserver.sendingIO(io, 1, 2, 0,false);
										continue;
									case (byte) 1:
										JOptionPane.showMessageDialog(null, systemStatus.USERDISABLED.getLanguageName());
										System.exit(0);
										break;
									case (byte) 2:
										JOptionPane.showMessageDialog(null, systemStatus.USERLOCKED.getLanguageName());
										System.exit(0);
										break;
									case (byte) 3:
										tpserver.sendingIO(io, 1, 2, 0,false);
										break;
									}		
								}catch(Exception er) {}
								break;
							case (short) 2://this is the organization information
								try {
									switch (io.getHeaderByte(23)) {//this is the error code
									case (byte) 0:
										if (io.getHeaderByte(24) == (byte) 1) {//this is not the last one package
											if (io.getHeaderLong(48) == 0L) {
												fileSize = io.getHeaderLong(40);
											}
											cpoint += io.getHeaderInt(28);
											if ((tpserver.decodeIO(io)) == -1) throw new Exception(systemStatus.DECODEERROR.getLanguageName());
											byte[] tb = new byte[io.getHeaderInt(28)];
											System.arraycopy(io.getHeaderByteBuffer(), io.getHeaderPosition(64), tb, 0, tb.length);
											ickal.add(tb);									
										}
										if (cpoint < fileSize)
											tpserver.sendingIO(io, 1, 2, 0, false);
										else{
											int bytesRead = (int) cpoint;											
											byte[] orgb = new byte[bytesRead];
											bytesRead = 0;
																					
											for (int i =0;i < ickal.size();i++) {
												System.arraycopy(ickal.get(i), 0, orgb, bytesRead, ickal.get(i).length);
												bytesRead += ickal.get(i).length;	
											}
											
											BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(orgb),Charset.forName("utf-8")));
											String st;
											
											while ((st = br.readLine()) != null) {
												if (st.toUpperCase().indexOf("FF5278580800269405267FF") > -1) {
													break;
												}
												//st = new String(st.getBytes("ISO8859-1"),charSet);												
												String name1 = st;												
												st = br.readLine();
												//st = new String(st.getBytes("ISO8859-1"),charSet);
												String name2 = st;
												st = br.readLine();								
												//st = new String(st.getBytes("ISO8859-1"),charSet);
												String name3 = st;
												
												if (name3.indexOf('A') == 0)
												{
													((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 0)).addItem(name1);
													name2 = 'A' + name2;
												}else if (name3.indexOf('C') == 0) {
													name2 = 'A' + name2;
												}else if (name3.indexOf('D') == 0) {
													name2 = 'C' + name2;
												}
												//System.out.println(name3);
												idName.add(name1);//the item name
												pCode.add(name2);//the item's parent's idcode
												idCode.add(name3);//the item's idcode
											}
											
											while ((st = br.readLine()) != null){
												if (st.toUpperCase().indexOf("FF5278580800269405267FF") > -1) {
													break;
												}
												String name1 = st;
												st = br.readLine();	
												String name2 = st;
												st = br.readLine();												
												String name3 = st;												
												titleInfo.add(new String[]{name1,name2,name3});
											}
											
											if (((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 0)).getItemCount() > 0) 
												((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 0)).setSelectedIndex(0);
											else {
												((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 0)).setEditable(true);
												((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 1)).setEditable(true);
												((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 2)).setEditable(true);
											}
											((JTextField) Lpack.getoMComponent( componentType.oMJTextField, 0)).grabFocus();											
										}										
										continue;
									case (byte) 1:
										JOptionPane.showMessageDialog(null, systemStatus.USERDISABLED.getLanguageName());
										System.exit(0);
										break;
									case (byte) 2:
										JOptionPane.showMessageDialog(null, systemStatus.USERLOCKED.getLanguageName());
										System.exit(0);
										break;
									case (byte) 3:
										JOptionPane.showMessageDialog(null, systemStatus.UNKNOWNEERROR.getLanguageName());
										System.exit(0);
										break;
									}		
								}catch(Exception er) {
									er.printStackTrace();
								}
								break;
							case (short) 3:								
								break;
							}
						}catch(Exception er) {
							er.printStackTrace();
						}
						break;
					case (byte) 2:
						try {							
							switch (io.getHeaderShort(20)) {
							case (short) 0://this is the request command
								try {
									switch (io.getHeaderByte(23)) {
									case (byte) 0:
										////System.out.println("ready to load staroa 1");
										int len = 0;
										System.arraycopy(io.getHeaderByteBuffer(), io.getHeaderPosition(64), io.getHeaderByteBuffer(), io.getHeaderPosition(32), 32);
										if ((len = tpserver.decodeIO(io)) == -1) throw new Exception(systemStatus.DECODEERROR.getLanguageName());										
										byte[] tb = new byte[io.getHeaderInt(28)];
										io.clearHeader(0, -1);	
										System.arraycopy(io.getHeaderByteBuffer(), io.getHeaderPosition(96), tb, 0, tb.length - 32);
										pblib.setChineseName(new String(tb,2,ByteBuffer.wrap(tb).getShort(0),"UTF-8"));
										pblib.setUserTitle(new String(tb,2 + ByteBuffer.wrap(tb).getShort(0) + 2, ByteBuffer.wrap(tb).getShort(2 + ByteBuffer.wrap(tb).getShort(0)),"UTF-8"));
										io.clearHeader(0, 64);
										tpserver.initScoutChannel(io);										
										continue;
									case (byte) 1://no rights to get the new client file,this is maybe the hack
										JOptionPane.showMessageDialog(null, systemStatus.DISABLEDLOGON.getLanguageName());
										((JButton) Lpack.getoMComponent( componentType.oMJButton, 0)).setEnabled(true);	
										((JTextField) Lpack.getoMComponent( componentType.oMJTextField, 0)).grabFocus();
										continue;
									case (byte) 2://user is locked,download it later
										JOptionPane.showMessageDialog(null, systemStatus.WRONGPASSWORD.getLanguageName());
										((JButton) Lpack.getoMComponent( componentType.oMJButton, 0)).setEnabled(true);	
										((JTextField) Lpack.getoMComponent( componentType.oMJTextField, 0)).grabFocus();
										continue;
									case (byte) 3://could not find this hostid in the server DB,should check it with the ADMIN
										JOptionPane.showMessageDialog(null, systemStatus.UNKNOWNEERROR.getLanguageName());
										((JButton) Lpack.getoMComponent( componentType.oMJButton, 0)).setEnabled(true);
										((JTextField) Lpack.getoMComponent( componentType.oMJTextField, 0)).grabFocus();
										continue;	
									case (byte) 4://could not find this hostid in the server DB,should check it with the ADMIN
										JOptionPane.showMessageDialog(null, systemStatus.USERLOCKED.getLanguageName());
										((JButton) Lpack.getoMComponent( componentType.oMJButton, 0)).setEnabled(true);	
										((JTextField) Lpack.getoMComponent( componentType.oMJTextField, 0)).grabFocus();
										continue;
									case (byte) 5://could not find this hostid in the server DB,should check it with the ADMIN
										JOptionPane.showMessageDialog(null, systemStatus.USERDISABLED.getLanguageName());
										((JButton) Lpack.getoMComponent( componentType.oMJButton, 0)).setEnabled(true);	
										((JTextField) Lpack.getoMComponent( componentType.oMJTextField, 0)).grabFocus();
										continue;	
									case (byte) 6://could not find this hostid in the server DB,should check it with the ADMIN
										JOptionPane.showMessageDialog(null, systemStatus.DISABLEDLOGON.getLanguageName());
										((JButton) Lpack.getoMComponent( componentType.oMJButton, 0)).setEnabled(true);
										((JTextField) Lpack.getoMComponent( componentType.oMJTextField, 0)).grabFocus();
										continue;	
									case (byte)7://could not find this hostid in the server DB,should check it with the ADMIN
										JOptionPane.showMessageDialog(null, systemStatus.UNKNOWUSER.getLanguageName());
										((JButton) Lpack.getoMComponent( componentType.oMJButton, 0)).setEnabled(true);
										((JTextField) Lpack.getoMComponent( componentType.oMJTextField, 0)).grabFocus();
										continue;
									case (byte)10:
										JOptionPane.showMessageDialog(null, systemStatus.NEWUSERISADDED.getLanguageName());
										System.exit(0);	
									}		
								}catch(Exception er) {
									((JButton) Lpack.getoMComponent( componentType.oMJButton, 0)).setEnabled(true);
								}
								finally {									
								}
								break;													
							}
						}catch(Exception er) {}
						finally {							
						}
						break;
					case (byte) 3:
						switch (io.getHeaderShort(20)) {
						case (short) 0:	
							switch (io.getHeaderByte(23)) {
							case 0:
								if ((tpserver.decodeIO(io)) == -1) throw new Exception(systemStatus.DECODEERROR.getLanguageName());
								this.icskdate = io.getHeaderLong(64);
								System.arraycopy(io.getHeaderByteBuffer(),io.getHeaderPosition(64 + 8),iak, 64, 64);
								try {//从本地配置文件中读取服务器地址，请把服务器地址保存在Loader.jar所在文件夹下面，格式为（SERVERIP:192.168.1.50;）
									if (Files.exists(new File(System.getProperty("user.dir") + File.separator + "config.inf").toPath())){
										byte[] fb = Files.readAllBytes(new File(System.getProperty("user.dir") + File.separator + "config.inf").toPath());
										//查找最后一次登陆服务器时的握手密钥，服务器每天更换一次握手密钥（可以根据需要更换，还可以改成每小时更换密钥）
										int i = 0,t = 0;
										boolean isf = false;
										while (!isf && i < fb.length) {
											t = 0;
											while (fb[i++] == ("ICSKEYDATE:").getBytes("UTF-8")[t++]) {
												if (t == 11) 
												{
													isf = true;
													break;
												}
											}											
										}
										RandomAccessFile rac = new RandomAccessFile(System.getProperty("user.dir") + File.separator + "config.inf","rw");
										rac.seek(0);
										rac.write(fb, 0, isf ? i - 11 : fb.length);										
										rac.write(("ICSKEYDATE:" + String.valueOf(icskdate) + ";" + '\r' + "ICSKEYDATA:").getBytes("UTF-8"));										
										rac.write(iak, 64, 64);
										rac.setLength(rac.getFilePointer());
										try {
											rac.getChannel().force(true);
											rac.close();
										}catch(Exception er) {}										
									}else {//why here
										throw new Exception("NOT FOUND CONFIG");
									}
								}catch(Exception er) {
									JOptionPane.showMessageDialog(null, systemStatus.CONFIGFILENOTFOUND.getLanguageName());
								}
								
								if (skb != null) {									
									io.putHeaderLong(32, ByteBuffer.wrap(skb).getLong(1024 + 8));//1024 - 1031 decodekey2,1032 - 1039 fileversion
									tpserver.sendingIO(io, 4, 0, 0, false);
								}else {//download the client file
									tpserver.sendingIO(io, 3, 0, 0, false);
									getComputerInformation(pg);
									pg.setValue(100);
								}
								break;
							case 1:
								try {
									byte[] bt = this.objectTobyte(pcInfo);//we don't think the len is larger than 5K here
									System.arraycopy(bt, 0, io.getHeaderByteBuffer(), io.getHeaderPosition(64), bt.length);
									io.getHeader().put(io.getHeaderPosition(24),(byte) 1);
									io.getHeader().put(io.getHeaderPosition(25),(byte) 3);
									io.getHeader().putInt(io.getHeaderPosition(28), bt.length);
									tpserver.sendingIO(io, 3, 1, 0,true);
								}catch(Exception er) {}	
								break;
							case 2://host is locked,电脑被锁定
								JOptionPane.showMessageDialog(null, systemStatus.USERLOCKED.getLanguageName());		
								System.exit(0);
								break;
							case 3://host is disabled，电脑被禁用
								JOptionPane.showMessageDialog(null, systemStatus.USERDISABLED.getLanguageName());
								System.exit(0);
								break;
							}														
							break;
						case (short) 1:							
							try {			
								ickal.clear();
								crc32Value = io.getHeaderLong(32);
								fileSize = io.getHeaderLong(40);
								fileVersion = io.getHeaderLong(8);
								if (tpserver.decodeIO(io) == -1) throw new Exception("Decode data error");
								byte[] tb = new byte[io.getHeaderInt(28)];
								System.arraycopy(io.getHeaderByteBuffer(), io.getHeaderPosition(64), tb, 0, tb.length);
								ickal.add(tb);
								byteread = tb.length;
								io.putHeaderLong(48, byteread);
								tpserver.sendingIO(io, 3, 2, 0,false);								
								((JLabel) Lpack.getoMComponent( componentType.oMJLabel, 6)).setText("Total get:" + byteread + " Bytes," + (byteread * 1000 / (System.currentTimeMillis() - bts)) + "bytes/s");
							}catch(Exception er) {}
							break;
						case (short) 2:
							try {																	
								if (tpserver.decodeIO(io) == -1) throw new Exception("Decode data error");
								byte[] tb = new byte[io.getHeaderInt(28)];
								System.arraycopy(io.getHeaderByteBuffer(), io.getHeaderPosition(64), tb, 0, tb.length);
								ickal.add(tb);
								byteread += tb.length;
								io.putHeaderLong(48, byteread);
								tpserver.sendingIO(io, 3, 2, 0,false);								
								((JLabel) Lpack.getoMComponent( componentType.oMJLabel, 6)).setText("Total get:" + byteread + " Bytes," + (byteread * 1000 / (System.currentTimeMillis() - bts)) + "bytes/s");
							}catch(Exception er) {}
							break;
						case (short) 3:
							try {
								pg.setValue(100);								
								try {
									Files.createDirectories(new File(rootPath + File.separator).toPath());
								}catch(Exception er) {}
								
								FileOutputStream starF = new FileOutputStream(rootPath + File.separator + sFile);
								try{
									Files.createDirectories(new File(rootPath).toPath());													
									for (int bindex = 0;bindex < ickal.size();bindex++){
										starF.write(ickal.get(bindex));
									}
									starF.flush();
									starF.close();
									starF = null;
								}catch(Exception er){
									er.printStackTrace();
									JOptionPane.showMessageDialog(null, "create file error,please TRY AGAIN later");
									Files.deleteIfExists(new File(rootPath + File.separator + sFile).toPath());
									System.exit(0);
								}
								
								if (crc32Value != doCRC32(rootPath + File.separator + sFile)){
									try {
										Files.deleteIfExists(new File(rootPath + File.separator + sFile).toPath());	
									}catch(Exception er) {}													
									JOptionPane.showMessageDialog(null, "The file has been changed during the transfer");
									System.exit(0);
								}
								
								try {
									int len = 0;
									if ((len = tpserver.decodeIO(io)) == -1) throw new Exception("Decode data error");
									byte[] tb = new byte[len + 8];											
									System.arraycopy(io.getHeaderByteBuffer(), io.getHeaderPosition(64), tb, 0, tb.length);
									ByteBuffer.wrap(tb).putLong(len,fileVersion);
																		
									ickal.clear();
									ickal.add(tb);	
									skb = tb;
									FileSystem starFS = null;
									try{
										starFS = FileSystems.newFileSystem(Paths.get(rootPath + File.separator + sFile));
										try{
											Files.walkFileTree(starFS.getPath("/"), new SimpleFileVisitor<Path>(){
												public FileVisitResult visitFile(Path file,BasicFileAttributes atts) throws IOException
												{
													if (file.endsWith("spos.dat")){
														Files.write(file, ickal.get(0), StandardOpenOption.WRITE);
														return FileVisitResult.TERMINATE;
													}
													return FileVisitResult.CONTINUE;
												}
											});													
										}catch(Exception e){e.printStackTrace();}
									}catch(Exception e2){
										starFS = null;
									}
									
									try{starFS.close();}catch(Exception e){
										e.printStackTrace();
									}
								}catch(Exception er) {}
								io.putHeaderLong(32, Files.readAttributes(new File(rootPath +File.separator + sFile).toPath(), BasicFileAttributes.class).creationTime().toMillis());
								tpserver.sendingIO(io, 3, 3, 0,false);	
							}catch(Exception er) {}
							break;
						case (short) 4:
							io.putHeaderLong(32, ByteBuffer.wrap(skb).getLong(1024));
							tpserver.sendingIO(io, 3, 4, 0,false);
							break;
						case 5:
							int len = 0;
							if ((len = tpserver.decodeIO(io)) == -1) throw new Exception(systemStatus.DECODEERROR.getLanguageName());
							if (len != 64) {
								JOptionPane.showMessageDialog(null, systemStatus.DECODKEYEERROR.getLanguageName());
								System.exit(0);
							}
							byte[] tb = new byte[len];
							System.arraycopy(io.getHeaderByteBuffer(), io.getHeaderPosition(64),tb, 0, 64);
							int dp = 0;		
							byte[] sk1 = new byte[512];
		    				for (int index = 0;index < 512;index++){//XOR, 0x1F (% 32)
		    					dp += (((tb[index & 0x3F]) & 0x1) > 0 ? 1 : -((tb[index & 0x3F]) & 0x1));
		    					sk1[index] = (byte)(skb[dp++] ^ (tb[index & 0x3F]));//store the secKey1. and this is encoded by 64 bytes							    					
		    				}										
		    				tpserver.initSecKey(sk1, 1);
		    				System.arraycopy(pblib.getSecKey(2), 0, tb, 0, 64);
		    				sk1 = String.valueOf(Files.readAttributes(new File(rootPath +File.separator + sFile).toPath(), BasicFileAttributes.class).creationTime().toMillis()).getBytes().clone();
		    				System.arraycopy(sk1, 0, tb, 0, sk1.length);
		    				tpserver.initSecKey(tb, 2);
		    				tpserver.sendingIO(io, 4, 2, 0, false);
		    				break;
						}						
						break;
					case (byte) 4:
						try {
							switch (io.getHeaderShort(20)) {
							case 0:
								try {
									try {
										Files.deleteIfExists(new File(rootPath +File.separator + sFile).toPath());
									}catch(Exception er) {}
									ickal.clear();
									tpserver.sendingIO(io, 3, 0, 0, false);
									getComputerInformation(pg);
									pg.setValue(100);									
								}catch(Exception er) {}							
								break;	
							case 1:
								tpserver.sendingIO(io, 4, 2, 0, false);
								break;	
							case 2:
								if (tpserver.decodeIO(io) == -1) throw new Exception(systemStatus.DECODEERROR.getLanguageName());
								byte[] lc = new byte[2048];
								System.arraycopy(io.getHeaderByteBuffer(), io.getHeaderPosition(64 + 8),lc,0, 2040);
								io.clearHeader(0, 1024);
								pblib.setUserID(io.getHeaderLong(64));
								pblib.setLevelControl(lc);
								pblib.setServerID(io.getHeaderLong(0));
								pblib.setHostID(io.getHeaderLong(8));
								this.loadMainBody();
								break;
							}
						}catch(Exception er) {}
						break;
					}
				}catch(Exception er) {}
			}
		}catch(Exception er) {}
	}
	
	//combobox or Jbutton 动作
	public void oMJActionListener(Object ob,componentType ct,ActionEvent e) {
		try {
			switch (ct) {
			case oMJButton:
				try {
					//获取事件中的对象
					JButton oab = (JButton) e.getSource();					
					if (oab.equals((JButton) Lpack.getoMComponent( componentType.oMJButton, 0))) {
						try {
							if (((JTextField) Lpack.getoMComponent( componentType.oMJTextField, 0)).getText().trim().equals(null) || ((JTextField) Lpack.getoMComponent( componentType.oMJTextField, 0)).getText().trim().length() == 0){
								JOptionPane.showMessageDialog(null, "UserName can not be empty");
								return;
							}						
							if (((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 0)).getItemCount() == 0){
								agencyID = "00000";
								companyID = "00000";
								departmentID = "00000";
								
								if (JOptionPane.showConfirmDialog(this, systemStatus.NEWSERVERDETECTED.getLanguageName(), "", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.CANCEL_OPTION){
									return;
								}
								
								if (((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 0)).getSelectedItem().toString().trim() == "")
								{
									JOptionPane.showMessageDialog(null, systemStatus.ANENCYISNULL.getLanguageName());
									return;
								}
								
								if (((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 1)).getSelectedItem().toString().trim() == "")
								{
									JOptionPane.showMessageDialog(null, systemStatus.COMPANYISNULL.getLanguageName());
									return;
								}
								
								if (((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 2)).getSelectedItem().toString().trim() == "")
								{
									JOptionPane.showMessageDialog(null, systemStatus.DEPARTMENTISNULL.getLanguageName());
									return;
								}
							}
							else
							{
								for (int tint1 = 0;tint1 < idName.size();tint1++){
									if (idName.get(tint1).equals(((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 0)).getSelectedItem())){
										agencyID = idCode.get(tint1);
									}
								}						
							}
							if (((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 1)).getItemCount() == 0)
							{
								companyID = "00000";
								departmentID = "00000";
							}else
							{
								for (int tint1 = 0;tint1 < idName.size();tint1++){
									if (idName.get(tint1).equals(((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 1)).getSelectedItem()) && pCode.get(tint1).equals(agencyID)){
										companyID = idCode.get(tint1);
									}
								}
							}
							if (((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 2)).getItemCount() == 0)
							{
								departmentID = "00000";
							}else
							{
								for (int tint1 = 0;tint1 < idName.size();tint1++){
									if (idName.get(tint1).equals(((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 2)).getSelectedItem()) && pCode.get(tint1).equals(companyID)){
										departmentID = idCode.get(tint1);
										pblib.setDepartmentName(idName.get(tint1).trim());
									}
								}
							}
							pblib.setUserName(((JTextField) Lpack.getoMComponent( componentType.oMJTextField, 0)).getText().trim());
							
							if (((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 0)).getItemCount() == 0) agencyID = "00000";
							if (((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 1)).getItemCount() == 0) companyID = "00000";
							if (((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 2)).getItemCount() == 0) departmentID = "00000";
							String str = String.valueOf(((JPasswordField) Lpack.getoMComponent( componentType.oMJTextField, 1)).getPassword());
							ioBuffer io = tpserver.getIO(null,this);
							io.clearHeader(0, -1);	
							io.putHeaderInt(64, Integer.valueOf(agencyID.substring(1)));
							io.putHeaderInt(64 + 4, Integer.valueOf(companyID.substring(1)));
							io.putHeaderInt(64 + 8, Integer.valueOf(departmentID.substring(1)));
							io.setHeaderPosition(64 + 64);
							io.getHeader().putShort((short) new String((pblib.getUsername()).getBytes(Charset.forName("UTF-8"))).getBytes().length);
							io.getHeader().put(new String((pblib.getUsername()).getBytes()).getBytes(Charset.forName("UTF-8")));	
							io.setHeaderPosition(64 + 64 + 64);
							io.getHeader().putShort((short) new String(((String) str).getBytes(Charset.forName("UTF-8"))).getBytes().length);
							io.getHeader().put(new String(((String) str).getBytes()).getBytes(Charset.forName("UTF-8")));
							
							if (((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 0)).getItemCount() == 0) {
								io.setHeaderPosition(64 + 64 + 64 + 64) ;
								str = ((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 0)).getSelectedItem().toString().trim();						io.getHeader().putShort((short) new String(((String) str).getBytes(Charset.forName("UTF-8"))).getBytes().length);
								io.getHeader().put(new String(((String) str).getBytes()).getBytes(Charset.forName("UTF-8")));
								str = ((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 1)).getSelectedItem().toString().trim();						io.getHeader().putShort((short) new String(((String) str).getBytes(Charset.forName("UTF-8"))).getBytes().length);
								io.getHeader().put(new String(((String) str).getBytes()).getBytes(Charset.forName("UTF-8")));
								str = ((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 2)).getSelectedItem().toString().trim();						io.getHeader().putShort((short) new String(((String) str).getBytes(Charset.forName("UTF-8"))).getBytes().length);
								io.getHeader().put(new String(((String) str).getBytes()).getBytes(Charset.forName("UTF-8")));
							}
							
							io.putHeaderByte(24,(byte) 1);
							io.putHeaderByte(25,(byte) 3);
							io.putHeaderInt(28, io.getHeaderCurrentPositionLen() - 64);							
							tpserver.sendingIO(io, 2, 0, 0, true);						
						}catch(Exception er) {}	
					}else if (oab.equals((JButton) Lpack.getoMComponent( componentType.oMJButton, 1))) {
						try {	
							System.exit(0);					
						}catch(Exception er) {}	
					}
				}catch(Exception er) {}
				break;
			case oMJButtonLabel:
				break;
			case oMJCheckBox:
				break;
			case oMJComboBox:
				try {
					//agency 列表内容改变
					if (e.getSource().equals((JComboBox<String>) Lpack.getoMComponent(componentType.oMJComboBox, 0))) {
						((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 1)).removeAllItems();
						companyID ="";
						agencyID = "";
						departmentID = "";
												
						for (int tint1 = 0;tint1 < idName.size();tint1++){
							if (idName.get(tint1).equals(((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 0)).getSelectedItem())){
								agencyID = idCode.get(tint1);
								for (int tint2 = 0;tint2 <pCode.size();tint2++){
									if (pCode.get(tint2).equals(idCode.get(tint1))){
										if (!pCode.get(tint2).equals(idCode.get(tint2))) 
										{	
											((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 1)).addItem(idName.get(tint2));
											for (int tint3 = 0;tint3 < idCode.size();tint3++){
												if (pCode.get(tint3).equals(idCode.get(tint2)) && !pCode.get(tint3).equals(idCode.get(tint3))){
													boolean b1 = false;
													for (int tint4 = 1;tint4 < ((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 2)).getItemCount();tint4++){
														if (((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 2)).getItemAt(tint4).equals(idName.get(tint3))){
															b1 = true;
															break;
														}
													}
													if (!b1) ((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 2)).addItem(idName.get(tint3));
												}
											}
										}
									}
								}
								if (((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 1)).getItemCount() > 0) ((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 1)).setSelectedIndex(0);
								return;
							}
						}
					}else if (e.getSource().equals((JComboBox<String>) Lpack.getoMComponent(componentType.oMJComboBox, 1))) {
						//company 列表内容改变
						if (agencyID.equals("")) return;
						((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 2)).removeAllItems();
						
						companyID ="";
						departmentID = "";
						for (int tint1 = 0;tint1 < idName.size();tint1++){
							if (idName.get(tint1).equals(((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 1)).getSelectedItem()) && agencyID.equals(pCode.get(tint1))){
								companyID = idCode.get(tint1);
								for (int tint3 = 0;tint3 < pCode.size();tint3++){
									if (pCode.get(tint3).equals(idCode.get(tint1))){
										boolean b1 = false;
										for (int tint4 = 1;tint4 < ((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 2)).getItemCount();tint4++){
											if (((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 2)).getItemAt(tint4).equals(idName.get(tint3))){
												b1 = true;
												break;
											}
										}
										if (!b1) 
											((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 2)).addItem(idName.get(tint3));
									}
								}
								if (((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 2)).getItemCount() > 0) ((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 2)).setSelectedIndex(0);
								return;
							}
						}
					}else if (e.getSource().equals((JComboBox<String>) Lpack.getoMComponent(componentType.oMJComboBox, 3))) {
						try {
							Lpack.setLanguage(((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 3)).getSelectedIndex());
							for (languageType lt:languageType.values()) {
								if (lt.getLanguageName().equals(((JComboBox<String>) Lpack.getoMComponent( componentType.oMJComboBox, 3)).getSelectedItem().toString()))
									languagestr = lt;
							}						
							pblib.setLanguageType(languagestr);
							Lpack.setLanguage(languagestr);
							Lpack.InitLoader();
							validate();
						}catch(Exception er) {}
					}					
				}catch(Exception er) {}
				break;
			case oMJDialog:
				break;
			case oMJDocument:
				break;
			case oMJDragTextPane:
				break;
			case oMJErrorString:
				break;
			case oMJLabel:
				break;
			case oMJMidBar:
				break;
			case oMJOptionString:
				break;
			case oMJPanel:
				break;
			case oMJPopupMenu:
				break;
			case oMJRadioButton:				
				break;
			case oMJTable:
				break;
			case oMJTableColumnAlignment:
				break;
			case oMJTableColumnBounds:
				break;
			case oMJTableColumnWeights:
				break;
			case oMJTableColumnWidth:
				break;
			case oMJTableRowHeight:
				break;
			case oMJTableRowObjects:
				break;
			case oMJTableRowWeights:
				break;
			case oMJTableTitleString:
				break;
			case oMJTextArea:
				break;
			case oMJTextField:
				break;
			case oMJTextPane:
				break;
			case oMJWeekDay:
				break;
			default:
				break;
			
			}			
		}catch(Exception er) {}
	}
	
	//处理键盘事件
	public void oMJKeyListener(oaKeyEvent oae,componentType ct,KeyEvent arg0) {
		try {
			switch (ct) {
			case oMJButton:
				try {
					//获取事件中的对象
					JButton oab = (JButton) arg0.getSource();					
					switch (oae) {
					case keyPressed:
						break;
					case keyReleased:
						if (arg0.getKeyCode() == 10 || arg0.getKeyCode() == 13 ){
							oab.doClick();//执行按钮的鼠标单击事情
						}				
						break;
					case keyTyped:
						break;
					default:
						break;
					
					}
				}catch(Exception er) {}
				break;
			case oMJButtonLabel:
				break;
			case oMJCheckBox:
				break;
			case oMJComboBox:
				break;
			case oMJDialog:
				break;
			case oMJDocument:
				break;
			case oMJDragTextPane:
				break;
			case oMJErrorString:
				break;
			case oMJLabel:
				break;
			case oMJMidBar:
				break;
			case oMJOptionString:
				break;
			case oMJPanel:
				break;
			case oMJPopupMenu:
				break;
			case oMJRadioButton:
				break;
			case oMJTable:
				break;
			case oMJTableColumnAlignment:
				break;
			case oMJTableColumnBounds:
				break;
			case oMJTableColumnWeights:
				break;
			case oMJTableColumnWidth:
				break;
			case oMJTableRowHeight:
				break;
			case oMJTableRowObjects:
				break;
			case oMJTableRowWeights:
				break;
			case oMJTableTitleString:
				break;
			case oMJTextArea:
				break;
			case oMJTextField:
				switch (oae) {
				case keyPressed:
					break;
				case keyReleased:
					if (arg0.getKeyCode() == 10 || arg0.getKeyCode() == 13 ){//处理会车事件
						JTextField jt = (JTextField) arg0.getSource();
						if (jt.getName().equals("0"))
						{
							((JTextField) Lpack.getoMComponent( componentType.oMJTextField, 1)).grabFocus();//让密码获取焦点
						}else {
							((JButton) Lpack.getoMComponent( componentType.oMJButton, 0)).doClick();//如果在密码输入完毕后敲回车，则执行确认按钮的鼠标单击事件
						}
					}
					break;
				case keyTyped:
					break;
				default:
					break;
				
				}
				break;
			case oMJTextPane:
				break;
			case oMJWeekDay:
				break;
			default:
				break;
			
			}			
		}catch(Exception er) {}
	}
	//处理鼠标事件
	public synchronized void oMJMouseEvent(oaMouseEvent mv,MouseEvent e,componentType ct) {
		try {
			switch (ct) {
			case oMJButton:
				try {
					JButton oab = (JButton) e.getSource();
					switch (mv) {
					case MOUSE_CLICKED:
						if (Lpack.getoMComponent(componentType.oMJButton, 0).equals(oab)) {
							
						}else
							System.exit(0);						
						break;
					case MOUSE_DRAG:
						break;
					case MOUSE_EXIT:						
						break;
					case MOUSE_IN:						
						break;
					case MOUSE_MOVE:
						break;
					case MOUSE_PRESS:					
						break;
					case MOUSE_RELEASE:						
						break;
					default:
						break;
					
					}
				}catch(Exception er) {}
				break;
			case oMJButtonLabel:
				break;
			case oMJCheckBox:
				break;
			case oMJComboBox:
				break;
			case oMJDialog:
				break;
			case oMJDocument:
				break;
			case oMJDragTextPane:
				break;
			case oMJErrorString:
				break;
			case oMJLabel:
				break;
			case oMJMidBar:
				break;
			case oMJOptionString:
				break;
			case oMJPanel:
				break;
			case oMJPopupMenu:
				break;
			case oMJRadioButton:
				break;
			case oMJTable:
				break;
			case oMJTableColumnAlignment:
				break;
			case oMJTableColumnBounds:
				break;
			case oMJTableColumnWeights:
				break;
			case oMJTableColumnWidth:
				break;
			case oMJTableRowHeight:
				break;
			case oMJTableRowObjects:
				break;
			case oMJTableRowWeights:
				break;
			case oMJTableTitleString:
				break;
			case oMJTextArea:
				break;
			case oMJTextField:
				break;
			case oMJTextPane:
				break;
			case oMJWeekDay:
				break;
			default:
				break;
			
			}
			
		}catch(Exception er) {}
	}
	
	//用来获取电脑配置信息
	private void getComputerInformation(JProgressBar pg) {
		try {								
			switch(OSType.valueOf(this.osString)){
			case WINDOWS:
				{
					try{								
						pcInfo.put("OS", new HashMap<String,String>());//the following command must be run in 64bit env,otherwise it will not get the serial number for the windows
						Process kp = Runtime.getRuntime().exec("reg query \"HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\"");
						int n = 0,m = 0,k = 0,len = 0;
						int totalB;
						try{
							while ((keyInfo[m++] = (byte)kp.getInputStream().read()) != -1){
							}
							totalB = m;									
						}catch(Exception et){}
						n = 0;
						m = 1;
						if (keyInfo[0] == 13 && keyInfo[1] == 10){
							n = 2;
							m = 2;
						}		
						while (m < 4000){
							if (keyInfo[m] == 13 && keyInfo[m + 1] == 10){
								byte[] btemp = new byte[m - n];
								System.arraycopy(keyInfo, n, btemp, 0, m - n);
								////System.out.println(new String(btemp));
								if (new String(btemp).trim().indexOf("ProductName") == 0)
								{														
									pcInfo.get("OS").put("ProductName".toUpperCase(), new String(btemp).substring(new String(btemp).indexOf("REG_SZ") + 6).trim());									
								}
								if (new String(btemp).trim().indexOf("ProductId") == 0)
								{														
									pcInfo.get("OS").put("ProductId".toUpperCase(), new String(btemp).substring(new String(btemp).indexOf("REG_SZ") + 6).trim());									
								}
								if (new String(btemp).trim().indexOf("DigitalProductId") == 0 && new String(btemp).trim().indexOf("DigitalProductId4") != 0)
								{			
									byte[] bt = new String(btemp).substring(new String(btemp).indexOf("REG_BINARY") + 10).trim().getBytes();
									short[] rbt = new short[bt.length /2];
									int pos = 0,j = 24;
									while (pos < rbt.length)
									{
										rbt[pos] =(short) ((bt[pos * 2] > 64 ? (bt[pos * 2] - 65) + 10 : bt[pos * 2] - 48) * 16 + (bt[pos * 2 + 1] > 64 ? (bt[pos * 2 + 1] - 65) + 10 : bt[pos * 2 + 1] - 48));							
										pos++;
									}
									int isWin8 = (rbt[66] / 6) & 0x1; 
									rbt[66] = (short)((rbt[66] & 0xF7) | ((isWin8 & 2) * 4));
									String zf = "BCDFGHJKMPQRTVWXY2346789";
									String key = "";
									int last = 0;
									while (j >= 0){
										int cur = 0;
										int y = 14;
										while (y >= 0){
											cur *= 256;
											cur += rbt[y + 52];
											rbt[y + 52] =(short)(cur / 24);									
											cur = cur % 24;
											y--;									
										}
										j--;
										key = zf.substring(cur, cur + 1) + key;
										last = cur;
									}
									if (isWin8 == 1){
										String part = key.substring(1, last + 1);
										key = part + "N" + key.substring(last + 1);												
									}
									key = key.substring(0, 5) + "-" + key.substring(5, 10) + "-" + key.substring(10, 15) + "-" + key.substring(15, 20) + "-" + key.substring(20, 25);
									pcInfo.get("OS").put("DigitalProductId".toUpperCase(),key);
								}
								if (pcInfo.size() == 3) break;
								n = m + 2;
							}
							m++;
						}
						totalB = 0;
						
						String[] subP1 = {"Manufacturer","Model","Product","SerialNumber","Version"};
						if ((m = getPCValue(kp,new String[]{"wmic","BASEBOARD","GET",""},"BASEBOARD",subP1,true,1)) == 0){
							//System.out.println("Could not get the BaseBoard Information");
						}

						pg.setValue(pg.getValue() + 5);
						totalB += m;
						String[] subP2 = {"BIOSVersion","Manufacturer","ReleaseDate"};
						if ((m = getPCValue(kp,new String[]{"wmic","BIOS","GET",""},"BIOS",subP2,true,1)) == 0){
							//System.out.println("Could not get the BIOS Information");
						}

						pg.setValue(pg.getValue() + 5);
						totalB += m;
						String[] subP3 = {"DeviceID","AddressWidth","DataWidth","Description","L2CacheSize","Manufacturer","Name","NumberOfCores","NumberOfLogicalProcessors","ProcessorId","Revision","SystemCreationClassName","SystemName"};
						if ((m = getPCValue(kp,new String[]{"wmic","CPU","GET",""},"CPU",subP3,true,1)) == 0){
							//System.out.println("Could not get the CPU Information");
						}

						pg.setValue(pg.getValue() + 5);
						totalB += m;
						String[] subP4 = {"IdentifyingNumber","Name","UUID","Vendor","Version"};
						if ((m = getPCValue(kp,new String[]{"wmic","CSPRODUCT","GET",""},"SMBIOS",subP4,true,1)) == 0){
							//System.out.println("Could not get the SMBIOS Information");
						}

						pg.setValue(pg.getValue() + 5);
						totalB += m;						
						
						String[] subP7 = {"BankLabel","Capacity","Description","DeviceLocator","PartNumber","SerialNumber","Speed","Tag"};
						if ((m = getPCValue(kp,new String[]{"wmic","MEMORYCHIP","GET",""},"MEMORYCHIP",subP7,false,1)) == 0){
							//System.out.println("Could not get the MEMORYCHIP Information");
						}
						pg.setValue(pg.getValue() + 5);
						totalB += m;
						/*
						String[] subP5 = {"Caption","DeviceID","FirmwareRevision","InterfaceType","Model","SerialNumber","Signature","Size"};
						if ((m = getPCValue(kp,new String[]{"wmic","DISKDRIVE","GET",""},"DISKDRIVE",subP5,true,1)) == 0){
							//System.out.println("Could not get the DISKDRIVE Information");
						}

						pg.setValue(pg.getValue() + 5);
						totalB += m;
						String[] subP6 = {"Caption","Description","DeviceID","FileSystem","FreeSpace","Size","VolumeSerialNumber","VolumeName"};
						if ((m = getPCValue(kp,new String[]{"wmic","LOGICALDISK","GET",""},"LOGICALDISK",subP6,true,1)) == 0){
							//System.out.println("Could not get the LOGICDISK Information");
						}
						//System.out.println("8:" + System.currentTimeMillis());
						pg.setValue(pg.getValue() + 5);
						
						//System.out.println("9:" + System.currentTimeMillis());
						
						
						String[] subP8 = {"FullName","HomeDirectory","LastLogon","LogonServer","Name","PasswordAge","UserType","UserId"};
						if ((m = getPCValue(kp,new String[]{"wmic","NETLOGIN","GET",""},"NETLOGIN",subP8,true,4)) == 0){
							//System.out.println("Could not get the NETLOGIN Information");
						}
						//System.out.println("10:" + System.currentTimeMillis());
						pg.setValue(pg.getValue() + 5);
						totalB += m;
						
						String[] subP9 = {"MACAddress","NetConnectionID","PNPDeviceID"};
						if ((m = getPCValue(kp,new String[]{"wmic","NIC","GET",""},"NIC",subP9,true,0)) == 0){
							//System.out.println("Could not get the NIC Information");
						}
						//System.out.println("11:" + System.currentTimeMillis());
						pg.setValue(pg.getValue() + 5);
						totalB += m;
						*/
						String[] subP10 = {"DefaultIPGateway","DHCPServer","IPAddress","MACAddress"};
						if ((m = getPCValue(kp,new String[]{"wmic","NICCONFIG","GET",""},"NICCONFIG",subP10,true,2)) == 0){
							//System.out.println("Could not get the NICCONFIG Information");
						}
						//System.out.println("12:" + System.currentTimeMillis());
						pg.setValue(pg.getValue() + 5);
						totalB += m;
						/*
						String[] subP11 = {"Caption","Description","DeviceID","Name","DriverDate","DriverVersion","PNPDeviceID"};
						if ((m = getPCValue(kp,new String[]{"wmic","path","WIN32_VideoController","GET",""},"VIDEOCONTROLLER",subP11,true,2)) == 0){
							//System.out.println("Could not get the VIDEO Information");
						}
						//System.out.println("13:" + System.currentTimeMillis());
						pg.setValue(pg.getValue() + 5);
						totalB += m;
						*/
						String[] subP12 = {"CreationClassName","CSDVersion","CSName","InstallDate","RegisteredUser","OSArchitecture","Caption"};
						if ((m = getPCValue(kp,new String[]{"wmic","OS","GET",""},"OSINFORMATION",subP12,true,2)) == 0){
							//System.out.println("Could not get the OS Information");
						}
						//System.out.println("14:" + System.currentTimeMillis());
						pg.setValue(pg.getValue() + 5);
						totalB += m;
						
						kp.destroy();
						len = 1;
						pg.setValue(pg.getValue() + 5);		
					}catch(Exception ew)
					{
						ew.printStackTrace();
					}
				}
				break;
			case APPLE:
				{
					//System.out.println("MAC is herer");
					Process kp = null;
					byte[] endB = {10,10};
					int lenOfkey = 0,getB = 0;
					keyInfo = new byte[4096 * 32];
					pg.setValue(5);
					String subP1 = "Model Name,Model Identifier,Serial Number (system)";
					if ((getB = getMACValue(kp,"system_profiler SPHardwareDataType","BASEBOARD",subP1,endB,lenOfkey)) == 0){
						//System.out.println("Could not get the BaseBoard Information");
					}			
					pg.setValue(pg.getValue() + 5);
					lenOfkey += getB; 
					String subP2 = "Part Number,Serial Number";
					if ((getB = getMACValue(kp,"system_profiler SPMemoryDataType","MEMORYCHIP",subP2,endB,lenOfkey)) == 0){
						//System.out.println("Could not get the Memory Information");
					}
					
					pg.setValue(pg.getValue() + 5);
					lenOfkey += getB;
					String subP3 = "System Version,Kernel Version,Computer Name,User Name";
					if ((getB = getMACValue(kp,"system_profiler SPSoftwareDataType","OS",subP3,endB,lenOfkey)) == 0){
						//System.out.println("Could not get the OS Information");
					}
					lenOfkey += getB;
					pg.setValue(pg.getValue() + 5);
					String subP4 = "Serial Number,Model";
					if ((getB = getMACValue(kp,"system_profiler SPStorageDataType","SATA",subP4,endB,lenOfkey)) == 0){
						//System.out.println("Could not get the SATA Information");
					}
					lenOfkey += getB;
					pg.setValue(pg.getValue() + 5);
					String subP5 = "Hardware (MAC) Address";
					if ((getB = getMACValue(kp,"system_profiler SPNetworkDataType","NIC",subP5,endB,lenOfkey)) == 0){
						//System.out.println("Could not get the NETWORK Information");
					}
					lenOfkey += getB;
					
					pg.setValue(pg.getValue() + 5);
					String subP6 = "Hardware (MAC) Address";
					if ((getB = getMACValue(kp,"system_profiler SPAirPortType","NIC10",subP6,endB,lenOfkey)) == 0){
						//System.out.println("Could not get the NETWORK Information");
					}
					lenOfkey += getB;
					
					pg.setValue(pg.getValue() + 5);
					String subP7 = "Hardware (MAC) Address";
					if ((getB = getMACValue(kp,"system_profiler SPSASDataType","SAS",subP7,endB,lenOfkey)) == 0){
						//System.out.println("Could not get the SAS Information");
					}
					/*
					lenOfkey += getB;
					
					pg.setValue(pg.getValue() + 5);
					String subP7 = "Unique ID";
					if ((getB = getMACValue(kp,"system_profiler SPCameraDataType","Camer",subP7,endB,lenOfkey)) == 0){
						//System.out.println("Could not get the Camer Information");
					}
					lenOfkey += getB;
					pg.setValue(pg.getValue() + 5);
					String subP8 = "Serial Number";
					if ((getB = getMACValue(kp,"system_profiler SPPowerDataType","Power",subP8,endB,lenOfkey)) == 0){
						//System.out.println("Could not get the Power Information");
					}
					*/	
					pg.setValue(pg.getValue() + 5);
				}
				break;
			case LINUX:
				{
					//Process kp = Runtime.getRuntime().exec("vi /var/log/dmesg");//the log file recorded by the linux start
					SocketChannel sc;
					if ((sc = pblib.get_scIn_Channel()) != null)
					{
						pcInfo.put("NICCONFIG1", new HashMap<String,String>());
						pcInfo.get("NICCONFIG1").put("IPAddress".toUpperCase(), (((SocketChannel)sc).socket().getLocalAddress().getHostAddress()));
						pcInfo.get("NICCONFIG1").put("MACAddress".toUpperCase(), tpserver.getMACAddress());
						pcInfo.put("OS", new HashMap<String,String>());
						pcInfo.get("OS").put("PRODUCTNAME", "LINUX");								
					}
					
					getLinuxValue();
				}
				break;
			}				
			ArrayList<Long> ml = new ArrayList<Long>();
			Iterator<String> its = pcInfo.keySet().iterator();
			while (its.hasNext()) {
				String SQL = its.next();
				if (pcInfo.get(SQL).get("MACADDRESS") != null)
				{
					String hs = pcInfo.get(SQL).get("MACADDRESS") + ":";
					String bs = "";
					String ls = "";
					try {
						for (int i = 0;i < hs.length();i++) {
							if (hs.substring(i, i + 1).indexOf(":") == 0) {
								if (bs.length() == 1)
									bs = "0" + bs;
								ls += bs;
								bs = "";
							}else
							{
								bs += hs.substring(i, i + 1);
							}
						}
						long lm = Long.parseLong(ls, 16);
						if (!ml.contains(lm))
						{
							ml.add(lm);
							pcInfo.get(SQL).put("MACADDRESS", String.valueOf(lm));
						}
					}catch(Exception er) {}								
				}
			}					
		}catch(Exception ewr) {
			
		}
	}
	//获取Linux系统信息
	private boolean getLinuxValue() {
		try {
			String str1;
			String cmd[] = {};
			Process kp = new ProcessBuilder().command("hostname").start();
			BufferedReader br = new BufferedReader(new InputStreamReader(kp.getInputStream(),charSet));
			pcInfo.put("OSINFORMATION", new HashMap<String,String>());
			pcInfo.get("OSINFORMATION").put("CSNAME", br.readLine());
			
			kp.destroy();
			kp = new ProcessBuilder().command("whoami").start();
			br = new BufferedReader(new InputStreamReader(kp.getInputStream(),charSet));

			if (pcInfo.get(("NETLOGIN")) == null) pcInfo.put(("NETLOGIN").toUpperCase(), new HashMap<String,String>());
			pcInfo.get("NETLOGIN").put("NAME", br.readLine());
			
			kp.destroy();
			cmd = new String[]{"cat","/proc/cpuinfo"};
			kp = new ProcessBuilder().command(cmd).start();
			br = new BufferedReader(new InputStreamReader(kp.getInputStream(),charSet));
			
			while ((str1 = br.readLine()) != null) {
				if (str1.toUpperCase().indexOf("MODEL NAME") != -1) {
					if (pcInfo.get(("CPU")) == null) pcInfo.put(("CPU").toUpperCase(), new HashMap<String,String>());
					pcInfo.get("CPU").put("NAME", str1.substring(str1.indexOf(':') + 1).trim());
					break;
				}
			}			
			kp.destroy();			
			return true;
		}catch(Exception er) {return false;}
	}
	//获取Windows系统配置信息
	private int getPCValue(Process kp,String cmd[],String tP,String[] thep,boolean cp,int sn){
		try{
			kp = null;
			String str1 = "";			
			pcInfo.put((tP + 1).toUpperCase(), new HashMap<String,String>());
			int i1 = 0,k = 0;
			try{
				for (String cs:thep){
					cmd[cmd.length - 1] = cs;
					kp = new ProcessBuilder().command(cmd).start();
					BufferedReader br = new BufferedReader(new InputStreamReader(kp.getInputStream()));
					i1 = 0;
					k = 1;
					
					while ((str1 = br.readLine()) != null){
						if (i1++ == 0) continue;
						if (pcInfo.get((tP+k).toUpperCase()) == null) pcInfo.put((tP + k).toUpperCase(), new HashMap<String,String>());
						if (str1.trim().equals("")){
							k++;
							continue;
						}						
						pcInfo.get((tP + k).toUpperCase()).put(cs.toUpperCase(), str1.trim());	
						k++;
					}
				}
				k = 1;
				while (pcInfo.get((tP + k).toUpperCase()) != null){
					if (pcInfo.get((tP + k).toUpperCase()).size() < (thep.length - sn) && cp)
						pcInfo.remove((tP + k).toUpperCase());
					k++;
				}
			}catch(Exception et){
				et.printStackTrace();
				return 0;
			}
			return 1;
		}catch(Exception e){
			e.printStackTrace();
		}
		return 0;
	}
	//获取苹果电脑配置信息
	private int getMACValue(Process kp,String cmd,String tP,String thep1,byte[] endB,int beg){
		try{
			kp = Runtime.getRuntime().exec(cmd);			
			int n = 0,m = 0,k = 1,index1 = -1;
			try{
				String str1 = "",s1 = "";
				ArrayList<String> hs = new ArrayList<String>();
				ArrayList<Integer> hi = new ArrayList<Integer>();
				m = 0;
				while ((keyInfo[m] = (byte)kp.getInputStream().read()) != -1)
				{
					if (keyInfo[m] == 13 || keyInfo[m] == 10)
					{	
						if (m > n) 
							str1 = new String(keyInfo,n,m - n);
						else
							continue;
					}
					else
					{
						m++;
						continue;
					}
					n = m + 1;
					if (str1.trim().equals("")) continue;
					if (str1.trim().length() < 3) continue;
					switch (tP){
					case "BASEBOARD":						
						if (str1.toUpperCase().indexOf("MODEL") > -1 && str1.toUpperCase().indexOf("IDENTIFIER") > -1)
						{
							if (pcInfo.get(("CSPRODUCT")) == null) pcInfo.put(("CSPRODUCT").toUpperCase(), new HashMap<String,String>());
							pcInfo.get("CSPRODUCT").put("IdentifyingNumber".toUpperCase(), str1.substring(str1.indexOf(':') + 1).trim());
						}
						if (str1.toUpperCase().indexOf("Processor".toUpperCase()) > -1 && str1.toUpperCase().indexOf("NAME") > -1)							
						{
							if (pcInfo.get(("CPU")) == null) pcInfo.put(("CPU").toUpperCase(), new HashMap<String,String>());
							if (pcInfo.get("CPU").get("NAME") == null) 
								pcInfo.get("CPU").put("NAME", str1.substring(str1.indexOf(':') + 1).trim());
							else
								pcInfo.get("CPU").put("NAME", str1.substring(str1.indexOf(':') + 1).trim() + pcInfo.get("CPU").get("NAME"));
						}
						if (str1.toUpperCase().indexOf("Processor".toUpperCase()) > -1 && str1.toUpperCase().indexOf("SPEED") > -1)							
						{
							if (pcInfo.get(("CPU")) == null) pcInfo.put(("CPU").toUpperCase(), new HashMap<String,String>());
							if (pcInfo.get("CPU").get("NAME") == null) 
								pcInfo.get("CPU").put("NAME", str1.substring(str1.indexOf(':') + 1).trim());
							else
								pcInfo.get("CPU").put("NAME", pcInfo.get("CPU").get("NAME") + " " + str1.substring(str1.indexOf(':') + 1).trim());
						}
						if (str1.toUpperCase().indexOf("SERIAL") > -1 && str1.toUpperCase().indexOf("SYSTEM") > -1)
						{
							if (pcInfo.get(("BASEBOARD")) == null) pcInfo.put(("BASEBOARD").toUpperCase(), new HashMap<String,String>());
							pcInfo.get("BASEBOARD").put("SerialNumber".toUpperCase(), str1.substring(str1.indexOf(':') + 1).trim());
						}	
						if (str1.toUpperCase().indexOf("MEMORY") > -1)
						{
							if (pcInfo.get(("MEMORYCHIP")) == null) pcInfo.put(("MEMORYCHIP").toUpperCase(), new HashMap<String,String>());
							pcInfo.get("MEMORYCHIP").put("Capacity".toUpperCase(), str1.substring(str1.indexOf(':') + 1).trim());
						}
						break;
					case "OS":
						if (str1.toUpperCase().indexOf("SYSTEM") > -1 && str1.toUpperCase().indexOf("VERSION") > -1)
						{
							if (pcInfo.get(("OS")) == null) pcInfo.put(("OS").toUpperCase(), new HashMap<String,String>());
							pcInfo.get("OS").put("PRODUCTNAME", str1.substring(str1.indexOf(':') + 1).trim());
						}
						if (str1.toUpperCase().indexOf("COMPUTER") > -1 && str1.toUpperCase().indexOf("NAME") > -1)
						{
							if (pcInfo.get(("OSINFORMATION")) == null) pcInfo.put(("OSINFORMATION").toUpperCase(), new HashMap<String,String>());
							pcInfo.get("OSINFORMATION").put("CSNAME", str1.substring(str1.indexOf(':') + 1).trim());
						}
						if (str1.toUpperCase().indexOf("USER") > -1 && str1.toUpperCase().indexOf("NAME") > -1)
						{
							if (pcInfo.get(("NETLOGIN")) == null) pcInfo.put(("NETLOGIN").toUpperCase(), new HashMap<String,String>());
							pcInfo.get("NETLOGIN").put("NAME", str1.substring(str1.indexOf(':') + 1).trim());
						}
						break;
					case "SATA":						
						if (str1.toUpperCase().indexOf("STORAGE:") > -1)
						{
							if (pcInfo.get(("LOGICALDISK1")) == null) pcInfo.put(("LOGICALDISK1").toUpperCase(), new HashMap<String,String>());
							s1 = "";
							k = 1;
						}
						if (pcInfo.get(("LOGICALDISK1")) == null) continue;
						if (str1.trim().length() < 2) break;
						if (str1.trim().indexOf(':') > -1 && str1.trim().substring(str1.trim().indexOf(':')).length() < 2){
							s1 = str1.trim();
						}
						if (str1.toUpperCase().indexOf("SIZE:") > -1)
						{
							if (pcInfo.get(("LOGICALDISK" + k)) == null) pcInfo.put(("LOGICALDISK" + k).toUpperCase(), new HashMap<String,String>());
							pcInfo.get("LOGICALDISK" + k).put("DEVICEID",s1.trim());
							pcInfo.get("LOGICALDISK" + k).put("Description".toUpperCase(),"Local Fixed Disk");
							pcInfo.get("LOGICALDISK" + k).put("SIZE",str1.substring(str1.indexOf(':') + 1,str1.indexOf('(') - 1).trim());
							s1 = "";
							k++;
						}
						break;
					case "NIC":						
						if (str1.trim().length() < 2) break;
						if (str1.trim().indexOf(':') > -1 && str1.trim().substring(str1.trim().indexOf(':')).length() < 2){
							s1 = str1.trim();
							index1 = str1.indexOf(s1);
							if (hs.size() == 0)
							{
								hs.add(s1);
								hi.add(index1);
							}else{
								if (index1 > hi.get(hi.size() - 1))
								{
									hs.add(s1);
									hi.add(index1);
								}else if (index1 == hi.get(hi.size() - 1))
								{
									hs.remove(hs.size() - 1);
									hs.add(s1);
									hi.remove(hi.size() - 1);
									hi.add(index1);
								}else
								{
									for (int i = hi.size() - 1;i > -1;i--)
									{
										if (hi.get(i) >= index1)
										{
											hs.remove(i);
											hi.remove(i);
										}
									}
									hs.add(s1);
									hi.add(index1);
								}
							}							
						}
						if (str1.toUpperCase().indexOf("ADDRESSES:") > -1)
						{
							boolean bt = false;
							for (int it:hi){
								if (it == str1.toUpperCase().indexOf("ADDRESSES:"))
									bt = true;
							}
							if (!bt) break;
							if (pcInfo.get(("NICCONFIG" + k)) == null) pcInfo.put(("NICCONFIG" + k).toUpperCase(), new HashMap<String,String>());
							pcInfo.get("NICCONFIG" + k).put("IPADDRESS",str1.trim().substring(str1.trim().indexOf(':') + 1).trim());
						}
						if (str1.toUpperCase().indexOf("ROUTER:") > -1)
						{
							if (pcInfo.get(("NICCONFIG" + k)) == null) pcInfo.put(("NICCONFIG" + k).toUpperCase(), new HashMap<String,String>());
							pcInfo.get("NICCONFIG" + k).put("DEFAULTIPGATEWAY",str1.trim().substring(str1.trim().indexOf(':') + 1).trim());
						}
						if (str1.toUpperCase().indexOf("SERVER") > -1 && str1.toUpperCase().indexOf("IDENTIFIER") > -1)
						{
							if (hs.get(hs.size() - 1).indexOf("DHCP") == -1) break;
							if (pcInfo.get(("NICCONFIG" + k)) == null) pcInfo.put(("NICCONFIG" + k).toUpperCase(), new HashMap<String,String>());
							pcInfo.get("NICCONFIG" + k).put("DHCPSERVER",str1.trim().substring(str1.trim().indexOf(':') + 1).trim());
						}
						if (str1.toUpperCase().indexOf("MAC") > -1 && str1.toUpperCase().indexOf("ADDRESS") > -1)
						{
							s1 = str1.trim();
							index1 = str1.indexOf(s1);
							if (pcInfo.get(("NIC" + k)) == null) pcInfo.put(("NIC" + k).toUpperCase(), new HashMap<String,String>());
							pcInfo.get("NIC" + k).put("PNPDEVICEID",hs.get(hs.size() - 1));
							pcInfo.get("NIC" + k).put("NetConnectionID".toUpperCase(),hs.get(hs.size() - 2));
							pcInfo.get("NIC" + k).put("MACAddress".toUpperCase(),str1.trim().substring(str1.trim().indexOf(':') + 1).trim().toUpperCase());
							if (pcInfo.get(("NICCONFIG" + k)) == null) pcInfo.put(("NICCONFIG" + k).toUpperCase(), new HashMap<String,String>());
							pcInfo.get("NICCONFIG" + k).put("MACADDRESS",str1.trim().substring(str1.trim().indexOf(':') + 1).trim());
							k++;																						
						}
						break;
					}
				
				}
			}catch(Exception et){
				et.printStackTrace();
			}		
			return 1;
		}catch(Exception e){
			e.printStackTrace();
		}
		return 0;
	}
	
	private long doCRC32(String filename){//md5 
		try{
			CheckedInputStream cis = new CheckedInputStream(new FileInputStream(filename),new CRC32());
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
	
	private byte[] objectTobyte(Object o) {
		try {
			ByteArrayOutputStream ba = new ByteArrayOutputStream();
			ObjectOutputStream outp = new ObjectOutputStream(ba);
			outp.writeObject(o);
			return ba.toByteArray();
		}catch(Exception er) {return null;}
	}
}
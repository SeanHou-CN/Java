package com.enals.Loader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Date;

public class ServerLoader{
	/**
	 * 
	 */
	private static String rpath = System.getProperty("user.dir");
	private static Process proc;
	private static boolean isNew = false;
	private static String[] cs = new String[] {
			"java",
			"-jar",
			"-Xms256m",
			"-Xmx1024m",
			"starOAServer.jar"
			};
	
	private static String[] csr = new String[] {
			"java",
			"-jar",
			"-Xms256m",
			"-Xmx1024m",
			"starOAServer.jar"
			};

	private static String[] cso = new String[] {
			"java",
			"-jar",
			"-Xms256m",
			"-Xmx1024m",
			"starOAServer.jar"
			};
	
	public static void main(String[] args){
    	try{
    		try{
	    		if (Files.notExists(new File(rpath + File.separator + "serverConfig.inf").toPath())) {
	    			Files.createFile(new File(rpath + File.separator + "serverConfig.inf").toPath());
	    			Files.write(new File(rpath + File.separator + "serverConfig.inf").toPath(), writeObjectTobyte(cs), StandardOpenOption.WRITE);							
	    		}
	    	}catch(Exception dd) {
	    		dd.printStackTrace();
	    	}
    		
    		try{
            Files.deleteIfExists(new File(rpath + File.separator + "ServerRunning.inf").toPath());
         }catch(Exception dd) {
 		    		dd.printStackTrace();
 		    }
			while (true) {
    			try{
    				csr = (String[]) readObjectFromFile(rpath + File.separator + "serverConfig.inf");
    				isNew = false;
    				for (int i = 0;i < csr.length;i++) {
    					if (!csr[i].toUpperCase().equals(cs[i].toUpperCase())) {
    						if (Files.exists(new File(rpath + File.separator + csr[csr.length - 1]).toPath())) {
    							isNew = true;
    							cso = cs.clone();
        						cs = csr.clone();
    						}
    						break;
    					}
    				}
    				
    				try{
			         if(Files.notExists(new File(rpath + File.separator + "ServerRunning.inf").toPath())){
			        	 	isNew = true;
			        	 	cs = cso.clone();
			         }else {
		         		if (new Date().getTime() - new File(rpath + File.separator + "ServerRunning.inf").lastModified() > 900000) //15 minutes
			         	{
			         	isNew = true;
			         	}
			         }
		         }catch(Exception dd) {
		 		    	dd.printStackTrace();
		 		    }
    				
    				if (isNew || proc == null)
	    				try {
							if (proc == null)
							{
								proc = new ProcessBuilder().command(cs).start();
							}
							else {
								proc.destroy();
								proc = new ProcessBuilder().command(cs).start();
							}
							System.out.println("Server (" + cs[4] + ") is running at:" + new Date());
						}catch(Exception er) {
							er.printStackTrace();
						}
    	    	}catch(Exception dd) {
    	    	}
    			
    			try{
    	    		Thread.sleep(5000 * 60);//1 minute
    	    	}catch(Exception dd) {
    	    	}
    		}
    	}catch(Exception dd) {
    	}finally {
    		System.out.println("Server is Stopped at:" + new Date());
    	}
	}
	
	public static byte[] writeObjectTobyte(Object o) {
		try {
			ByteArrayOutputStream ba = new ByteArrayOutputStream();
			ObjectOutputStream outp = new ObjectOutputStream(ba);
			outp.writeObject(o);
			return ba.toByteArray();
		}catch(Exception er) {
			return null;}
	}
	
	public static Object readObjectFromFile(String cfile) {
		try {
			byte[] fb = Files.readAllBytes(new File(cfile).toPath());
			ByteArrayInputStream ba = new ByteArrayInputStream(fb,0,fb.length);
			ObjectInputStream inp = new ObjectInputStream(ba);
			return inp.readObject();
		}catch(Exception er) {
			return null;
		}
	}
}

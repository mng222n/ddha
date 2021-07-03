package util;

/**
 * @author Julien Nguyen
 * @date 2008/09/20
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import overlay.IOverlay;
import overlay.IOverlayImpl;
import util.Parser;

import java.util.List;
import java.util.ArrayList;

public class Util {
	
	private String via_addr=null;
	private int host_port=0;
	private String fileName = "./config/gateway.cfg";
	public static final String softPhone = "C://Program Files//CounterPath//X-Lite//x-lite.exe";
	private IOverlay iover;
	
	public static final String MASS08_ACC_KEY = "mass08acc";
	public static final String MASS08_COM_KEY = "mass08com";
	public static final String MASS08_SER_KEY = "mass08ser";
	public static final String MASS08_SIP_KEY = "mass08sip";
	
	public static final int CUR_SHOW_ACC = 4;
	public static final int CUR_SHOW_COM = 5;
	public static final int CUR_SHOW_SER = 6;
	public static final int CUR_INPUT_ACC = 0;
	public static final int CUR_INPUT_COM = 2;
	public static final int CUR_INPUT_SER = 3;
	public static final int CUR_SHOW_SIPUSERS = 7;
	
	public static String chooseFile( JFrame f ) {
		JFileChooser fc = new JFileChooser();
    	int returnVal = fc.showOpenDialog(f);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            return file.toString();
        } 
        
		return null; 
	}
	
	public static List<String> readFile( String fileName ) {
		
		FileReader fin = null;
		List<String> lines  = new ArrayList<String>();
		String line = null;
		
		try {
			fin = new FileReader ( fileName );
			BufferedReader fileBuf = new BufferedReader( fin );
			
			try {
				while ( ( line = fileBuf.readLine() ) != null ) {
					lines.add(line);
				}
				
			} catch (Exception e) {
				System.out.println( "Error in File readline." );
			}
			
		} catch( Exception e ){
			System.out.println( "Error in FileReader." );
		}
		
		return lines;
	}
	
	
	//====
	
		 public void  loadFile(String file)
		   {  //System.out.println("DEBUG: loading Configuration");
		      if (file==null)
		      {  //System.out.println("DEBUG: no Configuration file");
		         return;
		      }
		      //else
		      BufferedReader in=null;
		      //System.out.println("DEBUG: !!!Configuration file");  
		      try
		      {  in=new BufferedReader(new FileReader(file));
		          
		         while (true)
		         {  String line=null;
		            try { line=in.readLine(); } catch (Exception e) { e.printStackTrace(); System.exit(0); }
		            if (line==null) break;
		         
		            if (!line.startsWith("#"))
		            {  parseLine(line);
		            }
		         
		         } 
		         in.close();
		      }
		      catch (Exception e)
		      {  System.err.println("WARNING: error reading file \""+file+"\"");
		         //System.exit(0);
		         return;
		      }
		      //System.out.println("DEBUG: loading Configuration: done.");
		   }
		 
		 /** Parses a single line (loaded from the config file) */
	   public void parseLine(String line)
	   {  String attribute;
	      Parser par;
	      int index=line.indexOf("=");
	      if (index>0) {  attribute=line.substring(0,index).trim(); par=new Parser(line,index+1);  }
	      else {  attribute=line; par=new Parser("");  }
	      
	      if (attribute.equals("via_addr")) {  via_addr=par.getString(); return;  }
	      if (attribute.equals("host_port")) {  host_port=par.getInt(); return; }
	   }  

		public void connect2Overlay() {
	     
		loadFile(fileName);  
		
		//establishing XML-RPC
		iover = new IOverlayImpl();
		//set gateway
		try {
			iover.setGateway(via_addr, host_port);
		} catch (Exception e) {
			
		}
		
		System.out.println("[INFO] Overlay gateway IP address is " + via_addr + ",port is " + host_port);
	}
		
	public int put(String key, String value, String ttl_sec) throws Exception {
		return iover.put(key, value, ttl_sec);
	}
	
	public List<String> get(String key, String maxvals ) throws Exception {
		return iover.get(key, maxvals);
	}
}

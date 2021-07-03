package com.int2.expeshare.p2psiphoc;

import java.io.BufferedReader;
import java.io.FileReader;

import org.zoolu.tools.Parser;

/**
 * @author skminh
 *
 */

public class Constants {
	
	static public String 
	  // BAMBOO_CFG_FILE = "/opt/Expeshare/config/dht.cfg";
	BAMBOO_CFG_FILE = "C:\\Cygwin\\Expeshare\\config\\dht.cfg";
	static public String 
	   //SIP_CFG_FILE = "/opt/Expeshare/config/sip.cfg";
		SIP_CFG_FILE = "C:\\Cygwin\\Expeshare\\config\\sip.cfg";
	static public String 
	  // SIP_DB_FILE = "/opt/Expeshare/db/users.db";
	SIP_DB_FILE = "C:\\Cygwin\\Expeshare\\db\\users.db";
	
	public static final String MASS08_ACC_KEY = "mass08acc";
	public static final String MASS08_COM_KEY = "mass08com";
	public static final String MASS08_SER_KEY = "mass08ser";
	public static final String MASS08_SIP_KEY = "mass08sip";
	//put nodes (not need anymore!!!)
	//static public String DHTGatewayIP = "157.159.228.8";
	static public int DHTGatewayPort = 3631;
	
	//2008.08.23
	//change to automatically config DHT gateway for P2P SIP Proxy
	static public String via_addr="";
	static public int host_port=0;
	
	/**	
	 * Return user,ip from an uri description
	 * @param url String in format e.g 'alice@123.123.123.123:1234'
	 * @return Array of String with 2 elements
	 */
	static public String[] getTarget(String uri) {  
		if (uri == null) return null;
		String[] strArr = new String[2];
		strArr = uri.split("@");
		return strArr;
	}
	
	static public void  loadFile(String file)
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
	static public void parseLine(String line)
	{  String attribute;
	   Parser par;
	   int index=line.indexOf("=");
	   if (index>0) {  attribute=line.substring(0,index).trim(); par=new Parser(line,index+1);  }
	   else {  attribute=line; par=new Parser("");  }
	   
	   if (attribute.equals("via_addr")) {  via_addr=par.getString(); return;  }
	   if (attribute.equals("host_port")) {  host_port=par.getInt(); return; }
	}  
	
	//2008.08.23
	//
	static public String getDHTGatewayIP() {
		loadFile(SIP_CFG_FILE);  
		return via_addr;
	}
}
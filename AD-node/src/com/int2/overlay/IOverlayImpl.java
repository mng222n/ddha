/**
 * 
 */
package com.int2.overlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.xmlrpc.XmlRpcClientLite;

/**
 * @author INT
 *
 */
public class IOverlayImpl extends IOverlay {
	
	private boolean isSetGw;
	private XmlRpcClientLite client;
	
	public IOverlayImpl() {
		isSetGw = false;
	}
	
	/**
	 * Set parameters for connection
	 * @param address
	 * @param port
	 * @throws Exception
	 */
	public void setGateway(String address, int port) throws Exception {
	
		String strPort = Integer.toString( port );
		
		String url = "http://" + address + ":" + strPort;
		//establishing XML-RPC
		client = new XmlRpcClientLite ( url );
		isSetGw = true;
	}
	
	/**
	 * Put (key, value) to Overlay
	 * @param key
	 * @param value
	 * @param ttl_sec
	 * @return 0 if success, -1 if fail
	 * @throws Exception
	 */
	public int put(String key, String value, Integer ttl_sec) throws Exception {
		// TODO add your code here
		//String secret = new String ("INT");
		
		if (isSetGw == false ) return -1;
		
    	Vector params = new Vector ();

        byte [] keyb = new byte [128];
       
        keyb = key.getBytes();
        params.add (keyb);

        byte [] valueb = new byte [1024];
        valueb = value.getBytes();
        params.add (valueb);

        params.add (ttl_sec.toString());
        
        //byte [] secretb = new byte [24];
        //secretb = secret.getBytes();
        //params.add (secretb);

        String application = "SCOPE";
        params.add (application);
	        
	    try {
	        Integer put_result = (Integer) client.execute ("put", params);
	        
	        if (put_result != 0)	return -1;
	        
		} catch (Exception ex) {
			return -1;
        
	    }
		
        return 0;

	}
	
	/**
	 * Get values from Overlay
	 * @param key
	 * @param maxvals Maximum number of values need to return
	 * @return List<String> if success, return null if fail
	 */
	public List<String> get(String key, Integer maxvals ) {
	
		if (isSetGw == false) return null;
		
		List<String> lines  = new ArrayList<String>();
		
		Vector params = new Vector ();
	
	    byte [] keyb = new byte [128];
	        
	    keyb = key.getBytes();
	    params.add (keyb);
	    
	    params.add (maxvals.toString());
	
	    byte [] placemark_bytes = new byte [0];
	    params.add (placemark_bytes);
	
	    String application = "XmlRpcTest";
	    params.add (application);
        
	    try { 
	        Vector get_result = (Vector) client.execute ("get", params);
	        
	        Vector values = (Vector) get_result.elementAt (0);
	        
	        for (Object obj : values) {
	            byte [] returned_bytes = (byte []) obj;
	            String strValue2 = new String ( returned_bytes );
	            lines.add(strValue2);
	        }
	         
	    } catch (Exception ex ) {
	    	return null;
	    }
		
	    return lines;
	}
	
	public int remove(String key) throws Exception {
	if (isSetGw == false ) return -1;
		
    	Vector params = new Vector ();

        byte [] keyb = new byte [128];
       
        keyb = key.getBytes();
        params.add (keyb);

        byte [] secretb = new byte [1024];
        String secret = new String("INT-SCOPE");
        secretb = secret.getBytes();
        params.add (secretb);

        params.add (new Integer(3600));

        String application = "SCOPE";
        params.add (application);
	        
	    try {
	        Integer rm_result = (Integer) client.execute ("rm", params);
	        
	        if (rm_result != 0)	return -1;
	        
		} catch (Exception ex) {
			return -1;
        
	    }
		
        return 0;
	}

}

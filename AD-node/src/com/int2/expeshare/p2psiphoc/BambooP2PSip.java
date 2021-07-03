/**
 * 
 */
package com.int2.expeshare.p2psiphoc;


import local.server.Proxy;
import local.server.ServerProfile;

import org.zoolu.sip.provider.SipProvider;
import org.zoolu.sip.provider.SipStack;

/**
 * @author skminh
 *
 */
public class BambooP2PSip extends P2PSipHoc {
	 // ****************************** MAIN *****************************

	   /** The main method. */
	   public static void main(String[] args)
	   {  
		   
		  //Running Bamboo
		  Thread dustdevil = new Thread(new DustDevilThread(), "DustDevilThread");
	      dustdevil.start();
		  
	      String file=null;
	      
	      //Initation SIP            
	      file = Constants.SIP_CFG_FILE;
	      SipStack.init(file);
	      SipProvider sip_provider=new SipProvider(file);
	      ServerProfile server_profile=new ServerProfile(file);
	      
	      //Starting Registrar/Proxy and wait for request
	      new Proxy(sip_provider,server_profile);
	   }

}

/**
 * 
 */
package com.int2.expeshare.test;

import org.zoolu.tools.Configure;

/**
 * @author skminh
 *
 */
public class NetworkStack extends Configure {
	
	  public static void init(String file)
	   {  
	      (new NetworkStack()).loadFile(file);
	   }

}

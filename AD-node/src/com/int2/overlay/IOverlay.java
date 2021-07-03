/**
 * Main class supports interface in order to access into INT's DHT Overlay
 */
package com.int2.overlay;

import java.util.List;


/**
 * @author INT
 *
 */
public abstract class IOverlay {
	
	public abstract void setGateway(String address, int port) throws Exception;
	
	public abstract int put(String key, String value, Integer ttl_sec) throws Exception;
	
	public abstract List<String> get(String key, Integer maxvals ) throws Exception;
	
	public abstract int remove(String key) throws Exception;
	
}

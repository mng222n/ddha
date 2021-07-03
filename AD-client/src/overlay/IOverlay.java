/**
 * Main class supports interface in order to access into INT's DHT Overlay
 */
package overlay;

import java.util.List;


/**
 * @author INT
 *
 */
public abstract class IOverlay {
	
	public abstract void setGateway(String address, int port) throws Exception;
	
	public abstract int put(String key, String value, String ttl_sec) throws Exception;
	
	public abstract List<String> get(String key, String maxvals ) throws Exception;
	
	public abstract int remove(String key) throws Exception;
	
}

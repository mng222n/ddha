/**
 * Test Overlay API
 */
package overlay;

import java.net.Inet4Address;
import java.util.List;
import java.util.ListIterator;
	
import overlay.IOverlay;
//import com.int2.expeshare.p2psiphoc.Constants;


/**
 * @author INT
 *
 */
public class OverlayTest {

	/**
	 * @param args
	 */
	
	public OverlayTest(){
		
	}
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		
		IOverlay iover = new IOverlayImpl();
		//set gateway
		iover.setGateway("192.168.0.13", 3631);
		
		String myip = Inet4Address. getLocalHost().getHostAddress();
		//put
		//String uri = "bob@123.123.123.123:1342";
		//String[] strArr = new String[2];
		//strArr = Constants.getTarget(uri);
		//System.out.println("uri: " + uri);
		//System.out.println("user: " + strArr[0] );
		//System.out.println("ip: " + strArr[1] );
		
		//int ret = iover.put(strArr[0], strArr[1], new Integer(3600) );
		int ret = iover.put("mimi", "mimi ui ui`", new String("36000"));
		if (ret < 0 )
			System.out.println("Put fail-myip="+myip);
		else
			System.out.println("Put succeed myip="+myip);
		
		//get
		List<String> lines;
		
		/*
		lines = iover.get("daouyenthy", new Integer(10) );
		if (lines == null)
			System.out.println("Get fail");
		else
			System.out.println("Get succeed");
		
		//return values
		if (lines != null ) {
		
			ListIterator<String> li = lines.listIterator();
			String line = null;
			
			while (li.hasNext()) {
				line = (String)li.next();
				System.out.println( line );
			}
		}
		*/
		
		//ret = iover.remove("daouyenthy");
		//if (ret < 0 )
		//	System.out.println("Remove fail");
		//else
		//	System.out.println("Remove succeed");
		
		//get2
		lines = iover.get("skdhtuser", new String("36000") );
		if (lines == null)
			System.out.println("Get2 fail");
		else
			System.out.println("Get2 succeed");
		
		//return values
		if (lines != null ) {
		
			ListIterator<String> li = lines.listIterator();
			String line = null;
			
			while (li.hasNext()) {
				line = (String)li.next();
				System.out.println( line );
			}
		}
	}

}

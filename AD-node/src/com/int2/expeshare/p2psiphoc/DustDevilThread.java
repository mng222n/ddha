package com.int2.expeshare.p2psiphoc;

import java.io.IOException;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import bamboo.lss.ASyncCoreImpl;
import bamboo.lss.DustDevil;
import bamboo.lss.DustDevilSink;

/**
 * @author skminh
 *
 */
public class DustDevilThread implements Runnable {
	
	Thread runner;
	
	public DustDevilThread() {
		
	}
	
	public DustDevilThread( String threadName) {
		runner = new Thread(this, threadName);
		System.out.println( runner.getName() );
		runner.start();
	}
	/**
	 * Run Bamboo's DustDevil 
	 */
	public void run(){
		System.out.println("DustDevil is running.");
		try {
			ddMain();
		} catch (Exception e) {
			
		}
		
	}
	
	public void ddMain()  throws Exception {
		  	
			String cfgfile = null;
			
			//config something
	        PatternLayout pl = new PatternLayout ("%d{ISO8601} %-5p %c: %m\n");
	        ConsoleAppender ca = new ConsoleAppender (pl);
	        Logger.getRoot ().addAppender (ca);
	        Logger.getRoot ().setLevel (Level.INFO);
	        cfgfile = Constants.BAMBOO_CFG_FILE;
        

	        Logger l = Logger.getLogger (DustDevil.class);
	        try {
	            DustDevil.set_acore_instance (new ASyncCoreImpl ());
	        }
	        catch (IOException e) {
	            l.fatal ("could not open selector", e);
	            System.exit (1);
	        }

	        DustDevil dd = new DustDevil ();
	        dd.main (cfgfile);

	        //Start the main loop.
	        try {
	            DustDevil.acore_instance ().async_main ();
	        }
	        catch (OutOfMemoryError e) {
	            DustDevilSink.reserve = null;
	            System.gc ();
	            l.fatal ("uncaught error", e);
	            System.exit (1);
	        }
	        catch (Throwable e) {
	            l.fatal ("uncaught exception", e);
	            System.exit (1);
	        }
	}
}

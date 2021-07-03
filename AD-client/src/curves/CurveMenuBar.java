package curves;

/**
 * @author me
 * @seen 9, April 2007
 * 
 */
import util.ScreenImage;

import java.awt.event.ActionEvent;
//import java.awt.event.KeyEvent;

import javax.swing.JFileChooser;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;

import util.Util;

import function.FunVariations;
import function.Function;
import function.Functions;

import java.awt.event.ActionListener;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;


public class CurveMenuBar extends JMenuBar implements ActionListener {

	private static final long serialVersionUID = 1L;
	
	private CurveFrame f;
	
	Stack<Process> pStack = new Stack<Process>();
	
	
	CurveMenuBar(final CurveFrame f) {
		
		this.f = f;
		
		//File
		JMenu file = (JMenu)this.add( new JMenu("File") );
		file.setMnemonic('F');
	
		JMenuItem mi;
		
		mi = (JMenuItem)file.add(new JMenuItem("Join to Community"));
		mi.setMnemonic('N');
		mi.addActionListener(this);
		
		mi = (JMenuItem)file.add(new JMenuItem("New Community"));
		mi.setMnemonic('O');
		mi.addActionListener(this);
		
		//file.add( new JSeparator() );
		//mi = (JMenuItem)file.add(new JMenuItem("Close"));
		//mi.setMnemonic('C');
		//mi.addActionListener(this);
		
		file.add( new JSeparator() );
		mi = (JMenuItem)file.add(new JMenuItem("Instant Messenger/VoIP"));
		mi.setMnemonic('V');
		mi.addActionListener(this);

		file.add( new JSeparator() );
		mi = (JMenuItem)file.add(new JMenuItem("Add service"));
		mi.setMnemonic('S');
		mi.addActionListener(this);
		
		
		file.add( new JSeparator() );
		mi = (JMenuItem)file.add(new JMenuItem("Exit"));
		mi.setMnemonic('E');
		mi.addActionListener(this);
		
		//View
		JMenu view = (JMenu)this.add( new JMenu("View") );
		view.setMnemonic('V');
		
		mi = (JMenuItem)view.add(new JMenuItem("Accounts"));
		mi.setMnemonic('A');
		mi.addActionListener(this);
		
		mi = (JMenuItem)view.add(new JMenuItem("Services"));
		mi.setMnemonic('S');
		mi.addActionListener(this);
		
		mi = (JMenuItem)view.add(new JMenuItem("Communities"));
		mi.setMnemonic('C');
		mi.addActionListener(this);
		
		view.add( new JSeparator() );
		mi = (JMenuItem)view.add(new JMenuItem("Private Box"));
		mi.setMnemonic('P');
		mi.addActionListener(this);
		
		mi = (JMenuItem)view.add(new JMenuItem("Public Messages"));
		mi.setMnemonic('U');
		mi.addActionListener(this);
		
		view.add( new JSeparator() );
		mi = (JMenuItem)view.add(new JMenuItem("MASS08 Daily news"));
		mi.setMnemonic('M');
		mi.addActionListener(this);
		
		//View
		JMenu tools = (JMenu)this.add( new JMenu("Tools") );
		view.setMnemonic('T');
		
		//JMenu submenu = (JMenu)this.add( new JMenu("Communities") );
		//submenu.setMnemonic('Z');
		
		mi = (JMenuItem)tools.add(new JMenuItem("Monitoring from SIP users"));
		mi.setMnemonic('M');
		mi.addActionListener(this);
		
	    mi = (JMenuItem)tools.add(new JMenuItem("Tracking of SuperNodes"));
		mi.setMnemonic('T');
		mi.addActionListener(this);
		
		//mi = (JMenuItem)submenu.add(new JMenuItem("75 %"));
		//mi.setMnemonic('W');
		//mi.addActionListener(this);
		
		//mi = (JMenuItem)submenu.add(new JMenuItem("100 %"));
		//mi.setMnemonic('W');
		//mi.addActionListener(this);
		
		//mi = (JMenuItem)submenu.add(new JMenuItem("125 %"));
		//mi.setMnemonic('F');
		//mi.addActionListener(this);
		
		//mi = (JMenuItem)submenu.add(new JMenuItem("150 %"));
		//mi.setMnemonic('F');
		//mi.addActionListener(this);
		
		//mi = (JMenuItem)submenu.add(new JMenuItem("200 %"));
		//mi.setMnemonic('W');
		//mi.addActionListener(this);
		
		//mi = (JMenuItem)submenu.add(new JMenuItem("300 %"));
		//mi.setMnemonic('F');
		//mi.addActionListener(this);
		
		//view.add(submenu);
		
		//Help
		JMenu help = (JMenu)this.add( new JMenu("Help") );
		help.setMnemonic('H');
		mi = (JMenuItem)help.add(new JMenuItem("About"));
		mi.setMnemonic('A');
		mi.addActionListener(this);
		
	}
	
	  public void actionPerformed(ActionEvent e) {
	        JMenuItem source = (JMenuItem)(e.getSource());
	        //System.out.println( source.getText() );
	        
	        if ( source.getText().equalsIgnoreCase("Open File...") )
	        {	
	        	String fileName;
	        	String line;
	        	List<String> lines;
	        	
	        	fileName = Util.chooseFile(f);
	        	if ( fileName != null) {
	        		//System.out.println( "............ " + fileName );
	        		lines = Util.readFile(  fileName );
	        		ListIterator li = lines.listIterator();

	        		f.clearFunctions();
	        		//f.infos.removeLabels();
	        		f.index = 0;
	        		
	        		while (li.hasNext()) {
	        			line = (String)li.next();
	        			System.out.println( "readFile " + line );
	            		f.addFunction( line );
	        		}
	        	
	        	} else {
		        	JOptionPane.showMessageDialog(f,
		        		    					  "Couldn't open file.",
		        		    					  "Curve Warning",
		        		    					  JOptionPane.WARNING_MESSAGE
		        		    					  );
	        	}
	        
	        } else if ( source.getText().equalsIgnoreCase("Exit") ) {
	        	System.exit(0);
	        
	        } else if ( source.getText().equalsIgnoreCase("New Window") ) {
	        	try {
	        		Function f = Functions.parse( "* x cos * 2 x" );
	        		CurveApplication.start(new FunVariations(f, -2 * Math.PI, 2 * Math.PI));
	    		} catch (Exception ex ) {
	    			System.out.println( "Error in start curve application");
	    		}
	        
	        } else if ( source.getText().equalsIgnoreCase("Accounts") ) {
        		f.tracer.showAccounts();
        		f.tracer.repaint();
	        	f.tracer.revalidate();
	        		
	        } else if ( source.getText().equalsIgnoreCase("Communities") ) {
        		f.tracer.showCommunities();
        		f.tracer.repaint();
	        	f.tracer.revalidate();
        		
	        } else if ( source.getText().equalsIgnoreCase("Services") ) {
        		f.tracer.showServices();
        		f.tracer.repaint();
	        	f.tracer.revalidate();
	        	
	        } else if ( source.getText().equalsIgnoreCase("Join to Community") ) {
        		f.tracer.showInputAccount();
        		f.tracer.repaint();
	        	f.tracer.revalidate();
	        	
	        } else if ( source.getText().equalsIgnoreCase("New Community") ) {
        		f.tracer.showInputCommunity();
        		f.tracer.repaint();
	        	f.tracer.revalidate();
	        	
	        } else if ( source.getText().equalsIgnoreCase("Add Service") ) {
        		f.tracer.showInputService();
        		f.tracer.repaint();
	        	f.tracer.revalidate();
	        	
	        } else if ( source.getText().equalsIgnoreCase("Monitoring from SIP users") ) {
	         		f.tracer.showSIPUsers();
	         		f.tracer.repaint();
	 	        	f.tracer.revalidate();
	        	
	        	
		    } else if ( source.getText().equalsIgnoreCase("Instant Messenger/VoIP") ) {
		    	runProg();
	        	f.tracer.repaint();
	        	f.tracer.revalidate();
	        	
			} else if ( source.getText().equalsIgnoreCase("About") ) {
			JOptionPane.showMessageDialog(f,
				  "MASS08 SCOPE DEMO v0.1,PC",
				  "About",
				  JOptionPane.INFORMATION_MESSAGE
				  );
	
			} else if ( source.getText().equalsIgnoreCase("75 %") ) {
	//			f.tracer.setZoomSize( 75.0);
	        	f.tracer.repaint();
	        	f.tracer.revalidate();
	
			} else if ( source.getText().equalsIgnoreCase("100 %") ) {
	//			f.tracer.setZoomSize( 100.0);
	        	f.tracer.repaint();
	        	f.tracer.revalidate();
	
			} else if ( source.getText().equalsIgnoreCase("125 %") ) {
	//			f.tracer.setZoomSize( 125.0);
	        	f.tracer.repaint();
	        	f.tracer.revalidate();
	
			} else if ( source.getText().equalsIgnoreCase("150 %") ) {
	//			f.tracer.setZoomSize( 150.0);
	        	f.tracer.repaint();
	        	f.tracer.revalidate();
	
			} else if ( source.getText().equalsIgnoreCase("200 %") ) {
	//			f.tracer.setZoomSize( 200.0);
	        	f.tracer.repaint();
	        	f.tracer.revalidate();
	
			} else if ( source.getText().equalsIgnoreCase("300 %") ) {
	//			f.tracer.setZoomSize(300.0);
				f.tracer.repaint();
	        	f.tracer.revalidate();
	  
			} else if ( source.getText().equalsIgnoreCase("Close") ) {
					f.dispose();
	  		
	  		} else if ( source.getText().equalsIgnoreCase("Export to Image") ) {
	  			try {
	  				
	  				ScreenImage.createImage( f.m_srollPane, "CurveTracer.jpg");
	  				//ScreenImage.createImage( f.infos, "CurveInfos.jpg");
	  				ScreenImage.createImage( f, "CurveFrame.jpg");
	  				
	  				JOptionPane.showMessageDialog(f,
	    					  "Export to JPEG Image successful",
	    					  "Curve Info",
	    					  JOptionPane.INFORMATION_MESSAGE
	    					  );
	  			} catch ( Exception ex ) {
	  				System.out.println( ex.toString() );
	  			}
	  		}
	        
	}
	  
	  public void runProg() {

		    new Thread() {
		      public void run() {

		        try {

		          pStack.push(Runtime.getRuntime().exec(Util.softPhone));

		        } catch (Exception ex) {
		        	
		        }
		      }
		    }.start();

		  }

}

/*
 * Created by JFormDesigner on Wed Jul 16 10:50:34 ICT 2008
 */

package com.int2.expeshare.test;

import java.awt.event.*;
import javax.swing.*;

import com.jgoodies.forms.layout.*;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import org.apache.xmlrpc.XmlRpcClientLite;
import org.zoolu.tools.Parser;

/**
 * @author Minh Nguyen
 */
public class ExpeshareClient extends JPanel{
	
	private static final long serialVersionUID = 1L;
	
	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner Evaluation license - Minh Nguyen
	private JLabel label3;
	private JLabel label4;
	private JSeparator separator2;
	private JLabel label5;
	private JLabel label2;
	private JTextField textField1;
	private JLabel label1;
	private JScrollPane scrollPane1;
	private JTextArea textArea1;
	private JButton button1;
	private JButton button2;
	private JSeparator separator1;
	private JLabel label6;
	private JLabel label7;
	private JTextField textField3;
	private JLabel label8;
	private JScrollPane scrollPane2;
	private JTextArea textArea2;
	private JButton button3;
	private JButton button4;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
	
	private String strKey;
	private String strValue;
	private static JFrame frame;
	private static XmlRpcClientLite client;
	private int failFlag;
	static public String via_addr=null;
	static public int host_port=0;
	
	//private static String fileName = "/home/skminh/eclipse/skminh_project/Expeshare/src/com/int2/expeshare/test/gateway.cfg";
	private static String fileName = "./config/gateway.cfg";
	public ExpeshareClient() {
		initComponents();
		textArea2.setEditable(false);
	}
	
	private void button1ActionPerformed(ActionEvent e) {
		// TODO add your code here
		
	    	Vector params = new Vector ();
	
	        byte [] key = new byte [128];
	        strKey = textField1.getText();
	        if ( strKey.equalsIgnoreCase("") ) {
	        	
		    	failFlag = 0;
	        	JOptionPane.showMessageDialog(frame,
						  "Please input key and text!",
						  "Input key",
						  JOptionPane.ERROR_MESSAGE
						  );
	        } else {
	        	failFlag = 1;
		        key = strKey.getBytes();
		        params.add (key);
		
		        byte [] value = new byte [1024];
		        strValue = textArea1.getText();
		        value = strValue.getBytes();
		        params.add (value);
		
		        Integer ttl_sec = new Integer (120);
		        params.add (ttl_sec);
		
		        String application = "XmlRpcTest";
		        params.add (application);
		        
			    try {
			        Integer put_result = (Integer) client.execute ("put", params);
			        
			        //if (put_result == bamboo.dht.bamboo_stat.BAMBOO_OK)
			        if (put_result == 0)
			        	JOptionPane.showMessageDialog(frame,
								  "Put data into Overlay successfully!",
								  "Put Successed",
								  JOptionPane.INFORMATION_MESSAGE
								  );
			        	
			        else 
			        	JOptionPane.showMessageDialog(frame,
			        			  "Put failed! Please, check network & start program again!",
								  "Put Failed",
								  JOptionPane.ERROR_MESSAGE
								  );
			        
				} catch (Exception ex) {
					if (failFlag == 1)
						JOptionPane.showMessageDialog(frame,
								  "Put failed! Please, exit program & check network!",
								  "Put Failed",
								  JOptionPane.ERROR_MESSAGE
								  );
		        
			    }
	        }

	}

	private void button3ActionPerformed(ActionEvent e) {
		// TODO add your code here
		
		textArea2.setText("");
		
    	Vector params = new Vector ();

        byte [] key = new byte [128];
        strKey = textField3.getText();
        
        if ( strKey.equalsIgnoreCase("") ) {
        	failFlag = 0;
        	JOptionPane.showMessageDialog(frame,
					  "Please input key!",
					  "Input key",
					  JOptionPane.ERROR_MESSAGE
					  );
        } else {
        	
	        failFlag = 1;
	        
	        key = strKey.getBytes();
	        params.add (key);
	        
	        Integer maxvals = new Integer (64);
	        params.add (maxvals);
	
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
		            textArea2.append(strValue2);
		            textArea2.append("\n");
		         }
		         
	        } catch (Exception ex ) {
	        	if (failFlag == 1)
		        	JOptionPane.showMessageDialog(frame,
							  "Get failed! Please, exit program & check network!",
							  "Get Failed",
							  JOptionPane.ERROR_MESSAGE
							  );
		    }
        }
	}

	private void button2ActionPerformed(ActionEvent e) {
		// TODO add your code here
		textField1.setText("");
		textArea1.setText("");
	}

	private void button4ActionPerformed(ActionEvent e) {
		// TODO add your code here	
		textField3.setText("");
		textArea2.setText("");
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner Evaluation license - Minh Nguyen
		label3 = new JLabel();
		label4 = new JLabel();
		separator2 = new JSeparator();
		label5 = new JLabel();
		label2 = new JLabel();
		textField1 = new JTextField();
		label1 = new JLabel();
		scrollPane1 = new JScrollPane();
		textArea1 = new JTextArea();
		button1 = new JButton();
		button2 = new JButton();
		separator1 = new JSeparator();
		label6 = new JLabel();
		label7 = new JLabel();
		textField3 = new JTextField();
		label8 = new JLabel();
		scrollPane2 = new JScrollPane();
		textArea2 = new JTextArea();
		button3 = new JButton();
		button4 = new JButton();
		CellConstraints cc = new CellConstraints();

		//======== this ========

		// JFormDesigner evaluation mark
		/*setBorder(new javax.swing.border.CompoundBorder(
			new javax.swing.border.TitledBorder(new javax.swing.border.EmptyBorder(0, 0, 0, 0),
				"JFormDesigner Evaluation", javax.swing.border.TitledBorder.CENTER,
				javax.swing.border.TitledBorder.BOTTOM, new java.awt.Font("Dialog", java.awt.Font.BOLD, 12),
				java.awt.Color.red), getBorder())); addPropertyChangeListener(new java.beans.PropertyChangeListener(){public void propertyChange(java.beans.PropertyChangeEvent e){if("border".equals(e.getPropertyName()))throw new RuntimeException();}});
		*/
		setLayout(new FormLayout(
			"2*(default, $lcgap), 68dlu, $lcgap, 52dlu, 15*($lcgap, default), $lcgap",
			"6*(default, $lgap), 50dlu, 3*($lgap, default), $lgap, 22dlu, 3*($lgap, default), $lgap, 50dlu, 3*($lgap, default)"));

		//---- label3 ----
		label3.setText("--------- Overlay");
		add(label3, cc.xy(5, 3));

		//---- label4 ----
		label4.setText("Testing ------");
		add(label4, cc.xy(7, 3));
		add(separator2, cc.xywh(1, 5, 23, 1));

		//---- label5 ----
		label5.setText("Put(key, text) into overlay");
		add(label5, cc.xywh(5, 7, 3, 1));

		//---- label2 ----
		label2.setText("Key");
		add(label2, cc.xywh(3, 9, 3, 1));

		//---- textField1 ----
		textField1.setText("k1");
		add(textField1, cc.xywh(5, 9, 10, 1));

		//---- label1 ----
		label1.setText("Text");
		add(label1, cc.xy(3, 11));

		//======== scrollPane1 ========
		{

			//---- textArea1 ----
			textArea1.setText("some text..");
			scrollPane1.setViewportView(textArea1);
		}
		add(scrollPane1, cc.xywh(5, 11, 10, 7));

		//---- button1 ----
		button1.setText("Put it!");
		button1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				button1ActionPerformed(e);
			}
		});
		add(button1, cc.xy(5, 19));

		//---- button2 ----
		button2.setText("Reset");
		button2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				button2ActionPerformed(e);
			}
		});
		add(button2, cc.xywh(7, 19, 8, 1));
		add(separator1, cc.xywh(1, 21, 25, 1));

		//---- label6 ----
		label6.setText("Get from overlay");
		add(label6, cc.xy(5, 23));

		//---- label7 ----
		label7.setText("Key");
		add(label7, cc.xy(3, 25));

		//---- textField3 ----
		textField3.setText("k1");
		add(textField3, cc.xywh(5, 25, 10, 1));

		//---- label8 ----
		label8.setText("Return");
		add(label8, cc.xy(3, 27));

		//======== scrollPane2 ========
		{

			//---- textArea2 ----
			textArea2.setText("some return...");
			scrollPane2.setViewportView(textArea2);
		}
		add(scrollPane2, cc.xywh(5, 27, 10, 5));

		//---- button3 ----
		button3.setText("Get it!");
		button3.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				button3ActionPerformed(e);
			}
		});
		add(button3, cc.xy(5, 33));

		//---- button4 ----
		button4.setText("Reset");
		button4.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				button4ActionPerformed(e);
			}
		});
		add(button4, cc.xywh(7, 33, 8, 1));
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	 private static void createAndShowGUI() {
	        //Create and set up the window.
	        frame = new JFrame("Expeshare - Overlay Test Program");
	        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	        //Create and set up the content pane.
	        JComponent newContentPane = new ExpeshareClient();
	        newContentPane.setOpaque(true); //content panes must be opaque
	        frame.setContentPane(newContentPane);

	        //Display the window.
	        frame.pack();
	        frame.setSize(400,500);
	        frame.setVisible(true);
	     
	  }
	 
	 public static void readFile2( String file) {
	 try 
     {
      FileInputStream fin = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fin);
      
            // Now read the buffered stream.
            while (bis.available() > 0) 
            {
                System.out.print((char)bis.read());
            }
        
        } 
        catch (Exception e) 
        {
            System.err.println("Error reading file: " + e);
        }
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

	public static void main (String [] args) throws Exception {
     
	loadFile(fileName);  
    //List<String> lines;
    //lines = readFile(fileName);
    //readFile2(fileName);
	
	//System.out.println( via_addr );
	String strPort = Integer.toString( host_port );
	//System.out.println( strPort );
	
	String url = "http://" + via_addr + ":" + strPort;
	//System.out.println( url );
	
	//establishing XML-RPC
	client = new XmlRpcClientLite ( url );
	System.out.println("[INFO] Overlay gateway IP address is " + via_addr );
		
	//Schedule a job for the event-dispatching thread:
    //creating and showing this application's GUI.
		
	javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            createAndShowGUI();
        }
    });
    }
}
package curves;

/**
 * @author skminh9
 * @date 2008/09/21
 */
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.ListIterator;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import javax.swing.JPanel;

import util.Util;

import model.Account;
import model.Community;
import model.Service;

/**
 * 
 * @author Julien Nguyen
 * 
 */

class CurveTracer extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private JScrollPane scrollPane;	//account
	private	JScrollPane scrollPane2; //service
	private	JScrollPane scrollPane3; //community
	private	JScrollPane scrollPane4; //sip users

	//account
	private JLabel usernameLabel; 
	private JLabel passwordLabel;
	private JLabel fullnameLabel;
    private JLabel companyLabel;
    private JLabel countryLabel;
    private JLabel interestLabel;
    private JLabel communityLabel;
    private JLabel emailLabel;
    
    private JTextField usernameEdit; 
	private JPasswordField passwordEdit;
	private JTextField fullnameEdit;
    private JTextField companyEdit;
    private JTextField countryEdit;
    private JTextField interestEdit;
    private JComboBox communityCombo;
    private JTextField emailEdit;
    private JButton inputAccountButton;
    
    //community
    private JLabel communityLabel2; 
	private JLabel creatorLabel;
	private JLabel interestLabel2;
	
    private JTextField communityEdit;
    private JTextField creatorEdit;
    private JTextField interestEdit2;
    private JButton inputCommunityButton;
    
    //service
    private JLabel protoLabel; 
	private JLabel ipLabel;
	private JLabel portLabel;
	private JLabel commentLabel;
	
	
    private JTextField protoEdit;
    private JTextField ipEdit;
    private JTextField portEdit;
    private JTextField commentEdit;
    private JButton inputServiceButton;
    
    private Account acc;
    private Community com;
    private Service ser;
    
    int cur; //0: input acc, 1: input ser, 2: input com, 3: table...
    
    Util util;
	private CurveFrame f;
	
	private List<String> lines;
	
	public CurveTracer(final CurveFrame f) {
        
		//super(new GridLayout(0,2));
		this.f = f;
		cur = -1;
		acc = new Account();
		com = new Community();
		ser = new Service();
		
		//connect to overlay
		util = new Util();
		util.connect2Overlay();
		
		createAccountTable();
		createServiceTable();
		createCommunityTable();
		createInputAccount();
		createInputCommunity();
		createInputService();
		createSIPUsersTable();
		
		showStartScreen();
		
		inputAccountButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ae ) {
				System.out.println("--- add account ---");
				dhtCreateAccount();
			}
		});
		
		inputCommunityButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ae ) {
				System.out.println("--- add community ---");
				dhtCreateCommunity();
			}
		});
		
		inputServiceButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ae ) {
				System.out.println("--- add service ---");
				dhtCreateService();
			}
		});
		

	}	
		public void showAccounts() {
			//getAccount from DHT
			//put to the Table
			//hidden 2 table
			//show table account
	        removeRest(cur);
			dhtGetAccounts();
			createAccountTable();
	        add(scrollPane);
	        cur = Util.CUR_SHOW_ACC;
		}
		
		public void showCommunities() {
			//getAccount from DHT
			//put to the Table
			//hidden 2 table
			//show table account
			removeRest(cur);
			dhtGetCommunities();
			createCommunityTable();
	        add(scrollPane3);
	        cur = Util.CUR_SHOW_COM;
		}
		
		public void showStartScreen() {
			//getAccount from DHT
			//put to the Table
			//hidden 2 table
			//show table account
			//removeRest(cur);
			createCommunityTable();
	        add(scrollPane3);
	        cur = Util.CUR_SHOW_COM;
		}
		
		public void showServices() {
			//getAccount from DHT
			//put to the Table
			//hidden 2 table
			//show table account
			removeRest(cur);
			dhtGetServices();
			createServiceTable();
	        add(scrollPane2);
	        cur = Util.CUR_SHOW_SER;
		}
		
		public void showSIPUsers() {
			//getAccount from DHT
			//put to the Table
			//hidden 2 table
			//show table account
			removeRest(cur);
			dhtGetSIPUsers();
			createSIPUsersTable();
	        add(scrollPane4);
	        cur = Util.CUR_SHOW_SIPUSERS;
		}
		
		public void createAccountTable() {
			//--table account
	        String[] columnAccount = {"Username",
	                                "University/Company",
	                                "Country",
	                                "Interest",
	                                "Community"};
	                                //"Email"};
	        
			//return values
	        //System.out.println("show account...");	        
			if (lines != null ) {
				//System.out.println("here?");
				ListIterator<String> li = lines.listIterator();
				String line = null;
				int vSize = lines.size();
			    String[][] data = new String[vSize][5];
			    
				int row = 0;
				while (li.hasNext()) {
					line = (String)li.next();
					//System.out.println( line );
					StringTokenizer st = new StringTokenizer(line,"|");

				    //prepare data
				    int col = 0; int tok = 0;
		            while (st.hasMoreTokens()) {
		            	 //!care ful when calling nextToken()
		            	 //System.out.println( "token: " + st.nextToken() );
		            	 if ( tok != 1 && tok !=2 && tok!=7 ) {
		            		 data[row][col]= st.nextToken();
		            		 col++;
		            	 } else {
		            		 st.nextToken();
		            	 }
		            	 tok++;
		            }
		            row++;
					
				}
				
				JTable table = new JTable(data, columnAccount) 
		        {
		            /**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					public boolean isCellEditable(int rowIndex, int vColIndex) {
		                return false;

		            }
		        };
		        
		        //table.setPreferredScrollableViewportSize(new Dimension(500, 70));
		        table.setFillsViewportHeight(true);
		        table.setVisible(true);

		        //Create the scroll pane and add the table to it.
		        scrollPane = new JScrollPane(table);
		        scrollPane.setAutoscrolls(true);
		    
			} else { //line == null or initilization
				Object[][] data = {
						{"-", "-" }
					    };
				JTable table = new JTable(data, columnAccount) 
		        {
		           
					private static final long serialVersionUID = 1L;

					public boolean isCellEditable(int rowIndex, int vColIndex) {
		                return false;

		            }
		        };
		        
		        //table.setPreferredScrollableViewportSize(new Dimension(500, 70));
		        table.setFillsViewportHeight(true);
		        table.setVisible(true);

		        //Create the scroll pane and add the table to it.
		        scrollPane = new JScrollPane(table);
		        scrollPane.setAutoscrolls(true);
				
			}
	        
		}
		
		public void createServiceTable() {
			 //---service table
	        String[] columnService = {"Protocol",
	                "IP",
	                "Port",
	                "Comments"};

	        if (lines != null ) {
				//System.out.println("here?");
				ListIterator<String> li = lines.listIterator();
				String line = null;
				int vSize = lines.size();
			    String[][] data = new String[vSize+1][4];
			    
			    data[0][0] = "SIP";
			    data[0][1] = "169.254.68.167";
			    data[0][2] = "5060";
			    data[0][3] = "a SIP SuperNode";
			    
				int row = 1;
				while (li.hasNext()) {
					line = (String)li.next();
					//System.out.println( line );
					StringTokenizer st = new StringTokenizer(line,"|");

				    //prepare data
				    int col = 0;
		            while (st.hasMoreTokens()) {
		            	 //!care ful when calling nextToken()
		            	 //System.out.println( "token: " + st.nextToken() );
		            		 data[row][col]= st.nextToken();
		            	
		            	 col++;
		            }
		            row++;
					
				}
				
				JTable table = new JTable(data, columnService) 
		        {
		            /**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					public boolean isCellEditable(int rowIndex, int vColIndex) {
		                return false;

		            }
		        };
		        
		        //table.setPreferredScrollableViewportSize(new Dimension(500, 70));
		        table.setFillsViewportHeight(true);
		        table.setVisible(true);

		        //Create the scroll pane and add the table to it.
		        scrollPane2 = new JScrollPane(table);
		        scrollPane2.setAutoscrolls(true);
		    
			} else { //line == null or initilization
				Object[][] data = {
						{"SIP", "169.254.68.167", "5060","a SIP SuperNode" }
					    };
				JTable table = new JTable(data, columnService) 
		        {
		           
					private static final long serialVersionUID = 1L;

					public boolean isCellEditable(int rowIndex, int vColIndex) {
		                return false;

		            }
		        };
		        
		        //table.setPreferredScrollableViewportSize(new Dimension(500, 70));
		        table.setFillsViewportHeight(true);
		        table.setVisible(true);

		        //Create the scroll pane and add the table to it.
		        scrollPane2 = new JScrollPane(table);
		        scrollPane2.setAutoscrolls(true);
				
			}
		}
		
		
		public void createSIPUsersTable() {
			 //---sip users table
	        String[] columnSIPUsers = {"Username",
	        						  "Contact"};
	
	        if (lines != null ) {
				//System.out.println("here?");
				ListIterator<String> li = lines.listIterator();
				String line = null;
				int vSize = lines.size();
			    String[][] data = new String[vSize][2];
			 
				int row = 0;
				while (li.hasNext()) {
					line = (String)li.next();
					//System.out.println( line );
					StringTokenizer st = new StringTokenizer(line,"@");
	
				    //prepare data
				    int col = 0;
		            while (st.hasMoreTokens()) {
		            	 //!care ful when calling nextToken()
		            	 //System.out.println( "token: " + st.nextToken() );
		            		 data[row][col]= st.nextToken();
		            	
		            	 col++;
		            }
		            row++;
					
				}
				
				JTable table = new JTable(data, columnSIPUsers) 
		        {
		            /**
					 * 
					 */
					private static final long serialVersionUID = 1L;
	
					public boolean isCellEditable(int rowIndex, int vColIndex) {
		                return false;
	
		            }
		        };
		        
		        //table.setPreferredScrollableViewportSize(new Dimension(500, 70));
		        table.setFillsViewportHeight(true);
		        table.setVisible(true);
	
		        //Create the scroll pane and add the table to it.
		        scrollPane4 = new JScrollPane(table);
		        scrollPane4.setAutoscrolls(true);
		    
			} else { //line == null or initilization
				Object[][] data = {
						{"-", "-" }
					    };
				JTable table = new JTable(data, columnSIPUsers) 
		        {
		           
					private static final long serialVersionUID = 1L;
	
					public boolean isCellEditable(int rowIndex, int vColIndex) {
		                return false;
	
		            }
		        };
		        
		        //table.setPreferredScrollableViewportSize(new Dimension(500, 70));
		        table.setFillsViewportHeight(true);
		        table.setVisible(true);
	
		        //Create the scroll pane and add the table to it.
		        scrollPane4 = new JScrollPane(table);
		        scrollPane4.setAutoscrolls(true);
			}
			
		}
		
		public void createCommunityTable() {
			//--table communities
			String[] columnCommunity = {"Community",
	                "Creator",
	                "Interest"};

			 if (lines != null ) {
					//System.out.println("here?");
					ListIterator<String> li = lines.listIterator();
					String line = null;
					int vSize = lines.size();
				    String[][] data = new String[vSize+1][3];
				    
				    data[0][0] = "Sensor Systems";
				    data[0][1] = "mass08";
				    data[0][2] = "talk about Sensor Systems";

					int row = 1;
					while (li.hasNext()) {
						line = (String)li.next();
						//System.out.println( line );
						StringTokenizer st = new StringTokenizer(line,"|");

					    //prepare data
					    int col = 0;
			            while (st.hasMoreTokens()) {
			            	 //!care ful when calling nextToken()
			            	 //System.out.println( "token: " + st.nextToken() );
			            		 data[row][col]= st.nextToken();
			            	
			            	 col++;
			            }
			            row++;
						
					}
					
					JTable table = new JTable(data, columnCommunity) 
			        {
			            /**
						 * 
						 */
						private static final long serialVersionUID = 1L;

						public boolean isCellEditable(int rowIndex, int vColIndex) {
			                return false;

			            }
			        };
			        
			        //table.setPreferredScrollableViewportSize(new Dimension(500, 70));
			        table.setFillsViewportHeight(true);
			        table.setVisible(true);

			        //Create the scroll pane and add the table to it.
			        scrollPane3 = new JScrollPane(table);
			        scrollPane3.setAutoscrolls(true);
			    
				} else { //line == null or initilization
					Object[][] data = {
							{"Sensor Systems", "mass08", "talk about Sensor Systems" }
						    };
					JTable table = new JTable(data, columnCommunity) 
			        {
			           
						private static final long serialVersionUID = 1L;

						public boolean isCellEditable(int rowIndex, int vColIndex) {
			                return false;

			            }
			        };
			        
			        //table.setPreferredScrollableViewportSize(new Dimension(500, 70));
			        table.setFillsViewportHeight(true);
			        table.setVisible(true);

			        //Create the scroll pane and add the table to it.
			        scrollPane3 = new JScrollPane(table);
			        scrollPane3.setAutoscrolls(true);
					
				}
		}
		
		public void createInputAccount() {
			
			usernameLabel = new JLabel("Username  "); 
			passwordLabel= new JLabel("Password  ");
			fullnameLabel= new JLabel("Full name ");
		    companyLabel= new JLabel("University/Company");
		    countryLabel= new JLabel("Country   ");
		    interestLabel= new JLabel("Interest  ");
		    communityLabel= new JLabel("Community");
		    emailLabel= new JLabel("Email        ");
		    
		    usernameEdit = new JTextField("");
		    usernameEdit.setPreferredSize( new Dimension(380, 20 ) );
			passwordEdit = new JPasswordField("");
			passwordEdit.setPreferredSize( new Dimension(380, 20 ) );
			passwordEdit.setEchoChar('*');
			fullnameEdit = new JTextField("");
			fullnameEdit.setPreferredSize( new Dimension(380, 20 ) );
			companyEdit = new JTextField("");
			companyEdit.setPreferredSize( new Dimension(335, 20 ) );
		    countryEdit = new JTextField("");
		    countryEdit.setPreferredSize( new Dimension(380, 20 ) );
		    interestEdit = new JTextField("");
		    interestEdit.setPreferredSize( new Dimension(380, 20 ) );
		    communityCombo = new JComboBox();
		    communityCombo.setPreferredSize( new Dimension(380, 20 ) );
		    communityCombo.addItem(makeObj("Sensor Systems"));
		    emailEdit = new JTextField("-");
		    emailEdit.setPreferredSize( new Dimension(380, 20 ) );
		    inputAccountButton = new JButton("--- OK ---");
		    
		}
		
		public void updateCombobox() {
			
			communityCombo.removeAllItems();
			communityCombo.addItem(makeObj("Sensor Systems"));
			dhtGetCommunities();
			
			if (lines != null ) {
					//System.out.println("here?");
					ListIterator<String> li = lines.listIterator();
					String line = null;

					while (li.hasNext()) {
						line = (String)li.next();
						//System.out.println( line );
						StringTokenizer st = new StringTokenizer(line,"|");
						boolean esc = false;
			            while (st.hasMoreTokens() && esc==false) {
			            	 //!care ful when calling nextToken()
			            	 //System.out.println( "token: " + st.nextToken() );
			            	 communityCombo.addItem(makeObj(st.nextToken()));
			            	 esc = true;
			            }		
					}
			}
		}

		public void showInputAccount() {
			
			removeRest(cur);
			//add for combo box
		    updateCombobox();
			addInputAccount();
			cur = Util.CUR_INPUT_ACC;
		}
		
		public void showInputCommunity() {
			
			removeRest(cur);
			addInputCommunity();
			cur = Util.CUR_INPUT_COM;
		}

		public void showInputService() {
		
			removeRest(cur);
			addInputService();
			cur = Util.CUR_INPUT_SER;
	}
		
		public void addInputAccount() {
			add(usernameLabel);
			add(usernameEdit);
			add(passwordLabel);
			add(passwordEdit);
			add(fullnameLabel);
			add(fullnameEdit);
			add(companyLabel);
			add(companyEdit);
			add(countryLabel);
			add(countryEdit);
			add(interestLabel);
			add(interestEdit);
			add(communityLabel);
			add(communityCombo);
			add(emailLabel);
			add(emailEdit);
			add(inputAccountButton);
		}
		
		public void addInputCommunity() {
			add(communityLabel2);
			add(communityEdit);
			add(creatorLabel);
			add(creatorEdit);
			add(interestLabel);
			add(interestEdit2);
			add(inputCommunityButton);
		}
		
		public void addInputService() {
			add(protoLabel);
			add(protoEdit);
			add(ipLabel);
			add(ipEdit);
			add(portLabel);
			add(portEdit);
			add(commentLabel);
			add(commentEdit);
			add(inputServiceButton);
		}
		
		public void removeInputAccount() {
			remove(usernameLabel);
			remove(usernameEdit);
			remove(passwordLabel);
			remove(passwordEdit);
			remove(fullnameLabel);
			remove(fullnameEdit);
			remove(companyLabel);
			remove(companyEdit);
			remove(countryLabel);
			remove(countryEdit);
			remove(interestLabel);
			remove(interestEdit);
			remove(communityLabel);
			remove(communityCombo);
			remove(emailLabel);
			remove(emailEdit);
			remove(inputAccountButton);
		}
		
		public void removeInputCommunity() {
			remove(communityLabel2);
			remove(communityEdit);
			remove(creatorLabel);
			remove(creatorEdit);
			remove(interestLabel);
			remove(interestEdit2);
			remove(inputCommunityButton);
		}
		
		public void removeInputService() {
			remove(protoLabel);
			remove(protoEdit);
			remove(ipLabel);
			remove(ipEdit);
			remove(portLabel);
			remove(portEdit);
			remove(commentLabel);
			remove(commentEdit);
			remove(inputServiceButton);
		}
		
		public void removeRest(int rcur) {
			if (rcur == Util.CUR_INPUT_ACC) {
				removeInputAccount();
			}
			else if (rcur == Util.CUR_INPUT_COM) {
				removeInputCommunity();			
			}
			else if (rcur == Util.CUR_INPUT_SER) {
				removeInputService();
			}
			else if (rcur == Util.CUR_SHOW_ACC) {
				remove(scrollPane);
			}
			else if (rcur == Util.CUR_SHOW_COM) {
				remove(scrollPane3);
			} else if (rcur == Util.CUR_SHOW_SIPUSERS) {
				remove(scrollPane4);
			} else {
				remove(scrollPane2);
			}
		}
		
		public void createInputCommunity() {
			
			communityLabel2 = new JLabel("Community "); 
			creatorLabel= new JLabel("Creator    ");
			interestLabel= new JLabel("Interest   ");
		   
		    communityEdit = new JTextField("");
		    communityEdit.setPreferredSize( new Dimension(380, 20 ) );
			creatorEdit = new JTextField("");
			creatorEdit.setPreferredSize( new Dimension(380, 20 ) );
			interestEdit2 = new JTextField("");
			interestEdit2.setPreferredSize( new Dimension(380, 20 ) );
		    inputCommunityButton = new JButton("--- Add ---");
		    
		}
		
		
		public void createInputService() {
			
			protoLabel = new JLabel("Protocol    "); 
			ipLabel= new JLabel("IP            ");
			portLabel= new JLabel("Port         ");
			commentLabel= new JLabel("Comment  ");
		   
		    protoEdit = new JTextField("");
		    protoEdit.setPreferredSize( new Dimension(380, 20 ) );
			ipEdit = new JTextField("");
			ipEdit.setPreferredSize( new Dimension(380, 20 ) );
			portEdit = new JTextField("");
			portEdit.setPreferredSize( new Dimension(380, 20 ) );
			commentEdit = new JTextField("");
			commentEdit.setPreferredSize( new Dimension(380, 20 ) );
		    inputServiceButton = new JButton("--- Add ---");
		}
		
	   private Object makeObj(final String item)  {
	     return new Object() { public String toString() { return item; } };
	   }
	   
	   private void dhtCreateAccount() {
		   
		   acc.username = usernameEdit.getText();
		   char pass[] = passwordEdit.getPassword();
		   acc.password = new String (pass);
		   acc.fullname = fullnameEdit.getText();
		   acc.company = companyEdit.getText();
		   acc.country = countryEdit.getText();
		   acc.interest = interestEdit.getText();
		   acc.community = communityCombo.getSelectedItem().toString();
		   acc.email = emailEdit.getText();
		   
		   if (verifyAccountFields()==false) return;
		   
		   String info = acc.username + "|" 
		   				+ acc.password + "|"
		   				+ acc.fullname + "|"
		   				+ acc.company + "|"
		   				+ acc.country + "|"
		   				+ acc.interest + "|"
		   				+ acc.community + "|"
		   				+ acc.email ;
		   
		   //System.out.println(info);
		   try {
			   
			int put_result = util.put(Util.MASS08_ACC_KEY, info, new String("36000"));
			
			if (put_result == 0)
		        	JOptionPane.showMessageDialog(f,
							  "Put data into Overlay successfully!",
							  "Put Successed",
							  JOptionPane.INFORMATION_MESSAGE
							  );
		        	
		        else 
		        	JOptionPane.showMessageDialog(f,
		        			  "Put failed! Please, check network & IP!",
							  "Put Failed",
							  JOptionPane.ERROR_MESSAGE
							  );
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	   }
	   
	   private void dhtCreateCommunity() {
		   
		   com.community = communityEdit.getText();
		   com.creator = creatorEdit.getText();
		   com.interest = interestEdit2.getText();
		   
		   if (verifyCommunityFields()==false) return;
		   
		   String info = com.community + "|" 
		   				+ com.creator + "|"
		   				+ com.interest;
		   				
		   
		   //System.out.println(info);
		   try {
			int put_result = util.put(Util.MASS08_COM_KEY, info, new String("36000"));
			
			if (put_result == 0)
	        	JOptionPane.showMessageDialog(f,
						  "Put data into Overlay successfully!",
						  "Put Successed",
						  JOptionPane.INFORMATION_MESSAGE
						  );
	        	
	        else 
	        	JOptionPane.showMessageDialog(f,
	        			  "Put failed! Please, check network & IP!",
						  "Put Failed",
						  JOptionPane.ERROR_MESSAGE
						  );
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	   }
	   
	   private void dhtCreateService() {
		   
		   ser.protocol = protoEdit.getText();
		   ser.ip = ipEdit.getText();
		   ser.port = portEdit.getText();
		   ser.comments = commentEdit.getText();
		   
		   if (verifyServiceFields()==false) return;
		   
		   String info = ser.protocol + "|" 
		   				+ ser.ip + "|"
		   				+ ser.port + "|"
		   				+ ser.comments ;
		   
		   //System.out.println(info);
		   try {
			int put_result = util.put(Util.MASS08_SER_KEY, info, new String("36000"));
			if (put_result == 0)
	        	JOptionPane.showMessageDialog(f,
						  "Put data into Overlay successfully!",
						  "Put Successed",
						  JOptionPane.INFORMATION_MESSAGE
						  );
	        	
	        else 
	        	JOptionPane.showMessageDialog(f,
	        			  "Put failed! Please, check network & IP!",
						  "Put Failed",
						  JOptionPane.ERROR_MESSAGE
						  );
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	   }
	   
	   public boolean verifyAccountFields()
	   {
	      String s = "";
	      
	      if (acc.username.length() == 0)    s += " Username";
	      if (acc.password.length() == 0)   s += "| Password";
	      if (acc.fullname.length() == 0) s += "| Full name";
	      if (acc.company.length() == 0)    s += "| Company";
	      if (acc.country.length() == 0) s += "| Country";
	      if (acc.interest.length() == 0)     s += "| Interest";
	      if (acc.community.length() == 0)    s += "| Community";
	      if (acc.email.length() == 0)    s += "| Email";
	  
	      if (s.length() > 0)
	      {
	    		JOptionPane.showMessageDialog(f,
  					  "Error! Please fill in the following fields:|" + s,
  					  "Error",
  					  JOptionPane.ERROR_MESSAGE
  					  );
	         return false;
	      }
	      else
	         return true;
	   }
	   
	   public boolean verifyCommunityFields()
	   {
	      String s = "";
	      
	      if (com.community.length() == 0)    s += " Community |";
	      if (com.creator.length() == 0)  	  s += " Creator |";
	      if (com.interest.length() == 0) 	  s += " Interest |";
	  
	      if (s.length() > 0)
	      {
	    		JOptionPane.showMessageDialog(f,
  					  "Error! Please fill in the following fields: " + s,
  					  "Error",
  					  JOptionPane.ERROR_MESSAGE
  					  );
	         return false;
	      }
	      else
	         return true;
	   }
	   
	   public boolean verifyServiceFields()
	   {
	      String s = "";
	      
	      if (ser.protocol.length() == 0)    s += " Protocol |";
	      if (ser.ip.length() == 0)  	  s += " IP |";
	      if (ser.port.length() == 0) 	  s += " Port |";
	      if (ser.comments.length() == 0) 	  s += " Comments |";
	      
	      if (s.length() > 0)
	      {
	    		JOptionPane.showMessageDialog(f,
  					  "Error! Please fill in the following fields: " + s,
  					  "Error",
  					  JOptionPane.ERROR_MESSAGE
  					  );
	         return false;
	      }
	      else
	         return true;
	   }
	   
	   private void dhtGetAccounts() {
		 //get

		try {
			lines = util.get(Util.MASS08_ACC_KEY, new String("512") );
			if (lines == null)
				System.out.println("[ERROR] DHT-Get fail");
			else
				System.out.println("[INFO] DHT-Get succeed");
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	   }
	   
	   private void dhtGetCommunities() {
			 //get

			try {
				lines = util.get(Util.MASS08_COM_KEY, new String("512"));
				if (lines == null)
					System.out.println("[ERROR] DHT-Get fail");
				else
					System.out.println("[INFO] DHT-Get succeed");
			
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		   }
	   
	   private void dhtGetServices() {
			 //get

			try {
				lines = util.get(Util.MASS08_SER_KEY, new String("512") );
				if (lines == null)
					System.out.println("[ERROR] DHT-Get fail");
				else
					System.out.println("[INFO] DHT-Get succeed");
			
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		   }
	   
	   	private void dhtGetSIPUsers() {
			 //get

			try {
				lines = util.get(Util.MASS08_SIP_KEY, new String("512") );
				if (lines == null)
					System.out.println("[ERROR] DHT-Get fail");
				else
					System.out.println("[INFO] DHT-Get succeed");
			
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		   }
	   
	}

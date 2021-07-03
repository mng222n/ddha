package curves;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.Color;

import java.util.List;
import java.util.ListIterator;
import java.util.Stack;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
//import javax.swing.JSlider;
import javax.swing.JTextArea;

import function.FunVariations;
import function.Function;
import function.Functions;



/**
 * @author casteran
 */

class CurveControls extends JPanel {

	private static final long serialVersionUID = 1L;
	
	Stack<Process> pStack = new Stack<Process>();
	
	protected final static Integer nStepsChoices[] = { 1, 2, 3, 4, 5, 10, 20,
			40, 80, 160, 320, 640 };

	private JComboBox cb;
	
	private List<FunctionVariations> fvarArr;

	CurveControls(final FunctionVariations var, final CurveFrame f, final List<FunctionVariations> fvarArr) {
		super();
		
		this.fvarArr = fvarArr;	
		
		JPanel precision = new JPanel();
		JLabel title = new JLabel("Communities");
		cb = new JComboBox(nStepsChoices);

		//setBackground( Color.orange );
		//precision.setBackground( Color.orange );
		precision.setLayout(new GridLayout(0, 1) );
		
		precision.add( new JLabel() );
		
		precision.add(title);
		precision.add(cb);
		
		precision.add( new JLabel() );
		
		JLabel title2 = new JLabel("User account");
		final JTextField text = new JTextField("");
		text.setPreferredSize( new Dimension(100, 20 ) );
		//text.setLocation( 20, 100 );
		
		JButton button = new JButton("Invite to Chat");
		
		precision.add( title2 );
		precision.add( text );
		precision.add(button);
		
		precision.remove(button);
		precision.add(button);
		
		JButton button2 = new JButton("Chat/Call");
		precision.add( new JLabel() );
		precision.add( new JLabel() );
		precision.add( button2 );
		
		add(precision);
		
		button.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ae ) {
				
				System.out.println( "[" +  text.getText() + "]" );
				f.addFunction( text.getText() );
			}
		});
		
		button2.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ae ) {
				//f.reset();
				runProg();
			}
		});
		
		cb.addItemListener((ItemListener) (new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					
					ListIterator li = fvarArr.listIterator();
					FunctionVariations fvar;
					
					while ( li.hasNext() ) {
						fvar = (FunctionVariations)li.next();
						//var.tabulate(CurveControls.this.currentPrecision());
						fvar.tabulate(CurveControls.this.currentPrecision());
						
					}
					//f.infos.update();
					//f.infos.setVisible(true);
					f.repaint();
				}
			}

		}));
		cb.setSelectedIndex(nStepsChoices.length / 2);
	}

	int currentPrecision() {
		return nStepsChoices[cb.getSelectedIndex()];
	}
	
	 /** Start the help, in its own Thread. */
	  public void runProg() {

	    new Thread() {
	      public void run() {

	        try {

	          pStack.push(Runtime.getRuntime().exec("D://Program Files//CounterPath//X-Lite//x-lite.exe"));

	        } catch (Exception ex) {
	        	
	        }
	      }
	    }.start();

	  }

}

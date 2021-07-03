package curves;

/**
 * @author me
 * @seen 9, April 2007
 * 
 */

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JToolBar;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;

import function.FunVariations;
import function.Function;
import function.Functions;

import util.Util;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

public class CurveToolBar extends JToolBar implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	private JButton[] buttons;
	private CurveFrame f;
	
	Stack<Process> pStack = new Stack<Process>();
	
	CurveToolBar ( final CurveFrame f) { 
		
		this.f = f;
		
		String[] iconFiles = { 
				/*"D://eclipse//skproject//Courbes2//curves//icons//new.gif", 
		   		"D://eclipse//skproject//Courbes2//curves//icons//open.gif", 
		   		"D://eclipse//skproject//Courbes2//curves//icons//zin.gif", 
		   		"D://eclipse//skproject//Courbes2//curves//icons//zout.gif", 
				"D://eclipse//skproject//Courbes2//curves//icons//new.gif" }; 
				*/
		   		".//icons//001_01.gif", 
		   		".//icons//001_46.gif", 
		   		".//icons//001_40.gif",
		   		".//icons//001_57.gif", 
				".//icons//001_32.gif" 
		   		}; 

				
		String[] buttonLabels = {  "New Account", "View Services", "View Communities","View Users", "Instant Messenger/VoIP" };
		ImageIcon[] icons = new ImageIcon[iconFiles.length];
		buttons = new JButton[ buttonLabels.length ];
		for (int i = 0; i < buttonLabels.length; ++i) {
			icons[i] = new ImageIcon(iconFiles[i]);
			buttons[i] = new JButton(icons[i]);
			buttons[i].setToolTipText(buttonLabels[i]);
			buttons[i].addActionListener(this);
			this.add(buttons[i]);
		}
	}
	
	public void actionPerformed(ActionEvent e)
    {
		if (e.getSource().equals(buttons[0])) 
		{
			f.tracer.showInputAccount();
    		f.tracer.repaint();
        	f.tracer.revalidate();
			
		}
		else if(e.getSource().equals(buttons[1]))
        {
			f.tracer.showServices();
    		f.tracer.repaint();
        	f.tracer.revalidate();
        }
        else if(e.getSource().equals(buttons[2]))
        {
        	f.tracer.showCommunities();
    		f.tracer.repaint();
        	f.tracer.revalidate();
        }
        else if(e.getSource().equals(buttons[3]))
        {
        	
        	f.tracer.showAccounts();
        	f.tracer.repaint();
        	f.tracer.revalidate();
        }
		
        else if(e.getSource().equals(buttons[4]))
        {
        	runProg();
    		f.tracer.repaint();
        	f.tracer.revalidate();
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

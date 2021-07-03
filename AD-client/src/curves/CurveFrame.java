package curves;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
//import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import function.FunVariations;
import function.Function;
import function.Functions;

/**
 * A class  to represent the variations of some function  in some interval.
 * Allows some control on the accuracy of this representation
 */

/**
 * @author casteran
 */
public class CurveFrame extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected int index;
	
	static int showedCount = 0;

	/** the Menu Bar */
	protected CurveMenuBar menu;
	
	/** the Tool Bar */
	protected CurveToolBar tool;
	
	/** the graphic part of the display */
	protected CurveTracer tracer;

	/** information bar */
	//protected CurveInfos infos;

	/** various commands */
	protected CurveControls controls;

    protected JScrollPane m_srollPane;
    
	private List<FunctionVariations> fvarArr;	//store functions object
	private List<String> fstrArr;		//store string of function
	
	/**
	 * Builds a top-level window from the varaiations of a function
	 * 
	 * @see FunctionVariations
	 */
	public CurveFrame(FunctionVariations fvar) {
		super("MASS08 SCOPE DEMO");
		
		fvarArr = new ArrayList<FunctionVariations>();
		fstrArr = new ArrayList<String>();

		showedCount++;
		index = 0;
		
		tool = new CurveToolBar(this);
		tracer = new CurveTracer(this);
		//infos = new CurveInfos(fvar, this, fvarArr);
		controls = new CurveControls(fvar, this, fvarArr );

		JPanel mainPane = new JPanel(new BorderLayout());
		
		m_srollPane = new JScrollPane();
		m_srollPane.setPreferredSize( new Dimension( 410, 310 ) );
		//m_srollPane.setPreferredSize( new Dimension( 310, 210 ) );
        m_srollPane.getViewport().add(tracer);
		m_srollPane.setAutoscrolls(true);
		
		mainPane.add(tool, BorderLayout.NORTH);
		
		mainPane.add(m_srollPane, BorderLayout.CENTER );
		mainPane.add(tracer, BorderLayout.CENTER);
		
		//JScrollPane infoScrollPane = new JScrollPane( infos );
		//infoScrollPane.setPreferredSize(new Dimension( 400, 180 ) );
		m_srollPane.setAutoscrolls(true);
		
		//mainPane.add(infoScrollPane, BorderLayout.SOUTH);
		//mainPane.add(infos, BorderLayout.SOUTH);
		//mainPane.add(controls, BorderLayout.EAST);
		

		tracer.addMouseListener(new MouseAdapter() {
			CurveFrame cf = CurveFrame.this;

			public void mouseEntered(MouseEvent e) {
			//	cf.infos.xmouse.setText("x = " + cf.tracer.realX(e.getX()));
			//	cf.infos.ymouse.setText("y = " + cf.tracer.realY(e.getY()));
			//	cf.infos.repaint();
			}

			public void mouseExited(MouseEvent e) {
			//	cf.infos.xmouse.setText("");
			//	cf.infos.ymouse.setText("");
			//	cf.infos.repaint();
			}
			
			 public void mouseClicked(MouseEvent e) 
			    {
			        if(e.getButton() == MouseEvent.BUTTON1)
			        {
			  //          tracer.zoomIn();                 
			        }
			        else if(e.getButton() == MouseEvent.BUTTON3)
			        {
			//            tracer.zoomOut();
			        }
			        tracer.repaint();
			        tracer.revalidate();
			    }
			
			
		});
		tracer.addMouseMotionListener(new MouseMotionAdapter() {
			CurveFrame cf = CurveFrame.this;

			public void mouseMoved(MouseEvent e) {
			//	cf.infos.xmouse.setText("x = " + cf.tracer.realX(e.getX()));
			//	cf.infos.ymouse.setText("y = " + cf.tracer.realY(e.getY()));
			//	cf.infos.repaint();
			}

			public void mouseDragged(MouseEvent e) {
			//	cf.infos.xmouse.setText("x = " + cf.tracer.realX(e.getX()));
			//	cf.infos.ymouse.setText("y = " + cf.tracer.realY(e.getY()));
			//	cf.infos.repaint();
			}
		});

		
		this.addWindowListener( new WindowAdapter() {
			public void windowClosing( WindowEvent e ) {
				
			}
		});
		
		//fvar.tabulate(controls.currentPrecision());
		
		menu = new CurveMenuBar(this);
		setJMenuBar(menu);
		
		setContentPane(mainPane);
		pack();
		//infos.update();
		System.out.println( "show " + showedCount );
		setLocation( new Point( 100 + 40 * showedCount, 50 + 40 * showedCount) );
		setVisible(true);
		tracer.setVisible(true);
	}
	
	public JScrollPane getScrollPane() {
		return m_srollPane;
	}
	
	public void addFunction2( int index, String s, FunctionVariations fv ) {
		fstrArr.add(s);
		fvarArr.add( fv );
		//infos.addLabels(index);
		//infos.update();
	}
	
	public List<String> getFunctionStrings() {
		return fstrArr;
	}
	
	public void clearFunctions() {
		index = 0;
		fstrArr.clear();
		fvarArr.clear();
	}
	
	public void addFunction( String s ) {
		try {
    		if ( index < 10 ) {
				Function fc = Functions.parse( s );
	    		FunctionVariations fvar = new FunVariations(fc, -2 * Math.PI, 2 * Math.PI);
	    		fvar.tabulate( controls.currentPrecision() );
	    		addFunction2(index++, s, fvar);
	    		tracer.repaint();
	    		tracer.revalidate();
    		}
    		
		} catch (Exception ex ) {
			System.out.println( "just debug");
		}
	}
	
	public void reset() {
		clearFunctions();

		tracer.repaint();
		tracer.revalidate();
		
		//infos.removeLabels();
		//infos.update();
		//infos.revalidate();
		//infos.setVisible(true);
		
		repaint();
	}
}

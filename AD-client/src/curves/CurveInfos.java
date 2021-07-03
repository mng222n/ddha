package curves;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import java.util.ListIterator;

/**
 * 
 * @author casteran
 * 
 */

class CurveInfos extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	JLabel xmin, xmax;
	
	JLabel[] ymin, ymax;
	
	JLabel[] func;

	JLabel[] integral;
	
	JLabel xmouse, ymouse;

	FunctionVariations fvar;
	
	CurveFrame f;
	
	private List<FunctionVariations> fvarArr;

	CurveInfos(FunctionVariations fvar, final CurveFrame f,final List<FunctionVariations> fvarArr) {
		this.fvar = fvar;
		this.f = f;
		this.fvarArr = fvarArr;
		setLayout(new GridLayout(0, 2, 10, 10));
		xmin = new JLabel();
		xmax = new JLabel();
		xmouse = new JLabel();
		ymouse = new JLabel();
		
			//Maximum 10 functions on the same window
		func = new JLabel[10];
		ymin = new JLabel[10];
		ymax = new JLabel[10];
		integral = new JLabel[10];
		
		add(xmin);
		add(xmax);
		
		add(xmouse);
		add(ymouse);
		
		setBackground(Color.cyan);
	}

	void update() {
		
		xmin.setText("xmin = " + fvar.getXmin());
		xmax.setText("xmax = " + fvar.getXmax());
		
		List<String> fstrArr = f.getFunctionStrings();
		
		ListIterator li = fstrArr.listIterator();
		ListIterator li2 = fvarArr.listIterator();
		
		int n = 0;
		while (li.hasNext() && n < 10 ) {
			fvar = (FunctionVariations)li2.next();
			setColor( func[n], n);
			setColor( ymin[n], n);
			setColor( ymax[n], n);
			setColor(integral[n], n);
			func[n].setText( "Function  [  " + (String)li.next() + "  ]" );
			ymin[n].setText("ymin = " + fvar.getYmin());
			ymax[n].setText("ymax = " + fvar.getYmax());
			xmouse.setText("");
			ymouse.setText("");
			integral[n].setText("sum = " + fvar.getIntegral());
			n++;
		}
	}
	
	private void setColor( JLabel label, int n ) {
    	switch  ( n ) {
	    	case 0:
	    		label.setForeground( Color.red);
	    		break;
	    	case 1:
	    		label.setForeground( Color.green);
	    		break;
	    	case 2:
	    		label.setForeground( Color.blue);
	    		break;
	    	case 3:
	    		label.setForeground( Color.magenta);
	    		break;
	    	case 4:
	    		label.setForeground( Color.orange);
	    		break;
	    	case 5:
	    		label.setForeground( Color.yellow);
	    		break;
	    	case 6:
	    		label.setForeground( Color.black);
	    		break;
	    	case 7:
	    		label.setForeground( Color.pink);
	    		break;
	    	case 8:
	    		label.setForeground( Color.gray);
	    		break;
	    	case 9:
	    		label.setForeground( Color.darkGray);
	    		break;
	    	default:
	    		break;
    		
    	}
    }
	
	public void addLabels(int n ) {
	
		func[n] = new JLabel();
		integral[n] = new JLabel();
		ymin[n] = new JLabel();
		ymax[n] = new JLabel();
		
		add(func[n]);
		add(integral[n]);
		
		add(ymin[n]);
		add(ymax[n]);

	}
	
	public void removeLabels() {
		
		removeAll();
		
		add(xmin);
		add(xmax);
		
		add(xmouse);
		add(ymouse);
	}
}

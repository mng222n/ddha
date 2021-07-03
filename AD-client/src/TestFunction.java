import java.io.IOException;

import javax.swing.UIManager;

import curves.CurveApplication;
import function.FunVariations;
import function.Function;
import function.Functions;
import function.SyntaxErrorException;


public class TestFunction {
	public static void main(String[] args) throws SyntaxErrorException, IOException {
		//Function f = times(X, comp(SIN, times(constant(2.), X)));
	try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");

	} catch (Exception e) {
	
	       // handle exception
    }

		Function f = Functions.parse("* x cos * 2 x");
		CurveApplication.start(new FunVariations(f, -2 * Math.PI, 2 * Math.PI));
	}
}

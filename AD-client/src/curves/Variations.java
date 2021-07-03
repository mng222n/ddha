package curves;

/**
 A standard implementation of FunVariations.
 Needs to be extended by a definition of fun to  work effectively.
 * @see FunVariations
 * @see ExampleTrigo
 * @see Escalier
 */

/**
 * 
 * @author casteran
 * 
 */

public abstract class Variations implements FunctionVariations {
	private double xmin, xmax, ymin, ymax;

	private double[] table;

	private int nbStep;

	private double step;

	protected abstract double fun(double x);

	private double integralApprox;

	public final void setXmax(double xmax) {
		this.xmax = xmax;
	}

	public final void setXmin(double xmin) {
		this.xmin = xmin;
	}

	public final double getXmax() {
		return xmax;
	}

	public final double getXmin() {
		return xmin;
	}

	public double getYmin() {
		return ymin;
	}

	public double getYmax() {
		return ymax;
	}

	public final double getIntegral() {
		return integralApprox;
	}

	public final int getStepNumber() {
		return nbStep;
	}

	public double getStepWidth() {
		return step;
	}

	public double getStepValue(int i) {
		return table[i];
	}

	public boolean isConstant() {
		return ymin == ymax;
	}

	public final void tabulate(int nbStep) {
		this.nbStep = nbStep;
		table = new double[nbStep + 1];
		step = (xmax - xmin) / nbStep;
		table[0] = ymin = ymax = fun(xmin);
		integralApprox = step * table[0];
		double x = xmin + step;
		for (int i = 1; i <= nbStep; i++, x+=step) {
			double val = fun(x);
			table[i] = val;
			integralApprox += step * val;
			if (val < ymin)
				ymin = val;
			else if (val > ymax)
				ymax = val;
		}
	}

}

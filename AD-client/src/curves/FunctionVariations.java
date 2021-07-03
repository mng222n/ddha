package curves;

/**
 * represents the (discretized) variations of a numeric function .
 *  @see ExampleTrigo
 *  @see Escalier 
 */
/**
 * 
 * @author casteran
 * 
 */
public interface FunctionVariations {
	/** sets the leftmost point of the considered interval */
	public void setXmin(double xmin);

	/** sets the rightmost point of the considered interval */
	public void setXmax(double xmax);

	/** gets the leftmost point of the considered interval */
	public double getXmin();

	/** gets the rightmost point of the considered interval */
	public double getXmax();

	/** gets the minimum value of the function */
	public double getYmin();

	/** gets the maximum value of the function */
	public double getYmax();

	/** returns an approximation of the integral of fun */
	public double getIntegral();

	/** returns the number of steps in the representation */
	public int getStepNumber();

	/** returns the width of a step */
	public double getStepWidth();

	/** checks wether the (DISCRETE) function fun has a unique value */
	public boolean isConstant();

	/** returns the ith step of fun's variations */
	public double getStepValue(int i);

	/** computes the array of the variations of fun */
	public void tabulate(int nbStep);

}

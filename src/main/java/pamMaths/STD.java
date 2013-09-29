package pamMaths;

/**
 * Calculate the mean and standard deviation of 
 * an double array. 
 * @author Doug Gillespie
 *
 */
public class STD {

	private double[] data;
	
	private double mean, std;
	
	/**
	 * Calculate the mean and standard deviation using
	 * new data
	 * @param data data array
	 * @return standard deviation
	 */
	public double getSTD(double[] data) {
		calculate(data);
		return std;
	}
	
	/**
	 * Get the standard deviation calculated in a previous
	 * call to getSTD(double[] data) or getMean(double[] data)
	 * @return standard deviation
	 */
	public double getSTD() {
		return std;
	}

	/**
	 * Calculate the mean and standard deviation using
	 * new data
	 * @param data data array
	 * @return mean value
	 */
	public double getMean(double[] data) {
		calculate(data);
		return mean;
	}
	
	/**
	 * Get the mean value calculated in a previous
	 * call to getSTD(double[] data) or getMean(double[] data)
	 * @return mean
	 */
	public double getMean() {
		return mean;
	}
	
	private void calculate(double[] data) {
		this.data = data;
		int n = data.length;
		if (n == 0) {
			std = Double.NaN;
			mean = Double.NaN;
			return;
		}
		mean = 0;
		for (int i = 0; i < n; i++) {
			mean += data[i];
		}
		mean /= n;
		double d;
		std = 0;
		for (int i = 0; i < n; i++) {
			d = data[i]-mean;
			std += (d*d);
		}
		std /= (n-1);
//		std /= n;
		std = Math.sqrt(std);
	}

	/**
	 * Get the data previously set with calls to STD and Mean funcs
	 * @return data array
	 */
	public double[] getData() {
		return data;
	}

	/**
	 * Get the skew as per page 612 or numerical recipes. 
	 * @return the skew of the data
	 */
	public double getSkew() {
		double mean = getMean();
		double sig = getSTD();
		return getSkew(mean, sig);
	}
	
	public double getSkew(double mean, double sig) {
		if (sig == 0) {
			return 0.;
		}
		double c = 0;
		if (mean == 0 && sig == 0) {
			return 0;
		}
		for (int i = 0; i < data.length; i++) {
			c += Math.pow(Math.abs((data[i]-mean)/sig), 3);
		}
		return c/data.length;
	}
	
	/**
	 * Get the nth moment of the distribution.
	 * @param i moment (skew = 3rd, kurtosis = 4th, etc)
	 * @return moment
	 */
//	public double getMoment(int i) {
//		return 0;
//	}

}

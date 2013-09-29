/**
 * 
 */
package IshmaelDetector;

/**
 * @author Dave Mellinger and Hisham Qayum
 */

import java.io.Serializable;

public class EnergySumParams extends IshDetParams implements Serializable, Cloneable {

	static public final long serialVersionUID = 0;
	double f0 = 0, f1 = 1000;		//frequency range to sum over
	double ratioF0, ratioF1;		//if ratios are used, this is the denominator
	boolean useLog = false;

	@Override
	protected EnergySumParams clone() {
		return (EnergySumParams) super.clone();
	}
}

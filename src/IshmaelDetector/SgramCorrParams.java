/**
 * 
 */
package IshmaelDetector;

/**
 * @author Dave Mellinger
 *
 */

import java.io.Serializable;

public class SgramCorrParams extends IshDetParams implements Serializable, Cloneable {

	static public final long serialVersionUID = 0;
	double segment[][] = { };		//really [nSeg][4]; each row has t0,f0,t1,f1
	double spread = 100;
	boolean useLog;
	//int nSegments = 0;
	
	@Override
	protected SgramCorrParams clone() {
		return (SgramCorrParams) super.clone();
	}
}

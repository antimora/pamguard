/**
 * 
 */
package IshmaelDetector;

/**
 * @author Dave Mellinger
 *
 */
import java.io.Serializable;

public class IshDetParams implements Serializable, Cloneable {
	static public final long serialVersionUID = 0;
	String name = "";				//copied from FFTParams; not sure how used
	public String inputDataSource;
	public int channelList = 1;
	double vscale = 50;				//passed to IshDetGraphics
	double thresh = 1;				//detection threshold
	double minTime = 0;				//time required over threshold
	double refractoryTime = 0;		//minimum time until next detection

	public String getName() { return name; }

	public void setName(String name) { this.name = name; }

	@Override
	protected IshDetParams clone() {
		try {
			return (IshDetParams) super.clone();
		}
		catch (CloneNotSupportedException Ex) {
			return null;
		}
	}
}

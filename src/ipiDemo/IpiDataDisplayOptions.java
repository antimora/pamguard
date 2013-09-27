package ipiDemo;

import java.io.Serializable;

public class IpiDataDisplayOptions implements Serializable, Cloneable {

	static final long serialVersionUID = 1;

	double maxVal = 1;

	double minVal = 0;


	@Override
	protected IpiDataDisplayOptions clone() {
		try {
			return (IpiDataDisplayOptions) super.clone();
		} catch (CloneNotSupportedException Ex) {
			Ex.printStackTrace();
		}
		return null;
	}

}

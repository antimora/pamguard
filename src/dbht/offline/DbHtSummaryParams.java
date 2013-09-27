package dbht.offline;

import java.io.Serializable;

public class DbHtSummaryParams implements Serializable, Cloneable {

	public static final long serialVersionUID = 1L;

	public int intervalSeconds = 60;

	@Override
	protected DbHtSummaryParams clone() {
		try {
			return (DbHtSummaryParams) super.clone();
		}
		catch (CloneNotSupportedException e) {
			return null;
		}
	}

	
}

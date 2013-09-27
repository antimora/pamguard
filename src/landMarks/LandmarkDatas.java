package landMarks;

import java.io.Serializable;
import java.util.ArrayList;

public class LandmarkDatas implements Serializable, Cloneable {

	static public final long serialVersionUID = 0;

	private ArrayList<LandmarkData> data = new ArrayList<LandmarkData>();
	
	public int size() {
		return data.size();
	}
	
	public LandmarkData get(int index) {
		return data.get(index);
	}
	
	public void add(LandmarkData newData) {
		data.add(newData);
	}
	
	public void remove(LandmarkData oldData) {
		data.remove(oldData);
	}
	
	public void remove(int oldData) {
		data.remove(oldData);
	}
	
	public void replace(LandmarkData oldOne, LandmarkData newOne) {
		int ind = data.indexOf(oldOne);
		if (ind >= 0) {
			data.remove(ind);
		}
		if (ind >= 0) {
			data.add(ind, newOne);
		}
		else {
			data.add(newOne);
		}
	}

	@Override
	protected LandmarkDatas clone() {
		try {
			return (LandmarkDatas) super.clone();
		}
		catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
}

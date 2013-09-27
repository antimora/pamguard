package spectrogramNoiseReduction;

import java.io.Serializable;
import java.util.ArrayList;

public class SpectrogramNoiseSettings implements Serializable, Cloneable {

	static public final long serialVersionUID = 0;

	public ArrayList<Serializable> methodSettings = new ArrayList<Serializable>();
	
	public String dataSource;
	
	public int channelList = 0xF;

	private boolean[] runMethod; 

	public void clearSettings() {
		methodSettings.clear();
	}

	public void addSettings(Serializable set) {
		methodSettings.add(set);
	}

	public Serializable getSettings(int iSet) {
		if (methodSettings.size() > iSet) {
			return methodSettings.get(iSet);
		}
		return null;
	}

	public boolean isRunMethod(int iMethod) {
		if (runMethod == null || runMethod.length <= iMethod) {
			return false;
		}
		return runMethod[iMethod];
	}

	public void setRunMethod(int iMethod, boolean run) {
		boolean[] newMethodList = runMethod;
		if (newMethodList == null || runMethod.length <= iMethod) {
			newMethodList = new boolean[iMethod+1];
			if (runMethod != null) {
				for (int i = 0; i < runMethod.length; i++) {
					newMethodList[i] = runMethod[i];
				}
			}
		}
		newMethodList[iMethod] = run;
		runMethod = newMethodList;
	}

	@Override
	public SpectrogramNoiseSettings clone() {
		// TODO Auto-generated method stub
		try {
			return (SpectrogramNoiseSettings) super.clone();
		}
		catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

}

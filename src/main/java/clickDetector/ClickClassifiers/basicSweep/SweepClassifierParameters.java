package clickDetector.ClickClassifiers.basicSweep;

import java.io.Serializable;
import java.util.Vector;

public class SweepClassifierParameters implements Serializable, Cloneable {

	public static final long serialVersionUID = 1L;
	
	private Vector<SweepClassifierSet> classifierSets = new Vector<SweepClassifierSet>();

	@Override
	protected SweepClassifierParameters clone() {
		try {
			return (SweepClassifierParameters) super.clone();
		}
		catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public int getNumSets() {
		return classifierSets.size();
	}
	
	public SweepClassifierSet getSet(int i) {
		return classifierSets.get(i);
	}
	
	public void addSet(SweepClassifierSet set) {
		classifierSets.add(set);
	}
	
	public void addSet(int ind, SweepClassifierSet set) {
		classifierSets.add(ind, set);
	}
	
	public int getSetRow(SweepClassifierSet set) {
		return classifierSets.indexOf(set);
	}
	
	public void remove(SweepClassifierSet set) {
		classifierSets.remove(set);
	}
	
	public void remove(int ind) {
		classifierSets.remove(ind);
	}

}

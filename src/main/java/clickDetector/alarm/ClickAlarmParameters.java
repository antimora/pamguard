package clickDetector.alarm;

import java.io.Serializable;
import java.util.Arrays;

public class ClickAlarmParameters implements Cloneable, Serializable {

	public static final long serialVersionUID = 1L;
	private boolean[] useSpeciesList;
	private double[] speciesWeightings;
	public boolean useEchoes;
	public boolean scoreByAmplitude;

	/**
	 * @return the showSpeciesList
	 */
	public boolean getUseSpecies(int speciesIndex) {
		if (useSpeciesList != null && useSpeciesList.length > speciesIndex) {
			return useSpeciesList[speciesIndex];
		}
		makeUseSpeciesList(speciesIndex);
		return true;
	}
	
	/**
	 * @param useSpeciesList the showSpeciesList to set
	 */
	public void setSpeciesWeighting(int speciesIndex, double speciesWeight) {
		makeUseSpeciesList(speciesIndex);
		speciesWeightings[speciesIndex] = speciesWeight;
	}
	public double getSpeciesWeight(int speciesIndex) {
		if (speciesWeightings != null && speciesWeightings.length > speciesIndex) {
			return speciesWeightings[speciesIndex];
		}
		makeUseSpeciesList(speciesIndex);
		return 1;
	}
	
	/**
	 * @param useSpeciesList the showSpeciesList to set
	 */
	public void setUseSpecies(int speciesIndex, boolean showSpecies) {
		makeUseSpeciesList(speciesIndex);
		useSpeciesList[speciesIndex] = showSpecies;
	}

	private void makeUseSpeciesList(int maxIndex) {
		if (useSpeciesList == null) {
			useSpeciesList = new boolean[0];
		}
		int oldLength = useSpeciesList.length;
		if (oldLength <= maxIndex) {
			useSpeciesList = Arrays.copyOf(useSpeciesList, maxIndex + 1);
			for (int i = oldLength; i <= maxIndex; i++) {
				useSpeciesList[i] = true;
			}
		}

		if (speciesWeightings == null) {
			speciesWeightings = new double[0];
		}
		oldLength = speciesWeightings.length;
		if (oldLength <= maxIndex) {
			speciesWeightings = Arrays.copyOf(speciesWeightings, maxIndex + 1);
			for (int i = oldLength; i <= maxIndex; i++) {
				speciesWeightings[i] = 1;
			}
		}
	}

	@Override
	protected ClickAlarmParameters clone() {
		try {
			return (ClickAlarmParameters) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

}

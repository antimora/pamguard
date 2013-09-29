package whistleClassifier.training;

import java.io.Serializable;

public class BatchTrainingParams implements Serializable, Cloneable{

	public static final long serialVersionUID = 1L;

	int[] fragmentLength = new int[3];
	int [] sectionLength = new int[3];
	double[] minProbability = new double[3];

	@Override
	protected BatchTrainingParams clone() {
		try {
			return (BatchTrainingParams) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

}

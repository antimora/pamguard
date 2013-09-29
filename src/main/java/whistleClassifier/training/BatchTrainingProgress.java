package whistleClassifier.training;

public class BatchTrainingProgress {

	int nRun;
	boolean isEnd;
	int type;
	long etaMillis;
	
	public static final int NBOOTS = 1;
	public static final int ETA = 2;
	
	/**
	 * @param nRun
	 * @param isEnd
	 */
	public BatchTrainingProgress(int nRun, boolean isEnd) {
		super();
		type = NBOOTS;
		this.nRun = nRun;
		this.isEnd = isEnd;
	}

	/**
	 * @param etaMillis
	 */
	public BatchTrainingProgress(long etaMillis) {
		super();
		type = ETA;
		this.etaMillis = etaMillis;
	}
	
	
	
}

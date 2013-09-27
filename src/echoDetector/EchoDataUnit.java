package echoDetector;

import PamguardMVC.PamDataUnit;

/**
 * This data unit is for measuring multipath echo delay
 * It requires raw acoustic data and can provide an estimate 
 * of the multipath delay computed via cepstral analysis. 
 * @author Brian Miller
 */
public class EchoDataUnit extends PamDataUnit {

	/**
	 * The echo detection signal (which is computed
	 * from the raw data).
	 */
	double[] echoData;
	
	/**
	 * Duration of the echo detection signal in samples
	 */
	int duration;
	
	/**
	 * The value of the echo detection function at the time of the echoDelay
	 */
	double echoValue;
	
	/**
	 * The peak width (s) of the echo detection function provides
	 * a measure of the echo delay uncertainty. Wider peaks
	 * mean less certainty than narrow peaks.
	 */
	double echoPeakWidth;
		
	/**
	 * Echo delay is time of the highest peak of the
	 * echo detection function relative to the start of the 
	 * detection.
	 */
	double echoDelay;
	
	/**
	 * Pointer to the echo detection parameters
	 */
	EchoController echoController;

	private float dataSampleRate;
	

	public EchoDataUnit(long echoTimeMilliseconds,
						float sampleRate,
						double[] data, 
						int minIx,
						int maxIx ) {
		
		super(echoTimeMilliseconds);
		
		this.dataSampleRate = sampleRate;

		maxIx = Math.min(data.length, maxIx);

		echoData = new double[data.length];
		
		for (int i = 0; i<data.length; i++)
			echoData[i] = data[i];
		
		echoValue = 0;
		int echoDelayIx = 0;
		
		for (int i = minIx; i < maxIx; i++) {
			if (data[i] > echoValue) {
				echoValue = data[i];
				echoDelayIx = i;
			}
		}
		/*
		 *  Don't forget the factor of 2 here. This is because the
		 *  bins in the cepstrum correspond to the 1/nyquist rate 
		 *  instead of the sampling period.
		 */
		echoDelay = echoDelayIx / dataSampleRate * 2;
	
		// TODO Auto-generated constructor stub
	}
	
	public double[] getEchoData(){
		return echoData;
	}

	public double getEchoDelay(){
		return echoDelay;
	}
	
	public double getEchoValue(){
		return echoValue;
	}
	
	public double getEchoSampleRate(){
		return dataSampleRate;
	}

	public double getPeakWidth(){
		return echoPeakWidth;
	}
	protected void setPeakWidth(){
		// TODO - Put some code here to find the echo delay peak width
		echoPeakWidth = 0;
	}
	
		
	/**
	 * This is strictly for debugging purposes
	 * @param filename - name of a text file to write echo function data
	public void writeEchoData(){
		String filename = "c:\\temp\\echo.txt";
		PrintWriter out;
		try {
			out = new PrintWriter(filename);
			for (int i = 0; i < echoData.length; i++){
				out.println(echoData[i]);
			}
			out.close();
			System.out.println("Wrote echo data to " + filename);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	*/
}

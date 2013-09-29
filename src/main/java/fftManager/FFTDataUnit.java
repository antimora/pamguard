package fftManager;

import PamDetection.AcousticDataUnit;

public class FFTDataUnit extends AcousticDataUnit {

	Complex[] fftData;
	
	private int fftSlice;
	
	public FFTDataUnit(long timeMilliseconds, int channelBitmap, long startSample, long duration, Complex[] fftData, int fftSlice) {
		super(timeMilliseconds, channelBitmap, startSample, duration);
		this.fftData = fftData;
		this.fftSlice = fftSlice;
	}

	public void setInfo(long timeMilliseconds, int channelBitmap, long startSample, long duration, int fftSlice) {
		// TODO Auto-generated method stub
		super.setInfo(timeMilliseconds, channelBitmap, startSample, duration);
		this.fftSlice = fftSlice;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
//		System.out.println("FinaliseFFT data unit at " + PamCalendar.formatDateTime(System.currentTimeMillis()));
		recycleComplexData(fftData);
		super.finalize();
	}
	
	private void recycleComplexData(Complex[] fftData) {
		FFTDataBlock fftDataBlock = (FFTDataBlock) getParentDataBlock();
		if (fftDataBlock != null && fftData != null) {
			fftDataBlock.recycleComplexArray(fftData);
		}
	}

	public Complex[] getFftData() {
		return fftData;
	}

	public void setFftData(Complex[] fftData) {
		this.fftData = fftData;
	}

	public int getFftSlice() {
		return fftSlice;
	}

}

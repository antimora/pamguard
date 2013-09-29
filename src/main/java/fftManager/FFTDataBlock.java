package fftManager;

import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import PamguardMVC.AcousticDataBlock;
import PamguardMVC.PamProcess;

public class FFTDataBlock extends AcousticDataBlock<FFTDataUnit> {
	
	private int fftLength;
	
	private int fftHop;
	
	private boolean recycle = false;
	
	private int maxRecycledComplexArrays = 500;
	
	private List<Complex[]> recycledComplexData = new Vector<Complex[]>();

	/**
	 * Gain of the window function (will generally be >0 and < 1)
	 */
	private double windowGain;

	public FFTDataBlock(String dataName,
			PamProcess parentProcess, int channelMap, int fftHop, int fftLength) {
		super(FFTDataUnit.class, dataName, parentProcess, channelMap);
		this.fftHop = fftHop;
		this.fftLength = fftLength;
	}
	
	private String getLongName() {
		return String.format("%s %d pt FFT, %d hop", dataName, fftLength, fftHop);
	}

	public int getFftLength() {
		return fftLength;
	}

	public void setFftLength(int fftLength) {
		this.fftLength = fftLength;
		noteNewSettings();
	}

	public int getFftHop() {
		return fftHop;
	}

	public void setFftHop(int fftHop) {
		this.fftHop = fftHop;
		noteNewSettings();
	}


	@Override
	synchronized public void dumpBlockContents() {
		ListIterator<FFTDataUnit> listIterator = getListIterator(0);
		FFTDataUnit unit;
		System.out.println(String.format("***** Data Dump from %s *****", getDataName()));
		while (listIterator.hasNext()) {
			unit = listIterator.next();
			System.out.println(String.format("Index %d, Time %d, Channels %d, Sample %d, Duration %d",
					unit.getAbsBlockIndex(),
					unit.getTimeMilliseconds(), unit.getChannelBitmap(),
					unit.getStartSample(), unit.getDuration()));
		}
	}
	

	/* (non-Javadoc)
	 * @see PamguardMVC.PamDataBlock#clearAll()
	 */
	@Override
	public void clearAll() {
		super.clearAll();
		recycledComplexData.clear();
	}

	public void recycleComplexArray(Complex[] complexArray) {
		if (recycle && recycledComplexData.size() < maxRecycledComplexArrays) {
			recycledComplexData.add(complexArray);
		}
	}
	
	public Complex[] getComplexArray(int arrayLength) {
		Complex[] newArray = null;
		int n = recycledComplexData.size();
		if (n > 0) {
			newArray = recycledComplexData.remove(n-1);
		}
		if (newArray == null || newArray.length != arrayLength) {
			newArray = Complex.allocateComplexArray(arrayLength);
		}
		return newArray;
	}
	
	@Override
	public double getDataGain(int iChan) {
		return windowGain;
	}

	/**
	 * @return the windowGain
	 */
	public double getWindowGain() {
		return windowGain;
	}

	/**
	 * @param windowGain the windowGain to set
	 */
	public void setWindowGain(double windowGain) {
		this.windowGain = windowGain;
	}

	public void setRecycle(boolean recycle) {
		this.recycle = recycle;
	}

	public boolean isRecycle() {
		return recycle;
	}

}

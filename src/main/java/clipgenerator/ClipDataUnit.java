package clipgenerator;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;

import fftManager.Complex;
import fftManager.FastFFT;

import wavFiles.WavFile;
import wavFiles.WavHeader;

import Acquisition.AcquisitionProcess;
import PamDetection.AcousticDataUnit;
import PamUtils.PamUtils;
import PamguardMVC.PamConstants;
import Spectrogram.WindowFunction;

public class ClipDataUnit extends AcousticDataUnit {

	public String fileName;
	
	public String triggerName;
	
	public long triggerMilliseconds;

	private double[][] rawData;
		

//	/**
//	 * Constructor to use when data have gone into a wav file. 
//	 * @param timeMilliseconds
//	 * @param triggerMilliseconds
//	 * @param startSample
//	 * @param durationSamples
//	 * @param channelMap
//	 * @param fileName
//	 * @param triggerName
//	 */
//	public ClipDataUnit(long timeMilliseconds, long triggerMilliseconds,
//			long startSample, int durationSamples, int channelMap, String fileName,
//			String triggerName) {
//		super(timeMilliseconds, channelMap, startSample, durationSamples);
//		this.triggerMilliseconds = triggerMilliseconds;
//		this.fileName = fileName;
//		this.triggerName = triggerName;
//	}

	/**
	 * Constructor to use if storing data into the binary system. 
	 * @param timeMilliseconds
	 * @param triggerMilliseconds
	 * @param startSample
	 * @param durationSamples
	 * @param channelMap
	 * @param fileName
	 * @param triggerName
	 * @param rawData
	 */
	public ClipDataUnit(long timeMilliseconds, long triggerMilliseconds,
			long startSample, int durationSamples, int channelMap, String fileName,
			String triggerName,	double[][] rawData) {
		super(timeMilliseconds, channelMap, startSample, durationSamples);
		this.triggerMilliseconds = triggerMilliseconds;
		this.fileName = fileName;
		this.triggerName = triggerName;
		this.rawData = rawData;
		if (this.fileName == null) {
			this.fileName = "";
		}
	}

	private BufferedImage[] clipImages = new BufferedImage[PamConstants.MAX_CHANNELS];
	public BufferedImage getClipImage(int channel, int fftLength, int fftHop, 
			double scaleMin, double scaleMax, Color[] colorTable) {
		double[][] specData = getSpectrogramData(channel, fftLength, fftHop);
		if (specData == null) {
			return null;
		}
		int nT = specData.length;
		int nF = specData[0].length;
		BufferedImage image = new BufferedImage(nT, nF, BufferedImage.TYPE_INT_RGB);
		AcquisitionProcess daqProcess = findDaqProcess();
		if (daqProcess == null) {
			return null;
		}
		daqProcess.prepareFastAmplitudeCalculation(channel);
		double ampDB;
		int lutInd;
		int nCols = colorTable.length;
		for (int i = 0; i < nT; i++) {
			for (int j = 0; j < nF; j++) {
				ampDB = daqProcess.fftAmplitude2dB(specData[i][j], channel, fftLength, true, true);
				lutInd = (int) Math.round((ampDB - scaleMin) / (scaleMax - scaleMin) * nCols);
				lutInd = Math.min(Math.max(0, lutInd), nCols-1);
				image.setRGB(i, nF-j-1, colorTable[lutInd].getRGB());
			}
		}
		
		return image;
	}
	
	private ClipSpecData[] clipSpecData = new ClipSpecData[PamConstants.MAX_CHANNELS];
	/**
	 * get spectrogram data for the clip. 
	 * @param fftLength FFT length
	 * @param fftHop FFT hop
	 * @return double array of mag squared data or null if the clip waveform cannot be found
	 */
	public double[][] getSpectrogramData(int channel, int fftLength, int fftHop) {
		if (clipSpecData[channel] == null 
				|| clipSpecData[channel].fftLength != fftLength 
				|| clipSpecData[channel].fftHop != fftHop) {
			double[][] specData = generateSpectrogram(channel, fftLength, fftHop);
			if (specData == null) {
				clipSpecData[channel] = null;
				return null;
			}
			else {
				clipSpecData[channel] = new ClipSpecData(channel, fftLength, fftHop,specData);
			}
		}
		return clipSpecData[channel].spectrogramData;
	}
	
	/**
	 * Generate spectrogram data for the clip. 
	 * @param fftLength FFT length
	 * @param fftHop FFT hop
	 * @return double array of mag squared data or null if the clip waveform cannot be found
	 */
	private double[][] generateSpectrogram(int channel, int fftLength, int fftHop) {
		// TODO Auto-generated method stub
		double[] wave = getWaveData(channel);
		if (wave == null) {
			return null;
		}
		int nFFT = (wave.length - (fftLength-fftHop)) / fftHop;
		if (nFFT <= 0) {
			return null;
		}
		double[][] specData = new double[nFFT][fftLength/2];
		double[] waveBit = new double[fftLength];
		double[] winFunc = getWindowFunc(fftLength);
		Complex[] complexOutput = Complex.allocateComplexArray(fftLength/2);
		int wPos = 0;
		getFastFFT(fftLength);
		int m = FastFFT.log2(fftLength);
		for (int i = 0; i < nFFT; i++) {
			wPos = i*fftHop;
			for (int j = 0; j < fftLength; j++) {
				waveBit[j] = wave[j+wPos]*winFunc[j];
			}
			fastFFT.rfft(waveBit, complexOutput, m);
			for (int j = 0; j < fftLength/2; j++) {
				specData[i][j] = complexOutput[j].magsq();
			}
		}
		return specData;
	}
	
	static FastFFT fastFFT;
	static FastFFT getFastFFT(int fftLength) {
		if (fastFFT == null) {
			fastFFT = new FastFFT();
		}
		return fastFFT;
	}
	
	AcquisitionProcess findDaqProcess() {
			return (AcquisitionProcess) getParentDataBlock().getSourceProcess();
	}
	
	
	static double[] windowFunc;
	static double[] getWindowFunc(int fftLength) {
		if (windowFunc == null || windowFunc.length != fftLength) {
			windowFunc = WindowFunction.getWindowFunc(WindowFunction.HANNING, fftLength);
		}
		return windowFunc;
	}
	
	/**
	 * Get all the wave data into an array. 
	 * @return the wave data or null if it can't be found. 
	 */
	private double[][] getWaveData() {
		if (rawData != null) {
			return rawData;
		}
		File wavFileName = findWavFile();
		if (wavFileName == null || wavFileName.exists() == false) {
			return null;
		}
		WavFile wavFile = null;
		try {
			wavFile = new WavFile(wavFileName.getAbsolutePath(), "r");
		}
		catch (Exception e) {
			return null;
		}
		if (wavFile == null) {
			return null;
		}
		WavHeader wavHeader = wavFile.readWavHeader();
		int nSamples = (int) (wavHeader.getDataSize() / wavHeader.getBlockAlign());
		int nChannels = wavHeader.getNChannels();
		double[][] waveData = new double[nChannels][nSamples];
		wavFile.readData(waveData);
		return waveData;
	}
	
	/**
	 * Get the wave data for a single channel. Note that the wave clip may have only 
	 * recorded data for a subset of channels, so it's necessary to look at the channel
	 * bitmap to work out which channel from the wave clip we actually want. 
	 * @param channel channel number
	 * @return array of wave data, or null if it can't be found. 
	 */
	private double[] getWaveData(int channel) {
		double[][] waveData = getWaveData();
		if (channel == 0) {
			// find the first waveData with data in it. 
			if (waveData == null) {
				return null;
			}
			for (int i = 0; i < waveData.length; i++) {
				if (waveData[i] != null) {
					return waveData[i];
				}
				return null;
			}
		}
		int channelPos = PamUtils.getChannelPos(channel, getChannelBitmap());
		if (waveData == null) {
			return null;
		}
		int waveChans = waveData.length;
		if (channelPos >= waveChans || channelPos < 0) {
			return null;
		}
		return waveData[channelPos];
	}

	private File findWavFile() {
		if (offlineFile == null || offlineFile.exists() == false) {
			ClipProcess clipProcess = (ClipProcess) getParentDataBlock().getParentProcess();
			offlineFile = clipProcess.findClipFile(this);
		}
		return offlineFile;
	}
	
	/**
	 * offlien file handle - keep persistently, since it may get needed > 1 time. 
	 */
	private File offlineFile;
	
	/**
	 * Class to hold a set of information about a generated spectrgram clip. 
	 * @author Doug Gillespie
	 *
	 */
	class ClipSpecData {
		public ClipSpecData(int channel, int fftLength, int fftHop,
				double[][] spectrogramData) {
			this.channel = channel;
			this.fftLength = fftLength;
			this.fftHop = fftHop;
			this.spectrogramData = spectrogramData;
		}
		int channel;
		double[][] spectrogramData = null;
		int fftLength, fftHop;
	}

	/**
	 * @return the triggerMilliseconds
	 */
	public long getTriggerMilliseconds() {
		return triggerMilliseconds;
	}

	/**
	 * @return the rawData
	 */
	public double[][] getRawData() {
		return rawData;
	}

	/**
	 * @param rawData the rawData to set
	 */
	public void setRawData(double[][] rawData) {
		this.rawData = rawData;
	}


}

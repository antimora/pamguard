package spectrogramNoiseReduction;

import java.util.ArrayList;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import spectrogramNoiseReduction.averageSubtraction.AverageSubtraction;
import spectrogramNoiseReduction.kernelSmoothing.KernelSmoothing;
import spectrogramNoiseReduction.medianFilter.SpectrogramMedianFilter;
import spectrogramNoiseReduction.threshold.SpectrogramThreshold;
import spectrogramNoiseReduction.threshold.ThresholdParams;

import fftManager.Complex;
import fftManager.FFTDataBlock;
import fftManager.FFTDataUnit;
import PamController.PamControlledUnit;
import PamController.PamController;
import PamguardMVC.Annotation;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamObserver;
import PamguardMVC.PamProcess;
import PamguardMVC.RequestCancellationObject;

public class SpectrogramNoiseProcess extends PamProcess {

//	private SpectrogramNoiseControl spectrogramNoiseControl;

	protected FFTDataBlock sourceData;
	
	protected FFTDataBlock outputData;

	private ArrayList<SpecNoiseMethod> methods = new ArrayList<SpecNoiseMethod>();
	
	private SpectrogramNoiseSettings noiseSettings = new SpectrogramNoiseSettings();
	
	private SpectrogramThreshold thresholdMethod;

	
	private int totalDelay;
	
	private Complex[][] delayedInputData;
	
	public SpectrogramNoiseProcess(PamControlledUnit pamControlledUnit) {
		super(pamControlledUnit, null);
		this.setProcessName(pamControlledUnit.getUnitName() + " Noise reduction");
//		this.spectrogramNoiseControl = spectrogramNoiseControl;

		methods.add(new SpectrogramMedianFilter());
		methods.add(new AverageSubtraction());
		methods.add(new KernelSmoothing());
		methods.add(thresholdMethod = new SpectrogramThreshold());

		addOutputDataBlock(outputData = new FFTDataBlock(pamControlledUnit.getUnitName() + " Noise free FFT data", 
				this, 0, 0, 0));
		outputData.setRecycle(true);
	}
	
	@Override
	public void setupProcess() {
		super.setupProcess();
		sourceData = (FFTDataBlock) PamController.getInstance().getDataBlock(FFTDataUnit.class, 
				getNoiseSettings().dataSource);
		setParentDataBlock(sourceData);
		int channelMap = getNoiseSettings().channelList;
		if (sourceData != null) {
			channelMap = getParentDataBlock().getChannelMap();
			/**
			 * The channel map of the noise process must be the same as that of the sourceData. 
			 */
			noiseSettings.channelList = channelMap;
			outputData.setChannelMap(channelMap);
			outputData.setFftHop(sourceData.getFftHop());
			outputData.setFftLength(sourceData.getFftLength());
//			smoothingChannelProcessList = new SmoothingChannelProcess[PamUtils.getHighestChannel(channelMap)+1];
//			for (int i = 0; i < PamUtils.getHighestChannel(channelMap)+1; i++) {
//				smoothingChannelProcessList[i] = new SmoothingChannelProcess();
//			}
		}
		prepareProcess();
		
		makeAnnotations();
	}

	@Override
	public void newData(PamObservable o, PamDataUnit arg) {
		/*
		 * Each noise reduction process will work on the data array
		 * in the pamDataUnit in place, so the first thing
		 * to do is to make a copy of the data which can then
		 * be passed in turn to each active noise reduction process
		 * prior to output. 
		 */
		FFTDataUnit fftDataUnit = (FFTDataUnit) arg;
		Complex[] fftData = fftDataUnit.getFftData();
		
//		shuffle along delayed data
		delayedInputData[totalDelay] = fftData;
		for (int i = 0; i < totalDelay; i++) {
			delayedInputData[i] = delayedInputData[i+1];
		}

		FFTDataUnit newFFTUnit;
//		newFFTUnit = outputData.getRecycledUnit();
//		if (newFFTUnit != null) {
//			newFFTUnit.setInfo(fftDataUnit.getTimeMilliseconds(), fftDataUnit.getChannelBitmap(), 
//					fftDataUnit.getStartSample(), fftDataUnit.getDuration(), fftDataUnit.getFftSlice());
//		}
//		else {
			newFFTUnit = new FFTDataUnit(arg.getTimeMilliseconds(), fftDataUnit.getChannelBitmap(), 
					fftDataUnit.getStartSample(), fftDataUnit.getDuration(), null, fftDataUnit.getFftSlice());
//		}
		/**
		 * Need to ensure a genuine copy is made and that data aren't still 
		 * pointing at the same Complex objects !
		 * But at the same time, recycle where possible
		 */
		Complex[] newFftData = newFFTUnit.getFftData();
		if (newFftData == null || newFftData.length != fftData.length) {
			newFftData = outputData.getComplexArray(fftData.length);
			newFFTUnit.setFftData(newFftData);
		}
		for (int i = 0; i < newFftData.length; i++) {
			newFftData[i].assign(fftData[i]);
		}

		for (int i = 0; i < methods.size(); i++) {
			if (noiseSettings.isRunMethod(i)) {
				methods.get(i).runNoiseReduction(newFFTUnit);
			}
		}
		
		ThresholdParams p = (ThresholdParams) thresholdMethod.getParams();
		if (p.finalOutput == SpectrogramThreshold.OUTPUT_RAW) {
			thresholdMethod.pickEarlierData(fftData, newFFTUnit.getFftData());
		}
		
		// and output the data unit. 
		outputData.addPamData(newFFTUnit);
	}
	
	
	@Override
	public void prepareProcess() {

		super.prepareProcess();
		
		int channelMap = getNoiseSettings().channelList;
		PamDataBlock source = getSourceDataBlock();
		if (source != null) {
			channelMap &= source.getChannelMap();
		}
		
		for (int i = 0; i < methods.size(); i++) {
			methods.get(i).initialise(channelMap);
		}
	}

	public SpectrogramNoiseSettings getNoiseSettings() {
		/**
		 * will need to rebuild the array list of individual modules settings
		 * first. 
		 */
		if (noiseSettings == null) {
			noiseSettings = new SpectrogramNoiseSettings();
		}
		noiseSettings.clearSettings();
		for (int i = 0; i < methods.size(); i++) {
			noiseSettings.addSettings(methods.get(i).getParams());
		}
		return noiseSettings;
	}
	
	public void setNoiseSettings(SpectrogramNoiseSettings noiseSettings) {
		this.noiseSettings = noiseSettings;
		if (noiseSettings == null) {
			noiseSettings = getNoiseSettings();
		}
		for (int i = 0; i < methods.size(); i++) {
			if (noiseSettings.getSettings(i) != null) {
				methods.get(i).setParams(noiseSettings.getSettings(i));
			}
		}
		setupProcess();
	}

	public ArrayList<SpecNoiseMethod> getMethods() {
		return methods;
	}



	public void setParentDataBlock(FFTDataBlock fftDataBlock) {
		super.setParentDataBlock(fftDataBlock);
		if (fftDataBlock != null) {
			outputData.setFftHop(fftDataBlock.getFftHop());
			outputData.setFftLength(fftDataBlock.getFftLength());
			outputData.setChannelMap(fftDataBlock.getChannelMap());
		}
	}

	@Override
	public void pamStart() {

		// work out the total delay, so that data can be sotored for use
		// after the final threhsolding
		totalDelay = 0;
		for (int i = 0; i < methods.size(); i++) {
			totalDelay += methods.get(i).getDelay();
		}
		delayedInputData = new Complex[totalDelay+1][];
	}

	@Override
	public void pamStop() {
		// TODO Auto-generated method stub

	}
	
	
	@Override
	public Annotation getAnnotation(PamDataBlock pamDataBlock, int annotation) {
		// TODO Auto-generated method stub
		return fftAnnotations.get(annotation);
	}

	@Override
	public int getNumAnnotations(PamDataBlock pamDataBlock) {
		if (fftAnnotations == null) {
			return 0;
		}
		return fftAnnotations.size();
	}
	
	private Vector<Annotation> fftAnnotations;
	
	public void makeAnnotations() {
		if (fftAnnotations == null) {
			fftAnnotations = new Vector<Annotation>();
		}
		else {
			fftAnnotations.clear();
		}

		for (int i = 0; i < methods.size(); i++) {
			if (noiseSettings.isRunMethod(i)) {
				fftAnnotations.add(methods.get(i).getAnnotation(this));
			}
		}
		outputData.createAnnotations(getSourceDataBlock(), this, true);
	}

	/**
	 * @return the outputData
	 */
	public FFTDataBlock getOutputDataBlock() {
		return outputData;
	}

	@Override
	public int getOfflineData(PamDataBlock dataBlock, PamObserver endUser, long startMillis,
			long endMillis, RequestCancellationObject cancellationObject) {
//		System.out.println("generate offline noise reduced fft data");
		prepareProcess();
		pamStart();
		return super.getOfflineData(dataBlock, endUser, startMillis, endMillis, cancellationObject);
	}

	@Override
	public boolean fillXMLParameters(Document doc, Element paramsEl) {
		SpecNoiseMethod method;
		String methodName;
		for (int i = 0; i < methods.size(); i++) {
			method = methods.get(i);
			methodName = method.getName();
//			System.out.println(methodName);
			Element n = doc.createElement("Method_"+i);
			n.setAttribute("Name", methodName);
			n.setAttribute("Enabled", new Boolean(noiseSettings.isRunMethod(i)).toString());
			method.fillXMLParameters(n);
			paramsEl.appendChild(n);
		}
		return true;
	}
	
	
}

package clickDetector.ClickClassifiers.basicSweep;

import java.awt.Frame;
import java.io.Serializable;

import javax.swing.JMenuItem;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import PamController.PamControlledUnitSettings;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamView.PamSymbol;
import clickDetector.ClickControl;
import clickDetector.ClickDetection;
import clickDetector.ClickDetector;
import clickDetector.ClickClassifiers.ClassifyDialogPanel;
import clickDetector.ClickClassifiers.ClickIdInformation;
import clickDetector.ClickClassifiers.ClickIdentifier;
import clickDetector.ClickClassifiers.ClickTypeCommonParams;

/**
 * An improvements on the BasicClickIdentifier based on work by 
 * Gillespie and Caillat in 2009. 
 * Click length is now measured based on the envelope waveform 
 * rather than a measure of total energy
 * Have also added some parameters extracted from zero crossings
 * and will include better diagnostic plots and histograms.
 *  
 * @author Doug Gillespie
 *
 */
public class SweepClassifier implements ClickIdentifier , PamSettings {

	private ClickDetector clickDetector;
	
	private ClickControl clickControl;
	
	private SweepClassifierPanel dialogPanel;
	
	private SweepClassifierWorker sweepClassifierWorker;
	
	protected SweepClassifierParameters sweepClassifierParameters = new SweepClassifierParameters();
	
	public SweepClassifier(ClickControl clickControl) {
		super();
		this.clickControl = clickControl;
		clickDetector = clickControl.getClickDetector();
		sweepClassifierWorker = new SweepClassifierWorker(clickControl, this);
		PamSettingManager.getInstance().registerSettings(this);
	}

	@Override
	public int codeToListIndex(int code) {
		int n = sweepClassifierParameters.getNumSets();
		for (int i = 0; i < n; i++) {
			if (sweepClassifierParameters.getSet(i).speciesCode == code) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public JMenuItem getMenuItem(Frame parentFrame) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getSpeciesList() {
		int n = sweepClassifierParameters.getNumSets();
		String[] speciesList = new String[n];
		for (int i = 0; i < n; i++) {
		  speciesList[i] = sweepClassifierParameters.getSet(i).name;	
		}		
		return speciesList;
	}
	/**
     * Returns a list of the currently-defined click types / species codes
     * @return int array with the codes
     */
	@Override
	public int[] getCodeList() {
		int n = sweepClassifierParameters.getNumSets();
		int[] codeList = new int[n];
		for (int i = 0; i < n; i++) {
			codeList[i] = sweepClassifierParameters.getSet(i).speciesCode;	
		}		
		return codeList;
	}

	@Override
	public PamSymbol getSymbol(ClickDetection click) {
		if (click.getClickType() <= 0) {
			return null;
		}
		SweepClassifierSet scs = findClicktypeSet(click.getClickType());
		if (scs == null) {
			return null;
		}
		return scs.symbol;
	}
	
	private SweepClassifierSet findClicktypeSet(int iSpeciesCode) {
		int n = sweepClassifierParameters.getNumSets();
		SweepClassifierSet scs;
		for (int i = 0; i < n; i++) {
			if ((scs = sweepClassifierParameters.getSet(i)).speciesCode == iSpeciesCode) {
				return scs;
			}
		}
		return null;
	}

    /**
     * Return the superclass of the click type parameters class - currently used for
     * accessing the alarm functions.  Subclasses include ClickTypeParams and
     * SweepClassifierSet.
     *
     * @param code the click type to check
     * @return the ClickTypeCommonParams object related to the species code
     */
	@Override
    public ClickTypeCommonParams getCommonParams(int code) {
        int codeIdx = codeToListIndex(code);
        return sweepClassifierParameters.getSet(codeIdx);
    }

	@Override
	public PamSymbol[] getSymbols() {
		int n = sweepClassifierParameters.getNumSets();
		if (n == 0) {
			return null;
		}
		PamSymbol[] symbols = new PamSymbol[n];
		for (int i = 0; i < n; i++) {
			symbols[i] = sweepClassifierParameters.getSet(i).symbol.clone();
		}
		return symbols;
	}

	@Override
	public ClassifyDialogPanel getDialogPanel(Frame windowFrame) {
		if (dialogPanel == null) {
			dialogPanel = new SweepClassifierPanel(this, windowFrame, clickControl);
		}
		return dialogPanel;
	}

	@Override
	public String getSpeciesName(int code) {
		int i = codeToListIndex(code);
		if (i < 0) {
			return null;
		}
		return sweepClassifierParameters.getSet(i).name;
	}

	@Override
	public Serializable getSettingsReference() {
		return sweepClassifierParameters;
	}

	@Override
	public long getSettingsVersion() {
		return SweepClassifierParameters.serialVersionUID;
	}

	@Override
	public String getUnitName() {
		return clickControl.getUnitName();
	}

	@Override
	public String getUnitType() {
		return "ClickSweepClassifier";
	}

	@Override
	public boolean restoreSettings(
			PamControlledUnitSettings pamControlledUnitSettings) {
		sweepClassifierParameters = ((SweepClassifierParameters) pamControlledUnitSettings.getSettings()).clone();
		return sweepClassifierParameters != null;
	}

	/**
	 * @return the clickDetector
	 */
	public ClickDetector getClickDetector() {
		return clickDetector;
	}
	
	protected int getNextFreeCode(int currCode) {
		int newCode = currCode;
		while (codeTaken(++newCode));
		return newCode;
	}
	
	protected int getPrevFreeCode(int currCode) {
		while (codeTaken(--currCode));
		return currCode;		
	}
	
	protected boolean codeTaken(int code) {
		int n = sweepClassifierParameters.getNumSets();
		for (int i = 0; i < n; i++) {
			if (sweepClassifierParameters.getSet(i).speciesCode == code) {
				return true;
			}
		}
		return false;
	}

	public boolean codeDuplicated(SweepClassifierSet sweepClassifierSet, int ignoreRow) {
		int code = sweepClassifierSet.speciesCode;
		int n = sweepClassifierParameters.getNumSets();
		for (int i = 0; i < n; i++) {
			if (i == ignoreRow) {
				continue;
			}
			if (sweepClassifierParameters.getSet(i) == sweepClassifierSet) {
				continue;
			}
			if (sweepClassifierParameters.getSet(i).speciesCode == code) {
				return true;
			}
		}
		return false;
	}

	@Override
	public synchronized ClickIdInformation identify(ClickDetection click) {
		return sweepClassifierWorker.identify(click);
	}

	@Override
	public boolean fillXMLParamaeters(Document doc, Element classEl) {
		if (sweepClassifierParameters.getNumSets() == 0) {
			return false;
		}
		Element spElement;
		SweepClassifierSet aSet;
		String setName;
		classEl.setAttribute("nSets", new Integer(sweepClassifierParameters.getNumSets()).toString());
		for (int i = 0; i < sweepClassifierParameters.getNumSets(); i++) {
			 aSet = sweepClassifierParameters.getSet(i);
			 setName = aSet.name;
//			 setName = setName.replaceAll(" ", "_");
			 spElement = doc.createElement("ClassifierSet");
			 spElement.setAttribute("name", setName);
//			 spElement.setAttribute("name", aSet.);
			 spElement.setAttribute("speciesCode", new Integer(aSet.speciesCode).toString());
			 spElement.setAttribute("enable", new Boolean(aSet.enable).toString());
			 spElement.setAttribute("discard", new Boolean(aSet.discard).toString());
			 spElement.setAttribute("channelChoices", new Integer(aSet.channelChoices).toString());
			 spElement.setAttribute("restrictLength", new Boolean(aSet.restrictLength).toString());
			 spElement.setAttribute("restrictedBins", new Integer(aSet.restrictedBins).toString());
			 spElement.setAttribute("enableLength", new Boolean(aSet.enableLength).toString());
			 spElement.setAttribute("lengthSmoothing", new Integer(aSet.lengthSmoothing).toString());
			 spElement.setAttribute("lengthdB", new Double(aSet.lengthdB).toString());
			 spElement.setAttribute("minLength", new Double(aSet.minLength).toString());
			 spElement.setAttribute("maxLength", new Double(aSet.maxLength).toString());
			 spElement.setAttribute("enableEnergyBands", new Boolean(aSet.enableEnergyBands).toString());
			 spElement.setAttribute("nControlBands", new Integer(SweepClassifierSet.nControlBands).toString());
			 String name;
			 for (int f = 0; f < 2; f++) {
				 name = String.format("testEnergyBand%d", f);
				 spElement.setAttribute(name, new Double(aSet.testEnergyBand[f]).toString());
				 name = String.format("energyThresholds%d", f);
				 spElement.setAttribute(name, new Double(aSet.energyThresholds[f]).toString());
			 }
			 for (int b = 0; b < SweepClassifierSet.nControlBands; b++) {
				 for (int f = 0; f < 2; f++) {
					 name = String.format("controlEnergyBand%d%d", b, f);
					 spElement.setAttribute(name, new Double(aSet.controlEnergyBand[b][f]).toString());
				 }
			 }
			 spElement.setAttribute("enableFFTFilter", new Boolean(aSet.enableFFTFilter).toString());
			 if (aSet.enableFFTFilter && aSet.fftFilterParams != null) {
				 Element filtEl = doc.createElement("FFTFilter");
				 aSet.fftFilterParams.fillXMLParamaters(doc, filtEl);
				 spElement.appendChild(filtEl);
			 }
			 spElement.setAttribute("enablePeak", new Boolean(aSet.enablePeak).toString());
			 spElement.setAttribute("enableWidth", new Boolean(aSet.enableWidth).toString());
			 spElement.setAttribute("enableMean", new Boolean(aSet.enableMean).toString());
			 for (int f = 0; f < 2; f++) {
				 spElement.setAttribute(String.format("peakSearchRange%d",f), new Double(aSet.peakSearchRange[f]).toString());
				 spElement.setAttribute(String.format("peakRange%d",f), new Double(aSet.peakRange[f]).toString());
				 spElement.setAttribute(String.format("peakWidthRange%d",f), new Double(aSet.peakWidthRange[f]).toString());
				 spElement.setAttribute(String.format("meanRange%d",f), new Double(aSet.meanRange[f]).toString());
			 }
			 spElement.setAttribute("peakSmoothing", new Integer(aSet.peakSmoothing).toString());
			 spElement.setAttribute("peakWidthThreshold", new Double(aSet.peakWidthThreshold).toString());
			 spElement.setAttribute("enableZeroCrossings", new Boolean(aSet.enableZeroCrossings).toString());
			 for (int f = 0; f < 2; f++) {
				 spElement.setAttribute(String.format("nCrossings%d",f), new Integer(aSet.nCrossings[f]).toString());
				 spElement.setAttribute(String.format("zcSweep%d",f), new Double(aSet.zcSweep[f]).toString());
			 }
			 
			 classEl.appendChild(spElement);
		}
		return true;
	}

	@Override
	public String getParamsInfo(ClickDetection click) {
		/**
		 * Get the parameters from the first classifier in the list and trun them all into a 
		 * nicely formatted string
		 */
		if (sweepClassifierParameters.getNumSets() == 0) {
			return null;
		}
		return getParamsInfo(sweepClassifierParameters.getSet(0), click);
	}

	private String getParamsInfo(SweepClassifierSet scs, ClickDetection click) {
		sweepClassifierWorker.identify(click);
		String str = "Classifier Output:";
		int[][] lengthData = sweepClassifierWorker.getLengthData(click, scs);
		int nChan = lengthData.length;
		
		str += "<p>&#x0009Length: ";
		double sampleRate = clickDetector.getSampleRate();
		double aLen;
		double totLen = 0;
		for (int i = 0; i < nChan; i++) {
			aLen = ((double) lengthData[i][1]-lengthData[i][0]) * 1000. / sampleRate;
			totLen += aLen;
			str += String.format("ch%d=%3.2f, ", i, aLen);
		}
		totLen/=nChan;
		str += String.format("mean=%3.2f ms", totLen);
		
		
		return str;
	}

	

}

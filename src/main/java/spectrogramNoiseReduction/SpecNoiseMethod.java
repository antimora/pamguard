package spectrogramNoiseReduction;

import java.io.Serializable;

import org.w3c.dom.Element;

import PamguardMVC.Annotation;
import PamguardMVC.PamProcess;

import fftManager.FFTDataUnit;

/**
 * Interface for multiple plugins to the spectrogram noise
 * reduction system. 
 * @author Doug Gillespie
 *
 */
public abstract class SpecNoiseMethod {

	/**
	 * Get a name for the method.
	 * @return String
	 */
	public abstract String getName();
	
	/**
	 * Get a longer description of the method
	 * in html format for hover texts in dialogs. 
	 * @return html description
	 */
	public abstract String getDescription();
	/**
	 * Run the noise reduction on the data in place. 
	 * @param fftData array of fft data (half fft length)
	 * @return true if ran OK/ 
	 */
	public abstract boolean runNoiseReduction(FFTDataUnit fftDataUnit);
	
	
	/**
	 * @return the delay imposed on the data by this operation.
	 */
	public abstract int getDelay();
	/**
	 * Set up the noise reduction process
	 * @return true if initialised OK. 
	 */
	public abstract boolean initialise(int channelMap);
	
	/**
	 * Set the params for this method - will check class is OK
	 * before casting. 
	 * @param noiseParams
	 * @return true if parameters match 
	 */
	public abstract boolean setParams(Serializable noiseParams);
	
	/**
	 * Get the noise params for this method for storage between euns
	 * @return
	 */
	public abstract Serializable getParams();
	
	/**
	 * Get a component to include in the overall noise reductino dialog
	 * @return component
	 */
	public abstract SpecNoiseDialogComponent getDialogComponent();
	
	/**
	 * Get an annotation to go with this method.
	 * @param pamProcess parent process
	 * @return annotation
	 */
	public Annotation getAnnotation(PamProcess pamProcess) {
		return new Annotation(pamProcess, this, "Noise Reduction", getName());
	}

	public abstract void fillXMLParameters(Element n);
	
}

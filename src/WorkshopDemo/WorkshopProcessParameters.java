package WorkshopDemo;

import java.awt.Color;
import java.io.Serializable;

import PamView.PamSymbol;

/**
 * for each module, it's best to keep all parameters controlling that module
 * in a single class which must be serialisable so that the Pamguard settings 
 * manager can save it between runs. 
 * @author Doug
 * @see PamController.PamSettingManager
 *
 */
public class WorkshopProcessParameters implements Serializable, Cloneable {

	static final long serialVersionUID = 1;
	
	/**
	 * use the first (0th) fft datablock in the model.
	 */
	int fftDataBlock = 0;  
	
	/**
	 * Low frequency for energy summation
	 */
	int lowFreq = 1000;

	/**
	 * High frequency for energy summation
	 */
	int highFreq = 10000;
	
	/**
	 * time constant for background noise measurement. 
	 */
	double backgroundTimeConstant = 10; 
	
	/**
	 * Detection threhsold in db. 
	 */
	double threshold = 6;
	
	/**
	 * Bitmap of channels to be used - use all available. 
	 */
	int channelList = 0xFFFF;
	
	/*
	 * Default symbol for the map
	 */
	PamSymbol mapSymbol = new PamSymbol(PamSymbol.SYMBOL_TRIANGLED, 15, 15, false, Color.CYAN, Color.CYAN);

	@Override
	/**
	 * overriding the clone function enables you to clone (copy) 
	 * these parameters easily in your code without having to 
	 * continually cast to (WorkshopProcessParameters) or handle
	 * the exception CloneNotSupportedException.
	 */
	protected WorkshopProcessParameters clone() {
		try {
			return (WorkshopProcessParameters) super.clone();
		}
		catch (CloneNotSupportedException ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
}

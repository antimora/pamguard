package echoDetector;

import java.awt.Color;
import java.io.Serializable;

import PamView.PamSymbol;
/**
 * This class stores the parameters for the echo detector.
 * @author Brian Miller
 *
 */
public class EchoProcessParameters implements Cloneable, Serializable {

	static final long serialVersionUID = 1;
	
	/**
	 * use the first (0th) click detector in the model.
	 */
	int clickDetector = 0;  
	
	/**
	 * Minimum echo delay in milliseconds
	 */
	int minEchoTime = 3;

	/**
	 * Maximum echo delay in milliseconds
	 */
	int maxEchoTime = 10;
	
	/**
	 * Number of samples used to compute the echo 
	 * detection function
	 */
	int echoDuration = 4096;
	
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
	PamSymbol mapSymbol = new PamSymbol(PamSymbol.SYMBOL_TRIANGLER, 15, 15, false, Color.CYAN, Color.CYAN);

	@Override
	/**
	 * overriding the clone function enables you to clone (copy) 
	 * these parameters easily in your code without having to 
	 * continually cast to (WorkshopProcessParameters) or handle
	 * the exception CloneNotSupportedException.
	 */
	protected EchoProcessParameters clone() {
		try {
			return (EchoProcessParameters) super.clone();
		}
		catch (CloneNotSupportedException ex) {
			ex.printStackTrace();
			return null;
		}
	}
	

}

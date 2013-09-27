package alarm;

import java.awt.Window;

import PamguardMVC.PamDataUnit;

abstract public class AlarmCounter {
	/**
	 * Get a count value from a source of alarm trigger data
	 * <p>Two types of counting are available. COUNT_SIMPLE in which 
	 * case each data unit scores either 0 or 1 and COUNT_SCORES in 
	 * which case each unit is assigned a value (could still be 0 or 1 !)
	 * @param countType type of counting
	 * @param dataUnit dataunit to consider
	 * @return value to add to trigger count. 
	 */
	public abstract double getValue(int countType, PamDataUnit dataUnit);
	
	/**
	 * @return true if the counter has module specific options. 
	 */
	public boolean hasOptions() {
		return false;
	}
	
	/**
	 * Show module specific options
	 * @param parent parent frame for dialog
	 * @return true if options were changed (false if cancel button pressed on dialog)
	 */
	public boolean showOptions(Window parent) {
		return false;
	}
	
	/**
	 * Add a count to the total. This has been put in this abstract class
	 * so that individual modules can override how they add things up, e.g. 
	 * a noise measurement in dB may need to be converted to energy before adding. 
	 * @param currentValue current value
	 * @param countToAdd amount to add
	 * @param countType type of counting
	 * @return the two added together in whichever way is most appropriate. 
	 */
	public double addCount(double currentValue, double countToAdd, int countType) {
		if (countType == AlarmParameters.COUNT_SIMPLE) {
			return currentValue + 1;
		}
		else {
			return currentValue + countToAdd;
		}
	}
	/**
	 * Subtract a count from the total. This has been put in this abstract class
	 * so that individual modules can override how they subtract things, e.g. 
	 * a noise measurement in dB may need to be converted to energy before subtracting. 
	 * @param currentValue current value
	 * @param countToSubtract amount to subtract off
	 * @param countType type of counting
	 * @return the two subtracted from one another in whichever way is most appropriate. 
	 */
	public double subtractCount(double currentValue, double countToSubtract, int countType) {
		if (countType == AlarmParameters.COUNT_SIMPLE) {
			return currentValue - 1;
		}
		else {
			return currentValue - countToSubtract;
		}
	}
}

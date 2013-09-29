package SoundRecorder.trigger;

import java.awt.Window;

import PamguardMVC.PamDataUnit;
import SoundRecorder.RecorderControl;

/**
 * RecorderTriggers are used to automatically make sound file 
 * recordings in response to detections, or any other PamGuard event.
 * <p>
 * A part of the PamGuard system that is to trigger recordings should 
 * first create a class that implements the RecorderTriggers interface. 
 * This class must then be registered with the PamGuard recorders using 
 * the static function void RecorderControl.addRecorderTrigger(RecorderTrigger 
 * recorderTrigger). The trigger will then be listed on the tab panel of each 
 * instance of the PamGuard recorder from where it can be enabled or disabled 
 * for each recorder independently. 
 * <p>
 * When an event occurs which should start a recording, call the static 
 * function void RecorderControl.actionRecorderTrigger(RecorderTrigger 
 * recorderTrigger). Each instance of the recorder will then be notified 
 * and will start recording depending on the state of check box on each 
 * recorder tab on the GUI.
 * <p>
 * Note that the RecorderTrigger passed to RecorderControl.actionRecorderTrigger
 * must be exactly the same instance as was passed to RecorderControl.addRecorderTrigger  
 * 
 * @author Doug Gillespie
 * @see SoundRecorder.RecorderControl
 * @see SoundRecorder.trigger.RecorderTriggerData
 *
 */
public abstract class RecorderTrigger {
	
	/**
	 * Get default data for this trigger. This will be cloned and 
	 * used locally in each individual recorder from where it can 
	 * be modified from the recorders control panel. 
	 * @return default settings for a recorder. 
	 */
	public abstract RecorderTriggerData getDefaultTriggerData();
	
	/**
	 * Called as each data unit is considered for triggering. Individual
	 * modules should override this in order to provide additional functionality, 
	 * e.g. the click detector may give options to only trigger on certain click
	 * types, the whistle detector only on whsitles in a given frequency band or of 
	 * a minimum length, etc. 
	 * @param dataUnit data unit to consider for recording triggering
	 * @return true if unit should be used. 
	 */
	public boolean triggerDataUnit(PamDataUnit dataUnit, RecorderTriggerData rtData) {
		return true;
	}
	
	/**
	 * This trigger has additional options available. 
	 * @return true if additional options are available. 
	 */
	public boolean hasOptions() {
		return false;
	}
	
	/**
	 * Show a dialog of additional options for the recorder trigger. 
	 * <p> Developers should probably put parameters controlling any 
	 * options into a class extending the defaultTriggerData, so that these get stored 
	 * on a recorder by recorder basis. 
	 * @param frame
	 * @param triggerData
	 * @return
	 */
	public boolean showOptionsDialog(Window frame, RecorderTriggerData triggerData) {
		return false;
	}
	/**
	 * 
	 * @return the name used to identify the trigger. 
	 * 	 */
	public final String getName() {
		return getDefaultTriggerData().getTriggerName();
	}
			
}

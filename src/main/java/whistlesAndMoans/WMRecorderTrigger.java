package whistlesAndMoans;

import java.awt.Window;
import java.io.Serializable;

import PamguardMVC.PamDataUnit;
import SoundRecorder.trigger.RecorderTrigger;
import SoundRecorder.trigger.RecorderTriggerData;

public class WMRecorderTrigger extends RecorderTrigger {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	WhistleMoanControl wmControl;
	
	RecorderTriggerData defaultData;
	
	WMRecorderTrigger(WhistleMoanControl wmControl) {
		super();
		this.wmControl = wmControl;
		defaultData = new WMRecorderTriggerData(wmControl);
	}

	@Override
	public RecorderTriggerData getDefaultTriggerData() {
		return defaultData;
	}

	/* (non-Javadoc)
	 * @see SoundRecorder.trigger.RecorderTrigger#hasOptions()
	 */
	@Override
	public boolean hasOptions() {
		return true;
	}

	/* (non-Javadoc)
	 * @see SoundRecorder.trigger.RecorderTrigger#showOptionsDialog(java.awt.Window, SoundRecorder.trigger.RecorderTriggerData)
	 */
	@Override
	public boolean showOptionsDialog(Window frame,
			RecorderTriggerData triggerData) {
		WMRecorderTriggerData wmTrigData = null;
		try {
			wmTrigData = (WMRecorderTriggerData) triggerData;
		}
		catch (ClassCastException e) {
			e.printStackTrace();
			return false;
		}
		boolean ok = WMRecordTriggerdialog.showDialog(frame, this, wmTrigData);
		
		return ok;
	}

	@Override
	public boolean triggerDataUnit(PamDataUnit dataUnit, RecorderTriggerData triggerData) {
		/*
		 *  Test to see whether the whistle data unit is going to have any energy inside 
		 *  the range we're triggering on.  
		 */
		WMRecorderTriggerData wmTrigData = (WMRecorderTriggerData) triggerData;
		if (wmTrigData.maxFreq == 0) {
			return true;
		}
		ConnectedRegionDataUnit crdu = (ConnectedRegionDataUnit) dataUnit;
		double[] freqs = crdu.getFrequency();
		if (freqs[0] > wmTrigData.maxFreq || freqs[1] < wmTrigData.minFreq) {
			return false;
		}
		
		return true;
	}


}

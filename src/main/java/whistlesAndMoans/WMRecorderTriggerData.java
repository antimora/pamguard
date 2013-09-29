package whistlesAndMoans;

import java.io.Serializable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import PamController.PamControlledUnit;
import SoundRecorder.trigger.RecorderTriggerData;

public class WMRecorderTriggerData extends RecorderTriggerData implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;
	
	protected double minFreq, maxFreq;
		
	public WMRecorderTriggerData(WhistleMoanControl wmControl) {
		super(wmControl.getUnitName(), 30, 60);
		setMinDetectionCount(3);
		setCountSeconds(5);
	}

	@Override
	public WMRecorderTriggerData clone() {
		return (WMRecorderTriggerData) super.clone();
	}

	/* (non-Javadoc)
	 * @see SoundRecorder.trigger.RecorderTriggerData#fillXMLParameters(org.w3c.dom.Document, org.w3c.dom.Element)
	 */
	@Override
	public boolean fillXMLParameters(Document doc, Element paramsEl) {
		super.fillXMLParameters(doc, paramsEl);
		PamControlledUnit.addXMLParameter(doc, paramsEl, minFreq, "minFreq", 
				"Min frequency in whistle to cause a trigger (0 = unlimited)", 0, 0);
		PamControlledUnit.addXMLParameter(doc, paramsEl, maxFreq, "maxFreq", 
				"Max frequency in whistle to cause a trigger (0 = unlimited)", 0, 0);
		return true;
	}

}

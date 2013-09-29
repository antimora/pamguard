package loggerForms;

import PamView.PamDetectionOverlayGraphics;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamProcess;
/**
 * 
 * @author Graham Weatherup
 *
 */
public class FormsDataBlock extends PamDataBlock<FormsDataUnit> {
	
	private FormDescription formDescription;

	public FormsDataBlock(FormDescription formDescription, String dataName,
			PamProcess parentProcess, int channelMap) {
		super(FormsDataUnit.class, dataName, parentProcess, channelMap);
		this.formDescription = formDescription;
		setNaturalLifetime(600);
//		setNaturalLifetimeMillis(60000);
	}

	public FormDescription getFormDescription() {
		return formDescription;
	}

	/* (non-Javadoc)
	 * @see PamguardMVC.PamDataBlock#addPamData(PamguardMVC.PamDataUnit)
	 */
	@Override
	public void addPamData(FormsDataUnit pamDataUnit) {
		super.addPamData(pamDataUnit);
		formDescription.dataBlockChanged();
	}

	/* (non-Javadoc)
	 * @see PamguardMVC.PamDataBlock#updatePamData(PamguardMVC.PamDataUnit, long)
	 */
	@Override
	public void updatePamData(FormsDataUnit pamDataUnit, long timeMillis) {
		super.updatePamData(pamDataUnit, timeMillis);
		formDescription.dataBlockChanged();
	}

	/* (non-Javadoc)
	 * @see PamguardMVC.PamDataBlock#removeOldUnitsT(long)
	 */
	@Override
	protected synchronized int removeOldUnitsT(long currentTimeMS) {
		int n = super.removeOldUnitsT(currentTimeMS);
		if (n > 0) {
			formDescription.dataBlockChanged();
		}
		return n;
	}

	/* (non-Javadoc)
	 * @see PamguardMVC.PamDataBlock#removeOldUnitsS(long)
	 */
	@Override
	protected synchronized int removeOldUnitsS(long mastrClockSample) {
		int n = super.removeOldUnitsS(mastrClockSample);
		if (n > 0) {
			formDescription.dataBlockChanged();
		}
		return n;
	}

}

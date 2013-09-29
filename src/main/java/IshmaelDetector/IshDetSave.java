/**
 * 
 */
package IshmaelDetector;

/**
 * @author Hisham
 */

import PamDetection.IshDetection;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamObserver;

public class IshDetSave implements PamObserver
{
	IshDetControl ishDetControl;
	public IshDetection inputData;
	PamDataBlock ishPeakDataBlock;
	float sampleRate;

	public IshDetSave(IshDetControl ishDetControl) {
		super();
		this.ishDetControl = ishDetControl;
		ishPeakDataBlock = ishDetControl.ishPeakProcess.outputDataBlock;
		ishPeakDataBlock.addObserver(this);	//call update() when unit added
	}

	public void update(PamObservable o, PamDataUnit arg1) {
		IshDetection arg = (IshDetection)arg1; 
		//inputData = arg.detData;
		//Here we have to save these segments.  So open a 
		//an ascii file with the encoded date and time, and
		//save the positive detection with ten seconds of 
		//data to either side.
	}

	@Override
	public PamObserver getObserverObject() {
		return this;
	}
	
	public void saveData() {
	}

	public String getObserverName() {
		return "File Saver for Energy Sum";
	}

	public long getRequiredDataHistory(PamObservable o, Object arg) {
		return 0;
	}

	public void noteNewSettings() {
		// TODO Auto-generated method stub
	}

	public void removeObservable(PamObservable o) {
		// TODO Auto-generated method stub
	}

	public void setSampleRate(float sampleRate, boolean notify) {
		this.sampleRate = sampleRate;
	}

	@Override
	public void masterClockUpdate(long milliSeconds, long sampleNumber) {
		// TODO Auto-generated method stub
		
	}

	public void prepareForRun() {}
}

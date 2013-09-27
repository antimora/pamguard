package clickDetector;

import java.util.ListIterator;

import Localiser.bearingLocaliser.DetectionGroupLocaliser;
import PamDetection.AbstractLocalisation;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamProcess;
import SoundRecorder.RecorderControl;
import SoundRecorder.trigger.RecorderTrigger;
import SoundRecorder.trigger.RecorderTriggerData;

public class ClickTrainDetector extends PamProcess {

	ClickControl clickControl;

	ClickDetector clickDetector;

	PamDataBlock<ClickDetection> clickDataBlock;

	ClickTrainDataBlock clickTrains;

	ClickTrainDataBlock newClickTrains;

	private CTRecorderTrigger ctRecorderTrigger;

	private final double minAngleForFit = 10 * Math.PI / 180.;

	//	ClickTrainLogger clickTrainLogger;

	DetectionGroupLocaliser detectionGroupLocaliser;

	ClickTrainDetector(ClickControl clickControl, PamDataBlock<ClickDetection> clickDataBlock) {

		super(clickControl, clickDataBlock, "Click Train Detector");

		this.clickControl = clickControl;

		this.clickDetector = clickControl.getClickDetector();

		this.clickDataBlock = clickDataBlock;
		
		ctRecorderTrigger = new CTRecorderTrigger();

		clickDataBlock.addObserver(this);

		addOutputDataBlock(clickTrains = new ClickTrainDataBlock(clickControl, clickControl.getUnitName() + " Click Trains", 
				this, clickControl.clickParameters.channelBitmap));
		clickTrains.setLocalisationContents(AbstractLocalisation.HAS_BEARING | AbstractLocalisation.HAS_RANGE);
		clickTrains.addObserver(this);
		ClickTrainGraphics clickTrainGraphics = new ClickTrainGraphics(clickControl, clickTrains);
		clickTrains.setOverlayDraw(clickTrainGraphics);
		clickTrains.setRecordingTrigger(ctRecorderTrigger);

		newClickTrains = new ClickTrainDataBlock(clickControl, clickControl.getUnitName() + " New Click Trains", 
				this, clickControl.clickParameters.channelBitmap);
		newClickTrains.addObserver(this);
		//		addOutputDataBlock(newClickTrains);



		//		clickTrains.setOverlayDraw(new ClickTrainLocalisationGraphics(clickControl, clickTrains));
		//		clickTrains.SetLogging(clickTrainLogger = new ClickTrainLogger(clickTrains, clickControl, this));

		detectionGroupLocaliser = new DetectionGroupLocaliser(this);
	}

	public void clearAllTrains()
	{
		//clickTrains.();
	}

	@Override
	public long getRequiredDataHistory(PamObservable o, Object arg) {
		if (clickControl.clickParameters.runClickTrainId == false) return 0;
		if (o == clickTrains) return (long) (clickControl.clickParameters.iciRange[1] * 1000);//3600 * 1000;
		if (o == newClickTrains) return (long) (clickControl.clickParameters.iciRange[1] * 1000);	
		else if (o == clickDataBlock) return 60000; // 60s of data for now
		return 0;		
	}

	@Override
	public void newData(PamObservable o, PamDataUnit arg) {
		if (clickControl.clickParameters.runClickTrainId == false) return;
		if (o == clickDataBlock){
			ClickDetection click = (ClickDetection) arg;
			if (click.dataType != ClickDetection.CLICK_CLICK) {
				return;
			}
			if (click.getLocalisation() == null) {
				return;
			}
//			synchronized (clickTrains) {
			//				synchronized (newClickTrains) {
			processClick(click);
			closeOldTrains(clickTrains, arg.getTimeMilliseconds());
			closeOldTrains(newClickTrains, arg.getTimeMilliseconds());
//				}
//			}
		}
	}

	protected double closeTrainTime() {
		return clickControl.clickParameters.iciRange[1] * 1000;
	}

	private void closeOldTrains(ClickGroupDataBlock<ClickTrainDetection> dataBlock, long now) {
		// close trains that have not had a click added for > 10s;
		ClickTrainDetection aTrain;
		//		long now = PamCalendar.getTimeInMillis();
		synchronized  (dataBlock) {
			ListIterator<ClickTrainDetection> ctdIterator = dataBlock.getListIterator(0);
			while (ctdIterator.hasNext()) {
				aTrain = ctdIterator.next();
				//			if (aTrain.getTrainStatus() == ClickTrain.STATUS_BINME) continue;
				if (now - aTrain.getLastClick().getTimeMilliseconds() > closeTrainTime()){
					if (aTrain.getTrainStatus() == ClickTrainDetection.STATUS_STARTING) {
						ctdIterator.remove();
						//					dataBlock.remove(aTrain);
						aTrain.setTrainStatus(ClickTrainDetection.STATUS_BINME);	
					}
					else if (aTrain.getTrainStatus() == ClickTrainDetection.STATUS_OPEN){
						aTrain.setTrainStatus(ClickTrainDetection.STATUS_CLOSED);
					}
				}
			}
		}
		//		int i = 0;
		//		PamDataUnit aUnit;
		//		int n;
		//		while (i < (n = clickTrains.getUnitsCount())) {
		//			aUnit = clickTrains.getDataUnit(i, PamDataBlock.REFERENCE_CURRENT);
		//			aTrain = (ClickTrain) aUnit.data;
		//			if (aTrain.getTrainStatus() == ClickTrain.STATUS_BINME) {
		//				clickTrains.remove(aUnit);
		//			}
		//			else {
		//				i++;
		//			}
		//		}
	}

	protected ClickTrainDetection processClick(ClickDetection newClick) {
		ClickTrainDetection clickTrain = matchClickIntoGroup(newClick);
		if (clickTrain == null) return null;
		if ((clickTrain.getMaxAngle() - clickTrain.getMinAngle()) > minAngleForFit) {
			localiseClickTrain(clickTrain);
		}
		//		else {
		//			setBearingOnlyInfo(clickTrain);
		//		}
		return clickTrain;
	}

	protected ClickTrainDetection matchClickIntoGroup(ClickDetection newClick) {

		// look back in time for the most likely group of clicks. If there is no reasonable
		// group, create a new one - it may die quite soon if no clicks are added to it. 

		// make local copy of clickParameters.
		ClickParameters clickParameters = clickControl.clickParameters;

		ClickTrainDetection bestTrain = null;
		double bestScore = 0;
		double newScore;
		ClickTrainDetection aTrain;

		boolean startupTrain = false;
		ListIterator<ClickTrainDetection> ctdIterator;
		synchronized (clickTrains) {
			ctdIterator = clickTrains.getListIterator(0);
			while (ctdIterator.hasNext()) {
				aTrain = ctdIterator.next();
				//System.out.println("test train");
				if (aTrain.getTrainStatus() == ClickTrainDetection.STATUS_CLOSED) continue;
				//System.out.println("test train not closed");
				if (aTrain.getTrainStatus() == ClickTrainDetection.STATUS_BINME){
					continue;
				}
				//System.out.println("test train not for bin");
				newScore = aTrain.testClick(newClick);
				if (newScore > bestScore) {
					bestScore = newScore;
					bestTrain = aTrain;
					startupTrain = false;
				}
			}
		}
		//and do the same with new click trains
		synchronized (newClickTrains) {
			ctdIterator = newClickTrains.getListIterator(0);
			while (ctdIterator.hasNext()) {
				aTrain = ctdIterator.next();
				//System.out.println("test train");
				if (aTrain.getTrainStatus() == ClickTrainDetection.STATUS_CLOSED) continue;
				//System.out.println("test train not closed");
				if (aTrain.getTrainStatus() == ClickTrainDetection.STATUS_BINME){
					continue;
				}
				//System.out.println("test train not for bin");
				newScore = aTrain.testClick(newClick);
				if (newScore > bestScore) {
					bestScore = newScore;
					bestTrain = aTrain;
					startupTrain = true;
				}
			}
		}
		if (bestTrain != null) {
			// see if the quality of the fit was good enough ...
			bestTrain.addClick(newClick);
			/*
			 * everytime a click is added to a train, a new message is sent to 
			 * the recorders. If they are not recording, they will start, taking 
			 * the specified buffer. IF they are already recording, they will continue
			 * until the specified time has elapsed after the last click update. So
			 * if lots of clicks keep getting added to trains, then you get one long recording.
			 * 
			 */
			if (startupTrain && (bestTrain.getTrainStatus() == ClickTrainDetection.STATUS_OPEN)) {
				newClickTrains.remove(bestTrain);
				clickTrains.addPamData(bestTrain);
			}
			// otherwise, set bestTrain back to null
		}
		if (bestTrain == null) {
			// sniff around to see if there is another click in about the right place
			// to start a train and begin a train with two clicks.
			PamDataBlock<ClickDetection> clickBlock = clickDetector.getClickDataBlock();
			ClickDetection lastClick =  newClick;
			double lastClickAngle = lastClick.getAngle();
			ClickDetection aClick;
			double dT;
			double clickAngle;
			synchronized (clickBlock) {
				ListIterator<ClickDetection> clickIterator = clickBlock.getListIterator(PamDataBlock.ITERATOR_END);
				while (clickIterator.hasPrevious()) {
					//			int iUnit = clickBlock.getUnitsCount() - 1;
					//			while (--iUnit >= 0) {
					aClick = clickIterator.previous();
					if (aClick == newClick) {
						continue; // this will happen the first time. 
					}
					if (aClick.dataType != ClickDetection.CLICK_CLICK) {
						continue;
					}
					if (aClick.eventId != 0) continue; // already assigned
					dT = (newClick.getTimeMilliseconds() - aClick.getTimeMilliseconds()) / 1000.;
					if (dT > clickControl.clickParameters.iciRange[1]) break;
					clickAngle = aClick.getAngle();
					if (Math.abs(clickAngle - lastClickAngle) > clickParameters.okAngleError) continue;
					if (dT < clickParameters.iciRange[0] || dT > clickParameters.iciRange[1]) continue;
					// if it gets here, its a good enough click to start a train
					bestTrain = new ClickTrainDetection(clickControl, aClick);
					//newDataUnit = clickTrains.getNewUnit(aClick.startSample, 0, aClick.channelList);
					bestTrain.addClick(newClick);
					newClickTrains.addPamData(bestTrain);

				}
			}
		}
		return bestTrain;
	}

	protected void localiseClickTrain(ClickTrainDetection clickTrain) {
		/*
		 * clear ezisting localisation information, then let it re-add the data.
		 * There is a chance that a click train will go from good (has a track point) to 
		 * bad (not track) if the track wobbles badly, so this will alow the system to remove lousy 
		 * detections - should get rid of LR ambiguities if boat turns and one solution becomes impossible 
		 */ 
		clickTrain.clearFitData();
		boolean gotLoc = detectionGroupLocaliser.localiseDetectionGroup(clickTrain, 1);
		boolean gotLoc2 = detectionGroupLocaliser.localiseDetectionGroup(clickTrain, -1);
	}

	class CTRecorderTrigger extends RecorderTrigger {

		RecorderTriggerData recorderTriggerData = new RecorderTriggerData(clickControl.getUnitName() + " Click Trains", 
				10, 120);

		public RecorderTriggerData getDefaultTriggerData() {
			return recorderTriggerData;
		}

		@Override
		public boolean triggerDataUnit(PamDataUnit dataUnit, RecorderTriggerData rtData) {
			// TODO Auto-generated method stub
			return true;
		}

		

	}

	@Override
	public void pamStart() {

	}

	@Override
	public void pamStop() {

	}

	@Override
	public void setSampleRate(float sampleRate, boolean notify) {
		super.setSampleRate(sampleRate, false);
	}

}

package clickDetector;

import java.util.ListIterator;

import generalDatabase.SQLLogging;
import Localiser.bearingLocaliser.DetectionGroupLocaliser;
import Localiser.bearingLocaliser.GroupDetection;
import PamDetection.AbstractLocalisation;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamProcess;

/**
 * Something similar to ClickTrainDetector, but working on the output of tracked clicks so that 
 * accurate positions for groups of tracked clicks are calculated and can be displayed / stored.
 * 
 * @author Doug Gillespie
 *
 */
public class TrackedClickLocaliser extends PamProcess {

	ClickGroupDataBlock<TrackedClickGroup> trackedClickGroups;
	
	PamDataBlock<ClickDetection> clickDataBlock;
	
	ClickControl clickControl;
	
	DetectionGroupLocaliser detectionGroupLocaliser;
	
	public TrackedClickLocaliser(ClickControl clickControl, PamDataBlock<ClickDetection> clickDataBlock) {
		super(clickControl, clickDataBlock);
		this.clickControl = clickControl;
		this.clickDataBlock = clickDataBlock;
		setProcessName("Tracked Click Localiser");
		trackedClickGroups = new ClickGroupDataBlock<TrackedClickGroup>(TrackedClickGroup.class, 
				clickControl.getUnitName() + " Tracked Click localisations", this, clickControl.clickParameters.channelBitmap);
		addOutputDataBlock(trackedClickGroups);
//		trackedClickGroups.setOverlayDraw(new ClickTrainLocalisationGraphics(clickControl, trackedClickGroups));
//		trackedClickGroups.setOverlayDraw(new TrackedClickGraphics(clickControl, trackedClickGroups));
		trackedClickGroups.setOverlayDraw(new TrackedClickGraphics(trackedClickGroups));
		trackedClickGroups.setLocalisationContents(AbstractLocalisation.HAS_BEARING | AbstractLocalisation.HAS_RANGE |
				AbstractLocalisation.HAS_LATLONG | AbstractLocalisation.HAS_AMBIGUITY | AbstractLocalisation.HAS_PERPENDICULARERRORS);
		trackedClickGroups.SetLogging(new TrackedClickGroupLogging(trackedClickGroups, SQLLogging.UPDATE_POLICY_WRITENEW));
		detectionGroupLocaliser = new DetectionGroupLocaliser(this);
	}

	@Override
	public void newData(PamObservable o, PamDataUnit arg) {
		if (o == clickDataBlock) {
			TrackedClickGroup bestTrain = groupClicks((ClickDetection) arg);
//			if (bestTrain != null) { // localisation is done in groupclicks
//				localiseGroup(bestTrain);
//			}
		}
	}

	protected TrackedClickGroup groupClicks(ClickDetection newClick) {
		
//		all we need to do is find an event with the same event number and add the clicks to it. 

		if (newClick.eventId == 0) return null;
				
		TrackedClickGroup bestTrain = null;
		TrackedClickGroup aTrain;
		boolean newGroup = false;
		
		synchronized (trackedClickGroups) {
			
			ListIterator<TrackedClickGroup> tcIterator = trackedClickGroups.getListIterator(0);
			while (tcIterator.hasNext()) {
				aTrain = tcIterator.next();
				//System.out.println("test train");
				if (aTrain.getStatus() == ClickTrainDetection.STATUS_CLOSED) continue;
				if (aTrain.getStatus() == ClickTrainDetection.STATUS_BINME){
					continue;
				}
				if (newClick.eventId == aTrain.getEventId()) {
					bestTrain = aTrain;
					break; // no need to look further ...
				}
			}
		}
		if (bestTrain != null) {
			bestTrain.addSubDetection(newClick);
		}
		else {
			bestTrain = new TrackedClickGroup(newClick);
			newGroup = true;
		}
		
		localiseGroup(bestTrain);
		
		if (newGroup) {
			trackedClickGroups.addPamData(bestTrain);
		}
		else {
			trackedClickGroups.updatePamData(bestTrain, newClick.getTimeMilliseconds());
		}
		
		
		return bestTrain;
	}
	
	/*
	 * return true if a localisation is calculated, false otherwise. 
	 */
	public boolean localiseGroup(GroupDetection detectionGroup) {
		if (detectionGroup.getSubDetectionsCount() < 2) {
			return false;
		}
		/*
		 * work out the most likely crossing point of all the clicks
		 * taking their origins from the correct hydrophones, etc. 
		 * The final position will be given as a range and bearing relative to the 
		 * LAST click in the sequence. 
		 * 
		 * Want to do everything by distance, rather than the times used in the click train 
		 * localiser. Will therefore have to get hydrophone positions for every click - which 
		 * could take time, so will modify click localisation to only ever calculate these once
		 * (May have to add to AbstractLocalisation class to achieve this). Will also allow for 
		 * ship course changes, so it will be necessary to do the fit for both the L and R sides 
		 * of the vessel. 
		 * 
		 */ 
		detectionGroup.clearFitData();
		boolean gotLoc = detectionGroupLocaliser.localiseDetectionGroup(detectionGroup, 1);
		boolean gotLoc2 = detectionGroupLocaliser.localiseDetectionGroup(detectionGroup, -1);
		return gotLoc || gotLoc2;
	}

	protected double closeTrainTime() {
		return 600 * 1000.; // 10 minutes. 
	}

	@Override
	public void pamStart() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pamStop() {
		
	}

}

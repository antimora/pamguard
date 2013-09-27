package staticLocaliser;

import java.util.concurrent.ExecutionException;

import PamDetection.PamDetection;
import PamguardMVC.PamDataUnit;
import offlineProcessing.OfflineTask;
import clickDetector.ClickDetection;
import clickDetector.ClickTabPanelControl;
import clickDetector.offlineFuncs.OfflineEventDataUnit;
import dataMap.OfflineDataMapPoint;

public class BatchLocaliseOfflineTask extends OfflineTask<PamDetection>{
	
	StaticLocaliserControl staticLocaliserControl;
	
	public BatchLocaliseOfflineTask(StaticLocaliserControl staticLocaliserControl){
		super();
		this.staticLocaliserControl = staticLocaliserControl;
		setParentDataBlock(staticLocaliserControl.getCurrentDatablock());
		
	}
	

	@Override
	public String getName() {
		return "Batch Localise";
	}

	@Override
	public boolean processDataUnit(PamDetection pamDetection) {
		
		//the function may pick out an event, in which case we must cycle through all sub detections
		if (pamDetection  instanceof OfflineEventDataUnit){
			OfflineEventDataUnit offlineEvent=(OfflineEventDataUnit) pamDetection;
			System.out.println("Detection Count: "+offlineEvent.getSubDetectionsCount());
			for (int i=0; i<offlineEvent.getSubDetectionsCount(); i++){
				processDataUnit(offlineEvent.getSubDetection(i));
			}
			return true;
		}
		
		if (staticLocaliserControl.getStaticMainPanel().getCurrentControlPanel().canLocalise(pamDetection)==false){
			System.out.println("Cannot Localise: "+pamDetection);
			return true; 
		}
		
		staticLocaliserControl.getStaticLocaliser().setPamDetection(pamDetection);
		staticLocaliserControl.runBatch();
		
		System.out.println("Can Localise: "+pamDetection);

		while (staticLocaliserControl.getStaticLocaliser().getEventLocaliserWorker().isDone()==false){
			//do nothing
		}
	
		return true;
	}

	@Override
	public void newDataLoad(long startTime, long endTime,
			OfflineDataMapPoint mapPoint) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public boolean callSettings() {
		 staticLocaliserControl.getStaticMainPanel().openOptionsDialog();
		 return true;
	}


	@Override
	public boolean hasSettings() {
		return true;
	}

	@Override
	public void loadedDataComplete() {
		System.out.println("OK, we're all done!");
		
	}
	

}

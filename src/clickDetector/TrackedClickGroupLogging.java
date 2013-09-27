package clickDetector;

import PamDetection.PamDetection;
import PamguardMVC.PamDataUnit;

public class TrackedClickGroupLogging extends ClickGroupLogging {
	
	ClickGroupDataBlock<TrackedClickGroup> clickGroupDataBlock;
	

	public TrackedClickGroupLogging(ClickGroupDataBlock<TrackedClickGroup> pamDataBlock, int updatePolicy) {
		super(pamDataBlock, updatePolicy);

		setCanView(true);
		
		this.clickGroupDataBlock = pamDataBlock;
	}

	@Override
	protected boolean fillDataUnit(PamDetection pamDetection) {
		boolean basicFillOk =  super.fillDataUnit(pamDetection);
		
		return basicFillOk;
	}

	@Override
	synchronized protected PamDataUnit createDataUnit(long timeMilliseconds, int databaseIndex) {
		TrackedClickGroup tcg = null;
		boolean isUpdate = true;
//		Timestamp ts = (Timestamp) getTableDefinition().getTimeStampItem().getValue();
//		long t = PamCalendar.millisFromTimeStamp(ts);
		int updateIndex = (Integer) getTableDefinition().getUpdateReference().getValue();
		if (updateIndex > 0) {
			tcg = this.clickGroupDataBlock.findByDatabaseIndex(updateIndex);
		}
		if (tcg == null) {
			tcg = new TrackedClickGroup(timeMilliseconds, (Integer) getChannelMap().getValue(), 
					(Integer) getStartSample().getValue(), (Integer) getDuration().getValue());
			isUpdate = false;
		}
		else {
			tcg.setDuration((Integer) getDuration().getValue());
		}
		tcg.setDatabaseIndex(databaseIndex);
		
		fillDataUnit(tcg);
		
		if (isUpdate) {
			clickGroupDataBlock.updatePamData(tcg, tcg.getTimeMilliseconds());
		}
		else {
			clickGroupDataBlock.addPamData(tcg);
		}
//		
//		System.out.println(String.format("Current Id = %d update of %d",  tcg.getDatabaseIndex(), updateIndex));
//		System.out.print("Id's in block: ");
//		for (int i = 0; i < clickGroupDataBlock.getUnitsCount(); i++) {
//			System.out.print(String.format("%d, ", clickGroupDataBlock.getDataUnit(i, PamDataBlock.REFERENCE_CURRENT).getDatabaseIndex()));
//		}
//		System.out.println();
		
		return tcg;
	}
}

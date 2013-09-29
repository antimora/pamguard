package AIS;

import generalDatabase.PamTableDefinition;
import generalDatabase.PamTableItem;
import generalDatabase.SQLLogging;

import java.sql.Timestamp;
import java.sql.Types;

import PamUtils.PamCalendar;
import PamguardMVC.PamDataUnit;

public class AISLogger extends SQLLogging {

	private ProcessAISData aisProcess;
	private PamTableDefinition tableDef;
	private AISDataBlock aisDataBlock;
	protected PamTableItem imoNumber,callSign,shipName,shipType,	//messageID,
	eta,	draft,	destination,	//dataTerminalReady,	//dataIndicator,
	navigationStatus,	rateOfTurn,	speedOverGround,	//positionAccuracy,
	longitude,	latitude,	courseOverGround,	trueHeading;
	protected PamTableItem dataString;

	static private final int AISRAWLENGTH = 128;
	//utcSeconds,	//utcMinutes,	//utcHours,
	//repeatIndicator,
	//commsState;

	
	public AISLogger(AISDataBlock aisDataBlock, ProcessAISData aisProcess) {
		super(aisDataBlock);
		this.aisProcess = aisProcess;
		this.aisDataBlock = aisDataBlock;

		setUpdatePolicy(SQLLogging.UPDATE_POLICY_WRITENEW);

		setCanView(true);

		PamTableItem tableItem;
		tableDef = new PamTableDefinition("AISData", getUpdatePolicy());
		tableDef.addTableItem(tableItem = new PamTableItem("GpsIndex", Types.INTEGER));
		tableItem.setCrossReferenceItem("GpsData", "Id");
		tableDef.addTableItem(imoNumber = new PamTableItem("imoNumber", Types.INTEGER));
		tableDef.addTableItem(callSign = new PamTableItem("callSign", Types.CHAR, 7));
		tableDef.addTableItem(shipName = new PamTableItem("shipName", Types.CHAR, 20));
		tableDef.addTableItem(shipType = new PamTableItem("shipType", Types.CHAR, 20));
		tableDef.addTableItem(eta = new PamTableItem("eta", Types.TIMESTAMP));
		tableDef.addTableItem(draft = new PamTableItem("draft", Types.DOUBLE));
		tableDef.addTableItem(destination = new PamTableItem("destination", Types.CHAR, 20));
		tableDef.addTableItem(navigationStatus = new PamTableItem("navigationStatus", Types.INTEGER));
		tableDef.addTableItem(rateOfTurn = new PamTableItem("rateOfTurn", Types.DOUBLE));
		tableDef.addTableItem(speedOverGround = new PamTableItem("speedOverGround", Types.DOUBLE));
		tableDef.addTableItem(latitude = new PamTableItem("latitude", Types.DOUBLE));
		tableDef.addTableItem(longitude = new PamTableItem("longitude", Types.DOUBLE));
		tableDef.addTableItem(courseOverGround = new PamTableItem("courseOverGround", Types.DOUBLE));
		tableDef.addTableItem(trueHeading = new PamTableItem("trueHeading", Types.DOUBLE));
		tableDef.addTableItem(dataString = new PamTableItem("Data String", Types.CHAR, AISRAWLENGTH));
		tableDef.setUseCheatIndexing(true);

		setTableDefinition(tableDef);
	}

//	@Override
//	public PamTableDefinition getTableDefinition() {
//		return tableDef;
//	}

	@Override
	public void setTableData(PamDataUnit pamDataUnit) {

		AISDataUnit aisData = (AISDataUnit) pamDataUnit;

		//		dateTime.setValue(PamCalendar.getTimeStamp(aisData.getTimeMilliseconds()));
		imoNumber.setValue(aisData.mmsiNumber);
		AISStaticData staticData = aisData.getStaticData();
		AISPositionReport positionReport = aisData.getPositionReport();
		if (staticData != null) {
			callSign.setValue(staticData.callSign);
			shipName.setValue(staticData.shipName);
			shipType.setValue(staticData.getStationTypeString(aisData.stationType, staticData.shipType));
			eta.setValue(PamCalendar.getTimeStamp(staticData.etaMilliseconds));
			draft.setValue(staticData.staticDraught);
			destination.setValue(staticData.destination);
		}
		else {
			callSign.setValue(null);
			shipName.setValue(null);
			shipType.setValue(aisData.getStationtypeString());
			eta.setValue(null);
			draft.setValue(null);
			destination.setValue(null);
		}
		if (positionReport != null) {
			navigationStatus.setValue(positionReport.navigationStatus);
			rateOfTurn.setValue(positionReport.rateOfTurn);
			speedOverGround.setValue(positionReport.speedOverGround);
			latitude.setValue(positionReport.getLatitude());
			longitude.setValue(positionReport.getLongitude());
			courseOverGround.setValue(positionReport.courseOverGround);
			trueHeading.setValue(positionReport.trueHeading);
		}
		else {
			navigationStatus.setValue(null);
			rateOfTurn.setValue(null);
			speedOverGround.setValue(null);
			latitude.setValue(null);
			longitude.setValue(null);
			courseOverGround.setValue(null);
			trueHeading.setValue(null);
		}
		dataString.setValue(aisData.charData);

	}

	private int lastMMONumber = 0;
	private long lastTime = 0;
	@Override
	protected PamDataUnit createDataUnit(long timeMilliseconds, int databaseIndex) {
		AISStaticData aisStaticData = null;
		AISPositionReport aisPositionReport = null;

//		Timestamp ts = (Timestamp) tableDef.getTimeStampItem().getValue();
//		long t = PamCalendar.millisFromTimeStamp(ts);

		Timestamp etaTime;

		int aimoNumber;
		// static data
		String acallSign = null;
		String ashipName;
		String ashipType;
		long aetaMillis;
		double adraught;
		String adestination;
		// position report
		int anavStatus;
		double arateOfTurn;
		double aspeedOverGround;
		double alatitude;
		double alongitude;
		double acourseOverGround; 
		double atrueHeading;
		String aisString;

		aimoNumber = (Integer) imoNumber.getValue();

		/*
		 * Problem in some 2008 data where all records are repeated
		 * So check for identical consecutive records. 
		 */
		if (lastMMONumber == aimoNumber && lastTime == timeMilliseconds) {
			return null;
		}
		else {
			lastMMONumber = aimoNumber;
			lastTime = timeMilliseconds;
		}

		if (callSign.getValue() != null) {
			// assume all static data are OK
			acallSign = (String) callSign.getValue();
			ashipName = (String) shipName.getValue();
			ashipType = (String) shipType.getValue();
			etaTime = (Timestamp) eta.getValue();
			aetaMillis = PamCalendar.millisFromTimeStamp(etaTime);
			//			aetaMillis = (Long) eta.getValue();
			adraught = (Double) draft.getValue();
			adestination = (String) destination.getValue();
			aisStaticData = new AISStaticData(acallSign, ashipName, 0,
					aetaMillis, adraught, adestination);
		}
		if (navigationStatus.getValue() != null) {
			anavStatus = (Integer) navigationStatus.getValue();
			arateOfTurn = (Double) rateOfTurn.getValue();
			aspeedOverGround = (Double) speedOverGround.getValue();
			alatitude = (Double) latitude.getValue();
			alongitude = (Double) longitude.getValue();
			acourseOverGround = (Double) courseOverGround.getValue();
			atrueHeading = (Double) trueHeading.getValue();
			aisPositionReport = new AISPositionReport(lastTime, anavStatus, arateOfTurn,
					aspeedOverGround, alatitude, alongitude, acourseOverGround, atrueHeading);
			aisPositionReport.timeMilliseconds = timeMilliseconds;
		}
		aisString = (String) dataString.getValue();
		int nBytes = (int) Math.floor(aisString.length() * 6 / 8);
		int nBits = nBytes * 8;
		int fillBits = aisString.length() * 6 - nBits;

		AISDataUnit aisDataUnit = new AISDataUnit(timeMilliseconds);
		aisDataUnit.setDatabaseIndex(databaseIndex);
		aisDataUnit.mmsiNumber = aimoNumber;
		if (aisStaticData != null) {
			aisDataUnit.setStaticData(aisStaticData);
		}
		if (aisPositionReport != null) {
			aisDataUnit.addPositionReport(aisPositionReport);
		}
		aisDataUnit.charData = aisString.trim();
		aisDataBlock.addAISData(aisDataUnit);
		return aisDataUnit;

	}

}

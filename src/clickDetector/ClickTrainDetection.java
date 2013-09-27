package clickDetector;

import PamUtils.LatLong;

public class ClickTrainDetection extends ClickGroupDetection {
	
	ClickControl clickControl;
	
	static public final int STATUS_STARTING = 0;
	static public final int STATUS_OPEN = 1;
	static public final int STATUS_CLOSED = 2;
	static public final int STATUS_BINME = 3;
	
	private int trainStatus = STATUS_STARTING;
	private double minAngle, maxAngle;
	double runningICI = -1;
	long lastClickTime;
	double firstClickAngle, lastFittedAngle;
	int lastFittedUnitIndex = -1;
	ClickDetector clickDetector;
	private ClickDetection lastClick;
	
	private static int globalEventId;
	private int trainId;
	
	private boolean shouldPlot = true;
	
		
	ClickTrainDetection(ClickControl clickControl, ClickDetection click) {
		
		super(click);
				
		this.clickControl = clickControl;
		
		firstClickAngle = minAngle = maxAngle = click.getLocalisation().getBearing(0);
		
		clickDetector = clickControl.getClickDetector();
		
//		int hydrophones = ((AcquisitionProcess) clickDetector.getSourceProcess()).		
//		getAcquisitionControl().ChannelsToHydrophones(getChannelBitmap());
	
		addClick(click);
		
		
		
		
	}
	
	
	double testClick (ClickDetection click) {
		if (click.dataType == ClickDetection.CLICK_NOISEWAVE || 
				click.getLocalisation() == null) {
			return 0;
		}
		//System.out.println(String.format("channelMap = %d, new click map = %d", channelMap, dataUnit.getChannelBitmap()));
		
		if (trainStatus == STATUS_CLOSED) return 0;
		
		if (getChannelBitmap() != click.getChannelBitmap()) return 0;
		
		// make local copy of clickParameters.
		ClickParameters clickParameters = clickControl.clickParameters;
		
		
		// check the angle difference
		double clickAngle = click.getAngle();
		clickAngle = click.getLocalisation().getBearing(0) * -1 + 
		(90-click.getPairAngle(0, false)) * Math.PI/180.;
		double exAngle = expectedAngle(click, 1);
		double angleError = clickAngle - exAngle;
		
		double expectedAngleError = 2.* Math.PI/180.;
		
		if (Math.abs(angleError / expectedAngleError) > 2.) return 0;
		
		// check the ICI difference
//		double newICI = (double) (click.getStartSample() - lastClickTime) / (double) clickDetector.getSampleRate();
		double newICI = getICI(click);
		if (newICI < 0) return 0;
		double iciRatio = clickParameters.maxIciChange;
		if (runningICI < 0) {
			if (newICI < clickParameters.iciRange[0] || newICI > clickParameters.iciRange[1]) return 0;
		}
		else {
			iciRatio = newICI / runningICI;
			if (iciRatio < 1.) iciRatio = 1./iciRatio;
		}
		if (iciRatio > clickParameters.maxIciChange) return 0;
		
		/*
		 * Had multiplied these two together, but that's not sensible since if there is no angle change
		 * the goodness would always come out at zero - changed to + on 12Feb 2008. 
		 */
		double badness = Math.pow(angleError / expectedAngleError, 2.) + Math.pow(iciRatio, 2.);
		
		return 1/badness;
	}
	
	double getICI(ClickDetection newClick) {
		ClickDetection exClick;
		synchronized (getSubDetectionSyncronisation()) {
			for (int i = getSubDetectionsCount()-1; i >= 0; i--) {
				exClick = getSubDetection(i);
				if (exClick.getChannelBitmap() == newClick.getChannelBitmap()) {
					return getICI(newClick, exClick);
				}
			}
		}
		return -1;
	}
	
	double getICI(ClickDetection secondClick, ClickDetection firstClick) {
		return (secondClick.getTimeMilliseconds() - firstClick.getTimeMilliseconds())/1000.;
	}
	
	// return the expected angle in radians.
	double expectedAngle(ClickDetection click, int side) {
		/*
		 * if a position already exists, then it's possible to work out the 
		 * expected angle.
		 * Otherwise, the expected angle is just the last angle for those channel 
		 * numbers.
		 * If no channels exist for those channel numbers, then it's NaN.
		 */   
		int iSide = 0;
		if (side != 1) {
			iSide = 1;
		}
		LatLong clickLatLong = click.getOriginLatLong(false);
		if (clickLatLong == null) return Double.NaN;
		if (getLatLong(iSide) != null) {
			double expBearing = clickLatLong.bearingTo(getLatLong(iSide));
			return (90 - expBearing) * Math.PI / 180.;
		}
		// if that didn't work, then find the last click that
		// has the same channel numbers and that's the angle. 
		ClickDetection lastClick;
		synchronized(getSubDetectionSyncronisation()) {
		for (int i = getSubDetectionsCount()-1; i >= 0; i--) {
			lastClick = getSubDetection(i);
			if (lastClick == null) {
				return Double.NaN;
			}
			if (lastClick.getChannelBitmap() == click.getChannelBitmap()) {
				return lastClick.getLocalisation().getBearing(iSide) * -side + 
				(90-lastClick.getPairAngle(0, false)) * Math.PI/180.;
			}
		}
		}
		return Double.NaN;
//		double x = (double) (eT0 - t) * currentSpeed / 1000.;
//		return Math.atan2(eRange, x) * 180. / Math.PI;
	}
	
	boolean addClick(ClickDetection click) {
		
//		boolean ok =  clickList.add(click);
		addSubDetection(click);
		
		if (getLocalisation() != null && getLocalisation().getArrayOrientationVectors() == null) {
			if (click.getLocalisation() != null) {
				getLocalisation().setArrayAxis(click.getLocalisation().getArrayOrientationVectors());
			}
		}

//		eventEnd = click.getTimeMilliseconds();
		
		maxAngle = Math.max(maxAngle, click.getLocalisation().getBearing(0));
		minAngle = Math.min(minAngle, click.getLocalisation().getBearing(0));
		
		if (trainStatus == STATUS_OPEN) {
			click.setEventId(trainId);
		}
		else if (trainStatus == STATUS_STARTING) {
			if (getSubDetectionsCount() >= clickControl.clickParameters.minTrainClicks) {
				trainId = ++globalEventId;
				trainStatus = STATUS_OPEN;
				for (int i = 0; i < getSubDetectionsCount(); i++) {
					(getSubDetection(i)).setEventId(trainId);
				}
			}
		}
		
		if (lastClickTime > 0) {
			double newICI = (double) (click.getStartSample() - lastClickTime) / (double) clickDetector.getSampleRate();
			click.setICI(newICI);
			if (runningICI > 0){
				runningICI = (1.0 - clickControl.clickParameters.iciUpdateRatio) * runningICI + 
					clickControl.clickParameters.iciUpdateRatio * newICI;
			}
			else {
				runningICI = newICI;
			}
		}
		else {
			click.setICI(0);
		}
		
		// see if it's possible to update the distance off ...
//		double thisClickAngle = click.getAngle();
//		if (Math.abs(thisClickAngle - lastFittedAngle) > 10) {
//			// need to avoid putting too much data into the fit or it will kill the CPU.
////			fitWhalePos();
//			lastFittedUnitIndex = getSubDetectionsCount();
//		}
//		else {
//			// just update it's initial position a bit for if it stays on this bearing for ages. 
//			eT0 = click.getTimeMilliseconds() - eventStart + (1000. * eRange / currentSpeed / Math.tan(thisClickAngle * Math.PI / 180.));
//		}
		
		lastClickTime = click.getStartSample();
		
		lastClick = click;
		
//		currentSpeed = clickSpeed(click);
		
		return true;
	}
	

	public int getTrainStatus() {
		return trainStatus;
	}


	public void setTrainStatus(int trainStatus) {
		this.trainStatus = trainStatus;
	}


	public int getTrainId() {
		return trainId;
	}


	public void setTrainId(int trainId) {
		this.trainId = trainId;
	}


	public double getMaxAngle() {
		return maxAngle;
	}


	public double getMinAngle() {
		return minAngle;
	}


	public long getLastClickTime() {
		return lastClickTime;
	}


	public ClickDetection getLastClick() {
		return lastClick;
	}


	public boolean isShouldPlot() {
		if (getNumLatLong() > 0) return true;
		return shouldPlot;
	}


	public void setShouldPlot(boolean shouldPlot) {
		this.shouldPlot = shouldPlot;
	}



}

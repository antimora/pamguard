package whistleDetector;

import PamDetection.PamDetection;

public class EventDataUnit extends PamDetection<ShapeDataUnit, PamDetection> {

	static public final int STATUS_OPEN = 1;
	
	static public final int STATUS_CLOSED = 2;
	
	public long startTimeMillis;
	
	public long endTimeMillis;
	
	public int status;
	
	EventDataUnit(ShapeDataUnit wslShape) {
		super(wslShape.getTimeMilliseconds(), wslShape.getChannelBitmap(),
				wslShape.getStartSample(), wslShape.getDuration());
		status = STATUS_OPEN;
		endTimeMillis = startTimeMillis = wslShape.getTimeMilliseconds();
	}
	
	public void addWhistle(ShapeDataUnit wslShape) {
		addSubDetection(wslShape);
		wslShape.addSuperDetection(this);
		duration = wslShape.getStartSample() + wslShape.getDuration() - getSubDetection(0).getStartSample();
		endTimeMillis = wslShape.getTimeMilliseconds();
	}
	
	public int getWhistleCount() {
		return getSubDetectionsCount();
	}

	@Override
	public long getDuration() {
		return endTimeMillis = startTimeMillis;
	}
	
	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}
}

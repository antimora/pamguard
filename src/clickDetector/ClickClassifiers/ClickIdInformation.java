package clickDetector.ClickClassifiers;

public class ClickIdInformation {

	public int clickType;
	
	public boolean discard;

	public ClickIdInformation(int clickType, boolean discard) {
		super();
		this.clickType = clickType;
		this.discard = discard;
	}

	public ClickIdInformation(int clickType) {
		super();
		this.clickType = clickType;
	}
	
}

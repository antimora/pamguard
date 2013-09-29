package videoRangePanel;

import java.util.ArrayList;

public class VRRangeMethods {
	
	private VRControl vrControl;
	
	private ArrayList<VRRangeMethod> methods = new ArrayList<VRRangeMethod>();
	
	private ArrayList<String> names = new ArrayList<String>();
	
	private int currentMethodId = METHOD_ROUND;
	
	private VRRangeMethod currentMethod;
	
	public static final int METHOD_ROUND = 0;
	public static final int METHOD_REFRACTION = 1;

	public VRRangeMethods(VRControl vrControl) {
		super();
		this.vrControl = vrControl;
		addMethod(new RoundEarthMethod(vrControl), "Round Earth Method");
		addMethod(new RefractionMethod(vrControl), "Refraction Method");
		setCurrentMethodId(METHOD_ROUND);
	}
	
	private void addMethod(VRRangeMethod method, String name) {
		
		methods.add(method);
		names.add(name);
	}
	
	public VRRangeMethod getCurrentMethod() {
		return currentMethod;
	}
	
	public void setCurrentMethodId(int methodId) {
		currentMethodId = methodId;
		currentMethod = methods.get(methodId);
	}

	public ArrayList<String> getNames() {
		return names;
	}
	
	public VRRangeMethod getMethod(int index) {
		return methods.get(index);
	}
}

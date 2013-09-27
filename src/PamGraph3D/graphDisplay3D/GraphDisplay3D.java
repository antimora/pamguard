//package PamGraph3D.graphDisplay3D;
//
//import java.awt.BorderLayout;
//import java.awt.Cursor;
//import java.awt.Dimension;
//import java.awt.Frame;
//import java.awt.Window;
//import java.awt.event.ComponentEvent;
//import java.awt.event.ComponentListener;
//import java.awt.event.MouseAdapter;
//import java.awt.event.MouseEvent;
//import java.util.ArrayList;
//import java.util.ListIterator;
//
//import javax.media.j3d.BoundingSphere;
//import javax.media.j3d.BranchGroup;
//import javax.media.j3d.Transform3D;
//import javax.media.j3d.TransformGroup;
//import javax.swing.JPanel;
//import javax.swing.JPopupMenu;
//import javax.vecmath.Matrix3f;
//import javax.vecmath.Point3d;
//import javax.vecmath.Vector3d;
//import javax.vecmath.Vector3f;
//
//
//import com.sun.j3d.utils.picking.PickCanvas;
//import com.sun.j3d.utils.picking.PickResult;
//
//
//import PamGraph3D.PamPanel3D;
//import PamView.PamPanel;
//import PamguardMVC.PamDataBlock;
//import PamguardMVC.PamDataUnit;
//
//import com.sun.j3d.utils.geometry.Sphere;
//
///**
//* This is a basic template for a Java3D graph. This function is useless unless combined with two AbstractGraphDisplay3DAxis. At the simples an axis can be a max and a min, however more
//* complicated axis can be created which contain scroll bars etc. This class provides the interactive framework to make Java3d work with these axis, including window aspect ratio policies, resize options etc etc. A datablock or multiple datablocks are required for the graph values. The actual number to take form each dataUnit is specified in the axis class. For exampe, the time axis getMeasurement function would
//* return PamDataUnit.getTimeMillis, whilst a bearing axis would return PamDataUnit.getLocalisation.getAngles[0]. 
//* <p>Hence this class is extremely flexible and can be used for 
//* 
// * Examples of possible uses: 
// * Bearing time display; 
// * Localisation result graphs; 
// * Click plots
// * ....
// * 
// * Java3d - detections -java3d units
// * -translate, must convert units. 
// * 
// * 
// * @author Jamie Macaulay
// *
// */
//public class GraphDisplay3D extends PamPanel{
//	
//	private static final long serialVersionUID = 1L;
//	PamPanel3D graph3D;
//	BranchGroup detectioGroup;
//	JPanel mainPanel;
//	private Frame frame;
//	
//	//data symbols
//	private float highLightSize=1.5f;
//	private DataUnitShape3D currentSelectedDetection=null;
//	
//	//data
//	ArrayList<PamDataBlock> pamDataBlocks;
//	
//	//3d data
//	BranchGroup detectionsGroup;
//	BranchGroup mainBranchGroup;
//
//	//Axis
//	AbstractGraphDisplay3DAxis horzAxis;
//	AbstractGraphDisplay3DAxis vertAxis;
//	private double horizontalRange=1;
//	private double verticalRange=1;
//	
//	//scrollBar Java3d translate for graph 
//	private double horzScrollPos=0;
//	private double vertScrollPos=0;
//	
//	//window size;
//	private double y;
//	private double x;
//	
//	//flags
//	boolean setSymbolAspectRatio=true;
//
//
//	public GraphDisplay3D(Frame frame){
//		
//		this.frame=frame;
//		
//		horizontalRange=2;
//		
//		//initialise the java3d stuff. 
//		graph3D=new PamPanel3D(frame,true);
//		graph3D.getCanvas3D().addMouseListener(new MousePick(graph3D.getPickCanvas()));
//		graph3D.getPickCanvas().setMode(PickCanvas.BOUNDS);
//		graph3D.removeLight();
//		graph3D.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
//		mainPanel=new JPanel(new BorderLayout());
//		mainPanel.add(graph3D);
//		
//		//create 3d branchGroups
//		//The detection group MUST only contain datablock detections. 
//		detectionsGroup=new BranchGroup();
//		detectionsGroup.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
//		detectionsGroup.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
//		detectionsGroup.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
//		detectionsGroup.setCapability(BranchGroup.ALLOW_DETACH);
//		
//		mainBranchGroup=new BranchGroup();
//		mainBranchGroup.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
//		mainBranchGroup.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
//		mainBranchGroup.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
//		mainBranchGroup.setCapability(BranchGroup.ALLOW_DETACH);
//		mainBranchGroup.setCapability(BranchGroup.ENABLE_PICK_REPORTING);
//		mainBranchGroup.addChild(detectionsGroup);
//		
//		//add components.
//		this.setLayout(new BorderLayout());
//		this.add(BorderLayout.CENTER,mainPanel);
//		this.addComponentListener(new WindowResize());
//
//		graph3D.getGraphicsGroup().addChild(mainBranchGroup);
//				
//	}
//	
//	public void setHorizontalAxis(AbstractGraphDisplay3DAxis horzAxis){
//		this.horzAxis=horzAxis;
//		this.add(BorderLayout.SOUTH, horzAxis.getPanel());
//		this.validate();
//	}
//	
//	public AbstractGraphDisplay3DAxis getHorizontalAxis(){
//		return horzAxis;
//	}
//		
//	
//	public void setVerticalAxis(AbstractGraphDisplay3DAxis vertAxis){
//		this.vertAxis=vertAxis;
//		this.add(BorderLayout.EAST, vertAxis.getPanel());
//		this.validate();
//	}
//	
//	public AbstractGraphDisplay3DAxis getVerticalAxis(){
//		return vertAxis;
//	}
//		
//	
//	/**
//	 * Set aspect ratio. Automatically compensates for the window size. 
//	 * @param xi
//	 * @param yi
//	 */
//	public void setAspectRatio(double xi, double yi){
//		Dimension panelSize=this.getSize();
//		y=Math.abs(panelSize.getHeight());
//		x=Math.abs(panelSize.getWidth());
//		if (x==0 && y==0) return;
//		graph3D.transformAspectRatio(new Vector3d(1/xi,(y/x)/yi,0.05));
//		graph3D.transformTranslationG(new Vector3d(-(horzScrollPos+1),-(y/x)*(vertScrollPos+1),0));
//	}
//	
//	/**
//	 * Changes the x and y axis to show the specified range. This is achieved by changing the aspect ratio.
//	 */
//	public void rangeValueChanged(){
//		//changes the aspect ratio of the graph to the corresponding scale set by the rangeSpinner. 
//		horizontalRange=horzAxis.getRange();
//		verticalRange=vertAxis.getRange();
//		//System.out.println("Horz: "+horizontalRange+" Vert: "+verticalRange);
//		if (horizontalRange==0.0 || verticalRange==0.0) return;
//		setAspectRatio((horizontalRange/2),(verticalRange/2)); 
//		setScrollBars();
//		if (setSymbolAspectRatio==true) setAspectRatioAllData();
//		
//	}
//	
//	
//	/**
//	 * Picks a shape3D when click on. Will bring up two pop up menus depending on whether a shape is slecected or not. Pop up menus are specified by each axis.
//	 * @author spn1
//	 *
//	 */
//	private class MousePick extends MouseAdapter{
//		
//		PickCanvas pickCanvas;
//		
//		public MousePick(PickCanvas pickCanvas){
//			this.pickCanvas=pickCanvas;
//		}
//		
//		public void mouseClicked(MouseEvent e){
//
//			pickCanvas.setShapeLocation(e.getX(),e.getY());
//			PickResult result = pickCanvas.pickClosest();
//			
//			//return all shapes to blue
//			if (result==null){
//				System.out.println("PickedShapes: "+null);
//				if (e.getButton()==MouseEvent.BUTTON3 || e.getButton()==MouseEvent.BUTTON2){
//				jMenuBlankSpace().show(mainPanel,e.getX(),e.getY());
//				}
//				return;
//			}
//			
//			//the previous highlighted shape to it's original size and position
//			
//			TransformGroup s = (TransformGroup)result.getNode(PickResult.TRANSFORM_GROUP);	
//			DataUnitShape3D selectedShape=(DataUnitShape3D) s.getChild(0);
//			PamDataUnit selectedDetection=selectedShape.getdataUnit();
//			
//			setSelectedDetection(selectedDetection);
//			detectionSelected(currentSelectedDetection.getdataUnit());
//		
//			if (e.getButton()==MouseEvent.BUTTON3 || e.getButton()==MouseEvent.BUTTON2){
//				JMenuSelDetection().show(mainPanel,e.getX(),e.getY());
//			}
//			
//		}
//	}
//	
//	/**
//	 * Set the size a slected detection is amplified by.
//	 * @param size
//	 */
//	public void setHighLightZoom(float size){
//		this.highLightSize=size;
//	}
//	
////	public void changeSymbolSize(TransformGroup shape, double size){
////		Transform3D trsf3D=new Transform3D();
////		shape.getTransform(trsf3D);
////		Vector3d trans=new Vector3d();
////		trsf3D.get(trans);
////		trsf3D.setTranslation(new Vector3d(trans.getX(), trans.getY(), +0.1));
////		trsf3D.setScale(new Vector3d(size,size,size));
////		shape.setTransform(trsf3D);
////	}
//	
//	/**
//	 * Pop up menu if blank space on the graph is clicked
//	 * @return
//	 */
//	public JPopupMenu jMenuBlankSpace(){
//		JPopupMenu popMenu=new JPopupMenu();
//		popMenu.setDefaultLightWeightPopupEnabled(false);
//		horzAxis.addMenuItems(popMenu);
//		vertAxis.addMenuItems(popMenu);
//		return popMenu;
//	}
//	
//	/**
//	 * Pop up menu if a data unit on the graph is selected
//	 * @return
//	 */
//	public JPopupMenu JMenuSelDetection(){
//		JPopupMenu popMenu=new JPopupMenu();
//		popMenu.setDefaultLightWeightPopupEnabled(false);
//		horzAxis.addMenuItems(popMenu);
//		vertAxis.addMenuItems(popMenu);
//		//popMenu.add(new JSeparator());
//		return popMenu;
//	}
//	
//	/**
//	 *Called everytime a Shape3D is selected.  Override this function to perform operations if a detection is selected. Allows the graph to be plugged into other modules, displays etc. ;
//	 */
//	public void detectionSelected(PamDataUnit pamDataUnit){
//		
//	}
//	
//	/**
//	 * Listens for window resize and changes the aspect ratio accordingly. 
//	 * @author spn1
//	 *
//	 */
//	class WindowResize implements ComponentListener{
//		
//		public void componentResized(ComponentEvent e) {
//		   setAspectRatio(horizontalRange/2,verticalRange/2);
//		   setAspectRatioAllData();
//		}
//
//
//		@Override
//		public void componentHidden(ComponentEvent arg0) {		
//		}
//
//		@Override
//		public void componentMoved(ComponentEvent arg0) {		
//		}
//
//		@Override
//		public void componentShown(ComponentEvent arg0) {
//		}
//		
//	}
//	
//	public float unit2Java3d(double unit, AbstractGraphDisplay3DAxis axis){
//		float unit3d=(float) (unit-axis.getAxisMin());	
//		return unit3d;
//	}
//	
//	
//	
//	/**
//	 * This function calculates the Java3d translation required for a movement of a scroll bar. 
//	 * @param scrollunit - the scroll bar value
//	 * @param scrollMax -the scroll bar max
//	 * @param scrollMin -the scroll bar min.
//	 * @param unitMax -the maximum value of the current data
//	 * @param unitMin - the minimum value of the current data
//	 * @param unitVisRange - the zoom factor (the range of units to see in the screen)
//	 * @return 
//	 */
//	private static Double getScrollBarTranslate(double scrollunit, double scrollMax, double scrollMin, double unitMax, double unitMin, double unitVisRange ){
//		double unitFrac=Math.abs((double) (scrollunit-scrollMin)/(double )(scrollMax-scrollMin));
//		double maxTranslate=2*((unitMax-unitMin-unitVisRange)/(unitVisRange));
//		Double j3dUnit=unitFrac*maxTranslate;
//		if (j3dUnit.equals(Double.NaN) || j3dUnit.equals(null) || j3dUnit.equals(Double.POSITIVE_INFINITY) ||  j3dUnit.equals(Double.NEGATIVE_INFINITY)) return 0.0;
//				
//		return j3dUnit;
//	}
//	
//	/**Add detection to the graph. The dataBlock contains either a reference to the default symbol, which is a blue sphere, or may have a specific symbol if the default get3DPlotSymbol has been overridden. 
//	 * 
//	 * @param pamDataBlock
//	 */
//	public void addDataBlock(ArrayList<PamDataBlock> pamDataBlocks){
//		
//		
//		this.pamDataBlocks=pamDataBlocks;
//
//		horizontalRange=horzAxis.getAxisMax();
//	
//		mainBranchGroup.removeAllChildren();
//		
//		detectionsGroup=new BranchGroup();
//		
//		detectionsGroup.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
//		detectionsGroup.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
//		detectionsGroup.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
//		detectionsGroup.setCapability(BranchGroup.ALLOW_DETACH);
//		
//////		//*****Test****//
////		detectionsGroup.addChild(PamShapes3D.createSphere(new Point3f(0,0.5f,0), PamShapes3D.sphereAppearanceMatt(PamShapes3D.green), 0.09f));
////		detectionsGroup.addChild(PamShapes3D.createSphere(new Point3f(5,1.5f,0), PamShapes3D.sphereAppearanceMatt(PamShapes3D.red), 0.09f));
////		detectionsGroup.addChild(PamShapes3D.createSphere(new Point3f(10,3f,0), PamShapes3D.sphereAppearanceMatt(PamShapes3D.red), 0.09f));
////		detectionsGroup.addChild(PamShapes3D.createSphere(new Point3f(0,3f,0), PamShapes3D.sphereAppearanceMatt(PamShapes3D.red), 0.09f));
////		detectionsGroup.addChild(PamShapes3D.createSphere(new Point3f(0,6.28f,0), PamShapes3D.sphereAppearanceMatt(PamShapes3D.grey), 0.2f));
////		detectionsGroup.addChild(PamShapes3D.createSphere(new Point3f(2,5f,0), PamShapes3D.sphereAppearanceMatt(PamShapes3D.grey), 0.09f));
//////		//*****Test****//
//		long time1=System.currentTimeMillis();
//
//		for (int i=0; i<pamDataBlocks.size();i++){
//	
//			ListIterator<PamDataUnit> listIterator=pamDataBlocks.get(i).getListIterator(0);
//			PamDataUnit unit=pamDataBlocks.get(i).getFirstUnit();
//			if (unit==null) return;
//			//drawDetection(unit,  detectionsGroup);	
//			while (listIterator.hasNext()){
//				unit=listIterator.next();
//				drawDetection(unit,  detectionsGroup);
//			}
//		}
//		
//		mainBranchGroup.addChild(detectionsGroup);
//		
//		if (setSymbolAspectRatio==true) setAspectRatioAllData();
//
//		long time2=System.currentTimeMillis();
//		
//		System.out.println("Time to add Data: "+(time2-time1));
//	}
//	
//	public void refreshData(){
//		if (pamDataBlocks==null) return;
//		addDataBlock(pamDataBlocks);
//	}
//	
//	/**
//	 * Draw the detections on the graph;
//	 * @param unit
//	 * @param detectionsGroup
//	 */
//	public void drawDetection(PamDataUnit unit, BranchGroup detectionsGroup){
//		
//		float horzUnit;
//		float vertUnit;
//		DataUnitShape3D symbol;
//		TransformGroup detection;
//		
//		horzUnit= unit2Java3d(horzAxis.getMeasurment(unit), horzAxis);
//		vertUnit =unit2Java3d(vertAxis.getMeasurment(unit),	vertAxis);
//				
//		//get the shape from the dataBlock
//		symbol=unit.getParentDataBlock().get3DPlotProvider().getSymbol(unit);
//		
//		// move the shape to the correct location on the Java3d canvas
//		Transform3D trsf=new Transform3D();
//		trsf.setTranslation(new Vector3d(horzUnit,vertUnit,0));
//		detection=new TransformGroup();
//		detection.addChild(symbol);
//		detection.setTransform(trsf);
//		detection.setBounds(new BoundingSphere(new Point3d(horzUnit,vertUnit,0), 5f));
//		
//		//make sure that this transformGroup can be removed, added, altered etc
//		detection.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
//		detection.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
//		detection.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
//		detection.setCapability(TransformGroup.ALLOW_BOUNDS_READ);
//		detection.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
//		detection.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
//		
//		
//		//add to the specified branchGroup
//		detectionsGroup.addChild(detection);
//	}
//	
//	/**
//	 * Converts all detections to stay roughly the same size no matter what the zoom factor is
//	 */
//	public void setAspectRatioAllData(){
//		for (int i=0; i<detectionsGroup.numChildren();i++){
//			setDataShapeAspectRatio((TransformGroup) detectionsGroup.getChild(i));
//		}
//	}
//	
//	/**
//	 * Convert a detection to stay the same size no matter what the window size etc.
//	 * @param trsgroup
//	 */
//	public void setDataShapeAspectRatio(TransformGroup trsgroup){
//		Transform3D trsf3D=new Transform3D();
//		trsgroup.getTransform(trsf3D);
//		
//		Vector3f translation = new Vector3f();
//		Vector3d scale = new Vector3d();
//		Matrix3f rotM=new Matrix3f();
//		trsf3D.get(rotM);
//		trsf3D.get(translation);
//		trsf3D.getScale(scale);
//		Double winRatio=y/x;
//		if (winRatio.equals(Double.NaN) || winRatio.equals(null)) winRatio=0.0;
//		trsf3D.setScale(new Vector3d(winRatio*horizontalRange,2*winRatio*verticalRange,0.05));
//		//trsf3D.setScale(new Vector3d(1/horizontalRange,1/(x/y)*verticalRange,0.05));
//		//trsf3D.setScale(new Vector3d(1/scale.x,1/scale.y,0.05));
//		//trsf3D.set(rotM);
//		//trsf3D.setTranslation(translation);
//	
//		trsgroup.setTransform(trsf3D);
//	}
//	
//	/**
//	 * Take the position of a scroll bar, convert this position to a java3d translatioon and then translate the graph. Takes into account aspect ratio. 
//	 */
//	public void setScrollBars(){
//		vertScrollPos=getScrollBarTranslate(vertAxis.getScrollBarValue(),vertAxis.getScrollBarMax(), vertAxis.getScrollBarMin(), vertAxis.getAxisMax(), vertAxis.getAxisMin(),vertAxis.getRange());
//		horzScrollPos=getScrollBarTranslate(horzAxis.getScrollBarValue(),horzAxis.getScrollBarMax(), horzAxis.getScrollBarMin(), horzAxis.getAxisMax(), horzAxis.getAxisMin(),horzAxis.getRange());
//		Double winRatio=y/x;
//		if (winRatio.equals(Double.NaN) || winRatio.equals(null)) winRatio=0.0;
//		//System.out.println("Vector: "+new Vector3d(-(horzScrollPos+1),-winRatio*(vertScrollPos+1),0));
//		graph3D.transformTranslationG(new Vector3d(-(horzScrollPos+1),-winRatio*(vertScrollPos+1),0));
//	}
//	
//	public DataUnitShape3D getSelectedDetection(){
//		return currentSelectedDetection;
//	}
//	
//
//	public void setSelectedDetection(PamDataUnit selDet){
//		
//		TransformGroup shape;
//		DataUnitShape3D shape3d;
//		DataUnitShape3D shape3dTemp=null;
//		
//		for (int i=0; i<detectionsGroup.numChildren(); i++){
//			
//			shape=(TransformGroup) detectionsGroup.getChild(i);
//			shape3d= (DataUnitShape3D) shape.getChild(0);
//			
//				if (shape3d.equals(currentSelectedDetection)){
//					if (setSymbolAspectRatio==true) setDataShapeAspectRatio(shape);
//
//					shape3d.setAppearance(shape3d.getdataUnit().getParentDataBlock().get3DPlotProvider().getSymbol(shape3d.getdataUnit()).getAppearance());
//					shape3d.setGeometry(shape3d.getdataUnit().getParentDataBlock().get3DPlotProvider().getSymbol(shape3d.getdataUnit()).getGeometry());
//
//				}
//				
//				if (shape3d.getdataUnit().equals(selDet)){
//					if (setSymbolAspectRatio==true) setDataShapeAspectRatio(shape);
//					shape3d.setAppearance(shape3d.getdataUnit().getParentDataBlock().get3DPlotProvider().getSymbolSelected(shape3d.getdataUnit()).getAppearance());
//					shape3d.setGeometry(shape3d.getdataUnit().getParentDataBlock().get3DPlotProvider().getSymbolSelected(shape3d.getdataUnit()).getGeometry());
//					shape3dTemp=shape3d;
//				}
//			}
//		 currentSelectedDetection=shape3dTemp;
//	}
//
//	public Window getFrame() {
//		return frame;
//	}
//	
//	/**
//	 * Enable constant size of data irrespective of the horizontal or vertical range window. 
//	 * @param setSymbolAspectRatio
//	 */
//	public void enableDataAspectRatioLock(boolean setSymbolAspectRatio){
//		this. setSymbolAspectRatio=setSymbolAspectRatio;
//	}
//	
//	public boolean getDataAspectRatioLock(){
//		return setSymbolAspectRatio;
//	}
//
//	
//
//
//
//}
//	
//
//
//
//	
//
//	

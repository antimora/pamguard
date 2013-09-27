package staticLocaliser.panels;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Group;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TransformGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;

import com.sun.j3d.utils.picking.PickCanvas;
import com.sun.j3d.utils.picking.PickResult;

import staticLocaliser.StaticLocalise;
import staticLocaliser.StaticLocaliserControl;
import Array.ArrayManager;
import Array.PamArray;
import PamDetection.PamDetection;
import PamGraph3D.MouseRightClickMenu;
import PamGraph3D.PamPanel3D;
import PamGraph3D.PamShapes3D;
import PamGraph3D.spectrogram3D.Spectrogram3DPamGraph;
import PamUtils.PamUtils;
import PamView.PamPanel;
import PamView.PopupTextField;

/**
 * The basic 3D map for the static localiser. This map is NOT intended to be used to show detailed geographic info. It is designed to allow the user to visualise the results of localisation algorithms, whether the probabaility distributions of MCMC, simplex error ellipses or just 3D bearings. 
 * Shapes on the map can be selected using Java3D pick functionality- selection of shapes links in directly with other displays in the static localiser. This linking is achieved using the update() function included in all  StaticDialogComponent panels. 
 * @author Jamie Macaulay
 *
 */
public class DialogMap3DSL extends DialogMap implements StaticDialogComponent  {
	
	StaticLocalisationMainPanel staticLocalisationDialog;
	
	StaticLocaliserControl staticLocaliserControl;
	
	PamDetection currentDetection;


	//The current transformGroup highlighted on the map;
	ArrayList<TransformGroup> currentHighlights=new ArrayList<TransformGroup>();
	
	//Panel components;
	private PamPanel mainPanel;
	private PamPanel3D graph3D;
	private BranchGroup seaSurface;
	private BranchGroup hydrophoneArray;
	private BranchGroup pickGroup;
	private BranchGroup axisVis;

	
	//menu components
	JCheckBoxMenuItem axisIcon;

	//Common colours
		Color3f red = new Color3f(1.0f, 0.0f, 0.0f);

		Color3f blue = new Color3f(0.0f, 0.0f, 1.0f);

		Color3f green = new Color3f(0.0f, 1.0f, 0.0f);

		Color3f black = new Color3f(0.0f, 0.0f, 0.0f);

		Color3f white = new Color3f(1.0f, 1.0f, 1.0f);

		Color3f grey = new Color3f(0.5f, 0.5f, 0.5f);
		
		double resetZoom=-5000;
		float range=1000;



	public DialogMap3DSL(StaticLocaliserControl staticLocaliserControl,
			StaticLocalisationMainPanel staticLocalisationDialog) {
		super(staticLocaliserControl, staticLocalisationDialog);
		
		this.staticLocalisationDialog=staticLocalisationDialog;
		this.staticLocaliserControl=staticLocaliserControl;
		
		mainPanel=new PamPanel(new BorderLayout());
		mainPanel.setBorder(new TitledBorder("Map"));
		Frame frame =staticLocalisationDialog.getStaticLocaliserControl().getPamView().getGuiFrame();
		graph3D=new PamPanel3D(frame, true);
		//speeds picking shapes up, especially if very complex like MCMC chains
		graph3D.getPickCanvas().setMode(PickCanvas.GEOMETRY_INTERSECT_INFO);

		graph3D.addMouseRotate();
		graph3D.addMouseTranslate();
		graph3D.addMouseZoom();
		
		pickGroup=new BranchGroup();
		pickGroup.setCapability(BranchGroup.ALLOW_DETACH);
		pickGroup.setCapability(Group.ALLOW_CHILDREN_READ);
		pickGroup.setCapability(Group.ALLOW_CHILDREN_WRITE);
		pickGroup.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		pickGroup.setCapability(BranchGroup.ENABLE_PICK_REPORTING);
		
		graph3D.addChildtoGraphicsGroup(pickGroup);
		
		
		//add extra mouse functionality
		graph3D.getCanvas3D().addMouseListener(new MousePick(graph3D.getPickCanvas()));
//		MShowPopUpMenu mouseRightClick=new MShowPopUpMenu();
//		mouseRightClick.setMainPanel(mainPanel);
//		graph3D.addMouseRightClickMenu(mouseRightClick);

		//add in 3D components
		seaSurface=PamShapes3D.createSeaSurface(range, blue);
		hydrophoneArray=createHydrophoneArray(null);
		
		createArrayandSea();
		graph3D.resetPlot(resetZoom);
		
		mainPanel.add(BorderLayout.CENTER,graph3D);
	}
	
	

	/**
	 * Get the hydrophone array position at a certain time. 
	 * @return a list of Point3d's denoting hydrophone positions at time timeMillis.
	 */
	private ArrayList<Point3d> getHydrophones3d(long timeMillis){
		PamArray currentArray=ArrayManager.getArrayManager().getCurrentArray();

		double[] array;
		int N=currentArray.getHydrophoneCount();
		//int[] hydrophoneMap=PamUtils.getChannelArray(pamDataUnit.getParentDataBlock().getChannelMap());
		ArrayList<Point3d> hydrophones=new  ArrayList<Point3d>();
		for (int i=0; i<N; i++){
//			array=currentArray.getHydrophoneCoordinates(i, timeMillis);
//			hydrophones.add(new Point3d(array));
		}
		
		return  hydrophones;
	}
	
	/**
	 * Create a branchGroup containing a 3D image of the hydrophone at the time of the pamDetection
	 * @param pamDetection
	 * @return
	 */
	private BranchGroup createHydrophoneArray(PamDetection pamDetection){
		ArrayList<Point3d> hydrophonePos;
		if (pamDetection==null ) hydrophonePos=getHydrophones3d(0);
		else hydrophonePos=getHydrophones3d(pamDetection.getTimeMilliseconds());
		BranchGroup hydrophoneArray=PamShapes3D.createHydrophoneArray(hydrophonePos);
		return hydrophoneArray;
	}
	
	
	/**Show pop up menu
	 * 
	 * @author EcologicUK
	 *
	 */
	public class MShowPopUpMenu extends MouseRightClickMenu {
		@Override
		public void showPopupMenu(JPanel mainPanel, Point point) {
			JPopupMenu menu=createPopupMenu();
			menu.show(getMainPanel(), point.x, point.y);

		}
	
	  } 
	
	class Reset implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			graph3D.resetPlot(resetZoom);
		}
	}
	
	class Rotate implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			graph3D. rotatePlot(-Math.PI/2);
		}
	}
	
	class SetSeaLength implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			setLineLength();
		}
	}
	
	class AddAxisVis implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			staticLocaliserControl.getParams().mapAxisVis=axisIcon.isSelected();
			if (axisIcon.isSelected()){
				axisVis=PamShapes3D.createAxis(100, 100, 100, 10, "x (m)", "y(m)", "Depth(m)", PamShapes3D.grey);
				graph3D.addChildtoGraphicsGroup(axisVis);
			}
			else{
				if (axisVis!=null);
				graph3D.getGraphicsGroup().removeChild(axisVis);
			}
		}
	}
	
	/**
	 * Creates a text box at the mouse position on the canvas3D. Input changes the range variable altering the size of localisation vectors. 
	 */
	private void setLineLength() {

		Point pt=mainPanel.getMousePosition();
		SwingUtilities.convertPointToScreen(pt, mainPanel);
		Double newVal = PopupTextField.getValue(mainPanel, "Length(m)", pt, (double) range);
		if (newVal != null) {
			range=newVal.floatValue();
			createArrayandSea();
		}

	}
	
	public void createArrayandSea(){
		try{
			graph3D.getRotateGroup().removeChild(seaSurface);
			graph3D.getRotateGroup().removeChild(hydrophoneArray);
		}
		catch(Exception e){}
		graph3D.addChildtoRotateGroup(seaSurface=PamShapes3D.createSeaSurface(range, blue));
		if (staticLocalisationDialog.getCurrentControlPanel()!=null) hydrophoneArray=createHydrophoneArray(staticLocalisationDialog.getCurrentControlPanel().getCurrentDetection());
		graph3D.addChildtoRotateGroup(hydrophoneArray);
	}
	
	private class MousePick extends MouseAdapter{
		
		PickCanvas pickCanvas;
		
		public MousePick(PickCanvas pickCanvas){
			this.pickCanvas=pickCanvas;
		}
		
		public void mouseClicked(MouseEvent e){

			pickCanvas.setShapeLocation(e.getX(),e.getY());
			PickResult result = pickCanvas.pickClosest();
			
			//return all shapes to blue
			if (result==null){
				if (e.getButton()==MouseEvent.BUTTON3 || e.getButton()==MouseEvent.BUTTON2){
					 createPopupMenu() .show(getPanel(),e.getX(),e.getY());
				}
				highLightLocShapes(null);
				return;
			}
			
			TransformGroup s = (TransformGroup)result.getNode(PickResult.TRANSFORM_GROUP);
			System.out.println("Map shape"+ s);
			highLightLocShapes(s);
			
			if (e.getButton()==MouseEvent.BUTTON3 || e.getButton()==MouseEvent.BUTTON2){
				 createPopupMenu() .show(getPanel(),e.getX(),e.getY());
			}
		}
	}
	
	public void highLightLocShapes(TransformGroup s){
		Integer tdSel=null;
		TransformGroup tg;
		LocShape3D locShape;

		for (int i=0; i<pickGroup.numChildren();i++){
			tg=(TransformGroup) ((BranchGroup) pickGroup.getChild(i)).getChild(0);
			if (tg==s){
				for (int j=0; j<tg.numChildren();j++){
					locShape=(LocShape3D) tg.getChild(j);
					tdSel=locShape.getStaticLocResult().getTimeDelay();
				}
			}
		}
		
		setTDSel( tdSel);
				
		staticLocaliserControl.getStaticLocaliser().settdSel(tdSel);
		updateOtherPanels(StaticLocaliserControl.TD_SEL_CHANGED);
	}
	
	public void updateOtherPanels(int flag){
		staticLocaliserControl.getStaticMainPanel().getLocalisationVisualisation().update(flag);
		staticLocalisationDialog.getLocalisationInformation().update(flag);
	}
	
	public void setTDSel(Integer tdSel){
		TransformGroup tg;
		LocShape3D locShape;
		ArrayList<TransformGroup> currentHighlights=new ArrayList<TransformGroup>();
		//remove all highlighted symbols. 
		for (int i=0; i<pickGroup.numChildren();i++){
			tg=(TransformGroup) ((BranchGroup) pickGroup.getChild(i)).getChild(0);
			for (int n=0; n<this.currentHighlights.size(); n++){
				if (tg==this.currentHighlights.get(n)){
					for (int j=0; j<tg.numChildren();j++){
						locShape=(LocShape3D) tg.getChild(j);
						locShape.getStaticLocResult().getAlgorithm().setNormalAppearance(locShape);
					}
				}
			}
		}
		//highlight all symbols which correspond to tdSel;
		for (int i=0; i<pickGroup.numChildren();i++){
			tg=(TransformGroup) ((BranchGroup) pickGroup.getChild(i)).getChild(0);
				for (int j=0; j<tg.numChildren();j++){
						 locShape=(LocShape3D) tg.getChild(j);
						 if (locShape.getStaticLocResult().getTimeDelay()==tdSel){
							locShape.setAppearance(locShape.getStaticLocResult().getAlgorithm().getHighlightAppearance());
							 currentHighlights.add(tg);
				}
			}
		}
		this.currentHighlights=currentHighlights;
			
	}


	
	public JPopupMenu createPopupMenu() {
		
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		
		JPopupMenu menu = new JPopupMenu();
		
		JMenuItem menuItem = new JMenuItem("Reset plot");
		menuItem.addActionListener(new Reset());
		menu.add(menuItem);
		menuItem = new JMenuItem("Rotate plot");
		menuItem.addActionListener(new Rotate());
		menu.add(menuItem);
		menuItem = new JMenuItem("Set Sea Length");
		menuItem.addActionListener(new SetSeaLength());
		menu.add(menuItem);
		axisIcon = new JCheckBoxMenuItem("Axis Icon");
		axisIcon.setSelected(staticLocaliserControl.getParams().mapAxisVis);
		axisIcon.addActionListener(new AddAxisVis());
		menu.add(axisIcon);
	
		return menu;
	}
	
	
	public void setLocalisationSymbol(TransformGroup tg){
		BranchGroup bg=new BranchGroup();
		bg.setCapability(BranchGroup.ALLOW_DETACH);
		bg.addChild(tg);
		pickGroup.addChild(bg);
		//graph3D.addChildtoGraphicsGroup(bg);
	}
	
	public void clearLocSymbols(){
		pickGroup.removeAllChildren();
		//graph3D.clearGraphicsGroup();
	}
	

	@Override
	public void setCurrentDetection(PamDetection pamDetection) {
		currentDetection=pamDetection;
	}



	@Override
	public void update(int flag) {
		
		switch (flag){
		
		case (StaticLocaliserControl.SEL_DETECTION_CHANGED):
			currentDetection=this.staticLocalisationDialog.getCurrentControlPanel().getCurrentDetection();
			createArrayandSea();
			pickGroup.removeAllChildren();
		break;
		case (StaticLocaliserControl.TD_SEL_CHANGED):
			setTDSel(staticLocaliserControl.getStaticLocaliser().getTDSel());
		break;
		case (StaticLocaliserControl.RUN_ALGORITHMS):
			pickGroup.removeAllChildren();
		break;
		case (StaticLocaliserControl.SEL_DETECTION_CHANGED_BATCH):
			pickGroup.removeAllChildren();	
		break;
		}
	}

	@Override
	public JPanel getPanel() {
		return mainPanel;
	}

	@Override
	public void notifyNewResults() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void settings() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void showMap(boolean b) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public StaticLocalisationMainPanel getStaticMainPanel() {
		return staticLocalisationDialog;
	}
	
	public BranchGroup getPickGroup(){
		return pickGroup;
	}


}

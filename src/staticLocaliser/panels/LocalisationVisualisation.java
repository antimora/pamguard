package staticLocaliser.panels;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Group;
import javax.media.j3d.Material;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;


import com.sun.j3d.utils.geometry.Text2D;
import com.sun.j3d.utils.picking.*;

import staticLocaliser.ExportTimeDelays;
import staticLocaliser.StaticLocalise;
import staticLocaliser.StaticLocaliserControl;
import staticLocaliser.panels.DialogMap3DSL.AddAxisVis;

import Layout.PamAxis;
import Layout.PamAxisPanel;
import PamDetection.AbstractDetectionMatch;
import PamDetection.PamDetection;
import PamGraph3D.PamPanel3D;
import PamGraph3D.PamShapes3D;
import PamUtils.PamUtils;
import PamView.PamPanel;

/**
 * Localisation visualisation constructs a visualisation of all the possible time delays calculated for a detection. Grey horizontal lines represent time windows on different hydrophones within which any vocalisation is a possible corresponding vocalisation to the selected detection. Depending 
 * on the number of these possible corresponding vocalisations on other hydrophones, there is then a set of possible time delays. Each vocalisation is represented by a blue sphere. The selected detection is represented by a red sphere. Time delay possibilties are represented by transparent blue lines which pass through one vocalisation per hydrophone time window line. 
 * These lines can selected, either to run an individual localisation, or highlight an existing localisation, possibility on the map.  
 * @author Jamie Macaulay
 *
 */
public class LocalisationVisualisation implements StaticDialogComponent {
	
	StaticLocalisationMainPanel staticLocalisationMainPanel;
	StaticLocalise staticLocaliser;
	
	//data
	PamDetection currentDetection;
	ArrayList<ArrayList<Double>> timeDelays;
	//Integer tdSel=null;
	int NTimeDelays=0;
	Frame frame;
		
	//Swing Components
	private PamPanel timeDelayVisualisation;
	private PamPanel3D graphTDVis;
	PamAxis timeAxis;
	JCheckBoxMenuItem channelsVis;
	JLabel nPossibilities=new JLabel("No. possibilities: ");
	
	//3D Components
	private BranchGroup timeWindowLines;
	private BranchGroup timeDelayLines;
	private BranchGroup pickGroup;
	private BranchGroup detectionLocations;
	boolean showChannels=false;
	
	private Appearance lineApb=PamShapes3D.lineAppearance(3.5f, false, PamShapes3D.blue);
	private Appearance lineApr=PamShapes3D.lineAppearance(4.5f, false, PamShapes3D.red);
	private Appearance lineApg=PamShapes3D.lineAppearance(3f, false, PamShapes3D.grey);
	
	public LocalisationVisualisation(StaticLocalisationMainPanel staticLocalisationDialog){
		
	this. staticLocalisationMainPanel= staticLocalisationDialog;
	this.staticLocaliser=staticLocalisationDialog.getStaticLocaliserControl().getStaticLocaliser();
		
	timeDelayVisualisation=new PamPanel(new BorderLayout());
	timeDelayVisualisation.setBorder(new TitledBorder("Time Delay Possibilities"));
	timeDelayVisualisation.add(BorderLayout.CENTER,timePosVisualisation());
	timeDelayVisualisation.add(BorderLayout.NORTH, nPossibilities);

	lineApb.setTransparencyAttributes(transParancyApp(0.8f));
	
	}
	
	public TransparencyAttributes transParancyApp(float level){
		TransparencyAttributes trans=new TransparencyAttributes();
		trans.setTransparencyMode(TransparencyAttributes.NICEST);		
		trans.setTransparency (level);
		return trans;
	}
	
	/**Creates the Java3D component to display the time delay visualisation graph. 
	 * 
	 * @return JPanel containing a pickable java3D canvas. 
	 */
	public PamPanel timePosVisualisation(){
		
		frame =staticLocalisationMainPanel.getStaticLocaliserControl().getPamView().getGuiFrame();
		
		graphTDVis=new PamPanel3D(frame,true);

		pickGroup=new BranchGroup();
		pickGroup.setCapability(BranchGroup.ALLOW_DETACH);
		pickGroup.setCapability(Group.ALLOW_CHILDREN_READ);
		pickGroup.setCapability(Group.ALLOW_CHILDREN_WRITE);
		pickGroup.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		pickGroup.setCapability(BranchGroup.ENABLE_PICK_REPORTING);
		graphTDVis.addChildtoGraphicsGroup(pickGroup);
		

		 //add a mouse listener to pick shapes
		graphTDVis.getCanvas3D().addMouseListener(new MousePick(graphTDVis.getPickCanvas()));
		//make sure the shapes change with the aspect ratio of the window
		graphTDVis.setAspectRatioEnabled(true);
		
		 //add to JPanel
		PamPanel timePosVis=new PamPanel(new BorderLayout());
		 timePosVis.add(BorderLayout.CENTER,graphTDVis);
		
		return timePosVis;
	}
	
	
	/**
	 * Creates a visualisation of all the possible time delays. Horizontal lines represent the time window with respect to the primary hydrophone, for all other hydrophones within the array.
	 * 
	 * @param pamDetection
	 * @return
	 */
	public void  createPosVisualisation(PamDetection pamDetection){
		
		double scaleFactor=0.8;
		double shiftFactor=-0.6;
		int primaryChannel;
		AbstractDetectionMatch detectionMatch;
		double[] timeWindows;
		ArrayList<Integer> indexM1;
		staticLocaliser.settdSel(null);
		
		//must map channels- important for unsynchronised systems where the first hydrophone isn't always hydrophone 0
		int[] hydrophoneMap=staticLocalisationMainPanel.getCurrentControlPanel().getChannelMap();
		//int[] hydrophoneMap=PamUtils.getChannelArray(pamDetection.getChannelBitmap());
				
		detectionMatch= pamDetection.getDetectionMatch(staticLocalisationMainPanel.getCurrentControlPanel().getDetectionType());
	
		//get time delays for this detection and if necessary filter out unwanted channels.
		 timeDelays=null;//;detectionMatch.getGroupTimeDelays(staticLocalisationMainPanel.getStaticLocaliserControl().getParams().channels);
		 
		 if (timeDelays==null) return;
		 
		System.out.println("Localisation.TimeDelays: "+timeDelays);
		System.out.println("Channel array length: "+hydrophoneMap.length);

			for (int i=0; i<hydrophoneMap.length; i++){
				System.out.print(" "+hydrophoneMap[i]);
			}

//		 if (timeDelays.get(0).get(0).isNaN()) {
//			 System.out.println("Time Dleay  Visualisation: NaN ");
//			 System.out.println("Channel array length: "+hydrophoneMap.length);
//			 return;
//		 }
		 NTimeDelays=timeDelays.size();
		 
		 primaryChannel=detectionMatch.getPrimaryChannel();
		 timeWindows=null;//detectionMatch.getTimeWindows(pamDetection.getTimeMilliseconds());
		 indexM1=AbstractDetectionMatch.indexM1(timeWindows.length);
		
		 
		//time window lengths
		timeWindowLines=new BranchGroup();
		timeWindowLines.setCapability(BranchGroup.ALLOW_DETACH);
		
		//click locations
		detectionLocations=new BranchGroup();
		detectionLocations.setCapability(BranchGroup.ALLOW_DETACH);
		detectionLocations.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
		detectionLocations.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		detectionLocations.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		
		//lines representing each set of time delays. This contains pickable shapes
		timeDelayLines=new BranchGroup();
		timeDelayLines.setCapability(BranchGroup.ALLOW_DETACH);
		timeDelayLines.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
		timeDelayLines.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		timeDelayLines.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);

		//find the max time window		
		double maxTW=0;
		
		for (int i=0; i<timeWindows.length; i++){
			if (timeWindows[i]>maxTW){
				maxTW=timeWindows[i];
			}
		}
		
		//create time window lines
		float x0,x1,y;
		Point3f p0;
		Point3f p1;
		ArrayList<Point3f> timeWin;
		//channel text;
		Text2D text2d;
		TransformGroup textTrs;
		Transform3D trs=new Transform3D();
		for (int j=0; j<timeWindows.length; j++){
	
			//create time window lines
			timeWin=new ArrayList<Point3f>();
			
			x0=(float) (-timeWindows[j]/maxTW);
			x1=(float) (timeWindows[j]/maxTW);
			y=(float) ((((2*scaleFactor)/timeWindows.length)*(timeWindows.length-j-1))+shiftFactor);
			
			p0=new Point3f(x0,y,0);
			p1=new Point3f(x1,y,0);
			timeWin.add(p0);
			timeWin.add(p1);
			
			Shape3D line=PamShapes3D.linePolygon3D(timeWin, lineApg );
			
			//create Channel number text
			if (showChannels==true){
				text2d = new Text2D(("ch "+Integer.toString(hydrophoneMap[j])), PamShapes3D.black, "Plain", 16, Font.BOLD);
				textTrs=new TransformGroup();
				trs.setTranslation(new Vector3d(-1, y+0.03, 0));
				textTrs.addChild(text2d);
				textTrs.setTransform(trs);
				//add text to branchgroup
				timeWindowLines.addChild(textTrs);
			}

			//add lines to branchgroup
			timeWindowLines.addChild(line);
		}
		

		//create appearance for the spheres which represent vocalisations. 
		Appearance sphereAp=PamShapes3D.sphereAppearance(PamShapes3D.blue);
		Appearance sphereApPrimary=PamShapes3D.sphereAppearance(PamShapes3D.red);
		ArrayList<Point3f> timeDelay;
		TransformGroup sphere;
		Point3f timePos;
		float time;

		int[] tdIndex=getCorrectTimeDelays(timeWindows.length,primaryChannel);
		
		//now add the sphere to correct positions on each of the time windows. 
		for (int n=0; n<timeDelays.size(); n++){
			
			timeDelay=new ArrayList<Point3f>();
			int N=0;
			
			for (int k=0; k<timeWindows.length; k++){
					
					if (hydrophoneMap[k]!=primaryChannel){
						if (timeDelays.get(n).get(tdIndex[N])!=null) {
							time=(float) -(timeDelays.get(n).get(tdIndex[N])/maxTW);
							if (indexM1.get(tdIndex[N])==primaryChannel) time=-time;
							y=(float) ((((2*scaleFactor)/timeWindows.length)*(timeWindows.length-k-1))+shiftFactor);
							timePos=new Point3f(time,y,0);
							timeDelay.add(timePos);
							
							sphere=PamShapes3D.createSphere(timePos, sphereAp,0.025f);
							detectionLocations.addChild(sphere);
						}
						N++;
					}
					else{
						time=0f;
						y=(float) ((((2*scaleFactor)/timeWindows.length)*(timeWindows.length-k-1))+shiftFactor);
						timePos=new Point3f(time,y,0);
						timeDelay.add(timePos);
					
						sphere=PamShapes3D.createSphere(timePos, sphereApPrimary,0.025f);
						detectionLocations.addChild(sphere);
					}
			}
			
			//add another point to ensure a line can be drawn if no other time delays exist;
			if (timeDelay.size()==1){
				y=(float) ((((2*scaleFactor)/timeWindows.length)*(timeWindows.length-primaryChannel-1))+shiftFactor);
				timeDelay.add(new Point3f(0f,y,0));
			}
			
			Shape3D line=PamShapes3D.linePolygon3D(timeDelay, lineApb );
			line.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
			line.setCapability(Shape3D.ENABLE_PICK_REPORTING);
			timeDelayLines.addChild(line);
		}
		
		//add all shapes and branchgroups to the graph
		graphTDVis.getGraphicsGroup().removeAllChildren();
		pickGroup.removeAllChildren();
		pickGroup.addChild(timeDelayLines);
		graphTDVis.addChildtoGraphicsGroup(pickGroup);
		graphTDVis.addChildtoGraphicsGroup(timeWindowLines);
		graphTDVis.addChildtoGraphicsGroup(detectionLocations);
		
	}
	

	
	
	/**
	 * We only want the time delays between the primary hydrophone (i.e. the hydrophone the selected detection corresponds to) and the rest of the array. This function creates an index matrix which selects only the relevant time delays;
	 * @param nhydrophones
	 * @param primaryhydrophone
	 * @return
	 */
	public int[] getCorrectTimeDelays(int nhydrophones, int primaryhydrophone){
		
		ArrayList<Integer> indexM1=AbstractDetectionMatch.indexM1(nhydrophones);
		ArrayList<Integer> indexM2=AbstractDetectionMatch.indexM2(nhydrophones);
		
		int[] tDindex=new int[nhydrophones-1];
		int n=0;
		for (int i=0; i<indexM1.size();i++){
			if (indexM1.get(i)==primaryhydrophone || indexM2.get(i)==primaryhydrophone){
				tDindex[n]=i;
				n++;
			}
		}
		return tDindex;
	}
	
	
	/**
	 * Picks and highlights time delay line if clicked on. If white space is clicked brings up a pop up menu with export all functionality. If a line is clicked on tdSel is set to that time delay combination. A pop up menu can also be invoked to run the localisation algorithm on the selected time delay combination;
	 * @author Jamie Macaulay
	 *
	 */
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
					createPopMenuAll().show(timeDelayVisualisation,e.getX(),e.getY());
				}
				for (int i=0; i<timeDelayLines.numChildren();i++){
					((Shape3D) timeDelayLines.getChild(i)).setAppearance(lineApb);
				}
				System.out.println("null pick result");
				staticLocaliser.settdSel(null);
				updateOtherPanels(StaticLocaliserControl.TD_SEL_CHANGED);
				return;
			}
			
			Shape3D s = (Shape3D)result.getNode(PickResult.SHAPE3D);
						
			BranchGroup timeDelayLines=(BranchGroup) pickGroup.getChild(0);
			
			for (int i=0; i<timeDelayLines.numChildren();i++){
				((Shape3D) timeDelayLines.getChild(i)).setAppearance(lineApb);
				if (s==timeDelayLines.getChild(i)){
					staticLocaliser.settdSel(i);
					((Shape3D) timeDelayLines.getChild(i)).setAppearance(lineApr);
					//System.out.println("Selected Time Delay: "+i);
				}
			}
			
			if (e.getButton()==MouseEvent.BUTTON3 || e.getButton()==MouseEvent.BUTTON2){
				createPopMenu().show(graphTDVis.getCanvas3D(),e.getX(),e.getY());
			}
			
			updateOtherPanels(StaticLocaliserControl.TD_SEL_CHANGED);
		}
	}
	
	public void updateOtherPanels(int flag){
		staticLocalisationMainPanel.getDialogMap3D().update(flag);
		staticLocalisationMainPanel.getLocalisationInformation().update(flag);
	}
	
	
	/**
	 * Sets the appearance of time delay td to red and resets the rest of the lines to blue. If td is null then all lines return to blue. 
	 * @param td- the array index of the time delay to set to red
	 */
	public void changeTDLineAppearance(Integer td){
		//if (td==null) return;
		if (timeDelayLines==null) return;
		for (Integer i=0; i<timeDelayLines.numChildren();i++){
			((Shape3D) timeDelayLines.getChild(i)).setAppearance(lineApb);
			if (td==i){
				((Shape3D) timeDelayLines.getChild(i)).setAppearance(lineApr);
			}
		}
	}
	
	/**
	 * @author Jamie Macaulay
	 *
	 */
	class KeyBoardPick extends KeyAdapter{
		
		PickCanvas pickCanvas;
		
		public KeyBoardPick (PickCanvas pickCanvas){
			this.pickCanvas=pickCanvas;
		}

	}
	
	
	public JPopupMenu createPopMenu(){
		JPopupMenu popMenu=new JPopupMenu();
		popMenu.setLightWeightPopupEnabled(false);
		JMenuItem menuItem=new JMenuItem("Run");
		menuItem.addActionListener(new RunSelTD());
		popMenu.add(menuItem);
		menuItem=new JMenuItem("Export");
		menuItem.addActionListener(new Export());
		popMenu.add(menuItem);
		channelsVis = new JCheckBoxMenuItem("Show Channels");
		channelsVis.setSelected(showChannels);
		channelsVis.addActionListener(new ShowChannels());
		popMenu.add(channelsVis);
		return popMenu;
	}
	
	public class Export implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent arg0) {
			Point pt = timeDelayVisualisation.getLocationOnScreen();
			pt.x += 10;
			pt.y += 20;
			//only want to export one time delay so make a 2D array with just one array;
			ArrayList<ArrayList<Double>> timeDelaysTemp=new ArrayList<ArrayList<Double>>();
			timeDelaysTemp.add(timeDelays.get(getTDSel()));
			ExportDelaysDialog.showDialog(frame, pt, staticLocaliser.getStaticLocaliserControl().getParams(), timeDelaysTemp);
			
		}
		
	}


	public class RunSelTD implements ActionListener{
			@Override
			 public void actionPerformed(ActionEvent e) {
				staticLocalisationMainPanel.getStaticLocaliserControl().run();
			 }
		}
	
	
	public JPopupMenu createPopMenuAll(){
		JPopupMenu popMenu=new JPopupMenu();
		popMenu.setLightWeightPopupEnabled(false);
		JMenuItem menuItem=new JMenuItem("Run all");
		menuItem.addActionListener(new RunAll());
		popMenu.add(menuItem);
		menuItem=new JMenuItem("Export all");
		menuItem.addActionListener(new ExportAll());
		popMenu.add(menuItem);
		channelsVis = new JCheckBoxMenuItem("Show Channels");
		channelsVis.setSelected(showChannels);
		channelsVis.addActionListener(new ShowChannels());
		popMenu.add(channelsVis);
		return popMenu;
	}
	
	
	public class ExportAll implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (timeDelays==null) return;
			
			Point pt = timeDelayVisualisation.getLocationOnScreen();
			pt.x += 10;
			pt.y += 20;
			ExportDelaysDialog.showDialog(frame, pt, staticLocaliser.getStaticLocaliserControl().getParams(), timeDelays);
			
		}
	}
	
	class ShowChannels implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			showChannels=channelsVis.isSelected();
			createPosVisualisation( currentDetection);
		}
		
	}
	
	public class RunAll implements ActionListener{
		@Override
		 public void actionPerformed(ActionEvent e) {
			staticLocalisationMainPanel.getStaticLocaliserControl().runAll();
		 }
	}
	
	
	@Override
	public JPanel getPanel() {
		return timeDelayVisualisation;
	}

	@Override
	public void setCurrentDetection(PamDetection pamDetection) {
		this.currentDetection=pamDetection;		
	}


	@Override
	public void update(int flag) {
		// if the current selected detection has been changed create a new graph
		switch (flag){
			case(StaticLocaliserControl.SEL_DETECTION_CHANGED):
				currentDetection=this.staticLocalisationMainPanel.getCurrentControlPanel().getCurrentDetection();
				createNewTDVisualisation();	
			break;
			case(StaticLocaliserControl.TD_SEL_CHANGED):
				changeTDLineAppearance(staticLocaliser.getTDSel());
			break;
			case(StaticLocaliserControl.RUN_ALGORITHMS):
			break;
			case (StaticLocaliserControl.SEL_DETECTION_CHANGED_BATCH):
				createNewTDVisualisation();	
			break;
		}
		
	}
	
	/**
	 * Calculates the number of possible time delays and displays a visualisation on the chart if and only if the number of possible time delays is less than or equal to the maximum time delays in the staticlocaliserparams.
	 * This ensures that large groups of detections resulting a large number of possibilities, for which all time delays must be calculated (often involving a cross correlation), don't slow everything down too much. It's much quicker to calculate the number of possibilites and then decide to display or not. 
	 * 
	 */
	private void createNewTDVisualisation(){
		if (currentDetection!=null){
			int nPos=0;//currentDetection.getDetectionMatch(staticLocalisationMainPanel.getCurrentControlPanel().getDetectionType()).getNPossibilities(staticLocalisationMainPanel.getStaticLocaliserControl().getParams().channels);
			if (nPos<=staticLocalisationMainPanel.getStaticLocaliserControl().getParams().maximumNumberofPossibilities){
				nPossibilities.setText("No. possibilities: "+ nPos);
				createPosVisualisation(currentDetection);
			}
			else{
				nPossibilities.setText("No. possibilities: "+ nPos+  "  (exceeds maximum)");
				clearGraph();
			}
		}
		else {
			nPossibilities.setText("No. possibilities: ");
			clearGraph();
		}
	}
		
	
	
	public void clearGraph(){
		pickGroup.removeAllChildren();
		staticLocaliser.settdSel(null);
		NTimeDelays=0;
		graphTDVis.getGraphicsGroup().removeAllChildren();
	}

	@Override
	public StaticLocalisationMainPanel getStaticMainPanel() {
		return staticLocalisationMainPanel;
	}


	public Integer getTDSel() {
		return staticLocaliser.getTDSel();
	}
	
	/**
	 * this sets the selected time delay. TDSel is stored in the staticlocalise class. 
	 */


	public int getNTimeDelays() {
		if (currentDetection==null) return NTimeDelays;
		return NTimeDelays;
	}


	
	
}

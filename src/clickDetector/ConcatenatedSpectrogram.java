package clickDetector;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;

import javax.media.j3d.Behavior;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Group;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.WakeupCriterion;
import javax.media.j3d.WakeupOnAWTEvent;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3d;
import javax.vecmath.Point3d;

import org.apache.commons.math.geometry.Vector3D;

import Layout.PamAxis;
import Layout.PamAxisPanel;
import Layout.PamInternalFrame;
import PamDetection.PamDetection;
import PamGraph3D.MouseGetVirtualLocation;
import PamGraph3D.MouseRightClickMenu;
import PamGraph3D.PamPanel3D;
import PamGraph3D.spectrogram3D.Surface3D;
import PamUtils.FrequencyFormat;
import PamUtils.PamUtils;
import PamUtils.FrequencyFormat;
import PamUtils.PamUtils;
import PamView.ClipboardCopier3D;
import PamView.ColourArray.ColourArrayType;
import PamView.PamBorderPanel;
import PamView.PamLabel;
import PamView.PamPanel;

import clickDetector.ClickSpectrum.SpectrumInfo;
import clickDetector.WignerPlot.SelectChannel;
import clickDetector.offlineFuncs.OfflineEventDataUnit;

import com.sun.j3d.utils.behaviors.mouse.MouseBehavior;
import com.sun.xml.internal.ws.api.server.Container;


public class ConcatenatedSpectrogram extends ClickDisplay {
	
	//Display components

	private ClickControl clickControl;
	
	protected JPanel mainPanel;
	
	private JPanel plotPanel;
	
	//3D components
	
	private SpectroInfo spectroInfo;
	
	private PamPanel3D cSpectrogram;
	
	private BranchGroup spectro2D=null;
	
	private BranchGroup spectro3D=null;
	
	double spectrogramJava3DSize=100;

	//swing
	
	ConcetantedSpectorgramAxis axisPanel;
	
	private PamAxis southAxis, westAxis;
	
	PamLabel cursorPos;
	

	//Mouse Functionality
	
	MouseRightClickMenu mouseRightClick;
	
	MouseGetVirtualLocation mouseGetVirtualLocation;
	
	//Map state indicators
	boolean threeD=false;
	
	boolean blank=true;
	
	//Click Data
	int chan;
	
	PamDetection currentEvent;
	
	private ClickDetection lastClick;
	
	private ArrayList<ArrayList<Float>> spectrogramData=null;
	
	PamDetection lastEvent;
		
	//Spectrogram Data
	
	ConcatenatedSpectParams concatenatedSpectParams;
	
	//Keep a track of the current params so we don't have to redraw the spectrogram if nothing changes after options dialog is brought up.
	ColourArrayType spectColour;
	
	boolean logVal;
	
	double maxLogVal;
	
	boolean normaliseAll;
	
	//ability to copy 3D images
	ClipboardCopier3D clipboardCopier3D;
	

	public ConcatenatedSpectrogram(ClickControl clickControl, ClickDisplayManager clickDisplayManager, 
			ClickDisplayManager.ClickDisplayInfo clickDisplayInfo) {
		
		super(clickControl, clickDisplayManager, clickDisplayInfo);
		this.clickControl = clickControl;
		
		chan=0;
		
		concatenatedSpectParams=new ConcatenatedSpectParams();
		spectColour=concatenatedSpectParams.colourMap;
		logVal=concatenatedSpectParams.logVal;
		maxLogVal=concatenatedSpectParams.maxLogValS;
		normaliseAll=concatenatedSpectParams.normaliseAll;
		
		mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		mainPanel.addComponentListener(new windowResize());
		
		Frame frame =clickControl.getGuiFrame();
		cSpectrogram=new PamPanel3D(frame);
		cSpectrogram.addMouseTranslate();
		cSpectrogram.addMouseZoom();

		
		//add extra mouse functionality
		mouseRightClick=new MShowPopUpMenu();
		mouseRightClick.setMainPanel(mainPanel);
		
		MouseDragged mouseDragged=new MouseDragged();
		mouseDragged.setSchedulingBounds(cSpectrogram.bounds);
		BranchGroup mouseDrag=new BranchGroup();
		mouseDrag.addChild(mouseDragged);
		
		MouseWheelMoved mouseWheelMoved=new MouseWheelMoved();
		mouseWheelMoved.setSchedulingBounds(cSpectrogram.bounds);
		BranchGroup mouseWheelMove=new BranchGroup();
		mouseWheelMove.addChild(mouseWheelMoved);
		
		mouseGetVirtualLocation=new MouseShowInfo();
	
		cSpectrogram.addMouseRightClickMenu(mouseRightClick);
		cSpectrogram.addMouseGetVirtualLocation(mouseGetVirtualLocation);
		cSpectrogram.addChildtoRotateGroup(mouseDrag);
		cSpectrogram.addChildtoRotateGroup(mouseWheelMove);
		
		plotPanel=cSpectrogram;
		spectroInfo = new SpectroInfo();
	
		mainPanel.add(BorderLayout.CENTER, plotPanel);
		mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		
		//create the clip board copier for 3D image
		clipboardCopier3D=new ClipboardCopier3D(cSpectrogram);
				
		axisPanel=new ConcetantedSpectorgramAxis();
		setNorthPanel(spectroInfo);
		setAxisPanel(axisPanel);
		setPlotPanel(mainPanel) ;

	}
	
	
	
	public ArrayList<ArrayList<Float>> generateTestData(int bins, int fftSize){
		
		ArrayList<ArrayList<Float>> specData=new ArrayList<ArrayList<Float>>();

		for (int i=0; i<bins; i++){
			ArrayList<Float> fftData=new ArrayList<Float>();
			for (int j=0; j<fftSize; j++){
				Double rand=Math.random();
				fftData.add(rand.floatValue());
			}
			specData.add(fftData);
		}
		
		return specData;
	}
	
	
	public void createSpectrograms(ArrayList<ArrayList<Float>> fftData, ColourArrayType colours){
//		System.out.println("Create spectrogram");
		 this.spectro2D=new BranchGroup();
		 this.spectro3D=new BranchGroup();	
		 spectro2D.setCapability(BranchGroup.ALLOW_DETACH);
		 spectro3D.setCapability(BranchGroup.ALLOW_DETACH);
		 this.spectro2D.addChild(new Surface3D(fftData,false,100,100,colours));
		 this.spectro3D.addChild(new Surface3D(fftData,true,100,100,colours));
	}
	
	/**
	 * Adds a spectrogram to the graph
	 */
	public void addSpectrogram2D( ){
		if (spectro2D!=null){
			cSpectrogram.addChildtoGraphicsGroup(spectro2D);
		}
	}
	
	public void addSpectrogram3D( ){
		if (spectro3D!=null){
			cSpectrogram.addChildtoGraphicsGroup(spectro3D);
		}
	}

	/**
	 * Normalises data with respect to the maximum value of ALL clicks. 
	 * @param fftData
	 * @return
	 */
	public ArrayList<ArrayList<Float>> normaliseFFTDataAll(ArrayList<ArrayList<Float>> fftData){
		
		//normalise the data
		float max=0;
		float value;
		
		ArrayList<ArrayList<Float>> fftDataNorm=new ArrayList<ArrayList<Float>> ();
		ArrayList<Float> fftDataNorm1D;
		
		//get max Value
		if (fftData!=null && fftData.size()>0){
			
			//get max value
			for (int i=0; i<fftData.size(); i++){
				for (int j=0; j<fftData.get(0).size(); j++){
					
					value=fftData.get(i).get(j);
					if (value>max){
						max=value;
					}
				}
			}
			
			//normalise
			for (int i=0; i<fftData.size(); i++){
				fftDataNorm1D=new ArrayList<Float>();
				for (int j=0; j<fftData.get(0).size(); j++){
					fftDataNorm1D.add(fftData.get(i).get(j)/max);
					}
				fftDataNorm.add(fftDataNorm1D);
				}
			}
		
		return fftDataNorm;
	}
	



	/**
	 * Normalise every fft individually. This mean waves of different amplitude appear equally 'as loud' in the spectrogram. 
	 * @param fftData
	 * @return
	 */
	public ArrayList<ArrayList<Float>> normaliseFFTDataIndiv(ArrayList<ArrayList<Float>> fftData){
		
		//normalise the data
		float max=0;
		float value;
		
		ArrayList<ArrayList<Float>> fftDataNorm=new ArrayList<ArrayList<Float>> ();
		ArrayList<Float> fftDataNorm1D;

			//normalise
			for (int i=0; i<fftData.size(); i++){
				max=0;
				for(int k=0; k<fftData.get(i).size(); k++){
					value=fftData.get(i).get(k);
					if (value>max){
						max=value;
					}
				}
				
				fftDataNorm1D=new ArrayList<Float>();
				
				for (int j=0; j<fftData.get(0).size(); j++){
					fftDataNorm1D.add(fftData.get(i).get(j)/max);
					}
				fftDataNorm.add(fftDataNorm1D);
				
				}
			
	
		return fftDataNorm;
	}
	

	/**
	 * A moving axis for the spectrogram. Changes with mouse zoom and translate.
	 * @author EcologicUK
	 *
	 */
	class ConcetantedSpectorgramAxis extends PamAxisPanel {

		private static final long serialVersionUID = 1L;

		public ConcetantedSpectorgramAxis() {
			super();
			southAxis = new PamAxis(0, 0, 1, 1, 0, 1, false, "Clicks", "%.1f");
			westAxis = new PamAxis(0, 0, 1, 1, 0, 1, true, "Frequency kHz", "%d");
			setSouthAxis(southAxis);
			setWestAxis(westAxis);
			this.SetBorderMins(10, 20, 10, 20);
		}
	}
	
	class SpectroInfo extends PamBorderPanel {

		String emptyText = "Move cursor over plot for frequency information";
		public SpectroInfo() {
			super();
			setLayout(new BorderLayout());
			add(BorderLayout.CENTER, cursorPos = new PamLabel(emptyText));
			setBorder(new EmptyBorder(new Insets(mainPanel.getInsets().left, 2, 2, 2)));
		}
	}
	
	/**
	 * Sets the plot axis every time the mouse is dragged. Note that this type of listener is required for Java3D windows. 
	 *  
	 * @author EcologicUK
	 *
	 */
	public class MouseDragged extends Behavior {
		
	    // create SimpleBehavior
	    MouseDragged() {
	    }
	    
	    public void initialize() {
	      // set initial wakeup condition
	      this.wakeupOn(new WakeupOnAWTEvent(MouseEvent.MOUSE_DRAGGED));
	  	//System.out.println("mouse dragged");
	    }
	    
	    // called by Java 3D when appropriate stimulus occures
	    public void processStimulus(Enumeration criteria) {
	    	setPlotAxis();
	      this.wakeupOn(new WakeupOnAWTEvent(MouseEvent.MOUSE_DRAGGED));
	    }
	  } 
	
	/**
	 * Sets the plot axis every time the mouse wheel is moved. Note that this type of listener is required for Java3D windows. 
	 *  
	 * @author EcologicUK
	 *
	 */
	public class MouseWheelMoved extends Behavior {
		
	    // create SimpleBehavior
		MouseWheelMoved() {
	    }
	    
	    public void initialize() {
	      // set initial wakeup condition
	      this.wakeupOn(new WakeupOnAWTEvent(MouseEvent.MOUSE_WHEEL));
	    }
	    
	    // called by Java 3D when appropriate stimulus occures
	    public void processStimulus(Enumeration criteria) {	    	
	    	setPlotAxis();
	      this.wakeupOn(new WakeupOnAWTEvent(MouseEvent.MOUSE_WHEEL));
	     
	    }
	  } 
	
	/**Show pop up menu
	 * 
	 * @author Jamie Macaulay
	 *
	 */
	public class MShowPopUpMenu extends MouseRightClickMenu {
		@Override
		public void showPopupMenu(JPanel mainPanel, Point point) {
			JPopupMenu menu=createPopupMenu();
			menu.show(cSpectrogram, point.x, point.y);
			menu.setLightWeightPopupEnabled(false);

		}
	
	  } 
	
	/**
	 * Show freq and click Info
	 * @author EcologicUK
	 *
	 */
	public class MouseShowInfo extends MouseGetVirtualLocation {
		Double click;
		Double freq;
		@Override
		public void mouseFunction(){
			if (spectrogramData!=null){
				float sampleRate = clickControl.getClickDetector().getSampleRate();
			click=(double) Math.round((spectrogramData.size()/spectrogramJava3DSize)*((this.x)+(spectrogramJava3DSize/2)))+1;
			freq=(sampleRate/2/spectrogramJava3DSize)*((this.y)+(spectrogramJava3DSize/2));
			String txt = String.format("Frequency %s", FrequencyFormat.formatFrequency(freq, true)+"   Click "+ click.toString());
			cursorPos.setText(txt);
			}
		}
	 } 
	
	
	/**
	 * Make axis for the x and y coordinates based on the ranges and centre of the plot
	 */
	private void setPlotAxis() {
		/**
		 * Need to ensure square axis. 
		 */
		double clicksMin;
		double clicksMax;
		double freqMin;
		double freqMax;

		int xPixs = plotPanel.getWidth();
		int yPixs = plotPanel.getHeight();
		
		clicksMin=1;
		if (spectrogramData!=null){
		 clicksMax=spectrogramData.size()+1;
		}
		else{
			clicksMax=1;
		}
		
		freqMin=0;
		float sampleRate = clickControl.getClickDetector().getSampleRate();
		freqMax=sampleRate/1000/2;
		 		 
		double freqBins=(freqMax-freqMin)/spectrogramJava3DSize;
		double clickBins=(clicksMax-clicksMin)/spectrogramJava3DSize;
		
		Point3d WindowMin=mouseGetVirtualLocation.getRealLoc(new Point(xPixs,yPixs));
		Point3d WindowMax=mouseGetVirtualLocation.getRealLoc(new Point(0,0));

		southAxis.setRange(Math.round((WindowMax.getX()+spectrogramJava3DSize/2)*clickBins)+1, Math.round((WindowMin.getX()+spectrogramJava3DSize/2)*clickBins)+1);
		westAxis.setRange((WindowMin.getY()+49)*freqBins, (WindowMax.getY()+53)*freqBins);
		westAxis.setInterval((-(WindowMin.getY()+49)+ (WindowMax.getY()+53))*freqBins/ 4);
		axisPanel.repaint();
		
	}
	
	/**
	 * Creates a new spectrogram from current fft data. This function is called if the spectrogram needs redrawn (colour change, log values,etc ). To change fft data calcSpecData() must be called.
	 * @param fftData
	 */
	public void createNewSpectrogram(ArrayList<ArrayList<Float>> fftData){
		if (concatenatedSpectParams.normaliseAll==true){
			fftData=normaliseFFTDataIndiv( fftData);
		}
		else{
			fftData=normaliseFFTDataAll( fftData);
		}
		blank=true;
		createSpectrograms(fftData,concatenatedSpectParams.colourMap);
	}
	
	
	@Override
	public void clickedOnClick(ClickDetection click) {
		lastClick = click;
		
		if (click == null) {
			return;
		}

//		System.out.println("Create a spectrogram");
		
		if (click.getSuperDetection(0) != null) {
			
			currentEvent = click.getSuperDetection(0);
		
			if (currentEvent.getSubDetectionsCount() > 1 && currentEvent!=lastEvent) {
				
				spectrogramData= new ArrayList<ArrayList<Float>>();
				spectrogramData=calcSpecData( currentEvent,  chan);
				
				lastEvent=currentEvent;	
				cSpectrogram.clearGraphicsGroup();
				createNewSpectrogram(spectrogramData);
				addSpectrogram2D();
			
				if (threeD==true){
				addSpectrogram3D();
				}
			
				cSpectrogram.resetPlot(calcdefaultZoomFactor());
				blank=false;
				setPlotAxis() ;
			}
			
			else if (currentEvent==lastEvent) {
//				System.out.println("The current event is the same as the last event....");
				return;
			}
		}
		
		else{
//			System.out.println("No attached Event...");
			lastEvent=null;
			cSpectrogram.clearGraphicsGroup();
			blank=true;
		}
		//System.out.println("sub detections: "+click.getSubDetectionsCount());
		//System.out.println("super detections: "+click.getSuperDetectionsCount());
	}
	
	
	/** convert double[] fft data to log scale
	 * 
	 * @param FFT
	 * @return
	 */
	public double[] logScale(double[] FFT){
		if (FFT==null){
			return null;
		}
		double max=-Double.MAX_VALUE;
		double min=Double.MAX_VALUE;
		
		double[] logScale=new double[FFT.length];
		for (int i=0; i<FFT.length; i++){
			logScale[i]=-10*Math.log10(FFT[i]);
			
			if (max<logScale[i]){
				max=logScale[i];
			}
			if (min>logScale[i]){
				min=logScale[i];
			}
		}
		
		
		double logRange=(max-min)-concatenatedSpectParams.maxLogValS;
		
		for (int i=0; i<FFT.length; i++){
			logScale[i]=max-logScale[i];
			
			if (logScale[i]<logRange) {
				logScale[i]=0;
			}
			else{
				logScale[i]=logScale[i]-logRange;
			}
		}
		
		return logScale;
	}
	
	/**
	 * Calculate an 2D float arraylist of all the click FFT data;
	 * @param currentEvent
	 * @param chan
	 * @return
	 */
	public ArrayList<ArrayList<Float>> calcSpecData(PamDetection currentEvent, int chan){
		
	 spectrogramData= new ArrayList<ArrayList<Float>>();

		for (int i=0; i<currentEvent.getSubDetectionsCount();i++){
			
			ClickDetection clickr=(ClickDetection) currentEvent.getSubDetection(i);
			
			double[] clickFFT=clickr.getPowerSpectrum(chan,256);
			if (concatenatedSpectParams.logVal==true) clickFFT=logScale(clickFFT);
				
			ArrayList<Float> clickFFTfloat=new ArrayList<Float>();
			
			for (int j=0; j<clickFFT.length; j++){
			Double fftpoint=clickFFT[j];
			clickFFTfloat.add(fftpoint.floatValue());
			}
			spectrogramData.add(clickFFTfloat);	
		}
		
		return spectrogramData;
	}
	
	
	/**
	 * Pop up menu allowing user to change channel, switch between 2D and 3D plus access plotOptions
	 * @param point
	 */
	public JPopupMenu createPopupMenu() {
		
		JCheckBoxMenuItem jBoxMenuItem;
		
		JPopupMenu menu = new JPopupMenu();
		menu.setLightWeightPopupEnabled(false);

		JMenuItem menuItem = new JMenuItem("Plot Options...");
		menuItem.addActionListener(new Options());
		menu.add(menuItem);

		if (lastClick != null) {
			menu.addSeparator();
			for (int i = 0; i < PamUtils.getNumChannels(lastClick.getChannelBitmap()); i++) {
				jBoxMenuItem = new JCheckBoxMenuItem("Channel " + 
						PamUtils.getNthChannel(i, lastClick.getChannelBitmap()));
				jBoxMenuItem.setSelected(i==chan);
				jBoxMenuItem.addActionListener(new channelSelct(i));
				menu.add(jBoxMenuItem);
			}
		}
		menu.addSeparator();

		menuItem = new JMenuItem("2D");
		menuItem.addActionListener(new twoD());
		menu.add(menuItem);
		
		menuItem = new JMenuItem("3D");
		menuItem.addActionListener(new threeD());
		menu.add(menuItem);

		menuItem = new JMenuItem("Reset plot.");
		menuItem.addActionListener(new Reset());
		menu.add(menuItem);
		
		menu.addSeparator();
		
		menuItem = clipboardCopier3D.getCopyMenuItem();
		menu.add(menuItem);

		return menu;
	}
	
	
	/**
	 * Changes the spectrogram if a new channel is selected
	 * @author Jamie Macaulay
	 *
	 */
	class channelSelct implements ActionListener {
		
		int channel;

		public channelSelct(int channel) {
			super();
			this.channel = channel;
		}
		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (lastEvent!=null){
				chan=channel;
				spectrogramData=calcSpecData(currentEvent, chan);
				cSpectrogram.clearGraphicsGroup();
				createNewSpectrogram(spectrogramData);
				addSpectrogram2D();
				blank=false;
				if (threeD==true){
					addSpectrogram3D();
				}
				
			}
		}
		
	}

	
	class threeD implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			if (threeD==false && blank ==false){
			cSpectrogram.addMouseRotate();
			addSpectrogram3D();
			}
			threeD=true;
			}
		}

	
	class twoD implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			if (threeD==true && blank==false){
			cSpectrogram.clearGraphicsGroup();
			addSpectrogram2D();
			cSpectrogram.resetPlotRotation();
			cSpectrogram.removeMouseRotate();
			threeD=false;
			}		
			
		}
	}

	
	class Reset implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			cSpectrogram.resetPlot(calcdefaultZoomFactor());
			setPlotAxis();
			
		}
	}
	

	class Options implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			Point pt = new Point();
			pt = plotPanel.getLocationOnScreen();
			ConcatenatedSpectParams newOptions = ConcatenatedSpectrogramdialog.showDialog(
					clickControl.getPamView().getGuiFrame(), pt, concatenatedSpectParams);
			
			if (newOptions!=null){
				concatenatedSpectParams=newOptions.clone();
			}
			
			/*Need to be a bit messy here so we don't redraw the spectrogram unnecessarily. I'm sure there's a better way to store previous values of the 
			 * concatenated spectrogram params.*/
			 
			if (spectColour!=concatenatedSpectParams.colourMap || concatenatedSpectParams.logVal!=logVal || concatenatedSpectParams.maxLogValS!=maxLogVal  || concatenatedSpectParams.normaliseAll!=normaliseAll){

				if ( concatenatedSpectParams.logVal!=logVal || concatenatedSpectParams.maxLogValS!=maxLogVal ||  spectrogramData==null || spectrogramData.size()==0 ){
					spectrogramData=calcSpecData(currentEvent, chan);
				}
				logVal=concatenatedSpectParams.logVal;
				maxLogVal=concatenatedSpectParams.maxLogValS;
				spectColour=concatenatedSpectParams.colourMap;
				normaliseAll= concatenatedSpectParams.normaliseAll;
				
				cSpectrogram.clearGraphicsGroup();
				createNewSpectrogram(spectrogramData);
				addSpectrogram2D();
				blank=false;
				if (threeD==true){
				addSpectrogram3D();
				}
			}
		}
	}
	
	
	
	public double calcdefaultZoomFactor(){
//		Dimension panelSize=plotPanel.getSize();
//		double y=panelSize.getHeight();
//		double x=panelSize.getWidth();
//		double zoom=-((x/y)*118.36)+1.6095;
//		return zoom;
		return -115.77;
	}
			
	
	class windowResize implements ComponentListener{
		
		public void componentResized(ComponentEvent e) {
		   //System.out.println("window resize");  
		   setAspectRatio();
		 
		}

		@Override
		public void componentHidden(ComponentEvent arg0) {
		// TODO Auto-generated method stub
		
		}

		@Override
		public void componentMoved(ComponentEvent arg0) {
		// TODO Auto-generated method stub
		
		}

		@Override
		public void componentShown(ComponentEvent arg0) {
		// TODO Auto-generated method stub
		
		}
			
	}
	
	public void setAspectRatio(){
		Dimension panelSize=plotPanel.getSize();
		double y=panelSize.getHeight();
		double x=panelSize.getWidth();
		//System.out.println("Window Height: " + y + "Width: "+x);
		cSpectrogram.transformAspectRatio(new Vector3d(1,y/x,1));
	}

	
	@Override
	public void noteNewSettings() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getName() {
		return "Concatenated Spectrogram";
	}
	
}






	
	
	



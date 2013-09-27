package pamScrollSystem;

import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.Serializable;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import PamController.PamController;
import PamController.PamControllerInterface;
import PamUtils.PamCalendar;
import PamView.PamSymbol;
import PamguardMVC.PamDataBlock;

abstract public class AbstractPamScroller implements Serializable {

	private static final long serialVersionUID = 1L;

	static public final int HORIZONTAL = Adjustable.HORIZONTAL;
	static public final int VERTICAL = Adjustable.VERTICAL;

	transient protected Vector<PamScrollObserver> observers = new Vector<PamScrollObserver>();

	transient protected Vector<PamDataBlock> usedDataBlocks = new Vector<PamDataBlock>();

	protected PamScrollerData scrollerData = new PamScrollerData();

	/**
	 * @return the Swing component to go into the GUI. 
	 */
	public abstract JComponent getComponent();

	/**
	 * reference to the global scroll manager. 
	 */
	private AbstractScrollManager scrollManager;

	private JButton pageForward, pageBack;
	private JButton showMenu;
	private JPanel buttonPanel;

	private boolean needsNotify;

	private ScrollerCoupling scrollerCoupling;

	Color iconLine = Color.BLUE;
	Color iconFill = Color.BLUE;
	

	public AbstractPamScroller(String name, int orientation, int stepSizeMillis, long defaultLoadTime, boolean hasMenu) {
		super();
		this.scrollerData.name = new String(name);
		this.scrollerData.stepSizeMillis = stepSizeMillis;
		this.scrollerData.defaultLoadtime = defaultLoadTime;
		scrollManager = AbstractScrollManager.getScrollManager();
		scrollManager.addPamScroller(this);
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new BorderLayout());
		//
		//		pageForward = new JButton("", new ImageIcon(ClassLoader
		//				.getSystemResource("Resources/doubleForwardArrow.png")));
		if (hasMenu) {
			PamSymbol ps;
			pageForward = new JButton("", ps = new PamSymbol(PamSymbol.SYMBOL_DOUBLETRIANGLER, 12, 12, true, 
					iconFill, iconLine));
			ps.setIconHorizontalAlignment(PamSymbol.ICON_HORIZONTAL_CENTRE);
			//		pageForward = new JButton(new Character('\u21F0').toString());
			pageForward.addActionListener(new PageForwardAction());
			pageForward.setToolTipText("Move loaded data forward");

			pageBack = new JButton("", ps = new PamSymbol(PamSymbol.SYMBOL_DOUBLETRIANGLEL, 12, 12, true, 
					iconFill, iconLine));
			ps.setIconHorizontalAlignment(PamSymbol.ICON_HORIZONTAL_CENTRE);
			//		pageBack = new JButton(new Character('\u21e6').toString());
			pageBack.addActionListener(new PageBackAction());
			pageBack.setToolTipText("Move loaded data back");

			Dimension d = pageBack.getMaximumSize();
			d.width = d.height;
			//		pageBack.setMinimumSize(d);
			pageForward.setPreferredSize(d);
			pageBack.setPreferredSize(d);

			//			Character c = '\u21b7';
			showMenu = new JButton("", ps = new PamSymbol(PamSymbol.SYMBOL_TRIANGLED, 12, 12, true, 
					Color.DARK_GRAY, Color.DARK_GRAY));
			ps.setIconHorizontalAlignment(PamSymbol.ICON_HORIZONTAL_CENTRE);
			showMenu.setToolTipText("Scroll and data loading options");
			showMenu.addActionListener(new ShowMenuButtonPress());
			showMenu.addMouseListener(new MenuButtonMouse());
			showMenu.setPreferredSize(d);
			if (orientation == HORIZONTAL) {
				//			buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
				buttonPanel.add(BorderLayout.WEST, pageBack);
				buttonPanel.add(BorderLayout.EAST, pageForward);
			}
			else {
				//			buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
				buttonPanel.add(BorderLayout.SOUTH, pageBack);
				buttonPanel.add(BorderLayout.NORTH, pageForward);
			}
			buttonPanel.add(BorderLayout.CENTER, showMenu);
		}
	}
	
	/**
	 * Add a component to the scrollers mouse wheel listener. 
	 * All mouse wheel actions over that component will then be sent 
	 * to the scroller for processing. 
	 * @param component component
	 */
	public void addMouseWheelSource(Component component) {
	  component.addMouseWheelListener(new MouseWheel());
	}
	
	class MouseWheel implements MouseWheelListener {
		@Override
		public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent) {
			doMouseWheelAction(mouseWheelEvent);
		}
	}
	
	abstract void doMouseWheelAction(MouseWheelEvent mouseWheelEvent);

	/**
	 * @return the buttonPanel
	 */
	protected JPanel getButtonPanel() {
		return buttonPanel;
	}

	class PageForwardAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			pageForward();
		}
	}
	class PageBackAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			pageBack();
		}
	}
	class ShowMenuButtonPress implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			menuButtonPress();
		}
	}

	private class MenuButtonMouse extends MouseAdapter {

		@Override
		public void mousePressed(MouseEvent me) {
			if (me.isPopupTrigger()) {
				showMenuButtonPopup(me);
			}
		}

		@Override
		public void mouseReleased(MouseEvent me) {
			if (me.isPopupTrigger()) {
				showMenuButtonPopup(me);
			}
		}

	}

	/**
	 * Show standard menu for mouse right click on 
	 * the middle options button. 
	 * <p>Concrete instances of the scroller can either 
	 * override this or add to the standard menu
	 * @param me Mouse event
	 */
	void showMenuButtonPopup(MouseEvent me) {
		JPopupMenu menu = scrollManager.getStandardOptionsMenu(this);
		if (menu != null) {
			menu.show(showMenu, me.getX(), me.getY());
		}
	}

	protected void pageForward() {
		long range = scrollerData.maximumMillis - scrollerData.minimumMillis;
		long step = range * scrollerData.pageStep / 100;
		long currVal = getValueMillis();
		long newMin = scrollerData.minimumMillis + step;
		long newMax = scrollerData.maximumMillis + step;
		newMin = scrollManager.checkGapPos(this, scrollerData.minimumMillis, scrollerData.maximumMillis, newMin, newMax, +1);
		scrollerData.minimumMillis = newMin;
		scrollerData.maximumMillis = newMin + range;
		
		scrollerData.maximumMillis = scrollManager.checkMaximumTime(scrollerData.maximumMillis);
		scrollerData.minimumMillis = scrollerData.maximumMillis - range;
		rangesChangedF(currVal);
	}

	protected void pageBack() {
		long range = scrollerData.maximumMillis - scrollerData.minimumMillis;
		long step = range * scrollerData.pageStep / 100;
		long currVal = getValueMillis();
		long newMin = scrollerData.minimumMillis - step;
		long newMax = scrollerData.maximumMillis - step;
		newMin = scrollManager.checkGapPos(this, scrollerData.minimumMillis, scrollerData.maximumMillis, newMin, newMax, -1);
		scrollerData.minimumMillis = newMin;
		scrollerData.maximumMillis = newMin + range;
		
		scrollerData.minimumMillis = scrollManager.checkMinimumTime(scrollerData.minimumMillis);
		scrollerData.maximumMillis = scrollerData.minimumMillis + range;
		rangesChangedF(currVal);
	}

	protected void menuButtonPress() {
		PamScrollerData newData = LoadOptionsDialog.showDialog(null, this, showMenu);
		if (newData != null) {
			scrollerData = newData;
			rangesChangedF(getValueMillis());
		}
	}

	public void destroyScroller() {
		scrollManager.removePamScroller(this);
	}

	/**
	 * Ad an observer that will receive notifications when the
	 * the scroller moves.  
	 * @param pamScrollObserver 
	 */
	public void addObserver(PamScrollObserver pamScrollObserver) {
		if (observers.indexOf(pamScrollObserver) < 0) {
			observers.add(pamScrollObserver);
		}
	}

	/**
	 * Remove an observer which no longer requires notifications when
	 * the scroller moves. 
	 * @param pamScrollObserver
	 */
	public void removeObserver(PamScrollObserver pamScrollObserver) {
		observers.remove(pamScrollObserver);
	}

	/**
	 * Add a datablock to the list for this scroller. 
	 * <p>
	 * When the scroller is moved, data from data blocks in 
	 * this list will re read from the database and binary stores. 
	 * Other data will not be read. 
	 * @param dataBlock a PamDataBlock
	 */
	public void addDataBlock(PamDataBlock dataBlock) {
		if (dataBlock == null) return;
		if (usedDataBlocks.indexOf(dataBlock) < 0) {
			usedDataBlocks.add(dataBlock);
		}
	}

	/**
	 * Remove a datablock from the viewed list. 
	 * @param dataBlock a PamDataBlock
	 */
	public void removeDataBlock(PamDataBlock dataBlock) {
		if (dataBlock == null) return;
		usedDataBlocks.remove(dataBlock);
	}
	
	/**
	 * Remove all datablocks from the viewed list. 
	 */
	public void removeAllDataBlocks() {
		usedDataBlocks.removeAllElements();
	}

	/**
	 * See if this scroller is using a particular data block
	 * @param dataBlock a Pamguard data block
	 * @return true if it's being used. 
	 */
	public boolean isDataBlockUsed(PamDataBlock dataBlock) {
		return (usedDataBlocks.indexOf(dataBlock) >= 0);
	}
	/**
	 * Another managed scroller moved it's position
	 * @param newValue new value in millis
	 */
	abstract public void anotherScrollerMovedInner(long newValue);

	/**
	 * Another managed scroller moved its outer position - will cause
	 * new data to be loaded. 
	 * @param newMin
	 * @param newMax
	 */
	public void anotherScrollerMovedOuter(long newMin, long newMax) {
		// basically, this scroller has to remain around 
		// or within the one which has just moved. 
		long thatLen = newMax-newMin;
		long thisLen = scrollerData.getLength();
		if (thisLen == thatLen && scrollerData.minimumMillis != newMin) {
			setRangeMillis(newMin, newMax, false);
		}
		else if (thisLen > thatLen) {
			if (scrollerData.minimumMillis > newMin) {
				setRangeMillis(newMin, newMin+thisLen, false);
			}
			else if (scrollerData.maximumMillis < newMax) {
				setRangeMillis(newMax-thisLen, newMax, false);
			}
		}
		else {
			if (scrollerData.minimumMillis < newMin) {
				setRangeMillis(newMin, newMin+thisLen, false);
			}
			else if (scrollerData.maximumMillis > newMax) {
				setRangeMillis(newMax-thisLen, newMax, false);
			}			
		}
	}

	/**
	 * Send notification to all observers of this scroll bar to say
	 * that the value set by the slider in the scroll bar has changed. 
	 */
	protected void notifyValueChange() {
		/*
		 * always set the calendar position to that of the latest
		 * scroll bar to move !
		 */
		PamCalendar.setViewPosition(getValueMillis());
		for (int i = 0; i < observers.size(); i++) {
			observers.get(i).scrollValueChanged(this);
		}
		notifyCoupledScrollers();
		/*
		 *  only put this out in viewer mode since it's causing trouble in real time
		 *  with modules which were lazily written and reset themselves everytime
		 *  ANY notification arrives. 
		 */
		if (PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW) {
			PamController.getInstance().notifyModelChanged(PamControllerInterface.NEW_SCROLL_TIME);
		}
	}

	/**
	 * Send a notification to all observers of this scroller to say
	 * that the range of data loaded has changed. 
	 */
	protected void notifyRangeChange() {
		if (PamCalendar.getTimeInMillis() <= 0) {
			PamCalendar.setViewPosition(getValueMillis());
		}
		notifyValueChange();
		for (int i = 0; i < observers.size(); i++) {
			observers.get(i).scrollRangeChanged(this);
		}
	}
	/**
	 * @return the minimumMillis
	 */
	public long getMinimumMillis() {
		return scrollerData.minimumMillis;
	}

	/**
	 * @return the maximumMillis
	 */
	public long getMaximumMillis() {
		return scrollerData.maximumMillis;
	}

	/**
	 * @return the difference between getMaximumMills and getMinimumMillis
	 */
	public long getRangeMillis() {
		return scrollerData.maximumMillis-scrollerData.minimumMillis;
	}

	/**
	 * Set the range of the currently loaded data and optionally notify other
	 * scrollers. 
	 * @param minimumMillis minimum time in milliseconds
	 * @param maximumMillis maximum time in milliseconds
	 * @param notify notify the rangesChanged function. 
	 */
	public void setRangeMillis(long minimumMillis, long maximumMillis, boolean notify) {
		scrollerData.minimumMillis = minimumMillis;
		scrollerData.maximumMillis = maximumMillis;
//		if (getValueMillis() < minimumMillis) {
//			setValueMillis(scrollerData.minimumMillis);
//		}
		if (notify) {
			rangesChangedF(getValueMillis());
		}
		else {
			needsNotify = true;
		}
	}

	/**
	 * called when the set range is changed with a flag to send out a
	 * notification. Does some things that must be done, but also 
	 * calls an abstract setRanges in order that specific scrollers
	 * can update their scrolling component. 
	 * @param setValue scroller position in milliseconds. 
	 */
	private final void rangesChangedF(long setValue) {
		rangesChanged(setValue);
		setValueMillis(setValue);
		notifyCoupledScrollers();
		scrollManager.moveOuterScroller(this, getMinimumMillis(), getMaximumMillis());
	}
	/**
	 * Called when ranges have been changed and tells 
	 * scroller to go to a particular absolute value. 
	 * @param setValue
	 */
	abstract public void rangesChanged(long setValue);

	/**
	 * Command passed through the the scroll manager telling it reload data. 
	 */
	public void reLoad() {
		scrollManager.reLoad();
	}

	//	/**
	//	 * Set the maximum of the range of the scroller in milliseconds. 
	//	 * @param millis milliseconds (using standard Jva millisecond time)
	//	 */
	//	public void setMaximumMillis(long millis) {
	//		scrollerData.maximumMillis = millis;
	//	}
	//	
	//	/**
	//	 * Set the maximum of the range of the scroller in milliseconds. 
	//	 * @param millis milliseconds (using standard Jva millisecond time)
	//	 */
	//	public void setMinimumMillis(long millis) {
	//		scrollerData.minimumMillis = millis;
	//	}

	/**
	 * @return the valueMillis
	 */
	abstract public long getValueMillis();

	/**
	 * @param valueMillis the valueMillis to set
	 */
	final public void setValueMillis(long valueMillis) {
		valueSetMillis(valueMillis);
		notifyCoupledScrollers();
	}

	/**
	 * Called when a new position has been set
	 * @param valueMillis new scroll value in milliseconds
	 */
	abstract public void valueSetMillis(long valueMillis) ;

	/**
	 * stepSizeMillis is the resolution of the scroller in milliseconds. 
	 * <p>For displays which will only ever display a short amount of data
	 * this can be one, however for longer displays this should be 1000 (a second)
	 * or more to avoid wrap around of the 32 bit integers used to control the 
	 * actual scroll bar. 
	 * @return the stepSizeMillis
	 */
	public int getStepSizeMillis() {
		return scrollerData.stepSizeMillis;
	}

	/**
	 * @param stepSizeMillis the stepSizeMillis to set
	 */
	public void setStepSizeMillis(int stepSizeMillis) {
		scrollerData.stepSizeMillis = Math.max(1, stepSizeMillis);
	}

	//	/**
	//	 * @return the blockIncrement
	//	 */
	//	public int getBlockIncrement() {
	//		return 1;
	//	}

	/**
	 * @param blockIncrement the blockIncrement to set in millis
	 */
	public void setBlockIncrement(long blockIncrement) {
	}
	//
	//	/**
	//	 * @return the unitIncrement
	//	 */
	//	public long getUnitIncrement()

	/**
	 * @param unitIncrement the unitIncrement to set in millis
	 */
	public void setUnitIncrement(long unitIncrement) {
	}

	//	/**
	//	 * @return the visibleAmount
	//	 */
	//	public int getVisibleAmount() {
	//		return visibleAmount;
	//	}

	/**
	 * @param visibleAmount the visibleAmount to set in millis
	 */
	public void setVisibleAmount(long visibleAmount) {
	}

	/**
	 * @return the observers
	 */
	public Vector<PamScrollObserver> getObservers() {
		return observers;
	}

	/**
	 * @return the pageStep
	 */
	public int getPageStep() {
		return scrollerData.pageStep;
	}

	/**
	 * @param pageStep the pageStep to set
	 */
	public void setPageStep(int pageStep) {
		scrollerData.pageStep = pageStep;
	}

	protected long getDefaultLoadtime() {
		return scrollerData.defaultLoadtime;
	}

	protected void setDefaultLoadtime(long defaultLoadtime) {
		scrollerData.defaultLoadtime = defaultLoadtime;
	}

	/**
	 * @return the scrollManager
	 */
	public AbstractScrollManager getScrollManager() {
		return scrollManager;
	}

	/**
	 * Set the visibility of the scroll bar component. 
	 * @param b
	 */
	public void setVisible(boolean b) {
		if (getComponent() == null) {
			return;
		}
		getComponent().setVisible(b);
	}

	/**
	 * Couple this scroller to another scroller so that both
	 * have exactly the same behaviour, load the same data period, 
	 * move their scrolls together, etc. 
	 * <p>
	 * Scollers are coupled by name so that they don't necessarily
	 * need to find references to each other in the code. These names 
	 * can be anything by measures should be taken to ensure that they
	 * are going to be unique, for example by using module names as
	 * part of the coupling name.  
	 * @param couplingName name of the coupling
	 * @return number of other scrollers in this coupling
	 */
	public ScrollerCoupling coupleScroller(String couplingName) {
		uncoupleScroller();
		return scrollerCoupling = scrollManager.coupleScroller(this, couplingName);
	}

	/**
	 * Remove the scroller from it's coupling. 
	 */
	public void uncoupleScroller() {
		scrollManager.uncoupleScroller(this);
		scrollerCoupling = null;
	}

	public ScrollerCoupling getScrollerCoupling() {
		return scrollerCoupling;
	}

	public void setScrollerCoupling(ScrollerCoupling scrollerCoupling) {
		this.scrollerCoupling = scrollerCoupling;
	}

	/**
	 * Called when a scroller which is coupled to this scroller changes
	 * in any way. 
	 * @param scroller coupled scroller which changes. 
	 */
	public void coupledScrollerChanged(AbstractPamScroller scroller) {
		scrollerData = scroller.scrollerData.clone();
		setValueMillis(scroller.getValueMillis());
		rangesChanged(scroller.getValueMillis());
	}

	/**
	 * Tell other scrollers coupled to this one that there has been a change 
	 */
	private void notifyCoupledScrollers() {
		if (scrollerCoupling != null) {
			scrollerCoupling.notifyOthers(this);
		}
	}

	/**
	 * @return the number of data blocks observed by this scroller
	 */
	public int getNumUsedDataBlocks() {
		return usedDataBlocks.size();
	}

	/**
	 * Get a specific data block observed by this scroller. 
	 * @param iBlock block index
	 * @return reference to an observed datablock 
	 */
	public PamDataBlock getUsedDataBlock(int iBlock) {
		return usedDataBlocks.get(iBlock);
	}

}

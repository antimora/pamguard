package PamView;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import javax.swing.SwingUtilities;

/**
 * Class to launch a thread which will get the screen size 
 * (inlcuding the bounds of multiple monitors)
 * <p>
 * The process of getting the dimensions is launched in a 
 * different thread and can be going on while other PAMGAURD
 * startup processes are initialising.
 *  
 * @author Doug
 *
 */
public class ScreenSize {

	private volatile static Rectangle screenDimension = null;
	
	private volatile static boolean hasRun = false;

	private ScreenSize() {
		//Problems with startup on Mac that 
		//seem to have been caused by the 
		//GetBounds not being invoked from within
		//the Event Display Thread
		//CJB 2009-06-15
		SwingUtilities.invokeLater(new GetBounds());
		//old version 
		//Thread thread = new Thread(new GetBounds());
		//thread.start();
		
	}
	
	/**
	 * Only needs to be called once to 
	 * start the background process which gets
	 * the virtual screen size. 
	 * <p>
	 * Is called from PAMGUARD Main to get it
	 * going asap. 
	 */
	public static void startScreenSizeProcess() {
		if (hasRun == false) {
			hasRun = true;
			new ScreenSize();
		}
	}
	
	/**
	 * Gets the screen bounds. Returns null 
	 * if they have not yet been set in the background thread. 
	 * @return Virtual screen size, or null. 
	 */
	public static Rectangle getScreenBounds() {
		return screenDimension;
	}
	
	/**
	 * Gets the virtual screen size. Will wait for them to be
	 * extracted from the background thread up to some set timeout
	 * @param maxWaitMillis Max time to wait.
	 * @return virtual screen dimension, or null if unavailable 
	 * after the set wait period. 
	 */
	public static Rectangle getScreenBounds(int maxWaitMillis) {
		
		startScreenSizeProcess();
		
		long endTime = System.currentTimeMillis() + maxWaitMillis;
		while (System.currentTimeMillis() <= endTime) {
			if (screenDimension != null) {
				return screenDimension;				
			}
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException e) {
				return screenDimension;
			}
		}
		return screenDimension;
	}
	
	/**
	 * 
	 * @return true if screen bounds are available. 
	 */
	public boolean haveScreenBounds() {
		return (screenDimension != null);
	}

	/**
	 * Thread to obtain the screen bounds. 
	 * @author Doug
	 *
	 */
	public class GetBounds implements Runnable {

		@Override
		public void run() {

			screenDimension = getScreenBounds();

		}

		private Rectangle getScreenBounds() {
			Rectangle virtualBounds = new Rectangle();
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice[] gs =ge.getScreenDevices();
			for (int j = 0; j < gs.length; j++) { 
				GraphicsDevice gd = gs[j];
				//Think to get screen sizes on multiple monitors
				//can just use getDefaultConfiguration for
				//each device rather than looping over 
				//all configurations. Hopefully should 
				//let PAMGUARD start up a little quicker
				//CJB 2009-06-15
				GraphicsConfiguration dgc = gd.getDefaultConfiguration();
				virtualBounds = virtualBounds.union(dgc.getBounds());
				
				//old version that loops over all configs
				//GraphicsConfiguration[] gc =
				//	gd.getConfigurations();
				//for (int i=0; i < gc.length; i++) {
				//	virtualBounds =
				//		virtualBounds.union(gc[i].getBounds());
				////}
			} 
			//System.out.println("virtualBounds="+virtualBounds);
			return virtualBounds;
		}

	}

}

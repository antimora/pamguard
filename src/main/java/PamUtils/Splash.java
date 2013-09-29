/*	PAMGUARD - Passive Acoustic Monitoring GUARDianship.
 * To assist in the Detection Classification and Localisation 
 * of marine mammals (cetaceans).
 *  
 * Copyright (C) 2006 
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package PamUtils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.SplashScreen;
import java.awt.Rectangle;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import PamController.PamController;
import PamController.PamguardVersionInfo;

/**
 * Splash screen
 * @author David McLaren / Douglas Gillespie
 * Displays a splash screen used at Program start up and also 
 * when Help / about menu selected.
 * <p>
 * Rewritten for V1.1.1 (August 2008) so that a simple images is used 
 * containing no text and the version information is written over the
 * image. Makes updating for future releases a lot easier !
 * <p>
 * Changed around a little to allow for the native JVM to be displayed
 * at startup and then this Class replaces that on screen with one
 * that shows the version number and the GPL. Also made sure that 
 * the main GUI changing bit is invoked from the Event Dispatch Thread
 * CJB 2009-06-15
 *
 */
 
//make this class implement Runnable so if needs be the 
//main stuff that can effect the GUI be called using
//SwingUtilities.invokeAndWait() CJB  
public class Splash extends JWindow implements Runnable {	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4269582461538539626L;

	static private final int border = 20;	
	static private volatile boolean startupErrors = false;
	private int displayDuration;
	private int runMode;
	
	public Splash(int inDisplayDuration, int inRunMode) {
		displayDuration=inDisplayDuration;
		runMode=inRunMode;
		//"About PAMGUARD" calls this from within the EDT
		//in which case just need to use run() 
		if (SwingUtilities.isEventDispatchThread()) {
			this.run();
		}
		else {
			//let's us invokeAndWait rahter than invokeLater
			//as shouldn't take long and want to try to 
			//ensure this Splash Screen is visible whilst 
			//most of other set up stuff starts to happen
			try {
	           SwingUtilities.invokeAndWait(this);
	        }
	        catch (Exception e) {
	            e.printStackTrace();
	        }
		}
	}
	
	
	public void run()  
	   { 
		//let's see if we have a JVM SplashScreen already displayed
		//and if so draw our own exactly where it is on screen 
		//which should cause the JVM one to close automatically
		SplashScreen jvmSplashScreen;
		Rectangle jvmSplashBounds;
		
		try {
			jvmSplashScreen=SplashScreen.getSplashScreen();
			jvmSplashBounds=jvmSplashScreen.getBounds();
		}
		catch (Exception e) {
			jvmSplashScreen=null;
			jvmSplashBounds=null;
	    }

		String filename = "Resources/pamguardSplash.png";
//		String filename = "Resources/pamguardSplashBack.jpg";
//		String filelogo = "Resources/pamguardSplashFore.png";

		/*
		 * Use classloader to get image out of Jar file. 
		 */
		URL url = ClassLoader.getSystemResource(filename);
		if (url == null) {
			System.out.println("Can't find splash image " + filename);
			return;
		}
		BufferedImage splashImage = null;
		try {
			splashImage = ImageIO.read(url);
//			splashImage = splashImage.getSubimage(splashImage.getWidth()-787, 0, 787, splashImage.getHeight());
		}
		catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		/*
		 * Use classloader to get image out of Jar file. 
		 */
//		URL urlLogo = ClassLoader.getSystemResource(filelogo);
//		if (urlLogo == null) {
//			System.out.println("Can't find splash image " + filelogo);
//			return;
//		}
//		BufferedImage foreImage = null;
//		try {
//			foreImage = ImageIO.read(urlLogo);
//		}
//		catch (IOException e) {
//			e.printStackTrace();
//			return;
//		}
		
		// write version information to a String
		String version = "v" + PamguardVersionInfo.version + " " + PamguardVersionInfo.getReleaseType().toString();
		
		switch (runMode) {
//		case PamController.RUN_NORMAL:
//			
//			break;
		case PamController.RUN_PAMVIEW:
			version = "Viewer Mode " + version;
			break;
		case PamController.RUN_MIXEDMODE:
			version = "Mixed Mode " + version;
			break;
		case PamController.RUN_NETWORKRECEIVER:
			version = "NetReceiver " + version;
			break;
//		case PamController.RUN_NOTHING:
//			
//			break;
//		case PamController.RUN_REMOTE:
//			
//			break;
		default:
			break;
		}
		
		// get image graphics handle.
		Graphics g = splashImage.getGraphics();
		
		// get a decent sized font. 
		Font f = new Font("Arial",Font.PLAIN | Font.BOLD, 20);
		int gr = 215;
		g.setColor(new Color(gr,gr,gr));
		g.setFont(f);
		FontMetrics fm = g.getFontMetrics();
		Rectangle2D r = fm.getStringBounds(version, g);
		Dimension d = new Dimension( splashImage.getWidth(), splashImage.getHeight());
		
		// draw the strings onto the image. 
		try{//if g can be classed as g2 the draw as antialiased
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
			//could use this method to draw PG logo on background image - not as smooth as pre-processed in gimp
			
//			int leftOffset,topOffset=-20;
//			double wScale,hScale=1.0;
//			leftOffset=80;
//			wScale=hScale=1.5;
//			g2.drawImage(foreImage, leftOffset, 0, leftOffset+(int)Math.round(wScale*foreImage.getWidth()), topOffset+(int)Math.round(foreImage.getHeight()*hScale), 0, 0, foreImage.getWidth(), foreImage.getHeight(), null);
			
			g2.drawString(version, (int) (d.width - r.getWidth() - border), (border + fm.getHeight()*1));
			r = fm.getStringBounds(PamguardVersionInfo.webAddress, g);
			g2.drawString(PamguardVersionInfo.webAddress, (int) (d.width - r.getWidth() - border), (border + fm.getHeight()*2));
		}catch (Exception e) {//else draw normally
			g.drawString(version, (int) (d.width - r.getWidth() - border), (border + fm.getHeight()*1));
			r = fm.getStringBounds(PamguardVersionInfo.webAddress, g);
			g.drawString(PamguardVersionInfo.webAddress, (int) (d.width - r.getWidth() - border), (border + fm.getHeight()*2));
		}
		
		// convert to Icon so it scales automatically in layout. 
		ImageIcon ii = new ImageIcon(splashImage);
		
		JLabel label = new JLabel(ii);
		
		//splashWindow.getContentPane().add(label, BorderLayout.CENTER);
		getContentPane().add(label, BorderLayout.CENTER);
		
		/*
		 * Add GNU license information in bottom panel. 
		 */
		JTextArea ta = new JTextArea(PamguardVersionInfo.license);
		ta.setBackground(new Color(250, 250, 255));
		Font taFont = ta.getFont().deriveFont((float) 12.0);
		ta.setFont(taFont);
		ta.setBorder(new EmptyBorder(10, 10, 10, 10));
		ta.setWrapStyleWord(true);
		ta.setLineWrap(true);
		ta.setEditable(false);
			
		getContentPane().add(ta, BorderLayout.SOUTH);
		
		pack();			//this sets the size for the window
		ta.validate();	//this sets the line returns etc knowing the width the window will be
		pack();			//this fixes the heights
		
		setAlwaysOnTop(true);

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		Dimension labelSize = label.getPreferredSize();

		if (jvmSplashBounds==null) {
			setLocation(screenSize.width / 2 - (labelSize.width / 2),
					screenSize.height / 2 - (labelSize.height / 2));
		}
		else {
			setLocation(jvmSplashBounds.x,jvmSplashBounds.y);
		}
		
		final Runnable closeSplashRunnable = new Runnable() {
			public void run() {
				try {
					setVisible(false);
					dispose();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		
		final int pauseTimeout = displayDuration; // Cast to final

		Runnable displayDurationRunnable = new Runnable() {
			public void run() {
				try {
					Thread.sleep(pauseTimeout);
					//Commented out as believe this call
					//now to be redundant as call made earlier
					//in main Pamguard.java code
					//CJB 2009-06-15
					//ScreenSize.getScreenBounds(5000);
					
					//OK at next chance get the EDT to close the JWindow  
					//CJB 2009-06-15
					SwingUtilities.invokeLater(closeSplashRunnable);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		setVisible(true);
		
		Thread splashThread = new Thread(displayDurationRunnable,
				"PamGuard Splash");
		splashThread.start();
		
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				closeSplashRunnable.run();
			}
		});
	}
	
	
//	private boolean waitForGuiFrame(int maxWaitSeconds) {
//		int waitTime = 1000;
//		int waitCount = 0;
//		while (PamGui.isSomethingShowing() == false & waitCount < maxWaitSeconds) {
//			if (startupErrors) {
//				return PamGui.isSomethingShowing();
//			}
//			try {
//				Thread.sleep(waitTime);
//				waitCount++;
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//		return PamGui.isSomethingShowing();
//	}
	public static boolean isStartupErrors() {
		return startupErrors;
	}
	public static void setStartupErrors(boolean startupErrors) {
		Splash.startupErrors = startupErrors;
	}
}
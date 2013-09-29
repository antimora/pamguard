package pamguard;
/*	PAMGUARD - Passive Acoustic Monitoring GUARDianship.
 * To assist in the Detection Classification and Localisation 
 * of marine mammals (cetaceans).
 *  
 * Copyright (C) 2006, Doug Gillespie, Paul Redmond, David McLaren, Rick Dewar
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

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import PamController.PamController;
import PamController.PamSettingManager;
import PamController.PamguardVersionInfo;
import PamController.pamBuoyGlobals;
import PamUtils.Splash;
import PamView.ScreenSize;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;

/**
 * Pamguard main class. 
 * 
 * @author Douglas Gillespie
 *
 */
public class Pamguard {

	/**
	 * PAMGUARD can be started in three different modes. <p>
	 * Normal mode (no args in) is for everyday data processing 
	 * and collection. <p>
	 * -v = Viewer mode which will connect to a database and re-display data
	 * from a given time period. <p>
	 * -m = Mixed mode which will connect to a database, Sounds are analysed from file
	 * and new results written to the database, but other data, such as GPS data, are read from
	 * the database synchronised in time to the audio data to correctly reconstruct 
	 * tracks, etc. 
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			//			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
			UIManager.setLookAndFeel(new WindowsLookAndFeel());
		} catch (Exception e) { }

		int runMode = PamController.RUN_NORMAL;
		String InputPsf = "NULL";

		if (args != null) {
			int nArgs = args.length;
			int iArg = 0;
			String anArg;
			while (iArg < nArgs) {
				anArg = args[iArg++];
				if (anArg.equalsIgnoreCase("-v")) {
					runMode = PamController.RUN_PAMVIEW;
					System.out.println("PAMGUARD Viewer");
				}
				else if (anArg.equalsIgnoreCase("-m")) {
					runMode = PamController.RUN_MIXEDMODE;
					System.out.println("PAMGUARD Offline mixed mode");
				}
				else if (anArg.equalsIgnoreCase("-nr")) {
					runMode = PamController.RUN_NETWORKRECEIVER;
					System.out.println("PAMGUARD Network Reciever Mode");
				}
				else if (anArg.equalsIgnoreCase("-r")) {
					runMode = PamController.RUN_REMOTE;
					PamSettingManager.RUN_REMOTE=true;//quick fix better fix to come
					System.out.println("PAMBUOY - remote non gui build.");
				}
				else if (anArg.equalsIgnoreCase("-psf")) {
					String autoPsf = args[iArg++];
					PamSettingManager.remote_psf = autoPsf;
					System.out.println("Running using settings from " + autoPsf);
				}
//				else if (args[i].equalsIgnoreCase("-wav")) {
//					PamSettingManager.external_wav = args[i+1];
//					pamBuoyGlobals.setWavString(PamSettingManager.external_wav);
//					System.out.println("Running using the wav " + PamSettingManager.external_wav + "BS" + pamBuoyGlobals.getWavString());
//				}
//				else if (args[i].equalsIgnoreCase("-recdir")) {
//					pamBuoyGlobals.setWavRecDir(args[i+1]);
//				}
//				else if (args[i].equalsIgnoreCase("-bin")) {
//					pamBuoyGlobals.setBinDir(args[i+1]);
//				}
				else if (anArg.equalsIgnoreCase("-port")) {
					pamBuoyGlobals.setNetworkControlPort(Integer.parseInt(args[iArg++]));
				}
				
				else if (anArg.equalsIgnoreCase("-help")) {
					System.out.println("--PamGuard Help");
					System.out.println("\n--For standard GUI deployment run without any options.\n");
					System.out.println("\n--For command line deployment the following options are valid.\n");

					System.out.println("  -r                           : run as a non GUI application");
					System.out.println("  -psf <filename>              : use psf settings from filename");
					System.out.println("  -port  <value>               : UDP connection port.");
					System.out.println("  -devDebug  <str>             : Debug String - Used to activate debug messages.");
					System.out.println("\n--Example command lines.\n");
					System.out.println("java -Djava.library.path=./lib -jar pamGuardGui.jar -r -psf ../experimentalData/opNiTestVisualFft.psf");
					System.out.println("\n");
					System.exit(0);
				}
			}
		}
		//going to need the runmode inside a Runnable later 
		final int chosenRunMode = runMode;
		if(runMode != PamController.RUN_REMOTE) {
			ScreenSize.startScreenSizeProcess();
		}

		/*
		 * 
		 */
		// put some text onto the console at the start saying which version 
		// and run mode we're in, etc.
		// write version information to a String
		if (System.getProperty("os.name").equals("Linux")) {
			String[] command = {
					"cat",
					"/proc/cpuinfo"
			};
			ProcessBuilder pb = new ProcessBuilder(command);
			try {
				Process p = pb.start();
				BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line = br.readLine();
				System.out.println("PAMGUARD running on");
				while (line != null) {
					System.out.println(line);
					if (line.startsWith("cpuid level")) {
						line = null;                  
					}else{
						line = br.readLine();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		System.out.println("PAMGUARD Version " + PamguardVersionInfo.version + " branch " + PamguardVersionInfo.getReleaseType().toString());
		System.out.println("Revision " + PamguardVersionInfo.getRevision());
		writePropertyString("java.version");
		writePropertyString("java.vendor");
		writePropertyString("java.vm.version");
		writePropertyString("java.vm.name");
		//		writePropertyString("java.specification.name");
		writePropertyString("os.name");
		writePropertyString("os.arch");
		writePropertyString("os.version");
		writePropertyString("java.library.path");
		//		writePropertyString("user.home");
		//		writePropertyString("user.dir");
		System.out.println("For further information and bug reporting visit " + PamguardVersionInfo.webAddress);
		System.out.println("If possible, bug reports and support requests should \ncontain a copy of the full text displayed in this window.");
		System.out.println("(Windows users right click on window title bar for edit / copy options)");
		System.out.println("");


		int spashTime = 5000;
		if(!PamSettingManager.RUN_REMOTE) {
			if (PamguardVersionInfo.getReleaseType() == PamguardVersionInfo.ReleaseType.SMRU){
				spashTime = 1000;
			}
			new Splash(spashTime, chosenRunMode);
		}
		
		final Runnable createPamguard = new Runnable() {
			public void run() {
				PamController.create(chosenRunMode);
			}
		};
		//Amongst other stuff the call to PamController.create()
		//will build and show the GUI and the user can't
		//do much else until that's done so let's have all
		//that kicked off from with the EDT CJB 2009-06-16 
		SwingUtilities.invokeLater(createPamguard);
		//PamController.create(chosenRunMode);
	}

	static private void writePropertyString(String key) {
		String property = System.getProperty(key);
		if (property == null) {
			System.out.println(String.format("%s: No such property", key));
		}
		else {
			System.out.println(String.format("%s %s", key, property));
		}
	}

}

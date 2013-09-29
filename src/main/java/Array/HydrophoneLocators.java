package Array;

import java.util.ArrayList;

import networkTransfer.receive.NetworkHydrophoneLocator;

import PamController.PamController;
import PamController.PamguardVersionInfo;

public class HydrophoneLocators {

	static ArrayList<String> locatorNames;
	
	static HydrophoneLocators singleInstance;
	
	private boolean isNetwork;
	
	private HydrophoneLocators() {
		isNetwork = (PamController.getInstance().getRunMode() == PamController.RUN_NETWORKRECEIVER);
		locatorNames = new ArrayList<String>();
		locatorNames.add("Straight / rigid Hydrophone");
		locatorNames.add("Threading Hydrophone");
		if (isNetwork || PamguardVersionInfo.getReleaseType() == PamguardVersionInfo.ReleaseType.SMRU) {
			locatorNames.add("Networked Buoy GPS data");
		}
//		locatorNames.add("Terrella Hydrophone Locator");
	}
	
	static HydrophoneLocators getInstance() {
		if (singleInstance == null) {
			singleInstance = new HydrophoneLocators();
		}
		return singleInstance;
	}
	
	public HydrophoneLocator get(int i, PamArray pamArray) {
		switch (i) {
		case 0:
			return new StraightHydrophoneLocator(pamArray);
		case 1:
			return new ThreadingHydrophoneLocator(pamArray);
		case 2: // don't change this - see Array<anager.checkBuoyHydropneStreamer
			return new NetworkHydrophoneLocator(pamArray);
//		case 2:
//			return new TerrellaHydrophoneLocator(pamArray);
		}
		return null;
	}
	
	public String getName(int i) {
		if (i < locatorNames.size()) {
			return locatorNames.get(i);
		}
		return null;
	}
	
	public int getCount() {
		return locatorNames.size();
	}
	
	public ArrayList<String> getNames() {
		return locatorNames;
	}
}

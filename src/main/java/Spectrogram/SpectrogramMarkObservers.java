package Spectrogram;

import java.util.ArrayList;

/**
 * Class for registration of classes that will observer
 * spectrgram displays. Only has static members.
 * @author Doug Gillespie
 *
 */
public class SpectrogramMarkObservers {
	
	private static ArrayList<SpectrogramMarkObserver> observers =
		new ArrayList<SpectrogramMarkObserver>();
	
	public static void addSpectrogramMarkObserver(SpectrogramMarkObserver spectrogramMarkObserver) {
		if (observers.contains(spectrogramMarkObserver) == false) {
			observers.add(spectrogramMarkObserver);
		}
	}
	
	public static void removeDisplayPanelProvider(SpectrogramMarkObserver spectrogramMarkObserver) {
		observers.remove(spectrogramMarkObserver);
	}
	
	public static ArrayList<SpectrogramMarkObserver> getSpectrogramMarkObservers() {
		return observers;
	}
	
	
}

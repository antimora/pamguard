package Spectrogram;

public interface SpectrogramMarkObserver {

	public static final int MOUSE_DOWN = 0;
	public static final int MOUSE_UP = 1;
	
	void spectrogramNotification(SpectrogramDisplay display, int downUp, int channel, 
			long startMilliseconds, long duration, double f1, double f2);
	
	String getMarkObserverName();
	
}

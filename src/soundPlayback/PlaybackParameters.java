package soundPlayback;

import java.io.Serializable;

/**
 * Parameters controlling sound playback
 * 
 * @author Doug Gillespie
 *
 */
public class PlaybackParameters implements Cloneable, Serializable {

	static public final long serialVersionUID = 0;
	
	/**
	 * source or raw audio data. 
	 */
	public int dataSource;
	
	/**
	 * channels to play back
	 */
	public int channelBitmap;
	
	/**
	 * number of sound card line - only used when playing back wav files, etc. Otherwise, 
	 * sound playback will be through the device that is acquiring data. 
	 */
	public int deviceNumber = 0;
	
	/**
	 * Device type only used with file playback, since for all real time 
	 * plyback, playback must be through the device aquiring data
	 */
	public int deviceType = 0;
	
	public boolean defaultSampleRate = true;
	
	public float playbackRate;

	@Override
	public PlaybackParameters clone() {

		try {
			return (PlaybackParameters) super.clone();
		}
		catch (CloneNotSupportedException ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
}

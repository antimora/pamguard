package soundPlayback;

import java.awt.Component;

/**
 * Class for playback systems to add a system specific dialog component
 * to the sound playback dialog. 
 * <p>
 * Generally, playback of incoming audio data must go out through the same device that 
 * it came in on so that the sound devices clock is correctly synchronised. 
 * So if you're acquiring data from a sound card, the output must be through the sound card, if
 * you're acquiring data through an NI board, the output must be through the same NI board. 
 * <p>
 * The class allows you to add a specific dialog component to the general playback dialog. 
 *  
 * @author Doug Gillespie
 * @see PlaybackSystem
 *
 */
public abstract class PlaybackDialogComponent  {

	/**
	 * Get the graphics component to be included in the playback dialog 
	 * @return
	 */
	abstract Component getComponent();
	
	/**
	 * Set the parameters in the dialog component
	 * @param playbackParameters
	 */
	abstract void setParams(PlaybackParameters playbackParameters);
	
	/**
	 * Get teh parameters from the dialog component
	 * @param playbackParameters
	 * @return PlaybackParameters or null.
	 * @see PlaybackParameters
	 */
	abstract PlaybackParameters getParams(PlaybackParameters playbackParameters);
	
}

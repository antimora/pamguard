package PamUtils;

public class PamAudioFileFilter extends PamFileFilter {

	public PamAudioFileFilter() {
		super("Audio Files", ".wav");
		addFileType(".WAV");
		addFileType(".aif");
		addFileType(".aiff");
		addFileType(".AIF");
		addFileType(".AIFF");
	}

}

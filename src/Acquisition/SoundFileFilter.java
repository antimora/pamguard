package Acquisition;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * File filter for selecting some standard sound file types
 * 
 */
public class SoundFileFilter extends FileFilter {

	@Override
	public boolean accept(File f) {
		if (f.isDirectory())
			return true;

		String fname = f.getName();
		return (fname != null) && 
			(  fname.endsWith(".wav")
			|| fname.endsWith(".WAV")
			|| fname.endsWith(".aif")
			//add more lines here as more file formats are added
			);
	}

	// The description of this filter
	@Override
	public String getDescription() {
		return "Sound Files (*.wav; *.aif)";
	}
}


package Acquisition.pamAudio;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;


public class PamAudioSystem {

	private static final long largeFileSize = 01L<<31;
	
	public static AudioInputStream getAudioInputStream(File file) throws UnsupportedAudioFileException, IOException {
		if (file != null && isWavFile(file) && file.length() > largeFileSize) {
			return WavFileInputStream.openInputStream(file);
		}
		else {
			return AudioSystem.getAudioInputStream(file);
		}
	}

	private static boolean isWavFile(File file) {
		String name = file.getName();
		if (name.length() < 4) {
			return false;
		}
		String end = name.substring(name.length()-4).toLowerCase();
		return (end.equals(".wav"));
	}
	
}

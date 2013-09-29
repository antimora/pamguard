package Acquisition.offlineFuncs;

import java.io.File;

import dataMap.OfflineDataMapPoint;

public class WavFileDataMapPoint extends OfflineDataMapPoint {
	
	private File soundFile;

	public WavFileDataMapPoint(File soundFile, long startTime, long endTime) {
		super(startTime, endTime, (int) Math.max(((endTime-startTime)/1000),1));
		this.soundFile = soundFile;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return soundFile.getName();
	}

	/**
	 * @return the soundFile
	 */
	public File getSoundFile() {
		return soundFile;
	}

}

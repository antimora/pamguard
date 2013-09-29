package SoundRecorder;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import PamUtils.PamCalendar;

/**
 * Implementation of RecorderStorage specific to audio files.
 * 
 * @author Doug Gillespie
 *@see SoundRecorder.RecorderStorage
 */
public class PamAudioFileStorage  implements RecorderStorage {

	private String fileName = null;
	
	private RecorderControl recorderControl;
	
	private AudioFormat audioFormat;
	
	private AudioInputStream audioInputStream;
	
	private File audioFile;
		
	private PipedInputStream pipedInputStream;
	
	private PipedOutputStream pipedOutputStream;
	
	byte[] byteBuffer;
	
	AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
	
	long fileBytes;
	
	long fileStartMillis;
	
	long lastDataMillis;
	
	private Thread writeThread;
	
	long totalFrames;
	
	public PamAudioFileStorage(RecorderControl recorderControl) {
		this.recorderControl = recorderControl;
//		AudioFileFormat.Type[] audioTypes = AudioSystem.getAudioFileTypes();
//		for (int i = 0; i < audioTypes.length; i++) {
//			System.out.println(audioTypes[i]);
//		}
	}
	
	public String getFileName() {
		return fileName;
	}

	synchronized public boolean addData(long dataTimeMillis, double[][] newData) {
		if (newData == null || newData.length != audioFormat.getChannels()) return false;
		if (fileName == null) {
			return false;
		}
		int nFrames = newData[0].length;
		totalFrames += nFrames;
	
		int nChannels = newData.length;
		//byte[] byteData = new byte[nFrames * audioFormat.getFrameSize()];
		if (byteBuffer == null || byteBuffer.length != nFrames * audioFormat.getFrameSize()) {
			byteBuffer = new byte[nFrames * audioFormat.getFrameSize()];
		}
		int iByte = 0;
		short shortData;
		for (int iSample = 0; iSample < nFrames; iSample++) {
			for (int iChan = 0; iChan < nChannels; iChan++) {
				/*
				 * Data are prepared big byte first (bigEndian)
				 * The AudioWriter will swap this as necessary for
				 * different file formats.
				 * Eventually, this needs modifying to support 8,16 and 24 byte files. 
				 */
				shortData = (short) (newData[iChan][iSample] * 32768);
				byteBuffer[iByte] = (byte) (0xFF & (shortData >> 8));
				byteBuffer[iByte+1] = (byte) (0xFF & shortData);
				iByte += 2;
			}
		}
		try {
			pipedOutputStream.write(byteBuffer);
		}
		catch (IOException Ex) {
			Ex.printStackTrace();
			return false;
		}
		lastDataMillis = dataTimeMillis;
		fileBytes += byteBuffer.length;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see SoundRecorder.RecorderStorage#closeStorage()
	 */
	synchronized public boolean closeStorage() {
		/*
		 * The exact order things happen in here is quite important. No more samples will be added
		 * but it is likely that there are stil samples in the output pipe, so fluch the pipe to make sure they all 
		 * get written to the rile. Then wait for the pipe to be empty, then close the storage, then wait for the 
		 * writing thread to have exited before finally closing the output stream.  
		 */
		if (fileName == null) return false;
		fileName = null;
		try {
//			System.out.println("Available bytes in input stream: " + pipedInputStream.available() + " before flush");

			audioInputStream.close();
			pipedOutputStream.flush();
			pipedOutputStream.close();
			
			// then wait for the input stream to be empty - meaning that all data have been written to the file. 
//			System.out.println("Available bytes in input stream: " + pipedInputStream.available() + " after flush");
			while(pipedInputStream.available() > 0) {
				Thread.sleep(1);
//				System.out.println("Available bytes in input stream: " + pipedInputStream.available() + " wiat loop");
			}

			pipedInputStream.close();
//			// then wait for the flag saying that the write thread has exited to be 0
			while (writeThread.isAlive()) {
//				System.out.println("Wait for write thread to die");
				Thread.sleep(1);
			}
			pipedOutputStream.close();
			audioInputStream = null;
			
		}
		catch (Exception Ex) {
			Ex.printStackTrace();
			return false;
		}
		recorderControl.recorderProcess.storageClosed();
		return true;
	}
	
	float sampleRate;
	AudioFileFormat.Type audioFileType;
	int nChannels, bitDepth;

	/**
	 * Write data to an audio file. 
	 * <p>
	 * Writing audio data is relatively straight forward. The actual writing is
	 * done in a separate thread. That thread needs an InputStream to read data from.
	 * This is one end of a pair of PipedInput and PipedOutput Streams. This thread
	 * writes data into the other end of the pipe as it arrives. 
	 */
	synchronized public boolean openStorage(AudioFileFormat.Type fileType, long recordingStart, 
			float sampleRate, int nChannels, int bitDepth) {
		
		closeStorage();
		
		this.sampleRate = sampleRate;
		this.audioFileType = fileType;
		this.nChannels = nChannels;
		this.bitDepth = bitDepth;
		
		totalFrames = 0;
		
		lastDataMillis = fileStartMillis = recordingStart;
		
		fileBytes = 0;

		this.fileType = fileType;
		String fileExtension = "." + fileType.getExtension();
		fileName = PamCalendar.createFileName(recordingStart, recorderControl.recorderSettings.outputFolder, 
				recorderControl.recorderSettings.fileInitials+"_", fileExtension);
//		System.out.println(fileName);
		
		audioFile = new File(fileName);
		
		audioFormat = new AudioFormat(sampleRate, bitDepth, nChannels, true, true);
		byteBuffer = new byte[(int) sampleRate * audioFormat.getFrameSize()];
		try {
			pipedInputStream = new PipedInputStream();
			pipedOutputStream = new PipedOutputStream(pipedInputStream);
		}
		catch (IOException Ex) {
			Ex.printStackTrace();
			return false;
		}
		audioInputStream = new AudioInputStream(pipedInputStream, audioFormat, AudioSystem.NOT_SPECIFIED);
		
		writeThread = new Thread(new WriteThread());
		writeThread.start();
//		writeData();
		
		return true;
	}
	
	@Override
	synchronized public boolean reOpenStorage(long recordingStart) {
		/*
		 * Make sure it's not currently writing a thread.
		 * 
		 */
		closeStorage();
		return openStorage(audioFileType, recordingStart, sampleRate, nChannels, bitDepth);
	}

	/**
	 * WriteThread makes a single call to AudioSystem.write. This 
	 * function blocks and only returns when the audioInputStream is 
	 * closed. 
	 * @author Doug Gillespie
	 *
	 */
	class WriteThread implements Runnable {
	
		public void run() {
			writeData();
		}
	}
	
	/**
	 * Called within the write thread, this does not return
	 * until the pipes get closed. 
	 */
	private void writeData() {
//		System.out.println("Enter write Data");
		try
		{
			long bytesWritten = AudioSystem.write(audioInputStream,	fileType, audioFile);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
//		System.out.println("Leave write Data");
	}

	public long getFileBytes() {
		if (fileName == null) return -1;
		return fileBytes;
	}

	public long getFileMilliSeconds() {
		if (fileName == null) return -1;
		return (lastDataMillis - fileStartMillis);
	}

}

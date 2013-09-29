package wavFiles;

import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;

import clickDetector.WindowsFile;
import java.io.ByteArrayInputStream;
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public class WavFile {

	private File file;

	private String fileName;

	private String fileMode;

	private WindowsFile windowsFile;

	private WavHeader wavHeader;

	/**
	 * Open a wav file for reading or writing. 
	 * <p>
	 * @param fileName file name with full path
	 * @param fileMode mode = "r" for read or "w" for write
	 */
	public WavFile(String fileName, String fileMode) {
		this.fileName = new String(fileName);
		this.fileMode = fileMode;
		if (fileMode.equalsIgnoreCase("r")) {
			openForReading();
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		close();
	}

	private boolean openForReading() {
		file = new File(fileName);
		if (file.exists() == false) {
			return reportError(String.format("file %s does not exist on the system", fileName));
		}
		try {
			windowsFile = new WindowsFile(file, fileMode);
		} catch (IOException e) {
			e.printStackTrace();
			return reportError(String.format("Unable to open file %s", fileName));
		}

		readWavHeader();
		return (wavHeader != null && wavHeader.isHeaderOk());
	}

	public WavHeader readWavHeader() {
		wavHeader = new WavHeader();
		if (wavHeader.readHeader(windowsFile)) {
			return wavHeader;
		}
		else {
			return null;
		}
	}

	/**
	 * @return the wavHeader
	 */
	public WavHeader getWavHeader() {
		return wavHeader;
	}

	public boolean positionAtData() {
		if (wavHeader == null) {
			readWavHeader();
		}
		if (wavHeader.isHeaderOk()) {
			try {
				windowsFile.seek(wavHeader.getDataStart());
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
		return false;
	}

	private boolean reportError(String warningText) {
		JOptionPane.showMessageDialog(null, warningText, "Wav file", JOptionPane.ERROR_MESSAGE);
		return false;
	}

	public void close() {
		if (windowsFile != null) {
			try {
				windowsFile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			windowsFile = null;
		}
	}

	private byte[] tempByteArray;

	private byte[] outputData;

	private AudioFormat currentFormat;
	/**
	 * Read a number of bytes from the wav file. 
	 * 
	 * @param byteArray byte array preallocated to desired length
	 * @return number of bytes actually read. 
	 */
	private int readData(byte[] byteArray) {
		int bytesRead = 0;
		try {
			bytesRead = windowsFile.read(byteArray);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bytesRead;
	}

	/**
	 * Read data into a preallocated double array. 
	 * The array should be 2 D, with the first dim being
	 * the number of channels, the second the number of samples to read. 
	 * @param doubleArray double array to receive data
	 * @return number of samples read (should be doubleArray[0].length if no EOF) 
	 */
	public int readData(double[][] doubleArray) {
		int bytes = wavHeader.getBitsPerSample()/8*wavHeader.getNChannels()*doubleArray[0].length;
		if (tempByteArray == null || tempByteArray.length != bytes) {
			tempByteArray = new byte[bytes];
		}
		int bytesRead = readData(tempByteArray);
		int samplesRead = bytesRead/wavHeader.getBlockAlign();
		switch(wavHeader.getBitsPerSample()) {
		case 16:
			unpackInt16(doubleArray, tempByteArray, samplesRead);
			break;
		case 24:
			unpackInt24(doubleArray, tempByteArray, samplesRead);
			break;
		case 32:
			break;
		}
		return samplesRead;
	}

	/**
	 * Unpack an array of 16 bit integer data in little endian format. 
	 * @param doubleArray
	 * @param tempByteArray
	 * @param samplesRead
	 */
	private void unpackInt16(double[][] doubleArray, byte[] tempByteArray,
			int samplesRead) {
		int nChan = wavHeader.getNChannels();
		int bytePointer = 0;
		double max = 1<<15;
		// the cast to short forces the first bit to become the sign again prior to
		// conversion to double
		for (int iChan = 0; iChan < nChan; iChan++) {
			for (int iSamp = 0; iSamp < samplesRead; iSamp++) {
				doubleArray[iChan][iSamp] = (short)((tempByteArray[bytePointer+1]&0xFF)<<8 | 
						(tempByteArray[bytePointer]&0xFF))/max;
				bytePointer += 2;
			}
		}
	}
	/**
	 * Unpack a byte array of data in little endian format. 
	 * @param doubleArray
	 * @param tempByteArray
	 * @param samplesRead
	 */
	private void unpackInt24(double[][] doubleArray, byte[] tempByteArray,
			int samplesRead) {
		int nChan = wavHeader.getNChannels();
		int bytePointer = 0;
		double max = 1<<31;
		// need to boost data up to int32 so that the sign bit gets into the right place. 
		for (int iChan = 0; iChan < nChan; iChan++) {
			for (int iSamp = 0; iSamp < samplesRead; iSamp++) {
				doubleArray[iChan][iSamp] = ((tempByteArray[bytePointer+2]&0xFF)<<24 |
						(tempByteArray[bytePointer+1]&0xFF)<<16 + 
						(tempByteArray[bytePointer]&0xFF)<<8)/max;
				bytePointer += 3;
			}
		}
	}

	/**
	 * Writes an array of double values to a WAV file.  This method only writes
	 * single channel data.
	 *
	 * @param format {@link AudioFormat} object describing the desired file
	 * @param doubleArray the array of double values to save
	 * @return boolean indicating success or failure of the write
	 */
	public boolean writeSingleChannel(AudioFormat format, double[] doubleArray) {

		/* convert the double array to a byte array */
		byte[] data = new byte[2 * doubleArray.length];
		for (int i = 0; i < doubleArray.length; i++) {
			int temp = (short) (doubleArray[i] * Short.MAX_VALUE);                  // DOES THIS ONLY WORK FOR 16BIT?
			data[2*i + 0] = (byte) temp;
			data[2*i + 1] = (byte) (temp >> 8);
		}

		/* try saving the file */
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			AudioInputStream ais = new AudioInputStream
			(bais, format, doubleArray.length);
			AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(fileName));
		}
		catch (Exception e) {
			System.out.println(e);
			return false;
		}
		return true;
	}

	/**
	 * Writes an array of double values to a WAV file.  This method writes
	 * single and multi-channel data.  This method always writes the data as
	 * 16-bit.
	 *
	 * @param sampleRate The sample rate of the raw acoustic data
	 * @param numChannels The number of channels to save
	 * @param doubleArray the array of double values to save.  Note that this is
	 * defined as a 2D array.  If a single-channel 1D vector is passed, it
	 * doesn't seem to be a problem.
	 * @return boolean indicating success or failure of the write
	 */
	public boolean write(
			float sampleRate,
			int numChannels,
			double[][] doubleArray) {

		/* create a new AudioFormat object describing the wav file */
		AudioFormat af = new AudioFormat(sampleRate,
				16,
				numChannels,
				true,
				false);     // figure out where bit rate is stored
		return write(af, doubleArray);
	}

	/**
	 * Writes an array of double values to a WAV file.  This method writes
	 * single and multi-channel data.
	 *
	 * This method writes the data properly as 16-bit.  The byte
	 * array conversion routine needs to be rewritten if a different bit rate is
	 * desired.
	 *
	 * @param format {@link AudioFormat} object describing the desired file
	 * @param doubleArray the array of double values to save.  Note that this is
	 * defined as a 2D array.  If a single-channel 1D vector is passed, it
	 * doesn't seem to be a problem.
	 * @return boolean indicating success or failure of the write
	 */
	public boolean write(AudioFormat format, double[][] doubleArray) {

		currentFormat = format;
		/* convert the double array to a byte array */
		outputData = new byte[(2 * doubleArray[0].length) *    // 2 * the number of points in each channel
		                      doubleArray.length];                            // times the number of channels
		int c = format.getChannels();
		for (int i = 0; i < doubleArray[0].length; i++) {
			for (int j=0; j<doubleArray.length; j++) {
				int temp = (short) (doubleArray[j][i] * Short.MAX_VALUE);                  // DOES THIS ONLY WORK FOR 16BIT?
				outputData[2*(c*i+j) + 0] = (byte) temp;
				outputData[2*(c*i+j) + 1] = (byte) (temp >> 8);
			}
		}

		/* try saving the file */
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(outputData);
			AudioInputStream ais = new AudioInputStream
			(bais, format, doubleArray[0].length);
			AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(fileName));
			ais.close();
		}
		catch (Exception e) {
			System.out.println(e);
			return false;
		}
		return true;
	}

	/**
	 * Append to an audio file. 
	 * <br>This implementation is pretty ugly since the current method of writing 
	 * seems to only send out a single block, so basically it's rewriting the same 
	 * data multiple times. 
	 * @param moreData data to add to the file. 
	 * @return true if sucessful, false if wrong format or other problem. 
	 */
	public boolean append(double[][] doubleArray) {        

		int xtraLength = (2 * doubleArray[0].length) *  doubleArray.length;
		int ol = outputData.length;
		outputData = Arrays.copyOf(outputData, ol+xtraLength);
		int c = currentFormat.getChannels();
		if (c != doubleArray.length) {
			return false;
		}
		for (int i = 0; i < doubleArray[0].length; i++) {
			for (int j=0; j<doubleArray.length; j++) {
				int temp = (short) (doubleArray[j][i] * Short.MAX_VALUE);                  // DOES THIS ONLY WORK FOR 16BIT?
				outputData[2*(c*i+j) + 0 + ol] = (byte) temp;
				outputData[2*(c*i+j) + 1 + ol] = (byte) (temp >> 8);
			}
		}

		int totalLength = outputData.length / currentFormat.getFrameSize();
		/* try saving the file */
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(outputData);
			AudioInputStream ais = new AudioInputStream
			(bais, currentFormat, totalLength);
			AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(fileName));
			ais.close();
		}
		catch (Exception e) {
			System.out.println(e);
			return false;
		}
		return true;
	}

}

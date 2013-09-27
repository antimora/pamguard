package wavFiles;

import java.io.IOException;
import java.util.ArrayList;

import clickDetector.WindowsFile;

public class WavHeader {

	private short fmtTag, nChannels = 0, blockAlign = 0, bitsPerSample = 0;
	
	private int sampleRate = 0, bytesPerSec = 0;
	
	private boolean headerOk = false;

	private long dataStart;
	
	private long dataSize;
	
	private ArrayList<WavHeadChunk> wavHeadChunks = new ArrayList<WavHeadChunk>();
	
	public boolean readHeader(WindowsFile windowsWavFile) {

		headerOk = false;
		if (windowsWavFile == null) {
			return false;
		}
		
		try {
			windowsWavFile.seek(0);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return false;
		}
		char riff[] = new char[4];
		long totalSize;
		char wave[] = new char[4];
		char dataHead[];
		char testChars[];
		String testString;
		long filePointer;
		int intDataSize;
		int fmtSize;
		long fmtEnd;
		int chunkSize = 0;
		byte[] headChunk;
		try {
			riff = read4Chars(windowsWavFile);
			totalSize = windowsWavFile.readWinInt();
			totalSize = checkUintProblem(totalSize);
			wave = read4Chars(windowsWavFile);
			while (true) {
				filePointer = windowsWavFile.getFilePointer();
				testChars = read4Chars(windowsWavFile);
				testString = new String(testChars);
				if (testString.equals("fmt ")) {
					break;
				}
				else {
					chunkSize = windowsWavFile.readWinInt();
				}
				headChunk = new byte[chunkSize];
				windowsWavFile.read(headChunk);
				wavHeadChunks.add(new WavHeadChunk(testString, headChunk));
//				windowsWavFile.seek(windowsWavFile.getFilePointer() + chunkSize);
			}
			// should now be at the start of the format section
			fmtSize = windowsWavFile.readWinInt();
			fmtEnd = windowsWavFile.getFilePointer() + fmtSize;
			fmtTag = (short) windowsWavFile.readWinShort();
			nChannels = (short) windowsWavFile.readWinShort();
			sampleRate = windowsWavFile.readWinInt();
			bytesPerSec = windowsWavFile.readWinInt();
			blockAlign = (short) windowsWavFile.readWinShort();
			bitsPerSample = (short) windowsWavFile.readWinShort();
			windowsWavFile.seek(fmtEnd);
			dataHead = read4Chars(windowsWavFile);
			dataSize = windowsWavFile.readWinInt();
			dataSize = checkUintProblem(dataSize);
			dataStart = windowsWavFile.getFilePointer();
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		headerOk = true;
		return true;
	}
	
	private long checkUintProblem(long totalSize) {
		if (totalSize < 0) {
			totalSize += (1L<<32);
		}
		return totalSize;
	}

	private char[] read4Chars(WindowsFile wFile) throws IOException {
		char[] chars = new char[4];
		for (int i = 0; i < 4; i++) {
			chars[i] = (char) wFile.readByte();
		}
		return chars;
	}

	/**
	 * @return the fmtTag
	 */
	public short getFmtTag() {
		return fmtTag;
	}

	/**
	 * @return the nChannels
	 */
	public short getNChannels() {
		return nChannels;
	}

	/**
	 * @return the blockAlign
	 */
	public short getBlockAlign() {
		return blockAlign;
	}

	/**
	 * @return the bitsPerSample
	 */
	public short getBitsPerSample() {
		return bitsPerSample;
	}

	/**
	 * @return the sampleRate
	 */
	public int getSampleRate() {
		return sampleRate;
	}

	/**
	 * @return the bytesPerSec
	 */
	public int getBytesPerSec() {
		return bytesPerSec;
	}

	/**
	 * @return the headerOk
	 */
	public boolean isHeaderOk() {
		return headerOk;
	}

	/**
	 * 
	 * @return byte number for the start of the data. 
	 */
	public long getDataStart() {
		return dataStart;
	}

	/**
	 * @return the dataSize
	 */
	public long getDataSize() {
		return dataSize;
	}
	
	/**
	 * Get the number of additional chunks in the wav header. 
	 * @return the number of additional chunks in the wav header. 
	 */
	public int getNumHeadChunks() {
		return wavHeadChunks.size();
	}
	
	/**
	 * Get a chunk from the wav header. 
	 * @param iChunk chunk number
	 * @return Chunk read from wav header. 
	 */
	public WavHeadChunk getHeadChunk(int iChunk) {
		return wavHeadChunks.get(iChunk);
	}
}

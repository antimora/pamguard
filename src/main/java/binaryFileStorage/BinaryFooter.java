package binaryFileStorage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

import PamUtils.PamCalendar;

public class BinaryFooter implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private long dataDate = PamCalendar.getTimeInMillis();
	
	private long analysisDate = System.currentTimeMillis();
	
	private long fileEndSample = 0;
	
	private int nObjects = 0;
	
	private long fileLength;
	
	private int fileEndReason = 0;

	public static final int END_UNKNOWN =  0;
	public static final int END_CRASHED =  1;
	public static final int END_RUNSTOPPED =  2;
	public static final int END_FILETOOBIG =  3;
	public static final int END_FILETOOLONG =  4;
	

	public BinaryFooter(long dataDate, long analysisDate, int objects, long fileLength) {
		super();
		this.dataDate = dataDate;
		this.analysisDate = analysisDate;
		this.nObjects = objects;
		this.fileLength = fileLength;
		nObjects = objects;
	}
	
	public BinaryFooter() {
		super();
	}

	public static int getStandardLength() {
		return 8*3+4*3;
	}
	
	public boolean writeFooter(DataOutputStream dos) {
		/**
		 * Was a bug in here up to 23/8/2010 whereby data only
		 * output a footer length of 8*3 + 4*3 bytes, so was a bit
		 * sort - so PAMGUARD would try to read beyond the end of
		 * it, which could cause all sorts of problems since the last 12 bytes 
		 * contain pretty random data (depending on file length). Usually
		 * got away with it since first four bytes would be zero unless file
		 * very long. 
		 */
		int footLength = 8*4 + 4*4;
		try {
			dos.writeInt(footLength);
			dos.writeInt(BinaryTypes.FILE_FOOTER);
			dos.writeInt(nObjects);
			dos.writeLong(dataDate);
			dos.writeLong(analysisDate);
			dos.writeLong(fileEndSample);
			dos.writeLong(fileLength);
			dos.writeInt(fileEndReason);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
	
	/**
	 * 
	 * @param dis
	 * @return
	 */
	public boolean readFooter(DataInputStream dis) {
		int footLen = 0;
		int footerId = 0;
		try {
			footLen = dis.readInt();
			footerId = dis.readInt();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return readFooterData(dis);
	}

	/**
	 * Reads footer data from after the footer id Int.
	 * @param fileStream
	 * @return true if read Ok. 
	 */
	public boolean readFooterData(DataInputStream dis) {
		try {
			nObjects = dis.readInt();
			dataDate = dis.readLong();
			analysisDate = dis.readLong();
			fileEndSample = dis.readLong();
			fileLength = dis.readLong();
			fileEndReason = dis.readInt();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * @return the dataDate
	 */
	public long getDataDate() {
		return dataDate;
	}

	/**
	 * @return the analysisDate
	 */
	public long getAnalysisDate() {
		return analysisDate;
	}

	public long getFileEndSample() {
		return fileEndSample;
	}

	public void setFileEndSample(long fileEndSample) {
		this.fileEndSample = fileEndSample;
	}

	/**
	 * @return the nObjects
	 */
	public int getNObjects() {
		return nObjects;
	}

	/**
	 * @return the fileLength
	 */
	public long getFileLength() {
		return fileLength;
	}

	public int getFileEndReason() {
		return fileEndReason;
	}

	public void setFileEndReason(int fileEndReason) {
		this.fileEndReason = fileEndReason;
	}

	/**
	 * @param dataDate the dataDate to set
	 */
	public void setDataDate(long dataDate) {
		this.dataDate = dataDate;
	}

	/**
	 * @param analysisDate the analysisDate to set
	 */
	public void setAnalysisDate(long analysisDate) {
		this.analysisDate = analysisDate;
	}

	/**
	 * @param nObjects the nObjects to set
	 */
	public void setnObjects(int nObjects) {
		this.nObjects = nObjects;
	}

	/**
	 * @param fileLength the fileLength to set
	 */
	public void setFileLength(long fileLength) {
		this.fileLength = fileLength;
	}
	
}

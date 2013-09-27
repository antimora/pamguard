package binaryFileStorage;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import PamguardMVC.PamDataBlock;

/**
 * Used to read data back from a binary data file and either send
 * it off to the associated PamDataBlock or hand it back to the 
 * BinaryStore controller, e.g. for writing into a new file if 
 * data are being updated. 
 * @author Doug Gillespie. 
 *
 */
public class BinaryInputStream {

	private PamDataBlock pamDataBlock;

	private BinaryStore binaryStore;

	private DataInputStream inputStream;

	//	private byte[] byteBuffer = null;

	private BinaryFooter binaryFooter = null;

	private int unitsRead = 0;

	private long lastObjectTime;

	private File currentFile;



	public BinaryInputStream(BinaryStore binaryStore, PamDataBlock pamDataBlock) {
		super();
		this.binaryStore = binaryStore;
		this.pamDataBlock = pamDataBlock;
	}


	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		closeFile();
	}

	/**
	 * Open an input file for reading. 
	 * @param inputFile file to open. 
	 * @return true is successful. 
	 */
	protected boolean openFile(File inputFile) {
		currentFile = inputFile;
		try {
			inputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(inputFile)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		unitsRead = 0;

		return true;
	}

	protected void closeFile() {
		if (inputStream != null) {
			try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			inputStream = null;
		}		
	}

	/**
	 * Reads the binary header from the file. 
	 * @return a binary header or null if the header could not be read
	 */
	protected BinaryHeader readHeader() {
		BinaryHeader bh = new BinaryHeader();
		if (bh.readHeader(inputStream)) {
			if (bh != null) {
				lastObjectTime = bh.getDataDate();
			}
			return bh;
		}
		return null;
	}

	/**
	 * Read the next object from the file. 
	 * <p>The object data will be packed up in a 
	 * BinaryObjectData which will have various fields
	 * filled in depending on the type of object.
	 * <p> null will be returned when there is an eof or 
	 * when the file footer has been identified. 
	 * <p>
	 * The calling function can check the file was complete by testing
	 * whether or not the file footer is null.  
	 * @return data object. 
	 */
	protected BinaryObjectData readNextObject() {
		int objectLength;
		int objectType;
		int binaryDataLength;
		byte[] byteBuffer = null;
		int buffLen = 0;
		long objectTime;
		int bytesRead;
		int moduleVersion;
		BinaryObjectData boData;

		try {
			objectLength = inputStream.readInt();
			objectType = inputStream.readInt();
			if (objectLength <= 0) {
				System.out.println("Error in file - objectlength < 0 !");
				return null;
			}
//			System.out.println(String.format("Read object type %d, length %d", objectType, objectLength));
			if (objectType == BinaryTypes.FILE_FOOTER) {
				if (objectLength == 36) {
					objectLength = 48;
				}
				binaryFooter = new BinaryFooter();
				binaryFooter.readFooterData(inputStream);
				return null;
			}
			else if (objectType == BinaryTypes.MODULE_HEADER) {
				moduleVersion = inputStream.readInt();
				if (objectLength >= 16) {
					buffLen = inputStream.readInt();
					byteBuffer = new byte[buffLen];
					bytesRead = inputStream.read(byteBuffer, 0, buffLen);
					if (bytesRead != buffLen) {
						return null;
					}
				}
				boData = new BinaryObjectData(objectType, byteBuffer, buffLen);
				boData.setVersionNumber(moduleVersion);
				return boData;
			}
			else if (objectType == BinaryTypes.MODULE_FOOTER) {
				buffLen = inputStream.readInt();
				byteBuffer = new byte[buffLen];
				bytesRead = inputStream.read(byteBuffer, 0, buffLen);
				if (bytesRead != buffLen) {
					return null;
				}
				boData = new BinaryObjectData(objectType, byteBuffer, buffLen);
				return boData;
			}
			else if (objectType == BinaryTypes.DATAGRAM) {
				buffLen = objectLength - 8;
				byteBuffer = new byte[buffLen];
				bytesRead = inputStream.read(byteBuffer, 0, buffLen);
				if (bytesRead != buffLen) {
					return null;
				}
				boData = new BinaryObjectData(objectType, byteBuffer, buffLen);
				return boData;
			}
			else{ // it's data - so get the extra timestamp
				objectTime = inputStream.readLong();
				binaryDataLength = inputStream.readInt();
				// this should be 20, but in some modules it's incorrectly set at 
				//12 !!!!!
				if (binaryDataLength + 12 == objectLength) {
//					System.out.println("Incorrect object data length in BinaryInputStream.readNextObject() " +
//							pamDataBlock.getDataName());
					binaryDataLength -= 8; // stoopid mess up with whether the time is included !
				}
				lastObjectTime = objectTime;
				//					objectTime += bh.getDataDate();
				//				buffLen = objectLength-16;
				//				if (byteBuffer == null || byteBuffer.length < buffLen) {
				if (binaryDataLength < 0) {
					System.out.println("Negative array size in BinaryInputStream.readNextObject() " +
							pamDataBlock.getDataName());
				}
				byteBuffer = new byte[binaryDataLength];
				//				}
				bytesRead = inputStream.read(byteBuffer, 0, binaryDataLength);
				if (bytesRead != binaryDataLength) {
					return null;
				}
				return new BinaryObjectData(objectType, objectTime, unitsRead++, byteBuffer, binaryDataLength);
			}
		} catch (IOException e) {
			if (currentFile != null) {
				System.out.println(String.format("Read error in file %s", currentFile.getName()));
			}
			System.out.println(e.getLocalizedMessage());
//			e.printStackTrace();
		}
		return null;
	}


	/**
	 * @return the binaryFooter
	 */
	public BinaryFooter getBinaryFooter() {
		return binaryFooter;
	}

	/**
	 * @return the unitsRead
	 */
	public int getUnitsRead() {
		return unitsRead;
	}


	/**
	 * @return the lastObjectTime
	 */
	public long getLastObjectTime() {
		return lastObjectTime;
	}
}

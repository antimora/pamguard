package binaryFileStorage;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import dataGram.Datagram;

import PamUtils.PamCalendar;
import PamguardMVC.PamDataBlock;

/**
 * Handles writing of an actual binary data file. 
 * <p>Is used during data analysis and also to rewrite
 * files if they are changed during offline analysis 
 * using the PAMGUARD viewer. 
 * @author Doug Gillespie
 * @see BinaryStore
 *
 */
public class BinaryOutputStream {

	private PamDataBlock parentDataBlock;

	private BinaryDataSource binaryDataSource;

	private BinaryStore binaryStore;

	private BinaryHeader header;

	private BinaryFooter footer;

	private byte[] moduleHeaderData;

	private byte[] moduleFooterData;

	private DataOutputStream dataOutputStream;

	private int storedObjects;

	private String mainFileName, indexFileName;

	public BinaryOutputStream(BinaryStore binaryStore,
			PamDataBlock parentDataBlock) {
		super();
		this.binaryStore = binaryStore;
		this.parentDataBlock = parentDataBlock;
		binaryDataSource = parentDataBlock.getBinaryDataSource();
		binaryDataSource.setBinaryStorageStream(this);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		closeFile();
		super.finalize();
	}

	protected synchronized void reOpen(long dataTime, long analTime, int endReason) {
		// finish off this file. 
		writeModuleFooter();
		writeFooter(dataTime, analTime, endReason);
		closeFile();
		// create the matching index file
		createIndexFile();
		
		// open the next file
		openFile(dataTime);
		writeHeader(dataTime, analTime);
		writeModuleHeader();
	}

	/**
	 * Open an output file. 
	 * <p> this call is used in real time ops to create a new file
	 * name based on the time and information from the datablock name.
	 * @param dataTime time in Java milliseconds 
	 * @return true if successful, false otherwise. 
	 */
	public synchronized boolean openFile(long dataTime) {

		createFileNames(dataTime);
		
		if (mainFileName == null) {
			System.out.println("Binary files not created - invalid folder ? " + binaryStore.binaryStoreSettings.getStoreLocation());
			return false;
		}
		
		File outputFile = new File(mainFileName);

		return openFile(outputFile);

	}

	/**
	 * Open an output file.
	 * <p>
	 * This version is used when rewriting files when data have 
	 * been changed offline. Generally the file will be a .tmp file 
	 * @param outputFile output file
	 * @return true if successful
	 */
	public boolean openFile(File outputFile) {
		header = new BinaryHeader(binaryDataSource.getModuleType(), binaryDataSource.getModuleName(),
				binaryDataSource.getStreamName());
		//		header.setExtraInfo(binaryDataSource.getModuleHeader());
		storedObjects = 0;

		try {
			dataOutputStream = new DataOutputStream(new BufferedOutputStream(new 
					FileOutputStream(outputFile)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		binaryDataSource.newFileOpened(outputFile);

		return true;
	}

	/**
	 * Create names for the main storage file and also for the 
	 * corresponding index file. 
	 * @param dataTime time in milliseconds. 
	 * @return true if folder is OK and file names have been created. 
	 */
	private boolean createFileNames(long dataTime) {

		String folderName = binaryStore.getFolderName(dataTime, true);

		if (folderName == null) {
			return false;
		}
		/**
		 * Don't check for spaces in the directory name since that will 
		 * have been set from the browser - so will be correct. 
		 * do however remove spaces from the rest of the file name. 
		 */
		String filePrefix = String.format("%s_%s_%s_", 
				binaryDataSource.getModuleType(), binaryDataSource.getModuleName(),
				binaryDataSource.getStreamName());
		mainFileName = folderName + fillSpaces(PamCalendar.createFileName(dataTime, 
				filePrefix, BinaryStore.fileType));
		indexFileName = folderName + fillSpaces(PamCalendar.createFileName(dataTime, 
				filePrefix, BinaryStore.indexFileType));
		return true;
	}

	/**
	 * Fill blank spaces in a string.
	 * @param str
	 * @return string with spaces replaced with the underscore character
	 */
	private String fillSpaces(String str) {
		return str.replaceAll(" ", "_");
	}

	public synchronized boolean closeFile() {
		if (dataOutputStream != null) {
			try {
				dataOutputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			dataOutputStream = null;
		}
		return true;
	}

	private long getSamplesFromMilliseconds(long timeMillis) {
		if (parentDataBlock == null) {
			return 0;
		}
		if (parentDataBlock.getParentProcess() == null) {
			return 0;
		}
		return parentDataBlock.getParentProcess().absMillisecondsToSamples(timeMillis);
	}

	public synchronized boolean writeHeader(long dataTime, long analTime) {
		if (dataOutputStream == null) {
			return false;
		}
		long sampleNumber = 0;
		if (parentDataBlock != null)
			header.setFileStartSample(getSamplesFromMilliseconds(dataTime));
		header.setDataDate(dataTime);
		header.setAnalysisDate(analTime);
		return header.writeHeader(dataOutputStream);
	}

	public synchronized boolean writeModuleHeader() {
		if (dataOutputStream == null) {
			return false;
		}
		byte[] moduleHeaderData = binaryDataSource.getModuleHeaderData();
		return writeModuleHeader(dataOutputStream, moduleHeaderData);
	}

	public synchronized boolean writeModuleHeader(byte[] headerData) { 
		return writeModuleHeader(dataOutputStream, headerData);
	}
	
	public synchronized boolean writeModuleHeader(DataOutputStream outputStream, byte[] headerData) { 
		moduleHeaderData = headerData;
		if (outputStream == null) {
			return false;
		}
		int objectLength = 16;
		int dataLength = 0;
		int moduleVersion = binaryDataSource.getModuleVersion();
		if (headerData != null) {
			dataLength = headerData.length;
			objectLength += dataLength;
		}

		try {
			outputStream.writeInt(objectLength);
			outputStream.writeInt(BinaryTypes.MODULE_HEADER);
			outputStream.writeInt(moduleVersion);
			outputStream.writeInt(dataLength);
			if (moduleHeaderData != null) {
				outputStream.write(headerData);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;

	}

	public synchronized boolean writeModuleFooter() {
		if (dataOutputStream == null) {
			return false;
		}
		moduleFooterData = binaryDataSource.getModuleFooterData();
		return writeModuleFooter(dataOutputStream, moduleFooterData);
	}
	
	/**
	 * Write a module footer to the output stream. 
	 * @param moduleFooter module footer
	 * @return true if successful
	 */
	public synchronized boolean writeModuleFooter(ModuleFooter moduleFooter) {
		byte[] footerData = moduleFooter.getByteArray();
		return writeModuleFooter(footerData);
	}

	/**
	 * Write module footer data to the output stream. Note that 
	 * this is the data alone, without the first 12 bytes of identifier and 
	 * length information. 
	 * @param footerData footer data as a binary array
	 * @return true if successful
	 */
	public synchronized boolean writeModuleFooter(byte[] footerData) {
		return writeModuleFooter(dataOutputStream, footerData);
	}
	
	/**
	 * Write module footer data to a specific output stream. 
	 * @param outputStream output stream
	 * @param footerData footer data in a binary array
	 * @return true if sucessful. 
	 */
	public synchronized boolean writeModuleFooter(DataOutputStream outputStream, byte[] footerData) {
		moduleFooterData = footerData; // will need this when index file is written !
		if (outputStream == null) {
			return false;
		}
		int objectLength = 12;
		int dataLength = 0;
		if (footerData != null) {
			dataLength = footerData.length;
			objectLength += dataLength;
		}

		try {
			outputStream.writeInt(objectLength);
			outputStream.writeInt(BinaryTypes.MODULE_FOOTER);
			outputStream.writeInt(dataLength);
			if (footerData != null) {
				outputStream.write(footerData);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public synchronized boolean writeFooter(long dataTime, long analTime, int endReason) {
		long fileLen = 0;
		if (dataOutputStream == null) {
			return false;
		}
		fileLen = dataOutputStream.size();
		footer = new BinaryFooter(dataTime, analTime,
				storedObjects, fileLen);
		footer.setFileEndSample(getSamplesFromMilliseconds(dataTime));
		footer.setFileEndReason(endReason);
		return footer.writeFooter(dataOutputStream);
	}
	
	public synchronized long getFileSize() {
		if (dataOutputStream == null) {
			return -1;
		}
		return dataOutputStream.size();
	}

	/**
	 * Write data to a file
	 * @param objectId unique object identifier.
	 * @param timeMillis time of object in java milliseconds
	 * @param data byte array of binary data to store. 
	 * @return true if written successfully. 
	 */
	public synchronized boolean storeData(int objectId, long timeMillis, byte[] data) {
		return storeData(objectId, timeMillis, data, data.length);
	}

	/**
	 * Write data to a file.  
	 * @param binaryObjectData data to store.  
	 * @return true if written successfully
	 */
	public boolean storeData(BinaryObjectData binaryObjectData) {
		return storeData(binaryObjectData.getObjectType(), binaryObjectData.getTimeMillis(),
				binaryObjectData.getData(), binaryObjectData.getDataLength());
	}

	/**
	 * Writes data to a file. Note that the length of data may be greater than
	 * the actual length of useful data which is held in onjectLength 
	 * @param objectId unique object identifier.
	 * @param timeMillis time of object in java milliseconds
	 * @param data byte array of binary data to store. 
	 * @param objectLength length of useful data in data (often = data.length) 
	 * @return true if written successfully.
	 */
	public synchronized boolean storeData(int objectId, long timeMillis, byte[] data, int objectLength) {
		int lengthInFile = objectLength + 20;
		/*
		 *  extra space for length in file       (4)
		 *                  object identifier    (4)
		 *                  object binary length (4)
		 *                  timestamp            (8) total = 20 bytes.   
		 */
		int dataLength = objectLength + 8; // don't add the additional timestamp length

		if (dataOutputStream == null) {
			return false;
		}

		int newLength = dataOutputStream.size() + lengthInFile + BinaryFooter.getStandardLength();

		if (binaryStore.binaryStoreSettings.limitFileSize && 
				newLength > binaryStore.binaryStoreSettings.getMaxSizeMegas()) {
			reOpen(PamCalendar.getTimeInMillis(), System.currentTimeMillis(), BinaryFooter.END_FILETOOBIG);
		}

		try {
			dataOutputStream.writeInt(lengthInFile);
			dataOutputStream.writeInt(objectId);
			dataOutputStream.writeLong(timeMillis);
			dataOutputStream.writeInt(dataLength);
			dataOutputStream.write(data, 0, objectLength);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		storedObjects++;


		return true;
	}

	public boolean createIndexFile() {
		if (indexFileName != null) {
			return createIndexFile(new File(indexFileName));
		}
		return false;
	}

	public boolean createIndexFile(File indexFile){
		DataOutputStream iStream;
		try {
			iStream = new DataOutputStream(new BufferedOutputStream(new 
					FileOutputStream(indexFile)));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

		header.writeHeader(iStream);
		writeModuleHeader(iStream, moduleHeaderData);
		writeModuleFooter(iStream, moduleFooterData);
		footer.writeFooter(iStream);

		try {
			iStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return true;
	}

	/**
	 * Write a Datagram to an output stream
	 * @param datagram
	 * @return true if successful. 
	 * @see Datagram
	 */
	public boolean writeDatagram(Datagram datagram) {
		if (dataOutputStream == null || datagram == null) {
			return false;
		}
		return datagram.writeDatagram(dataOutputStream);
	}

	/**
	 * Write a file footer to the binary output stream. 
	 * @param footer file footer. 
	 * @return true if successful.
	 */
	public boolean writeFileFooter(BinaryFooter footer) {
		if (dataOutputStream == null || footer == null) {
			return false;
		}
		return footer.writeFooter(dataOutputStream);
	}

	/**
	 * @return the mainFileName
	 */
	public String getMainFileName() {
		if (mainFileName == null) {
			return null;
		}
		File aFile = new File(mainFileName);
		return aFile.getName();
	}
}

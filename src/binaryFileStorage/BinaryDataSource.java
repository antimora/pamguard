package binaryFileStorage;

import java.io.File;

import dataMap.DataMapDrawing;

import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;

/**
 * Not just a source, but also a sink for binary data from the
 * binary store. Every BinaryDataSource is tied
 * to a single PamDataBlock
 * @author Doug Gillespie
 * @see PamDataBlock
 *
 */
public abstract class BinaryDataSource {

	private BinaryOutputStream binaryStorageStream;
	
	private PamDataBlock sisterDataBlock;
	
	private boolean doBinaryStore = true;
	
	private boolean storeData = true;
	
	/**
	 * Create a binary data source. These are used both to store data in binary 
	 * files, and possibly also to send data to other PAMguard instances over the network. 
	 * @param sisterDataBlock dataBlock for data to store / send
	 */
	public BinaryDataSource(PamDataBlock sisterDataBlock) {
		super();
		this.sisterDataBlock = sisterDataBlock;
	}

	/**
	 * Create a binary data source. These are used both to store data in binary 
	 * files, and possibly also to send data to other PAMguard instances over the network. 
	 * @param sisterDataBlock dataBlock for data to store / send
	 * @param doBinaryStore true if data to be stored by default, false otherwise. 
	 */
	public BinaryDataSource(PamDataBlock sisterDataBlock, boolean doBinaryStore) {
		super();
		this.sisterDataBlock = sisterDataBlock;
		setDoBinaryStore(doBinaryStore);
	}
	
	/**
	 *
	 * @return Module type to be stored in the file header
	 */
	public String getModuleType() {
		return sisterDataBlock.getParentProcess().getPamControlledUnit().getUnitType();
	}
	
	/**
	 * 
	 * @return Module name to be stored in the file header
	 */
	public String getModuleName() {
		return sisterDataBlock.getParentProcess().getPamControlledUnit().getUnitName();
	}
	
	/**
	 * 
	 * @return Stream name to be stored in the file header
	 */
	public abstract String getStreamName();
	
	/**
	 * 
	 * @return Stream version name to be stored in the 
	 * Module Specific Control structure
	 */
	public abstract int getStreamVersion();

	/**
	 * Get a version number for the module. 
	 * <p>This is different to the version number in the main
	 * file header and allows individual modules to update their 
	 * format and maintain backwards compatibility with old data
	 * @return integer module version number
	 */
	public abstract int getModuleVersion();
	/**
	 * 
	 * @return Additional information (e.g. a control structure
	 * for a detector) to be stored in the 
	 * Module Specific Control structure
	 */
	public abstract byte[] getModuleHeaderData();
	
	/**
	 * @return data for the binary footer, or null. 
	 */
	public byte[] getModuleFooterData() {
		return null;
	}

	/**
	 * Convert data read back in in viewer mode into the correct
	 * type of PamDataUnit.
	 * <p><strong>DO NOT</strong> add this unit directly to the datablock, but pass
	 * it back to the calling process which will add it to the datablock
	 * if necessary. 
	 * @param binaryObjectData Binary data read back from a file. 
	 * @param bh binary header from start of file.
	 * @param moduleVersion 
	 * @return the PamDataUnit created from these data 
	 */
	public abstract PamDataUnit sinkData(BinaryObjectData binaryObjectData, BinaryHeader bh, int moduleVersion);

	/**
	 * Do something with module header information
	 * @param binaryObjectData data for the module header. 
	 * @param bh Binary header information
	 */
	public abstract ModuleHeader sinkModuleHeader(BinaryObjectData binaryObjectData, BinaryHeader bh);
	
	/**
	 * Do something with module footer information
	 * @param binaryObjectData data for the module header. 
	 * @param bh Binary header information
	 */
	public abstract ModuleFooter sinkModuleFooter(BinaryObjectData binaryObjectData, 
			BinaryHeader bh, ModuleHeader moduleHeader);
	
	/**
	 * @param binaryStorageStream the binaryStorageStream to set
	 */
	public void setBinaryStorageStream(BinaryOutputStream binaryStorageStream) {
		this.binaryStorageStream = binaryStorageStream;
	}

	/**
	 * @return the binaryStorageStream
	 */
	public BinaryOutputStream getBinaryStorageStream() {
		return binaryStorageStream;
	}

	/**
	 * Save data into the binary stream
	 * @param pamDataUnit
	 */
	public final boolean saveData(PamDataUnit pamDataUnit) {
		if (getBinaryStorageStream() == null) {
			return false;
		}
		if (storeData == false) {
			return false;
		}
		PackedBinaryObject data = getPackedData(pamDataUnit);
		if (data == null) {
			return false;
		}
		return getBinaryStorageStream().storeData(data.objectId, pamDataUnit.getTimeMilliseconds(), data.data);
	}

	/**
	 * Get packed binary data for either sending to file or over the network
	 * @param pamDataUnit data unit to pack
	 * @return packed binary data object
	 */
	abstract public PackedBinaryObject getPackedData(PamDataUnit pamDataUnit);
	
	/**
	 * 
	 * @param objectId
	 * @param timeMillis
	 * @param data
	 * @return
	 */
	public boolean storeData(int objectId, long timeMillis, byte[] data) {
		if (binaryStorageStream == null) {
			return false;
		}
		return binaryStorageStream.storeData(objectId, timeMillis, data);
	}

	public PamDataBlock getSisterDataBlock() {
		return sisterDataBlock;
	}

	/**
	 * Called from the BinaryOutputStream whenever a new output
	 * file is opened.  
	 * @param outputFile file information.
	 */
	public abstract void newFileOpened(File outputFile);
	

	/**
	 * REturn a class capable of overriding the normal drawing on 
	 * the data map 
	 * @return null if nothign exists. 
	 */
	public DataMapDrawing getSpecialDrawing() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @param doBinaryStore the doBinaryStore to set
	 * This flag tells the binary store that this unit is available for binary storage. It's
	 * used mostly in raw data blocks which use this same class to write to a network socket
	 * but have the ability to wrote to the binary store disabled. 
	 */
	public void setDoBinaryStore(boolean doBinaryStore) {
		this.doBinaryStore = doBinaryStore;
	}

	/**
	 * @return the doBinaryStore
	 */
	public boolean isDoBinaryStore() {
		return doBinaryStore;
	}

	/**
	 * Reset anything needing resetting in the binary data source. 
	 * This get's called just before PamStart(). 
	 */
	public void reset() {
	}

	/**
	 * Flag to say we want to actually store the data. 
	 * @return the storeData
	 */
	public boolean isStoreData() {
		return storeData;
	}

	/**
	 * Flag to say we want to actually store the data. 
	 * @param storeData the storeData to set
	 */
	public void setStoreData(boolean storeData) {
		this.storeData = storeData;
	}

	
}

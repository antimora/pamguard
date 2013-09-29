package binaryFileStorage;

import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;

import PamController.PamController;
import PamUtils.FileFinder;
import PamUtils.PamUtils;

import dataGram.Datagram;
import dataMap.OfflineDataMapPoint;


public class BinaryOfflineDataMapPoint extends OfflineDataMapPoint implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private BinaryHeader binaryHeader;
	private BinaryFooter binaryFooter;
	private ModuleHeader moduleHeader;
	private ModuleFooter moduleFooter;
	private Datagram datagram;
	
	//removed so can be just relative to data store location this means when whole folder copied or read on different platform it's still compatible
//	private File binaryFile;
	
	private URI relativeURI;
//	transient BinaryStore binaryStore;
	
	/*
	 * 
	 * relPathInsideBinStorage will store at pos0 the filename and the parent at pos1 etc etc.
	 * but will stop at the binary storage folder name and not store it.
	 * This is so we can store the relative path in the data map but it
	 * does not depend on either the absolute path which will cause problems
	 * when the folder is moved or a network drive is mapped to a location on
	 * a different computer and also so it does not depend on OS by having just
	 * a string which will contain the OS specific file separator.
	 * 
	 */
	

	
	public BinaryOfflineDataMapPoint(BinaryStore binaryStore, File file, BinaryHeader binaryHeader,
			BinaryFooter binaryFooter, ModuleHeader moduleHeader, ModuleFooter moduleFooter, Datagram datagram) {
		super(binaryHeader.getDataDate(), 
					binaryFooter != null ? binaryFooter.getDataDate() : -1, 
							binaryFooter != null ? binaryFooter.getNObjects() : -1);
		
//		this.binaryStore=binaryStore;
		String binaryStoreFolderLocation = binaryStore.binaryStoreSettings.getStoreLocation();
		URI binaryStoreFolderURI = new File(binaryStoreFolderLocation).toURI();
		URI binaryFileURI = file.toURI();
		
		
		this.relativeURI = binaryStoreFolderURI.relativize(binaryFileURI);
//		this.binaryFile = file; 
		this.binaryHeader = binaryHeader;
		this.binaryFooter = binaryFooter;
		this.moduleHeader = moduleHeader;
		this.moduleFooter = moduleFooter;
		this.datagram = datagram;
	}
	
	public BinaryOfflineDataMapPoint() {
		super(0,0,0);
	}
	
	public void update(BinaryStore binaryStore, File file, BinaryHeader binaryHeader,
			BinaryFooter binaryFooter, ModuleHeader moduleHeader, ModuleFooter moduleFooter, Datagram datagram) {
		
//		this.binaryStore=binaryStore;
		String binaryStoreFolderLocation = binaryStore.binaryStoreSettings.getStoreLocation();
		URI binaryStoreFolderURI = new File(binaryStoreFolderLocation).toURI();
		URI binaryFileURI = file.toURI();
		
		
		this.relativeURI = binaryStoreFolderURI.relativize(binaryFileURI);
//		this.binaryFile = file;
		this.binaryHeader = binaryHeader;
		this.binaryFooter = binaryFooter;
		this.moduleHeader = moduleHeader;
		this.moduleFooter = moduleFooter;
		this.datagram = datagram;
	}


	@Override
	public String getName() {
		if (relativeURI == null) {
			return "No file !";
		}
//		This apparently caused problems and can see that File(URI) should 
//		be absolute URI though this wasn't required at original change.
//		return new File(relativeURI).getName();
		
		
		String name = relativeURI.toString();
		if (name.lastIndexOf("/")!=-1){
			name = name.substring(name.lastIndexOf("/")+1);
		}
		
//		Shouldn't happen but should make caller throw null pointer exception
		if (name.length()==0) name =null;
		
		return name;
	}

	/**
	 * 
	 * @return the binary file header
	 */
	public BinaryHeader getBinaryHeader() {
		return binaryHeader;
	}
	
	/**
	 * 
	 * @return the binary file footer
	 */
	public BinaryFooter getBinaryFooter() {
		return binaryFooter;
	}
	
	/**
	 * @return the binaryFile
	 */
	public File getBinaryFile(BinaryStore binaryStore) {
		if (relativeURI == null) {
			return null;
		}
		return new File(new File(binaryStore.binaryStoreSettings.getStoreLocation()).toURI().resolve(relativeURI));
		
	}
	
//	/**
//	 * @param the The file where the binaryFile has been moved to
//	 */
//	public void setBinaryFile(BinaryStore binaryStore, File file) {
//		this.binaryFile=file;
//	}
	
//	/**
//	 * @return the binaryFile
//	 */
//	public File getBinaryFile() {
//		File dir;
//		
//		FileFinder.findFileName(initDir, binaryFile.getName())
//		return findFile(binaryFile.getName(),dir);
//		
//	}
	
	

	/**
	 * @return the moduleHeader
	 */
	public ModuleHeader getModuleHeader() {
		return moduleHeader;
	}

	/**
	 * @param moduleHeader the moduleHeader to set
	 */
	public void setModuleHeader(ModuleHeader moduleHeader) {
		this.moduleHeader = moduleHeader;
	}

	/**
	 * @return the moduleFooter
	 */
	public ModuleFooter getModuleFooter() {
		return moduleFooter;
	}

	/**
	 * @param moduleFooter the moduleFooter to set
	 */
	public void setModuleFooter(ModuleFooter moduleFooter) {
		this.moduleFooter = moduleFooter;
	}

	/**
	 * @return the datagram
	 */
	public Datagram getDatagram() {
		return datagram;
	}

	/**
	 * @param datagram the datagram to set
	 */
	public void setDatagram(Datagram datagram) {
		this.datagram = datagram;
	}

	
}

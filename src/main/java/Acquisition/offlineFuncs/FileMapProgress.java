package Acquisition.offlineFuncs;

public class FileMapProgress {
	
	public boolean countingFiles;
	
	public int totalFiles;
	
	public int openedFiles;
	
	public String fileName;

	public FileMapProgress(boolean countingFiles, int totalFiles,
			int openedFiles, String fileName) {
		super();
		this.countingFiles = countingFiles;
		this.totalFiles = totalFiles;
		this.openedFiles = openedFiles;
		this.fileName = fileName;
	}

}

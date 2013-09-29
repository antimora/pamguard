package generalDatabase;

import PamguardMVC.PamDataBlock;

/**
 *  simple class for passing information about data map making
 *  from swing worker to dialog. 
 */
public class CreateMapInfo {

	static public final int BLOCK_COUNT = 0;
	
	static public final int START_TABLE = 1;
	
	private int status;
	
	private int numBlocks;
	
	private String databaseName;
	
	private int tableNum;
	
	private String tableName;
	
	private PamDataBlock dataBlock;
	
	public CreateMapInfo(int numBlocks, String databaseName) {
		status = BLOCK_COUNT;
		this.numBlocks = numBlocks;
		this.databaseName = databaseName;
	}
	
	public CreateMapInfo(int tableNum, PamDataBlock dataBlock, String tableName) {
		status = START_TABLE;
		this.tableName = tableName;
		this.tableNum = tableNum;
		this.dataBlock = dataBlock;
	}

	/**
	 * @return the status
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * @return the numBlocks
	 */
	public int getNumBlocks() {
		return numBlocks;
	}

	/**
	 * @return the databaseName
	 */
	public String getDatabaseName() {
		return databaseName;
	}

	/**
	 * @return the tableNum
	 */
	public int getTableNum() {
		return tableNum;
	}

	/**
	 * @return the tableName
	 */
	public String getTableName() {
		return tableName;
	}

	/**
	 * @return the dataBlock
	 */
	public PamDataBlock getDataBlock() {
		return dataBlock;
	}
	
	
}

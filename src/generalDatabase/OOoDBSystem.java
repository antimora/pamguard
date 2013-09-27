package generalDatabase;

import generalDatabase.pamCursor.PamCursor;
import generalDatabase.pamCursor.ScrollablePamCursor;

import java.awt.Component;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.management.openmbean.OpenDataException;
import javax.swing.JFileChooser;

//import org.apache.tools.ant.types.FileSet;
//import org.apache.tools.zip.ZipUtil;

import com.sun.net.httpserver.Authenticator.Success;

import PamController.PamControlledUnitSettings;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamUtils.FolderZipper;
import PamUtils.PamFileFilter;
import PamUtils.PamUtils;
import PamUtils.ZipUtility;
import PamView.PamDialog;


/**
 * http://digiassn.blogspot.com/2006/07/java-creating-jdbc-connection-to.html
 * The unzipping/zipping part needs implemented.
 * 
 * @author gw
 *
 */
public class OOoDBSystem extends DBSystem implements PamSettings{
	
	transient ArrayList<File> recentDatabases;

	transient private DBControl dbControl;
	transient private OOoDialogPanel dialogPanel;
	transient private SQLTypes sqlTypes = new OOoDBSQLTypes();
	transient public static final String SYSTEMNAME ="Open Office Database";
	transient private final String driverClass = "org.hsqldb.jdbcDriver";
	transient private static String tempString = "temp"; 
	transient private static String fileExtension = "odb"; 
	
	transient private File openDatabaseFile;//the ODBFile
	
	transient private String openDatabaseDirName;// the UnZipped and rename file that make up the database inside
	
	
	public OOoDBSystem(DBControl dbControl, int settingsStore) {
		super();
		this.dbControl = dbControl;
//		openDatabaseDirName="C:\\Users\\gw\\Documents\\OOTest\\OOTest\\NewDatabase.odb";
		PamSettingManager.getInstance().registerSettings(this, settingsStore);
		if (recentDatabases == null) {
			recentDatabases = new ArrayList<File>();
		}
	}

	
	
	String browseDatabases(Component parent) {
		PamFileFilter fileFilter = new PamFileFilter("Open Database File", "."+fileExtension);
//		fileFilter.addFileType(".accdb");
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter(fileFilter);
		if (getDatabaseName() != null) {
			fileChooser.setSelectedFile(new File(getDatabaseName()));
		}
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		int state = fileChooser.showOpenDialog(parent);
		if (state == JFileChooser.APPROVE_OPTION) {
			File currFile = fileChooser.getSelectedFile();
			//System.out.println(currFile);
			openDatabaseFile=currFile;
			
			return currFile.getAbsolutePath();
			
		}
		return null;
	}

	@Override
	boolean canCreate() {
		
		return false;
	}

	@Override
	boolean create() {
		
		return false;
	}

	@Override
	public PamCursor createPamCursor(EmptyTableDefinition tableDefinition) {
		return new ScrollablePamCursor(tableDefinition);//check what I have done to scrollable cursor is ok.
	}

	@Override
	boolean exists() {
		openDatabaseFile = new File(getDatabaseName());
		if (openDatabaseFile == null) return false;
		return openDatabaseFile.exists();
	}
	
	
	@Override
	Connection getConnection() {
		if (openDatabaseFile==null)return null;
		
		//Example at http://programmaremobile.blogspot.com/2009/01/java-and-openoffice-base-db-through.html
		
		String unzipPath=unzip(openDatabaseFile);
		if(unzipPath==null||!(new File(unzipPath).exists())){
			System.out.println("Could not be unzipped");
			return null;
		}
		
//		check has data, if not make dir
		File dbDir=new File(unzipPath+File.separator+"database");
		if(!dbDir.exists()){
			dbDir.mkdir();
		}
		
//		rename files
		File[] filesFound = dbDir.listFiles();
		for (int i=0;i<filesFound.length;i++){
			if (filesFound[i].getName().indexOf(".")==-1){//no dot
				String renameName=filesFound[i].getParentFile().getPath()+File.separator+openDatabaseFile.getName().substring(0,openDatabaseFile.getName().length()-4)+"."+filesFound[i].getName();
				filesFound[i].renameTo(new File(renameName));
			}else{//has dot => was unable to rename on close
//				filesFound[i].delete();
			}
		}
		
//		set opened database data location
		openDatabaseDirName=unzipPath+File.separator+"database"+File.separator+openDatabaseFile.getName().substring(0,openDatabaseFile.getName().length()-4);
		
        try {
			Properties info = new Properties();
			info.setProperty("user", "sa");
			info.setProperty("password", "");
			System.out.println("ShutdownConnection?"+info.getProperty("shutdown"));
			info.setProperty("shutdown", "true");
			System.out.println("ShutdownConnection?"+info.getProperty("shutdown"));
			connection=DriverManager.getConnection("jdbc:hsqldb:file:" + openDatabaseDirName, info);
			connection.setAutoCommit(true);
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		} 
		return connection;
		
	}
	

	/**
	 * 
	 * @param dbName
	 * @return Absolute Path of the folder that the zip file has been extracted to.
	 */
	private synchronized String unzip(File odbFile) {
		
		File tempDir = getTempDir();
    	//File odbFile = openDatabaseFile;
    	
    	if (tempDir.exists()){
    		String msg = "PAMGuard did not close the database correctly last time it exited and will now correct and use the previously opened version.";
    		System.out.println(msg);
    		PamDialog.showWarning(dbControl.getGuiFrame(), "Repair Open Database", msg);
    	}else {
	    	try {
				ZipUtility.unzip(odbFile, tempDir);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
    	}
		return tempDir.getAbsolutePath();
	}
	
	public File getTempDir() {
		String path=openDatabaseFile.getAbsolutePath();
		return new File(path.substring(0,path.length()-4 )+tempString);
	}
	
	@Override
	synchronized void closeConnection() {
		
		System.out.println("CLOSING ODB CONNECTION!!!");
		if (connection != null) {
			try {
				//Close the connection!!
//				connection.commit();
				PreparedStatement stmt = connection.prepareStatement("SHUTDOWN");
				boolean shutdown = stmt.execute();
				System.out.println("shutdown:"+shutdown);
				connection.close();
				connection = null;
			}
			catch (SQLException ex) {
				ex.printStackTrace();
				return;
			}
			
			boolean renamedFiles = renameFiles();//rename files in database folder in temp directory
			
			//rezip files
			boolean zipped = false;
			if (renamedFiles) {
				zipped = reZip();
			}else{
				System.out.println("Could not rename internal database files");
			}
			boolean deletedTemp=false;
			if(zipped){
				ArrayList<File> filesLeft = new ArrayList<File>();
				deleteFolder(getTempDir(),filesLeft);//contents
				if(filesLeft.size()==0){
					getTempDir().delete();
					deletedTemp=true;
				}
			}else{
				System.out.println("Could not zip temp database folder");
			}
		}
	}
	
	/**
	 * called on close to rename files and return
	 * @return true if renamed all
	 */
	private boolean renameFiles() {
		//rename Data files
		
		File[] filesToUnPrefix = new File(openDatabaseDirName.substring(0, openDatabaseDirName.lastIndexOf(File.separator))).listFiles();
		
		boolean allOK=true;
		for (int i=0;i<filesToUnPrefix.length;i++){
			String name=filesToUnPrefix[i].getName();
			File newFile=new File(filesToUnPrefix[i].getParentFile().getPath()+File.separator+name.substring(name.lastIndexOf(".")+1, name.length()));
			
			System.out.println(filesToUnPrefix[i].getName()+"\t\tGOES TO \t"+newFile.getName());
			
			if (!filesToUnPrefix[i].renameTo(newFile)){
				allOK=false;
			}
		}
		return allOK;
	}
	
	synchronized boolean reZip(){
		
		File zip=new File(openDatabaseFile.getAbsolutePath()+"GOOD");
		File dir= getTempDir();
		
//		zip = new File("C:\\Users\\gw\\Desktop\\BlankDatabase.odb");
//		dir = new File(openDatabaseFile.getAbsolutePath().substring(0, openDatabaseFile.getAbsolutePath().length()-4)+tempString+"\\database");
		
		FolderZipper.zipFolder(dir.getAbsolutePath(), zip.getAbsolutePath(), false );
		
		File old = new File(openDatabaseFile.getAbsolutePath());
		dir.delete();
		if (old.delete()){
			System.out.println(old.getName()+":deleted");
			zip.renameTo(old);
			System.out.println(zip.getName()+" RENAMED TO "+old.getName());
		}else{
			PamDialog.showWarning(dbControl.getGuiFrame(), "Could not delete old "+getShortDatabaseName(), "Please make sure the database is not open in any other programs and then click \"OK\" to try again.");
			System.out.println("Could not delete old version of .odb file");
			if (old.delete()){
				System.out.println(old.getName()+":deleted");
				zip.renameTo(old);
				System.out.println(zip.getName()+" RENAMED TO "+old.getName());
				return true;
			}
			return false;
		}
		return true;
		
	}
	
	/**
	 * deletes folder hierarcy
	 * @param directory to be deleted
	 * @return 
	 * @return number of files unable to be deleted -1 if problem
	 */
	static void deleteFolder(File dir,ArrayList<File> filesLeft){
		int notDel=0;
		File[] files = dir.listFiles();
		for (File f:files){
			if (f.isDirectory()){
				deleteFolder(f,filesLeft);//contents
			}
			if(!f.delete()){
				filesLeft.add(f);
			}
			
		}
	}
	
	
	
	
/////////////////////////////
//
//    DBSYSTEM METHODS
//
//
	@Override
	public boolean hasDriver() {
		try {
			Class.forName(driverClass);
		} catch (ClassNotFoundException e) {
			return false;
		}
		return true;
	}
	
		

	@Override
	String getDatabaseName() {
		if (recentDatabases == null) return null;
		if (recentDatabases.size() < 1) return null;
		return recentDatabases.get(0).getAbsolutePath();
	}
	
	@Override
	public String getShortDatabaseName() {
		if (recentDatabases == null) return null;
		if (recentDatabases.size() < 1) return null;
		return recentDatabases.get(0).getName();
	}
	

	@Override
	SystemDialogPanel getDialogPanel(Component parent) {
		if (dialogPanel == null) {
			dialogPanel = new OOoDialogPanel(parent, this);
		}
		return dialogPanel;
	}

	@Override
	public	SQLTypes getSqlTypes() {
		return sqlTypes;
	}

	@Override
	public String getSystemName() {
		return "Open Office Database";
	}
	
	
	/**
	 * Useful OOo db locations/strings/functions
	 * @author GrahamWeatherup
	 *
	 */
	static class utils{
		/**
		 * 
		 * @param dbFileName
		 * @return dbFileName with extension removed
		 */
		private static String dbNameNoExt(String dbFileName){
			String dbFileNameNoExt;
			try{
				dbFileNameNoExt = dbFileName.substring(0, dbFileName.indexOf("."));
			}catch (IndexOutOfBoundsException e){
				dbFileNameNoExt = dbFileName;
			}
			return dbFileNameNoExt;
		}
		
		/**
		 * 
		 * @param ODBFile
		 * @return the tempFolder
		 */
		private static File getTempDir(File ODBFile) {
			String dbFileName=ODBFile.getAbsolutePath();
			return new File(dbNameNoExt(dbFileName)+tempString);
		}
		
		/**
		 * 
		 * @param ODBFile
		 * @return path to database used by hsqldb driver
		 */
		private static String getDBConnectionPath(File ODBFile){
			String fs=File.separator;
			return getTempDir(ODBFile).getAbsolutePath()
					+fs
					+"database"+fs+dbNameNoExt(ODBFile.getName());
		}
	}
	
	
	
	/////////////////////////////
	//
	//    PAM SETTINGS METHODS
	//
	//

	@Override
	public String getUnitName() {
		return dbControl.getUnitName();
	}

	@Override
	public String getUnitType() {
		return "OOo Database System";
	}

	@Override
	public Serializable getSettingsReference() {
		return recentDatabases;
	}

	@Override
	public long getSettingsVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		recentDatabases = (ArrayList<File>) (pamControlledUnitSettings.getSettings());
		if (recentDatabases.size() > 0) {
			// will crash on first use without the if !
			openDatabaseFile = recentDatabases.get(0);
		}
		return true;
	}

}

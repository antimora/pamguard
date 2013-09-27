package generalDatabase;

import java.io.Serializable;

public class DBParameters implements Cloneable, Serializable {
	
  public static final long serialVersionUID = 0;
  
  int databaseSystem = 0;
  
//  String databaseName = "C:\\Pamguard\\TestDB.mdb";
//  
//  String userName = "";
//  
//  String passWord = "";
  
  @Override
  public DBParameters clone() {
	  try {
		  return (DBParameters) super.clone();
	  }
	  catch (CloneNotSupportedException ex) {
		  ex.printStackTrace();
		  return null;
	  }
  }
  
}

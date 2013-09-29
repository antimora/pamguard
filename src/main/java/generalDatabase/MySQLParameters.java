package generalDatabase;

import java.io.Serializable;

public class MySQLParameters implements Cloneable, Serializable {

	static public final int serialVersionUID = 1;
	
	String databaseName;
	
	String ipAddress = "localhost";
	
	int portNumber = 3306;
	
	String userName = "root";
	
	String passWord = "";

	@Override
	protected MySQLParameters clone() {

		try {
			return (MySQLParameters) super.clone();
		}
		catch (CloneNotSupportedException ex) {
			return null;
		}
	}
	
}

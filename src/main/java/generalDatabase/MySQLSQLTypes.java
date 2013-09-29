package generalDatabase;

import java.sql.Types;


public class MySQLSQLTypes extends SQLTypes {

	@Override
	public String typeToString(int sqlType, int length, boolean counter) {
		if (sqlType == Types.INTEGER && counter) {
			return "INTEGER NOT NULL AUTO_INCREMENT";
		}
		return super.typeToString(sqlType, length, counter);
	}

}

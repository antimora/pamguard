package generalDatabase;

import java.sql.Connection;

import PamguardMVC.PamDataUnit;


/**
 * simple abstract class for any extra database functions which 
 * do not easily work with PamDataUnits and PamDatablocks
 * 
 * @author Doug Gillespie
 *
 */
abstract class DbSpecial extends SQLLogging{

	DBControl dbControl;
	public DbSpecial(DBControl dbControl) {
		super(null);
		this.dbControl = dbControl;
	}

	public boolean logData(PamDataUnit dataUnit) {
		return super.logData(dbControl.getConnection(), dataUnit);
	}

	abstract void pamStart(Connection con);
	
	abstract void pamStop(Connection con);
	
	
}

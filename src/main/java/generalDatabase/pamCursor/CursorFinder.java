package generalDatabase.pamCursor;

import generalDatabase.DBControlUnit;
import generalDatabase.EmptyTableDefinition;

import java.sql.Connection;

/**
 * Class for managing PamCurosrs - returns the previous one
 * if at all possible, but creates a new one if the connection 
 * has changed. 
 * @author Doug Gillespie
 *
 */
public class CursorFinder {
	
	private PamCursor currentCursor = null;
	
	public CursorFinder() {
		
	}
	
	public PamCursor getCursor(Connection con, EmptyTableDefinition tableDefinition) {
		if (currentCursor == null ||currentCursor.getCurrentConnection() != con) {
			
			DBControlUnit dbc = DBControlUnit.findDatabaseControl();
			if (dbc == null) {
				currentCursor = null;
			}
			else {
				currentCursor = dbc.createPamCursor(tableDefinition);
			}
		}
		return currentCursor;
	}
}

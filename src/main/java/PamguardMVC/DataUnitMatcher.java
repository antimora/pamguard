package PamguardMVC;

/**
 * Class for matching data units to a set of other 
 * criteria. Is used by DataUnitFinder
 * @see DataUnitFinder
 * @see DefaultUnitMatcher
 * @author Doug Gillespie
 *
 */
public interface DataUnitMatcher {

	boolean match(PamDataUnit dataUnit, Object... criteria);
	
}

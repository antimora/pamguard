package PamguardMVC;

/**
 * Default data unit matcher for use with 
 * DataUnitFinder
 * Matches clicks on time and possibly also channel number
 * depending on the number of arguments supplied.
 * @see DataUnitFinder 
 * @author Doug Gillespie
 *
 */
public class DefaultUnitMatcher implements DataUnitMatcher {

	@Override
	public boolean match(PamDataUnit dataUnit, Object... criteria) {
		if (dataUnit.getTimeMilliseconds() != (Long) criteria[0]) {
			return false;
		}
		if (criteria.length >= 2 &&
				dataUnit.channelBitmap != (Integer) criteria[1]) {
			return false;
		}
		return true;
	}

}

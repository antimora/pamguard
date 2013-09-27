package PamguardMVC;

/**
 * Adapter class for PamObserver so not necessary to implement
 * absolutely everything. 
 * @author Doug Gillespie
 *
 */
public abstract class PamObserverAdapter implements PamObserver {

	@Override
	public PamObserver getObserverObject() {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public long getRequiredDataHistory(PamObservable o, Object arg) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void masterClockUpdate(long milliSeconds, long sampleNumber) {
		// TODO Auto-generated method stub

	}

	@Override
	public void noteNewSettings() {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeObservable(PamObservable o) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSampleRate(float sampleRate, boolean notify) {
		// TODO Auto-generated method stub

	}

	@Override
	public void update(PamObservable o, PamDataUnit arg) {
		// TODO Auto-generated method stub

	}

}

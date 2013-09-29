package simulatedAcquisition;

import java.util.ArrayList;

/**
 * Class to list and manage different types of signals. 
 * @author Doug Gillespie
 * @see SimSignal
 *
 */
public class SimSignals  {

	private ArrayList<SimSignal> simSignalList = new ArrayList<SimSignal>();
	
	private SimProcess simProcess;

	public SimSignals(SimProcess simProcess) {
		super();
		this.simProcess = simProcess;
		simSignalList.add(new ImpulseSignal(simProcess.getSampleRate()));
		simSignalList.add(new ClickSound(simProcess.getSampleRate(), 130000, 0.1e-3));
		simSignalList.add(new ClickSound(simProcess.getSampleRate(), 5000, 0.5e-3));
		simSignalList.add(new ClickSound(simProcess.getSampleRate(), 3000, 2e-3));
		simSignalList.add(new LinearChirp(simProcess.getSampleRate(), 3000, 6000, 0.1));
		simSignalList.add(new LinearChirp(simProcess.getSampleRate(), 3000, 8000, .5));
		simSignalList.add(new RandomWhistles(simProcess.getSampleRate()));
		simSignalList.add(new BranchedChirp(simProcess.getSampleRate(), 5000, 12000, 8000, 3000, .6));
		simSignalList.add(new RightWhales(simProcess.getSampleRate()));
//		simSignalList.add(new BranchedChirp(simProcess.getSampleRate(), 3000, 8000, 12000, .3));
	}
	
	public int getNumSignals() {
		return simSignalList.size();
	}
	
	public SimSignal getSignal(int i) {
		return simSignalList.get(i);
	}
	
	public SimSignal findSignal(String name) {
		for (int i = 0; i < simSignalList.size(); i++) {
			if (simSignalList.get(i).getName().equals(name)) {
				return simSignalList.get(i);
			}
		}
		return simSignalList.get(0);
	}
	
	public SimSignal findSignal(Class signalClass) {
		for (int i = 0; i < simSignalList.size(); i++) {
			if (simSignalList.get(i).getClass() == signalClass) {
				return simSignalList.get(i);
			}
		}
		return null;
	}
	
	
}

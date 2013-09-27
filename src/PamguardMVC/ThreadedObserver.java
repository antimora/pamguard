package PamguardMVC;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import PamModel.PamModel;
import PamUtils.PamCalendar;

/**
 * This is a decorator class for PamObservers which intercepts any
 * data being sent to that observer and puts the data into a list
 * which will then be re-read in a separate thread, rather than
 * sending if for direct execution.
 * <p>
 * There is a bit of jiggledy piggledy to get the data history times
 * right since they may need to be extended slightly to allow for 
 * data that are not yet used. 
 *  
 * @author Doug Gillespie
 *
 */
public class ThreadedObserver implements PamObserver {

	private PamObserver singleThreadObserver;
	private PamObservable pamObservable;
	private List<PamDataUnit> dataUnitList;
	private NewObserverThread newObserverThread;
	private Thread observerThread;
	private volatile boolean killThread = false;
	private volatile boolean emptyRead = true;
	private int maxQueSize = 10;
	private int maxJitter, jitterSleep;

	public ThreadedObserver(PamObservable pamObservable,
			PamObserver singleThreadObserver) {
		super();
		this.pamObservable = pamObservable;
		this.singleThreadObserver = singleThreadObserver;
		this.maxJitter = PamModel.getPamModel().getPamModelSettings().getThreadingJitterMillis();
		jitterSleep = Math.max(1, maxJitter/10);
		dataUnitList = Collections.synchronizedList(new LinkedList<PamDataUnit>());
		observerThread = new Thread(newObserverThread = new NewObserverThread());
		observerThread.setPriority(Thread.MAX_PRIORITY);
		observerThread.start();
	}

	@Override
	public PamObserver getObserverObject() {
		return singleThreadObserver;
	}

	public PamObserver getSingleThreadObserver() {
		return singleThreadObserver;
	}

	@Override
	public String getObserverName() {
		if (canMultiThread()) {
			return singleThreadObserver.getObserverName() + " (MT)";
		}
		else {
			return singleThreadObserver.getObserverName();
		}
	}

	@Override
	public long getRequiredDataHistory(PamObservable o, Object arg) {
		/**
		 * if there is nothing in the list, then the observing thread
		 * is up to date, and can use the normal history time.
		 * <p>
		 * If there is data in the buffer, then the history has to
		 * go back by an amount equivalent to the delay from the
		 * first data to now. 
		 */
		long h = singleThreadObserver.getRequiredDataHistory(o, arg);
		synchronized (dataUnitList) { 
			if (dataUnitList.size() > 0) {
				/*
				 * There are unused data items in the list, so 
				 * get the time of the first one and allow 
				 * additional time for it. 
				 */
				long firstTime = dataUnitList.get(0).getTimeMilliseconds();
				long now = PamCalendar.getTimeInMillis();
				h += (now - firstTime);
			}
		}

		return h;
	}

	public int getInterThreadListSize() {
		return dataUnitList.size();
	}

	@Override
	public void noteNewSettings() {
		singleThreadObserver.noteNewSettings();
	}

	@Override
	public void removeObservable(PamObservable o) {
		killThread = true;
		singleThreadObserver.removeObservable(o);
	}

	@Override
	public void setSampleRate(float sampleRate, boolean notify) {
		singleThreadObserver.setSampleRate(sampleRate, notify);
	}

	@Override
	public void masterClockUpdate(long milliSeconds, long sampleNumber) {
		singleThreadObserver.masterClockUpdate(milliSeconds, sampleNumber);
	}

	@Override
	public void update(PamObservable o, PamDataUnit arg) {
		// not always possible to multi thread, so check. 
		if (canMultiThread()) {
			/**
			 * To stop data piling up in the buffer, we
			 * need to check that they are not getting too far
			 * behind. 
			 * If there is too much in the buffer, then wait until it
			 * has emptied somewhat. This means that if things get behind
			 * data will ultimately back up in the sound acquisition module
			 * which is equipped to handle such situations. 
			 */
			boolean needSleep = true;
			while(needSleep) {
				synchronized (dataUnitList) {
					int sz = dataUnitList.size();
					if (sz > 2) {
						long dt = dataUnitList.get(sz-1).timeMilliseconds - 
						dataUnitList.get(0).timeMilliseconds;
						if (dt < maxJitter) {
							needSleep = false;
						}
					}
					else {
						needSleep = false;
					}
				}
				if (needSleep) {
					try {
						//					int n1 = dataUnitList.size();
						Thread.sleep(jitterSleep);
						//					int n2 = dataUnitList.size();
						//					System.out.println("Quick kip reduces units from " + n1 + " to " + n2 + " in " + getObserverName());
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			//			while (dataUnitList.size() > maxQueSize) {
			//				try {
			//					Thread.sleep(0, 10);
			//					System.out.println("Stuck waiting to write in " + getObserverName());
			//				}
			//				catch(InterruptedException ex) {
			//					ex.printStackTrace();
			//				}
			//			}
			synchronized (dataUnitList) {
				dataUnitList.add(arg);
			}
		}
		else {
			singleThreadObserver.update(o, arg);
		}

	}

	/**
	 * Not possible to multi thread when data come from a sound file,
	 * do don't try. Can leave the objects wrapped up in the decorator
	 * but just pass the data on, rather than multi threading it. 
	 * @return
	 */
	private boolean canMultiThread() {
		return true;
		//		return (PamCalendar.isSoundFile() == false);
	}

	/**
	 * New observer thread. 
	 * <p>
	 * Very simply, the observable had put the new data into a list
	 * and this thread takes the data back out of the list and passes
	 * it on to the real observer. 
	 * 
	 * @author Doug
	 *
	 */
	class NewObserverThread implements Runnable {

		@Override
		public void run() {
			PamDataUnit du;
			while (!killThread) {
				if (dataUnitList.isEmpty()) {
					emptyRead = true;
					try {
						Thread.sleep(1);
						//						System.out.println("Stuck waiting to read in " + getObserverName());
					}
					catch(InterruptedException ex) {
						ex.printStackTrace();
					}
				}
				else {
					//					synchronized (dataUnitList){	
					while (dataUnitList.isEmpty() == false) {
						emptyRead = false;
						du = dataUnitList.remove(0);
						singleThreadObserver.update(pamObservable, du);
					}	
					//					}
				}
			}			
		}

	}

	public boolean isEmptyRead() {
		return emptyRead;
	}

}

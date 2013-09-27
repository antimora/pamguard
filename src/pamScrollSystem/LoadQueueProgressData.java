package pamScrollSystem;

public class LoadQueueProgressData {

	private int totalStreams;
	
	private int iStream;
	
	private String storeType;
	
	private String streamName;
	private long loadStart;
	private long loadEnd;
	private long loadCurrent;
	private int nLoaded;
	private int state;

	public LoadQueueProgressData(String storeType, String streamName,
			int totalStreams, int stream, int state, long loadStart, long loadEnd, long loadCurrent, int nLoaded) {
		super();
		this.storeType = storeType;
		this.streamName = streamName;
		this.totalStreams = totalStreams;
		this.iStream = stream;
		this.state = state;
		this.loadStart = loadStart;
		this.loadEnd = loadEnd;
		this.loadCurrent = loadCurrent;
		this.nLoaded = nLoaded;
	}

	/**
	 * @return the totalStreams
	 */
	public int getTotalStreams() {
		return totalStreams;
	}

	/**
	 * @return the iStream
	 */
	public int getIStream() {
		return iStream;
	}

	/**
	 * @return the storeType
	 */
	public String getStoreType() {
		return storeType;
	}

	/**
	 * @return the streamName
	 */
	public String getStreamName() {
		return streamName;
	}

	/**
	 * @return the iStream
	 */
	public int getiStream() {
		return iStream;
	}

	/**
	 * @return the loadStart
	 */
	public long getLoadStart() {
		return loadStart;
	}

	/**
	 * @return the loadEnd
	 */
	public long getLoadEnd() {
		return loadEnd;
	}

	/**
	 * @return the loadCurrent
	 */
	public long getLoadCurrent() {
		return loadCurrent;
	}

	/**
	 * @return the nLoaded
	 */
	public int getnLoaded() {
		return nLoaded;
	}

	/**
	 * @return the state
	 */
	public int getState() {
		return state;
	} 
	
	
}

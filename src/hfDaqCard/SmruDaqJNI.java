package hfDaqCard;

/**
 * Functions to interface with the SMRU Ltd DAQ card C code via JNI
 * @author Doug Gillespie
 *
 */
public class SmruDaqJNI {

	private static final String SILIB = "PamguardJNI";

	private static final String DEVNAME = "/dev/cypress_smru0";

	private SmruDaqSystem smruDaqSystem;

	private boolean haveLibrary;

	private boolean loadLibraryTried;

	native private int jniGetCardId(String cardName);

	native private int jniCloseCard(int device);
	
	native private int jniResetCard(int device);

	native private int jniSetLED(int device, int led, int state);

	native private int jniGetLED(int device, int led);

	native private int jniSetChannelMask(int device, int mask);

	native private int jniSetSampleRateIndex(int device, int sampleRateIndex);

	native private int jniPrepareChannel(int device, int channel, int gainIndex, int filterIndex);
	
	native private int jniStartSystem(int device);
	
	native private int jniStopSystem(int device);

	native private int jniSystemStopped(int device);

	native private int jniGetRunStatus(int device);
	
	native private short[] jniReadSamples(int device, int channel, int samples);

	private int cardId;

	public SmruDaqJNI(SmruDaqSystem smruDaqSystem) {
		super();
		this.smruDaqSystem = smruDaqSystem;
		loadLibrary();
		cardId = getCardId(DEVNAME);
		System.out.println(String.format("Card id = 0x%X", cardId));
	}

	@Override
	protected void finalize() throws Throwable {
		System.out.println("Fialise Daq JNI - close card");
		closeCard(cardId);
	}

	private void loadLibrary() {
		if (loadLibraryTried == false) {
			try  {
				System.loadLibrary(SILIB);
				haveLibrary = true;
			}
			catch (UnsatisfiedLinkError e)
			{
//				e.printStackTrace();
				System.out.println ("SMRU DAQ Card lib '" + SILIB + "' cannot be loaded for the following reason:");
//				+ System.getProperty ("java.library.path"));
				System.out.println("Unsatisfied Link Error : " + e.getMessage());
				haveLibrary = false;
			}
		}
		loadLibraryTried = true;
	}

	private boolean haveLibrary() {
		return haveLibrary;
	}

	public int toggleLED(int led) {
		return toggleLED(cardId, led);
	}

	public int toggleLED(int board, int led) {
		int state = getLED(board, led);
		System.out.println("state="+state);
		if (state == 0) {
			state = 1;
		}
		else {
			state = 0;
		}
		setLED(board, led, state);
		return getLED(board, led);
	}

	public int setLED(int board, int led, int state) {
		if (!haveLibrary()) {
			return 0;
		}
		return jniSetLED(board, led, state);
	}

	public int getLED(int board, int led) {
		if (!haveLibrary()) {
			return 0;
		}
		return jniGetLED(board, led);
	}

	public int getLED(int led) {
		return getLED(cardId, led);
	}

	public int getCardId(String cardName) {
		if (!haveLibrary()) {
			return Integer.MIN_VALUE;
		}
		return jniGetCardId(cardName);
	}

	public int closeCard(int device) {
		if (!haveLibrary()) {
			return -1;
		}
		return jniCloseCard(device);
	}
	
	public boolean resetCard() {
		if (!haveLibrary()) {
			return false;
		}
		return (jniResetCard(cardId)==0);
	}

	public boolean setSampleRateIndex(int sampleRateIndex) {
		if (!haveLibrary()) {
			return false;
		}
		return (jniSetSampleRateIndex(cardId, sampleRateIndex) == 0);
	}

	public boolean setChannelMask(int channelMask) {
		if (!haveLibrary()) {
			return false;
		}
		return (jniSetChannelMask(cardId, channelMask) == 0);
	}

	public boolean prepareChannel(int channel, int gainIndex, int filterIndex) {
		if (!haveLibrary()) {
			return false;
		}
		return (jniPrepareChannel(cardId, channel, gainIndex, filterIndex) == 0);
	}

	public boolean startSystem() {
		if (!haveLibrary()) {
			return false;
		}
		return (jniStartSystem(cardId) == 0);
	}

	public boolean stopSystem() {
		if (!haveLibrary()) {
			return false;
		}
		return (jniStopSystem(cardId) == 0);
	}

	public boolean systemStopped() {
		if (!haveLibrary()) {
			return false;
		}
		return (jniSystemStopped(cardId) == 0);
	}
	
	public short[] readSamples(int channel, int nSamples) {
		if (!haveLibrary()) {
			return null;
		}
		return jniReadSamples(cardId, channel, nSamples);
	}


}

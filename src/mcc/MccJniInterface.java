package mcc;

import java.util.ArrayList;

/**
 * Interface to Measurement Computing data acquisition boards. 
 * Interfaces to native source code (MS Windows only)
 *  
 * @author Douglas Gillespie
 *
 */
public class MccJniInterface {
	

	/* Selectable A/D Ranges codes */
	static public final int BIP20VOLTS      = 15;              /* -20 to +20 Volts */
	static public final int BIP10VOLTS      = 1 ;             /* -10 to +10 Volts */
	static public final int BIP5VOLTS       = 0 ;             /* -5 to +5 Volts */
	static public final int BIP4VOLTS       = 16;             /* -4 to + 4 Volts */
	static public final int BIP2PT5VOLTS    = 2 ;             /* -2.5 to +2.5 Volts */
	static public final int BIP2VOLTS       = 14;             /* -2.0 to +2.0 Volts */
	static public final int BIP1PT25VOLTS   = 3 ;             /* -1.25 to +1.25 Volts */
	static public final int BIP1VOLTS       = 4 ;             /* -1 to +1 Volts */
	static public final int BIPPT625VOLTS   = 5 ;             /* -.625 to +.625 Volts */
	static public final int BIPPT5VOLTS     = 6 ;             /* -.5 to +.5 Volts */
	static public final int BIPPT25VOLTS    = 12;              /* -0.25 to +0.25 Volts */
	static public final int BIPPT2VOLTS     = 13;              /* -0.2 to +0.2 Volts */
	static public final int BIPPT1VOLTS     = 7 ;             /* -.1 to +.1 Volts */
	static public final int BIPPT05VOLTS    = 8 ;             /* -.05 to +.05 Volts */
	static public final int BIPPT01VOLTS    = 9 ;             /* -.01 to +.01 Volts */
	static public final int BIPPT005VOLTS   = 10;             /* -.005 to +.005 Volts */
	static public final int BIP1PT67VOLTS   = 11;             /* -1.67 to +1.67 Volts */

	static public final int[] bipolarRanges = {BIP20VOLTS, BIP10VOLTS, BIP5VOLTS, BIP4VOLTS, 
		BIP2PT5VOLTS, BIP2VOLTS, BIP1PT25VOLTS, BIP1VOLTS, BIPPT625VOLTS, BIPPT5VOLTS, BIPPT25VOLTS,
		BIPPT2VOLTS, BIPPT1VOLTS, BIPPT05VOLTS, BIPPT01VOLTS, BIPPT005VOLTS, BIP1PT67VOLTS};
	
	/**
	 * Work out where in the list of bipolar ranges a particular range is. 
	 * Used for setting combo box list positions. 
	 * @param range bipolar MCC range
	 * @return index in bipolarRanges list. or -1 if range not found. 
	 */
	static public int getBipolarRangeIndex(int range) {
		for (int i = 0; i < bipolarRanges.length; i++) {
			if (bipolarRanges[i] == range) {
				return i;
			}
		}
		return -1;
	}
	static public String sayBibolarRange(int range) {
		switch (range) {
		case BIP20VOLTS      :       
			return "-20 to +20 Volts";
		case BIP10VOLTS      :         
			return "-10 to +10 Volts";
		case BIP5VOLTS       :          
			return "-5 to +5 Volts";
		case BIP4VOLTS       :          
			return "-4 to + 4 Volts";
		case BIP2PT5VOLTS    :           
			return "-2.5 to +2.5 Volts";
		case BIP2VOLTS       :          
			return "-2.0 to +2.0 Volts";
		case BIP1PT25VOLTS   :          
			return "-1.25 to +1.25 Volts";
		case BIP1VOLTS       :            
			return "-1 to +1 Volts";
		case BIPPT625VOLTS   :          
			return "-.625 to +.625 Volts";
		case BIPPT5VOLTS     :        
			return "-.5 to +.5 Volts";
		case BIPPT25VOLTS    :           
			return "-0.25 to +0.25 Volts";
		case BIPPT2VOLTS     :          
			return "-0.2 to +0.2 Volts";
		case BIPPT1VOLTS     :        
			return "-.1 to +.1 Volts";
		case BIPPT05VOLTS    :      
			return "-.05 to +.05 Volts";
		case BIPPT01VOLTS    :      
			return "-.01 to +.01 Volts";
		case BIPPT005VOLTS   :     
			return "-.005 to +.005 Volts";
		case BIP1PT67VOLTS   :      
			return "-1.67 to +1.67 Volts";
		default:
			return "Unknown range";
		}
	}

	static public final int UNI10VOLTS      = 100;            /* 0 to 10 Volts*/
	static public final int UNI5VOLTS       = 101;            /* 0 to 5 Volts */
	static public final int UNI4VOLTS       = 114;            /* 0 to 4 Volts */
	static public final int UNI2PT5VOLTS    = 102;            /* 0 to 2.5 Volts */
	static public final int UNI2VOLTS       = 103;            /* 0 to 2 Volts */
	static public final int UNI1PT67VOLTS   = 109;            /* 0 to 1.67 Volts */
	static public final int UNI1PT25VOLTS   = 104;            /* 0 to 1.25 Volts */
	static public final int UNI1VOLTS       = 105;            /* 0 to 1 Volt */
	static public final int UNIPT5VOLTS     = 110;            /* 0 to .5 Volt */
	static public final int UNIPT25VOLTS    = 111;            /* 0 to 0.25 Volt */
	static public final int UNIPT2VOLTS     = 112;            /* 0 to .2 Volt */
	static public final int UNIPT1VOLTS     = 106;            /* 0 to .1 Volt */
	static public final int UNIPT05VOLTS    = 113;            /* 0 to .05 Volt */
	static public final int UNIPT02VOLTS    = 108;            /* 0 to .02 Volt*/
	static public final int UNIPT01VOLTS    = 107;            /* 0 to .01 Volt*/

//	public static native boolean haveLibrary();
		
	private native String jniGetBoardName(int iBoard);
	
	private native double jniReadVoltage(int iBoard, int iChannel, int range);
	
	private native int jniGetLastErrorCode();
	
	private native String jniGetErrorString(int errorCode);

//	private static final String SILIB = "../MccJniBorland/mccjni";
	private static final String SILIB = "mccjniinterface";

	private static boolean loadTried = false;
	private static boolean loadOK = false;
	
	private static final int MAXMCCBOARDS = 10;
	
	public static final double MCCERRORVALUE = -99999; // matches a similar value in the C code
	
	private ArrayList<String> boardList;
	
	public MccJniInterface() {
		super();
		loadLibrary();
	}

	public static boolean loadLibrary() {

		// just try loading once. If it fails the first time, it will also fail on
		// subsequent calls. 
		if (loadTried == false) {
			try  {
				System.loadLibrary(SILIB);
				loadOK = true;
			}
			catch (UnsatisfiedLinkError e)
			{
				System.out.println ("native lib '" + SILIB + "' not found in 'java.library.path': "
						+ System.getProperty ("java.library.path"));
				loadOK = false;
			}
			loadTried = true;
		}
		
		return loadOK;
	}
	
	public int getNumBoards() {
		// have to do this my listing and seeing which are empty. 
		if (loadLibrary() == false) {
			return 0;
		}
		if (boardList == null) {
			createBoardList();
		}
		return boardList.size();
	}
	
	private void createBoardList() {
		boardList = new ArrayList<String>();
		String newName;
		for (int i = 0; i < MAXMCCBOARDS; i++) {
			newName = jniGetBoardName(i);
			if (newName == null || newName.length() == 0) {
				break;
			}
			boardList.add(newName);
		}
	}
	
	public String getBoardName(int iBoard) {
		if (loadLibrary() == false) {
			return null;
		}
		if (boardList == null) {
			createBoardList();
		}
		if (iBoard < boardList.size()) {
			return boardList.get(iBoard);
		}
		else {
			return null;
		}
	}
	
	public double readVoltage(int iBoard, int iChannel, int range) {
		if (loadLibrary() == false) {
			return 0;
		}
		double v = jniReadVoltage(iBoard, iChannel, range);
//		System.out.println(String.format("Board %d channel %d, range %d, voltage %f",
//				iBoard, iChannel, range, v));
		return v;
	}

	public int getLastErrorCode() {
		return jniGetLastErrorCode();
	}
	
	public String getErrorString(int errorCode) {
		return jniGetErrorString(errorCode);
	}
	
}

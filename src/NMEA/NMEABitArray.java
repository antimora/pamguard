package NMEA;

import com.sun.org.apache.xalan.internal.xsltc.dom.BitArray;

public class NMEABitArray extends BitArray {

	public static final long serialVersionUID = 0;

	public NMEABitArray() {
		super();
	}

	public NMEABitArray(int size, int[] bits) {
		super(size, bits);
	}

	public NMEABitArray(int size) {
		super(size);
	}

	/**
	 * Get an unsigned integer from the bit array. 
	 * <p>
	 * Integers can be any number of bits. 
	 * The first bit is the most significant.
	 * @param b1 First bit to unpack
	 * @param b2 Last bit to unpack
	 * @return unsigned integer value
	 */
	public int getUnsignedInteger(int b1, int b2) {
		int a = 0;
		int iB = 0;
		for (int i = b2; i >= b1; i--) {
			if (getBit(i)) {
				a += 1<<iB;
			}
			iB++;
		}
//		System.out.println(Integer.toBinaryString(a));
		return a;
	}
	/**
	 * Get a signed integer from the bit array. 
	 * <p>
	 * Integers can be any number of bits and are stored in 
	 * 2's compliment format.
	 * @param b1 First bit to unpack
	 * @param b2 Last bit to unpack
	 * @return signed integer value
	 */
	public int getSignedInteger(int b1, int b2) {
		int a = getUnsignedInteger(b1, b2);
		if (getBit(b1)) {
			a -=  1<<(b2-b1+1);
		}
		return a;
	}
	
	/**
	 * Lookup table to convert from integer to 6 bit ASCII data for
	 * AIS vessel names, destinations, etc. 
	 * Table 23 from page 55 of IEC 61993
	 */
	static public final char[] ASCII6 = {
		'@', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 
		'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 
		'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 
		'X', 'Y', 'Z', '[', '\\', ']', '^', '_', 
		' ', '!', '\"', '#', '$', '%', '&', '\'', 
		'(', ')', '*', '+', ',', '-', '.', '/', 
		'0', '1', '2', '3', '4', '5', '6', '7', 
		'8', '9', ':', ';', '<', '=', '>', '?'};
	
	/*
	 * changed from this on 19 June 2008. 
	 * Seems there was an error between '`' and '\''
	 * 
	static public final char[] ASCII6 = {
		'@', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 
		'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 
		'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 
		'X', 'Y', 'Z', '[', '\\', ']', '^', '_', 
		' ', '!', '\"', '#', '$', '%', '&', '`', 
		'(', ')', '*', '+', ',', '-', '.', '/', 
		'0', '1', '2', '3', '4', '5', '6', '7', 
		'8', '9', ':', ';', '<', '=', '>', '?'};
	 */

	/**
	 * Gets a string based on packed bits from an AIS string
	 * using the 6 bit ascii character set. 
	 * @param b1 First bit from AIS / NMEA data
	 * @param b2 Last bit from AIS / NMEA data
	 * @return character string
	 */
	public String getString(int b1, int b2) {
		// repacks the data as a six bit character string.
		// take six bits at a time and get theinteger, then look up the character
		String string = new String();
		int i1, i2;
		i1 = b1;
		i2 = i1 + 5;
		int n;
		char ch;
		while (i2 <= b2) {
			n = getUnsignedInteger(i1, i2);
			ch = ASCII6[n];
			string += ch;
			i1 = i2 + 1;
			i2 = i1 + 5;
		}
		
		return string;
	}

	static private byte[] lut6;
	/**
	 * converts a character from an AIS data string
	 * into a six bit integer value packed into an
	 * 8 bit byte. 
	 * @param ch Character from AIS or NMEA string
	 * @return 6 bit integer (0 to 63)
	 */
	public static byte convert628(char ch) {
		// returns a six bit to eight bit LUT - 
		if (lut6 == null) lut6 = createLUT6();
		return lut6[ch];
	}
	/**
	 * Convert a six bit integer value to AIS / NMEA character data
	 * 
	 * @param n an six bit ascii code
	 * @return a six bit character
	 */
	public static char convert826(int n) {
		// returns a six bit to eight bit LUT - 
		return charLUTData[n];
	}
	/**
	 * Lookup table data to go from characters to 6 bit integers. 
	 * The table as is, could be used to convert a number to a character.
	 * When unpacking AIS data, we need to do the opposite, so the data
	 * are flicked around in createLUT6.
	 * <p>
	 * Table G1 from page 171 of  IEC 61993
	 * <p>
	 * 19 June - changed '\'' to 0x60 (opposite of error in ASCII6 array !
	 */
//	static char[] charLUTData = {
//		'0', '1', '2', '3', '4', '5', '6', '7',
//		'8', '9', ':', ';', '<', '=', '>', '?', 
//		'@', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 
//		'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 
//		'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 
//		'\'', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 
//		'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 
//		'p', 'q', 'r', 's', 't', 'u', 'v', 'w'};
	static char[] charLUTData = {
		'0', '1', '2', '3', '4', '5', '6', '7',
		'8', '9', ':', ';', '<', '=', '>', '?', 
		'@', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 
		'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 
		'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 
		0x60, 'a', 'b', 'c', 'd', 'e', 'f', 'g', 
		'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 
		'p', 'q', 'r', 's', 't', 'u', 'v', 'w'};
	/**
	 * Converts the character lookup data to an integer-to-character
	 * LUT, where the indexing is the standard ASCII character, converted to
	 * an integer. 
	 * The resulting lookup table is sparsely populated and should will return 0 for any 
	 * index not used in charLUTData.
	 * @return a six bit value packed into one byte
	 */
	public static byte[] createLUT6() {
		lut6 = new byte[256];
		int i6;
		for (int i = 0; i < charLUTData.length; i++) {
			i6 = charLUTData[i];
			lut6[i6] = (byte) i;
		}
		return lut6;
	}
	

	@Override
	public String toString() {
		String str = new String();
		for (int i = 0; i <= 38; i++) {
			if (getBit(i)) {
				str += "1";
			}
			else {
				str += "0";
			}
		}
		return str;
	}
	
}

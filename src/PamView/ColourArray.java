package PamView;

import java.awt.Color;
import java.io.Serializable;

/**
 * A series of functions for creating arrays of colours
 * <p>
 * Can be used for spectrogram colouring, contour colouring, etc.
 * @author Doug Gillespie
 *
 */
public class ColourArray implements Cloneable, Serializable{


	private Color[] colours;
	
	private ColourArray() {
		
	}
	
//	public static final int GREY = 0;
//	public static final int REVERSEGREY = 1;
//	public static final int BLUE = 2;
//	public static final int RED = 3;
//	public static final int GREEN = 4;
//	public static final int HOT = 5;
	
	public static enum ColourArrayType{GREY, REVERSEGREY, BLUE,  GREEN, RED, HOT}
	
	public static String getName(ColourArrayType type) {
		switch (type){
		case GREY:
			return "Grey (black to white)";
		case REVERSEGREY:
			return "Grey (white to black)";
		case BLUE:
			return "Blue";
		case GREEN:
			return "Green";
		case RED:
			return "Red";
		case HOT:
			return "Hot (multicoloured)";
		default:
			return "Unknown";
		}
	}
	
	public static ColourArray createStandardColourArray(int nPoints, ColourArrayType type) {
		if (type == null) {
			type = ColourArrayType.GREY;
		}
		switch (type){
		case GREY:
			return createMergedArray(nPoints, Color.WHITE, Color.BLACK);
		case REVERSEGREY:
			return createMergedArray(nPoints, Color.BLACK, Color.WHITE);
		case BLUE:
			return createMergedArray(nPoints, Color.BLACK, Color.BLUE);
		case GREEN:
			return createMergedArray(nPoints, Color.BLACK, Color.GREEN);
		case RED:
			return createMergedArray(nPoints, Color.BLACK, Color.RED);
		case HOT:
			return createHotArray(nPoints);
		default:
			return createMergedArray(nPoints, Color.GREEN, Color.RED);
		}
	}

	
	public static ColourArray createWhiteToBlackArray(int nPoints) {
		return createMergedArray(nPoints, Color.WHITE, Color.BLACK);
	}
	
	public static ColourArray createBlackToWhiteArray(int nPoints) {
		return createMergedArray(nPoints, Color.BLACK, Color.WHITE);
	}
	
	public static ColourArray createHotArray(int nPoints) {
		// go from black to blue to cyan to green to orange to red
		// that's five stages in total. 		
		return createMultiColouredArray(nPoints, Color.BLACK, Color.BLUE, Color.CYAN,
				Color.GREEN, Color.ORANGE, Color.RED);
	}
	
	/**
	 * Create a multicoloured array of colours that merges in turn between each of
	 * the colours given in the list. 
	 * @param nPoints total number of colour points
	 * @param colourList variable number of colours. 
	 * @return a new ColourArray object. 
	 */
	public static ColourArray createMultiColouredArray(int nPoints, Color ... colourList) {

		if (colourList.length == 1) {
			return createMergedArray(nPoints, colourList[0], colourList[0]);
		}
		else if (colourList.length == 2) {
			return createMergedArray(nPoints, colourList[0], colourList[1]);
		}
		
		ColourArray ca = new ColourArray();
		ca.colours = new Color[nPoints];
		
		int nSegments = (colourList.length - 1);
		int segPoints = nPoints / nSegments-1;
		int lastPoints = nPoints - segPoints * (nSegments-1);
		int thisSegPoints;
		int pointsToCreate;
		int iPoint = 0;
		ColourArray subArray;
		for (int i = 0; i < nSegments-1; i++) {
			subArray = createMergedArray(segPoints+1, colourList[i], colourList[i+1]);
			for (int j = 0; j < segPoints; j++) {
				ca.colours[iPoint++] = subArray.colours[j];
			}
		}
		// now the last one
		subArray = createMergedArray(lastPoints, colourList[nSegments-1], colourList[nSegments]);
		for (int j = 0; j < lastPoints; j++) {
			ca.colours[iPoint++] = subArray.colours[j];
		}
		
		return ca;
	}
	
//	static ColourArray createHotArray(int nPoints) {
//		
//	}
	
	public static ColourArray createMergedArray(int nPoints, Color c1, Color c2) {
		ColourArray ca = new ColourArray();
		ca.colours = new Color[nPoints];
		float col1[] = new float[3];
		float col2[] = new float[3];
		float step[] = new float[3];
		col1 = c1.getColorComponents(null);
		col2 = c2.getColorComponents(null);
		if (nPoints == 1) {
			ca.colours[0] = new Color(col1[0], col1[1], col1[2]);
			return ca;
		}
		for (int i = 0; i < 3; i++) {
			step[i] = (col2[i]-col1[i]) / (nPoints-1);
		}
		for (int c = 0; c < nPoints; c++) {
			try {
		  ca.colours[c] = new Color(col1[0], col1[1], col1[2]);
			}
			catch (IllegalArgumentException ex) {
				System.out.println(String.format("Illegal colour arguments red %3.3f green %3.3f blue %3.3f",
						col1[0], col1[1], col1[2]));
			}
		  for (int i = 0; i < 3; i++) {
			  col1[i] += step[i];
			  col1[i] = Math.max(0, Math.min(1, col1[i]));
		  }
		}		
		return ca;
	}
	
	public Color[] getColours() {
		return colours;
	}
	
	public Color getColour(int iCol) {
		return colours[iCol];
	}
	
	public int[] getIntColourArray(int iCol) {
		int[] rgb = new int[3];
		rgb[0] = colours[iCol].getRed();
		rgb[1] = colours[iCol].getGreen();
		rgb[2] = colours[iCol].getBlue();
		return rgb;
	}
	
	public int getNumbColours() {
		if (colours == null) {
			return 0;
		}
		else {
			return colours.length;
		}
	}

	public void reverseArray() {
		Color[] newColours = new Color[colours.length];
		for (int i = 0; i < colours.length; i++) {
			newColours[i] = colours[colours.length-1-i];
		}
		colours = newColours;
	}

	@Override
	protected ColourArray clone() {
		try {
			ColourArray newArray = (ColourArray) super.clone();
			newArray.colours = this.colours.clone();
			return newArray;
		}
		catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
}

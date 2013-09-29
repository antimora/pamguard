package Localiser.bearingLocaliser;

import java.util.Arrays;

import Array.ArrayManager;
import Array.PamArray;
import PamUtils.PamUtils;
import pamMaths.PamVector;

/**
 * Really simple BearingLocaliser which works with two element closely
 * spaced arrays. 
 * @author Doug Gillespie
 *
 */
public class PairBearingLocaliser implements BearingLocaliser {

	private int[] phoneNumbers;
	
	private double spacing, spacingError;
	
	private double speedOfSound, speedOfSoundError;
	
	private PamArray currentArray;

	private PamVector[] arrayAxis;

	private int arrayType;
	
	private double timingError;

	private double wobbleRadians;
	
	public PairBearingLocaliser(int hydrophoneBitMap, double timingError) {
		prepare(PamUtils.getChannelArray(hydrophoneBitMap), timingError);
	}
	
	public PairBearingLocaliser(int[] hydrophoneList, double timingError) {
		prepare(hydrophoneList, timingError);
	}
	
	
	@Override
	public void prepare(int[] arrayElements, double timingError) {
		this.timingError = timingError;
		if (arrayElements.length != 2) {
			System.out.println("Attempt to use PairBearingLocaliser with less than or greater than two elements");
		}
		phoneNumbers = Arrays.copyOf(arrayElements, 2);
		int phoneMap = PamUtils.makeChannelMap(phoneNumbers); 
		ArrayManager arrayManager = ArrayManager.getArrayManager();
		currentArray = arrayManager.getCurrentArray();
		arrayType = arrayManager.getArrayShape(currentArray, phoneMap);
		if (arrayType != ArrayManager.ARRAY_TYPE_LINE) {
			System.out.println("The Hydrophones to not lie in a line, but are a " 
					+ ArrayManager.getArrayTypeString(arrayType));
		}
		spacing = currentArray.getSeparation(phoneNumbers[0], phoneNumbers[1]);
		spacingError = currentArray.getSeparationError(phoneNumbers[0], phoneNumbers[1]);
		wobbleRadians = currentArray.getWobbleRadians(phoneNumbers[0], phoneNumbers[1]);
		speedOfSound = currentArray.getSpeedOfSound();
		speedOfSoundError = currentArray.getSpeedOfSoundError();
		arrayAxis = arrayManager.getArrayDirections(currentArray, phoneMap); 
		/*
		 * May need to set spacing to it's negative if the principle array axis is in the 
		 * opposite direction to the pair axis !
		 */
		PamVector v1, v0;
		v1 = currentArray.getAbsHydrophoneVector(arrayElements[1]);
		v0 = currentArray.getAbsHydrophoneVector(arrayElements[0]);
		if (v0 == null || v1 == null) {
			spacing = 0;
		}
		else {
			PamVector pairVector = v1.sub(v0);
			if (pairVector.dotProd(arrayAxis[0]) > 0) {
				spacing = -spacing;
			}
		}
//		PamVector pairVector = currentArray.getAbsHydrophoneVector(arrayElements[1]).
//		sub(currentArray.getAbsHydrophoneVector(arrayElements[0]));
		
	}

	@Override
	public PamVector[] getArrayAxis() {
		return arrayAxis;
	}

	@Override
	public int getArrayType() {
		return arrayType;
	}

	@Override
	public double[][] localise(double[] delays) {
		double[][] ans = new double[2][1];
		if (delays == null || delays.length == 0) {
			return null;
		}
		double ct = speedOfSound * delays[0] / spacing;
		ct = Math.max(-1., Math.min(1., ct));
		
		double angle = Math.acos(ct);
		
		double e1 = speedOfSound * timingError;
		double e2 = speedOfSound * delays[0] / spacing * spacingError;
		double e3 = delays[0] * speedOfSoundError;
		double error = (e1*e1 + e2*e2 + e3*e3) / spacing / Math.sin(angle);
		error += wobbleRadians;
		error = Math.sqrt(error);
		
		ans[0][0] = angle;
		ans[1][0] = error;
		
//		System.out.println(String.format("Pair angle %3.1f +- %3.1f",
//				Math.toDegrees(angle), Math.toRadians(error)));
		
		return ans;
	}

}

package org.openmolecules.fx.viewer3d.interactions.drugscore;

import com.actelion.research.util.FastSpline;
import com.actelion.research.util.SmoothingSplineInterpolator;

import java.util.Arrays;

public class DrugScorePotential {
	public static final double BINS_PER_ANGSTROM = 20;
	public static final double ANGSTROMS_PER_BIN = 0.05;
	private static final double MAX_REPULSION = 2.0;

	private final double mBaseDistance,mMaxDistance;
	private final short[] mPotentialTimes10000;
	private FastSpline mSplineFunction;

	public DrugScorePotential(String values) {
		String[] value = values.split(" ");
		int skipCount = Integer.parseInt(value[0]);
		mBaseDistance = (double)skipCount * ANGSTROMS_PER_BIN;
		mMaxDistance = (double)(skipCount + value.length-1) * ANGSTROMS_PER_BIN;
		mPotentialTimes10000 = new short[value.length-1];
		for (int i=0; i<value.length-1; i++)
			mPotentialTimes10000[i] = Short.parseShort(value[i+1]);
	}

	/**
	 * Generates a DrugScorePotential from unsmoothed potential values derived from binned count values
	 * @param potential values starting at bin startIndex * ANGSTROMS_PER_BIN
	 * @param startDistance distance that refers to left edge of bin[0], typically lower than the VDW-radii sum of interacting atoms to cover part of the repulsion
	 */
	public DrugScorePotential(double[] potential, double startDistance) {
		int startIndex = 0;
		while (potential[startIndex] > MAX_REPULSION)
			startIndex++;

		mBaseDistance = startDistance + (double)startIndex * ANGSTROMS_PER_BIN;
		mMaxDistance = startDistance + ANGSTROMS_PER_BIN * potential.length;
		mPotentialTimes10000 = new short[potential.length-startIndex];
		for (int i=0; i<mPotentialTimes10000.length; i++)
			mPotentialTimes10000[i] = (short)Math.round(10000 * potential[startIndex+i]);
	}

	/**
	 * @param distance
	 * @return the interaction strength of this interaction potential at the given distance
	 */
	public double getPotential(double distance) {
		if (mSplineFunction == null)
			mSplineFunction = createSplineFunction();

		return (distance < mBaseDistance) ? MAX_REPULSION : (distance > mMaxDistance) ? 0.0 : mSplineFunction.value(distance);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder(Long.toString(Math.round(mBaseDistance * BINS_PER_ANGSTROM)));
		for (short s : mPotentialTimes10000)
			sb.append(' ').append(s);
		return sb.toString();
	}

	private FastSpline createSplineFunction() {
		double[] x = new double[mPotentialTimes10000.length];
		double[] y = new double[mPotentialTimes10000.length];

		for (int i=0; i<mPotentialTimes10000.length; i++) {
			x[i] = mBaseDistance + (0.5 + i) * ANGSTROMS_PER_BIN;
			y[i] = 0.0001 * mPotentialTimes10000[i];
		}

		double[] sigma = new double[mPotentialTimes10000.length];
		Arrays.fill(sigma, 1);

		SmoothingSplineInterpolator interpolator = new SmoothingSplineInterpolator();
		interpolator.setLambda(0.0001);	// seems a good compromise between smoothing and retaining full extend of maxima
		interpolator.setSigma(sigma);

		return interpolator.interpolate(x, y);
	}
}

/*
 * FXMolViewer, showing and manipulating molecules and protein structures in 3D.
 * Copyright (C) 2019 Thomas Sander

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * @author Thomas Sander
 */

package org.openmolecules.fx.surface;

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.chem.conf.VDWRadii;
import com.actelion.research.chem.prediction.CLogPPredictor;
import com.actelion.research.util.SortedList;
import javafx.collections.ObservableFloatArray;
import javafx.collections.ObservableIntegerArray;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.TriangleMesh;
import org.sunflow.math.Point3;

/**
 * Created by thomas on 28.03.16.
 */
public class PropertyTexture extends SurfaceTexture {
	private final float REACH = 1.5f;
//	private final float REACH = 0.666666f;

	private final int TEXTURE_STEPS = 192;
	private final int TEXTURE_GRAY = 192;
	private final float TEXTURE_GRAY_F = 0.75f;

	private int mMode;
	private float[] mAtomValue;
	private float mSurfaceSurplus;

	public PropertyTexture(TriangleMesh mesh, StereoMolecule mol, int mode, double opacity, float surfaceSurplus) {
		super(mesh, mol);
		mMode = mode;

		mSurfaceSurplus = surfaceSurplus;

		switch (mode) {
		case SurfaceMesh.SURFACE_COLOR_POLARITY:
			createMonoPolarTexture(opacity);
			calculateLogPIncrements();
			break;
		case SurfaceMesh.SURFACE_COLOR_DONORS_ACCEPTORS:
			createBipolarTexture(opacity);
			calculateDonorsAndAcceptors();
			break;
		}
	}

	private void createBipolarTexture(double opacity) {
		mImage = new WritableImage(2*TEXTURE_STEPS, 1);
		PixelWriter pw = ((WritableImage)mImage).getPixelWriter();
		for (int i=0; i<TEXTURE_STEPS; i++) {
			int gray = TEXTURE_GRAY - (i * TEXTURE_GRAY / TEXTURE_STEPS);
			int color = TEXTURE_GRAY + (i * (256 - TEXTURE_GRAY) / TEXTURE_STEPS);
			pw.setColor(TEXTURE_STEPS-i-1, 0, Color.rgb(gray, gray, color, opacity));
			pw.setColor(TEXTURE_STEPS+i, 0, Color.rgb(color, gray, gray, opacity));
		}
	}

	private void createMonoPolarTexture(double opacity) {
		mImage = new WritableImage(TEXTURE_STEPS, 1);
		PixelWriter pw = ((WritableImage)mImage).getPixelWriter();
		for (int i=0; i<TEXTURE_STEPS; i++) {
			int gray = TEXTURE_GRAY - (i * TEXTURE_GRAY / TEXTURE_STEPS);
			int color = TEXTURE_GRAY + (i * (256 - TEXTURE_GRAY) / TEXTURE_STEPS);
			pw.setColor(i, 0, Color.rgb(color, gray, gray, opacity));
		}
	}

	@Override
	public void applyToSurface() {
		ObservableFloatArray texCoords = mMesh.getTexCoords();
		texCoords.clear();
		float dx = 1f / (float) mImage.getWidth();
		float dy = 1f / (float) mImage.getHeight();
		for (float y = dy / 2; y < 1f; y += dy)
			for (float x = dx / 2; x < 1f; x += dx)
				texCoords.addAll(x, y);

		// determine an index into the texture coordinate list for every mesh vertex
		ObservableFloatArray points = mMesh.getPoints();
		int[] textureIndex = new int[points.size()];
		int count = points.size() / 3;
		for (int i = 0; i < count; i++) {
			int base = i * 3;
			float value = calculateNormalizedProperty(points.get(base), points.get(base + 1), points.get(base + 2), mSortedAtomsFX, mMol);
			textureIndex[i] = getIndexFromValue(value);
		}

		// write texture indexes into the face table (one for every face corner)
		ObservableIntegerArray faces = mMesh.getFaces();
		if (SurfaceMesh.USE_NORMALS) {
			for (int i = 0; i < faces.size(); i += 9) {
				faces.set(i + 2, textureIndex[faces.get(i)]);
				faces.set(i + 5, textureIndex[faces.get(i + 3)]);
				faces.set(i + 8, textureIndex[faces.get(i + 6)]);
			}
		}
		else {
			for (int i = 0; i < faces.size(); i += 6) {
				faces.set(i + 1, textureIndex[faces.get(i)]);
				faces.set(i + 3, textureIndex[faces.get(i + 2)]);
				faces.set(i + 5, textureIndex[faces.get(i + 4)]);
			}
		}
	}

	@Override
	public org.sunflow.image.Color getSurfaceColor(Point3 p) {
		float value = calculateNormalizedProperty(p.x, p.y, p.z, mSortedAtomsSunFlow, mConformerSunFlow.getMolecule());
		if (mMode == SurfaceMesh.SURFACE_COLOR_POLARITY) {
			float gray = TEXTURE_GRAY_F - value * TEXTURE_GRAY_F;
			float color = TEXTURE_GRAY_F + value * (1f - TEXTURE_GRAY_F);
			return new org.sunflow.image.Color(color, gray, gray);
		}
		else {
			if (value < 0.5f) {
				float v = 1f - 2f*value;
				float gray = TEXTURE_GRAY_F - v * TEXTURE_GRAY_F;
				float color = TEXTURE_GRAY_F + v * (1f - TEXTURE_GRAY_F);
				return new org.sunflow.image.Color(gray, gray, color);
			}
			else {
				float v = 2f*value - 1f;
				float gray = TEXTURE_GRAY_F - v * TEXTURE_GRAY_F;
				float color = TEXTURE_GRAY_F + v * (1f - TEXTURE_GRAY_F);
				return new org.sunflow.image.Color(color, gray, gray);
			}
		}
	}

	private float calculateNormalizedProperty(float x, float y, float z, SortedList<AtomWithXCoord> sortedAtomList, StereoMolecule mol) {
		// variables for surface property coloring
		float sum = 0.0f;
		float weightSum = 0.0001f;   // not 0 to prevent infinity

		int i1 = getLowIndex(x - REACH - mSurfaceSurplus, sortedAtomList);
		int i2 = getHighIndex(x + REACH + mSurfaceSurplus, sortedAtomList);
		for (int index=i1; index<i2; index++) {
			int atom = toAtom(index, sortedAtomList);
			float vdwr = VDWRadii.VDW_RADIUS[mMol.getAtomicNo(atom)];
			float influenceRadius = vdwr + REACH + mSurfaceSurplus;
			float d = distanceToPoint(x, y, z, influenceRadius, mol.getCoordinates(atom));
			if (d != Float.MAX_VALUE) {
				float weight = calculateWeight(d-vdwr-mSurfaceSurplus);
				weightSum += weight;
				sum += weight * mAtomValue[atom];
			}
		}

		return sum / weightSum;
	}

	private void calculateLogPIncrements() {
		mAtomValue = new float[mMol.getAllAtoms()];
		new CLogPPredictor().getCLogPIncrements(new StereoMolecule(mMol), mAtomValue);
		for (int atom=0; atom<mMol.getAtoms(); atom++)
			mAtomValue[atom] = -mAtomValue[atom];   // we want high values in neutral color
		copyValuesToHydrogen();
		limitAndNormalizeValues(-1f, 1f);
	}

	private void calculateDonorsAndAcceptors() {
		mAtomValue = new float[mMol.getAllAtoms()];
		for (int atom=0; atom<mMol.getAtoms(); atom++)
			if ((mMol.getAtomicNo(atom) == 7 || mMol.getAtomicNo(atom) == 8)
			 && mMol.getAllHydrogens(atom) > 0)
				mAtomValue[atom] = 1;
		copyValuesToHydrogen(); // we copy the donor character to the hydrogen
		for (int atom=0; atom<mMol.getAtoms(); atom++)
			if (mAtomValue[atom] == 0 && (mMol.getAtomicNo(atom) == 7 || mMol.getAtomicNo(atom) == 8))
				mAtomValue[atom] = -1f;
		limitAndNormalizeValues(-1f, 1f);
	}

	/**
	 * Assigns the values of hydrogen bearing atoms also to the hydrogen atoms.
	 */
	private void copyValuesToHydrogen() {
		for (int atom=mMol.getAtoms(); atom<mMol.getAllAtoms(); atom++)
			if (mMol.getConnAtoms(atom) == 1)
				mAtomValue[atom] = mAtomValue[mMol.getConnAtom(atom, 0)];
	}

	/**
	 * Normalizes the mAtomValue array to contain values v with 0.0 <= v <= 1.0
	 */
	private void normalizeValues() {
		float min = Float.MAX_VALUE;
		float max = -Float.MAX_VALUE;
		for (int i=0; i<mAtomValue.length; i++) {
			if (min > mAtomValue[i])
				min = mAtomValue[i];
			if (max < mAtomValue[i])
				max = mAtomValue[i];
		}
		float d = (max - min);
		for (int i=0; i<mAtomValue.length; i++)
			mAtomValue[i] = (mAtomValue[i] - min) / d;
	}

	/**
	 * Normalizes the mAtomValue array to contain values v with 0.0 <= v < 1.0
	 */
	private void limitAndNormalizeValues(float min, float max) {
		float d = (max - min);
		for (int i=0; i<mAtomValue.length; i++) {
			if (mAtomValue[i] < min)
				mAtomValue[i] = min;
			else if (mAtomValue[i] > max)
				mAtomValue[i] = max;

			mAtomValue[i] = (mAtomValue[i] - min) / d;
		}
	}

	private int getIndexFromValue(double surfaceValue) {
		if (mMode == SurfaceMesh.SURFACE_COLOR_POLARITY)
			return (int)(surfaceValue * 191.999);
		else
			return (int)(surfaceValue * 383.999);
	}
}

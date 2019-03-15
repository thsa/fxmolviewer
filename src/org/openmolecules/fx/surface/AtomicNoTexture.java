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

import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.chem.conf.VDWRadii;
import javafx.collections.ObservableFloatArray;
import javafx.collections.ObservableIntegerArray;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.TriangleMesh;
import org.openmolecules.mesh.MoleculeSurfaceAlgorithm;
import org.sunflow.math.Point3;

import java.util.Arrays;
import java.util.TreeMap;

import static org.openmolecules.render.MoleculeArchitect.ATOM_ARGB;

/**
 * Created by thomas on 28.03.16.
 */
public class AtomicNoTexture extends SurfaceTexture {
	private final float SURPLUS = MoleculeSurfaceAlgorithm.DEFAULT_VOXEL_SIZE; // larger than distance triangle center to far corner
	private final float REACH = 1.0f;   // reach distance in addition do VDW-radius for atoms to influence surface color

	private final int MAX_ATOMS_ON_TRIANGLE = 16;
	private final int IMAGE_STRIPE_WIDTH = 16;  // for 1D-gradient (2 colors)
	private final int IMAGE_AREA_SIZE = 32;     // for 2D-gradient (3 colors)
	private final int IMAGE_AREA_BORDER = 4;    // around 2D-gradient to avoid FX artefacts
	private final int IMAGE_HEIGHT = IMAGE_AREA_SIZE+2*IMAGE_AREA_BORDER;

	private int mSingleCount,mDoubleCount,mTripleCount;
	private TreeMap<Integer,Integer> mTripleMap;    // map from atomicNoTriple to imageAreaIndex
	private TreeMap<Integer,Integer> mTextureMap;   // map from image pixel index to mesh texture index
	private int[] mInAreaIndex; // pixel indexes of face corners within triple area
	private int[] mTripleOfFace;
	private float[] mAtomicNoWeightBuffer;
	private int[] mAtomicNoBuffer;
	private int[] mSortedAtomicNoBuffer;
	private float mSurfaceSurplus;
	private int mNeutralRGB;

	private float mPixelWidth,mPixelHeight,mPixelWidthHalf,mPixelHeightHalf;

	public AtomicNoTexture(TriangleMesh mesh, Conformer conformer, Color color, double opacity, float surfaceSurplus) {
		super(mesh, conformer);

		mAtomicNoWeightBuffer = new float[MAX_ATOMS_ON_TRIANGLE];
		mAtomicNoBuffer = new int[MAX_ATOMS_ON_TRIANGLE];
		mSortedAtomicNoBuffer = new int[3];

		mSurfaceSurplus = surfaceSurplus;

		compileAtomicNoTriples();
		createImage(color, opacity);

		mNeutralRGB = (color == null) ? -1
					: ((int)(255.99 * color.getRed()) << 16)
					+ ((int)(255.99 * color.getGreen()) << 8)
					+  (int)(255.99 * color.getBlue());
	}

	@Override
	public org.sunflow.image.Color getSurfaceColor(Point3 p) {
		float r = 0f;
		float g = 0f;
		float b = 0f;
		float weightSum = 0.0000001f;   // to avoid infinity
		int i1 = getLowIndex(p.x - REACH - mSurfaceSurplus, mSortedAtomsSunFlow);
		int i2 = getHighIndex(p.x + REACH + mSurfaceSurplus, mSortedAtomsSunFlow);
		for (int index=i1; index<i2; index++) {
			int atom = toAtom(index, mSortedAtomsSunFlow);
			int atomicNo = mMol.getAtomicNo(atom);
			float vdwr = VDWRadii.VDW_RADIUS[atomicNo];
			float influenceRadius = vdwr + REACH + mSurfaceSurplus;
			float d = distanceToAtom(p.x, p.y, p.z, influenceRadius, atom, mConformerSunFlow);
			if (d != Float.MAX_VALUE) {
				int argb = (mNeutralRGB != -1 && (atomicNo == 1 || atomicNo == 6)) ?
						mNeutralRGB : ATOM_ARGB[atomicNo];
				float weight = calculateWeight(d-vdwr-mSurfaceSurplus);
				r += weight * (float)(argb & 0x00FF0000) / 16711680f;
				g += weight * (float)(argb & 0x0000FF00) / 65280f;
				b += weight * (float)(argb & 0x000000FF) / 255f;
				weightSum += weight;
			}
		}
		if (weightSum == 0.0000001f) {
			System.out.println("WARNING: no atom for surface color found+");
		}
		return new org.sunflow.image.Color(r/weightSum, g/weightSum, b/weightSum);
	}

	private void compileAtomicNoTriples() {
		mTripleMap = new TreeMap<>();
		ObservableIntegerArray faces = mMesh.getFaces();
		ObservableFloatArray points = mMesh.getPoints();
		int[] atom = new int[MAX_ATOMS_ON_TRIANGLE];
		mSingleCount = 0;
		mDoubleCount = 0;
		mTripleCount = 0;
		int faceInc = SurfaceMesh.USE_NORMALS ? 3 : 2;
		int faceSize = 3*faceInc;
		int faceCount = faces.size() / faceSize;
		mTripleOfFace = new int[faceCount];
		mInAreaIndex = new int[3*faceCount];
		for (int i=0; i<faceCount; i++) {
			int p1 = 3*faces.get(faceSize*i);
			int p2 = 3*faces.get(faceSize*i+faceInc);
			int p3 = 3*faces.get(faceSize*i+2*faceInc);

			float x1 = points.get(p1);
			float x2 = points.get(p2);
			float x3 = points.get(p3);
			float y1 = points.get(p1+1);
			float y2 = points.get(p2+1);
			float y3 = points.get(p3+1);
			float z1 = points.get(p1+2);
			float z2 = points.get(p2+2);
			float z3 = points.get(p3+2);

			int triple = determineAtomicNoTriple((x1+x2+x3)/3.0f, (y1+y2+y3)/3.0f, (z1+z2+z3)/3.0f, atom);
			if ((triple & 0x00FFFF00) == 0) {
				if (!mTripleMap.containsKey(triple))
					mTripleMap.put(triple, mSingleCount++);
				mInAreaIndex[3*i]   = mTripleMap.get(triple); // triple is atomicNo
				mInAreaIndex[3*i+1] = mTripleMap.get(triple); // triple is atomicNo
				mInAreaIndex[3*i+2] = mTripleMap.get(triple); // triple is atomicNo
			}
			else if ((triple & 0x00FF0000) == 0) {
				if (!mTripleMap.containsKey(triple))
					mTripleMap.put(triple, mDoubleCount++);
				mInAreaIndex[3*i]   = calculateColorIndex1D(x1, y1, z1, atom, triple);
				mInAreaIndex[3*i+1] = calculateColorIndex1D(x2, y2, z2, atom, triple);
				mInAreaIndex[3*i+2] = calculateColorIndex1D(x3, y3, z3, atom, triple);
			}
			else {
				if (!mTripleMap.containsKey(triple))
					mTripleMap.put(triple, mTripleCount++);
				mInAreaIndex[3*i]   = calculateColorIndex2D(x1, y1, z1, atom, triple);
				mInAreaIndex[3*i+1] = calculateColorIndex2D(x2, y2, z2, atom, triple);
				mInAreaIndex[3*i+2] = calculateColorIndex2D(x3, y3, z3, atom, triple);
			}
			mTripleOfFace[i] = triple;
		}
	}

	/**
	 * We determine all atoms in the vicinity of the triangle center such that all those atoms
	 * are included, which should contribute to the texture color of any of the triangle's corners.
	 * If these include more than three different atomicNos, then we determine those three atomicNos
	 * with the highest contribution to a blended color and remove all atoms, which don't belong to
	 * these atomicNos.<br>
	 * @param x
	 * @param y
	 * @param z
	 * @param nearAtom atoms in vicinity; indexes: 0...n-1 nearAtom[n] = -1
	 * @return unique triple of up to three distinct sorted atomic nos (LSB is highest atomic no)
	 */
	private int determineAtomicNoTriple(float x, float y, float z, int[] nearAtom) {
		int atomCount = 0;
		int atomicNoCount = 0;
		float inverseReach = 1f / (REACH + SURPLUS);
		int i1 = getLowIndex(x - REACH - mSurfaceSurplus - SURPLUS, mSortedAtomsFX);
		int i2 = getHighIndex(x + REACH + mSurfaceSurplus + SURPLUS, mSortedAtomsFX);
		for (int index=i1; index<i2; index++) {
			int atom = toAtom(index, mSortedAtomsFX);
			float vdwr = VDWRadii.VDW_RADIUS[mMol.getAtomicNo(atom)];
			float influenceRadius = vdwr + REACH + mSurfaceSurplus + SURPLUS;
			float d = distanceToAtom(x, y, z, influenceRadius, atom, mConformerFX);
			if (d != Float.MAX_VALUE) {
				if (atomCount == MAX_ATOMS_ON_TRIANGLE-1) {
					System.out.println("WARNING: more near atoms than MAX_ATOMS_ON_TRIANGLE");
					break;
				}
				nearAtom[atomCount] = atom;
				float weight = calculateWeight(d-vdwr-mSurfaceSurplus);
				int aNo = mMol.getAtomicNo(atom);
				boolean found = false;
				for (int i=0; i<atomicNoCount; i++) {
					if (mAtomicNoBuffer[i] == aNo) {
						mAtomicNoWeightBuffer[i] += weight;
						found = true;
					}
				}
				if (!found) {
					mAtomicNoBuffer[atomicNoCount] = aNo;
					mAtomicNoWeightBuffer[atomicNoCount] = weight;
					atomicNoCount++;
				}
				atomCount++;
			}
		}

		nearAtom[atomCount] = -1;

		if (atomicNoCount == 0)
			return 0;
		if (atomicNoCount == 1)
			return mAtomicNoBuffer[0];
		if (atomicNoCount == 2)
			return mAtomicNoBuffer[0] < mAtomicNoBuffer[1] ? (mAtomicNoBuffer[0] << 8) | mAtomicNoBuffer[1] : (mAtomicNoBuffer[1] << 8) | mAtomicNoBuffer[0];

		if (atomicNoCount > 3) {
			// bubble sort atomicNos with decreasing weights
			for (int i=atomicNoCount-1; i>=1; i--) {
				for (int j=0; j<i; j++) {
					if (mAtomicNoWeightBuffer[j] < mAtomicNoWeightBuffer[j + 1]) {
						float ft = mAtomicNoWeightBuffer[j];
						mAtomicNoWeightBuffer[j] = mAtomicNoWeightBuffer[j + 1];
						mAtomicNoWeightBuffer[j + 1] = ft;
						int at = mAtomicNoBuffer[j];
						mAtomicNoBuffer[j] = mAtomicNoBuffer[j + 1];
						mAtomicNoBuffer[j + 1] = at;
					}
				}
			}

			// remove atoms that don't have one of the three high weight atomicNos
			for (int i=atomCount-1; i>=0; i--) {
				int aNo = mMol.getAtomicNo(nearAtom[i]);
				if (aNo != mAtomicNoBuffer[0] && aNo != mAtomicNoBuffer[1] && aNo != mAtomicNoBuffer[2]) {
					for (int j=i+1; j<atomCount; j++)
						nearAtom[j-1] = nearAtom[j];
					atomCount--;
				}
			}
			nearAtom[atomCount] = -1;
		}

		for (int i=0; i<3; i++)
			mSortedAtomicNoBuffer[i] = mAtomicNoBuffer[i];

		Arrays.sort(mSortedAtomicNoBuffer);
		return (mSortedAtomicNoBuffer[0] << 16) + (mSortedAtomicNoBuffer[1] << 8) + mSortedAtomicNoBuffer[2];
	}

	private int getTextureIndex(int x, int y, int imageWidth) {
		int imageIndex = x + y * imageWidth;
		Integer textureIndex = mTextureMap.get(imageIndex);
		if (textureIndex != null)
			return textureIndex.intValue();

		ObservableFloatArray texCoords = mMesh.getTexCoords();
		int index = texCoords.size()/2;
		mTextureMap.put(imageIndex, index);
		texCoords.addAll(mPixelWidth*x+mPixelWidthHalf, mPixelHeight*y+mPixelHeightHalf);
		return index;
	}

	/**
	 * @param x
	 * @param y
	 * @param z
	 * @param atom
	 * @return color index within an three color area as x+y*IMAGE_AREA_HEIGHT
	 */
	private int calculateColorIndex2D(float x, float y, float z, int[] atom, int triple) {
		float weight1 = 0f;
		float weight2 = 0f;
		float weight3 = 0f;
		int atomicNo1 = (triple & 0xFF0000) >> 16;
		int atomicNo2 = (triple & 0x00FF00) >> 8;
		int atomicNo3 = triple & 0x0000FF;
		for (int i=0; i<MAX_ATOMS_ON_TRIANGLE && atom[i] != -1; i++) {
			float vdwr = VDWRadii.VDW_RADIUS[mMol.getAtomicNo(atom[i])];
			float influenceRadius = vdwr + REACH + mSurfaceSurplus;
			float d = distanceToAtom(x, y, z, influenceRadius, atom[i], mConformerFX);
			if (d != Float.MAX_VALUE) {
				float weight = calculateWeight(d-vdwr-mSurfaceSurplus);
				int atomicNo = mMol.getAtomicNo(atom[i]);
				if (atomicNo == atomicNo1)
					weight1 += weight;
				else if (atomicNo == atomicNo2)
					weight2 += weight;
				else if (atomicNo == atomicNo3)
					weight3 += weight;
				else
					System.out.println("WARNING: 2D Other atomicNo found!!!!!!!!!!!!!");
			}
		}
		if (weight2 + weight3 == 0f)
			return IMAGE_AREA_SIZE /2;
		int xx = get1DIndex(weight2, weight3, IMAGE_AREA_SIZE);
		float w12 = weight2 / (weight2 + weight3);
		int yy = get1DIndex(weight1, w12*weight2+(1f-w12)*weight3, IMAGE_AREA_SIZE);
		return xx+yy* IMAGE_AREA_SIZE;
		}

	/**
	 * @param x
	 * @param y
	 * @param z
	 * @param atom
	 * @return color index within a two color area as x+y*IMAGE_AREA_HEIGHT
	 */
	private int calculateColorIndex1D(float x, float y, float z, int[] atom, int triple) {
		int atomicNo1 = (triple & 0x00FF00) >> 8;
		int atomicNo2 = triple & 0x0000FF;
		float weight1 = 0f;
		float weight2 = 0f;
		for (int i=0; i<MAX_ATOMS_ON_TRIANGLE && atom[i] != -1; i++) {
			float vdwr = VDWRadii.VDW_RADIUS[mMol.getAtomicNo(atom[i])];
			float influenceRadius = vdwr + REACH + mSurfaceSurplus;
			float d = distanceToAtom(x, y, z, influenceRadius, atom[i], mConformerFX);
			if (d != Float.MAX_VALUE) {
				float weight = calculateWeight(d-vdwr-mSurfaceSurplus);
				int atomicNo = mMol.getAtomicNo(atom[i]);
				if (atomicNo == atomicNo1)
					weight1 += weight;
				else if (atomicNo == atomicNo2)
					weight2 += weight;
				else
					System.out.println("WARNING: 1D Other atomicNo found! " + atomicNo1 + "," + atomicNo2 + " other:" + atomicNo);
			}
		}

		return get1DIndex(weight1, weight2, IMAGE_HEIGHT);
	}

	private int get1DIndex(float weight1, float weight2, int count) {
		return Math.min(count-1, Math.round(weight2 / (weight1 + weight2) * count));
	}

	public Image getImage() {
		return mImage;
	}

	private void createImage(Color moleculeColor, double opacity) {
		Color[] color = new Color[ATOM_ARGB.length];
		color[0] = new Color(0,0,0,opacity);
		for (int atom=0; atom<mMol.getAllAtoms(); atom++) {
			int atomicNo = mMol.getAtomicNo(atom);
			if (color[atomicNo] == null) {
				int argb = ATOM_ARGB[atomicNo];
				color[atomicNo] = Color.rgb((argb & 0xFF0000) >> 16, (argb & 0x00FF00) >> 8, argb & 0x0000FF, opacity);
			}
		}

		if (moleculeColor != null)
			color[6] = new Color(moleculeColor.getRed(), moleculeColor.getGreen(), moleculeColor.getBlue(), opacity);

//BufferedImage bi = new BufferedImage(1+mDoubleCount* IMAGE_STRIPE_WIDTH +mTripleCount*IMAGE_HEIGHT, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		mImage = new WritableImage(1+mDoubleCount* IMAGE_STRIPE_WIDTH +mTripleCount*IMAGE_HEIGHT, IMAGE_HEIGHT);
		PixelWriter pw = ((WritableImage)mImage).getPixelWriter();
		Color unusedColor = new Color(0.25, 0.75, 0.25, opacity);
		for (int i = 0; i< IMAGE_HEIGHT -1; i++) {
			pw.setColor(0, i, unusedColor);  // unused color to fill empty spots
//bi.setRGB(0, i, 0xFF40C040);
		}
		pw.setColor(0, IMAGE_HEIGHT -1, new Color(0.75, 0, 0.75, opacity));  // lila for 'no atom nearby'
//bi.setRGB(0, IMAGE_HEIGHT -1, 0xFFC000C0);
		for (int triple:mTripleMap.keySet()) {
			int index = mTripleMap.get(triple);
			if ((triple & 0x00FFFF00) == 0) {
				pw.setColor(0, index, color[triple & 0x000000FF]);  // one pixel per single atomic-no color

//Color c=color[triple & 0x000000FF]; int cv = 0xFF000000+((int)(c.getRed()*255)<<16)+((int)(c.getGreen()*255)<<8)+(int)(c.getBlue()*255);
//bi.setRGB(0, index, 0xFF000000+((int)(c.getRed()*255)<<16)+((int)(c.getGreen()*255)<<8)+(int)(c.getBlue()*255));
			}
			else if ((triple & 0x00FF0000) == 0) {
				int atomicNo1 = (triple & 0x0000FF00) >> 8;
				int atomicNo2 =  triple & 0x000000FF;
				for (int y = 0; y< IMAGE_HEIGHT; y++) {
					double r = (color[atomicNo2].getRed() * y
							  + color[atomicNo1].getRed() * (IMAGE_HEIGHT -y-1))
							 / (IMAGE_HEIGHT -1);
					double g = (color[atomicNo2].getGreen() * y
							  + color[atomicNo1].getGreen() * (IMAGE_HEIGHT -y-1))
							 / (IMAGE_HEIGHT -1);
					double b = (color[atomicNo2].getBlue() * y
							  + color[atomicNo1].getBlue() * (IMAGE_HEIGHT -y-1))
							 / (IMAGE_HEIGHT -1);
					for (int x = 0; x< IMAGE_STRIPE_WIDTH; x++) {
						pw.setColor(1 + index * IMAGE_STRIPE_WIDTH + x, y, new Color(r, g, b, opacity));
//bi.setRGB(1 + index * IMAGE_STRIPE_WIDTH + x, y, 0xFF000000 + ((int) (r * 255) << 16) + ((int) (g * 255) << 8) + (int) (b * 255));
					}
				}
			}
			else {
				int x0 = 1+mDoubleCount* IMAGE_STRIPE_WIDTH +index*IMAGE_HEIGHT;
				int atomicNo1 = (triple & 0x00FF0000) >> 16;
				int atomicNo2 = (triple & 0x0000FF00) >> 8;
				int atomicNo3 =  triple & 0x000000FF;
				for (int x = 0; x< IMAGE_HEIGHT; x++) {
					int xx = Math.max(0, Math.min(IMAGE_AREA_SIZE -1, x-IMAGE_AREA_BORDER));
					double r23 = (color[atomicNo3].getRed() * xx
							+ color[atomicNo2].getRed() * (IMAGE_AREA_SIZE -xx-1))
							/ (IMAGE_AREA_SIZE -1);
					double g23 = (color[atomicNo3].getGreen() * xx
							+ color[atomicNo2].getGreen() * (IMAGE_AREA_SIZE -xx-1))
							/ (IMAGE_AREA_SIZE -1);
					double b23 = (color[atomicNo3].getBlue() * xx
							+ color[atomicNo2].getBlue() * (IMAGE_AREA_SIZE -xx-1))
							/ (IMAGE_AREA_SIZE -1);
					for (int y = 0; y< IMAGE_HEIGHT; y++) {
						int yy = Math.max(0, Math.min(IMAGE_AREA_SIZE-1, y-IMAGE_AREA_BORDER));
						double r = (r23 * yy + color[atomicNo1].getRed() * (IMAGE_AREA_SIZE -yy-1))
								 / (IMAGE_AREA_SIZE -1);
						double g = (g23 * yy + color[atomicNo1].getGreen() * (IMAGE_AREA_SIZE -yy-1))
								 / (IMAGE_AREA_SIZE -1);
						double b = (b23 * yy + color[atomicNo1].getBlue() * (IMAGE_AREA_SIZE -yy-1))
								 / (IMAGE_AREA_SIZE -1);
						pw.setColor(x0+x, y, new Color(r, g, b, opacity));
//bi.setRGB(x0+x, y, 0xFF000000+((int)(r*255)<<16)+((int)(g*255)<<8)+(int)(b*255));
					}
				}
			}
		}
//try { ImageIO.write(bi, "png", new File("/home/thomas/test.png")); } catch (IOException e) {}
	}

	@Override
	public void applyToSurface() {
		assignColorsToFaces();
	}

	private void assignColorsToFaces() {
		mTextureMap = new TreeMap<>();

		ObservableIntegerArray faces = mMesh.getFaces();
		ObservableFloatArray texCoords = mMesh.getTexCoords();
		texCoords.clear();

		int imageWidth = 1 + mDoubleCount* IMAGE_STRIPE_WIDTH + mTripleCount*IMAGE_HEIGHT;
		mPixelWidth = 1.0f / imageWidth;
		mPixelHeight = 1.0f / IMAGE_HEIGHT;
		mPixelWidthHalf = 0.5f * mPixelWidth;
		mPixelHeightHalf = 0.5f * mPixelHeight;

		int offset = SurfaceMesh.USE_NORMALS ? 2 : 1;
		int inc = SurfaceMesh.USE_NORMALS ? 3 : 2;
		int faceInc = 3*inc;

		int faceCount = faces.size() / faceInc;
		for (int i=0; i<faceCount; i++) {
			int triple = mTripleOfFace[i];
			if (triple == 0) {
				for (int j=0; j<3; j++)
					faces.set(faceInc*i+inc*j+offset, getTextureIndex(0, IMAGE_HEIGHT -1, imageWidth));
			}
			else if ((triple & 0x00FFFF00) == 0) {
				for (int j=0; j<3; j++)
					faces.set(faceInc*i+inc*j+offset, getTextureIndex(0, mInAreaIndex[3*i+j], imageWidth));
			}
			else if ((triple & 0x00FF0000) == 0) {
				int x = 1 + mTripleMap.get(triple)* IMAGE_STRIPE_WIDTH + IMAGE_STRIPE_WIDTH /2;
				for (int j=0; j<3; j++) {
					faces.set(faceInc*i+inc*j+offset, getTextureIndex(x, mInAreaIndex[3*i+j], imageWidth));
				}
			}
			else {
				int x = 1 + mDoubleCount* IMAGE_STRIPE_WIDTH + mTripleMap.get(triple) * IMAGE_HEIGHT;
				for (int j=0; j<3; j++) {
					int x0 = IMAGE_AREA_BORDER+mInAreaIndex[3*i+j] % IMAGE_AREA_SIZE;
					int y0 = IMAGE_AREA_BORDER+mInAreaIndex[3*i+j] / IMAGE_AREA_SIZE;
					faces.set(faceInc*i+inc*j+offset, getTextureIndex(x + x0, y0, imageWidth));
				}
			}
		}
	}
}

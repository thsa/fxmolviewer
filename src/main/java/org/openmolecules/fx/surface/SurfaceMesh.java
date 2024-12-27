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

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.Molecule3D;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.util.DoubleFormat;
import javafx.collections.ObservableFloatArray;
import javafx.collections.ObservableIntegerArray;
import javafx.scene.paint.Color;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import org.openmolecules.mesh.MeshBuilder;
import org.openmolecules.mesh.MoleculeSurfaceAlgorithm;
import org.openmolecules.mesh.SmoothMarchingCubesAlgorithm;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import static org.openmolecules.mesh.MoleculeSurfaceAlgorithm.CONNOLLY;

public class SurfaceMesh extends TriangleMesh implements MeshBuilder {
	public static final boolean USE_NORMALS = false;	// doesn't seem to do anything for FX (JDK 1.8.0_74)

	private static final boolean BUILD_TRIANGLE_STATISTICS = false;
	private static final String ANGLE_STATISTICS_FILE_DIR = "/home/thomas/doc/marchingCubes/";
	private BufferedWriter mAngleStatisticsWriter;

	public static final int SURFACE_COLOR_INHERIT = 0;	// Use the color defined for the molecule
	public static final int SURFACE_COLOR_PLAIN = 1;	// This and above have plain colors
	public static final int SURFACE_COLOR_POLARITY = 2;
//	public static final int SURFACE_COLOR_ELECTRONEGATIVITY = ?;  // dummy for now
	public static final int SURFACE_COLOR_DONORS_ACCEPTORS = 3;	// this and above have 1D color gradient
	public static final int SURFACE_COLOR_ATOMIC_NOS = 4;	// this has 2D color gradients
	public static final String[] SURFACE_COLOR_MODE_CODE = { "inherit", "plain", "polarity", "donorsAcceptors", "atomicNo" };

	public static final boolean VERBOSE_POINTS_AND_TRIANGLES = false;    // print addPoint and addTriangle

	private static final String VOLVIS_DATA_PATH = "/home/thomas/data/voxel_data_from_volvis.org/";
	private static final String VOXEL_DATA_BONSAI = VOLVIS_DATA_PATH + "bonsai_256x256x256.raw";
	private static final String VOXEL_DATA_SKULL = VOLVIS_DATA_PATH + "skull_256x256x256.raw";
	private static final String VOXEL_DATA_LOBSTER = VOLVIS_DATA_PATH + "lobster_301x324x56.raw";
	private static final String VOXEL_DATA_RANDOM = "random";	// followed by cube size to create random data
	public static final String VOXEL_DATA_FILE = VOXEL_DATA_BONSAI; // VOXEL_DATA_RANDOM+"100";  // use one of above or use null for molecules

	private SurfaceTexture mTexture;
	private int mSurfaceType;
	private float mProbeSize;

	/**
	 * Generate the molecule's solvent accessible surface (Conolly surface) mesh
	 * using an enhanced marched cubes algorithm to avoid skinny triangles.
	 * It uses a probe with 1.4 Angstrom radius and a voxel size of 0.5 Angstrom.
	 * The surface is uniformly colored.
	 * @param mol
	 * @param surfaceType MoleculeSurfaceAlgorithm.CONNOLLY or LEE_RICHARDS
	 * @param postCreationCutter optional surface cutter to remove parts of the surface just after creation
	 */
	public SurfaceMesh(Molecule3D mol, int surfaceType, SurfaceCutter postCreationCutter) {
		this(mol, surfaceType, SURFACE_COLOR_PLAIN, null, 1f, postCreationCutter);
	}

	/**
	 * Generate the molecule's solvent accessible surface (Conolly surface) mesh
	 * using an enhanced marched cubes algorithm to avoid skinny triangles.
	 * It uses a probe with 1.4 Angstrom radius and a voxel size of 0.5 Angstrom.
	 * The surface is colored according to the textureMode.
	 * @param mol
	 * @param surfaceType MoleculeSurfaceAlgorithm.CONNOLLY or LEE_RICHARDS
	 * @param textureMode
	 * @param neutralColor color of hydrogen and carbon in case of AtomicNoTexture
	 * @param opacity
	 * @param postCreationCutter optional surface cutter to remove parts of the surface just after creation
	 */
	public SurfaceMesh(StereoMolecule mol, int surfaceType, int textureMode, Color neutralColor, double opacity, SurfaceCutter postCreationCutter) {
		super(USE_NORMALS ? VertexFormat.POINT_NORMAL_TEXCOORD : VertexFormat.POINT_TEXCOORD);

		mSurfaceType = surfaceType;
		mProbeSize = MoleculeSurfaceAlgorithm.DEFAULT_PROBE_SIZE;

		getTexCoords().addAll(0, 0);    // create one texture point to refer to

		if (BUILD_TRIANGLE_STATISTICS) {
			try {
				mAngleStatisticsWriter = new BufferedWriter(new FileWriter(ANGLE_STATISTICS_FILE_DIR + "angleStatistics.txt"));
				mAngleStatisticsWriter.write("triangle\tangle\tarea");
				mAngleStatisticsWriter.newLine();
				}
			catch (IOException ioe) {}
			}

		new MoleculeSurfaceAlgorithm(mol, surfaceType, mProbeSize, MoleculeSurfaceAlgorithm.DEFAULT_VOXEL_SIZE, this);

		if (postCreationCutter != null)
			postCreationCutter.cut(this);

//		System.out.println("SurfaceMesh() complete. Triangles:"+getFaces().size()/getFaceElementSize()+" Vertexes:"+getPoints().size()/getPointElementSize());

		if (BUILD_TRIANGLE_STATISTICS)
			try { mAngleStatisticsWriter.close(); } catch (IOException ioe) {}

		if (textureMode > SURFACE_COLOR_PLAIN)
			updateTexture(mol, textureMode, neutralColor, opacity);

		if (USE_NORMALS)
			unifyNormals();
		}

	/**
	 * Generate the molecule's solvent accessible surface (Conolly surface) mesh
	 * using an enhanced marched cubes algorithm to avoid skinny triangles.
	 * It uses a probe with 1.4 Angstrom radius and a voxel size of 0.5 Angstrom.
	 * The surface is colored according to the textureMode.
	 */
	public SurfaceMesh() {
		super(USE_NORMALS ? VertexFormat.POINT_NORMAL_TEXCOORD : VertexFormat.POINT_TEXCOORD);

		mProbeSize = MoleculeSurfaceAlgorithm.DEFAULT_PROBE_SIZE;

		getTexCoords().addAll(0, 0);    // create one texture point to refer to

		if (BUILD_TRIANGLE_STATISTICS) {
			try {
				mAngleStatisticsWriter = new BufferedWriter(new FileWriter(ANGLE_STATISTICS_FILE_DIR + "angleStatistics.txt"));
				mAngleStatisticsWriter.write("triangle\tangle\tarea");
				mAngleStatisticsWriter.newLine();
			}
			catch (IOException ioe) {}
		}

		long t = System.currentTimeMillis();
		SmoothMarchingCubesAlgorithm mesh = new SmoothMarchingCubesAlgorithm(this, 0.1f);
		if (VOXEL_DATA_FILE == VOXEL_DATA_BONSAI)
			mesh.create(VOXEL_DATA_BONSAI, 256, 256, 256, 40.5f);
		else if (VOXEL_DATA_FILE == VOXEL_DATA_SKULL)
			mesh.create(VOXEL_DATA_SKULL, 256, 256, 256, 40.5f);
		else if (VOXEL_DATA_FILE == VOXEL_DATA_LOBSTER)
			mesh.create(VOXEL_DATA_LOBSTER, 56, 324, 301, 40.5f);
//			    mesh.create(VOXEL_DATA_LOBSTER, 56, 324, 301, 0, 56, 100, 324, 100, 301, 40.5f);
		else if (VOXEL_DATA_FILE.startsWith(VOXEL_DATA_RANDOM))
			mesh.createRandom(Integer.parseInt(VOXEL_DATA_FILE.substring(VOXEL_DATA_RANDOM.length())));

//			System.out.println("Time taken for triangulation: "+(System.currentTimeMillis()-t)+" ms");
//		    mesh.createRandomMesh(7);
//		    mesh.createAllCubes(0, 256);

//		System.out.println("SurfaceMesh() complete. Triangles:"+getFaces().size()/getFaceElementSize()+" Vertexes:"+getPoints().size()/getPointElementSize());

		if (BUILD_TRIANGLE_STATISTICS)
			try { mAngleStatisticsWriter.close(); } catch (IOException ioe) {}

		if (USE_NORMALS)
			unifyNormals();
	}

	/**
	 * Depending on the surface type, the surface amy be larger than the van-der-Waals radius.
	 * For the Connolly surface this method returns 0.0, for the Lee-Richards surface it
	 * returns half od the probe size.
	 * @return the amount by which the surface extends beyond the van-der-Waals radius
	 */
	public float getSurfaceSurplus() {
		return mSurfaceType == CONNOLLY ? 0 : mProbeSize;
		}

	@Override
	public void getPoint(int index, float[] xyz) {
		index *= getPointElementSize();
		xyz[0] = getPoints().get(index);
		xyz[1] = getPoints().get(index+1);
		xyz[2] = getPoints().get(index+2);
		}

	@Override
	public int addPoint(float x, float y, float z) {
		if (VERBOSE_POINTS_AND_TRIANGLES)
			System.out.println("addPoint("+DoubleFormat.toString(x)+", "+DoubleFormat.toString(y)+", "+DoubleFormat.toString(z)+")");

		getPoints().addAll(x, y, z);
		// add empty normal associated to the point

		if (USE_NORMALS)
			getNormals().addAll(0, 0, 0);

		return getPoints().size()/getPointElementSize()-1;
		}

	@Override
	public void addTriangle(int i1, int i2, int i3) {
		if (VERBOSE_POINTS_AND_TRIANGLES)
			System.out.println("addTriangle("+i1+", "+i2+", "+i3+")");
/*
ObservableFloatArray p = getPoints();
float x = p.get(i1*3);
float y = p.get(i1*3+1);
float z = p.get(i1*3+2);
if (x < 3 || x > 1) return;
if (y < 0) return;
if (z < -0.4 || z>0.0) return;
System.out.println(i1+"("+p.get(i1*3)+","+p.get(i1*3+1)+","+p.get(i1*3+2)+") "+i2+"("+p.get(i2*3)+","+p.get(i2*3+1)+","+p.get(i2*3+2)+") "+i3+"("+p.get(i3*3)+","+p.get(i3*3+1)+","+p.get(i3*3+2)+") ");
*/
//		if (checkTriangle(i1, i2, i3))
//			return;

		if (BUILD_TRIANGLE_STATISTICS)
			writeTriangleStats(getFaces().size()/getFaceElementSize(), i1, i2, i3);

		if (USE_NORMALS) {
			getFaces().addAll(i3, i3, 0, i2, i2, 0, i1, i1, 0);
	
			ObservableFloatArray points = getPoints();
			float vx = points.get(i2*3  ) - points.get(i1*3);
			float vy = points.get(i2*3+1) - points.get(i1*3+1);
			float vz = points.get(i2*3+2) - points.get(i1*3+2);
			float wx = points.get(i3*3  ) - points.get(i1*3);
			float wy = points.get(i3*3+1) - points.get(i1*3+1);
			float wz = points.get(i3*3+2) - points.get(i1*3+2);
			float nx = vz*wy - vy*wz;
			float ny = vx*wz - vz*wx;
			float nz = vy*wx - vx*wy;
			float l = (float)Math.sqrt(nx*nx + ny*ny + nz*nz);
			nx /= l;
			ny /= l;
			nz /= l;

			ObservableFloatArray normals = getNormals();
			normals.set(i1*3  , normals.get(i1*3  ) + nx);
			normals.set(i1*3+1, normals.get(i1*3+1) + ny);
			normals.set(i1*3+2, normals.get(i1*3+2) + nz);
			normals.set(i2*3  , normals.get(i2*3  ) + nx);
			normals.set(i2*3+1, normals.get(i2*3+1) + ny);
			normals.set(i2*3+2, normals.get(i2*3+2) + nz);
			normals.set(i3*3  , normals.get(i3*3  ) + nx);
			normals.set(i3*3+1, normals.get(i3*3+1) + ny);
			normals.set(i3*3+2, normals.get(i3*3+2) + nz);
			}
		else {
			getFaces().addAll(i3, 0, i2, 0, i1, 0);

			// without any smoothing group it is assumed that all faces are group 1
//			getFaceSmoothingGroups().addAll(1);
			}
		}

	private void writeTriangleStats(int i, int i1, int i2, int i3) {
			i1 *= getPointElementSize();
			i2 *= getPointElementSize();
			i3 *= getPointElementSize();
			float x1 = getPoints().get(i1);
			float y1 = getPoints().get(i1+1);
			float z1 = getPoints().get(i1+2);
			float x2 = getPoints().get(i2);
			float y2 = getPoints().get(i2+1);
			float z2 = getPoints().get(i2+2);
			float x3 = getPoints().get(i3);
			float y3 = getPoints().get(i3+1);
			float z3 = getPoints().get(i3+2);
			float aSquare = (x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1);
			float bSquare = (x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1);
			float cSquare = (x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2);
			float t = aSquare+bSquare-cSquare;
			float area = 0.25f*(float)Math.sqrt(4*aSquare*bSquare-t*t);
			try {
				double angle1 = new Coordinates(x2-x1, y2-y1, z2-z1).getAngle(new Coordinates(x3-x1, y3-y1, z3-z1));
				double angle2 = new Coordinates(x1-x2, y1-y2, z1-z2).getAngle(new Coordinates(x3-x2, y3-y2, z3-z2));
				double angle3 = new Coordinates(x1-x3, y1-y3, z1-z3).getAngle(new Coordinates(x2-x3, y2-y3, z2-z3));
				mAngleStatisticsWriter.write(Integer.toString(i)+"\t"+DoubleFormat.toString(angle1)+"\t"+DoubleFormat.toString(area));
				mAngleStatisticsWriter.newLine();
				mAngleStatisticsWriter.write(Integer.toString(i)+"\t"+DoubleFormat.toString(angle2)+"\t"+DoubleFormat.toString(area));
				mAngleStatisticsWriter.newLine();
				mAngleStatisticsWriter.write(Integer.toString(i)+"\t"+DoubleFormat.toString(angle3)+"\t"+DoubleFormat.toString(area));
				mAngleStatisticsWriter.newLine();
				}
			catch (IOException ioe) {}
			}

	/*	private boolean checkTriangle(int i1, int i2, int i3) {
		i1 *= getPointElementSize();
		i2 *= getPointElementSize();
		i3 *= getPointElementSize();
		float x1 = getPoints().get(i1);
		float y1 = getPoints().get(i1+1);
		float z1 = getPoints().get(i1+2);
		float x2 = getPoints().get(i2);
		float y2 = getPoints().get(i2+1);
		float z2 = getPoints().get(i2+2);
		float x3 = getPoints().get(i3);
		float y3 = getPoints().get(i3+1);
		float z3 = getPoints().get(i3+2);
		float aSquare = (x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1);
		float bSquare = (x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1);
		float cSquare = (x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2);
		float t = aSquare+bSquare-cSquare;
		float area = 0.25f*(float)Math.sqrt(4*aSquare*bSquare-t*t);
		if (area < 0.002)
//			System.out.println("small triangle area:"+area+" a:"+Math.sqrt(aSquare)+" b:"+Math.sqrt(bSquare)+" c:"+Math.sqrt(cSquare));
			System.out.println("small triangle area:"+area+" ("+x1+","+y1+","+z1+") ("+x2+","+y2+","+z2+") ("+x3+","+y3+","+z3+")");
		return (area < 0.002f);
		}*/

	public SurfaceTexture getTexture() {
		return mTexture;
		}

	private void unifyNormals() {
		ObservableFloatArray normals = getNormals();
		for (int i=0; i<normals.size(); i+=3) {
			float l = (float)Math.sqrt(normals.get(i)*normals.get(i)
									 + normals.get(i+1)*normals.get(i+1)
									 + normals.get(i+2)*normals.get(i+2));
			normals.set(i  , normals.get(i  ) / l);
			normals.set(i+1, normals.get(i+1) / l);
			normals.set(i+2, normals.get(i+2) / l);
			}
		}

	/**
	 * @param mol
	 * @param textureMode
	 * @param neutralColor color of hydrogen and carbon in case of AtomicNoTexture
	 * @param opacity
	 */
	public void updateTexture(StereoMolecule mol, int textureMode, Color neutralColor, double opacity) {
		// list coordinates for every pixel in the image
		if (textureMode <= SURFACE_COLOR_PLAIN) {
			mTexture = null;
			getTexCoords().clear();
			getTexCoords().addAll(0, 0);

			ObservableIntegerArray faces = getFaces();
			if (USE_NORMALS) {
				for (int i=0; i<faces.size(); i+=9) {
					faces.set(i + 2, 0);
					faces.set(i + 5, 0);
					faces.set(i + 8, 0);
				}
			}
			else {
				for (int i=0; i<faces.size(); i+=6) {
					faces.set(i + 1, 0);
					faces.set(i + 3, 0);
					faces.set(i + 5, 0);
				}
			}
		}
		else if (textureMode == SURFACE_COLOR_ATOMIC_NOS) {
			mTexture = new AtomicNoTexture(this, mol, neutralColor, opacity, getSurfaceSurplus());
			mTexture.applyToSurface();
		}
		else {
			mTexture = new PropertyTexture(this, mol, textureMode, opacity, getSurfaceSurplus());
			mTexture.applyToSurface();
			}
		}
	}

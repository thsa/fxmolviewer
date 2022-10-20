package org.openmolecules.mesh;

import com.actelion.research.chem.StereoMolecule;

import java.util.ArrayList;

public class SurfaceAreaAndVolumeCalculator implements MeshBuilder {
	public static final int MODE_AREA = 1;
	public static final int MODE_VOLUME = 2;
	public static final int MODE_AREA_AND_VOLUME = MODE_AREA | MODE_VOLUME;

	private static final int POINT_BLOCK_SIZE = 1024;
	private static final float VOXEL_SIZE = 0.4f;
	private static final float PROBE_SIZE = MoleculeSurfaceAlgorithm.DEFAULT_PROBE_SIZE;
	private static final int SURFACE_TYPE = MoleculeSurfaceAlgorithm.CONNOLLY;

	private ArrayList<float[]> mPointBlockList;
	private float mSurfaceArea,mSurfaceVolume;
	private int mPointCount;

	public SurfaceAreaAndVolumeCalculator(StereoMolecule mol, int mode) {
		mSurfaceArea = 0f;
		mPointBlockList = new ArrayList<>();
		MoleculeSurfaceAlgorithm algorithm = new MoleculeSurfaceAlgorithm(VOXEL_SIZE, this);
		float[] grid = algorithm.calculateGrid(mol, SURFACE_TYPE, PROBE_SIZE);
		if ((mode & MODE_VOLUME) != 0)
			mSurfaceVolume = 0.98f * algorithm.calculateVolume(grid);
		if ((mode & MODE_AREA) != 0)
			algorithm.polygonise(grid);
		}

	@Override
	public int addPoint(float x, float y, float z) {
		int h = mPointCount / POINT_BLOCK_SIZE;
		int l = (mPointCount % POINT_BLOCK_SIZE) * 3;
		if (mPointBlockList.size() == h)
			mPointBlockList.add(new float[3*POINT_BLOCK_SIZE]);
		float[] p = mPointBlockList.get(h);
		p[l] = x;
		p[l+1] = y;
		p[l+2] = z;
		mPointCount++;
		return mPointCount-1;
		}

	@Override
	public void addTriangle(int i1, int i2, int i3) {
		int h,l;
		float[] p;

		h = i1 / POINT_BLOCK_SIZE;
		l = (i1 % POINT_BLOCK_SIZE) * 3;
		p = mPointBlockList.get(h);
		float x1 = p[l];
		float y1 = p[l+1];
		float z1 = p[l+2];

		h = i2 / POINT_BLOCK_SIZE;
		l = (i2 % POINT_BLOCK_SIZE) * 3;
		p = mPointBlockList.get(h);
		float x2 = p[l];
		float y2 = p[l+1];
		float z2 = p[l+2];

		h = i3 / POINT_BLOCK_SIZE;
		l = (i3 % POINT_BLOCK_SIZE) * 3;
		p = mPointBlockList.get(h);
		float x3 = p[l];
		float y3 = p[l+1];
		float z3 = p[l+2];

		float aSquare = (x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1);
		float bSquare = (x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1);
		float cSquare = (x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2);
		float t = aSquare+bSquare-cSquare;
		float area = 0.25f*(float)Math.sqrt(4*aSquare*bSquare-t*t);
		mSurfaceArea += area;
		}

	@Override
	public void getPoint(int index, float[] xyz) {
		int h = index / POINT_BLOCK_SIZE;
		int l = (index % POINT_BLOCK_SIZE) * 3;
		float[] p = mPointBlockList.get(h);
		xyz[0] = p[l];
		xyz[1] = p[l+1];
		xyz[2] = p[l+2];
		}

	public float getVolume() {
		return mSurfaceVolume;
		}

	public float getArea() {
		return mSurfaceArea;
		}
	}

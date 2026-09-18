package org.openmolecules.fx.viewer3d.interactions;

import com.actelion.research.chem.StereoMolecule;
import org.openmolecules.mesh.SmoothMarchingCubesAlgorithm;

public class V3DInteractionMeshAlgorithm extends SmoothMarchingCubesAlgorithm {
	public static final float VOXEL_SIZE = 0.1f;	// angstrom

	private int mGridSizeX,mGridSizeY,mGridSizeZ;
	private float mXOffset,mYOffset,mZOffset;

	public V3DInteractionMeshAlgorithm(V3DInteractionMesh meshData) {
		super(meshData, VOXEL_SIZE);
	}

	public void build(InteractionVolumeBuilder volumeBuilder, StereoMolecule mol, int atom, int pType) {
		float[] grid = calculateGrid(volumeBuilder, mol, atom);
		volumeBuilder.buildVolume(grid, mGridSizeX, mGridSizeY, mGridSizeZ, mXOffset, mYOffset, mZOffset, atom, pType);
		polygonise(grid, mGridSizeX, mGridSizeY, mGridSizeZ, 1.3f);
	}

	public float[] calculateGrid(InteractionVolumeBuilder volumeBuilder, StereoMolecule mol, int atom) {
		float r = volumeBuilder.getExtend(atom);
		float x = (float)mol.getAtomX(atom);
		float y = (float)mol.getAtomY(atom);
		float z = (float)mol.getAtomZ(atom);
		float xmin = x - r;
		float xmax = x + r;
		float ymin = y - r;
		float ymax = y + r;
		float zmin = z - r;
		float zmax = z + r;
		float xsize = xmax - xmin;
		float ysize = ymax - ymin;
		float zsize = zmax - zmin;

		mGridSizeX = (int)(xsize/VOXEL_SIZE+1f);	// 3f instead of 2f to avoid edge effects
		mGridSizeY = (int)(ysize/VOXEL_SIZE+1f);
		mGridSizeZ = (int)(zsize/VOXEL_SIZE+1f);

		mXOffset = xmin - ((mGridSizeX-1)*VOXEL_SIZE - xsize) / 2f;
		mYOffset = ymin - ((mGridSizeY-1)*VOXEL_SIZE - ysize) / 2f;
		mZOffset = zmin - ((mGridSizeZ-1)*VOXEL_SIZE - zsize) / 2f;

		setOffset(mXOffset, mYOffset, mZOffset);

		// 3-dimensional voxel grid with values on voxel corners, where the voxels touch.
		// The coordinates within the voxel box range from 0 ... sx-1, (sy-1, sz-1)
		return new float[mGridSizeX*mGridSizeY*mGridSizeZ];
	}
}

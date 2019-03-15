package org.openmolecules.fx.surface;

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.Conformer;
import org.openmolecules.fx.viewer3d.V3DMolecule;

public class RemovedAtomSurfaceCutter extends SurfaceCutter {
	private Conformer mConformer;
	private StereoMolecule mMol;

	public RemovedAtomSurfaceCutter(V3DMolecule fxmol) {
		mConformer = fxmol.getConformer();
		mMol = mConformer.getMolecule();
	}

	@Override
	protected boolean isVertexToBeRemoved(float x, float y, float z) {
		float minSquareDistance = Float.MAX_VALUE;
		boolean isClosestAtomMarked = false;

		for (int atom=0; atom<mMol.getAllAtoms(); atom++) {
			float dx = x - (float)mConformer.getX(atom);
			float sdx = dx * dx;
			if (sdx < minSquareDistance) {
				float dy = y - (float)mConformer.getY(atom);
				float sdy = dy * dy;
				if (sdy < minSquareDistance) {
					float dz = z - (float)mConformer.getZ(atom);
					float sdz = dz * dz;
					if (sdz < minSquareDistance) {
						float squareDistance = sdx + sdy + sdz;
						if (squareDistance < minSquareDistance) {
							minSquareDistance = squareDistance;
							isClosestAtomMarked = mMol.isMarkedAtom(atom);
						}
					}
				}
			}
		}
		return isClosestAtomMarked;
	}

	@Override
	protected void addCutPosition(float xi, float yi, float zi, float xo, float yo, float zo, float[] location) {
		location[0] += (xi+xo)/2;
		location[1] += (yi+yo)/2;
		location[2] += (zi+zo)/2;
	}
}

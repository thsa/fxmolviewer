package org.openmolecules.fx.viewer3d.interactions;

import com.actelion.research.chem.interactions.InteractionPoint;
import org.openmolecules.fx.viewer3d.V3DMolecule;

public class V3DInteractionPoint extends InteractionPoint {

	private final V3DMolecule mFXMol;

	public V3DInteractionPoint(V3DMolecule fxmol, int[] atoms, int type) {
		super(fxmol.getMolecule(), atoms, type);
		mFXMol = fxmol;
	}

	public V3DInteractionPoint(V3DMolecule fxmol, int atom, int type) {
		super(fxmol.getMolecule(), atom, type);
		mFXMol = fxmol;
	}

	public V3DMolecule getFXMol() {
		return mFXMol;
	}

	public void updateCoordinates() {
		// TODO update ring centers etc
	}
}

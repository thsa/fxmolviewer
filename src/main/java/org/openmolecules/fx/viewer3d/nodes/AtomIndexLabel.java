package org.openmolecules.fx.viewer3d.nodes;

import com.actelion.research.chem.Coordinates;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;
import org.openmolecules.fx.viewer3d.MolCoordinatesChangeListener;
import org.openmolecules.fx.viewer3d.TransformationListener;
import org.openmolecules.fx.viewer3d.V3DRotatableGroup;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.render.MoleculeArchitect;

public class AtomIndexLabel {
	private V3DRotatableGroup mWorld;
	private NodeDetail mNodeDetail;
	private NonRotatingLabel mLabel;
	private V3DMolecule mFXMol;
	private MolCoordinatesChangeListener mMCCL;
	private TransformationListener mTL;

	public AtomIndexLabel(V3DRotatableGroup world, V3DMolecule fxmol, NodeDetail detail) {
		mWorld = world;
		mFXMol = fxmol;
		mNodeDetail = detail;
		int atom = detail.getAtom();
		Coordinates c = fxmol.getMolecule().getAtomCoordinates(atom);
		Point3D p = new Point3D(c.x, c.y, c.z);
		Color color = MoleculeArchitect.getAtomColor(fxmol.getMolecule().getAtomicNo(atom), 1.0);
		mLabel = NonRotatingLabel.create(mFXMol, Integer.toString(atom), p, color);

		mTL = () -> updateAtomLabel();
		mMCCL = () -> updateAtomLabel();

		fxmol.addMoleculeCoordinatesChangeListener(mMCCL);
		world.addRotationListener(mTL);
	}

	public void remove() {
		mFXMol.removeMoleculeCoordinatesChangeListener(mMCCL);
		mWorld.removeRotationListener(mTL);
		mLabel.remove(mFXMol);
	}

	private void updateAtomLabel() {
		int atom = mNodeDetail.getAtom();
		Coordinates wc =  mFXMol.getMolecule().getAtomCoordinates(atom);
		mLabel.update(new Point3D(wc.x, wc.y, wc.z), Integer.toString(atom));
	}
}

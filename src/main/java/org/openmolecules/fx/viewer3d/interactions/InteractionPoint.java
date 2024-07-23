package org.openmolecules.fx.viewer3d.interactions;

import com.actelion.research.calc.SingularValueDecomposition;
import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.StereoMolecule;
import org.openmolecules.fx.viewer3d.V3DMolecule;

public class InteractionPoint {

	private final V3DMolecule mFXMol;
	private final StereoMolecule mMol;
	private final int[] mAtoms;
	private final int mType;

	public InteractionPoint(V3DMolecule fxmol, int[] atoms, int type) {
		mFXMol = fxmol;
		mMol = fxmol.getMolecule();
		mAtoms = atoms;
		mType = type;
	}

	public InteractionPoint(V3DMolecule fxmol, int atom, int type) {
		mFXMol = fxmol;
		mMol = fxmol.getMolecule();
		mAtoms = new int[1];
		mAtoms[0] = atom;
		mType = type;
	}

	public V3DMolecule getFXMol() {
		return mFXMol;
	}

	public int getType() {
		return mType;
	}

	public StereoMolecule getMol() {
		return mFXMol.getMolecule();
	}

	public int getAtom() {
		return mAtoms[0];
	}

	public int[] getAtoms() {
		return mAtoms;
	}

	public Coordinates getCenter() {
		if (mAtoms.length == 1)
			return mMol.getCoordinates(mAtoms[0]);

		Coordinates c = new Coordinates();
		for (int atom : mAtoms)
			c.add(mMol.getAtomCoordinates(atom));
		return c.scale(1.0 / mAtoms.length);
	}

	public void updateCoordinates() {
		// TODO update ring centers etc
	}

	public Coordinates calculatePlaneNormal() {
		Coordinates cog = new Coordinates();
		for (int mAtom : mAtoms)
			cog.add(mMol.getCoordinates(mAtom));
		cog.scale(1.0 / mAtoms.length);

		double[][] coords = new double[mAtoms.length][3];
		for (int i=0; i<mAtoms.length; i++) {
			coords[i][0] = mMol.getAtomX(mAtoms[i]) - cog.x;
			coords[i][1] = mMol.getAtomY(mAtoms[i]) - cog.y;
			coords[i][2] = mMol.getAtomZ(mAtoms[i]) - cog.z;
		}

		double[][] squareMatrix = new double[3][3];
		for (int i=0; i<mAtoms.length; i++)
			for (int j=0; j<3; j++)
				for (int k=0; k<3; k++)
					squareMatrix[j][k] += coords[i][j] * coords[i][k];

		SingularValueDecomposition svd = new SingularValueDecomposition(squareMatrix, null, null);
		double[] S = svd.getSingularValues();
		int minIndex = 0;
		for (int i=1; i<3; i++)
			if (S[i] < S[minIndex])
				minIndex = i;

		Coordinates n = new Coordinates();
		double[][] U = svd.getU();
		n.x = U[0][minIndex];
		n.y = U[1][minIndex];
		n.z = U[2][minIndex];
		return n;
	}

}

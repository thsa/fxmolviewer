package org.openmolecules.render;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.StereoMolecule;

import java.util.ArrayList;

public class RibbonSideChainConstructionFilter extends MaskConstructionFilter {
	private static final double MAX_DISTANCE = 3.8;
	/**
	 * Creates a construction filter for a protein with known ligand that marks all
	 * protein backbone atoms and all side chains of which atom least one atom is in
	 * a given distance to ligand atom.
	 * @param protein
	 * @param ligands
	 * @param isBackboneAtom
	 */
	public RibbonSideChainConstructionFilter(StereoMolecule protein, ArrayList<StereoMolecule> ligands, boolean[] isBackboneAtom) {
		super(protein, createMask(protein, ligands, isBackboneAtom));
	}

	/**
	 * From all protein atoms that are within a given distance to the ligand,
	 * walk the graph towards the backbone and mark all seen atoms including
	 * all backbone atoms.
	 * @param protein
	 * @param ligands
	 * @param isBackboneAtom
	 * @return
	 */
	private static boolean[] createMask(StereoMolecule protein, ArrayList<StereoMolecule> ligands, boolean[] isBackboneAtom) {
		protein.ensureHelperArrays(StereoMolecule.cHelperNeighbours);
		boolean[] atomMask = new boolean[protein.getAllAtoms()];

		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double minZ = Double.MAX_VALUE;
		double maxX = -Double.MAX_VALUE;
		double maxY = -Double.MAX_VALUE;
		double maxZ = -Double.MAX_VALUE;

		for (StereoMolecule ligand : ligands) {
			for (int i=0; i<ligand.getAllAtoms(); i++) {
				Coordinates c = ligand.getAtomCoordinates(i);
				if (minX > c.x)
					minX = c.x;
				if (minY > c.y)
					minY = c.y;
				if (minZ > c.z)
					minZ = c.z;
				if (maxX < c.x)
					maxX = c.x;
				if (maxY < c.y)
					maxY = c.y;
				if (maxZ < c.z)
					maxZ = c.z;
			}
		}

		double squareMaxDistance = MAX_DISTANCE * MAX_DISTANCE;

		int[] graphAtom = new int[protein.getAllAtoms()];

		for (int ap=0; ap<protein.getAllAtoms(); ap++) {
			if (!atomMask[ap]) {
				Coordinates cp = protein.getAtomCoordinates(ap);

				if (cp.x < minX - MAX_DISTANCE
				 || cp.x > maxX + MAX_DISTANCE
				 || cp.y < minY - MAX_DISTANCE
				 || cp.y > maxY + MAX_DISTANCE
				 || cp.z < minZ - MAX_DISTANCE
				 || cp.z > maxZ + MAX_DISTANCE)
					continue;

				for (StereoMolecule ligand : ligands) {
					for (int i=0; i<ligand.getAllAtoms(); i++) {
						Coordinates cl = ligand.getAtomCoordinates(i);
						double dx = Math.abs(cl.x - cp.x);
						if (dx < MAX_DISTANCE) {
							double dy = Math.abs(cl.y - cp.y);
							if (dy < MAX_DISTANCE) {
								double dz = Math.abs(cl.z - cp.z);
								if (dz < MAX_DISTANCE) {
									double squareDistance = dx*dx+dy*dy+dz*dz;
									if (squareDistance < squareMaxDistance) {
										atomMask[ap] = true;
										graphAtom[0] = ap;
										int current = 0;
										int highest = 0;
										while (current <= highest) {
											int parent = graphAtom[current];
											boolean parentIsBackbondAtom = (parent < isBackboneAtom.length && isBackboneAtom[parent]);
											for (int j=0; j<protein.getAllConnAtoms(parent); j++) {
												int candidate = protein.getConnAtom(parent, j);
												if (!atomMask[candidate]) {
													boolean candidateIsBackbondAtom = (candidate < isBackboneAtom.length && isBackboneAtom[candidate]);
													// we don't want to extend from backbone atoms to non-backbone atoms
													if (!parentIsBackbondAtom || candidateIsBackbondAtom) {
														atomMask[candidate] = true;
														if (!candidateIsBackbondAtom)
															graphAtom[++highest] = candidate;
//														else {	// mark two layers of neighbour backbone atoms
//															for (int k=0; k<protein.getConnAtoms(candidate); k++) {
//																int connAtom = protein.getConnAtom(candidate, k);
//																if (isBackboneAtom[connAtom]) {
//																	atomMask[connAtom] = true;
//																	for (int l=0; l<protein.getConnAtoms(connAtom); l++) {
//																		int nextConn = protein.getConnAtom(connAtom, l);
//																		if (isBackboneAtom[nextConn])
//																			atomMask[nextConn] = true;
//																	}
//																}
//															}
//														}
													}
												}
											}
											current++;
										}
									}
								}
							}
						}
					}
				}
			}
		}

		return atomMask;
	}
}

package org.openmolecules.fx.viewer3d.interactions.drugscore;

import com.actelion.research.chem.AtomFunctionAnalyzer;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;

public class DrugScoreAtomClassifier implements InteractionAtomClassifier {
	public final static int TYPE_UNKNOWN = 0;
	public final static int TYPE_C_AR = 1;
	public final static int TYPE_C_SP1 = 2;
	public final static int TYPE_C_SP2 = 3;
	public final static int TYPE_C_SP3 = 4;
	public final static int TYPE_C_CAT = 5;
	public final static int TYPE_N_AR = 6;
	public final static int TYPE_N_AM = 7;
	public final static int TYPE_N_PL3 = 8;
	public final static int TYPE_N_SP1 = 9;
	public final static int TYPE_N_SP2 = 10;
	public final static int TYPE_N_SP3 = 11;
	public final static int TYPE_N_PLUS = 12;
	public final static int TYPE_O_AR = 13;
	public final static int TYPE_O_CO2 = 14;
	public final static int TYPE_O_SP2 = 15;
	public final static int TYPE_O_SP3 = 16;
	public final static int TYPE_P_AR = 17;
	public final static int TYPE_P_O1 = 18;
	public final static int TYPE_P_O2 = 19;
	public final static int TYPE_P_SP2 = 20;
	public final static int TYPE_P_SP3 = 21;
	public final static int TYPE_S_AR = 22;
	public final static int TYPE_S_O1 = 23;
	public final static int TYPE_S_O2 = 24;
	public final static int TYPE_S_SP2 = 25;
	public final static int TYPE_S_SP3 = 26;
	public final static int TYPE_F = 27;
	public final static int TYPE_CL = 28;
	public final static int TYPE_BR = 29;
	public final static int TYPE_I = 30;
	public final static int TYPE_MET = 31;

	public final static String[] TYPE_NAME = {
			"?", "C.ar", "C.1", "C.2", "C.3", "C.cat",
			"N.ar", "N.am", "N.pl3", "N.1", "N.2", "N.3", "N.4",
			"O.ar", "O.CO2", "O.2", "O.3",
			"P.ar", "P.O1", "P.O2", "P.SP2", "P.SP3",
			"S.ar", "S.O1","S.O2", "S.SP2", "S.SP3",
			"F", "Cl", "Br", "I", "Met"
	};

	public static String typeName(int type) {
		return TYPE_NAME[type];
	}

	@Override
	public String getAtomTypeName(int type) {
		return TYPE_NAME[type];
	}

	@Override
	public int[] classifyAtoms(StereoMolecule mol) {
		mol.ensureHelperArrays(Molecule.cHelperRings);
		int[] type = new int[mol.getAtoms()];
		for (int atom=0; atom<mol.getAtoms(); atom++) {
			if (mol.getAtomicNo(atom) == 6) {
				if (mol.isAromaticAtom(atom)) {
					type[atom] = TYPE_C_AR;
				}
				else if (mol.getAtomPi(atom) == 2) {
					type[atom] = TYPE_C_SP1;
				}
				else if (isAmidineOrGuanidineCarbon(mol, atom)) {
					type[atom] = TYPE_C_CAT;
				}
				else if (mol.getAtomPi(atom) == 1) {
					type[atom] = TYPE_C_SP2;
				}
				else {
					type[atom] = TYPE_C_SP3;
				}
			}
			else if (mol.getAtomicNo(atom) == 7) {
				if (mol.isAromaticAtom(atom)) {
					type[atom] = TYPE_N_AR;
				}
				else if (isAmideNitrogen(mol, atom)) {
					type[atom] = TYPE_N_AM;
				}
				else if (isAmidineOrGuanidineNitrogen(mol, atom)) {
					type[atom] = TYPE_N_PL3;
				}
				else if (mol.getAtomPi(atom) == 2) {
					type[atom] = TYPE_N_SP1;
				}
				else if (mol.getAtomPi(atom) == 1) {
					type[atom] = TYPE_N_SP2;
				}
				else {
					type[atom] = AtomFunctionAnalyzer.isBasicNitrogen(mol, atom) ? TYPE_N_PLUS : TYPE_N_SP3;
				}
			}
			else if (mol.getAtomicNo(atom) == 8) {
				if (mol.isAromaticAtom(atom)) {
					type[atom] = TYPE_O_AR;	// not in original DrugScore2018 set
				}
				else if (mol.getConnAtoms(atom) == 1) {
					int connAtom = mol.getConnAtom(atom, 0);
					if (mol.getAtomPi(connAtom) != 0) {
						for (int i=0; i<mol.getConnAtoms(connAtom); i++) {
							int connConn = mol.getConnAtom(connAtom, i);
							if (connConn != atom
							 && mol.getConnAtoms(connConn) == 1
							 && mol.getAtomicNo(connConn) == 8) {
								type[atom] = TYPE_O_CO2;
								break;
							}
						}
					}
				}
				if (type[atom] == 0) {
					type[atom] = (mol.getAtomPi(atom) == 0) ? TYPE_O_SP3 : TYPE_O_SP2;
				}
			}
			else if (mol.getAtomicNo(atom) == 9) {
				type[atom] = TYPE_F;
			}
			else if (mol.getAtomicNo(atom) == 15) {
				if (mol.isAromaticAtom(atom)) {
					type[atom] = TYPE_P_AR;	// not in original DrugScore2018 set
				}
				else {
					int count = 0;
					for (int i=0; i<mol.getConnAtoms(atom); i++) {
						int connAtom = mol.getConnAtom(atom, i);
						if (mol.getAtomicNo(connAtom) == 8
								&& mol.getConnAtoms(connAtom) == 1)
							count++;
					}
					if (count >= 2)
						type[atom] = TYPE_P_O2;	// not in original DrugScore2018 set
					else if (count == 1)
						type[atom] = TYPE_P_O1; 	// not in original DrugScore2018 set
					else
						type[atom] = (mol.getAtomPi(atom) == 0) ? TYPE_P_SP3 : TYPE_P_SP2;	// 2nd not in original DrugScore2018 set
				}
			}
			else if (mol.getAtomicNo(atom) == 16) {
				if (mol.isAromaticAtom(atom)) {
					type[atom] = TYPE_S_AR;	// not in original DrugScore2018 set
				}
				else {
					int count = 0;
					for (int i=0; i<mol.getConnAtoms(atom); i++) {
						int connAtom = mol.getConnAtom(atom, i);
						if (mol.getAtomicNo(connAtom) == 8
						 && mol.getConnAtoms(connAtom) == 1)
							count++;
					}
					if (count >= 2)
						type[atom] = TYPE_S_O2;
					else if (count == 1)
						type[atom] = TYPE_S_O1;
					else
						type[atom] = (mol.getAtomPi(atom) == 0) ? TYPE_S_SP3 : TYPE_S_SP2;
				}
			}
			else if (mol.getAtomicNo(atom) == 17) {
				type[atom] = TYPE_CL;
			}
			else if (mol.getAtomicNo(atom) == 35) {
				type[atom] = TYPE_BR;
			}
			else if (mol.getAtomicNo(atom) == 53) {
				type[atom] = TYPE_I;
			}
			else if (mol.isMetalAtom(atom)) {
				type[atom] = TYPE_MET;
			}
			else {
				type[atom] = TYPE_UNKNOWN;
			}
		}

		return type;
	}

	private boolean isAmidineOrGuanidineCarbon(StereoMolecule mol, int atom) {
		if (mol.getAtomPi(atom) != 1)
			return false;

		int nitrogenCount = 0;
		int piBondCount = 0;
		for (int i=0; i<mol.getConnAtoms(atom); i++) {
			if (mol.getAtomicNo(mol.getConnAtom(atom, i)) == 7) {
				nitrogenCount++;
				if (mol.getConnBondOrder(atom, i) > 1)
					piBondCount++;
			}
		}
		return nitrogenCount >= 2 && piBondCount == 1;
	}

	private boolean isAmideNitrogen(StereoMolecule mol, int atom) {
		for (int i=0; i<mol.getConnAtoms(atom); i++) {
			int connAtom = mol.getConnAtom(atom, i);
			if (mol.getConnBondOrder(atom, i) == 1) {
				for (int j=0; j<mol.getConnAtoms(connAtom); j++) {
					if (mol.getConnBondOrder(connAtom, j) == 2
					 && mol.getAtomicNo(mol.getConnAtom(connAtom, j)) == 8)
						return true;	// we include also sulfonyl-amides
				}
			}
		}
		return false;
	}

	private boolean isAmidineOrGuanidineNitrogen(StereoMolecule mol, int atom) {
		for (int i=0; i<mol.getConnAtoms(atom); i++) {
			int connAtom = mol.getConnAtom(atom, i);
			if (mol.getAtomicNo(connAtom) == 6
			 && isAmidineOrGuanidineCarbon(mol, connAtom))
				return true;
			}
		return false;
	}
}

/*
 * Copyright 2017 Thomas Sander, Therwilerstrasse 41, CH-4153 Reinach, Switzerland
 *
 * This file is part of openmolecules.org's 3D-Molecule-Viewer.
 *
 * 3D-Molecule-Viewer is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * 3D-Molecule-Viewer is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with 3D-Molecule-Viewer.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Thomas Sander
 */

package org.openmolecules.pdb;

import com.actelion.research.chem.*;
import org.rcsb.mmtf.api.StructureDataInterface;
import org.rcsb.mmtf.dataholders.MmtfStructure;
import org.rcsb.mmtf.decoder.GenericDecoder;
import org.rcsb.mmtf.decoder.ReaderUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.TreeMap;

@Deprecated     // MMTF is not supported anymore by the PDB database from July 2nd, 2024 (a pity, because for us,
				// it is much more useful than its more bulky replacement binaryCIF, because it included bonds!
public class MMTFParser {
	public static final int MODE_DONT_SPLIT = 0;
	public static final int MODE_SPLIT_MODELS = 1;
	public static final int MODE_SPLIT_CHAINS = 2;

	private static final int[] BOND_TYPE = { 0, 1, 2, 4, 4 };   // quadruple bond is not supported and generates a triple bond
	private static TreeMap<String,Integer> sAtomicNoMap;

	public static File saveMMTF(String name, String filepath) {
		File file = new File(filepath);
		if (file.exists())
			return null;

		try {
			byte[] bytes = ReaderUtils.getByteArrayFromUrl(name);
			Files.write(file.toPath(), bytes);
			return file;
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
	}

	public static Molecule3D[] getStructureFromName(String name, int mode) {
		try {
			MmtfStructure mmtfData = ReaderUtils.getDataFromUrl(name);
			return getStructure(mmtfData, name, mode);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
	}

	public static Molecule3D[] getStructureFromFile(String path, String pdbName, int mode) {
		try {
			MmtfStructure mmtfData = ReaderUtils.getDataFromFile(Paths.get(path));
			return getStructure(mmtfData, pdbName, mode);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
	}

	private static Molecule3D[] getStructure(MmtfStructure mmtfData, String name, int mode) {
		// Decode message pack byte array as flat arrays holding the structural data
		StructureDataInterface sdi = new GenericDecoder(mmtfData);

		int[] modelChainCount = sdi.getChainsPerModel();

		int moleculeCount = 0;
		if (mode == MODE_DONT_SPLIT) {
			moleculeCount = 1;
		}
		else if (mode == MODE_SPLIT_MODELS) {
			moleculeCount = modelChainCount.length;
		}
		else if (mode == MODE_SPLIT_CHAINS) {
			for (int chainCount:modelChainCount)
				moleculeCount += chainCount;
		}

//		int atoms = sdi.getNumAtoms();
//		int bonds = sdi.getNumBonds();

		Molecule3D[] molecule = new Molecule3D[moleculeCount];
		int[] molAtomOffset = new int[moleculeCount];

		int destMol = -1;
		int destAtom = 0;

		int sourceAtom = 0;

		int chainIndex = 0;
		int groupIndex = 0;

		// traverse models

		float[] x = sdi.getxCoords();
		float[] y = sdi.getyCoords();
		float[] z = sdi.getzCoords();

		for (int model=0; model<modelChainCount.length; model++) {
			String modelName = name+(sdi.getNumModels() == 1 ? "" : " " + Integer.toString(model));

			if (mode == MODE_SPLIT_MODELS) {
				destMol++;
				destAtom = 0;

				int atomCount = 0;
				int bondCount = 0;
				int localChainIndex = chainIndex;
				int localGroupIndex = groupIndex;
				for (int chain=0; chain<modelChainCount[model]; chain++) {
					int groupCount = sdi.getGroupsPerChain()[localChainIndex];
					for (int group=0; group<groupCount; group++) {
						int groupType = sdi.getGroupTypeIndices()[localGroupIndex];
						atomCount += sdi.getNumAtomsInGroup(groupType);
						bondCount += sdi.getGroupBondOrders(groupType).length;
						localGroupIndex++;
					}
					localChainIndex++;
				}

				molecule[destMol] = new Molecule3D(atomCount, bondCount);
				molecule[destMol].setName(modelName);
				molAtomOffset[destMol] = sourceAtom;
			}

			for (int chain=0; chain<modelChainCount[model]; chain++) {
				int groupCount = sdi.getGroupsPerChain()[ chainIndex ];

				if (mode == MODE_SPLIT_CHAINS) {
					destMol++;
					destAtom = 0;

					int atomCount = 0;
					int bondCount = 0;
					int localGroupIndex = groupIndex;
					for (int group=0; group<groupCount; group++) {
						int groupType = sdi.getGroupTypeIndices()[localGroupIndex];
						atomCount += sdi.getNumAtomsInGroup(groupType);
						bondCount += sdi.getGroupBondOrders(groupType).length;
						localGroupIndex++;
					}

					molecule[destMol] = new Molecule3D(atomCount, bondCount);
					molecule[destMol].setName(modelName + " " + sdi.getChainNames()[chainIndex]);
					molAtomOffset[destMol] = sourceAtom;
				}

/*				System.out.println("  Chain:"+chainIndex);
				System.out.println(sdi.getChainIds()[chainIndex]);
				System.out.println(sdi.getChainNames()[chainIndex]);
*/
				// traverse groups
				for (int group=0; group<groupCount; group++) {
					int groupType = sdi.getGroupTypeIndices()[ groupIndex ];

					boolean isLigand = "NON-POLYMER".equals(sdi.getGroupChemCompType(groupType));

/*					if (!set.contains(gt)) {
						set.add(gt);
						System.out.println(gt);
						System.out.println("  Group:" + g);
						System.out.println(sdi.getGroupIds()[g]);
						System.out.println(sdi.getInsCodes()[g]);
						System.out.println(sdi.getSecStructList()[g]);
						System.out.println(sdi.getGroupSequenceIndices()[g]);
						System.out.println(sdi.getGroupTypeIndices()[g]);
						System.out.println(sdi.getGroupName(groupType));
						System.out.println(sdi.getGroupSingleLetterCode(groupType));
						System.out.println(sdi.getGroupChemCompType(groupType));
						}*/

					int atomOffset = destAtom;

					int atomCount = sdi.getNumAtomsInGroup(groupType);

					for (int i=0; i<atomCount; i++) {
						molecule[destMol].addAtom(x[sourceAtom], y[sourceAtom], z[sourceAtom]);
//  		            System.out.println(sdi.getbFactors()[sourceAtom]);
//	                	System.out.println(sdi.getAtomIds()[sourceAtom]);
//		                System.out.println(sdi.getAltLocIds()[sourceAtom]);
//		                System.out.println(sdi.getOccupancies()[sourceAtom]);
						molecule[destMol].setAtomCharge(destAtom, sdi.getGroupAtomCharges(groupType)[i]);
						molecule[destMol].setAtomicNo(destAtom, getAtomicNoFromLabel(sdi.getGroupElementNames(groupType)[i]));
						molecule[destMol].setAtomMarker(destAtom, isLigand);
//						System.out.println(sdi.getGroupAtomNames(groupType)[i]);    // k+1?
						sourceAtom++;
						destAtom++;
					}

					int[] gbi = sdi.getGroupBondIndices(groupType);
					int[] gbo = sdi.getGroupBondOrders(groupType);
					int bondCount = gbo.length;
					for (int i=0; i<bondCount; i++) {
						int atom1 = atomOffset + gbi[i*2];
						int atom2 = atomOffset + gbi[i*2+1];
						int order = BOND_TYPE[gbo[i]];
						molecule[destMol].addBond(atom1, atom2, order);
					}

					groupIndex++;
				}
				chainIndex++;
			}
		}

		// traverse inter-group bonds
		destMol = 0;
		int[] igbi = sdi.getInterGroupBondIndices();
		int[] igbo = sdi.getInterGroupBondOrders();
		for (int i=0; i<igbo.length; i++) {
			int atom1 = igbi[i*2];
			int atom2 = igbi[i*2+1];
			while (destMol<molAtomOffset.length-1 && molAtomOffset[destMol+1] <= atom1)
				destMol++;
			int order = BOND_TYPE[igbo[i]];
			molecule[destMol].addBond(atom1-molAtomOffset[destMol], atom2-molAtomOffset[destMol], order);
		}

		for (StereoMolecule mol:molecule)
			mol.setName((mol.getName() == null ? "" : mol.getName() + " ") + new MolecularFormula(mol).getFormula());

		for (StereoMolecule mol:molecule)
			addMissingCharges(mol); // in some molecules quarternary nitrogen atoms are not charged, e.g. 8BXH

		return molecule;
	}

	private static void addMissingCharges(StereoMolecule mol) {
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);
		for (int atom=0; atom<mol.getAtoms(); atom++)
			if (mol.getAtomicNo(atom) == 7
			 && mol.getOccupiedValence(atom) == 4
			 && mol.getAtomCharge(atom) == 0
			 && mol.getAtomRadical(atom) == 0)
				mol.setAtomCharge(atom, 1);
	}

	public static void centerMolecules(Molecule3D[] molecule) {
		Coordinates cog = new Coordinates();
		for (Molecule3D mol:molecule) {
			for (int atom = 0; atom<mol.getAllAtoms(); atom++)
				cog.add(mol.getAtomCoordinates(atom));
			cog.scale(1.0 / mol.getAllAtoms());
		}

		for (Molecule3D mol:molecule)
			for (int atom = 0; atom<mol.getAllAtoms(); atom++)
				mol.getAtomCoordinates(atom).sub(cog);
	}

	private static int getAtomicNoFromLabel(String label) {
		if (sAtomicNoMap == null) {
			sAtomicNoMap = new TreeMap<>();
			for (int i=1; i<104; i++)
				sAtomicNoMap.put(Molecule.cAtomLabel[i], i);
		}
		return sAtomicNoMap.get(label);
	}
}

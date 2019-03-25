package org.openmolecules.fx.viewer3d;

import java.util.ArrayList;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.AtomAssembler;
import com.actelion.research.chem.conf.BondLengthSet;
import com.actelion.research.chem.conf.Conformer;

import javafx.scene.Node;

public class V3DMoleculeModifier {
	
	public static void changeAtomElement(V3DMolecule v3dMol, int a, int atomicNo) {
		if(v3dMol.getConformer().getMolecule().getAtomicNo(a)==1)changeHydrogenAtomElement(v3dMol, a, atomicNo);
		else changeHeavyAtomElement(v3dMol, a, atomicNo);
		
	}
	public static void changeHydrogenAtomElement(V3DMolecule v3dMol, int a, int atomicNo) {
		StereoMolecule mol = v3dMolToStereoMol(v3dMol);
		ArrayList<Integer> bonds = new ArrayList<Integer>(); //bonds that will be rebuilt
		ArrayList<Node> nodesToBeDeleted = new ArrayList<>();
		for(Node node : v3dMol.getChildren()) {
			NodeDetail detail = (NodeDetail)node.getUserData();;
			if (detail != null && detail.isAtom()) {
				if(detail.getAtom()==a) nodesToBeDeleted.add(node);
			}
			else if (detail != null && detail.isBond() ) {
				int bond = detail.getBond();
				//for(int j=0;j<mol.getAllConnAtoms(a);j++) {
					if(bond==mol.getBond(a, mol.getConnAtom(a,0))) {
						nodesToBeDeleted.add(node);
						if(!bonds.contains(bond)) {
							bonds.add(bond);
						}
					}
				
			}
		}
		v3dMol.getChildren().removeAll(nodesToBeDeleted);
		
		mol.setAtomicNo(a, atomicNo); 
		//cleaning up the geometry
		int aa1 = mol.getConnAtom(a, 0);
		Coordinates c1 = mol.getCoordinates(a);
		Coordinates cc1 = mol.getCoordinates(aa1);
		int bond = mol.getBond(a, aa1);
		double lNew = BondLengthSet.getBondLength(BondLengthSet.getBondIndex(mol,bond));
		Coordinates v = c1.subC(cc1);
		double l = v.dist();
		Coordinates c1New=cc1.addC(v.scale(lNew/l));
		c1.x = c1New.x;
		c1.y = c1New.y;
		c1.z = c1New.z;

		int[] atomMap = mol.getHandleHydrogenMap();
		int[] bondMap = mol.getHandleHydrogenBondMap();
		a = atomMap[a]; //update index of atom
		for(int i=0;i<bonds.size();i++) {
			int oldIndex = bonds.get(i);
			bonds.set(i,bondMap[oldIndex]); //update indeces of bonds
		}
		mol.ensureHelperArrays(Molecule.cHelperRings); //now the indeces of bonds and atoms changed

		updateNodeIndeces(v3dMol,atomMap,bondMap);

		ArrayList<Integer> atoms = new ArrayList<Integer>();
		atoms.add(a);
		constructHydrogens(mol,new int[] {a},atoms,bonds);
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);
		v3dMol.setConformer(new Conformer(mol));
		V3DMoleculeBuilder builder = new V3DMoleculeBuilder(v3dMol);
		builder.buildMolecule(atoms,bonds);


	}
	
	public static void changeHeavyAtomElement(V3DMolecule v3dMol, int a, int atomicNo) {
		StereoMolecule mol = v3dMolToStereoMol(v3dMol);
		boolean[] hydrogensToBeDeleted = getHydrogensFromAtoms(mol,a);
		//the bonds from the atom to the hydrogens are deleted in the StereoMolecule as well as removed from the V3DMolecule
		boolean[] hydrogenBonds = new boolean[mol.getAllBonds()];
		//the bonds to heavy atoms are removed from the V3DMolecule and reconstructed
		boolean[] heavyAtomBonds = new boolean[mol.getAllBonds()];
		for(int i=0;i<mol.getAllConnAtoms(a);i++) {
			int aa = mol.getConnAtom(a, i);
			int b = mol.getBond(a, aa);
			if(mol.getAtomicNo(aa)==1) hydrogenBonds[b] = true;
			else heavyAtomBonds[b] = true;
			
		}
		ArrayList<Integer> bonds = new ArrayList<Integer>();
		ArrayList<Node> nodesToBeDeleted = new ArrayList<>();
		for(Node node : v3dMol.getChildren()) {
			NodeDetail detail = (NodeDetail)node.getUserData();;
			if (detail != null && detail.isAtom()) {
				int index = detail.getAtom();
				if(index==a) nodesToBeDeleted.add(node);
				else if(hydrogensToBeDeleted[index]) nodesToBeDeleted.add(node);

			}
			else if (detail != null && detail.isBond() ) {
				int bond = detail.getBond();
				if(heavyAtomBonds[bond] || hydrogenBonds[bond]) {
					nodesToBeDeleted.add(node);

					if(heavyAtomBonds[bond] && !bonds.contains(bond))bonds.add(bond);
					}
		}
		}
		v3dMol.getChildren().removeAll(nodesToBeDeleted);
		mol.setAtomicNo(a, atomicNo);
		int[] bondMap = mol.getDeleteAtomsBondMap(hydrogensToBeDeleted);
		int[] atomMap = mol.deleteAtoms(hydrogensToBeDeleted);
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);
		if(atomMap!=null && bondMap!=null ) {
		a = atomMap[a];
		for(int i=0;i<bonds.size();i++) {
			int oldIndex = bonds.get(i);
			bonds.set(i,bondMap[oldIndex]);
		}
		updateNodeIndeces(v3dMol,atomMap,bondMap);
		}
		ArrayList<Integer> atoms = new ArrayList<Integer>();
		atoms.add(a);
		constructHydrogens(mol,new int[] {a},atoms,bonds);
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);
		v3dMol.setConformer(new Conformer(mol));
		V3DMoleculeBuilder builder = new V3DMoleculeBuilder(v3dMol);
		builder.buildMolecule(atoms,bonds);
		
		
	}
	
	public static void drawBond(V3DMolecule v3dMol, int a, int b) {
		StereoMolecule mol = v3dMolToStereoMol(v3dMol);
		boolean[] hydrogensToBeDeleted = getHydrogensFromAtoms(mol,new int[] {a,b});
		//the bonds from the atom to the hydrogens are deleted in the StereoMolecule as well as removed from the V3DMolecule
		boolean[] hydrogenBonds = new boolean[mol.getAllBonds()];
		
		for(int i=0;i<mol.getAllConnAtoms(a);i++) {
			int aa = mol.getConnAtom(a, i);
			int bond = mol.getBond(a, aa);
			if(mol.getAtomicNo(aa)==1) hydrogenBonds[bond] = true;
			
		}
		for(int i=0;i<mol.getAllConnAtoms(b);i++) {
			int bb = mol.getConnAtom(b, i);
			int bond = mol.getBond(b, bb);
			if(mol.getAtomicNo(bb)==1) hydrogenBonds[bond] = true;
			
		}
		ArrayList<Integer> bonds = new ArrayList<Integer>();
		ArrayList<Integer> atoms = new ArrayList<Integer>();
		ArrayList<Node> nodesToBeDeleted = new ArrayList<>();
		for(Node node : v3dMol.getChildren()) {
			NodeDetail detail = (NodeDetail)node.getUserData();;
			if (detail != null && detail.isAtom()) {
				int index = detail.getAtom();
				if(hydrogensToBeDeleted[index]) nodesToBeDeleted.add(node);

			}
			else if (detail != null && detail.isBond() ) {
				int bond = detail.getBond();
				if(hydrogenBonds[bond]) {
					nodesToBeDeleted.add(node);

					}
			}
		}
		v3dMol.getChildren().removeAll(nodesToBeDeleted);
		
		int[] bondMap = mol.getDeleteAtomsBondMap(hydrogensToBeDeleted);
		int[] atomMap = mol.deleteAtoms(hydrogensToBeDeleted);
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);
		if(atomMap!=null && bondMap!=null ) {
		a = atomMap[a];
		b = atomMap[b];

		updateNodeIndeces(v3dMol,atomMap,bondMap);
		}
		mol.addBond(a, b);	
		atomMap = mol.getHandleHydrogenMap();
		bondMap = mol.getHandleHydrogenBondMap();
		if(atomMap!=null && bondMap!=null ) {
		mol.ensureHelperArrays(Molecule.cHelperRings);
		for(int i=0;i<bonds.size();i++) {
			int oldIndex = bonds.get(i);
			bonds.set(i,bondMap[oldIndex]);
		}
		updateNodeIndeces(v3dMol,atomMap,bondMap);
		}
		bonds.add(mol.getBond(a,b));
		constructHydrogens(mol,new int[] {a,b},atoms,bonds);
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);
		v3dMol.setConformer(new Conformer(mol));
		V3DMoleculeBuilder builder = new V3DMoleculeBuilder(v3dMol);
		builder.buildMolecule(atoms,bonds);

	}
	
	public static void deleteBond(V3DMolecule v3dMol, int bond) {
		System.out.println("delete bond");
		StereoMolecule mol = v3dMolToStereoMol(v3dMol);
		int a = mol.getBondAtom(0, bond);
		int b = mol.getBondAtom(1, bond);
		boolean[] hydrogensToBeDeleted = getHydrogensFromAtoms(mol,new int[] {a,b});
		//the bonds from the atom to the hydrogens are deleted in the StereoMolecule as well as removed from the V3DMolecule
		boolean[] hydrogenBonds = new boolean[mol.getAllBonds()];
		
		for(int i=0;i<mol.getAllConnAtoms(a);i++) {
			int aa = mol.getConnAtom(a, i);
			int bondTemp = mol.getBond(a, aa);
			if(mol.getAtomicNo(aa)==1) hydrogenBonds[bondTemp] = true;
			
		}
		for(int i=0;i<mol.getAllConnAtoms(b);i++) {
			int bb = mol.getConnAtom(b, i);
			int bondTemp = mol.getBond(b, bb);
			if(mol.getAtomicNo(bb)==1) hydrogenBonds[bondTemp] = true;
			
		}
		ArrayList<Integer> bonds = new ArrayList<Integer>();
		ArrayList<Integer> atoms = new ArrayList<Integer>();
		ArrayList<Node> nodesToBeDeleted = new ArrayList<>();
		for(Node node : v3dMol.getChildren()) {
			NodeDetail detail = (NodeDetail)node.getUserData();;
			if (detail != null && detail.isAtom()) {
				int index = detail.getAtom();
				if(hydrogensToBeDeleted[index]) nodesToBeDeleted.add(node);

			}
			else if (detail != null && detail.isBond() ) {
				int bondTemp = detail.getBond();
				if(hydrogenBonds[bondTemp] || bondTemp == bond) {
					nodesToBeDeleted.add(node);

					}
			}
		}
		v3dMol.getChildren().removeAll(nodesToBeDeleted);
		
		int[] bondMap = mol.getDeleteAtomsBondMap(hydrogensToBeDeleted);
		int[] atomMap = mol.deleteAtoms(hydrogensToBeDeleted);
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);
		if(atomMap!=null && bondMap!=null ) {
		a = atomMap[a];
		b = atomMap[b];

		updateNodeIndeces(v3dMol,atomMap,bondMap);
		}
		bondMap = new int[mol.getAllBonds()];
		int bondDest = 0;
		for (int bnd=0; bnd<bondMap.length; bnd++)
			bondMap[bnd] = bnd==bond ? -1 : bondDest++;
		mol.deleteBond(bond);	
		atomMap = mol.getHandleHydrogenMap();
		mol.ensureHelperArrays(Molecule.cHelperRings);
		if(atomMap!=null && bondMap!=null ) {
			updateNodeIndeces(v3dMol,atomMap,bondMap);
		}
		
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);
		constructHydrogens(mol,new int[] {a,b},atoms,bonds);
		v3dMol.setConformer(new Conformer(mol));
		V3DMoleculeBuilder builder = new V3DMoleculeBuilder(v3dMol);
		builder.buildMolecule(atoms,bonds);

	}
	
	public static void changeBondOrder(V3DMolecule v3dMol, int bond) {
		StereoMolecule mol = v3dMolToStereoMol(v3dMol);
		int a = mol.getBondAtom(0, bond);
		int b = mol.getBondAtom(1, bond);
		boolean[] hydrogensToBeDeleted = getHydrogensFromAtoms(mol,new int[] {a,b});
		//the bonds from the atom to the hydrogens are deleted in the StereoMolecule as well as removed from the V3DMolecule
		boolean[] hydrogenBonds = new boolean[mol.getAllBonds()];
		
		for(int i=0;i<mol.getAllConnAtoms(a);i++) {
			int aa = mol.getConnAtom(a, i);
			int bondTemp = mol.getBond(a, aa);
			if(mol.getAtomicNo(aa)==1) hydrogenBonds[bondTemp] = true;
			
		}
		for(int i=0;i<mol.getAllConnAtoms(b);i++) {
			int bb = mol.getConnAtom(b, i);
			int bondTemp = mol.getBond(b, bb);
			if(mol.getAtomicNo(bb)==1) hydrogenBonds[bondTemp] = true;
			
		}
		ArrayList<Integer> bonds = new ArrayList<Integer>();
		ArrayList<Integer> atoms = new ArrayList<Integer>();
		ArrayList<Node> nodesToBeDeleted = new ArrayList<>();
		for(Node node : v3dMol.getChildren()) {
			NodeDetail detail = (NodeDetail)node.getUserData();;
			if (detail != null && detail.isAtom()) {
				int index = detail.getAtom();
				if(hydrogensToBeDeleted[index]) nodesToBeDeleted.add(node);

			}
			else if (detail != null && detail.isBond() ) {
				int bondTemp = detail.getBond();
				if(hydrogenBonds[bondTemp] || bondTemp == bond) {
					nodesToBeDeleted.add(node);

					}
			}
		}
		v3dMol.getChildren().removeAll(nodesToBeDeleted);
		
		int[] bondMap = mol.getDeleteAtomsBondMap(hydrogensToBeDeleted);
		int[] atomMap = mol.deleteAtoms(hydrogensToBeDeleted);
		bonds.add(mol.getBond(a,b));
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);
		if(atomMap!=null && bondMap!=null ) {
		a = atomMap[a];
		b = atomMap[b];

		updateNodeIndeces(v3dMol,atomMap,bondMap);
		}
		int order = mol.getBondOrder(bond);
		switch(order) {
			case(1):
				mol.setBondOrder(bond, 2);
				break;
			case(2):
				mol.setBondOrder(bond, 3);
				break;
			case(3):
				mol.setBondOrder(bond, 1);
				break;
		}

		constructHydrogens(mol,new int[] {a,b},atoms,bonds);
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);
		v3dMol.setConformer(new Conformer(mol));
		V3DMoleculeBuilder builder = new V3DMoleculeBuilder(v3dMol);
		builder.buildMolecule(atoms,bonds);

	}
	
	public static void constructHydrogens(StereoMolecule mol, int[] ats, ArrayList<Integer> atoms, ArrayList<Integer> bonds) {
		AtomAssembler assembler = new AtomAssembler(mol);
		int addedHs = 0;
		for(Integer at:ats)
			addedHs+=assembler.addImplicitHydrogens(at);

		for(int at=mol.getAllAtoms()-addedHs;at<mol.getAllAtoms();at++) {
			atoms.add(at);
		}
		for(int bondTemp=mol.getAllBonds()-addedHs;bondTemp<mol.getAllBonds();bondTemp++) {
			bonds.add(bondTemp);
		}

		
	}
		
		
		

	public static boolean[] getHydrogensFromAtoms(StereoMolecule mol, int[] atoms) {
		ArrayList<Integer> hydrogensToBeDeleted = new ArrayList<Integer>();
		for(int i=0;i<atoms.length;i++) {
			int a = atoms[i];
			if (mol.getAllConnAtoms(a) != mol.getConnAtoms(a)) {
				for(int j=0;j<mol.getAllConnAtoms(a);j++) {
					int aa1 = mol.getConnAtom(a, j);
					if(mol.getAtomicNo(aa1)==1) {
						hydrogensToBeDeleted.add(aa1);
				}
			}
		}
		}
				
		boolean[] toBeDeleted = new boolean[mol.getAllAtoms()];
		for(Integer hydrogen : hydrogensToBeDeleted) {
			toBeDeleted[hydrogen] = true;
		}

		return toBeDeleted;
		}
	
	public static boolean[] getHydrogensFromAtoms(StereoMolecule mol, int atom) {
		return getHydrogensFromAtoms(mol,new int[] {atom});
	}
		
	
	

	
	public static int getFirstHydrogenBond(StereoMolecule mol) {
		for(int bond=0;bond<mol.getAllBonds();bond++) {
			if(mol.getAtomicNo(mol.getBondAtom(0,bond))==1 || mol.getAtomicNo(mol.getBondAtom(1,bond))==1) {
				return bond;
			}
		}
		return -1;
		
	}
	public static void updateNodeIndeces(V3DMolecule v3dMol,int[] atomMap, int[] bondMap) {
		for(Node node : v3dMol.getChildren()) {
			NodeDetail detail = (NodeDetail)node.getUserData();
			if (detail.isAtom() && detail.getAtom()>0) {
				detail.setIndex(atomMap[detail.getAtom()]);
			}
			else if (detail.isBond() && detail.getBond()>0) {
				detail.setIndex(bondMap[detail.getBond()]);
			}
		}
	}
	
	public static StereoMolecule v3dMolToStereoMol(V3DMolecule v3dMol) {
		Conformer conf = v3dMol.getConformer();
		StereoMolecule mol = conf.getMolecule();
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);
		int oldAtoms = mol.getAllAtoms();
		for(int i=0;i<oldAtoms;i++) {
			mol.setAtomX(i, conf.getX(i));
			mol.setAtomY(i, conf.getY(i));
			mol.setAtomZ(i, conf.getZ(i));
			}
		return mol;
	}
	

}

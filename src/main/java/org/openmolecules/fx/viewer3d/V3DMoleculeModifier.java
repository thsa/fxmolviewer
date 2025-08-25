package org.openmolecules.fx.viewer3d;

import java.util.ArrayList;

import org.openmolecules.chem.conf.gen.ConformerGenerator;
import org.openmolecules.fx.viewer3d.nodes.NodeDetail;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.IDCodeParserWithoutCoordinateInvention;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.HydrogenAssembler;
import com.actelion.research.chem.conf.BondLengthSet;
import com.actelion.research.chem.conf.TorsionDB;
import com.actelion.research.chem.conf.TorsionDetail;

import javafx.scene.Node;

public class V3DMoleculeModifier {
	
	
	public static void changeAtomElement(V3DMolecule v3dMol, int a, int atomicNo) {
		if(v3dMol.getMolecule().getAtomicNo(a)==1)changeHydrogenAtomElement(v3dMol, a, atomicNo);
		else changeHeavyAtomElement(v3dMol, a, atomicNo);
		
	}
	
	public static void addFragment(V3DMolecule v3dMol,int atom, String[] fragmentIDCode) {
		if(v3dMol.getMolecule().getAtomicNo(atom)!=1) 
			return;
		int dummyAtomFragment = -1;
		int attachmentPointFragment = -1;
		IDCodeParserWithoutCoordinateInvention parser = new IDCodeParserWithoutCoordinateInvention();
		StereoMolecule mol = v3dMol.getMolecule();
		int attachmentPoint = mol.getConnAtom(atom, 0);
		ArrayList<Integer> bondConstructionList = new ArrayList<Integer>(); //bonds that will be rebuilt
		ArrayList<Integer> atomConstructionList = new ArrayList<Integer>();
		ArrayList<Integer> atomsToBeRemoved = new ArrayList<Integer>(); //bonds that will be rebuilt
		ArrayList<Integer> bondsToBeRemoved = new ArrayList<Integer>();
		atomsToBeRemoved.add(atom);
		bondsToBeRemoved.add(mol.getBond(atom, mol.getConnAtom(atom,0)));
		cleanNodes(v3dMol,atomsToBeRemoved,bondsToBeRemoved);
		StereoMolecule fragment = parser.getCompactMolecule(fragmentIDCode[0],fragmentIDCode[1]);
		fragment.ensureHelperArrays(Molecule.cHelperCIP);
		for(int at=0;at<fragment.getAllAtoms();at++) {
			if (fragment.getAtomicNo(at)==0) {
				if(dummyAtomFragment==-1) {
					dummyAtomFragment = at;
					attachmentPointFragment = fragment.getConnAtom(dummyAtomFragment, 0);
				}
				else {
					fragment.setAtomicNo(at, 1);
					int aa1 = fragment.getConnAtom(at, 0);
					int bond = fragment.getBond(at, aa1);
					Coordinates c1 = fragment.getAtomCoordinates(at);
					Coordinates cc1 = fragment.getAtomCoordinates(aa1);
					double lNew = BondLengthSet.getBondLength(BondLengthSet.getBondIndex(fragment,bond));
					Coordinates v = c1.subC(cc1);
					double l = v.dist();
					Coordinates c1New=cc1.addC(v.scale(lNew/l));
					c1.x = c1New.x;
					c1.y = c1New.y;
					c1.z = c1New.z;
				}
			}
		}
		if(dummyAtomFragment==-1 || attachmentPointFragment==-1) return; //no attachment point in fragment
		Coordinates t = mol.getAtomCoordinates(atom).subC(fragment.getAtomCoordinates(attachmentPointFragment));
		fragment.translate(t.x, t.y, t.z);
		//preorient fragment so that the two bond extension vectors coincide (Rodrigues Rotation Formula)
		Coordinates v1 = mol.getAtomCoordinates(attachmentPoint).subC(mol.getAtomCoordinates(atom));
		Coordinates v2 = fragment.getAtomCoordinates(dummyAtomFragment).subC(fragment.getAtomCoordinates(attachmentPointFragment));
		alignFragmentOnBondVector(fragment,v1,v2,attachmentPointFragment);
		//placeFragment(mol, atom, attachmentPoint, fragment, dummyAtomFragment, attachmentPointFragment);
		int[] bondMap = mol.getDeleteAtomsBondMap(new int[] {atom});
		int[] atomMap = mol.deleteAtoms(new int[] {atom});
		mol.ensureHelperArrays(Molecule.cHelperRings);
		updateNodeIndeces(v3dMol,atomMap,bondMap);		
		int[] fragmentMap = mol.addSubstituent(fragment, attachmentPoint);
		
		for(int at=mol.getAllAtoms()-(fragment.getAllAtoms()-1);at<mol.getAllAtoms();at++) {
			atomConstructionList.add(at);
		}
		for(int bnd=mol.getAllBonds()-(fragment.getAllBonds());bnd<mol.getAllBonds();bnd++) {
			bondConstructionList.add(bnd);
		}
		bondMap = mol.getHandleHydrogenBondMap();
		atomMap = mol.getHandleHydrogenMap();
		if(atomMap!=null && bondMap!=null ) {
			updateListIndeces(atomConstructionList,atomMap);
			updateListIndeces(bondConstructionList,bondMap);
			updateNodeIndeces(v3dMol,atomMap,bondMap);
		}
		mol.ensureHelperArrays(Molecule.cHelperRings); //now the indeces of bonds and atoms changed
		Coordinates c1 = mol.getAtomCoordinates(atomMap[attachmentPoint]);
		Coordinates cc1 = mol.getAtomCoordinates(atomMap[fragmentMap[attachmentPointFragment]]);
		int bond = mol.getBond(atomMap[attachmentPoint], atomMap[fragmentMap[attachmentPointFragment]]);
		double lNew = BondLengthSet.getBondLength(BondLengthSet.getBondIndex(mol,bond));
		Coordinates v = cc1.subC(c1);
		double l = v.dist();
		t = v.scaleC(lNew/l).subC(v);
		for(int at=0;at<fragment.getAllAtoms();at++) {
			int index = fragmentMap[at];
			if(index!=-1 &&  attachmentPoint!=index) mol.getAtomCoordinates(atomMap[index]).add(t);
		}
		optimizeDihedral(mol,atomMap[attachmentPoint],atomMap[fragmentMap[attachmentPointFragment]],atomConstructionList);
		V3DMoleculeBuilder builder = new V3DMoleculeBuilder(v3dMol);
		builder.buildMolecule(atomConstructionList,bondConstructionList);
		mol.ensureHelperArrays(Molecule.cHelperRings);

	}
	
	public static void placeAtom(V3DMolecule v3dMol, int atomicNo) {
		StereoMolecule mol = v3dMol.getMolecule();
		mol.addAtom(atomicNo);
		HydrogenAssembler assembler = new HydrogenAssembler(mol);
		assembler.addImplicitHydrogens();
		mol.ensureHelperArrays(Molecule.cHelperRings);
		V3DMoleculeBuilder builder = new V3DMoleculeBuilder(v3dMol);
		builder.buildMolecule();

		
		
	}
	
	public static void fuseRing(V3DMolecule v3dMol,int bond, String[] fragmentIDCode) {
		StereoMolecule mol = v3dMol.getMolecule();
		int bondAtom1 = mol.getBondAtom(0, bond);
		int bondAtom2 = mol.getBondAtom(1, bond);
		ArrayList<Integer> bondAtoms = new ArrayList<Integer>();
		bondAtoms.add(bondAtom1);
		bondAtoms.add(bondAtom2);
		ArrayList<Integer> atomConstructionList = new ArrayList<Integer>();
		ArrayList<Integer> atomsToBeRemoved = new ArrayList<Integer>();
		ArrayList<Integer> bondConstructionList  = new ArrayList<Integer>();
		ArrayList<Integer> bondsToBeRemoved = new ArrayList<Integer>();
		if(!mol.isAromaticBond(bond)) {
			if(getHighestPossibleBondOrder(mol,bond)<3) return;
			else if(mol.getBondOrder(bond)!=2) changeBondOrder(v3dMol,bond,2);
		}
		else if(getHighestPossibleBondOrder(mol,bond)-mol.getBondOrder(bond)<1) return; 
		//else if (mol.getBondOrder(bond)!=1)changeBondOrder(v3dMol,bond,1);
		IDCodeParserWithoutCoordinateInvention parser = new IDCodeParserWithoutCoordinateInvention();
		ArrayList<Integer> hydrogensToBeDeleted = getHydrogensFromAtoms(mol,bondAtoms);
		//the bonds from the atom to the hydrogens are deleted in the StereoMolecule as well as removed from the V3DMolecule
		for(Integer hydrogen : hydrogensToBeDeleted) {
			atomsToBeRemoved.add(hydrogen);
			bondsToBeRemoved.add(mol.getBond(hydrogen, mol.getConnAtom(hydrogen,0)));
		}
		cleanNodes(v3dMol,atomsToBeRemoved,bondsToBeRemoved);
		
		int[] hydrogensToBeRemoved = hydrogensToBeDeleted.stream().mapToInt(i->i).toArray();
		int[] bondMap = mol.getDeleteAtomsBondMap(hydrogensToBeRemoved);
		int[] atomMap = mol.deleteAtoms(hydrogensToBeRemoved);
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);
		if(atomMap!=null && bondMap!=null ) {
			updateNodeIndeces(v3dMol,atomMap,bondMap);
		}

		StereoMolecule fragment = parser.getCompactMolecule(fragmentIDCode[0],fragmentIDCode[1]);
		int[] fragmentBondAtoms = new int[2];
		int[] fragmentDummyAtoms = new int[2];
		for(int i=0,counter=0;i<fragment.getAllAtoms();i++) {
			if (fragment.getAtomicNo(i)==0) {
				fragmentDummyAtoms[counter]=i;
				fragmentBondAtoms[counter] = fragment.getConnAtom(i,0);
				counter++;
			}
		}
	
		Coordinates t = mol.getAtomCoordinates(bondAtom1).subC(fragment.getAtomCoordinates(fragmentBondAtoms[0]));
		fragment.translate(t.x,t.y,t.z);
		Coordinates v1 = mol.getAtomCoordinates(bondAtom1).subC(mol.getAtomCoordinates(bondAtom2));
		Coordinates v2 = fragment.getAtomCoordinates(fragmentBondAtoms[0]).subC(fragment.getAtomCoordinates(fragmentBondAtoms[1]));
		alignFragmentOnBondVector(fragment,v1,v2,fragmentBondAtoms[0]);

		ArrayList<Integer> fragmentAtomsToDelete = new ArrayList<Integer>();
		int[] fragmentAnchorPoints = new int[2];
		fragmentAtomsToDelete.add(fragmentDummyAtoms[0]);
		fragmentAtomsToDelete.add(fragmentDummyAtoms[1]);
		fragmentAtomsToDelete.add(fragmentBondAtoms[0]);
		fragmentAtomsToDelete.add(fragmentBondAtoms[1]);
		for(int i=0, counter=0;i<2;i++) {
			int bondAtom = fragmentBondAtoms[i];
			for (int j=0;j<fragment.getAllConnAtoms(bondAtom);j++) {
				int connAtom = fragment.getConnAtom(bondAtom, j);
				if(mol.getAtomicNo(connAtom)==1) fragmentAtomsToDelete.add(connAtom); 
				else if(mol.getAtomicNo(connAtom)!=0 && !fragmentAtomsToDelete.contains(connAtom)) {
					fragmentAnchorPoints[counter] = connAtom;
					counter++;
				}
			}
		}
		int dihedralAtom1 = mol.getConnAtom(bondAtom1,0)==bondAtom2 ? mol.getConnAtom(bondAtom1,1) : mol.getConnAtom(bondAtom1,0);
		int dihedralAtom2 = bondAtom1;
		int dihedralAtom3 = bondAtom2;
		int dihedralAtom4 = fragmentAnchorPoints[1];
		Coordinates[] dihedralCoords = {mol.getAtomCoordinates(dihedralAtom1),
				mol.getAtomCoordinates(dihedralAtom2),mol.getAtomCoordinates(dihedralAtom3),
				fragment.getAtomCoordinates(dihedralAtom4)};
		double dihedral = Math.PI-Coordinates.getDihedral(dihedralCoords[0], dihedralCoords[1], dihedralCoords[2], dihedralCoords[3]);
		rotateFragmentAroundVector(fragment,-dihedral,v1,mol.getAtomCoordinates(bondAtom1));
		int[] fragmentMap = fragment.deleteAtoms(fragmentAtomsToDelete.stream().mapToInt(i->i).toArray());
		fragmentAnchorPoints[0]= fragmentMap[fragmentAnchorPoints[0]];
		fragmentAnchorPoints[1]= fragmentMap[fragmentAnchorPoints[1]];
		fragmentMap = mol.addMolecule(fragment);
		fragmentAnchorPoints[0]= fragmentMap[fragmentAnchorPoints[0]];
		fragmentAnchorPoints[1]= fragmentMap[fragmentAnchorPoints[1]];
		for(int at=mol.getAllAtoms()-(fragment.getAllAtoms()-1);at<mol.getAllAtoms();at++) {
			atomConstructionList.add(at);
		}
		for(int bnd=mol.getAllBonds()-(fragment.getAllBonds());bnd<mol.getAllBonds();bnd++) {
			bondConstructionList.add(bnd);
		}
		bondConstructionList.add(mol.addBond(bondAtom1, fragmentAnchorPoints[0]));
		bondConstructionList.add(mol.addBond(bondAtom2, fragmentAnchorPoints[1]));
		bondMap = mol.getHandleHydrogenBondMap();
		atomMap = mol.getHandleHydrogenMap();
		mol.ensureHelperArrays(Molecule.cHelperRings);
		if(atomMap!=null && bondMap!=null ) {
			updateNodeIndeces(v3dMol,atomMap,bondMap);
			updateListIndeces(atomConstructionList,atomMap);
			updateListIndeces(bondConstructionList,bondMap);
		}
		for(Integer bondAtom : bondAtoms)
			constructHydrogens(mol,bondAtom,atomConstructionList,bondConstructionList);
		mol.ensureHelperArrays(Molecule.cHelperRings);
		V3DMoleculeBuilder builder = new V3DMoleculeBuilder(v3dMol);
		builder.buildMolecule(atomConstructionList,bondConstructionList);
		
	}
	
	public static void changeHydrogenAtomElement(V3DMolecule v3dMol, int atom, int atomicNo) {

		StereoMolecule mol = v3dMol.getMolecule();
		ArrayList<Integer> atomConstructionList = new ArrayList<Integer>();
		ArrayList<Integer> atomsToBeRemoved = new ArrayList<Integer>();
		ArrayList<Integer> bondConstructionList  = new ArrayList<Integer>();
		ArrayList<Integer> bondsToBeRemoved = new ArrayList<Integer>();
		atomsToBeRemoved.add(atom);
		atomConstructionList.add(atom);
		int aa1 = mol.getConnAtom(atom, 0);
		int bond = mol.getBond(atom, aa1);
		bondsToBeRemoved.add(bond);
		bondConstructionList.add(bond);
		cleanNodes(v3dMol,atomsToBeRemoved,bondsToBeRemoved);
		
		mol.setAtomicNo(atom, atomicNo); 


		int[] atomMap = mol.getHandleHydrogenMap();
		int[] bondMap = mol.getHandleHydrogenBondMap();
		if(atomMap!=null && bondMap!=null ) {
			updateListIndeces(atomConstructionList,atomMap);
			updateListIndeces(bondConstructionList,bondMap);
			updateNodeIndeces(v3dMol,atomMap,bondMap);
		}
		
		mol.ensureHelperArrays(Molecule.cHelperRings); //now the indeces of bonds and atoms changed
		//cleaning up the geometry
		Coordinates c1 = mol.getAtomCoordinates(atomMap[atom]);
		Coordinates cc1 = mol.getAtomCoordinates(atomMap[aa1]);
		double lNew = BondLengthSet.getBondLength(BondLengthSet.getBondIndex(mol,bondMap[bond]));
		Coordinates v = c1.subC(cc1);
		double l = v.dist();
		Coordinates c1New=cc1.addC(v.scale(lNew/l));
		c1.x = c1New.x;
		c1.y = c1New.y;
		c1.z = c1New.z;
		for(Integer at: new ArrayList<Integer>(atomConstructionList))
			constructHydrogens(mol,at,atomConstructionList,bondConstructionList);
		//v3dMol.setConformer(new Conformer(mol));
		V3DMoleculeBuilder builder = new V3DMoleculeBuilder(v3dMol);
		builder.buildMolecule(atomConstructionList,bondConstructionList);
		mol.ensureHelperArrays(Molecule.cHelperRings);

	}
	
	public static void changeHeavyAtomElement(V3DMolecule v3dMol, int atom, int atomicNo) {
		
		StereoMolecule mol = v3dMol.getMolecule();
		ArrayList<Integer> bondsToBeRemoved = new ArrayList<Integer>();
		ArrayList<Integer> atomsToBeRemoved = new ArrayList<Integer>();
		ArrayList<Integer> hydrogensToBeRemoved = new ArrayList<Integer>();
		ArrayList<Integer> bondConstructionList  = new ArrayList<Integer>();
		ArrayList<Integer> atomConstructionList = new ArrayList<Integer>();
		atomsToBeRemoved.add(atom);
		atomConstructionList.add(atom);
		for(int i=0;i<mol.getAllConnAtoms(atom);i++) {
			int aa = mol.getConnAtom(atom, i);
			if(mol.getAtomicNo(aa)==1) {
				atomsToBeRemoved.add(aa);
				hydrogensToBeRemoved.add(aa);
				bondsToBeRemoved.add(mol.getBond(atom, aa));
			}
			else {
				int bond = mol.getBond(atom, aa);
				bondsToBeRemoved.add(bond);
				if(!bondConstructionList.contains(bond)) bondConstructionList.add(bond);
				
				
			}
			
		}
		cleanNodes(v3dMol,atomsToBeRemoved,bondsToBeRemoved);
		mol.setAtomicNo(atom, atomicNo);

		int[] hydrogensToBeDeleted = hydrogensToBeRemoved.stream().mapToInt(i->i).toArray();
		int[] bondMap = mol.getDeleteAtomsBondMap(hydrogensToBeDeleted);
		int[] atomMap = mol.deleteAtoms(hydrogensToBeDeleted);
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);
		if(atomMap!=null && bondMap!=null ) {
			updateListIndeces(atomConstructionList,atomMap);
			updateListIndeces(bondConstructionList,bondMap);
			updateNodeIndeces(v3dMol,atomMap,bondMap);
		}
		for(Integer at: new ArrayList<Integer>(atomConstructionList)) {
			constructHydrogens(mol,at,atomConstructionList,bondConstructionList);
		}
		mol.ensureHelperArrays(Molecule.cHelperRings);
		//v3dMol.setConformer(new Conformer(mol));
		V3DMoleculeBuilder builder = new V3DMoleculeBuilder(v3dMol);
		builder.buildMolecule(atomConstructionList,bondConstructionList);
		
	}
	
	public static void drawBond(V3DMolecule v3dMol, int atom1, int atom2) {
		StereoMolecule mol = v3dMol.getMolecule();
		ArrayList<Integer> bondAtoms = new ArrayList<Integer>();
		bondAtoms.add(atom1);
		bondAtoms.add(atom2);
		ArrayList<Integer> bondsToBeRemoved = new ArrayList<Integer>();
		ArrayList<Integer> atomsToBeRemoved = new ArrayList<Integer>();
		ArrayList<Integer> bondConstructionList  = new ArrayList<Integer>();
		ArrayList<Integer> atomConstructionList = new ArrayList<Integer>();
		int aH = mol.getAllConnAtoms(atom1)-mol.getConnAtoms(atom1);
		int bH = mol.getAllConnAtoms(atom2)-mol.getConnAtoms(atom2);
		if(aH==0 || bH==0) return; //valences already full, cannot add bond
		ArrayList<Integer> hydrogensToBeDeleted = getHydrogensFromAtoms(mol,bondAtoms);
		//the bonds from the atom to the hydrogens are deleted in the StereoMolecule as well as removed from the V3DMolecule
		for(Integer hydrogen : hydrogensToBeDeleted) {
			atomsToBeRemoved.add(hydrogen);
			bondsToBeRemoved.add(mol.getBond(hydrogen, mol.getConnAtom(hydrogen,0)));
		}
		
		cleanNodes(v3dMol,atomsToBeRemoved,bondsToBeRemoved);
		
		int[] bondMap = mol.getDeleteAtomsBondMap(hydrogensToBeDeleted.stream().mapToInt(i->i).toArray());
		int[] atomMap = mol.deleteAtoms(hydrogensToBeDeleted.stream().mapToInt(i->i).toArray());
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);
		if(atomMap!=null && bondMap!=null ) {
			updateNodeIndeces(v3dMol,atomMap,bondMap);
		}
		mol.addBond(atom1, atom2);	
		atomMap = mol.getHandleHydrogenMap();
		bondMap = mol.getHandleHydrogenBondMap();
		mol.ensureHelperArrays(Molecule.cHelperRings);
		int bond = mol.getBond(atom1,atom2);
		if(bond==-1) return;
		bondConstructionList.add(bond);
		if(atomMap!=null && bondMap!=null ) {
			updateNodeIndeces(v3dMol,atomMap,bondMap);
		}
		for(Integer bondAtom:bondAtoms)
			constructHydrogens(mol,bondAtom,atomConstructionList,bondConstructionList);
		mol.ensureHelperArrays(Molecule.cHelperRings);
		//v3dMol.setConformer(new Conformer(mol));
		V3DMoleculeBuilder builder = new V3DMoleculeBuilder(v3dMol);
		builder.buildMolecule(atomConstructionList,bondConstructionList);


	}
	
	public static void deleteBond(V3DMolecule v3dMol, int bond) {
		StereoMolecule mol = v3dMol.getMolecule();
		int atom1 = mol.getBondAtom(0, bond);
		int atom2 = mol.getBondAtom(1, bond);
		ArrayList<Integer> bondAtoms = new ArrayList<Integer>();
		bondAtoms.add(atom1);
		bondAtoms.add(atom2);
		ArrayList<Integer> bondsToBeRemoved = new ArrayList<Integer>();
		ArrayList<Integer> hydrogensToBeDeleted = getHydrogensFromAtoms(mol,bondAtoms);
		ArrayList<Integer> bondConstructionList  = new ArrayList<Integer>();
		ArrayList<Integer> atomConstructionList = new ArrayList<Integer>();
		bondsToBeRemoved.add(bond);
		//ArrayList<Integer> hydrogensToBeDeleted = getHydrogensFromAtoms(mol,bondAtoms);
		//the bonds from the atom to the hydrogens are deleted in the StereoMolecule as well as removed from the V3DMolecule
		for(Integer hydrogen : hydrogensToBeDeleted) {
			bondsToBeRemoved.add(mol.getBond(hydrogen, mol.getConnAtom(hydrogen,0)));
		}
		cleanNodes(v3dMol, hydrogensToBeDeleted,bondsToBeRemoved);

		int[] bondMap = mol.getDeleteAtomsBondMap(hydrogensToBeDeleted.stream().mapToInt(i->i).toArray());
		int[] atomMap = mol.deleteAtoms(hydrogensToBeDeleted.stream().mapToInt(i->i).toArray());
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);
		if(atomMap!=null && bondMap!=null ) 
			updateNodeIndeces(v3dMol,atomMap,bondMap);
		
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
		for(Integer bondAtom:bondAtoms)
			constructHydrogens(mol,bondAtom,atomConstructionList,bondConstructionList);
		V3DMoleculeBuilder builder = new V3DMoleculeBuilder(v3dMol);
		builder.buildMolecule(atomConstructionList,bondConstructionList);
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);
	

	}
	
	public static void deleteAtom(V3DMolecule v3dMol, int atom) {
		StereoMolecule mol = v3dMol.getMolecule();
		ArrayList<Integer> bondsToBeRemoved = new ArrayList<Integer>();
		ArrayList<Integer> atomsToBeRemoved = new ArrayList<Integer>();
		ArrayList<Integer> bondConstructionList  = new ArrayList<Integer>();
		ArrayList<Integer> atomConstructionList = new ArrayList<Integer>();
		ArrayList<Integer> hydrogensToBeDeleted = getHydrogensFromAtom(mol,atom);
		ArrayList<Integer> heavyAtomNeighbours = new ArrayList<Integer>();
		//the bonds from the atom to the hydrogens are deleted in the StereoMolecule as well as removed from the V3DMolecule
		atomsToBeRemoved.add(atom);
		for(Integer hydrogen : hydrogensToBeDeleted) {
			atomsToBeRemoved.add(hydrogen);
			bondsToBeRemoved.add(mol.getBond(hydrogen, mol.getConnAtom(hydrogen,0)));
		}
		for(int i=0;i<mol.getConnAtoms(atom);i++) {
			bondsToBeRemoved.add(mol.getBond(atom, mol.getConnAtom(atom, i)));
			heavyAtomNeighbours.add(mol.getConnAtom(atom, i));
		}
		cleanNodes(v3dMol, atomsToBeRemoved,bondsToBeRemoved);
		
		int[] bondMap = mol.getDeleteAtomsBondMap(atomsToBeRemoved.stream().mapToInt(i->i).toArray());
		int[] atomMap = mol.deleteAtoms(atomsToBeRemoved.stream().mapToInt(i->i).toArray());
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);
		if(atomMap!=null && bondMap!=null ) 
			updateListIndeces(heavyAtomNeighbours,atomMap);
			updateNodeIndeces(v3dMol,atomMap,bondMap);


		for(Integer heavyAtomNeighbour:heavyAtomNeighbours)
			constructHydrogens(mol,heavyAtomNeighbour,atomConstructionList,bondConstructionList);
		V3DMoleculeBuilder builder = new V3DMoleculeBuilder(v3dMol);
		builder.buildMolecule(atomConstructionList,bondConstructionList);
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);
	

	}
	
	public static void toggleBondOrder(V3DMolecule v3dMol,int bond) {
		StereoMolecule mol = v3dMol.getMolecule();
		//int highestPossibleBondOrder = mol.getBondOrder(bond)+Math.min(mol.getFreeValence(a)+aH,mol.getFreeValence(b)+bH);
		int highestPossibleBondOrder = getHighestPossibleBondOrder(mol,bond);
		if(highestPossibleBondOrder==1) return;
		int currentOrder = mol.getBondOrder(bond);
		if(highestPossibleBondOrder>currentOrder) changeBondOrder(v3dMol,bond,currentOrder+1);
		else changeBondOrder(v3dMol,bond,1);
	}
	
	public static void changeBondOrder(V3DMolecule v3dMol, int bond, int order) {
		StereoMolecule mol = v3dMol.getMolecule();
		int atom1 = mol.getBondAtom(0, bond);
		int atom2 = mol.getBondAtom(1, bond);
		ArrayList<Integer> bondAtoms = new ArrayList<Integer>();
		bondAtoms.add(atom1);
		bondAtoms.add(atom2);
		ArrayList<Integer> bondsToBeRemoved = new ArrayList<Integer>();
		ArrayList<Integer> atomsToBeRemoved = new ArrayList<Integer>();
		ArrayList<Integer> bondConstructionList  = new ArrayList<Integer>();
		ArrayList<Integer> atomConstructionList = new ArrayList<Integer>();
		ArrayList<Integer> hydrogensToBeDeleted = getHydrogensFromAtoms(mol,bondAtoms);
		bondsToBeRemoved.add(bond);
		//the bonds from the atom to the hydrogens are deleted in the StereoMolecule as well as removed from the V3DMolecule
		for(Integer hydrogen : hydrogensToBeDeleted) {
			atomsToBeRemoved.add(hydrogen);
			bondsToBeRemoved.add(mol.getBond(hydrogen, mol.getConnAtom(hydrogen,0)));
		}
		cleanNodes(v3dMol, atomsToBeRemoved,bondsToBeRemoved);
		int[] atomsToBeDeleted= atomsToBeRemoved.stream().mapToInt(i->i).toArray();
		int[] bondMap = mol.getDeleteAtomsBondMap(atomsToBeDeleted);
		int[] atomMap = mol.deleteAtoms(atomsToBeDeleted);
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);
		if(atomMap!=null && bondMap!=null ) 
			updateNodeIndeces(v3dMol,atomMap,bondMap);

		bondConstructionList.add(bond);
		mol.setBondOrder(bond, order);
		mol.ensureHelperArrays(Molecule.cHelperRings);
		for(Integer bondAtom:bondAtoms)
			constructHydrogens(mol,bondAtom,atomConstructionList,bondConstructionList);
		mol.ensureHelperArrays(Molecule.cHelperRings);
		V3DMoleculeBuilder builder = new V3DMoleculeBuilder(v3dMol);
		builder.buildMolecule(atomConstructionList,bondConstructionList);

	}
	public static void increaseCharge(V3DMolecule v3dMol, int atom) {
		changeCharge(v3dMol,atom,1);
	}
	
	public static void decreaseCharge(V3DMolecule v3dMol, int atom) {
		changeCharge(v3dMol,atom,-1);
	}
	
	public static void changeCharge(V3DMolecule v3dMol, int atom, int chargeChange) {
		StereoMolecule mol = v3dMol.getMolecule();
		int origCharge = mol.getAtomCharge(atom);
		if(mol.getAtomicNo(atom)==1) return;
		ArrayList<Integer> bondsToBeRemoved = new ArrayList<Integer>();
		ArrayList<Integer> atomsToBeRemoved = new ArrayList<Integer>();
		ArrayList<Integer> bondConstructionList  = new ArrayList<Integer>();
		ArrayList<Integer> atomConstructionList = new ArrayList<Integer>();
		ArrayList<Integer> hydrogensToBeDeleted = getHydrogensFromAtom(mol,atom);
		//the bonds from the atom to the hydrogens are deleted in the StereoMolecule as well as removed from the V3DMolecule
		for(Integer hydrogen : hydrogensToBeDeleted) {
			atomsToBeRemoved.add(hydrogen);
			bondsToBeRemoved.add(mol.getBond(hydrogen, mol.getConnAtom(hydrogen,0)));
		}
		cleanNodes(v3dMol, atomsToBeRemoved,bondsToBeRemoved);
		int[] hydrogensToBeRemoved = hydrogensToBeDeleted.stream().mapToInt(i->i).toArray();
		int[] bondMap = mol.getDeleteAtomsBondMap(hydrogensToBeRemoved);
		int[] atomMap = mol.deleteAtoms(hydrogensToBeRemoved);
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);
		if(atomMap!=null && bondMap!=null ) {
			updateNodeIndeces(v3dMol,atomMap,bondMap);
		}
		mol.setAtomCharge(atom, origCharge+chargeChange);
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);
		constructHydrogens(mol,atom,atomConstructionList,bondConstructionList);
		V3DMoleculeBuilder builder = new V3DMoleculeBuilder(v3dMol);
		builder.buildMolecule(atomConstructionList,bondConstructionList);
		mol.ensureHelperArrays(Molecule.cHelperRings);
	}
	
	public static void placeFragment(V3DMolecule v3dMol,  String[] fragmentIDCode) {
		IDCodeParserWithoutCoordinateInvention parser = new IDCodeParserWithoutCoordinateInvention();
		StereoMolecule mol = v3dMol.getMolecule();
		mol.setName("Molecule");
		
		StereoMolecule fragment = parser.getCompactMolecule(fragmentIDCode[0],fragmentIDCode[1]);
		for(int at=0;at<fragment.getAllAtoms();at++) 
			if (fragment.getAtomicNo(at)==0) {
				fragment.setAtomicNo(at, 1);
				int bond = fragment.getBond(at, fragment.getConnAtom(at, 0));
				double lNew = BondLengthSet.getBondLength(BondLengthSet.getBondIndex(fragment,bond));
				Coordinates v = fragment.getAtomCoordinates(at).subC(fragment.getAtomCoordinates(fragment.getConnAtom(at, 0)));
				double l = v.dist();
				Coordinates t = v.scaleC(lNew/l).subC(v);
				fragment.getAtomCoordinates(at).add(t);
		}
		fragment.ensureHelperArrays(Molecule.cHelperCIP);
		mol.addMolecule(fragment);
		V3DMoleculeBuilder builder = new V3DMoleculeBuilder(v3dMol);
		builder.buildMolecule();

		
	}

	

	
	public static void constructHydrogens(StereoMolecule mol, int atom, ArrayList<Integer> atomConstructionList, ArrayList<Integer> bondConstructionList) {
		HydrogenAssembler assembler = new HydrogenAssembler(mol);

		int addedHs=assembler.addImplicitHydrogens(atom);
	

		for(int at=mol.getAllAtoms()-addedHs;at<mol.getAllAtoms();at++) {
			atomConstructionList.add(at);
		}
		for(int bondTemp=mol.getAllBonds()-addedHs;bondTemp<mol.getAllBonds();bondTemp++) {
			bondConstructionList.add(bondTemp);

		}
		mol.ensureHelperArrays(StereoMolecule.cHelperRings);
	}
		
		
		

	public static ArrayList<Integer> getHydrogensFromAtoms(StereoMolecule mol, ArrayList<Integer> atoms) {
		ArrayList<Integer> hydrogens = new ArrayList<Integer>();
		for(int i=0;i<atoms.size();i++) {
			int atom = atoms.get(i);
			hydrogens.addAll(getHydrogensFromAtom(mol,atom));
		}
		return hydrogens;
	}
		
		public static ArrayList<Integer> getHydrogensFromAtom(StereoMolecule mol, int atom){
			ArrayList<Integer> hydrogens = new ArrayList<Integer>();
			if (mol.getAllConnAtoms(atom) != mol.getConnAtoms(atom)) {
				for(int j=0;j<mol.getAllConnAtoms(atom);j++) {
					int aa1 = mol.getConnAtom(atom, j);
					if(mol.getAtomicNo(aa1)==1) {
						hydrogens.add(aa1);
						}
				}
			}
			return hydrogens;
		}

	

	
	

	
	public static int getFirstHydrogenBond(StereoMolecule mol) {
		for(int bond=0;bond<mol.getAllBonds();bond++) {
			if(mol.getAtomicNo(mol.getBondAtom(0,bond))==1 || mol.getAtomicNo(mol.getBondAtom(1,bond))==1) {
				return bond;
			}
		}
		return -1;
		
	}
	
	private static void cleanNodes(V3DMolecule v3dMol,ArrayList<Integer> atomsToBeDeleted,ArrayList<Integer> bondsToBeDeleted) {
		ArrayList<Node> nodesToBeDeleted = new ArrayList<Node>();
		for(Node node : v3dMol.getChildren()) {
			NodeDetail detail = (NodeDetail)node.getUserData();
			if (detail != null && detail.isAtom()) {
				int atom = detail.getAtom();
				if(atomsToBeDeleted.contains(atom)) nodesToBeDeleted.add(node);
		
			}
			else if (detail != null && detail.isBond() ) {
				int bond = detail.getBond();
				if(bondsToBeDeleted.contains(bond)) nodesToBeDeleted.add(node);
				
		}
		}
		v3dMol.getChildren().removeAll(nodesToBeDeleted);
		
	}
	
	
	private static void alignFragmentOnBondVector(StereoMolecule fragment, Coordinates v1,Coordinates v2, int attachmentPoint) {
		v1.unit();
		v2.unit();
		Coordinates k = v2.cross(v1);
		double sinTheta = k.dist();
		k = k.scale(1.0/k.dist());
		double cosTheta = v1.dot(v2);

		
		for(int at=0;at<fragment.getAllAtoms();at++) {
			Coordinates v = fragment.getAtomCoordinates(at).subC(fragment.getAtomCoordinates(attachmentPoint));
			Coordinates vNew = v.scaleC(cosTheta);
			vNew.add(k.cross(v).scale(sinTheta));
			vNew.add(k.scaleC(k.dot(v)).scale(1-cosTheta));
			vNew.add(fragment.getAtomCoordinates(attachmentPoint));
			fragment.setAtomX(at, vNew.x);
			fragment.setAtomY(at, vNew.y);
			fragment.setAtomZ(at, vNew.z);
		}
	}
	
	
	
	
	public static void optimizeDihedral(StereoMolecule mol,int atom1, int atom2, ArrayList<Integer> atomList) {
		TorsionDB.initialize(TorsionDB.MODE_ANGLES);
		int bond = mol.getBond(atom1, atom2);
		int atom3,atom4;
		int aa1 = 0;
		int aa2 = 0;
		for(int i=0;i<mol.getConnAtoms(atom1);i++) {
			if (mol.getAtomicNo(mol.getConnAtom(atom1, i))>1 && mol.getConnAtom(atom1, i)!=atom2) {
				aa1 = i;
				break;
			}
		}
		for(int i=0;i<mol.getConnAtoms(atom1);i++) {
			if (mol.getAtomicNo(mol.getConnAtom(atom2, i))>1 && mol.getConnAtom(atom2, i)!=atom1) {
				aa2 = i;
				break;
			}
		}
		atom3 = mol.getConnAtom(atom1, aa1);
		atom4 = mol.getConnAtom(atom2, aa2);
		int[] torsionAtoms = new int[] {atom3,atom1,atom2,atom4};
		TorsionDetail detail = new TorsionDetail();
		String torsionID = TorsionDB.getTorsionID(mol, bond, new int[4], detail);
		short[] dihedral = TorsionDB.getTorsions(torsionID);
		if(dihedral==null) {
			dihedral = new short[] {0,60,120,180,240,300,360};
		}
		boolean [][] skipCollisionCheck = new boolean[mol.getAllAtoms()][];
		for (int atom=1; atom<mol.getAllAtoms(); atom++)
			skipCollisionCheck[atom] = new boolean[atom];
		for (int atom=0; atom<mol.getAllAtoms(); atom++) {
			int index1 = mol.getConnAtom(atom, 0);
			if(index1>atom) skipCollisionCheck[index1][atom]=true;
			else skipCollisionCheck[atom][index1]=true;
			for (int i=1; i<mol.getAllConnAtoms(atom); i++) {
				index1 = mol.getConnAtom(atom, i);
				if(index1>atom) skipCollisionCheck[index1][atom]=true;
				else skipCollisionCheck[atom][index1]=true;
				for (int j=0; j<i; j++) {
					int index2 = mol.getConnAtom(atom, j);
					if(index1>index2) skipCollisionCheck[index1][index2]=true;
					else skipCollisionCheck[index2][index1]=true;
				}

				}
			}
		double minCollision = Float.MAX_VALUE;
		int bestDihedral = dihedral[0];
		for(Short angle:dihedral) {
			double torsion = 180*mol.calculateTorsion(torsionAtoms)/Math.PI;
			int deltaTorsion = angle - (int)torsion;
			rotateAtomsAroundBond(mol,Math.PI * deltaTorsion / 180.0,atomList,atom1,atom2);
			double collision = getCollision(mol,skipCollisionCheck);
			if (collision<minCollision) {
				bestDihedral = angle;
				minCollision = collision; 
			}
		}
		double torsion = 180*mol.calculateTorsion(torsionAtoms)/Math.PI;
		int deltaTorsion = bestDihedral - (int) torsion;
		rotateAtomsAroundBond(mol,Math.PI * deltaTorsion / 180.0,atomList,atom1,atom2);

			
		
		
	}
	public static void updateNodeIndeces(V3DMolecule v3dMol,int[] atomMap, int[] bondMap) {
		for(Node node : v3dMol.getChildren()) {
			NodeDetail detail = (NodeDetail)node.getUserData();
			if (detail!=null) {
				if(detail.isAtom() && detail.getAtom()>=0 ) {
					detail.setIndex(atomMap[detail.getAtom()]);
				}
				else if (detail.isBond() && detail.getBond()>=0) {
					detail.setIndex(bondMap[detail.getBond()]);
			}
		}
	}

	}
	
	private static int getPotentialBondsBetweenAtoms(StereoMolecule mol, int atom1, int atom2) {
		if(mol.getAtomicNo(atom1)==1) {
			if(mol.getAtomicNo(atom2)==1) return 0;
			else return 1;
		}
		int aH = mol.getAllConnAtoms(atom1)-mol.getConnAtoms(atom1);
		int bH = mol.getAllConnAtoms(atom2)-mol.getConnAtoms(atom2);
		return Math.min(mol.getFreeValence(atom1)+aH,mol.getFreeValence(atom2)+bH);

	}
	
	private static int getHighestPossibleBondOrder(StereoMolecule mol, int bond) {
		int atom1 = mol.getBondAtom(0, bond);
		int atom2 = mol.getBondAtom(1, bond);
		return mol.getBondOrder(bond)+getPotentialBondsBetweenAtoms(mol,atom1,atom2);
	}
	

	
	private static void rotateAtomsAroundBond(StereoMolecule mol, double angle, ArrayList<Integer> atomsToRotate, int torsionAtom1,int torsionAtom2) {
		Coordinates t2 = mol.getAtomCoordinates(torsionAtom2);
		Coordinates unit = t2.subC(mol.getAtomCoordinates(torsionAtom1)).unit();
		
		double[][] m = getRotationMatrix(unit, -angle);
		for (int atom:atomsToRotate) {
			if (atom != torsionAtom2) {
				double x = mol.getAtomX(atom) - t2.x;
				double y = mol.getAtomY(atom) - t2.y;
				double z = mol.getAtomZ(atom) - t2.z;
				mol.setAtomX(atom, x*m[0][0]+y*m[0][1]+z*m[0][2] + t2.x);
				mol.setAtomY(atom, x*m[1][0]+y*m[1][1]+z*m[1][2] + t2.y);
				mol.setAtomZ(atom, x*m[2][0]+y*m[2][1]+z*m[2][2] + t2.z);
				}
			}
		}
	
	private static void updateListIndeces(ArrayList<Integer> list, int[] map) {
		for(int i=0;i<list.size();i++) {
			list.set(i, map[list.get(i)]);
		}
		
	}
	
	
	
	private static void rotateFragmentAroundVector(StereoMolecule fragment, double angle, Coordinates direction, Coordinates origin) {
		direction.unit();
		double[][] m = getRotationMatrix(direction, -angle);
		for (int i=0;i<fragment.getAllAtoms();i++) {
			double x = fragment.getAtomX(i) - origin.x;
			double y = fragment.getAtomY(i) - origin.y;
			double z = fragment.getAtomZ(i) - origin.z;
			fragment.setAtomX(i, x*m[0][0]+y*m[0][1]+z*m[0][2] + origin.x);
			fragment.setAtomY(i, x*m[1][0]+y*m[1][1]+z*m[1][2] + origin.y);
			fragment.setAtomZ(i, x*m[2][0]+y*m[2][1]+z*m[2][2] + origin.z);
				}
		}

	private static double[][] getRotationMatrix(Coordinates n, double alpha) {
		double sinAlpha = Math.sin(alpha);
		double cosAlpha = Math.cos(alpha);
		double invcosAlpha = 1.0-cosAlpha;

		// rotation matrix is:  m11 m12 m13
		//					    m21 m22 m23
		//					    m31 m32 m33
		double[][] m = new double[3][3];
		m[0][0] = n.x*n.x*invcosAlpha+cosAlpha;
		m[1][1] = n.y*n.y*invcosAlpha+cosAlpha;
		m[2][2] = n.z*n.z*invcosAlpha+cosAlpha;
		m[0][1] = n.x*n.y*invcosAlpha-n.z*sinAlpha;
		m[1][2] = n.y*n.z*invcosAlpha-n.x*sinAlpha;
		m[2][0] = n.z*n.x*invcosAlpha-n.y*sinAlpha;
		m[0][2] = n.x*n.z*invcosAlpha+n.y*sinAlpha;
		m[1][0] = n.y*n.x*invcosAlpha+n.z*sinAlpha;
		m[2][1] = n.z*n.y*invcosAlpha+n.x*sinAlpha;
		return m;
		}
	
	public static double getCollision(StereoMolecule mol,boolean[][] skipCheck) {
		double collisionIntensitySum=0;
		for (int atom1=1; atom1<mol.getAllAtoms(); atom1++) {
			double vdwr1 = ConformerGenerator.getToleratedVDWRadius(mol.getAtomicNo(atom1));
			for (int atom2=0; atom2<atom1; atom2++) {
				if(skipCheck[atom1][atom2])continue;
				double minDistance = vdwr1+ConformerGenerator.getToleratedVDWRadius(mol.getAtomicNo(atom2));
				double dx = Math.abs(mol.getAtomX(atom1) - mol.getAtomX(atom2));
				if (dx < minDistance) {
					double dy = Math.abs(mol.getAtomY(atom1) - mol.getAtomY(atom2));
					if (dy < minDistance) {
						double dz = Math.abs(mol.getAtomZ(atom1) - mol.getAtomZ(atom2));
						if (dz < minDistance) {
							double distance = Math.sqrt(dx*dx+dy*dy+dz*dz);
							if (distance < minDistance) {
								double relativeCollision = (minDistance - distance) / minDistance;
								double collisionIntensity = relativeCollision * relativeCollision;
								collisionIntensitySum += collisionIntensity;
								
							}
						}
					}
				}
			}
		}
		return collisionIntensitySum;
	}




}
	

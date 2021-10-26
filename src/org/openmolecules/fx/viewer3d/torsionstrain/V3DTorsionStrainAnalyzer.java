package org.openmolecules.fx.viewer3d.torsionstrain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.openmolecules.fx.viewer3d.MolCoordinatesChangeListener;
import org.openmolecules.fx.viewer3d.MolStructureChangeListener;
import org.openmolecules.fx.viewer3d.V3DMolecule;

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.BondRotationHelper;
import com.actelion.research.chem.conf.TorsionDB;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;

public class V3DTorsionStrainAnalyzer implements MolStructureChangeListener{
	
	private V3DMolecule fxmol;
	private HashMap<Integer,TorsionAngle> torsionAngles = new HashMap<Integer,TorsionAngle>();
	
	public V3DTorsionStrainAnalyzer(V3DMolecule fxmol) {
		TorsionDB.initialize(TorsionDB.MODE_ANGLES | TorsionDB.MODE_BINS);
		this.fxmol = fxmol;
		fxmol.addMoleculeStructureChangeListener(this);
		torsionAngles = new HashMap<Integer,TorsionAngle>();
	}
	/*
	public void init() {
		BondRotationHelper rotHelper = BondRotationHelper(fxmol.getMolecule());
		for(int b=0;b<fxmol.getMolecule().getBonds();b++) {
			for(int rotBondIndex=0;rotBondIndex<fxmol.getBondRotationHelper().getRotatableBonds().length;rotBondIndex++) {
				int rotBond = fxmol.getBondRotationHelper().getRotatableBonds()[rotBondIndex];
				if(b==rotBond) {
					String torsionID = fxmol.getBondRotationHelper().getTorsionIDs()[rotBondIndex];
					int atoms[] = fxmol.getBondRotationHelper().getTorsionAtoms()[rotBondIndex];
					TorsionAngle torsionAngle = new TorsionAngle(atoms, torsionID);
					torsionAngles.put(b, torsionAngle);
				}
			}
		}
			
	}
	*/
	
	public void init() {
		boolean[] isRotatableBond = new boolean[fxmol.getMolecule().getBonds()];
		TorsionDB.findRotatableBonds(fxmol.getMolecule(),false, isRotatableBond);
		for(int b=0;b<isRotatableBond.length;b++) {
			if(!isRotatableBond[b])
				continue;
			int[] torsionAtom = new int[4];
			String torsionID = TorsionDB.getTorsionID(fxmol.getMolecule(), b, torsionAtom, null);
			TorsionAngle torsionAngle = new TorsionAngle(torsionAtom, torsionID);
			torsionAngles.put(b, torsionAngle);
		}
	}



	@Override
	public void structureChanged() {
		init();
		
	}
	
	public int getStrainColor(int b) {
		return torsionAngles.get(b).getStrain(fxmol.getMolecule());
	}
	
	public Set<Integer> getRotBonds() {
		return torsionAngles.keySet();
	}
	
	public V3DMolecule getV3DMolecule() {
		return this.fxmol;
	}
	
	private static class TorsionAngle {
		int[] atoms;
		String torsionID;
		
		TorsionAngle(int[] atoms, String torsionID) {
			this.atoms = atoms;
			this.torsionID = torsionID;
		}
		
		public int getStrain(StereoMolecule mol) {
			double angle = TorsionDB.calculateTorsionExtended(mol,atoms);
			return TorsionDB.getTorsionStrainClass(torsionID, angle);
		}
		
		
	}

	

}

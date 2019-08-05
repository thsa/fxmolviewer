package org.openmolecules.render;

import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DPharmacophore;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.phesa.AcceptorPoint;
import com.actelion.research.chem.phesa.DonorPoint;
import com.actelion.research.chem.phesa.MolecularVolume;
import com.actelion.research.chem.phesa.PPGaussian;
import com.actelion.research.chem.phesa.ExclusionGaussian;

import javafx.application.Platform;
import javafx.scene.paint.PhongMaterial;

public class PharmacophoreArchitect {

	private PharmacophoreBuilder builder;
	private int exclusionDetail;


	
	public PharmacophoreArchitect(PharmacophoreBuilder builder) {
		this.builder = builder;
		exclusionDetail = 3;
	}
	
	private int ppRole(PPGaussian pp) {
		int id = 0;
		int atom = pp.getAtomId();
		if(pp.getPharmacophorePoint() instanceof AcceptorPoint) {
			id = ((AcceptorPoint)pp.getPharmacophorePoint()).getAcceptorID();
		}
		return ((id << MoleculeBuilder.ROLE_DETAIL_SHIFT) |MoleculeBuilder.ROLE_IS_PHARMACOPHORE | atom);
		}
	
	private int exclusionRole(ExclusionGaussian eg) {
		return ((exclusionDetail++ << MoleculeBuilder.ROLE_DETAIL_SHIFT) |MoleculeBuilder.ROLE_IS_PHARMACOPHORE | eg.getAtomId());
		
		
	}
	
	public void buildPharmacophore(MolecularVolume molVol, int fromAtom) {
		for(PPGaussian pp : molVol.getPPGaussians()) {
			if(fromAtom<=pp.getAtomId()) {
				Platform.runLater(() -> {
				builder.addPharmacophorePoint(ppRole(pp), pp);});
				//buildDirectionalityVector(pp);
			}
			}			
	
	}

}

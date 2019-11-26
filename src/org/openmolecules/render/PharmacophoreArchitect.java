package org.openmolecules.render;

import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DCustomizablePheSA;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.phesa.MolecularVolume;
import com.actelion.research.chem.phesa.pharmacophore.AcceptorPoint;
import com.actelion.research.chem.phesa.pharmacophore.ChargePoint;
import com.actelion.research.chem.phesa.pharmacophore.DonorPoint;
import com.actelion.research.chem.phesa.pharmacophore.PPGaussian;
import com.actelion.research.chem.phesa.ExclusionGaussian;

import javafx.application.Platform;
import javafx.scene.paint.PhongMaterial;

public class PharmacophoreArchitect {

	private PharmacophoreBuilder builder;
	private static int exclusionDetail = 0;
	private static int chargeShift = 3;


	
	public PharmacophoreArchitect(PharmacophoreBuilder builder) {
		this.builder = builder;
	}
	
	private int ppRole(PPGaussian pp) {
		int id = 0;
		int atom = pp.getAtomId();
		if(pp.getPharmacophorePoint() instanceof AcceptorPoint) {
			id = ((AcceptorPoint)pp.getPharmacophorePoint()).getAcceptorID();
		}
		if(pp.getPharmacophorePoint() instanceof ChargePoint) {
			id = chargeShift;
		}
		return ((id << MoleculeBuilder.ROLE_DETAIL_SHIFT) |MoleculeBuilder.ROLE_IS_PHARMACOPHORE | atom);
		}
	
	public static int exclusionRole(ExclusionGaussian eg) {
		return ((exclusionDetail++ << MoleculeBuilder.ROLE_DETAIL_SHIFT) |MoleculeBuilder.ROLE_IS_EXCLUSION | eg.getAtomId());
		
		
	}
	
	public void buildPharmacophore(MolecularVolume molVol, int fromAtom) {
		for(PPGaussian pp : molVol.getPPGaussians()) {
			if(fromAtom<=pp.getAtomId()) {
				Platform.runLater(() -> {
				builder.addPharmacophorePoint(ppRole(pp), pp);});
				//buildDirectionalityVector(pp);
			}
		}
		for(ExclusionGaussian eg : molVol.getExclusionGaussians()) {
				if(fromAtom<=eg.getAtomId()) {
					Platform.runLater(() -> {
					builder.addExclusionSphere(exclusionRole(eg), eg);});
					//buildDirectionalityVector(pp);
				}
			}			
	
	}

}


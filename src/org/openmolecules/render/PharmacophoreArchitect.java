package org.openmolecules.render;

import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DCustomizablePheSA;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.phesa.MolecularVolume;
import com.actelion.research.chem.phesa.pharmacophore.AcceptorPoint;
import com.actelion.research.chem.phesa.pharmacophore.AromRingPoint;
import com.actelion.research.chem.phesa.pharmacophore.ChargePoint;
import com.actelion.research.chem.phesa.pharmacophore.DonorPoint;
import com.actelion.research.chem.phesa.pharmacophore.PPGaussian;
import com.actelion.research.chem.phesa.VolumeGaussian;

import javafx.application.Platform;
import javafx.scene.paint.PhongMaterial;

public class PharmacophoreArchitect {

	private PharmacophoreBuilder builder;
	private static int chargeShift = 3;
	private static int aromRingShift = 4;


	
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
		if(pp.getPharmacophorePoint() instanceof AromRingPoint) {
			id = aromRingShift;
		}
		return ((id << MoleculeBuilder.ROLE_DETAIL_SHIFT) |MoleculeBuilder.ROLE_IS_PHARMACOPHORE | atom);
		}
	
	public static int exclusionRole(VolumeGaussian eg) {
		int exclusionRole = ( MoleculeBuilder.ROLE_DETAIL_SHIFT) |MoleculeBuilder.ROLE_IS_EXCLUSION | eg.getAtomId();
		return exclusionRole;
		
		
	}
	
	public void buildPharmacophore(MolecularVolume molVol, int fromAtom) {
		for(PPGaussian pp : molVol.getPPGaussians()) {
			if(fromAtom<=pp.getAtomId()) {
				Platform.runLater(() -> {
				builder.addPharmacophorePoint(pp);});
				//buildDirectionalityVector(pp);
			}
		}
		for(VolumeGaussian eg : molVol.getVolumeGaussians()) {
				if(fromAtom<=eg.getAtomId()) {
					Platform.runLater(() -> {
					builder.addExclusionSphere(eg);});
					//buildDirectionalityVector(pp);
				}
			}			
	
	}

}


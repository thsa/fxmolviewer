package org.openmolecules.render;

import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DCustomizablePheSA;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.phesa.MolecularVolume;
import com.actelion.research.chem.phesa.pharmacophore.pp.AcceptorPoint;
import com.actelion.research.chem.phesa.pharmacophore.pp.AromRingPoint;
import com.actelion.research.chem.phesa.pharmacophore.pp.ChargePoint;
import com.actelion.research.chem.phesa.pharmacophore.pp.DonorPoint;
import com.actelion.research.chem.phesa.pharmacophore.pp.PPGaussian;
import com.actelion.research.chem.phesa.VolumeGaussian;

import javafx.application.Platform;
import javafx.scene.paint.PhongMaterial;

public class PharmacophoreArchitect {

	private PharmacophoreBuilder builder;

	
	public PharmacophoreArchitect(PharmacophoreBuilder builder) {
		this.builder = builder;
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


package org.openmolecules.render;


import com.actelion.research.chem.phesa.AtomicGaussian;
import com.actelion.research.chem.phesa.BindingSiteVolume;
import com.actelion.research.chem.phesa.pharmacophore.pp.PPGaussian;

import javafx.application.Platform;

public class BindingSiteVolumeArchitect {

	private BindingSiteVolumeBuilder builder;
	
	public BindingSiteVolumeArchitect(BindingSiteVolumeBuilder builder) {
		this.builder = builder;
	}
	
	
	public void buildPharmacophore(BindingSiteVolume bsVol) {
		for(PPGaussian pp : bsVol.getPPGaussians()) {
			Platform.runLater(() -> {
				builder.addSimplePharmacophorePoint(pp);});
			}		
		
		for(AtomicGaussian ag : bsVol.getAtomicGaussians()) {
			Platform.runLater(() -> {
				builder.addShapePoint(ag);});
			}	
	}
	
	

}

package org.openmolecules.render;

import com.actelion.research.chem.phesa.AtomicGaussian;
import com.actelion.research.chem.phesa.pharmacophore.pp.PPGaussian;
import com.actelion.research.chem.phesa.pharmacophore.pp.PPGaussian;

public interface BindingSiteVolumeBuilder {
	
	public void addSimplePharmacophorePoint(PPGaussian pp);
	
	public void addShapePoint(AtomicGaussian ag);
	
	

}

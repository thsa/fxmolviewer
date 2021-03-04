package org.openmolecules.render;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.phesa.VolumeGaussian;
import com.actelion.research.chem.phesa.pharmacophore.pp.IPharmacophorePoint;
import com.actelion.research.chem.phesa.pharmacophore.pp.PPGaussian;

import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;

/**
 * Method set to be implemented by any class that uses MoleculeArchitect
 * to construct a molecule for a 3D environment for rendering.
 */
public interface PharmacophoreBuilder {
	
	public static final double PP_RADIUS = 0.5;
	public static final double PP_RADIUS_CHARGE = 0.8;
	public static final double CYLINDER_RADIUS = 0.1;
	public static final double VECTOR_LENGTH = 1.5;

	public void addPharmacophorePoint(PPGaussian pp);
	public void addExclusionSphere(VolumeGaussian eg);
	//public void addDirectionalityArrow(int role, double radius, double length, Coordinates center, double rotationY, double rotationZ, PhongMaterial material);

	}

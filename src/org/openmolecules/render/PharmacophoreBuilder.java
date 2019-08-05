package org.openmolecules.render;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.phesa.ExclusionGaussian;
import com.actelion.research.chem.phesa.IPharmacophorePoint;
import com.actelion.research.chem.phesa.PPGaussian;

import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;

/**
 * Method set to be implemented by any class that uses MoleculeArchitect
 * to construct a molecule for a 3D environment for rendering.
 */
public interface PharmacophoreBuilder {
	
	public static final double PP_RADIUS = 0.5;
	public static final double CYLINDER_RADIUS = 0.2;
	public static final double VECTOR_LENGTH = 1.5;

	public void addPharmacophorePoint(int role, PPGaussian pp);
	public void addExclusionSphere(int role, ExclusionGaussian eg);
	//public void addDirectionalityArrow(int role, double radius, double length, Coordinates center, double rotationY, double rotationZ, PhongMaterial material);

	}

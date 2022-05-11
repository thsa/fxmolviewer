package org.openmolecules.render;

import com.actelion.research.chem.Coordinates;

public interface TorsionStrainVisBuilder {
	
	public void addTorsionCylinder(int role, double radius, double length, Coordinates c, double rotationY, double rotationZ, int argb);

}

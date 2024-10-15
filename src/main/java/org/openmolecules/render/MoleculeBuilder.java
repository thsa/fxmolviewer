package org.openmolecules.render;

import com.actelion.research.chem.Coordinates;

/**
 * Method set to be implemented by any class that uses MoleculeArchitect
 * to construct a molecule or other object for a 3D environment for rendering.
 */
public interface MoleculeBuilder {
	public void init();
	public void done();
	public void addAtomSphere(int role, Coordinates c, double radius, int argb);
	public void addBondCylinder(int role, double radius, double length, Coordinates c, double rotationY, double rotationZ, int argb);
	public void addAtomCone(int role, double radius, double height, Coordinates c, double rotationY, double rotationZ, int argb);
	}

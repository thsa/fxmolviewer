package org.openmolecules.render;

import com.actelion.research.chem.Coordinates;

/**
 * Method set to be implemented by any class that uses MoleculeArchitect
 * to construct a molecule for a 3D environment for rendering.
 */
public interface MoleculeBuilder {
	public static final int ROLE_INDEX_BITS = 0x003FFFFF;
	public static final int ROLE_IS_PHARMACOPHORE = 0x00800000;
	public static final int ROLE_IS_EXCLUSION = 0x00400000;
	public static final int ROLE_IS_ATOM = 0x01000000;
	public static final int ROLE_IS_BOND = 0x02000000;
	public static final int ROLE_DETAIL_SHIFT = 26;
	public static final int ROLE_ORDER_SHIFT = 30;

	public void init();
	public void addSphere(int role, Coordinates c, double radius, int argb);
	public void addCylinder(int role, double radius, double length, Coordinates c, double rotationY, double rotationZ, int argb);
	public void addCone(int role, double radius, double length, Coordinates c, double rotationY, double rotationZ, int argb);
	public void done();
	}

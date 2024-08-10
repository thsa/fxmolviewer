package org.openmolecules.render;

import com.actelion.research.chem.Coordinates;

public interface PrimitiveBuilder {
	public void init();
	public void done();
	public Object addSphere(Coordinates c, double radius, int argb, int divisions);
	public Object addCylinder(double radius, double length, Coordinates c, double rotationY, double rotationZ, int argb, int divisions);
	public Object addCone(double radius, double height, Coordinates c, double rotationY, double rotationZ, int argb, int divisions);

}

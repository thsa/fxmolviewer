package org.openmolecules.fx.viewer3d;

import com.actelion.research.chem.Coordinates;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import org.openmolecules.mesh.Cone;
import org.openmolecules.render.PrimitiveBuilder;

import java.util.TreeMap;

public class V3DPrimitiveBuilder implements PrimitiveBuilder {
	private static TreeMap<Integer,PhongMaterial> sMaterialMap;
	private final V3DRotatableGroup mParent;

	@Override
	public void init() {}

	@Override
	public void done() {}

	public V3DPrimitiveBuilder(V3DRotatableGroup parent) {
		mParent = parent;
		if (sMaterialMap == null)
			sMaterialMap = new TreeMap<>();
		}

	@Override
	public Object addSphere(Coordinates c, double radius, int argb, int divisions) {
		PhongMaterial material = getMaterial(argb);
		Sphere sphere = new Sphere(radius, divisions);
		sphere.setMaterial(material);
		sphere.setTranslateX(c.x);
		sphere.setTranslateY(c.y);
		sphere.setTranslateZ(c.z);
		mParent.getChildren().add(sphere);
		return sphere;
	}

	@Override
	public Object addCylinder(double radius, double length, Coordinates center, double rotationY, double rotationZ, int argb, int divisions) {
		PhongMaterial material = getMaterial(argb);
		Cylinder cylinder = new Cylinder(radius, length, divisions);
		cylinder.setMaterial(material);
		cylinder.setTranslateX(center.x);
		cylinder.setTranslateY(center.y);
		cylinder.setTranslateZ(center.z);

		Transform r1 = new Rotate(90+180/Math.PI*rotationY, Rotate.X_AXIS);
		Transform r2 = new Rotate(90+180/Math.PI*rotationZ, Rotate.Z_AXIS);
		cylinder.getTransforms().add(r2);
		cylinder.getTransforms().add(r1);
		mParent.getChildren().add(cylinder);
		return cylinder;
	}

	@Override
	public Object addCone(double radius, double height, Coordinates center, double rotationY, double rotationZ, int argb, int divisions) {
		PhongMaterial material = getMaterial(argb);
		Cone cone = new Cone(radius, height, divisions);
		cone.setMaterial(material);
		cone.setTranslateX(center.x);
		cone.setTranslateY(center.y);
		cone.setTranslateZ(center.z);

		Transform r1 = new Rotate(90+180/Math.PI*rotationY, Rotate.X_AXIS);
		Transform r2 = new Rotate(90+180/Math.PI*rotationZ, Rotate.Z_AXIS);
		cone.getTransforms().add(r2);
		cone.getTransforms().add(r1);
		mParent.getChildren().add(cone);
		return cone;
	}

	public static PhongMaterial getMaterial(int argb) {
		PhongMaterial material = sMaterialMap.get(argb);
		if (material == null) {
			if (argb == 0) {    // transparent material
				material = new PhongMaterial();
				material.setDiffuseColor(Color.rgb(0, 0, 0, 0.0));
			}
			else {              // opaque material
				Color color = Color.rgb((argb & 0x00FF0000) >> 16, (argb & 0x0000FF00) >> 8, argb & 0x000000FF);
				material = new PhongMaterial();
				material.setDiffuseColor(color.darker());
				material.setSpecularColor(color);
			}
			sMaterialMap.put(argb, material);
		}
		return material;
	}
}

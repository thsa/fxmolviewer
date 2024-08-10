/*
 * FXMolViewer, showing and manipulating molecules and protein structures in 3D.
 * Copyright (C) 2019 Thomas Sander

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * @author Thomas Sander
 */

package org.openmolecules.fx.viewer3d.nodes;

import com.actelion.research.chem.Coordinates;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import org.openmolecules.fx.sunflow.RayTraceOptions;
import org.openmolecules.fx.viewer3d.RotatableGroup;
import org.openmolecules.render.PrimitiveBuilder;

public class DashedRod extends RotatableGroup {
	private static final float RADIUS = 0.02f;
	private static final float DASH_LENGTH = 0.18f;
	private static final float GAP_LENGTH = 0.12f;
	private final Color mColor;

	private final float mRadius;

	public DashedRod(Point3D p1, Point3D p2, Color color) {
		this(p1,p2,color,RADIUS,DASH_LENGTH,GAP_LENGTH);
	}

	public DashedRod(Point3D p1, Point3D p2, Color color, float radius, float dashLength, float gapLength) {
		mColor = color;
		mRadius = radius;
		PhongMaterial material = createMaterial(color, 1.0);

		float height = (float)p1.distance(p2);
		int gaps = (height <= dashLength) ? 0
				 : (int)((height+dashLength)/(dashLength+gapLength));

		if (gaps == 0) {
			Cylinder cylinder = new Cylinder(radius, height);
			cylinder.setMaterial(material);
			getChildren().add(cylinder);
			}
		else {
			float firstLength = (height + dashLength - gaps*(dashLength+gapLength))/2;
			Cylinder cylinder1 = new Cylinder(radius, firstLength);
			cylinder1.setTranslateY(firstLength/2-height/2);
			cylinder1.setMaterial(material);
			getChildren().add(cylinder1);

			for (int i=0; i<gaps-1; i++) {
				Cylinder cylinder = new Cylinder(radius, dashLength);
				cylinder.setTranslateY(firstLength+gapLength+i*(dashLength+gapLength)+dashLength/2-height/2);
				cylinder.setMaterial(material);
				getChildren().add(cylinder);
				}

			Cylinder cylinder2 = new Cylinder(radius, firstLength);
			cylinder2.setTranslateY(height/2-firstLength/2);
			cylinder2.setMaterial(material);
			getChildren().add(cylinder2);
			}

		Point3D center = p1.midpoint(p2);
		setTranslateX(center.getX());
		setTranslateY(center.getY());
		setTranslateZ(center.getZ());

		double angle1 = -180/Math.PI*getAngleXY(p1, p2);
		double angle2 = -180/Math.PI*Math.asin((p1.getZ()-p2.getZ())/height);

		Transform r = new Rotate(angle1, Rotate.Z_AXIS);
		r = r.createConcatenation(new Rotate(angle2, Rotate.X_AXIS));
		getTransforms().add(r);
		}

	public void build(PrimitiveBuilder builder, RayTraceOptions options) {
		for (Node node : getChildren()) {
			Cylinder cylinder = (Cylinder)node;
			double height = cylinder.getHeight();
			Coordinates c1 = new Coordinates();
			Coordinates c2 = new Coordinates();
			options.localToSunflow(this, cylinder.getTranslateX(), cylinder.getTranslateY()-height/2, cylinder.getTranslateZ(), c1);
			options.localToSunflow(this, cylinder.getTranslateX(), cylinder.getTranslateY()+height/2, cylinder.getTranslateZ(), c2);
			Coordinates center = new Coordinates(c1).center(c2);
			Coordinates delta = c2.subC(c1);

			double d = delta.getLength();
			double dxy = Math.sqrt(delta.x * delta.x + delta.y * delta.y);
			double b = Math.asin(c2.z > c1.z ? dxy / d : -dxy / d);
			double c = (delta.x < 0.0) ? Math.atan(delta.y / delta.x) + Math.PI
					: (delta.x > 0.0) ? Math.atan(delta.y / delta.x)
					: (delta.y > 0.0) ? Math.PI / 2 : -Math.PI / 2;

			int argb = (Math.round((float)mColor.getOpacity() * 255f) << 24)
					 + (Math.round((float)mColor.getRed() * 255f) << 16)
					 + (Math.round((float)mColor.getGreen() * 255f) << 8)
					 + (Math.round((float)mColor.getBlue() * 255f));

			builder.addCylinder(mRadius, d, center, b, c, argb, cylinder.getDivisions());
			}
	}

	/*
	public void update(Point3D p1, Point3D p2) {
		Point3D center = p1.midpoint(p2);
		float distance = (float)p1.distance(p2);
		setTranslateX(center.getX());
		setTranslateY(center.getY());
		setTranslateZ(center.getZ());
		double angle1 = -180/Math.PI*getAngleXY(p1, p2);
		double angle2 = -180/Math.PI*Math.asin((p1.getZ()-p2.getZ())/distance);

		Transform r = new Rotate(angle1, Rotate.Z_AXIS);
		r = r.createConcatenation(new Rotate(angle2, Rotate.X_AXIS));
		getTransforms().clear();
		getTransforms().add(r);
	}
	*/
	
	public Color getColor() {
		return mColor;
	}
}

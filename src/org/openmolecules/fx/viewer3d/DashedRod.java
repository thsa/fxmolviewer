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

package org.openmolecules.fx.viewer3d;

import javafx.geometry.Point3D;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;

public class DashedRod extends RotatableGroup {
	private static final float RADIUS = 0.02f;
	private static final float DASH_LENGTH = 0.18f;
	private static final float GAP_LENGTH = 0.12f;
	private Color mColor;

	public DashedRod(Point3D p1, Point3D p2, Color color) {
		PhongMaterial material = createMaterial(color, 1.0);
		mColor = color;

		float distance = (float)p1.distance(p2);
		int gaps = (distance <= DASH_LENGTH) ? 0
				 : (int)((distance+DASH_LENGTH)/(DASH_LENGTH+GAP_LENGTH));

		if (gaps == 0) {
			Cylinder cylinder = new Cylinder(RADIUS, distance);
			cylinder.setMaterial(material);
			getChildren().add(cylinder);
			}
		else {
			float firstLength = (distance + DASH_LENGTH - gaps*(DASH_LENGTH+GAP_LENGTH))/2;
			Cylinder cylinder1 = new Cylinder(RADIUS, firstLength);
			cylinder1.setTranslateY(firstLength/2-distance/2);
			cylinder1.setMaterial(material);
			getChildren().add(cylinder1);

			for (int i=0; i<gaps-1; i++) {
				Cylinder cylinder = new Cylinder(RADIUS, DASH_LENGTH);
				cylinder.setTranslateY(firstLength+GAP_LENGTH+i*(DASH_LENGTH+GAP_LENGTH)+DASH_LENGTH/2-distance/2);
				cylinder.setMaterial(material);
				getChildren().add(cylinder);
				}

			Cylinder cylinder2 = new Cylinder(RADIUS, firstLength);
			cylinder2.setTranslateY(distance/2-firstLength/2);
			cylinder2.setMaterial(material);
			getChildren().add(cylinder2);
			}

		Point3D center = p1.midpoint(p2);
		setTranslateX(center.getX());
		setTranslateY(center.getY());
		setTranslateZ(center.getZ());

		double angle1 = -180/Math.PI*getAngleXY(p1, p2);
		double angle2 = -180/Math.PI*Math.asin((p1.getZ()-p2.getZ())/distance);

		Transform r = new Rotate(angle1, Rotate.Z_AXIS);
		r = r.createConcatenation(new Rotate(angle2, Rotate.X_AXIS));
		getTransforms().add(r);
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

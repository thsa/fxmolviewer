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

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;

import java.util.ArrayList;

public class RotatableGroup extends Group implements ChangeListener<Number> {
	private Transform mRotation;
	private ArrayList<TransformationListener> mTransformationListenerList;

	public RotatableGroup() {
		mRotation = new Rotate();
		getTransforms().add(mRotation);
		mTransformationListenerList = new ArrayList<TransformationListener>();
		translateXProperty().addListener(this);
		translateYProperty().addListener(this);
		translateZProperty().addListener(this);
		}

	public Transform getRotation() {
		return mRotation;
		}

	public void addRotationListener(TransformationListener l) {
		mTransformationListenerList.add(l);
		}

	public void removeRotationListener(TransformationListener l) {
		mTransformationListenerList.remove(l);
	}

	public void clearTransform() {
		mRotation = new Rotate();
		setTransform(mRotation);
		}

	public void setTransform(Transform t) {
		mRotation = t;
		getTransforms().set(0, mRotation);
		setTranslateX(0.0);
		setTranslateY(0.0);
		setTranslateZ(0.0);
		fireTransformationChanged();
		}

	@Override
	public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
		fireTransformationChanged();
		}

	public void rotate(Rotate r) {
		mRotation = r.createConcatenation(mRotation);
		getTransforms().set(0, mRotation);
		fireTransformationChanged();
		}

	private void fireTransformationChanged() {
		for (TransformationListener l: mTransformationListenerList)
			l.transformationChanged();
		}

	/**
	 * Calculates the angle of the line from p1 to p2
	 * projected into the x/y plane. With Y facing upwards and X right,
	 * if the line points in Y direction, ten angle is 0.0 increasing
	 * in clockwise direction.
	 * @param p1
	 * @param p2
	 * @return -PI < angle < PI
	 */
	public double getAngleXY(Point3D p1, Point3D p2) {
		double dx = p2.getX() - p1.getX();
		double dy = p2.getY() - p1.getY();

		if (dy == 0.0)
			return (dx > 0.0) ? Math.PI/2.0 : -Math.PI/2.0;

		double angle = Math.atan(dx/dy);
		if (dy < 0.0)
			return (dx < 0.0) ? angle - Math.PI : angle + Math.PI;

		return angle;
		}

	public PhongMaterial createMaterial(Color c, double opacity) {
		PhongMaterial material = new PhongMaterial();
		Color dc = c.darker();
		material.setDiffuseColor(opacity != 1 ? new Color(c.getRed(), c.getGreen(), c.getBlue(), opacity) : c);
		material.setSpecularColor(dc);
		return material;
		}
	}

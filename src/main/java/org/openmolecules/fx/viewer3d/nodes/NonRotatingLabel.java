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



import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Transform;
import org.openmolecules.fx.viewer3d.LabelDeletionListener;
import org.openmolecules.fx.viewer3d.RotatableGroup;
import org.openmolecules.fx.viewer3d.TransformationListener;

public class NonRotatingLabel extends Label implements TransformationListener {
	private static final String FONT_NAME = "Tahoma";
	private static final int FONT_SIZE = 40;
	private static final double SCALE = 0.012;

	private static Font sFont;

	private Parent mParent;
	private Point3D mPoint;
	private double mWidth, mHeight;
	private ContextMenu mMenu;
	private boolean mDeleted;
	private LabelDeletionListener mListener;
	

	/**
	 * Create a correctly sized and positioned label.
	 * @parem parent
	 * @param text
	 * @param point
	 * @param color
	 */
	private NonRotatingLabel(Parent parent, String text, Point3D point, Color color) {
		super(text);
		mDeleted = false;
		mParent = parent;
		mPoint = point;
		if (sFont == null)
			sFont = Font.font(FONT_NAME, FONT_SIZE);

		Text t = new Text(text);
		t.setFont(sFont);
		Bounds bounds = t.getLayoutBounds();
		mWidth = bounds.getWidth() * SCALE;
		mHeight = bounds.getHeight() * SCALE;

		if (color == null)
			color = Color.AQUA;

		setFont(sFont);
		setTextFill(color);

		getTransforms().add(Transform.scale(SCALE, SCALE, 0, 0));

		updatePosition();
		
		mMenu = new ContextMenu();
		MenuItem itemRemove = new MenuItem("Remove");
		itemRemove.setOnAction(e -> {
			mDeleted = true;
			mListener.labelDeleted(this);
		});
		mMenu.getItems().add(itemRemove);
	}
	

	public void setLabelDeletionListener(LabelDeletionListener l) {
		mListener = l;
	}

	public static NonRotatingLabel create(Parent parent, String text, Point3D point, Color color) {
		NonRotatingLabel label = new NonRotatingLabel(parent, text, point, color);
		if (parent instanceof RotatableGroup)
			((RotatableGroup) parent).addRotationListener(label);
		while (parent.getParent() != null) {
			parent = parent.getParent();
			}

		((Group)parent).getChildren().add(label);
		return label;
		}

	public void remove(Parent parent) {
		while (parent.getParent() != null) {
			if (parent instanceof RotatableGroup)
				((RotatableGroup) parent).removeRotationListener(this);
			parent = parent.getParent();
			}

		((Group)parent).getChildren().remove(this);
		}

	private void updatePosition() {
		Point3D p = mParent.localToScene(mPoint);
		setTranslateX(p.getX() - mWidth/2);
		setTranslateY(p.getY() - mHeight/2);
		setTranslateZ(p.getZ() - 0.5);
		}

	public void update(Point3D point, String text) {
		mPoint = point;
		setText(text);
		Point3D p = mParent.localToScene(mPoint);
		setTranslateX(p.getX() - mWidth/2);
		setTranslateY(p.getY() - mHeight/2);
		setTranslateZ(p.getZ() - 0.5);
	}
	
	public boolean isDeleted() {
		return mDeleted;
	}

	@Override
	public void transformationChanged() {
		updatePosition();
		}
	
	public void showMenu(double x, double y) {
		mMenu.show(this, x, y);
        }
	}

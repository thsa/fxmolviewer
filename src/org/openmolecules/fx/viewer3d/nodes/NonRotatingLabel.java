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



import org.openmolecules.fx.viewer3d.LabelDeletionListener;
import org.openmolecules.fx.viewer3d.RotatableGroup;
import org.openmolecules.fx.viewer3d.TransformationListener;

import javafx.event.EventHandler;
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
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;

public class NonRotatingLabel extends Label implements TransformationListener {
	private static final String FONT_NAME = "Tahoma";
	private static final int FONT_SIZE = 40;
	private static final double SCALE = 0.012;

	private static Font sFont;

	private Parent mParent;
	private Point3D mP1,mP2;
	private double mWidth, mHeight;
	private ContextMenu mMenu;
	private boolean mDeleted;
	private LabelDeletionListener mListener;
	

	/**
	 * Create a correctly sized and positioned label.
	 * @parem parent
	 * @param text
	 * @param p1
	 * @param p2
	 * @param color
	 */
	private NonRotatingLabel(Parent parent, String text, Point3D p1, Point3D p2, Color color) {
		super(text);
		mDeleted = false;
		mParent = parent;
		mP1 = p1;
		mP2 = p2;
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
		MenuItem deleteMeasurement = new MenuItem("Remove");
		deleteMeasurement.setOnAction(e -> {mDeleted = true;
		mListener.labelDeleted(this);
		});
		mMenu.getItems().add(deleteMeasurement);
	}
	

	public void setLabelDeletionListener(LabelDeletionListener l) {
		mListener = l;
	}

	public static NonRotatingLabel create(Parent parent, String text, Point3D p1, Point3D p2, Color color) {
		NonRotatingLabel label = new NonRotatingLabel(parent, text, p1, p2, color);
		while (parent.getParent() != null) {
			if (parent instanceof RotatableGroup)
				((RotatableGroup) parent).addRotationListener(label);
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
		Point3D p = mParent.localToScene(mP1.midpoint(mP2));
		setTranslateX(p.getX() - mWidth/2);
		setTranslateY(p.getY() - mHeight/2);
		setTranslateZ(p.getZ() - 0.5);
		
		}
	public void update(Point3D p1, Point3D p2, String text) {
		mP1 = p1;
		mP2 = p2;
		setText(text);
		Point3D p = mParent.localToScene(mP1.midpoint(mP2));
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

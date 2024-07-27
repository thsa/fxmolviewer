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

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.input.KeyCode;

public class V3DKeyHandler {
	public static boolean sControlIsDown;
	private Group mAxis;

	public V3DKeyHandler(final V3DScene scene) {
		final PerspectiveCamera camera = (PerspectiveCamera)scene.getCamera();

		scene.setOnKeyPressed(ke -> {
				switch (ke.getCode()) {
				case CONTROL:
					sControlIsDown = true;
					break;
				case Z:
					scene.setCameraZ(V3DScene.CAMERA_INITIAL_Z);
					break;
				case X:
					if (mAxis != null)
						mAxis.setVisible(!mAxis.isVisible());
					break;
				case Y:
					double fieldOfView = camera.getFieldOfView();
					double screenSize = camera.isVerticalFieldOfView() ? scene.getHeight() : scene.getWidth();
					double sizeAtZ0 = -camera.getTranslateZ() * Math.tan(Math.PI*fieldOfView/90);
					System.out.println("sizeAtZ0:"+sizeAtZ0);
					break;
				default:
					int index = ke.getCode().ordinal() - KeyCode.DIGIT1.ordinal();
					if (index >= 0 && index < 9) {
						int molIndex = 0;
						for (Node node:scene.getWorld().getChildren()) {
							if (node instanceof V3DMolecule) {
								if (index == molIndex) {
									V3DMolecule fxmol = (V3DMolecule)node;
									fxmol.setVisible(!fxmol.isVisible());
									break;
									}
								molIndex++;
								}
							}
						}
					break;
					}
				} );
		scene.setOnKeyReleased(ke -> {
				if (ke.getCode() == KeyCode.CONTROL) {
					sControlIsDown = false;
					}
				} );
		}

	public void setAxis(Group axis) {
		mAxis = axis;
		}
	}

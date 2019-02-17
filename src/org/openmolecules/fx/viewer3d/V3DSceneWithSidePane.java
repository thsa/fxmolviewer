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
import org.controlsfx.control.HiddenSidesPane;
import org.openmolecules.fx.viewer3d.panel.SidePane;

/**
 * Created by thomas on 25.09.16.
 */
public class V3DSceneWithSidePane extends HiddenSidesPane {
	private V3DScene mScene3D;
	private SidePane mMoleculePanel;

	public V3DSceneWithSidePane() {
		this(1024, 768);
	}

	public V3DSceneWithSidePane(int width, int height) {
		mScene3D = new V3DScene(new Group(), width, height);

		mMoleculePanel = new SidePane(mScene3D, this);
		mMoleculePanel.getStyleClass().add("side-panel");
		setLeft(mMoleculePanel);
//		setContent(mScene3D);
		setContent(new V3DSceneWithSelection(mScene3D));
	}

	public V3DScene getScene3D() {
		return mScene3D;
	}

	public SidePane getMoleculePanel() {
		return mMoleculePanel;
	}
}

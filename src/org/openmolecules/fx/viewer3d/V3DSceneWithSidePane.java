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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.openmolecules.fx.viewer3d.panel.MolGroupPane;
import org.openmolecules.fx.viewer3d.panel.SlidingHBox;

import java.util.EnumSet;

/**
 * Created by thomas on 25.09.16.
 */
public class V3DSceneWithSidePane extends BorderPane {
	private V3DScene mScene3D;
	private MolGroupPane mMoleculePanel;
	
	public V3DSceneWithSidePane(EnumSet<V3DScene.ViewerSettings> settings) {
		this(1024, 768, settings);
	}

	public V3DSceneWithSidePane(int width, int height, EnumSet<V3DScene.ViewerSettings> settings) {
		mScene3D = new V3DScene(new Group(), width, height, settings);
		BorderPane center = new BorderPane();
		mMoleculePanel = new MolGroupPane(mScene3D);
		mMoleculePanel.getStyleClass().add("side-panel");
		SlidingHBox slidingBox = new SlidingHBox(mMoleculePanel);
		center.setLeft(slidingBox.getBox());
		Pane dummyPane = new Pane();
		dummyPane.setVisible(false);
		dummyPane.setPickOnBounds(false);
		center.setCenter(dummyPane);
	    StackPane stackPane = new StackPane();
	    V3DSceneWithSelection sceneWithSelection = new V3DSceneWithSelection(mScene3D);
	    stackPane.getChildren().add(sceneWithSelection);
	    stackPane.getChildren().add(center);
	    center.setPickOnBounds(false);
		setCenter(stackPane);
	}

	public V3DScene getScene3D() {
		return mScene3D;
	}

	public MolGroupPane getMoleculePanel() {
		return mMoleculePanel;
	}
}

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

package org.openmolecules.fx.viewerapp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.stage.Stage;
import org.openmolecules.fx.viewer3d.V3DScene;
import org.openmolecules.fx.viewer3d.V3DSceneWithSidePane;

import java.io.File;
import java.util.EnumSet;
import java.util.Optional;

public class ViewerApp extends Application {
	private static final int INITIAL_WIDTH = 1200;
	private static final int INITIAL_HEIGHT = 900;

	public static void main(String[] args) {
		launch(args);
		}

	@Override
	public void start(Stage primaryStage) {
		String modeString = System.getProperty("mode", "viewer");
		String path = System.getProperty("file", "");
		int mode = -1;
		try { mode = Integer.parseInt(modeString.substring(modeString.length()-1)); } catch (NumberFormatException nfe) {}

		EnumSet<V3DScene.ViewerSettings> sceneMode = V3DScene.GENERAL_MODE;

		Parent view;
		V3DScene scene3D;

		V3DSceneWithSidePane sceneWithSidePane =  new V3DSceneWithSidePane(sceneMode);
		scene3D = sceneWithSidePane.getScene3D();
		view = sceneWithSidePane;

		Scene scene = new Scene(view, INITIAL_WIDTH, INITIAL_HEIGHT, true, SceneAntialiasing.BALANCED);
		String css = getClass().getResource("/resources/molviewer.css").toExternalForm();
		scene.getStylesheets().add(css);

		scene.widthProperty().addListener((observableValue, number, t1) -> scene3D.setWidth(scene.getWidth()));
		scene.heightProperty().addListener((observableValue, number, t1) -> scene3D.setHeight(scene.getHeight()));

		primaryStage.setTitle("Molecule Viewer");
		primaryStage.setScene(scene);
		primaryStage.show();
		if (!path.isEmpty())
			Platform.runLater(() -> new StartOptions(StartOptions.MODE_PDB_ENTRY, path.substring(1+path.lastIndexOf(File.separatorChar), path.lastIndexOf('.')), path, true).initializeScene(scene3D) );
		else if (mode != -1)
			Platform.runLater(() -> new StartOptions(StartOptions.MODE_SMALL_MOLECULES, null, null, false).initializeScene(scene3D) );
		else if (System.getProperty("test") != null)
			Platform.runLater(() -> showStartOptionDialog(scene3D) );

		scene3D.setWidth(scene.getWidth());
		scene3D.setHeight(scene.getHeight());
	}

	private static void showStartOptionDialog(V3DScene scene) {
		Optional<StartOptions> result = new StartOptionDialog(scene.getScene().getWindow(), null).showAndWait();
		result.ifPresent(options -> {
			options.initializeScene(scene);
		} );
	}
}

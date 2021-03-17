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
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.stage.Stage;
import org.openmolecules.fx.viewer3d.*;

import java.io.File;
import java.util.Optional;

public class ViewerApp extends Application {
	private V3DSceneWithSidePane mViewer;

	public static void main(String[] args) {
		launch(args);
		}

	@Override
	public void start(Stage primaryStage) {
		String modeString = System.getProperty("mode", "viewer");
		String path = System.getProperty("file", "");
		boolean isEditor = modeString.startsWith("editor");
		int mode = -1;
		try { mode = Integer.parseInt(modeString.substring(modeString.length()-1)); } catch (NumberFormatException nfe) {}
		mViewer =  new V3DSceneWithSidePane(V3DScene.GENERAL_MODE);
		String css = getClass().getResource("/resources/molviewer.css").toExternalForm();
		Scene scene = new Scene(mViewer, 1024, 768, true, SceneAntialiasing.BALANCED);
		scene.getStylesheets().add(css);
		mViewer.getScene3D().widthProperty().bind(scene.widthProperty());
		mViewer.getScene3D().heightProperty().bind(scene.heightProperty());
		primaryStage.setTitle("Molecule Viewer");
		primaryStage.setScene(scene);
		primaryStage.show();
		if (path.length() != 0)
			Platform.runLater(() -> new StartOptions(StartOptions.MODE_PDB_ENTRY, path.substring(1+path.lastIndexOf(File.separatorChar), path.lastIndexOf('.')), path.substring(0, path.lastIndexOf(File.separatorChar)+1), true).initializeScene(mViewer.getScene3D()) );
		else if (mode != -1)
			Platform.runLater(() -> new StartOptions(StartOptions.MODE_SMALL_MOLECULES, null, null, false).initializeScene(mViewer.getScene3D()) );
		else if (System.getProperty("test") != null)
			Platform.runLater(() -> showStartOptionDialog(mViewer.getScene3D()) );
	}

	private static void showStartOptionDialog(V3DScene scene) {
		Optional<StartOptions> result = new StartOptionDialog(scene.getScene().getWindow(), null).showAndWait();
		result.ifPresent(options -> {
			options.initializeScene(scene);
		} );
	}

}

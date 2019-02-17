/*
 * Copyright 2017 Thomas Sander, Therwilerstrasse 41, CH-4153 Reinach, Switzerland
 *
 * This file is part of openmolecules.org's 3D-Molecule-Viewer.
 *
 * 3D-Molecule-Viewer is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * 3D-Molecule-Viewer is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with 3D-Molecule-Viewer.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Thomas Sander
 */

package org.openmolecules.fx.viewer3d.panel;

import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DScene;
import org.openmolecules.fx.viewer3d.V3DSceneWithToolsPane;
import org.openmolecules.fx.viewer3d.tools.MoleculeFileReader;

import java.io.File;
//import org.openmolecules.fx.viewer3d.MoleculeMinimizer;


/**
 * Created by JW on 15.1.17
 */
public class ToolsPane extends Accordion  {
	private V3DScene mScene3D;
	private V3DSceneWithToolsPane mScene3DWithToolsPane;
	private Stage mPrimaryStage;
	//private CheckBox mCheckBoxPin;


	public ToolsPane(final Stage primaryStage, final V3DSceneWithToolsPane scene3DWithToolsPane) {
		super();
		mScene3D = scene3DWithToolsPane.getScene3D();
		mScene3DWithToolsPane = scene3DWithToolsPane;
		mPrimaryStage = primaryStage;
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Resource File");
		fileChooser.getExtensionFilters().add(new ExtensionFilter("SD Files", "*.sdf"));
		//pane.setPinnedSide(Side.RIGHT);
		
		Button loadMol = new Button("Load Molecule");
		
		loadMol.setOnAction(event -> {
			
			File selectedFile = fileChooser.showOpenDialog(mPrimaryStage);
			 if (selectedFile != null) {
				    MoleculeFileReader mfr = new MoleculeFileReader();
				    V3DMolecule[] mols = mfr.readMolFile(selectedFile.toString());
				    for(V3DMolecule vm: mols) {
						vm.activateEvents();
						mScene3D.addMolecule(vm);
				    }
				 }

		});
		
		TitledPane readWritePane = new TitledPane();
		readWritePane.setStyle("-fx-font-size: 1.5em ;");
		readWritePane.setText("Read/Write");
		VBox readWriteBox = new VBox();
		readWriteBox.setStyle("-fx-font-size: 1.5em ;");
		readWriteBox.getChildren().add(loadMol);
		readWritePane.setContent(readWriteBox);
		
		
		
		Button minimizeMolecule = new Button("Minimization");
		
		minimizeMolecule.setOnAction(event -> {
			mScene3DWithToolsPane.minimizeVisibleMols();
		});
		
		
		TitledPane forcefieldPane = new TitledPane();
		forcefieldPane.setText("ForceField");
		forcefieldPane.setStyle("-fx-font-size: 1.5em ;");
		VBox ffBox = new VBox();
		ffBox.getChildren().add(minimizeMolecule);
		forcefieldPane.setContent(ffBox);

	
		this.getPanes().addAll(readWritePane,forcefieldPane);


		
	}

	public V3DScene getV3DScene() {
		return mScene3D;
	}






}

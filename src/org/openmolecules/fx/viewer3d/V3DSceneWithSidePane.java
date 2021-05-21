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

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.alignment3d.transformation.Rotation;
import com.actelion.research.chem.alignment3d.transformation.TransformationSequence;
import com.actelion.research.chem.alignment3d.transformation.Translation;
import com.actelion.research.chem.docking.shape.ShapeDocking;
import com.actelion.research.chem.io.DWARFileParser;
import com.actelion.research.chem.phesa.DescriptorHandlerShape;
import com.actelion.research.chem.phesa.MolecularVolume;
import com.actelion.research.chem.phesa.ShapeVolume;
import com.actelion.research.chem.phesa.VolumeGaussian;
import com.actelion.research.jfx.gui.chem.MoleculeView;
import com.actelion.research.jfx.gui.chem.MoleculeViewSkin;

import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Callback;
import org.openmolecules.fx.tasks.V3DDockingEngine;
import org.openmolecules.fx.tasks.V3DShapeAlignerInPlace;
import org.openmolecules.fx.tasks.V3DShapeAlignment;
import org.openmolecules.fx.viewer3d.V3DMolecule.MoleculeRole;
import org.openmolecules.fx.viewer3d.io.V3DMoleculeParser;
import org.openmolecules.fx.viewer3d.io.V3DMoleculeWriter;
import org.openmolecules.fx.viewer3d.panel.DraggableHBox;
import org.openmolecules.fx.viewer3d.panel.EditorPane;
import org.openmolecules.fx.viewer3d.panel.MolGroupPane;
import org.openmolecules.render.TorsionStrainVisualization;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by thomas on 25.09.16.
 */
public class V3DSceneWithSidePane extends BorderPane {
	
	public static int counter1=0;
	public static int counter2=0;
	
	public enum GlobalMode { EXPLORER,BINDING_SITE_ANALYSIS, PHESA, ALIGNMENT;}
	public static final double TOOL_BUTTON_SIZE = 30.0;
	public static final double SIDEPANEL_WIDTH = 400.0;
	public static final double LOG_WIDTH = 400.0;
	public static Alert ONLY_ONE_PHESA_ALERT  = new Alert(AlertType.ERROR);
	static {
		ONLY_ONE_PHESA_ALERT.setTitle("Error");
		ONLY_ONE_PHESA_ALERT.setHeaderText("Improper Selection");
		ONLY_ONE_PHESA_ALERT.setContentText("Please select a single PheSA model");
	}
	public static Alert ONLY_ONE_MOLECULE_ALERT  = new Alert(AlertType.ERROR);
	static {
		ONLY_ONE_MOLECULE_ALERT.setTitle("Error");
		ONLY_ONE_MOLECULE_ALERT.setHeaderText("Improper Selection");
		ONLY_ONE_MOLECULE_ALERT.setContentText("Please select a single reference molecule");
	}
	public static Alert NO_PHESAMODEL_SELECTED  = new Alert(AlertType.ERROR);
	static {
		NO_PHESAMODEL_SELECTED.setTitle("Error");
		NO_PHESAMODEL_SELECTED.setHeaderText("Improper Selection");
		NO_PHESAMODEL_SELECTED.setContentText("Please select at least one PheSA model");
	}
	protected V3DScene mScene3D;
	protected MolGroupPane mMoleculePanel;
	protected GridPane upperPanel;
	protected MoleculeView molView;
	protected EditorPane editorPane;
	protected SimpleObjectProperty<GlobalMode> globalModeProperty;
	protected TextArea outputLog;
	protected DraggableHBox slidingBox;
	protected ToggleGroup rightPaneToggleGroup;
	
	public V3DSceneWithSidePane(EnumSet<V3DScene.ViewerSettings> settings) {
		this(1024, 768, settings);
	}

	public V3DSceneWithSidePane(int width, int height, EnumSet<V3DScene.ViewerSettings> settings) {
		rightPaneToggleGroup = new ToggleGroup();
		globalModeProperty = new SimpleObjectProperty<GlobalMode>(GlobalMode.EXPLORER);
		globalModeProperty.addListener((v,nv,ov) -> {
			this.setTop(null);
			createUpperPanel();
		});
		mScene3D = new V3DScene(new Group(), width, height, settings);
		editorPane = new EditorPane(mScene3D);
		BorderPane center = new BorderPane();
		outputLog = new TextArea();
		outputLog.setEditable(false);
		outputLog.appendText("OUTPUT LOG");
		outputLog.appendText("\n");
		outputLog.appendText("------------");
		outputLog.appendText("\n");
		outputLog.setPrefWidth(LOG_WIDTH);
		outputLog.setMaxWidth(LOG_WIDTH);
		
	    StackPane stackPane = new StackPane();
	    V3DSceneWithSelection sceneWithSelection = new V3DSceneWithSelection(mScene3D);
	    stackPane.getChildren().add(sceneWithSelection);
	    stackPane.getChildren().add(center);
	    center.setPickOnBounds(false);
		if(settings.contains(V3DScene.ViewerSettings.SIDEPANEL)) {
			//SplitPane splitPane = new SplitPane();
			createSidePane(center,settings);
			//center.setCenter(splitPane);
			setCenter(stackPane);
		}
		else {
			setCenter(stackPane);
		}
		if(settings.contains(V3DScene.ViewerSettings.UPPERPANEL))
			createUpperPanel();

		
	}
	
	protected void createSidePane(BorderPane center,EnumSet<V3DScene.ViewerSettings> settings) {
	
		this.setStyle("-fx-background-color:black");
		mMoleculePanel = new MolGroupPane(mScene3D);
		mMoleculePanel.getStyleClass().add("side-panel");
		molView = new MoleculeView();
		molView.setBackgroundColor(Color.web(GUIColorPalette.BLUE1));
		molView.setStyle("-fx-opacity: 0.9;");
		((MoleculeViewSkin) molView.getSkin()).setBorderColor(null);
		molView.setPrefHeight(200);
		mMoleculePanel.getMolTable().setOnMousePressed((e) -> {
			try {
				StereoMolecule mol = mMoleculePanel.getMolTable().getSelectionModel().getSelectedItem().getValue().getMolecule2D();
				((MoleculeViewSkin) molView.getSkin()).setOverruleColors(Color.WHITE, molView.getBackgroundColor());
				molView.setMolecule(mol);
				mMoleculePanel.updateTableSelection();
			
			}
			catch(Exception exc) {};
		});
		
		BorderPane borderPane = new BorderPane();
		borderPane.setCenter(mMoleculePanel);
		borderPane.setBottom(molView);
		borderPane.setPrefWidth(SIDEPANEL_WIDTH);
		borderPane.setMaxWidth(SIDEPANEL_WIDTH);

		Pane dummyPane = new Pane();
		dummyPane.setVisible(false);
		dummyPane.setPickOnBounds(false);
		
		slidingBox = new DraggableHBox(borderPane);
		center.setLeft(slidingBox.getBox());
		center.setCenter(dummyPane);

		
	}
	
	protected void toggleShowOutputLog() {
		if(getRight()==outputLog)
			setRight(null);
		else 
			setRight(outputLog);

			
	}
	
	protected void createUpperPanel() {
		upperPanel = new GridPane();
		upperPanel.setStyle("-fx-background-color:" + GUIColorPalette.BLUE3);
		this.setTop(upperPanel);
		upperPanel.setMaxHeight(TOOL_BUTTON_SIZE);
		upperPanel.setPrefHeight(TOOL_BUTTON_SIZE);
		createModePanel();
		constructUpperPanel();
	}

	public V3DScene getScene3D() {
		return mScene3D;
	}

	public MolGroupPane getMoleculePanel() {
		return mMoleculePanel;
	}
	
	protected void createModePanel() {
		ToggleButton explorerModeButton = new ToggleButton();
		explorerModeButton.setTooltip(new Tooltip("Explorer Mode"));
		explorerModeButton.getStyleClass().add("explorer-icon");
		explorerModeButton.prefHeightProperty().bind(upperPanel.heightProperty());
		upperPanel.add(explorerModeButton, counter1++, counter2);
		
		ToggleButton bindingSiteAnalysisButton = new ToggleButton("  P-L  ");
		bindingSiteAnalysisButton.setTooltip(new Tooltip("Binding Site Mode"));
		bindingSiteAnalysisButton.prefHeightProperty().bind(upperPanel.heightProperty());
		upperPanel.add(bindingSiteAnalysisButton,counter1++, counter2);
		
		ToggleButton pheSAButton = new ToggleButton("PheSA");
		pheSAButton.setTooltip(new Tooltip("PheSA Wizard"));
		pheSAButton.prefHeightProperty().bind(upperPanel.heightProperty());
		upperPanel.add(pheSAButton, counter1++, counter2);
		
		ToggleButton alignmentButton = new ToggleButton("Alignment");
		alignmentButton.setTooltip(new Tooltip("Alignment Mode"));
		alignmentButton.prefHeightProperty().bind(upperPanel.heightProperty());
		upperPanel.add(alignmentButton, counter1++, counter2);
		
		ToggleGroup group = new ToggleGroup();
		explorerModeButton.setToggleGroup(group);
		bindingSiteAnalysisButton.setToggleGroup(group);
		pheSAButton.setToggleGroup(group);
		
		explorerModeButton.setSelected(globalModeProperty.get()==GlobalMode.EXPLORER);
		bindingSiteAnalysisButton.setSelected(globalModeProperty.get()==GlobalMode.BINDING_SITE_ANALYSIS);
		pheSAButton.setSelected(globalModeProperty.get()==GlobalMode.PHESA);
		alignmentButton.setSelected(globalModeProperty.get()==GlobalMode.ALIGNMENT);
		
		explorerModeButton.setOnMouseReleased(e -> globalModeProperty.set(GlobalMode.EXPLORER));
		bindingSiteAnalysisButton.setOnMouseReleased(e -> globalModeProperty.set(GlobalMode.BINDING_SITE_ANALYSIS));
		pheSAButton.setOnMouseReleased(e -> globalModeProperty.set(GlobalMode.PHESA));
		alignmentButton.setOnMouseReleased(e -> globalModeProperty.set(GlobalMode.ALIGNMENT));
		
		
	}
	
	protected void constructUpperPanel() {
		int i=counter1;
		int j=counter2;
		constructSeparator(i,j);
		i++;
		constructVisibleButton(i,j);
		i++;
		constructInvisibleButton(i,j);
		i++;
		constructLogButton(i,j);
		i++;
		if(globalModeProperty.get()==GlobalMode.EXPLORER) {
			constructFileOpenButton(i,j);
			i++;
			constructFileSaveButton(i,j);
			i++;
			constructDeleteButton(i,j);
			i++;
			constructClearButton(i,j);
			i++;
			constructEditorButton(i,j);
			i++;
			constructHydrogenButton(i,j);
			i++;
			constructMinimizationButton(i,j);
			i++;
			constructTorsionButton(i,j);
			i++;
		}

		if(globalModeProperty.get()==GlobalMode.BINDING_SITE_ANALYSIS) {
			boolean success = handleBindingSiteMode();
			if(!success) {
				globalModeProperty.set(GlobalMode.EXPLORER);
				return;
			}
			constructInteractionButton(i,j);
			i++;
			constructSettingsButton(i,j);
			i++;
			constructDockingButton(i,j);
			i++;
			constructNegRecButton(i,j);
			i++;
		}
		if(globalModeProperty.get()==GlobalMode.PHESA) {
			constructPheSAButtons(i,j);
		}
		if(globalModeProperty.get()==GlobalMode.ALIGNMENT) {
			constructAlignmentButton(i,j);
		}
			
		
	}
	
	protected void constructSeparator(int i, int j) {

		Separator sep = new Separator();
		sep.setOrientation(Orientation.VERTICAL);
		sep.setMaxHeight(TOOL_BUTTON_SIZE);
		upperPanel.add(sep, i, j);

	}
	
	protected void constructPheSAButtons(int i, int j) {
		Button addButton = new Button("");
		addButton.getStyleClass().add("add-icon");
		addButton.setTooltip(new Tooltip("Add Pharmacophore Model"));
		addButton.setMaxHeight(TOOL_BUTTON_SIZE);
		upperPanel.add(addButton, i, j);
		addButton.getStyleClass().add("toolBarButton");
		addButton.setOnMouseReleased((e) -> {
			List<V3DMolecule> fxmols = mMoleculePanel.getAllSelectedMolecules();
			boolean sizeViolation = false;
			for(V3DMolecule fxmol : fxmols) {
				if(fxmol.getMolecule().getAtoms()>DescriptorHandlerShape.SIZE_CUTOFF) {
					sizeViolation = true;
					break;
				}
			}
			if(sizeViolation)
				V3DShapeAlignerInPlace.MOL_SIZE_ALERT.showAndWait();
			else {
				fxmols.stream().forEach(fxm -> fxm.addPharmacophore());
			}
		});
		addButton.prefHeightProperty().bind(upperPanel.heightProperty());
		i++;
		
		
		Button saveButton = new Button("");
		saveButton.getStyleClass().add("save-icon");
		saveButton.setTooltip(new Tooltip("Save PheSA Query"));
		saveButton.setMaxHeight(TOOL_BUTTON_SIZE);
		upperPanel.add(saveButton, i, j);
		saveButton.getStyleClass().add("toolBarButton");
		saveButton.setOnMouseReleased((e) -> {
			getPheSASaveDialog();
		});
		saveButton.prefHeightProperty().bind(upperPanel.heightProperty());
		
		i++;
		Button fileOpenButton = new Button("");
		fileOpenButton.getStyleClass().add("load-icon");
		fileOpenButton.setTooltip(new Tooltip("Load PheSA Query"));
		fileOpenButton.setMaxHeight(TOOL_BUTTON_SIZE);
		upperPanel.add(fileOpenButton, i, j);
		fileOpenButton.getStyleClass().add("toolBarButton");
		fileOpenButton.setOnMouseReleased((e) -> {
				FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle("Load PheSA Query");
				fileChooser.getExtensionFilters().add(new ExtensionFilter("DWAR Files", "*.dwar"));
				File selectedFile = fileChooser.showOpenDialog(null);
				if (selectedFile != null) {
					List<V3DMolecule> fxMols = V3DMoleculeParser.readPheSAQuery(mMoleculePanel.getV3DScene(), selectedFile, 0);
					V3DMolGroup phesaInput = new V3DMolGroup("PheSA Queries");
				    for(V3DMolecule fxMol: fxMols) 
				    	phesaInput.addMolGroup(fxMol);
				    mMoleculePanel.getV3DScene().addMolGroup(phesaInput);
				
				}
		});
		
		fileOpenButton.prefHeightProperty().bind(upperPanel.heightProperty());
		
		i++;
		Button addInclButton = new Button("");
		addInclButton.setTooltip(new Tooltip("Add Inclusion Sphere"));
		addInclButton.getStyleClass().add("inclusion-icon");
		addInclButton.setMaxHeight(TOOL_BUTTON_SIZE);
		upperPanel.add(addInclButton, i, j);
		addInclButton.getStyleClass().add("toolBarButton");
		addInclButton.setOnMouseReleased((e) -> {
			handlePheSACustomVolume(VolumeGaussian.INCLUSION);
		});
		addInclButton.prefHeightProperty().bind(upperPanel.heightProperty());
		
		i++;
		Button addExclButton = new Button("");
		addExclButton.setTooltip(new Tooltip("Add Exclusion Sphere"));
		addExclButton.getStyleClass().add("exclusion-icon");
		addExclButton.setMaxHeight(TOOL_BUTTON_SIZE);
		upperPanel.add(addExclButton, i, j);
		addExclButton.getStyleClass().add("toolBarButton");
		addExclButton.setOnMouseReleased((e) -> {
			handlePheSACustomVolume(VolumeGaussian.EXCLUSION);
		});
		addExclButton.prefHeightProperty().bind(upperPanel.heightProperty());

	}
	
	protected void handlePheSACustomVolume(int function) {
		List<V3DMolGroup> selectedGroups = mMoleculePanel.getAllSelectedMolGroups();
		if(selectedGroups.size()!=1)
			ONLY_ONE_PHESA_ALERT.showAndWait();
		else if(!(selectedGroups.get(0) instanceof V3DCustomizablePheSA))
				ONLY_ONE_PHESA_ALERT.showAndWait();
		else 
			 ((V3DCustomizablePheSA) selectedGroups.get(0)).placeExclusionSphere(function);
	}
	
	protected void constructVisibleButton(int i, int j) {
		//Image eye = new Image(EditorPane.class.getResource("/resources/eye.png").toString(), TOOL_BUTTON_SIZE,
		//		TOOL_BUTTON_SIZE, true, true);
		//ImageView imgView = new ImageView(eye);
		Button setVisibleButton = new Button("");
		setVisibleButton.getStyleClass().add("eye-icon");
		setVisibleButton.setTooltip(new Tooltip("Make Selected Visible"));
		setVisibleButton.setMaxHeight(TOOL_BUTTON_SIZE);
		upperPanel.add(setVisibleButton, i, j);
		setVisibleButton.getStyleClass().add("toolBarButton");
		setVisibleButton.setOnMouseReleased((e) -> mMoleculePanel.changeVisibilitySelected(true));
		setVisibleButton.prefHeightProperty().bind(upperPanel.heightProperty());

	}
	
	protected void constructInvisibleButton(int i, int j) {
		//Image eyeCrossed = new Image(EditorPane.class.getResource("/resources/eye_crossed.png").toString(), TOOL_BUTTON_SIZE,
		//		TOOL_BUTTON_SIZE, true, true);
		//ImageView imgView = new ImageView(eyeCrossed);
		Button setInvisibleButton = new Button("");
		setInvisibleButton.setTooltip(new Tooltip("Make Selected Invisible"));
		setInvisibleButton.getStyleClass().add("eye-crossed-icon");
		setInvisibleButton.setMaxHeight(TOOL_BUTTON_SIZE);
		upperPanel.add(setInvisibleButton, i, j);
		setInvisibleButton.getStyleClass().add("toolBarButton");
		setInvisibleButton.setOnMouseReleased((e) -> mMoleculePanel.changeVisibilitySelected(false));
		setInvisibleButton.prefHeightProperty().bind(upperPanel.heightProperty());
		
	}
	
	protected void constructFileOpenButton(int i, int j) {
		//Image fileOpen = new Image(EditorPane.class.getResource("/resources/file_open.png").toString(), TOOL_BUTTON_SIZE,
		//		TOOL_BUTTON_SIZE, true, true);
		//ImageView imgView = new ImageView(fileOpen);
		Button fileOpenButton = new Button("");
		fileOpenButton.setTooltip(new Tooltip("Load Molecule(s) from File"));
		fileOpenButton.getStyleClass().add("load-icon");
		fileOpenButton.setMaxHeight(TOOL_BUTTON_SIZE);
		upperPanel.add(fileOpenButton, i, j);
		fileOpenButton.getStyleClass().add("toolBarButton");
		fileOpenButton.setOnMouseReleased((e) -> {
				File selectedFile = V3DPopupMenu.getMoleculeFileChooser().showOpenDialog(mScene3D.getScene().getWindow());
				if (selectedFile != null) {
				    V3DMoleculeParser.readMoleculeFile(mScene3D,selectedFile.toString());
				}
		});
		fileOpenButton.prefHeightProperty().bind(upperPanel.heightProperty());
	}
	
	protected void constructAlignmentButton(int i, int j) {
		//Image fileOpen = new Image(EditorPane.class.getResource("/resources/file_open.png").toString(), TOOL_BUTTON_SIZE,
		//		TOOL_BUTTON_SIZE, true, true);
		//ImageView imgView = new ImageView(fileOpen);
		Button alignmentButton = new Button("");
		alignmentButton.setTooltip(new Tooltip("Perform Alignment"));
		alignmentButton.getStyleClass().add("align-icon");
		alignmentButton.setMaxHeight(TOOL_BUTTON_SIZE);
		upperPanel.add(alignmentButton, i, j);
		alignmentButton.getStyleClass().add("toolBarButton");
		alignmentButton.setOnMouseReleased((e) -> {			List<V3DMolecule> selectedMols = mMoleculePanel.getAllSelectedMolecules();
		    if(selectedMols.size()!=1) {
		    	ONLY_ONE_MOLECULE_ALERT.showAndWait();
		    	return;
		    }
			File selectedFile = V3DPopupMenu.getMoleculeFileChooser().showOpenDialog(mScene3D.getScene().getWindow());
			if (selectedFile != null) {
			    List<StereoMolecule> fitMols = V3DMoleculeParser.parseChemFile(selectedFile.getAbsolutePath());
		    	V3DMolecule refmol = selectedMols.get(0);
		    	MolecularVolume refMolVol = new MolecularVolume(refmol.getMolecule());
		    	V3DShapeAlignment alignment = new V3DShapeAlignment(mScene3D,refmol, refMolVol, fitMols)	;
		    	alignment.align(false);
			    	
			    
			}
		});
		alignmentButton.prefHeightProperty().bind(upperPanel.heightProperty());
	}
	
	
	protected void constructFileSaveButton(int i, int j) {
		//Image save= new Image(EditorPane.class.getResource("/resources/save.png").toString(), TOOL_BUTTON_SIZE,
		//		TOOL_BUTTON_SIZE, true, true);
		//ImageView imgView = new ImageView(save);
		Button saveButton = new Button("");
		saveButton.setTooltip(new Tooltip("Save Molecules to File"));
		saveButton.getStyleClass().add("save-icon");
		saveButton.setMaxHeight(TOOL_BUTTON_SIZE);
		upperPanel.add(saveButton, i, j);
		saveButton.getStyleClass().add("toolBarButton");
		saveButton.setOnMouseReleased((e) -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Save Molecule File");
			fileChooser.getExtensionFilters().add(new ExtensionFilter("SD Files", "*.sdf"));
			fileChooser.getExtensionFilters().add(new ExtensionFilter("DWAR Files", "*.dwar"));
	        File file = fileChooser.showSaveDialog(null);
	        V3DMoleculeWriter.saveMolList(file.getPath(), mMoleculePanel.getAllSelectedMolecules());
		});
		saveButton.prefHeightProperty().bind(upperPanel.heightProperty());
		
	}
	
	protected void constructDeleteButton(int i, int j) {
		//Image trash= new Image(EditorPane.class.getResource("/resources/trash.png").toString(), TOOL_BUTTON_SIZE,
		//		TOOL_BUTTON_SIZE, true, true);
		//ImageView imgView = new ImageView(trash);
		Button deleteButton = new Button("");
		deleteButton.setTooltip(new Tooltip("Delete Selected Models"));
		deleteButton.getStyleClass().add("garbage-icon");
		deleteButton.setMaxHeight(TOOL_BUTTON_SIZE);
		upperPanel.add( deleteButton, i, j);
		deleteButton.getStyleClass().add("toolBarButton");
		deleteButton.setOnMouseReleased((e) -> mScene3D.delete(mMoleculePanel.getAllSelectedMolGroups()));
		deleteButton.prefHeightProperty().bind(upperPanel.heightProperty());
	}
	
	protected void constructClearButton(int i, int j) {
		//Image trash= new Image(EditorPane.class.getResource("/resources/trash.png").toString(), TOOL_BUTTON_SIZE,
		//		TOOL_BUTTON_SIZE, true, true);
		//ImageView imgView = new ImageView(trash);
		Button deleteButton = new Button("");
		deleteButton.setTooltip(new Tooltip("Clear"));
		deleteButton.getStyleClass().add("clear-icon");
		deleteButton.setMaxHeight(TOOL_BUTTON_SIZE);
		upperPanel.add( deleteButton, i, j);
		deleteButton.getStyleClass().add("toolBarButton");
		deleteButton.setOnMouseReleased((e) -> mScene3D.clearAll(false));
		deleteButton.prefHeightProperty().bind(upperPanel.heightProperty());
	}
	
	protected void constructEditorButton(int i, int j) {
		ToggleButton toolButton = new ToggleButton("");
		toolButton.setTooltip(new Tooltip("Show Molecular Editor Panel"));
		toolButton.setToggleGroup(rightPaneToggleGroup);
		toolButton.getStyleClass().add("tool-icon");
		toolButton.setMaxHeight(TOOL_BUTTON_SIZE);
		upperPanel.add(toolButton, i, j);
		toolButton.getStyleClass().add("toolBarButton");
		toolButton.setOnMouseReleased((e) -> {
			if(getRight()==editorPane) {
				setRight(null);
			}
			else {
				setRight(editorPane);
			}
		});
		toolButton.prefHeightProperty().bind(upperPanel.heightProperty());
	}
	
	protected void constructHydrogenButton(int i, int j) {
		Button addHydrogenButton = new Button("H");
		addHydrogenButton.setTooltip(new Tooltip("Add Hydrogens"));
		addHydrogenButton.setMaxHeight(TOOL_BUTTON_SIZE);
		upperPanel.add(addHydrogenButton, i, j);
		addHydrogenButton.getStyleClass().add("toolBarButton");
		addHydrogenButton.setOnMouseReleased((e) -> {
			mMoleculePanel.getAllSelectedMolecules().stream().forEach(v3dm -> v3dm.addImplicitHydrogens());
		});

		addHydrogenButton.prefHeightProperty().bind(upperPanel.heightProperty());
		
	}
	
	protected void constructInteractionButton(int i, int j) {
		ToggleButton addInteractionsButton = new ToggleButton("D--A");
		addInteractionsButton.setTooltip(new Tooltip("Show Interactions"));
		addInteractionsButton.setMaxHeight(TOOL_BUTTON_SIZE);
		upperPanel.add(addInteractionsButton, i, j);
		//addInteractionsButton.getStyleClass().add("toolBarButton");
		addInteractionsButton.setOnMouseReleased((e) -> {
			if(mScene3D.getInteractionHandler()==null)
				mScene3D.handleInteractions();
			else 
				mScene3D.getInteractionHandler().toggleVisibility();
		});

		addInteractionsButton.prefHeightProperty().bind(upperPanel.heightProperty());
		
	}
	
	protected void constructNegRecButton(int i, int j) {
		Button negRecButton = new Button("");
		negRecButton.setTooltip(new Tooltip("Construct Negative Receptor Image"));
		negRecButton.getStyleClass().add("neg-rec-img-icon");
		negRecButton.getStyleClass().add("toolBarButton");
		upperPanel.add(negRecButton, i, j);
		
		negRecButton.setOnMouseReleased((e) -> {
			mScene3D.getBindingSiteHelper().addNegRecImg();
		});
	
		negRecButton.prefHeightProperty().bind(upperPanel.heightProperty());
		
	}
	
	protected void constructLogButton(int i, int j) {
		ToggleButton logButton = new ToggleButton("");
		logButton.setTooltip(new Tooltip("Show Output Log"));
		logButton.setToggleGroup(rightPaneToggleGroup);
		logButton.getStyleClass().add("log-icon");
		logButton.getStyleClass().add("toolBarButton");
		upperPanel.add(logButton, i, j);
		
		logButton.setOnMouseReleased((e) -> {
			toggleShowOutputLog();
		});
	
		logButton.prefHeightProperty().bind(upperPanel.heightProperty());
		
	}
	
	protected void constructTorsionButton(int i, int j) {
		Button addTorsionVisButton = new Button("");
		addTorsionVisButton.setTooltip(new Tooltip("Show Torsion Strain"));
		addTorsionVisButton.getStyleClass().add("torsion-icon");
		addTorsionVisButton.setMaxHeight(TOOL_BUTTON_SIZE);
		upperPanel.add(addTorsionVisButton, i, j);
		addTorsionVisButton.getStyleClass().add("toolBarButton");
		addTorsionVisButton.setOnMouseReleased((e) -> {
			List<V3DMolecule> mols =  mMoleculePanel.getAllSelectedMolecules();
			mols.forEach(m -> {
				TorsionStrainVisualization torsionStrainVis = m.getTorsionStrainVis();
				if(torsionStrainVis==null)
					m.addTorsionStrainVisualization();
				else 
					torsionStrainVis.toggleVisibility();
			
			});
		});

		addTorsionVisButton.prefHeightProperty().bind(upperPanel.heightProperty());
		
	}
	
	protected void constructDockingButton(int i, int j) {
		//Image optim = new Image(EditorPane.class.getResource("/resources/optimization.png").toString(), TOOL_BUTTON_SIZE,
		//		TOOL_BUTTON_SIZE, true, true);
		//ImageView imgView = new ImageView(optim);
		MenuButton dockingButton = new MenuButton("");
		dockingButton.setTooltip(new Tooltip("Docking"));
		dockingButton.getStyleClass().add("docking-icon");
		dockingButton.setMaxHeight(TOOL_BUTTON_SIZE);
		upperPanel.add(dockingButton, i, j);
		dockingButton.getStyleClass().add("dropdownButton");
		MenuItem reDockItem = new MenuItem("Re-Docking");
		reDockItem.setOnAction(e -> {
			V3DDockingEngine dEngine = new V3DDockingEngine(mScene3D,mScene3D.getBindingSiteHelper());
			ExecutorService executor = Executors.newSingleThreadExecutor();
			executor.execute(() -> dEngine.reDock());
		});
		dockingButton.getItems().add(reDockItem);
		dockingButton.prefHeightProperty().bind(upperPanel.heightProperty());
		
		MenuItem libDockItem = new MenuItem("Dock Library");
		libDockItem.setOnAction(e -> {
			V3DDockingEngine dEngine = new V3DDockingEngine(mScene3D,mScene3D.getBindingSiteHelper());
			ExecutorService executor = Executors.newSingleThreadExecutor();
			File selectedFile = V3DPopupMenu.getMoleculeFileChooser().showOpenDialog(mScene3D.getScene().getWindow());
			if (selectedFile != null) {
				List<StereoMolecule> lib = V3DMoleculeParser.parseChemFile(selectedFile.getAbsolutePath());
				executor.execute(() -> dEngine.dockLibrary(lib));	
			}
		});
		dockingButton.getItems().add(libDockItem);
		
		MenuItem shapeDockItem = new MenuItem("Shape Docking");
		shapeDockItem.setOnAction(e -> {
			V3DBindingSiteVolume siteVol = null;
			List<V3DMolGroup> selectedGroups = mMoleculePanel.getAllSelectedMolGroups();
			if(selectedGroups.size()!=1)
				return;
			if(selectedGroups.get(0) instanceof V3DBindingSiteVolume)
				siteVol = (V3DBindingSiteVolume)selectedGroups.get(0);
			if(siteVol!=null) {
				ShapeVolume shapeVol = new ShapeVolume(siteVol.getShapeVolume());
				Coordinates origCOM = shapeVol.getCOM();
				Rotation rot = shapeVol.preProcess(null);
				TransformationSequence transformation = new TransformationSequence();
				transformation.addTransformation(rot.getInvert());
				transformation.addTransformation(new Translation(new double[] {origCOM.x,origCOM.y,origCOM.z}));
				ShapeDocking shapeDock = new ShapeDocking(shapeVol,transformation);
				ExecutorService executor = Executors.newSingleThreadExecutor();
				File selectedFile = V3DPopupMenu.getMoleculeFileChooser().showOpenDialog(mScene3D.getScene().getWindow());
				if (selectedFile != null) {
					List<StereoMolecule> lib = V3DMoleculeParser.parseChemFile(selectedFile.getAbsolutePath());
					executor.execute(() -> {
						for(StereoMolecule toDock : lib) {
							//StereoMolecule aligned = shapeDock.dock(toDock);
					}
				});
				
			}
			}});
				/*
				if(molGroup instanceof V3DCustomizablePheSA)
					phesaModels.add((V3DCustomizablePheSA) molGroup);
			}
			V3DDockingEngine dEngine = new V3DDockingEngine(mScene3D,mScene3D.getBindingSiteHelper());
			ExecutorService executor = Executors.newSingleThreadExecutor();
			File selectedFile = V3DPopupMenu.getMoleculeFileChooser().showOpenDialog(mScene3D.getScene().getWindow());
			if (selectedFile != null) {
				List<StereoMolecule> lib = V3DMoleculeParser.parseChemFile(selectedFile.getAbsolutePath());
				executor.execute(() -> dEngine.dockLibrary(lib));	
			}
			*/

		dockingButton.getItems().add(shapeDockItem);
		dockingButton.prefHeightProperty().bind(upperPanel.heightProperty());
	}
	
	
	protected void constructMinimizationButton(int i, int j) {
		//Image optim = new Image(EditorPane.class.getResource("/resources/optimization.png").toString(), TOOL_BUTTON_SIZE,
		//		TOOL_BUTTON_SIZE, true, true);
		//ImageView imgView = new ImageView(optim);
		Button optiButton = new Button("");
		optiButton.getStyleClass().add("optim-icon");
		optiButton.setTooltip(new Tooltip("Minimize"));
		optiButton.setMaxHeight(TOOL_BUTTON_SIZE);
		upperPanel.add(optiButton, i, j);
		optiButton.getStyleClass().add("toolBarButton");
		optiButton.setOnMouseReleased((e) -> {
			if(globalModeProperty.get()==GlobalMode.BINDING_SITE_ANALYSIS) {
				V3DDockingEngine dEngine = new V3DDockingEngine(mScene3D,mScene3D.getBindingSiteHelper());
				ExecutorService executor = Executors.newSingleThreadExecutor();
				executor.execute(() -> dEngine.refineNativePose());
			}
			else
				V3DPopupMenu.showMinimizerDialog(mScene3D,null,null);
		});

		optiButton.prefHeightProperty().bind(upperPanel.heightProperty());
	}
	
	protected void constructSettingsButton(int i, int j) {
		MenuButton settingsMenu = new MenuButton("");
		settingsMenu.setTooltip(new Tooltip("Settings"));
		final String setting1 = "Protein Wires - Ligand Ball and Sticks";
		final String setting2 = "Protein Wires - Ligand Sticks";
		final String setting3 = "Protein Sticks - Ligand Ball and Sticks";
		final String setting4 = "Protein Sticks and Surface - Ligand Ball and Sticks";
		ToggleGroup toggleGroup = new ToggleGroup();
		RadioMenuItem menu1 = new RadioMenuItem(setting1);
		RadioMenuItem menu2 = new RadioMenuItem(setting2);
		RadioMenuItem menu3 = new RadioMenuItem(setting3);
		RadioMenuItem menu4 = new RadioMenuItem(setting4);
		menu1.setToggleGroup(toggleGroup);
		menu2.setToggleGroup(toggleGroup);
		menu3.setToggleGroup(toggleGroup);
		menu4.setToggleGroup(toggleGroup);
		menu1.setOnAction(e -> {
			if(mScene3D.getBindingSiteHelper()!=null) {
				mScene3D.getBindingSiteHelper().setDisplayMode(V3DBindingSite.DisplayMode.MODE1);
			}
		});
		menu2.setOnAction(e -> {
			if(mScene3D.getBindingSiteHelper()!=null) {
				mScene3D.getBindingSiteHelper().setDisplayMode(V3DBindingSite.DisplayMode.MODE2);
			}
		});
		menu3.setOnAction(e -> {
			if(mScene3D.getBindingSiteHelper()!=null) {
				mScene3D.getBindingSiteHelper().setDisplayMode(V3DBindingSite.DisplayMode.MODE3);
			}
		});
		menu4.setOnAction(e -> {
			if(mScene3D.getBindingSiteHelper()!=null) {
				mScene3D.getBindingSiteHelper().setDisplayMode(V3DBindingSite.DisplayMode.MODE4);
			}
		});

		settingsMenu.getItems().add(menu1);
		settingsMenu.getItems().add(menu2);
		settingsMenu.getItems().add(menu3);
		settingsMenu.getItems().add(menu4);
		menu1.setSelected(true);
		settingsMenu.getStyleClass().add("settings-icon");
		settingsMenu.setMaxHeight(TOOL_BUTTON_SIZE);
		upperPanel.add(settingsMenu, i, j);

		settingsMenu.prefHeightProperty().bind(upperPanel.heightProperty());
	}
	
	protected void getPheSASaveDialog() {
		Dialog<Boolean> dialog = new Dialog<>();
		dialog.setTitle("PheSA Query Specifications");
		dialog.setResizable(false);
		
		HBox hbox1 = new HBox();
		
		Label label = new Label("Save as: ");
		
		ToggleGroup toggleGroupInput = new ToggleGroup();
		RadioButton singleConf = new RadioButton("Single Conformer");
		singleConf.setSelected(true);
		RadioButton ensembleConf = new RadioButton("Conformer Ensemble");
		ensembleConf.setToggleGroup(toggleGroupInput);
		singleConf.setToggleGroup(toggleGroupInput);
		hbox1.getChildren().addAll(label, singleConf, ensembleConf);
		List<V3DCustomizablePheSA> phesaModels = new ArrayList<>();
		List<V3DMolGroup> selectedGroups = mMoleculePanel.getAllSelectedMolGroups();
		for(V3DMolGroup molGroup : selectedGroups) {
			if(molGroup instanceof V3DCustomizablePheSA)
				phesaModels.add((V3DCustomizablePheSA) molGroup);
		}
		
	
		dialog.getDialogPane().setContent(hbox1);
		
		ButtonType buttonTypeSave = new ButtonType("Save", ButtonData.OK_DONE);

		dialog.getDialogPane().getButtonTypes().add(buttonTypeSave);
		dialog.setResultConverter(new Callback<ButtonType, Boolean>() {
		    @Override
		    public Boolean call(ButtonType b) {
		    	FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle("Save PheSA Queries");
				fileChooser.getExtensionFilters().add(new ExtensionFilter("DWAR Files", "*.dwar"));
				File file = fileChooser.showSaveDialog(null);
				boolean generateConfs = ensembleConf.isSelected() ? true : false;
		        V3DMoleculeWriter.savePhesaQueries(file, phesaModels,generateConfs);
		        return true;
		        
		    }
		});
		
		dialog.showAndWait();
	}
	
	
	protected boolean handleBindingSiteMode() {
		boolean success=false;
		List<V3DMolecule> selectedMols = mMoleculePanel.getAllSelectedMolecules();
		V3DMolecule theProtein = null;
		V3DMolecule theLigand = null;
		if(selectedMols.size()==2) {
			V3DMolecule mol1 = selectedMols.get(0);
			V3DMolecule mol2 = selectedMols.get(1);
			if(mol1.getRole()==MoleculeRole.LIGAND) {
				theLigand=mol1; 
				if(mol2.getRole()==MoleculeRole.MACROMOLECULE)
					theProtein=mol2;
			}
			else if(mol1.getRole()==MoleculeRole.MACROMOLECULE) {
				theProtein=mol1; 
				if(mol2.getRole()==MoleculeRole.LIGAND)
					theLigand=mol2;
			}
		}
		
		if(theProtein!=null && theLigand!=null) {
			success=true;
			V3DBindingSite bdsHelper = new V3DBindingSite();
			bdsHelper.setNativeLigand(theLigand);
			bdsHelper.setReceptor(theProtein);
			bdsHelper.initialize();
			//mScene3D.crop(theLigand, 12.0);
			theProtein.reconstruct();
			mScene3D.setBindingSiteHelper(bdsHelper);
			mScene3D.optimizeView(mScene3D.getBindingSiteHelper().getNativeLigand());
			mScene3D.getCamera().setTranslateZ(-25);
			List<V3DMolecule> toDelete = new ArrayList<>();
			for(V3DMolecule v3dMol : mMoleculePanel.getAllMolecules()) {
				if(!selectedMols.contains(v3dMol))
					toDelete.add(v3dMol);
			}
			mScene3D.delete(toDelete);
		}
			
		
		else {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Error Dialog");
			alert.setHeaderText("Cannot enter Binding Site Analysis Mode");
			alert.setContentText("Please have exactly one receptor structure and one ligand molecule in the selection");
	
			alert.showAndWait();
		}
		
		return success;
	}
	
	
}
 
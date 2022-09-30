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

package org.openmolecules.fx.sunflow;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.StringConverter;
import org.openmolecules.mesh.MoleculeSurfaceAlgorithm;
import org.openmolecules.render.MoleculeArchitect;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.render.SunflowMoleculeBuilder;

public class RayTraceDialog extends Dialog<RayTraceOptions> implements EventHandler<ActionEvent> {
	private static final Color DEFAULT_BACKGROUND = Color.rgb(SunflowMoleculeBuilder.DEFAULT_BACKGROUND.getRed(),
															  SunflowMoleculeBuilder.DEFAULT_BACKGROUND.getGreen(),
															  SunflowMoleculeBuilder.DEFAULT_BACKGROUND.getBlue());
	private static final Color DEFAULT_FLOOR_COLOR = Color.rgb(SunflowMoleculeBuilder.DEFAULT_FLOOR_COLOR.getRed(),
															   SunflowMoleculeBuilder.DEFAULT_FLOOR_COLOR.getGreen(),
															   SunflowMoleculeBuilder.DEFAULT_FLOOR_COLOR.getBlue());

	private static final String[] SIZE_OPTIONS = {
			"160 x 120",
			"192 x 108",
			"640 x 480",
			"1024 x 768",
			"1600 x 1200",
			"1920 x 1080",
			"2560 x 1600",
			"3840 x 2160" };

	private ComboBox    mComboboxSize,mComboboxMode,mComboboxAtomMaterial,mComboboxBondMaterial;
	private ComboBox[]  mComboboxSurfaceMaterial;
	private CheckBox    mCheckboxOptimizeTranslation,mCheckboxUseBackground,mCheckboxUseFloor,mCheckboxOptimizeRotation,
						mCheckboxShinyFloor,mCheckboxDepthBlurring;
	private ColorPicker mBackgroundColorPicker,mFloorColorPicker;
	private Slider		mSliderBrightness;

	/**
	 * @param parent
	 */
	public RayTraceDialog(Window parent, RayTraceOptions options, V3DMolecule mol) {
		super();

		initOwner(parent);
		initStyle(StageStyle.UNDECORATED);
		initModality(Modality.WINDOW_MODAL);
		setTitle("Create Photo-Realistic Image");
		setHeaderText("Define details to render molecule!");
//		setGraphic(new ImageView(this.getClass().getResource("something.png").toString()));

		ButtonType renderButtonType = new ButtonType("Render", ButtonBar.ButtonData.OK_DONE);
		getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, renderButtonType);

		GridPane grid = new GridPane();
		grid.setHgap(8);
		grid.setVgap(8);
		grid.setPadding(new javafx.geometry.Insets(8, 8, 8, 8));

		int yIndex = 0;

		grid.add(new Label("Image size:"), 0, yIndex);
		mComboboxSize = new ComboBox();
		for (String item:SIZE_OPTIONS)
			mComboboxSize.getItems().add(item);
		mComboboxSize.getSelectionModel().select(options != null ? options.size : SIZE_OPTIONS[0]);
		grid.add(mComboboxSize, 1, yIndex);

		if (mol != null) {
			grid.add(new Label("Render mode:"), 0, ++yIndex);
			mComboboxMode = new ComboBox();
			for (String item : MoleculeArchitect.MODE_TEXT)
				mComboboxMode.getItems().add(item);
			mComboboxMode.getSelectionModel().select(options != null ? options.mode : mol.getConstructionMode());
			grid.add(mComboboxMode, 1, yIndex);
		}

		grid.add(new Label("Atom material:"), 0, ++yIndex);
		mComboboxAtomMaterial = new ComboBox();
		for (String item: SunflowMoleculeBuilder.MATERIAL_TEXT)
			mComboboxAtomMaterial.getItems().add(item);
		mComboboxAtomMaterial.getSelectionModel().select(options != null ? options.atomMaterial : SunflowMoleculeBuilder.DEFAULT_ATOM_MATERIAL);
		grid.add(mComboboxAtomMaterial, 1, yIndex);

		grid.add(new Label("Bond material:"), 0, ++yIndex);
		mComboboxBondMaterial = new ComboBox();
		for (String item: SunflowMoleculeBuilder.MATERIAL_TEXT)
			mComboboxBondMaterial.getItems().add(item);
		mComboboxBondMaterial.getSelectionModel().select(options != null ? options.bondMaterial : SunflowMoleculeBuilder.DEFAULT_BOND_MATERIAL);
		grid.add(mComboboxBondMaterial, 1, yIndex);

		mComboboxSurfaceMaterial = new ComboBox[MoleculeSurfaceAlgorithm.SURFACE_TYPE.length];
		for (int type=0; type<mComboboxSurfaceMaterial.length; type++) {
			if (mol != null && mol.getSurfaceMode(type) != V3DMolecule.SurfaceMode.NONE) {
				grid.add(new Label(MoleculeSurfaceAlgorithm.SURFACE_TYPE[type]+" surface material:"), 0, ++yIndex);
				mComboboxSurfaceMaterial[type] = new ComboBox();
				for (String item : SunflowMoleculeBuilder.SURFACE_TEXT)
					mComboboxSurfaceMaterial[type].getItems().add(item);
				mComboboxSurfaceMaterial[type].getSelectionModel().select(
						  options != null && options.surfaceMaterial[type] != -1 ? options.surfaceMaterial[type]
						: mol.getSurfaceMode(type) == V3DMolecule.SurfaceMode.WIRES ? SunflowMoleculeBuilder.SURFACE_WIRES
						: mol.getSurfaceTransparency(type) > 0.2  ?
								SunflowMoleculeBuilder.SURFACE_TRANSPARENT : SunflowMoleculeBuilder.SURFACE_SHINY);
				grid.add(mComboboxSurfaceMaterial[type], 1, yIndex);
			}
		}

		mCheckboxUseBackground = new CheckBox("Opaque background");
		mCheckboxUseBackground.setSelected(options != null ? options.backgroundColor != null : SunflowMoleculeBuilder.DEFAULT_USE_BACKGROUND);
		mCheckboxUseBackground.addEventHandler(ActionEvent.ACTION, this);
		grid.add(mCheckboxUseBackground, 0, ++yIndex);

		mBackgroundColorPicker = new ColorPicker(options != null && options.backgroundColor != null ? options.backgroundColor : DEFAULT_BACKGROUND);
		grid.add(mBackgroundColorPicker, 1, yIndex);

		mCheckboxUseFloor = new CheckBox("Floor with shadows");
		mCheckboxUseFloor.setSelected(options != null ? options.floorColor != null : SunflowMoleculeBuilder.DEFAULT_USE_FLOOR);
		mCheckboxUseFloor.addEventHandler(ActionEvent.ACTION, this);
		grid.add(mCheckboxUseFloor, 0, ++yIndex);

		mFloorColorPicker = new ColorPicker(options != null && options.floorColor != null ? options.floorColor : DEFAULT_FLOOR_COLOR);
		grid.add(mFloorColorPicker, 1, yIndex);

		mCheckboxShinyFloor = new CheckBox("Glossy floor");
		mCheckboxShinyFloor.setSelected(options != null ? options.shinyFloor : SunflowMoleculeBuilder.DEFAULT_GLOSSY_FLOOR);
		mCheckboxShinyFloor.setAlignment(Pos.CENTER);
		grid.setColumnSpan(mCheckboxShinyFloor, 2);
		grid.add(mCheckboxShinyFloor, 1, ++yIndex);

		mCheckboxDepthBlurring = new CheckBox("Apply depth blurring");
		mCheckboxDepthBlurring.setSelected(options != null ? options.depthBlurring : SunflowMoleculeBuilder.DEFAULT_DEPTH_BLURRING);
		mCheckboxDepthBlurring.setAlignment(Pos.CENTER);
		grid.setColumnSpan(mCheckboxDepthBlurring, 2);
		grid.add(mCheckboxDepthBlurring, 0, ++yIndex);

		if (mol != null) {
			mCheckboxOptimizeTranslation = new CheckBox("Move and zoom to fill image");
			mCheckboxOptimizeTranslation.setSelected(options != null ? options.optimizeTranslation : true);
			grid.setColumnSpan(mCheckboxOptimizeTranslation, 2);
			grid.add(mCheckboxOptimizeTranslation, 0, ++yIndex);

			mCheckboxOptimizeRotation = new CheckBox("Rotate for best view");
			mCheckboxOptimizeRotation.setSelected(options != null ? options.optimizeRotation : true);
			grid.setColumnSpan(mCheckboxOptimizeRotation, 2);
			grid.add(mCheckboxOptimizeRotation, 0, ++yIndex);
			}

		mSliderBrightness = new Slider(0.0, 2.0, options != null ? options.brightness : 1.0);
		//mSliderBrightness.setPrefWidth(200);
		mSliderBrightness.setShowTickMarks(true);
		mSliderBrightness.setShowTickLabels(true);
		StringConverter<Double> labelCreater = new StringConverter<Double>() {
			@Override public String toString(Double d) {
				return d.doubleValue() == 0.0 ? "darker" : "brighter";
			}

			@Override public Double fromString(String string) {
				return null;
			}
		};
		mSliderBrightness.setLabelFormatter(labelCreater);

		HBox sliderPane = new HBox();
		sliderPane.setPadding(new Insets(8, 4, 4, 4));
		sliderPane.setSpacing(16);
		sliderPane.getChildren().addAll(new Label("Relative brightness:"), mSliderBrightness);
		sliderPane.setAlignment(Pos.CENTER);
		grid.setColumnSpan(sliderPane, 2);
		grid.add(sliderPane, 0, ++yIndex);

		getDialogPane().setContent(grid);

		disableItems();

		setResultConverter(dialogButton -> {
			if (dialogButton == renderButtonType) {
				RayTraceOptions outOptions = new RayTraceOptions();
				outOptions.size = (String)mComboboxSize.getSelectionModel().getSelectedItem();
				outOptions.mode = (mComboboxMode == null) ? -1 : mComboboxMode.getSelectionModel().getSelectedIndex();
				outOptions.atomMaterial = mComboboxAtomMaterial.getSelectionModel().getSelectedIndex();
				outOptions.bondMaterial = mComboboxBondMaterial.getSelectionModel().getSelectedIndex();
				outOptions.surfaceMaterial = new int[MoleculeSurfaceAlgorithm.SURFACE_TYPE.length];
				for (int i = 0; i<MoleculeSurfaceAlgorithm.SURFACE_TYPE.length; i++)
					outOptions.surfaceMaterial[i] = (mComboboxSurfaceMaterial[i] == null) ?
							-1 : mComboboxSurfaceMaterial[i].getSelectionModel().getSelectedIndex();
				outOptions.backgroundColor = mCheckboxUseBackground.isSelected() ? mBackgroundColorPicker.valueProperty().get() : null;
				outOptions.floorColor = mCheckboxUseFloor.isSelected() ? mFloorColorPicker.valueProperty().get() : null;
				outOptions.shinyFloor = mCheckboxShinyFloor.isSelected();
				outOptions.depthBlurring = mCheckboxDepthBlurring.isSelected();
				outOptions.optimizeTranslation = (mol != null) && mCheckboxOptimizeTranslation.isSelected();
				outOptions.optimizeRotation = (mol != null) && mCheckboxOptimizeRotation.isSelected();
				outOptions.brightness = (float)mSliderBrightness.getValue();
				return outOptions;
				}
			return null;
			});
		}

	@Override
	public void handle(ActionEvent event) {
		if (event.getSource() == mCheckboxUseBackground
		 || event.getSource() == mCheckboxUseFloor) {
			disableItems();
			return;
		}
	}

	private void disableItems() {
		mBackgroundColorPicker.setDisable(!mCheckboxUseBackground.isSelected());
		mFloorColorPicker.setDisable(!mCheckboxUseFloor.isSelected());
		mCheckboxShinyFloor.setDisable(!mCheckboxUseFloor.isSelected());
	}
}

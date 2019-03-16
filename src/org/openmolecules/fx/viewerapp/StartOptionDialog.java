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

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class StartOptionDialog extends Dialog<StartOptions> implements EventHandler<ActionEvent> {
	private static final String[] MODE_OPTIONS = {
			"Get from PDB",
			"Test small molecules",
			"Test molecules surfaces",
			"Test Conformers",
			"Test protein",
			"Test surface from voxel data" };

	private ComboBox mComboboxMode;
	private TextField mTextFieldPDBCode;
	private CheckBox mCheckBoxCropLigand;

	/**
	 * @param parent
	 */
	public StartOptionDialog(Window parent, StartOptions options) {
		super();

		initOwner(parent);
		initStyle(StageStyle.UNDECORATED);
		initModality(Modality.WINDOW_MODAL);
		setTitle("FXMolViewer Mode");
//		setHeaderText("Select modus:");
//		setGraphic(new ImageView(this.getClass().getResource("something.png").toString()));

//		ButtonType renderButtonType = new ButtonType("Custom", ButtonBar.ButtonData.OK_DONE);
		getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

		GridPane grid = new GridPane();
		grid.setHgap(8);
		grid.setVgap(8);
		grid.setPadding(new javafx.geometry.Insets(8, 8, 8, 8));

		int yIndex = -1;

		grid.add(new Label("Viewer Mode:"), 0, ++yIndex);
		mComboboxMode = new ComboBox();
		for (String mode:MODE_OPTIONS)
			mComboboxMode.getItems().add(mode);
		mComboboxMode.getSelectionModel().select(options == null ? 0 : options.getMode());
		mComboboxMode.addEventHandler(ActionEvent.ACTION, this);
		grid.add(mComboboxMode, 1, yIndex);

		grid.add(new Label("PDB entry code:"), 0, ++yIndex);
		mTextFieldPDBCode = new TextField(options == null ? "5om7" : options.getPDBEntryCode());
		grid.add(mTextFieldPDBCode, 1, yIndex);

		mCheckBoxCropLigand = new CheckBox("Crop ligand");
		mCheckBoxCropLigand.setSelected(options == null || options.geCropLigand());
		grid.add(mCheckBoxCropLigand, 0, ++yIndex);

		getDialogPane().setContent(grid);

		disableItems();

		setResultConverter(dialogButton -> {
			if (dialogButton == ButtonType.OK) {
				StartOptions outOptions = new StartOptions(
						mComboboxMode.getSelectionModel().getSelectedIndex(),
						mTextFieldPDBCode.getText(),
						null,
						mCheckBoxCropLigand.isSelected() );
				return outOptions;
			}
			return null;
		});
	}

	@Override
	public void handle(ActionEvent event) {
		if (event.getSource() == mComboboxMode) {
			disableItems();
			return;
		}
	}

	private void disableItems() {
		mTextFieldPDBCode.setDisable(mComboboxMode.getSelectionModel().getSelectedIndex() != 0);
		mCheckBoxCropLigand.setDisable(mComboboxMode.getSelectionModel().getSelectedIndex() != 0);
	}
}

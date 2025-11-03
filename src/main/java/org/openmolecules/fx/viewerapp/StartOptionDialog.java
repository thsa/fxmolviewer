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
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.File;

public class StartOptionDialog extends Dialog<StartOptions> implements EventHandler<ActionEvent> {

	private final ComboBox<String> mComboboxMode;
	private final TextField mTextFieldPDBCode;
	private final CheckBox mCheckBoxCropLigand;

	/**
	 * @param parent
	 */
	public StartOptionDialog(Window parent, StartOptions options) {
		super();

		initOwner(parent);
		initStyle(StageStyle.UNDECORATED);
		initModality(Modality.WINDOW_MODAL);
		setTitle("FXMolViewer Test Mode");
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
		mComboboxMode = new ComboBox<>();
		for (String mode:StartOptions.MODE_OPTIONS)
			mComboboxMode.getItems().add(mode);
		mComboboxMode.getSelectionModel().select(options == null ? 0 : options.getMode());
		mComboboxMode.addEventHandler(ActionEvent.ACTION, this);
		grid.add(mComboboxMode, 1, yIndex);

		grid.add(new Label("PDB entry code:"), 0, ++yIndex);
		mTextFieldPDBCode = new TextField(options == null ? "5om7" : options.getPDBEntryCode());
		grid.add(mTextFieldPDBCode, 1, yIndex);

		mCheckBoxCropLigand = new CheckBox("Crop ligand");
		mCheckBoxCropLigand.setSelected(options == null || options.getCropLigand());
		grid.add(mCheckBoxCropLigand, 0, ++yIndex);

		getDialogPane().setContent(grid);

		disableItems();

		setResultConverter(dialogButton -> {
			if (dialogButton == ButtonType.OK) {
				int mode = mComboboxMode.getSelectionModel().getSelectedIndex();
				String pdbID = (mode == 0) ? mTextFieldPDBCode.getText() : null;
				String file = (mode == 1) ? selectPDBFile(parent) : null;
				return new StartOptions( mode, pdbID, file, mCheckBoxCropLigand.isSelected() );
			}
			return null;
		});
	}

	public static String selectPDBFile(Window parent) {
		String path = System.getProperty("homepath") != null ? System.getProperty("homepath") : StartOptions.HOME_PATH;
		File dir = new File(path);
		FileChooser fileChooser = new FileChooser();
		if (dir.exists())
			fileChooser.setInitialDirectory(dir);
		fileChooser.setTitle("Open Protein/Ligand Structure File");
		fileChooser.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter("PDB/MMCIF-Files", "*.pdb", "*.cif", "*.mmcif"));
		return fileChooser.showOpenDialog(parent).getPath();
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
		mCheckBoxCropLigand.setDisable(mComboboxMode.getSelectionModel().getSelectedIndex() >= 2);
	}
}

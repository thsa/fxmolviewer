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

package org.openmolecules.fx.viewer3d.panel;

import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;
import org.controlsfx.control.HiddenSidesPane;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DScene;
import org.openmolecules.fx.viewer3d.V3DSceneListener;

/**
 * Created by thomas on 27.09.16.
 */
public class SidePane extends BorderPane implements V3DSceneListener {
	private static final boolean AUTO_HIDE_AT_START = false;
	private V3DScene mScene3D;
	private CheckBox mCheckBoxPin;
	private ObservableList<MoleculeModel> mCellModelList;
	private BooleanProperty mShowStructure;

	public SidePane(final V3DScene scene3D, final HiddenSidesPane pane) {
		super();

		mScene3D = scene3D;
		mScene3D.setSceneListener(this);

		mCheckBoxPin = new CheckBox("Auto-Hide Panel");
		mCheckBoxPin.setSelected(AUTO_HIDE_AT_START);
		if (!AUTO_HIDE_AT_START)
			pane.setPinnedSide(Side.LEFT);
		mCheckBoxPin.setOnAction(event -> pane.setPinnedSide(mCheckBoxPin.isSelected() ? null : Side.LEFT) );
		mCheckBoxPin.setPadding(new Insets(8, 8, 8, 8));
		mCheckBoxPin.setStyle("-fx-text-fill: white;");

		setBottom(mCheckBoxPin);

		mCellModelList = FXCollections.observableArrayList(new Callback<MoleculeModel, Observable[]>() {
			@Override
			public Observable[] call(MoleculeModel molModel) {
				return new Observable[] { molModel.getMolecule3D().visibleProperty() };
			}});

		mShowStructure = new SimpleBooleanProperty(false);

		ListView<MoleculeModel> listView = new ListView<>(mCellModelList);
		listView.setStyle("-fx-background-color: #00000000;");
		listView.setPrefWidth(160);
		listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		listView.setCellFactory(new MoleculeCellFactory(mShowStructure));
		setCenter(listView);

//		setPrefSize(160, 160);
	}

	public V3DScene getV3DScene() {
		return mScene3D;
	}

	@Override
	public void addMolecule(V3DMolecule fxmol) {
		mCellModelList.add(new MoleculeModel(fxmol));
	}

	@Override
	public void initialize(boolean isSmallMoleculeMode) {
		mShowStructure.set(isSmallMoleculeMode);
	}

	@Override
	public void removeMolecule(V3DMolecule fxmol) {
		for (int i = 0; i< mCellModelList.size(); i++) {
			if (mCellModelList.get(i).getMolecule3D() == fxmol) {
				mCellModelList.remove(i);
			}
		}
	}
}

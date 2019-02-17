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

import com.actelion.research.jfx.gui.chem.MoleculeView;
import com.actelion.research.jfx.gui.chem.MoleculeViewSkin;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DScene;

/**
 * Created by thomas on 20.10.16.
 */
public class MoleculeCell extends ListCell<MoleculeModel> implements ChangeListener<Boolean> {
	private Control mView;
	private MoleculeModel mModel;
	BooleanProperty mShowStructure;

	public MoleculeCell(BooleanProperty showStructure) {
		mShowStructure = showStructure;
		showStructure.addListener(this);
		setOnMousePressed(me -> mousePressed(me));
		setOnMouseReleased(me -> mouseReleased(me));
		setStyle("-fx-background-color: #00000000; -fx-padding: 0;");
	}

	@Override
	public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
		updateView(false);
		if (mModel != null)
			configureView();
	}

	@Override
	public void updateItem(MoleculeModel item, boolean empty) {
		super.updateItem(item, empty);

		mModel = item;
		updateView(empty);
	}

	public void updateView(boolean empty) {
		if (mShowStructure.get()) {
			mView = (empty || mModel == null) ? null : new MoleculeView(mModel.getMolecule2D());
			if (mView != null) {
				((MoleculeView)mView).setBackgroundColor(new Color(0, 0, 0, 0));   // for transparency
				configureView();
			}
		}
		else {
			mView = (empty || mModel == null) ? null : new Label(mModel.getMoleculeName());
			if (mView != null) {
				configureView();
			}
		}

		setGraphic(mView);
	}

	private void mousePressed(MouseEvent me) {
		if (me.isPopupTrigger()) {
			handlePopupMenu(me);
		}
		else if (mModel != null) {
			mModel.getMolecule3D().setVisible(!mModel.getMolecule3D().isVisible());
		}
	}

	private void configureView() {
		if (mView instanceof MoleculeView) {
			MoleculeViewSkin skin = (MoleculeViewSkin)mView.getSkin();
			if (mModel.getMolecule3D().isVisible())
				skin.setOverruleColors(null, null);
			else
				skin.setOverruleColors(new Color(0.3, 0.3, 0.3, 1), null);
		}
		else {
			if (mModel.getMolecule3D().isVisible())
				((Label)mView).textFillProperty().setValue(Color.WHITE);
			else
				((Label)mView).textFillProperty().setValue(new Color(0.3, 0.3, 0.3, 1));
		}
	}

	private void mouseReleased(MouseEvent me) {
		if (me.isPopupTrigger()) {
			handlePopupMenu(me);
		}
	}

	private void handlePopupMenu(MouseEvent me) {
		if (mModel == null)
			return;

		V3DMolecule fxmol = mModel.getMolecule3D();
		V3DScene scene = getScene3D();

		MenuItem itemShow = new MenuItem("This Molecule");
		itemShow.setDisable(mModel.getMolecule3D().isVisible());
		itemShow.setOnAction(e -> fxmol.setVisible(true));

		MenuItem itemHide = new MenuItem("This Molecule");
		itemHide.setDisable(!mModel.getMolecule3D().isVisible());
		itemHide.setOnAction(e -> fxmol.setVisible(false));

		MenuItem itemShowAll = new MenuItem("All Molecules");
		itemShowAll.setOnAction(e -> scene.setAllVisible(true));

		MenuItem itemHideAll = new MenuItem("All Molecules");
		itemHideAll.setOnAction(e -> scene.setAllVisible(false));

		MenuItem itemDelete = new MenuItem("This Molecule");
		itemDelete.setOnAction(e -> scene.delete(mModel.getMolecule3D()));

		MenuItem itemDeleteHidden = new MenuItem("Hidden Molecules");
		itemDeleteHidden.setOnAction(e -> scene.deleteInvisibleMolecules());

		MenuItem itemDeleteAll = new MenuItem("All Molecules");
		itemDeleteAll.setOnAction(e -> scene.deleteAllMolecules());

		RadioMenuItem itemModeText = new RadioMenuItem("Show Name");
		itemModeText.setSelected(!mShowStructure.get());
		itemModeText.setOnAction(e -> mShowStructure.setValue(false));
		RadioMenuItem itemModeStructure = new RadioMenuItem("Show Structure");
		itemModeStructure.setSelected(mShowStructure.get());
		itemModeStructure.setOnAction(e -> mShowStructure.setValue(true));
		Menu menuMode = new Menu("List Style");
		menuMode.getItems().addAll(itemModeText, itemModeStructure);

		ContextMenu popup = new ContextMenu();

		Menu menuShow = new Menu("Show");
		menuShow.getItems().addAll(itemShow, itemShowAll);
		popup.getItems().add(menuShow);

		Menu menuHide = new Menu("Hide");
		menuHide.getItems().addAll(itemHide, itemHideAll);
		popup.getItems().add(menuHide);

		popup.getItems().add(new SeparatorMenuItem());

		Menu menuDelete = new Menu("Delete");
		menuDelete.getItems().addAll(itemDelete, itemDeleteHidden, itemDeleteAll);
		popup.getItems().add(menuDelete);

		popup.getItems().add(new SeparatorMenuItem());

		popup.getItems().add(menuMode);

		popup.show(this, me.getScreenX(), me.getScreenY());
	}

	private V3DScene getScene3D() {
		Node parent = getParent();
		while (!(parent instanceof SidePane))
			parent = parent.getParent();
		return ((SidePane)parent).getV3DScene();
	}
}

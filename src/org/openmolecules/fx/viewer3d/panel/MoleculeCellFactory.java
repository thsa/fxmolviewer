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

import javafx.beans.property.BooleanProperty;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;

/**
 * Created by thomas on 20.10.16.
 */
public class MoleculeCellFactory implements Callback<TableColumn<MoleculeModel,MoleculeModel>, TableCell<MoleculeModel,MoleculeModel>> {
	private BooleanProperty mShowStructure;
	public MoleculeCellFactory(BooleanProperty showStructure) {
		mShowStructure = showStructure;
	}



	@Override
	public TableCell<MoleculeModel, MoleculeModel> call(TableColumn<MoleculeModel, MoleculeModel> param) {
		// TODO Auto-generated method stub
		return new MoleculeCell(mShowStructure);
	}
}

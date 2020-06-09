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

import org.openmolecules.fx.viewer3d.V3DMolGroup;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DMolecule.MoleculeRole;

import com.actelion.research.jfx.gui.chem.MoleculeView;
import com.actelion.research.jfx.gui.chem.MoleculeViewSkin;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;


/**
 * Created by thomas on 20.10.16.
 */
public class MoleculeCell extends TreeTableCell<MolGroupModel,MolGroupModel> implements MolGroupModelChangeListener {
	private Control mView;
	private MolGroupModel mModel;
	BooleanProperty mShowStructure;

	public MoleculeCell(BooleanProperty showStructure) {
		mShowStructure = showStructure;
		showStructure.addListener((v,ov,nv) -> updateView(false));

	}



	@Override
	public void updateItem(MolGroupModel item, boolean empty) {
		super.updateItem(item, empty);
		mModel = item;
		if (empty || item == null) {
		     setText(null);
		     setGraphic(null);}
		else {
			 updateView(empty);
		 }
		if(item!=null)
			item.addMolGroupModelChangeListener(this);
		

	}

	public void updateView(boolean empty) {
		if (mShowStructure.get()) {
			mView = (empty || mModel == null) ? null : new MoleculeView(mModel.getMolecule2D());
			if (mView != null) {
				((MoleculeView)mView).setBackgroundColor(new Color(0, 0, 0, 0));
				configureView();
			}
		}
		else {
			//mView = (empty || mModel == null) ? null : new Label(mModel.getMoleculeName(), RoleShapeFactory.fromRole(mModel.getMolecule3D().getRole()));
		mView = (empty || mModel == null) ? null : new Label(mModel.getMoleculeName());	
		if (mView != null) {
				((Label)mView).setFont(Font.font(15));
				configureView();
			
		}
		}
		setGraphic(mView);	
		this.getTreeTableView().refresh();
	}



	private void configureView() {
		if (mView instanceof MoleculeView) {
			MoleculeViewSkin skin = (MoleculeViewSkin)mView.getSkin();

			skin.setOverruleColors(null, null);

		}
		else {

				((Label)mView).textFillProperty().setValue(Color.WHITE);
				V3DMolGroup content = mModel.getMolecule3D();
				if(content instanceof V3DMolecule) {
					V3DMolecule fxmol = (V3DMolecule) content;
					((Label)mView).setGraphic(RoleShapeFactory.fromRole(fxmol.getRole()));
				}
		
		}
	}



	@Override
	public void groupModelChanged() {
		if(mModel==null || mView==null)
			return;
		updateView(false);
		
	}











}

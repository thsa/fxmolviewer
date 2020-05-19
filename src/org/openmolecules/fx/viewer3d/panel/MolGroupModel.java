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

import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.coords.CoordinateInventor;

import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import java.util.ArrayList;

import org.openmolecules.fx.viewer3d.MolStructureChangeListener;
import org.openmolecules.fx.viewer3d.V3DMolGroup;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DMolecule.MoleculeRole;

/**
 * Created by thomas on 09/10/16.
 */
public class MolGroupModel implements  ChangeListener<Boolean> {
	private V3DMolGroup mMol3D;
	private BooleanProperty isMolVisible;
	private BooleanProperty isSelected;

	public MolGroupModel(V3DMolGroup mol3D) {
		mMol3D = mol3D;
		isMolVisible =  new SimpleBooleanProperty(true);
		isMolVisible.addListener(this);
		//isSelected = new SimpleBooleanProperty(false);
		//isSelected.addListener((o,ov,nv) -> mMol3D.setIncluded(nv));
	}



	public String getMoleculeName() {
		 return mMol3D.getName();
	}



	public V3DMolGroup getMolecule3D() {
		return mMol3D;
	}

	
	public BooleanProperty visibleProperty() {
		return this.isMolVisible;
	}
	

	
	public void setVisibleProperty(boolean isVisible) {
		isMolVisible.set(isVisible);
	}
	
	
	public void setSelectionProperty(boolean selection) {
		isSelected.set(selection);
	}
	


	@Override
	public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
		mMol3D.setVisible(isMolVisible.get());
		
	}


}

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
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DMolecule.MoleculeRole;

/**
 * Created by thomas on 09/10/16.
 */
public class MoleculeModel implements MolStructureChangeListener, ChangeListener<Boolean> {
	private V3DMolecule mMol3D;
	private ObjectProperty<StereoMolecule> mMol2D;
	private ObjectProperty<MoleculeRole> mRole;
	private IntegerProperty mGroup;
	private BooleanProperty isMolVisible;
	private BooleanProperty isSelected;

	public MoleculeModel(V3DMolecule mol3D) {
		mMol3D = mol3D;
		mMol3D.addMoleculeStructureChangeListener(this);
		mMol2D = new SimpleObjectProperty<StereoMolecule>();
		mRole = new SimpleObjectProperty<MoleculeRole>(mMol3D.getRole());
		mRole.addListener((o,ov,nv) -> mMol3D.setRole(mRole.get()));
		mGroup = new SimpleIntegerProperty(mMol3D.getGroup());
		mGroup.addListener((o,ov,nv) -> {mMol3D.setGroup((Integer)nv);});
		isMolVisible =  new SimpleBooleanProperty(true);
		isMolVisible.addListener(this);
		isSelected = new SimpleBooleanProperty(false);
		isSelected.addListener((o,ov,nv) -> mMol3D.setIncluded(nv));
	}

	private StereoMolecule createMolecule2D(V3DMolecule mol3D) {
		StereoMolecule mol = mol3D.getMolecule().getCompactCopy();
		mol.ensureHelperArrays(Molecule.cHelperParities);	// create parities from 3D-coords
		new CoordinateInventor(CoordinateInventor.MODE_REMOVE_HYDROGEN).invent(mol);
		mol.setStereoBondsFromParity();

		return mol;
	}

	public String getMoleculeName() {
		if (mMol3D.getMolecule().getName() == null)
			mMol3D.getMolecule().setName("Structure "+ mMol3D.getID());

		return mMol3D.getMolecule().getName();
	}

	public StereoMolecule getMolecule2D() {
		if (mMol3D != null) {
			mMol2D.set(createMolecule2D(mMol3D));
		}

		return mMol2D.get();
	}

	public V3DMolecule getMolecule3D() {
		return mMol3D;
	}

	
	public BooleanProperty visibleProperty() {
		return this.isMolVisible;
	}
	
	public ObjectProperty<StereoMolecule> twoDProperty() {
		return mMol2D;
	}
	
	public void setVisibleProperty(boolean isVisible) {
		isMolVisible.set(isVisible);
	}
	
	public void setGroupProperty(int group) {
		mGroup.set(group);
	}
	
	public void setSelectionProperty(boolean selection) {
		isSelected.set(selection);
	}
	
	
	public IntegerProperty groupProperty() {
		return mGroup;
	}
	
	public ObjectProperty<MoleculeRole> roleProperty() {
		return mRole;
	}
	
	public void setRoleProperty(MoleculeRole role) {
		mRole.set(role);
	}

	@Override
	public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
		mMol3D.setVisible(isMolVisible.get());
		
	}

	@Override
	public void structureChanged() {
		mMol2D.set(this.createMolecule2D(mMol3D));
	}
}

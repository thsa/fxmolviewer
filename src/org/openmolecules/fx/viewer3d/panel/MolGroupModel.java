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
import java.util.HashSet;

import org.openmolecules.fx.viewer3d.MolStructureChangeListener;
import org.openmolecules.fx.viewer3d.V3DMolGroup;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DMolecule.MoleculeRole;

/**
 * Created by thomas on 09/10/16.
 */
public class MolGroupModel implements MolStructureChangeListener {
	private V3DMolGroup mMol3D;
	private BooleanProperty isMolVisible;
	private ObjectProperty<StereoMolecule> mMol2D;
	private ObjectProperty<MoleculeRole> mRole;
	private HashSet<MolGroupModelChangeListener> mChangeListeners;

	public MolGroupModel(V3DMolGroup mol3D)  {
		mMol3D = mol3D;
		mChangeListeners = new HashSet<>();
		mMol2D = new SimpleObjectProperty<StereoMolecule>();
		isMolVisible =  mMol3D.visibleProperty();
		if(mMol3D instanceof V3DMolecule) {
			mRole = ((V3DMolecule)mMol3D).RoleProperty();
			mRole.addListener((v,ov,nv) -> mChangeListeners.forEach(e -> e.groupModelChanged()));
			((V3DMolecule)mMol3D).addMoleculeStructureChangeListener(this);
		}
	}

	private StereoMolecule createMolecule2D(V3DMolGroup molGroup) {
		StereoMolecule mol2D = null;
		if(molGroup instanceof V3DMolecule) {
			V3DMolecule mol3D = (V3DMolecule) molGroup;
			StereoMolecule mol = mol3D.getMolecule().getCompactCopy();
			if(mol.getAllAtoms()<=100) {
				mol.ensureHelperArrays(Molecule.cHelperParities);	// create parities from 3D-coords
				new CoordinateInventor(CoordinateInventor.MODE_REMOVE_HYDROGEN).invent(mol);
				mol.setStereoBondsFromParity();
				mol2D = mol;
			}
		}
		return mol2D;
	}
	
	public StereoMolecule getMolecule2D() {
		if (mMol3D != null) {
			if(mMol3D instanceof V3DMolecule)
				mMol2D.set(createMolecule2D((V3DMolecule)mMol3D));
		}

		return mMol2D.get();
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

	
	public void setRole(MoleculeRole role) {
		if(mRole!=null)
			mRole.set(role);
	}
	
	public void addMolGroupModelChangeListener(MolGroupModelChangeListener listener) {
		mChangeListeners.add(listener);
		
	}
	
	public ObjectProperty<MoleculeRole> roleProperty() {
		return mRole;
	}
	
	public ObjectProperty<StereoMolecule> twoDProperty() {
		return mMol2D;
	}
	
	@Override
	public void structureChanged() {
		mChangeListeners.forEach(e -> e.groupModelChanged());
	}
	



}

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
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.openmolecules.fx.viewer3d.V3DMolGroup;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DMolecule.MoleculeRole;
import org.openmolecules.fx.viewer3d.V3DScene;
import org.openmolecules.fx.viewer3d.V3DSceneListener;


/**
 * Created by thomas on 27.09.16.
 */
public class MolGroupPane extends ScrollPane implements ListChangeListener<V3DMolGroup> {
	//private static final boolean AUTO_HIDE_AT_START = false;
	private V3DScene mScene3D;
	//private CheckBox mCheckBoxPin;
	private BooleanProperty mShowStructure;
	private MoleculeGroupTable mGroupTable;
	//private Map<Integer,ArrayList<V3DMolecule>> mGroups;

	public MolGroupPane(final V3DScene scene3D) {
		super();
		setFitToWidth(true);
		setFitToHeight(true);
		mScene3D = scene3D;
		mScene3D.getWorld().addListener(this);
		//mGroups = new HashMap<Integer,ArrayList<V3DMolecule>>();
		setFitToHeight(true);
		setFitToWidth(true);
		setHbarPolicy(ScrollBarPolicy.NEVER);
		//setContent(mContainer);
		mShowStructure = new SimpleBooleanProperty(false);
		MolPaneMouseHandler mouseHandler = new MolPaneMouseHandler(this);
		mGroupTable = new MoleculeGroupTable(this);
		setContent(mGroupTable.getTable());

	}
	
	
	public void clearTableSelections() {
		mGroupTable.getTable().getSelectionModel().clearSelection();
	}
	
	public V3DScene getV3DScene() {
		return mScene3D;
	}
	
	public BooleanProperty getShowStructureProperty() { 
		return mShowStructure;
	}
	
	public boolean isShowStructure() {
		return mShowStructure.get();
	}
	
	public void setShowStructure(boolean showStructure) {
		mShowStructure.set(showStructure);
	}
	
	public TreeTableView<MolGroupModel> getMolTable() {
		return mGroupTable.getTable();
	}
	

	public void changeVisibilityAll(boolean isVisible) {
		mGroupTable.changeVisibility(isVisible);
	}
	
	public void changeVisibilitySelected(boolean isVisible) {
		mGroupTable.changeVisibilitySelected(isVisible);

	}
	
	/*
	public void changeGroupSelected(int targetGroup) {

			mGroupTable.changeGroupSelected(targetGroup);
		}
	}
	
	public void changeRoleSelected(MoleculeRole role) {
		ArrayList<Integer> groups = new ArrayList<Integer>(mGroupTable.keySet());
		for(int group : groups) {
			mGroupTable.get(group).changeRoleSelected(role);
		}
	}
	*/
	

	
	public List<V3DMolGroup> getAllSelectedMols(){
		List<V3DMolGroup> allSelectedMols = new ArrayList<V3DMolGroup>();
		allSelectedMols.addAll(mGroupTable.getSelectedMols());
		return allSelectedMols;
	}
	


	
	public void addMolecule(V3DMolGroup fxmol) {
		V3DMolGroup parent = mScene3D.getWorld().getParent(fxmol);	
		addMolecule(fxmol,parent);
	}
	
	private void addMolecule(V3DMolGroup fxmol, V3DMolGroup parent) {
		mGroupTable.addGroup(fxmol, parent);
		if(fxmol.getMolGroups().size()>0) {
			for(V3DMolGroup child : fxmol.getMolGroups()) {
				addMolecule(child,fxmol);
			}
		}
		else {
			return;
		}
	}

	
	
	//@Override
	public void initialize(boolean isSmallMoleculeMode) {
		mShowStructure.set(isSmallMoleculeMode);
	}

	//@Override
	public void removeMolecule(V3DMolGroup fxmol) {
	
		mGroupTable.removeMolecule(fxmol);

		
	}
	



	@Override
	public void onChanged(Change<? extends V3DMolGroup> c) {
		while(c.next()) {
			List<? extends V3DMolGroup> added = c.getAddedSubList();
			for(V3DMolGroup group : added) {
				addMolecule(group);
			}
			
			List<? extends V3DMolGroup> removed = c.getRemoved();
			for(V3DMolGroup group : removed) {
				removeMolecule(group);
			}
		}
	}
		
		
	
}

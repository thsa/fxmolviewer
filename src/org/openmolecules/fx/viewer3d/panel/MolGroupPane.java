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

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.SortedList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableRow;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import javafx.util.converter.IntegerStringConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.controlsfx.control.HiddenSidesPane;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DScene;
import org.openmolecules.fx.viewer3d.V3DSceneListener;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;

/**
 * Created by thomas on 27.09.16.
 */
public class MolGroupPane extends ScrollPane implements V3DSceneListener {
	//private static final boolean AUTO_HIDE_AT_START = false;
	private V3DScene mScene3D;
	//private CheckBox mCheckBoxPin;
	private BooleanProperty mShowStructure;
	private VBox mContainer;
	private Map<Integer,MoleculeGroupTable> mGroupTable;
	//private Map<Integer,ArrayList<V3DMolecule>> mGroups;

	public MolGroupPane(final V3DScene scene3D) {
		super();
		mScene3D = scene3D;
		mScene3D.setSceneListener(this);
		mGroupTable = new HashMap<Integer,MoleculeGroupTable>();
		//mGroups = new HashMap<Integer,ArrayList<V3DMolecule>>();
		mContainer = new VBox();
		setHbarPolicy(ScrollBarPolicy.NEVER);
		setContent(mContainer);
		mContainer.prefWidthProperty().bind(this.widthProperty());
		mShowStructure = new SimpleBooleanProperty(false);
		MolPaneMouseHandler mouseHandler = new MolPaneMouseHandler(this);

	}
	
	
	public void clearTableSelections() {
		mGroupTable.forEach((k,v) -> v.getTable().getSelectionModel().clearSelection());
	}
	
	public void clearOtherTableSelections(TableView table) {
		mGroupTable.values().stream().map(e -> e.getTable()).filter(t -> t!=table).forEach(t -> t.getSelectionModel().clearSelection());		
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
	
	public List<TableView> getMolTables() {
		return mGroupTable.values().stream().map(e -> e.getTable()).collect(Collectors.toList());
	}
	
	public Set<Integer> getGroups() {
		return mGroupTable.keySet();
	}
	
	public void changeVisibilityAll(boolean isVisible) {
		mGroupTable.values().forEach(e -> e.changeVisibility(isVisible));
	}
	
	public void changeVisibilitySelected(boolean isVisible) {
		Iterator<MoleculeGroupTable> iterator = mGroupTable.values().iterator();
		while(iterator.hasNext()) 
			iterator.next().changeVisibilitySelected(isVisible);

	}
	
	public void changeGroupSelected(int targetGroup) {
		ArrayList<Integer> groups = new ArrayList<Integer>(mGroupTable.keySet());
		for(int group : groups) {
			mGroupTable.get(group).changeGroupSelected(targetGroup);
		}
	}
	
	public VBox getContainer() {
		return mContainer;
	}
	
	public List<V3DMolecule> getAllSelectedMols(){
		List<V3DMolecule> allSelectedMols = new ArrayList<V3DMolecule>();
		mGroupTable.forEach((k,v) -> allSelectedMols.addAll(v.getSelectedMols()));
		return allSelectedMols;
	}
	


	@Override
	public void addMolecule(V3DMolecule fxmol) {
		int group = fxmol.getGroup();

		if(mGroupTable.containsKey(group)) {
			mGroupTable.get(group).addMolecule(fxmol);
		}
		else {
			mGroupTable.put(group, new MoleculeGroupTable(this,group,new ArrayList<V3DMolecule>()));
			mGroupTable.get(group).addMolecule(fxmol);
		}

	}

	@Override
	public void initialize(boolean isSmallMoleculeMode) {
		mShowStructure.set(isSmallMoleculeMode);
	}

	@Override
	public void removeMolecule(V3DMolecule fxmol) {
		List<Integer> toBeDeleted = new ArrayList<Integer>();
		mGroupTable.forEach((k,v) -> {
			if(v.containsMolecule(fxmol))
				if(v.removeMolecule(fxmol)) //mol list is empty
					toBeDeleted.add(k);

		});
		for(Integer group : toBeDeleted) {
			mGroupTable.get(group).cleanup();
			mGroupTable.remove(group);
		}
		
	}
	



	public void updateGroups(V3DMolecule fxmol) {
		removeMolecule(fxmol);
		addMolecule(fxmol);
		
	}
		
		
	
}

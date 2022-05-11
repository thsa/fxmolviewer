package org.openmolecules.fx.viewer3d;

import com.actelion.research.chem.Coordinates;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Point3D;

import java.util.ArrayList;
import java.util.List;

public class V3DMolGroup extends RotatableGroup implements IV3DMoleculeGroup {
	
	protected ObservableList<V3DMolGroup> children;
	protected List<ListChangeListener<V3DMolGroup>> listeners;

	
	public V3DMolGroup(String name) {
		super(name);
		children = FXCollections.observableArrayList();
		listeners = new ArrayList<ListChangeListener<V3DMolGroup>>();
	}

	
	public void addMolGroup(V3DMolGroup fxmol) {
		for(ListChangeListener<V3DMolGroup> listener : listeners)
			fxmol.addListener(listener);
		children.add(fxmol);
		getChildren().add(fxmol);
		this.visibleProperty().addListener((v,ov,nv) -> fxmol.setVisible(nv));
	}
	
	public void deleteMolecule(V3DMolGroup toDelete) {
		deleteMolecule(toDelete,this);
	}
	
	private void deleteMolecule(V3DMolGroup toDelete, V3DMolGroup root) {
		List<V3DMolGroup> children = root.children;
		if(children.size()==0)
			return;
		if(children.contains(toDelete)) {
			root.children.remove(toDelete);
			root.getChildren().remove(toDelete);
		}
		for(V3DMolGroup child : children)
			deleteMolecule(toDelete,child);
	}
	
	/*
	public V3DMolGroup getParent(V3DMolGroup group) {
		return getParent(group,this);
	}
	
	private V3DMolGroup getParent(V3DMolGroup group, V3DMolGroup root) {
		List<V3DMolGroup> children = root.children;
		if(children.size()==0)
			return null;
		else {
			if(children.contains(group))
				return root;
			else {
				for(V3DMolGroup child : children) {
					V3DMolGroup temp = getParent(group,child);
					if(temp!=null)
						return temp;
				}
			}
		}
		return null;
		
	}
	*/
	//all nodes of the subtree attached to this group
	public List<V3DMolGroup> getAllAttachedMolGroups() {
		List<V3DMolGroup> allChildren = new ArrayList<V3DMolGroup>();
		getAllAttachedMolGroups(this,allChildren);
		return allChildren;
	}
	
	private void getAllAttachedMolGroups(V3DMolGroup root, List<V3DMolGroup> allChildren) {
		allChildren.add(root);
		if(root.getMolGroups().size()==0)
			return;
		for(V3DMolGroup group :root.getMolGroups())
			getAllAttachedMolGroups(group,allChildren);
		
	}
	
	public List<V3DMolGroup> getMolGroups() {
		return this.children;
	}

	
	public void addListener(ListChangeListener<V3DMolGroup> listener) {
		listeners.add(listener);
		children.addListener(listener);
		children.forEach(e -> e.addListener(listener));
	}
	
	/**
	 * the parent group of all V3DMolGroups (and therefore all V3DMolecules) is the "world" group which is 
	 * attached to the scene. As subGroups we consider all groups that are child nodes of the world group.
	 * @param world
	 * @return
	 */
	/*
	public V3DMolGroup getParentSubGroup(V3DMolGroup world) {
		if(this==world)
			return null;
		V3DMolGroup subGroup = this;
		while(true) {
			V3DMolGroup parent = world.getParent(subGroup);
			if(parent==world)
				return subGroup;
			subGroup = parent;
		}
	}
	*/
	public Coordinates getWorldCoordinates(V3DScene scene, Coordinates coordinates) {
		Point3D point = new Point3D(coordinates.x, coordinates.y, coordinates.z);
		if(this==scene.getWorld())
			return new Coordinates(coordinates.x, coordinates.y, coordinates.z);
		V3DMolGroup subGroup = this;
		while(true) {
			V3DMolGroup parent = scene.getParent(subGroup);
			point = subGroup.localToParent(point);
			if(parent==scene.getWorld())
				return new Coordinates(point.getX(), point.getY(), point.getZ());
			subGroup = parent;
		}
		
		
	}
	
	public Coordinates getWorldToLocalCoordinates(V3DScene scene, Coordinates coordinates) {
		Point3D point = new Point3D(coordinates.x, coordinates.y, coordinates.z);
		if(this==scene.getWorld())
			return new Coordinates(coordinates.x, coordinates.y, coordinates.z);
		V3DMolGroup subGroup = this;
		while(true) {
			V3DMolGroup parent = scene.getParent(subGroup);
			point = subGroup.parentToLocal(point);
			if(parent==scene.getWorld())
				return new Coordinates(point.getX(), point.getY(), point.getZ());
			subGroup = parent;
		}
		
		
	}
	

	
	
}

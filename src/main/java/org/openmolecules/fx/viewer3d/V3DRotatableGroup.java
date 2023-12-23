package org.openmolecules.fx.viewer3d;

import com.actelion.research.chem.Coordinates;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Point3D;

import java.util.ArrayList;
import java.util.List;

public class V3DRotatableGroup extends RotatableGroup implements IV3DMoleculeGroup {
	
	protected ObservableList<V3DRotatableGroup> children;
	protected List<ListChangeListener<V3DRotatableGroup>> listeners;

	
	public V3DRotatableGroup(String name) {
		super(name);
		children = FXCollections.observableArrayList();
		listeners = new ArrayList<>();
	}

	
	public void addGroup(V3DRotatableGroup group) {
		for(ListChangeListener<V3DRotatableGroup> listener : listeners)
			group.addListener(listener);
		children.add(group);
		getChildren().add(group);
		this.visibleProperty().addListener((v,ov,nv) -> group.setVisible(nv));
	}
	
	public void deleteGroup(V3DRotatableGroup group) {
		deleteGroup(group,this);
	}
	
	private void deleteGroup(V3DRotatableGroup group, V3DRotatableGroup root) {
		List<V3DRotatableGroup> children = root.children;
		if(children.size()==0)
			return;
		if(children.contains(group)) {
			root.children.remove(group);
			root.getChildren().remove(group);
		}
		for(V3DRotatableGroup child : children)
			deleteGroup(group,child);
	}
	
	/*
	public V3DRotatableGroup getParent(V3DRotatableGroup group) {
		return getParent(group,this);
	}
	
	private V3DRotatableGroup getParent(V3DRotatableGroup group, V3DRotatableGroup root) {
		List<V3DRotatableGroup> children = root.children;
		if(children.size()==0)
			return null;
		else {
			if(children.contains(group))
				return root;
			else {
				for(V3DRotatableGroup child : children) {
					V3DRotatableGroup temp = getParent(group,child);
					if(temp!=null)
						return temp;
				}
			}
		}
		return null;
		
	}
	*/
	//all nodes of the subtree attached to this group
	public List<V3DRotatableGroup> getAllAttachedRotatableGroups() {
		List<V3DRotatableGroup> allChildren = new ArrayList<V3DRotatableGroup>();
		getAllAttachedRotatableGroups(this,allChildren);
		return allChildren;
	}
	
	private void getAllAttachedRotatableGroups(V3DRotatableGroup root, List<V3DRotatableGroup> allChildren) {
		allChildren.add(root);
		if(root.getGroups().size()==0)
			return;
		for(V3DRotatableGroup group :root.getGroups())
			getAllAttachedRotatableGroups(group,allChildren);
		
	}
	
	public List<V3DRotatableGroup> getGroups() {
		return this.children;
	}

	
	public void addListener(ListChangeListener<V3DRotatableGroup> listener) {
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
	public V3DRotatableGroup getParentSubGroup(V3DRotatableGroup world) {
		if(this==world)
			return null;
		V3DRotatableGroup subGroup = this;
		while(true) {
			V3DRotatableGroup parent = world.getParent(subGroup);
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
		V3DRotatableGroup subGroup = this;
		while(true) {
			V3DRotatableGroup parent = scene.getParent(subGroup);
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
		V3DRotatableGroup subGroup = this;
		while(true) {
			V3DRotatableGroup parent = scene.getParent(subGroup);
			point = subGroup.parentToLocal(point);
			if(parent==scene.getWorld())
				return new Coordinates(point.getX(), point.getY(), point.getZ());
			subGroup = parent;
		}
		
		
	}
	

	
	
}

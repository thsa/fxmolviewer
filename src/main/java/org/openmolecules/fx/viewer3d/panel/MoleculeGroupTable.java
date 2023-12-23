package org.openmolecules.fx.viewer3d.panel;

import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.*;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DMolecule.MoleculeRole;
import org.openmolecules.fx.viewer3d.V3DRotatableGroup;
import org.openmolecules.fx.viewer3d.V3DScene.ViewerSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class MoleculeGroupTable {
	private TreeTableView<MolGroupModel> mMolTable;
	private MolGroupPane mPane;
	
	
	public MoleculeGroupTable(MolGroupPane pane) {
		mPane = pane;
		initTable();
		construct();
		
		mMolTable.getSelectionModel().selectedItemProperty().addListener((v,ov,nv) -> {
			List<MolGroupModel> selectedModels = mMolTable.getSelectionModel().getSelectedItems().stream()
					.map(e -> e.getValue()).collect(Collectors.toList());
			for(TreeItem<MolGroupModel> selectedItem : mMolTable.getSelectionModel().getSelectedItems()) {
				List<TreeItem<MolGroupModel>> treeItems = new ArrayList<>();
				getAllChildren(selectedItem,treeItems);
				//treeItems.forEach(m -> mMolTable.getSelectionModel().select(m));
			}
			List<MolGroupModel> tableItems = getTableItems().stream().map(e -> e.getValue()).collect(Collectors.toList());
			for(MolGroupModel model : tableItems) {
				if(model.getMolecule3D() instanceof V3DMolecule) {
					V3DMolecule v3dMol = (V3DMolecule) model.getMolecule3D();
					if (selectedModels.contains(model)) {
						v3dMol.SelectionProperty().set(true);
					
					}
					else 
						v3dMol.SelectionProperty().set(false);
			
			}	
			}
			
		});
		
		

	}
	
	public void updateTableSelection() {

		List<MolGroupModel> selectedModels = mMolTable.getSelectionModel().getSelectedItems().stream()
				.map(e -> e.getValue()).collect(Collectors.toList());
		for(TreeItem<MolGroupModel> selectedItem : mMolTable.getSelectionModel().getSelectedItems()) {
			List<TreeItem<MolGroupModel>> treeItems = new ArrayList<>();
			getAllChildren(selectedItem,treeItems);
			treeItems.forEach(m -> mMolTable.getSelectionModel().select(m));
		}
		selectedModels = mMolTable.getSelectionModel().getSelectedItems().stream()
				.map(e -> e.getValue()).collect(Collectors.toList());
		List<MolGroupModel> tableItems = getTableItems().stream().map(e -> e.getValue()).collect(Collectors.toList());
		for(MolGroupModel model : tableItems) {
			if(model.getMolecule3D() instanceof V3DMolecule) {
				V3DMolecule v3dMol = (V3DMolecule) model.getMolecule3D();
				if (selectedModels.contains(model)) {
					v3dMol.SelectionProperty().set(true);
				
				}
				else 
					v3dMol.SelectionProperty().set(false);
		
		}	
		}
			
	
	}
	
	private void construct() {
		MolGroupModel rootModel = new MolGroupModel(mPane.getV3DScene().getWorld());
		mMolTable.setRoot(new TreeItem<MolGroupModel>(rootModel));
		mMolTable.setShowRoot(false);

	}

	public void addGroup(V3DRotatableGroup molGroup, V3DRotatableGroup parent) {
		addChild(molGroup,parent);
	}
	
	public void removeMolecule(V3DRotatableGroup fxmol) {
		mMolTable.getSelectionModel().clearSelection();
		TreeItem<MolGroupModel> root = mMolTable.getRoot();
		deleteTreeItem(root,fxmol);
		
	}


	
	private void initTable() {
		
	
		//mTitledPane.setText("Group " + Integer.toString(mGroup));
		//mTitledPane.setStyle("-fx-font-size: 18;");
		
		
		/*
		mCellModelList = FXCollections.observableArrayList(new Callback<MolGroupModel, Observable[]>() {
			@Override
			public Observable[] call(MolGroupModel molModel) {
				return new Observable[] {molModel.twoDProperty(),molModel.roleProperty(),molModel.groupProperty()};
			}});
		*/

	    	    			  

		mMolTable = new TreeTableViewNoHeader<MolGroupModel>();

		mMolTable.setEditable(true);
		mMolTable.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
		
		mMolTable.setFocusTraversable(false);
		mMolTable.setPlaceholder(new Label(""));
		mMolTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		
		TreeTableColumn<MolGroupModel,MolGroupModel> strucCol = new TreeTableColumn<MolGroupModel,MolGroupModel>("Molecule");
		mMolTable.getColumns().add(strucCol);
		strucCol.setCellValueFactory(cellData -> new SimpleObjectProperty<MolGroupModel>(cellData.getValue().getValue()));
		strucCol.setCellFactory(new MoleculeCellFactory());	
		
		TreeTableColumn<MolGroupModel,MolGroupModel> roleCol = new TreeTableColumn<MolGroupModel,MolGroupModel>("Role");
		mMolTable.getColumns().add(roleCol);
		if(mPane.getV3DScene().getSettings().contains(ViewerSettings.ROLE)) {
			roleCol.setCellValueFactory(cellData -> new SimpleObjectProperty<MolGroupModel>(cellData.getValue().getValue()));
			roleCol.setCellFactory((e) -> {
				return new RoleCell();
			});
			roleCol.prefWidthProperty().bind(mPane.widthProperty().multiply(0.1));
		}
		//groupCol.setStyle("-fx-font-size: 15");
		
		mMolTable.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);

		strucCol.prefWidthProperty().bind(mPane.widthProperty().multiply(0.9));
		
		strucCol.maxWidthProperty().bind(strucCol.prefWidthProperty());
		strucCol.setResizable(false);

		//visibleCol.setEditable(true);
			
	}
	
	public void changeVisibility(boolean visible) {
		for(TreeItem<MolGroupModel> item : getTableItems()) {
			System.out.println("change vis");
			item.getValue().setVisibleProperty(visible);
		}
	}
	
	
	public void changeVisibilitySelected(boolean visible) {

		for(TreeItem<MolGroupModel> item : mMolTable.getSelectionModel().getSelectedItems()) {
			item.getValue().setVisibleProperty(visible);
		}


	}


	
	public TreeTableView<MolGroupModel> getTable() {
		return mMolTable;
	}

	
	public List<V3DRotatableGroup> getSelectedMols() {
		 return  mMolTable.getSelectionModel().getSelectedItems().stream().map(e -> e.getValue().getMolecule3D()).collect(Collectors.toList());
	}
	
	public List<TreeItem<MolGroupModel>> getTableItems() { 
		TreeItem<MolGroupModel> root = mMolTable.getRoot();
		List<TreeItem<MolGroupModel>> treeItems = new ArrayList<TreeItem<MolGroupModel>>();
		getAllChildren(root,treeItems);
		return treeItems;
	}
	
	private void deleteTreeItem(TreeItem<MolGroupModel> parent, V3DRotatableGroup toDelete) {

			
		if(parent.getChildren().size()==0)
			return;
		for(TreeItem<MolGroupModel> child : parent.getChildren()) {
			if(child.getValue().getMolecule3D() == toDelete) {
				parent.getChildren().remove(child);
				return;
			}
			deleteTreeItem(child,toDelete);
		}
	}
	
	private void addChild(V3DRotatableGroup child, V3DRotatableGroup parent) {
		MolGroupModel newModel = new MolGroupModel(child);
		TreeItem<MolGroupModel> newItem = new TreeItem<>(newModel);
		if(newItem.getValue().getMolecule3D() instanceof V3DMolecule) {
			V3DMolecule v3dMol = (V3DMolecule) newItem.getValue().getMolecule3D();
			v3dMol.SelectionProperty().addListener((v,ov,nv) -> {
				if(v3dMol.SelectionProperty().get())
					mMolTable.getSelectionModel().select(newItem);
				else {
					mMolTable.getSelectionModel().clearSelection(mMolTable.getRow(newItem), mMolTable.getTreeColumn());
				}
			});
		}
			
		List<TreeItem<MolGroupModel>> treeItems = getTableItems();
		for(TreeItem<MolGroupModel> item : treeItems) {
			if(parent==item.getValue().getMolecule3D()) {
				item.getChildren().add(newItem);

				break;
			}
		}
		
	}
	
	
	
	private void getAllChildren(TreeItem<MolGroupModel> parent, List<TreeItem<MolGroupModel>> allItems) {
		allItems.add(parent);
		if(parent.getChildren()==null)
			return;
		if(parent.getChildren().size()==0)
			return;
		for(TreeItem<MolGroupModel> child : parent.getChildren()) {
			getAllChildren(child,allItems);
		}
	}
	
	
	
	
	public void changeRoleSelected(MoleculeRole role) {
		List<MolGroupModel> selectedModels = new ArrayList<MolGroupModel>();
		mMolTable.getSelectionModel().getSelectedItems().stream().forEach(e -> {
			selectedModels.add(e.getValue());
		});
		for(MolGroupModel selectedModel : selectedModels) {
			selectedModel.setRole(role);
		}
	}
	

}

package org.openmolecules.fx.viewer3d.panel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.IV3DMoleculeGroup;
import org.openmolecules.fx.viewer3d.V3DMolGroup;
import org.openmolecules.fx.viewer3d.V3DMolecule.MoleculeRole;

import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.control.cell.CheckBoxTreeTableCell;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

public class MoleculeGroupTable {
	private TreeTableView<MolGroupModel> mMolTable;
	private BooleanProperty mShowStructure;
	private MolGroupPane mPane;
	
	
	public MoleculeGroupTable(MolGroupPane pane) {
		mPane = pane;
		initTable();
		construct();
		mMolTable.getSelectionModel().selectedItemProperty().addListener((v,ov,nv) -> {
			List<MolGroupModel> selectedModels = mMolTable.getSelectionModel().getSelectedItems().stream()
					.map(e -> e.getValue()).collect(Collectors.toList());
			List<MolGroupModel> tableItems = getTableItems().stream().map(e -> e.getValue()).collect(Collectors.toList());
			for(MolGroupModel model : tableItems) {
				if (selectedModels.contains(model))
					model.getMolecule3D().setIncluded(true);
				else 
					model.getMolecule3D().setIncluded(false);
			}
			});

	}
	
	private void construct() {
		MolGroupModel rootModel = new MolGroupModel(mPane.getV3DScene().getWorld());
		mMolTable.setRoot(new TreeItem<MolGroupModel>(rootModel));
		mMolTable.setShowRoot(false);

	}

	public void addGroup(V3DMolGroup molGroup, V3DMolGroup parent) {
		addChild(molGroup,parent);
	}
	
	public void removeMolecule(V3DMolGroup fxmol) {
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

	    	    			  

		mShowStructure = mPane.getShowStructureProperty();
		mMolTable = new TreeTableView<MolGroupModel>();
		mMolTable.setEditable(true);
		mMolTable.setFocusTraversable(false);
		mMolTable.setPlaceholder(new Label(""));
		mMolTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		TreeTableColumn<MolGroupModel,MolGroupModel> strucCol = new TreeTableColumn<MolGroupModel,MolGroupModel>("Molecule");
		mMolTable.getColumns().add(strucCol);
		strucCol.setCellValueFactory(cellData -> new SimpleObjectProperty<MolGroupModel>(cellData.getValue().getValue()));
		strucCol.setCellFactory(new MoleculeCellFactory(mShowStructure));	

		//groupCol.setStyle("-fx-font-size: 15");
		
		mMolTable.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
		TreeTableColumn<MolGroupModel,Boolean> visibleCol = new TreeTableColumn<MolGroupModel,Boolean>("V");
		visibleCol.setEditable(true);
		visibleCol.setCellValueFactory(
		new Callback<CellDataFeatures<MolGroupModel,Boolean>,ObservableValue<Boolean>>()
		{

		    @Override
		    public ObservableValue<Boolean> call(CellDataFeatures<MolGroupModel, Boolean> param)
		    {   
		        return param.getValue().getValue().visibleProperty();
		    }   
		});


		visibleCol.setCellFactory( CheckBoxTreeTableCell.forTreeTableColumn(visibleCol) );

		mMolTable.getColumns().add(visibleCol);



		
		strucCol.prefWidthProperty().bind(mPane.widthProperty().multiply(0.7));
		strucCol.maxWidthProperty().bind(strucCol.prefWidthProperty());
		strucCol.setResizable(false);
		
		visibleCol.prefWidthProperty().bind(mPane.widthProperty().multiply(0.15));
		visibleCol.maxWidthProperty().bind(visibleCol.prefWidthProperty());
		visibleCol.setResizable(false);
		//visibleCol.setEditable(true);
			
	}
	
	public void changeVisibility(boolean visible) {
		for(TreeItem<MolGroupModel> item : getTableItems()) {
			item.getValue().setVisibleProperty(visible);
		}
	}
	
	
	public void changeVisibilitySelected(boolean visible) {
		mMolTable.getSelectionModel().getSelectedItems().stream().forEach(e -> e.getValue().setVisibleProperty(visible));
	}


	
	public TreeTableView<MolGroupModel> getTable() {
		return mMolTable;
	}

	
	public List<V3DMolGroup> getSelectedMols() {
		 return  mMolTable.getSelectionModel().getSelectedItems().stream().map(e -> e.getValue().getMolecule3D()).collect(Collectors.toList());
	}
	
	public List<TreeItem<MolGroupModel>> getTableItems() { 
		TreeItem<MolGroupModel> root = mMolTable.getRoot();
		List<TreeItem<MolGroupModel>> treeItems = new ArrayList<TreeItem<MolGroupModel>>();
		getAllChildren(root,treeItems);
		return treeItems;
	}
	
	private void deleteTreeItem(TreeItem<MolGroupModel> parent, V3DMolGroup toDelete) {

			
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
	
	private void addChild(V3DMolGroup child, V3DMolGroup parent) {
		MolGroupModel newModel = new MolGroupModel(child);
		TreeItem<MolGroupModel> newItem = new TreeItem<>(newModel);
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
		if(parent.getChildren().size()==0)
			return;
		for(TreeItem<MolGroupModel> child : parent.getChildren()) {
			getAllChildren(child,allItems);
		}
	}
	

}

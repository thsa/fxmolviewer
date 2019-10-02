package org.openmolecules.fx.viewer3d.panel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.openmolecules.fx.viewer3d.V3DMolecule;

import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.EventHandler;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.util.Callback;
import javafx.util.converter.IntegerStringConverter;

public class MoleculeGroupTable {
	private int mGroup;
	private ArrayList<V3DMolecule> mFXmols;
	private TableViewWithVisibleRowCountNoHeader<MoleculeModel> mMolTable;
	private ObservableList<MoleculeModel> mCellModelList;
	private BooleanProperty mShowStructure;
	private MolGroupPane mPane;
	private TitledPane mTitledPane;
	
	
	public MoleculeGroupTable(MolGroupPane pane,int group, ArrayList<V3DMolecule> fxmols) {
		mGroup = group;
		mFXmols = fxmols;
		mPane = pane;
		initTable();
		construct();
	

	}
	
	private void construct() {
		for(V3DMolecule fxmol: mFXmols) {
			addMolecule(fxmol);
		}
	}

	public void addMolecule(V3DMolecule fxmol) {
		mCellModelList.add(new MoleculeModel(fxmol));
	}
	
	public boolean removeMolecule(V3DMolecule fxmol) {
		for (int i = 0; i< mCellModelList.size(); i++) {
			if (mCellModelList.get(i).getMolecule3D() == fxmol) {
				mCellModelList.remove(i);
			}
		}
		boolean isEmpty = false;
		if(mCellModelList.size()==0)
			isEmpty = true;
		return isEmpty;
	}
	
	public boolean containsMolecule(V3DMolecule fxmol) {
		return mCellModelList.stream().map(e -> e.getMolecule3D()).collect(Collectors.toList()).contains(fxmol);
	}

	
	private void initTable() {
		mTitledPane = new TitledPane();
		mTitledPane.setText("Group " + Integer.toString(mGroup));
		//mTitledPane.setStyle("-fx-font-size: 18;");
		mPane.getContainer().getChildren().add(mTitledPane);
		mCellModelList = FXCollections.observableArrayList(new Callback<MoleculeModel, Observable[]>() {
			@Override
			public Observable[] call(MoleculeModel molModel) {
				return new Observable[] {molModel.twoDProperty(),molModel.roleProperty(),molModel.groupProperty()};
			}});
		
	    SortedList<MoleculeModel> sortedList = new SortedList<>(mCellModelList, 
	    	      (MoleculeModel mm1, MoleculeModel mm2) -> {
	    	    	  int id1 = mm1.getMolecule3D().getID();
	    	    	  int id2 = mm2.getMolecule3D().getID();
	    	    	  if(id1>id2)
	    	    		  return 1;
	    	    	  else if (id1<id2)
	    	    		  return -1;
	    	    	  else
	    	    		  return 0;
	    	    				 
	    	      });
	    	    			  

	    
		mShowStructure = mPane.getShowStructureProperty();
		mMolTable = new TableViewWithVisibleRowCountNoHeader<MoleculeModel>();
		mMolTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		mCellModelList.addListener((ListChangeListener<? super MoleculeModel>)((e) -> {mMolTable.getVisibleRowCount().set(mCellModelList.size());mMolTable.refresh();}));
		TableColumn<MoleculeModel,MoleculeModel> strucCol = new TableColumn<MoleculeModel,MoleculeModel>("Molecule");
		mMolTable.getColumns().add(strucCol);
		strucCol.setCellValueFactory(cellData -> new SimpleObjectProperty(cellData.getValue()));
		strucCol.setCellFactory(new MoleculeCellFactory(mShowStructure));	

		//groupCol.setStyle("-fx-font-size: 15");
		
		mMolTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		TableColumn visibleCol = new TableColumn("V");
		visibleCol.setCellValueFactory(
		new Callback<CellDataFeatures<MoleculeModel,Boolean>,ObservableValue<Boolean>>()
		{

		    @Override
		    public ObservableValue<Boolean> call(CellDataFeatures<MoleculeModel, Boolean> param)
		    {   
		        return param.getValue().visibleProperty();
		    }   
		});


		visibleCol.setCellFactory( CheckBoxTableCell.forTableColumn(visibleCol) );

		mMolTable.getColumns().add(visibleCol);
		mMolTable.setEditable(true);

		mMolTable.setItems(sortedList);
		mTitledPane.setContent(mMolTable);

		
		strucCol.prefWidthProperty().bind(mPane.widthProperty().multiply(0.7));
		strucCol.maxWidthProperty().bind(strucCol.prefWidthProperty());
		strucCol.setResizable(false);
		
		visibleCol.prefWidthProperty().bind(mPane.widthProperty().multiply(0.15));
		visibleCol.maxWidthProperty().bind(visibleCol.prefWidthProperty());
		visibleCol.setResizable(false);
			
	}
	
	public void changeVisibility(boolean visible) {
		for(MoleculeModel molModel : mCellModelList) 
			molModel.setVisibleProperty(visible);
	}
	
	
	public void changeVisibilitySelected(boolean visible) {
		mMolTable.getSelectionModel().getSelectedItems().stream().forEach(e -> e.setVisibleProperty(visible));
	}
	
	public void changeGroupSelected(int group) {
		List<MoleculeModel> selectedModels = new ArrayList<MoleculeModel>();
		mMolTable.getSelectionModel().getSelectedItems().stream().forEach(e -> {
			selectedModels.add(e);
		});
		for(MoleculeModel selectedModel : selectedModels) {
			selectedModel.setGroupProperty(group);
			mPane.updateGroups(selectedModel.getMolecule3D());
		}

	}
	
	public void cleanup() {
		mPane.getContainer().getChildren().remove(mTitledPane);
	}
	
	public TableView<MoleculeModel> getTable() {
		return mMolTable;
	}

	
	public List<V3DMolecule> getSelectedMols() {
		 return  mMolTable.getSelectionModel().getSelectedItems().stream().map(e -> e.getMolecule3D()).collect(Collectors.toList());
	}
	

}

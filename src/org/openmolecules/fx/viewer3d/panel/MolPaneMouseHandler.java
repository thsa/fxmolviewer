package org.openmolecules.fx.viewer3d.panel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.openmolecules.fx.viewer3d.V3DMolecule;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;

public class MolPaneMouseHandler {
	
	private MolGroupPane mMolPane;
	private TableView mLastActiveTable;
	private static ContextMenu sContextMenu;
	
	public MolPaneMouseHandler(MolGroupPane molPane) {
		mMolPane = molPane;
		mMolPane.addEventFilter(MouseEvent.MOUSE_PRESSED,
		        new EventHandler<MouseEvent>() {
            		public void handle(final MouseEvent mouseEvent) {
            	mousePressed(mouseEvent);}
            });
	}
	
	private void mousePressed(MouseEvent me) {
		if(sContextMenu!=null && sContextMenu.isShowing()) 
			sContextMenu.hide();
		boolean selectionMode = me.isControlDown();
    	TableRow<MoleculeModel> pickedRow = null;
    	Node pickedNode = me.getPickResult().getIntersectedNode();
    	while(pickedNode.getParent()!=null) {
    		if (pickedNode.getParent() instanceof TableRow)
    			pickedRow = (TableRow) pickedNode.getParent();
    		pickedNode = pickedNode.getParent();
    	}
		if(me.isPopupTrigger()) {
			if(pickedRow==null) 
				handlePopupMenu(me,null);
			else {
				MoleculeModel model = pickedRow.getItem();
				handlePopupMenu(me,model);
			}
			return;
		}
    	if (pickedRow != null) {
    		if(pickedRow.getTableView()==mLastActiveTable) {  //click within same table already handled by default event handling
    			if(!selectionMode)
    				mMolPane.clearOtherTableSelections(pickedRow.getTableView());
    			return;
    		}
    		else if (!selectionMode) { //click at different TableView with selection deactivated
    			mMolPane.clearTableSelections();
    		}

    		mLastActiveTable = pickedRow.getTableView();
    	}
    	else {
    		mMolPane.clearTableSelections();
    		mLastActiveTable = null;
    	}

	}
	

	
	private void handlePopupMenu(MouseEvent me, MoleculeModel model) {
		
		
		if(sContextMenu!=null && sContextMenu.isShowing()) 
			sContextMenu.hide();
		ContextMenu popup = new ContextMenu();
		sContextMenu = popup;
		popup.getItems().add(new SeparatorMenuItem());
		
		if(model!=null) {
			MenuItem menuRole = new MenuItem("Change Role");
			menuRole.setOnAction(e -> this.createRoleChooserDialog(model));
			popup.getItems().add(menuRole);
			MenuItem menuGroup = new MenuItem("Change Group");
			menuGroup.setOnAction(e -> this.createGroupChangeDialog(model));
			popup.getItems().add(menuGroup);
		}


		RadioMenuItem itemModeText = new RadioMenuItem("Show Name");
		itemModeText.setSelected(!mMolPane.isShowStructure());
		itemModeText.setOnAction(e -> mMolPane.setShowStructure(false));
		RadioMenuItem itemModeStructure = new RadioMenuItem("Show Structure");
		itemModeStructure.setSelected(mMolPane.isShowStructure());
		itemModeStructure.setOnAction(e -> {mMolPane.setShowStructure(true);});
		Menu menuMode = new Menu("List Style");
		menuMode.getItems().addAll(itemModeText, itemModeStructure);

		
		MenuItem itemDelete = new MenuItem("Selected Molecules");
		itemDelete.setOnAction(e -> mMolPane.getV3DScene().delete(mMolPane.getAllSelectedMols()));

		MenuItem itemDeleteHidden = new MenuItem("All Hidden Molecules");
		itemDeleteHidden.setOnAction(e -> mMolPane.getV3DScene().deleteInvisibleMolecules());

		MenuItem itemDeleteAll = new MenuItem("All Molecules");
		itemDeleteAll.setOnAction(e -> mMolPane.getV3DScene().deleteAllMolecules());

		Menu menuDelete = new Menu("Delete");
		menuDelete.getItems().addAll(itemDelete, itemDeleteHidden, itemDeleteAll);
		popup.getItems().add(menuDelete);
		
		MenuItem itemHideAll = new MenuItem("All Molecules");
		itemHideAll.setOnAction(e -> {
			mMolPane.changeVisibilityAll(false);
		});
		
		MenuItem itemHide = new MenuItem("Selected Molecules");
		itemHide.setOnAction(e -> {
			mMolPane.changeVisibilitySelected(false);
		});
		
		Menu menuHide = new Menu("Hide");
		menuHide.getItems().addAll(itemHide, itemHideAll);
		popup.getItems().add(menuHide);
		
		MenuItem itemShowAll = new MenuItem("All Molecules");
		itemShowAll.setOnAction(e -> {
			mMolPane.changeVisibilityAll(true);
		});
		
		MenuItem itemShow = new MenuItem("Selected Molecules");
		itemShow.setOnAction(e -> {
			mMolPane.changeVisibilitySelected(true);
		});
		
		Menu menuShow = new Menu("Show");
		menuShow.getItems().addAll(itemShow, itemShowAll);
		popup.getItems().add(menuShow);
		
		popup.getItems().add(new SeparatorMenuItem());

		popup.getItems().add(menuMode);
		
		MenuItem move = new MenuItem("Move to new Group");
		move.setOnAction(e -> {
			int newGroup = mMolPane.getV3DScene().getMaxGroupID()+1;
			mMolPane.changeGroupSelected(newGroup);
		});
		
		popup.getItems().add(move);


		popup.show(mMolPane, me.getScreenX(), me.getScreenY());
	}
	
	private void createGroupChangeDialog(MoleculeModel model) {
		List<String> choices = new ArrayList<>();
		for(Integer group : mMolPane.getGroups())
			choices.add(Integer.toString(group));

		ChoiceDialog<String> dialog = new ChoiceDialog<>(Integer.toString(model.groupProperty().get()), choices);
		dialog.setTitle("Molecule Group Dialog");
		dialog.setContentText("Select Group: ");
		Optional<String> result = dialog.showAndWait();
		result.ifPresent(group -> {
			model.setGroupProperty(Integer.parseInt(group));
			
		});
	}
	
	private void createRoleChooserDialog(MoleculeModel model) {
		List<String> choices = new ArrayList<>();
		choices.add(V3DMolecule.MoleculeRole.LIGAND.toString());
		choices.add(V3DMolecule.MoleculeRole.MACROMOLECULE.toString());
		choices.add(V3DMolecule.MoleculeRole.COFACTOR.toString());
		choices.add(V3DMolecule.MoleculeRole.SOLVENT.toString());
		ChoiceDialog<String> dialog = new ChoiceDialog<>(model.roleProperty().get().toString(), choices);
		dialog.setTitle("Molecule Role Dialog");
		dialog.setContentText("Select Role: ");
		Optional<String> result = dialog.showAndWait();
		result.ifPresent(role -> {
			if (!role.equals(model.roleProperty().get().toString())) {
				if(role.equals(V3DMolecule.MoleculeRole.LIGAND.toString()))
					model.setRoleProperty(V3DMolecule.MoleculeRole.LIGAND);
				else if(role.equals(V3DMolecule.MoleculeRole.MACROMOLECULE.toString()))
					model.setRoleProperty(V3DMolecule.MoleculeRole.MACROMOLECULE);
				else if(role.equals(V3DMolecule.MoleculeRole.COFACTOR.toString()))
					model.setRoleProperty(V3DMolecule.MoleculeRole.COFACTOR);
				else if(role.equals(V3DMolecule.MoleculeRole.SOLVENT.toString()))
					model.setRoleProperty(V3DMolecule.MoleculeRole.SOLVENT);
			}
		});
	}

}

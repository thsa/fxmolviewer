package org.openmolecules.fx.viewer3d.panel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.openmolecules.fx.tasks.V3DPheSAScreener;
import org.openmolecules.fx.viewer3d.V3DCustomizablePheSA;
import org.openmolecules.fx.viewer3d.V3DMolGroup;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DMolecule.MoleculeRole;
import org.openmolecules.fx.viewer3d.io.V3DMoleculeParser;
import org.openmolecules.fx.viewer3d.io.V3DMoleculeWriter;

import com.actelion.research.chem.phesa.VolumeGaussian;

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
import javafx.scene.control.TreeTableRow;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

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
		mMolPane.addEventFilter(MouseEvent.MOUSE_RELEASED,
		        new EventHandler<MouseEvent>() {
            		public void handle(final MouseEvent mouseEvent) {
            	mousePressed(mouseEvent);}
           });
	}
	
	
	

	
	private void mousePressed(MouseEvent me) {
		if(sContextMenu!=null && sContextMenu.isShowing()) 
			sContextMenu.hide();
		//boolean selectionMode = me.isControlDown();
    	TreeTableRow<MolGroupModel> pickedRow = null;
    	Node pickedNode = me.getPickResult().getIntersectedNode();
    	while(pickedNode.getParent()!=null) {
    		if (pickedNode.getParent() instanceof TreeTableRow)
    			pickedRow = (TreeTableRow<MolGroupModel>) pickedNode.getParent();
    		pickedNode = pickedNode.getParent();
    	}
		if(me.isPopupTrigger()) {
			if(pickedRow==null) 
				handlePopupMenu(me,null);
			else {
				MolGroupModel model = pickedRow.getItem();
				handlePopupMenu(me,model);
			}
			return;
		}
		/*
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
    	*/

	}
	

	
	private void handlePopupMenu(MouseEvent me, MolGroupModel model) {
		
		
		if(sContextMenu!=null && sContextMenu.isShowing()) 
			sContextMenu.hide();
		ContextMenu popup = new ContextMenu();
		sContextMenu = popup;
		popup.getItems().add(new SeparatorMenuItem());
		
		if(model!=null) {
			V3DMolGroup group = model.getMolecule3D();
			if(group instanceof V3DCustomizablePheSA) {
				V3DCustomizablePheSA phesaModel = (V3DCustomizablePheSA) group;
				MenuItem itemES = new MenuItem("Add ExclusionSphere");
				itemES.setOnAction(e -> phesaModel.placeExclusionSphere(VolumeGaussian.EXCLUSION));
				popup.getItems().add(itemES);
				
				MenuItem itemIS = new MenuItem("Add InclusionSphere");
				itemIS.setOnAction(e -> phesaModel.placeExclusionSphere(VolumeGaussian.INCLUSION));
				popup.getItems().add(itemIS);
				popup.getItems().add(new SeparatorMenuItem());
				
				MenuItem savePheSA = new MenuItem("Save as PheSA Query");
				savePheSA.setOnAction(e -> {
					File saveFile = createFileSaverDialog();
					if(saveFile!=null) {
						V3DMoleculeWriter.savePhesaQueries(saveFile, phesaModel);
					}
				});
				
				popup.getItems().add(savePheSA);
			}
				
		}
		
		/*
		if(model!=null) {
			MenuItem menuRole = new MenuItem("Change Role");
			menuRole.setOnAction(e -> this.createRoleChooserDialog(model));
			popup.getItems().add(menuRole);
		}
		
		MenuItem menuGroup = new MenuItem("Change Group");
		menuGroup.setOnAction(e -> this.createGroupChangeDialog());
		popup.getItems().add(menuGroup);
		*/


		
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

		/*
		MenuItem move = new MenuItem("Move to new Group");
		move.setOnAction(e -> {
			int newGroup = mMolPane.getV3DScene().getMaxGroupID()+1;
			mMolPane.changeGroupSelected(newGroup);
		});
		
		
		popup.getItems().add(move);
		*/
		
		/*
		MenuItem savePheSA = new MenuItem("Save as PheSA Queries");
		savePheSA.setOnAction(e -> {
			File saveFile = createFileSaverDialog();
			if(saveFile!=null) {
				List<V3DMolecule> fxmols = mMolPane.getAllSelectedMols();
				V3DMoleculeWriter.savePhesaQueries(saveFile, fxmols);
			}
		});
		
		popup.getItems().add(savePheSA);
		*/
		
		MenuItem loadPheSA = new MenuItem("Load PheSA Queries");
		loadPheSA.setOnAction(e -> {
			File loadFile = createDWARParserDialog();
			if(loadFile!=null) {
				List<V3DMolecule> fxMols = V3DMoleculeParser.readPheSAQuery(mMolPane.getV3DScene(), loadFile, 0);
			    for(V3DMolecule fxMol: fxMols) {
			    	mMolPane.getV3DScene().addMolecule(fxMol);
			    }
			}
		});
		
		popup.getItems().add(loadPheSA);
		
		/*
		MenuItem screenPheSA = new MenuItem("Run PheSA Screening");
		screenPheSA.setOnAction(e -> {
			FileChooser fileChooser =  getMolFileLoader();
			File loadFile = fileChooser.showOpenDialog(null);
			if(loadFile!=null) {
				V3DPheSAScreener.screen(mMolPane.getV3DScene(),loadFile);
			    }
			});
		
		popup.getItems().add(screenPheSA);
		*/

		popup.show(mMolPane, me.getScreenX(), me.getScreenY());
	}
	/*
	
	private void createGroupChangeDialog() {
		List<String> choices = new ArrayList<>();
		for(Integer group : mMolPane.getGroups())
			choices.add(Integer.toString(group));

		ChoiceDialog<String> dialog = new ChoiceDialog<>("1",choices);
		dialog.setTitle("Molecule Group Dialog");
		dialog.setContentText("Select Group: ");
		Optional<String> result = dialog.showAndWait();
		result.ifPresent(group -> {
			mMolPane.changeGroupSelected(Integer.parseInt(group));
		});
	}
	
	private void createRoleChooserDialog(MolGroupModel model) {
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
					mMolPane.changeRoleSelected(MoleculeRole.LIGAND);
					//model.setRoleProperty(V3DMolecule.MoleculeRole.LIGAND);
				else if(role.equals(V3DMolecule.MoleculeRole.MACROMOLECULE.toString()))
					mMolPane.changeRoleSelected(MoleculeRole.MACROMOLECULE);
					//model.setRoleProperty(V3DMolecule.MoleculeRole.MACROMOLECULE);
				else if(role.equals(V3DMolecule.MoleculeRole.COFACTOR.toString()))
					mMolPane.changeRoleSelected(MoleculeRole.COFACTOR);
					//model.setRoleProperty(V3DMolecule.MoleculeRole.COFACTOR);
				else if(role.equals(V3DMolecule.MoleculeRole.SOLVENT.toString()))
					mMolPane.changeRoleSelected(MoleculeRole.SOLVENT);
					//model.setRoleProperty(V3DMolecule.MoleculeRole.SOLVENT);
			}
		});
	}
	
	*/
	
	
	private File createDWARParserDialog() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open DWAR File");
		fileChooser.getExtensionFilters().add(new ExtensionFilter("DWAR Files", "*.dwar"));
		//pane.setPinnedSide(Side.RIGHT);
        File file = fileChooser.showOpenDialog(null);
        return file;

	}
	
	private FileChooser getMolFileLoader() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Molecule File");
		fileChooser.getExtensionFilters().add(new ExtensionFilter("SD Files", "*.sdf"));
		fileChooser.getExtensionFilters().add(new ExtensionFilter("DWAR Files", "*.dwar"));
		//pane.setPinnedSide(Side.RIGHT);
		
		return fileChooser;
	}
	
	private File createFileSaverDialog() {
		FileChooser fileChooser = new FileChooser();
        //Show save file dialog
        File file = fileChooser.showSaveDialog(null);
        return file;

    }
}

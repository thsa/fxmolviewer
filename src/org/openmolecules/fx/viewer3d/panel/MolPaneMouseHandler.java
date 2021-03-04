package org.openmolecules.fx.viewer3d.panel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.openmolecules.fx.viewer3d.V3DCustomizablePheSA;
import org.openmolecules.fx.viewer3d.V3DMolGroup;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DMolecule.MoleculeRole;
import org.openmolecules.fx.viewer3d.V3DScene.ViewerSettings;
import org.openmolecules.fx.viewer3d.io.V3DMoleculeParser;
import org.openmolecules.fx.viewer3d.io.V3DMoleculeWriter;

import com.actelion.research.chem.phesa.VolumeGaussian;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeTableRow;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class MolPaneMouseHandler {
	
	private MolGroupPane mMolPane;
	private static ContextMenu sContextMenu;
	
	public MolPaneMouseHandler(MolGroupPane molPane) {
		mMolPane = molPane;
		mMolPane.addEventFilter(MouseEvent.MOUSE_PRESSED,
		        new EventHandler<MouseEvent>() {
            		public void handle(final MouseEvent mouseEvent) {
            			if(mouseEvent.isPopupTrigger() && !mouseEvent.isConsumed())
            				mousePressed(mouseEvent);}
            });
		mMolPane.addEventFilter(MouseEvent.MOUSE_RELEASED,
		        new EventHandler<MouseEvent>() {
            		public void handle(final MouseEvent mouseEvent) {
            			if(mouseEvent.isPopupTrigger() && !mouseEvent.isConsumed())
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


	}
	


	
	private void handlePopupMenu(MouseEvent me, MolGroupModel model) {
		
		
		if(sContextMenu!=null && sContextMenu.isShowing()) 
			sContextMenu.hide();
		ContextMenu popup = new ContextMenu();
		sContextMenu = popup;
		popup.getItems().add(new SeparatorMenuItem());
		if(model!=null) {
			V3DMolGroup group = model.getMolecule3D();
			MenuItem itemZoom = new MenuItem("Center View");
			itemZoom.setOnAction(e -> {
				mMolPane.getV3DScene().optimizeView(group);
				mMolPane.getV3DScene().getCamera().setTranslateZ(-25);
			});
		
			popup.getItems().add(itemZoom);
			if(mMolPane.getV3DScene().getSettings().contains(ViewerSettings.EDITING)) {	
				MenuItem itemAddSubGroup = new MenuItem("Add New Subgroup");
				itemAddSubGroup.setOnAction(e-> {
					String groupName = createGroupDialog();
					group.addMolGroup(new V3DMolGroup(groupName));
				});
				popup.getItems().add(itemAddSubGroup);
				
				
				
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
				
				MenuItem loadNegRecImg = new MenuItem("Load Negative Receptor Image");
				loadNegRecImg.setOnAction(e -> {
					File loadFile = createDWARParserDialog();
					if(loadFile!=null) {
						List<V3DMolecule> fxMols = V3DMoleculeParser.readNegReceptorImage(mMolPane.getV3DScene(), loadFile, 0);
					    for(V3DMolecule fxMol: fxMols) {
					    	mMolPane.getV3DScene().addMolecule(fxMol);
					    }
					}
				});
				
				popup.getItems().add(loadNegRecImg);
				
				MenuItem moveGroups = new MenuItem("Move Selected to Group");
				
				moveGroups.setOnAction(e -> {
					V3DMolGroup targetGroup = createGroupChooserDialog();
					List<V3DMolGroup> toMove = mMolPane.getAllSelectedMolGroups();
					mMolPane.getV3DScene().moveToGroup(toMove, targetGroup);
					
					
				});
				
				popup.getItems().add(moveGroups);
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
	
			MenuItem menuRole = new MenuItem("Change Role of Selected");
			menuRole.setOnAction(e -> this.createRoleChooserDialog(model));
			popup.getItems().add(menuRole);
		}
		
		if(!mMolPane.getV3DScene().getSettings().contains(ViewerSettings.UPPERPANEL)) {
			MenuItem itemDelete = new MenuItem("Selected Molecules");
			itemDelete.setOnAction(e -> mMolPane.getV3DScene().delete(mMolPane.getAllSelectedMolGroups()));
	
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
		}

		}
		
		

		popup.show(mMolPane, me.getScreenX(), me.getScreenY());
	}
	
	
	private V3DMolGroup createGroupChooserDialog() {
		V3DMolGroup targetGroup = null;
		List<String> choices = new ArrayList<>();
		for(V3DMolGroup group : mMolPane.getV3DScene().getWorld().getAllChildren())
			choices.add(group.getName());

		ChoiceDialog<String> dialog = new ChoiceDialog<>(choices.get(0),choices);
		dialog.setTitle("Molecule Group Dialog");
		dialog.setContentText("Select Target Group: ");
		Optional<String> result = dialog.showAndWait();
		String target = result.get();
		for(V3DMolGroup group : mMolPane.getV3DScene().getWorld().getAllChildren()) {
			if(group.getName()==target) {
				targetGroup = group;
				break;
			}
		}
		return targetGroup;
		
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
	
	
	private String createGroupDialog() {
		TextInputDialog dialog = new TextInputDialog("Group");
		 
		dialog.setTitle("New Group");
		dialog.setHeaderText("Enter Group Name:");
		dialog.setContentText("Name:");
		 
		Optional<String> result = dialog.showAndWait();
		
		return result.get();

	}
	
	private File createFileSaverDialog() {
		FileChooser fileChooser = new FileChooser();
        //Show save file dialog
        File file = fileChooser.showSaveDialog(null);
        return file;
    }
}

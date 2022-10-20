package org.openmolecules.fx.viewer3d.panel;

import org.openmolecules.fx.viewer3d.V3DMolecule.MoleculeRole;

import com.actelion.research.jfx.gui.chem.MoleculeView;
import com.actelion.research.jfx.gui.chem.MoleculeViewSkin;
import com.actelion.research.share.gui.editor.Model;

import javafx.beans.property.BooleanProperty;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TreeTableCell;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class RoleCell extends TreeTableCell<MolGroupModel,MolGroupModel> implements MolGroupModelChangeListener {
	private static final Color LIGAND_COLOR = Color.DARKRED;
	private static final Color PROTEIN_COLOR = Color.STEELBLUE;
	private static final Color SOLVENT_COLOR = Color.BLANCHEDALMOND;
	private static final Color COFACTOR_COLOR = Color.WHITE;
	
	private Label mLabel;
	private MolGroupModel mModel;

	public RoleCell() {

	}



	@Override
	public void updateItem(MolGroupModel item, boolean empty) {
		super.updateItem(item, empty);
		mModel = item;
		if (empty || item == null) {
		     setText(null);
		     setGraphic(null);}
		else {
			 updateView(empty);
		 }
		if(item!=null)
			item.addMolGroupModelChangeListener(this);
		

	}

	public void updateView(boolean empty) {
		if(mModel.roleProperty()!=null) {
			mLabel = (empty || mModel == null) ? null : new Label(mModel.roleProperty().getValue().toString());	
			if (mLabel != null) {
					MoleculeRole role = mModel.roleProperty().getValue();
					mLabel.setFont(Font.font(15));
					switch(role) {
						case LIGAND:
							mLabel.setTextFill(LIGAND_COLOR);
							break;
						case MACROMOLECULE:
							mLabel.setTextFill(PROTEIN_COLOR);
							break;
						case SOLVENT:
							mLabel.setTextFill(SOLVENT_COLOR);
							break;
						case COFACTOR:
							mLabel.setTextFill(COFACTOR_COLOR);
							break;
						default:
							mLabel.setTextFill(LIGAND_COLOR);
	
				
			}
			setGraphic(mLabel);	
			}
			this.getTreeTableView().refresh();
		}
	}






	@Override
	public void groupModelChanged() {
		if(mModel==null || mLabel==null)
			return;
		updateView(false);
		
	}



}

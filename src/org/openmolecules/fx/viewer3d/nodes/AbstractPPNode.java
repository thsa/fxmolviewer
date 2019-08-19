package org.openmolecules.fx.viewer3d.nodes;

import java.util.Optional;

import org.openmolecules.fx.viewer3d.V3DMolecule;

import com.actelion.research.chem.phesa.pharmacophore.PPGaussian;

import javafx.scene.Group;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.paint.PhongMaterial;

public abstract class AbstractPPNode extends Group implements IPPNode {

	protected PPGaussian ppg;
	protected PhongMaterial material;
	protected int role;
	private ContextMenu menu;
	
	
	AbstractPPNode(PPGaussian ppg,PhongMaterial material, int role){
		setUserData(new NodeDetail(material, role , false));
		this.ppg = ppg;
		this.material = material;
		this.role = role;
		createMenu();
	}
	
	
	private void createMenu() {
		TextInputDialog dialog = new TextInputDialog(Double.toString(ppg.getWeight()));
		dialog.setTitle("Weight");
		dialog.setHeaderText("Adjust Pharmacophore Weight");
		dialog.setContentText("Enter value between 1.0 to 10.0");
		menu = new ContextMenu();
		MenuItem delete = new MenuItem("Remove");
		delete.setOnAction(e -> {
			cleanup();
		});
		MenuItem changeWeight = new MenuItem("Adjust Weight");
		changeWeight.setOnAction(e -> {
			Optional<String> weight = dialog.showAndWait();
			if(weight.isPresent()) {
				double weightFactor = Double.parseDouble(weight.get());
				if(weightFactor<1.0)
					weightFactor = 1.0;
				if(weightFactor>10.0) 
					weightFactor = 10.0;
				ppg.setWeight(weightFactor);
				getChildren().clear();
				construct();
			}
		});
		menu.getItems().add(delete);
		menu.getItems().add(changeWeight);
	}
	
		
	
	@Override
	public final void showMenu(double x, double y) {

		menu.show(this, x, y);
        }

	

	@Override
	public final void cleanup() {
		V3DMolecule fxmol = (V3DMolecule) this.getParent().getParent();
		fxmol.getPharmacophore().getMolVol().getPPGaussians().remove(ppg);
		fxmol.getPharmacophore().getChildren().remove(this);
	}
	
	@Override 
	
	public final double getScalingFactor() {
		double scalingFactor = 1.0;
		if(ppg.getWeight()>=1.0)
			scalingFactor = 1 + (ppg.getWeight()-1.0)*(1.0/9.0);
			
		else
			scalingFactor = 1 + (ppg.getWeight()-1.0)*(10.0/18.0);
		
		return scalingFactor;
	}


}

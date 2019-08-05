package org.openmolecules.fx.viewer3d;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.phesa.ExclusionGaussian;

import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.TransformChangedEvent;

public class ExclusionSphere extends Sphere  {
	public static PhongMaterial sExclusionMaterial;
	private ExclusionGaussian eg;
	private ContextMenu menu;
	
	public ExclusionSphere(ExclusionGaussian eg, int role) {
		super(eg.getWidth(), 100);
		sExclusionMaterial = new PhongMaterial();
		sExclusionMaterial.setSpecularColor(new Color(0.9,0.9,0.9,0.1));
		sExclusionMaterial.setDiffuseColor(new Color(0.9,0.9,0.9,0.1).darker());
		this.eg = eg;
		this.setMaterial(sExclusionMaterial);
		this.setTranslateX(eg.getCenter().x);
		this.setTranslateY(eg.getCenter().y);
		this.setTranslateZ(eg.getCenter().z);
		setUserData(new NodeDetail(sExclusionMaterial, role , false));
		EventHandler<TransformChangedEvent> eh = e -> {
			eg.setShiftVector(new Coordinates(getTranslateX(), getTranslateY(), getTranslateZ()));
		};
		this.addEventHandler(TransformChangedEvent.TRANSFORM_CHANGED, eh);
		
		menu = new ContextMenu();
		MenuItem delete = new MenuItem("Remove");
		delete.setOnAction(e -> {
			cleanup();
		});
		menu.getItems().add(delete);
	
	}
	
	private void cleanup() {
		V3DMolecule fxmol = (V3DMolecule) this.getParent();
		fxmol.getPharmacophore().getMolVol().getExclusionGaussians().remove(eg);
		fxmol.getChildren().remove(this);
		
	}
	
	public void showMenu(double x, double y) {

		menu.show(this, x, y);
        }

}
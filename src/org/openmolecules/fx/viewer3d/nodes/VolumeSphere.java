package org.openmolecules.fx.viewer3d.nodes;

import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DScene;
import org.openmolecules.render.PharmacophoreBuilder;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.PeriodicTable;
import com.actelion.research.chem.phesa.VolumeGaussian;
import com.actelion.research.chem.phesa.Gaussian3D;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Sphere;

public class VolumeSphere extends Group  {
	private static final double RADIUS_SCALING = 0.5;
	public static PhongMaterial sMaterialExcl;
	public static PhongMaterial sMaterialIncl;
	public VolumeGaussian volGauss;
	private ContextMenu menu;
	private IcosahedronMesh icosahedron;
	private Sphere sphere;
	private DoubleProperty sphereRadius;
	private PhongMaterial material;
	private int role;
	private Property<Coordinates> translate;
	static {
	sMaterialExcl = new PhongMaterial();
	sMaterialExcl.setSpecularColor(new Color(1.0,1.0,1.0,0.1));
	sMaterialExcl.setDiffuseColor(new Color(1.0,1.0,1.0,0.1).darker());
	sMaterialIncl = new PhongMaterial();
	sMaterialIncl.setSpecularColor(new Color(0.0,1.0,0.0,0.1));
	sMaterialIncl.setDiffuseColor(new Color(0.0,1.0,0.0,0.1).darker());
	}
	
	public VolumeSphere (VolumeGaussian volGauss, int role) {
		sphereRadius = new SimpleDoubleProperty(PeriodicTable.getElement(volGauss.getAtomicNo()).getVDWRadius());
		
		translate = new SimpleObjectProperty<Coordinates>(new Coordinates(volGauss.getCenter()));
		
		sphereRadius.addListener((o,ov,nv) -> {
			Platform.runLater(() -> {
			getChildren().remove(icosahedron);
			getChildren().remove(sphere);
			construct(); });
		});
		
		
		this.role = role;
		this.volGauss = volGauss;
		translate.addListener((o,ov,nv) -> {
			Coordinates diff = nv.subC(ov);
			volGauss.addShift(diff);
			
		});
		
		
		construct();
		
		menu = new ContextMenu();
		MenuItem delete = new MenuItem("Remove");
		delete.setOnAction(e -> {
			cleanup();
		});
		menu.getItems().add(delete);
		
		Menu menuVol = new Menu("Volume");
		
		ToggleGroup group = new ToggleGroup();
		
		RadioMenuItem f = new RadioMenuItem("F");
		f.setSelected(volGauss.getAtomicNo()==9);
		f.setOnAction(e -> {
			volGauss.setAtomicNo(9);
			sphereRadius.set(volGauss.getWidth());
		});
		f.setToggleGroup(group);
		
		RadioMenuItem c = new RadioMenuItem("C");
		c.setSelected(volGauss.getAtomicNo()==6);
		c.setOnAction(e -> {
			volGauss.setAtomicNo(6);
			sphereRadius.set(PeriodicTable.getElement(volGauss.getAtomicNo()).getVDWRadius());
		});
		
		c.setToggleGroup(group);
		
		RadioMenuItem cl = new RadioMenuItem("Cl");
		cl.setSelected(volGauss.getAtomicNo()==17);
		cl.setOnAction(e -> {
			volGauss.setAtomicNo(17);
			sphereRadius.set(PeriodicTable.getElement(volGauss.getAtomicNo()).getVDWRadius());
		});
		
		cl.setToggleGroup(group);
		
		RadioMenuItem br = new RadioMenuItem("Br");
		br.setSelected(volGauss.getAtomicNo()==35);
		br.setOnAction(e -> {
			volGauss.setAtomicNo(35);
			sphereRadius.set(PeriodicTable.getElement(volGauss.getAtomicNo()).getVDWRadius());
		});
		
		br.setToggleGroup(group);
		
		menuVol.getItems().addAll(f,c,cl,br);
		menu.getItems().add(menuVol);
		
		
	
	}
	
	public void addTranslate(double x, double y, double z) {
		Coordinates tr = translate.getValue();
		Coordinates newCoords = new Coordinates(tr);
		newCoords.x += x;
		newCoords.y += y;
		newCoords.z += z;
		sphere.setTranslateX(newCoords.x);
		sphere.setTranslateY(newCoords.y);
		sphere.setTranslateZ(newCoords.z);
		
		icosahedron.setTranslateX(newCoords.x);
		icosahedron.setTranslateY(newCoords.y);
		icosahedron.setTranslateZ(newCoords.z);
		
		translate.setValue(newCoords);
		
	}
		
	private void construct() {
		icosahedron = new IcosahedronMesh((float)(sphereRadius.get()*RADIUS_SCALING),3);
		icosahedron.setDrawMode(DrawMode.FILL);
		if(volGauss.getRole() == VolumeGaussian.INCLUSION)
			material = sMaterialIncl;
		if(volGauss.getRole() == VolumeGaussian.EXCLUSION)
			material = sMaterialExcl;
		icosahedron.setDrawMode(DrawMode.LINE);
		icosahedron.setMaterial(material);
		Coordinates tr = translate.getValue();
		
		sphere = new Sphere(RADIUS_SCALING*0.5*sphereRadius.get(),20);
		sphere.setMaterial(material);
		sphere.setTranslateX(tr.x);
		sphere.setTranslateY(tr.y);
		sphere.setTranslateZ(tr.z);
		icosahedron.setTranslateX(tr.x);
		icosahedron.setTranslateY(tr.y);
		icosahedron.setTranslateZ(tr.z);
		sphere.setCullFace(CullFace.NONE);

		getChildren().add(icosahedron);
		getChildren().add(sphere);
		sphere.setUserData(new NodeDetail(material, role , false));
	}
	
	private void cleanup() {
		getChildren().remove(icosahedron);
		getChildren().remove(sphere);
		V3DMolecule fxmol = (V3DMolecule) this.getParent().getParent();
		fxmol.getPharmacophore().getMolVol().getVolumeGaussians().remove(volGauss);
		fxmol.getPharmacophore().getChildren().remove(this);
		
	}
	
	public Sphere getSphere() {
		return sphere;
	}
	
	public void showMenu(double x, double y) {

		menu.show(this, x, y);
        }

}

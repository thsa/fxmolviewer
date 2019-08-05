package org.openmolecules.fx.viewer3d;

import java.util.Optional;

import org.openmolecules.mesh.Cone;
import org.openmolecules.render.PharmacophoreBuilder;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.phesa.PPGaussian;

import javafx.geometry.Orientation;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TextInputDialog;

public class SphereWith3DArrow extends Group {
	
	private Sphere sphere;
	private Cylinder cylinder;
	private double arrowLength;
	private double sphereRadius;
	private Cone cone;
	private PhongMaterial material;
	private static final double CONE_HEIGHT_RATIO = 0.2;
	private static final double CONE_WIDTH_RATIO = 2.0;
	private static final Coordinates YAXIS = new Coordinates(0.0,1.0,0.0);
	private ContextMenu menu;
	private PPGaussian ppg;
	private int role;
	
	public SphereWith3DArrow(PPGaussian ppg,PhongMaterial material, int role) {
		setUserData(new NodeDetail(material, role , false));
		TextInputDialog dialog = new TextInputDialog(Double.toString(ppg.getWeight()));
		dialog.setTitle("Weight");
		dialog.setHeaderText("Adjust Pharmacophore Weight");
		dialog.setContentText("Enter value between 0.1 to 10.0");
		this.ppg = ppg;
		this.material = material;
		this.role = role;

		
		construct();
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
				if(weightFactor<0.1)
					weightFactor = 0.1;
				if(weightFactor>10.0) 
					weightFactor = 10.0;
				ppg.setWeight(weightFactor);
				getChildren().remove(cylinder);
				getChildren().remove(cone);
				getChildren().remove(sphere);
				construct();
			}
		});
		menu.getItems().add(delete);
		menu.getItems().add(changeWeight);
	}
	
	private void cleanup() {
		V3DMolecule fxmol = (V3DMolecule) this.getParent();
		fxmol.getPharmacophore().getMolVol().getPPGaussians().remove(ppg);
		fxmol.getChildren().remove(this);
	}
	

	private double getScalingFactor() {
		double scalingFactor = 1.0;
		if(ppg.getWeight()>=1.0)
			scalingFactor = 1 + (ppg.getWeight()-1.0)*(1.0/9.0);
			
		else
			scalingFactor = 1 + (ppg.getWeight()-1.0)*(10.0/18.0);
		
		return scalingFactor;
	}
	
	
	private void construct() {
		double scalingFactor = getScalingFactor();
		arrowLength = PharmacophoreBuilder.VECTOR_LENGTH*scalingFactor;
		sphereRadius = PharmacophoreBuilder.PP_RADIUS*scalingFactor;
		double cylinderRadius = PharmacophoreBuilder.CYLINDER_RADIUS*scalingFactor;
		Coordinates sphereCenter = ppg.getPharmacophorePoint().getCenter();
		Coordinates endPoint = sphereCenter.addC(ppg.getPharmacophorePoint().getDirectionality().scaleC(arrowLength));
		sphere = new Sphere(sphereRadius);
		sphere.setMaterial(material);
		updateSphere(sphereCenter);
		Coordinates arrowCenter = sphereCenter.addC(endPoint.subC(sphereCenter).unitC().scaleC(sphereRadius-0.05));
		constructArrow(arrowCenter,endPoint, cylinderRadius);
		cylinder.setUserData(new NodeDetail(material, role , false));
		sphere.setUserData(new NodeDetail(material, role , false));
		cone.setUserData(new NodeDetail(material, role , false));
		getChildren().add(cylinder);
		getChildren().add(sphere);
		getChildren().add(cone);
	}
	
	private void updateSphere(Coordinates p1) {
		sphere.setTranslateX(p1.x);
		sphere.setTranslateY(p1.y);
		sphere.setTranslateZ(p1.z);
	}

	
	
	private void constructArrow(Coordinates p1, Coordinates p2, double r) {
		Coordinates delta = new Coordinates();
		delta.set(p2).sub(p1);
		double d = delta.getLength();
		cylinder = new Cylinder(r, d, 10);
		cylinder.setMaterial(material);
		double coneHeight = d*CONE_HEIGHT_RATIO;
		cone = new Cone(CONE_WIDTH_RATIO*r,coneHeight);
		updateNode(cylinder,delta, p1, p2, d);
		Coordinates c1 = p2;
		Coordinates c2 = c1.addC(delta.unitC().scaleC(coneHeight));
		delta.set(c2).sub(c1);
		updateNode(cone,delta,c1,c2,delta.getLength());
		
	}
	

	
	private void updateNode(Shape3D shape,Coordinates delta, Coordinates p1, Coordinates p2, double d) {
		Coordinates center = new Coordinates();
		center.center(p1,p2);

	    
	    Coordinates axisOfRotation = delta.cross(YAXIS);
	    double angle = Math.acos(delta.unitC().dot(YAXIS));
	    Rotate rotateAroundCenter = new Rotate(-Math.toDegrees(angle), new Point3D(axisOfRotation.x,
	    		axisOfRotation.y,axisOfRotation.z));
	    
		shape.setMaterial(material);
		shape.setTranslateX(center.x);
		shape.setTranslateY(center.y);
		shape.setTranslateZ(center.z);
		shape.getTransforms().clear();
		shape.getTransforms().add(rotateAroundCenter);

	}
	
	public void update(PPGaussian ppg) {
		Coordinates sphereCenter = ppg.getPharmacophorePoint().getCenter();
		Coordinates endPoint = sphereCenter.addC(ppg.getPharmacophorePoint().getDirectionality().scaleC(arrowLength));
		updateSphere(sphereCenter);
		Coordinates delta = new Coordinates();
		delta.set(endPoint).sub(sphereCenter);
		double d = delta.getLength();
		Coordinates arrowCenter = sphereCenter.addC(endPoint.subC(sphereCenter).unitC().scaleC(sphereRadius-0.05));
		updateNode(cylinder,delta,arrowCenter,endPoint,d);
		double coneHeight = d*CONE_HEIGHT_RATIO;
		Coordinates c1 = endPoint;
		Coordinates c2 = c1.addC(delta.unitC().scaleC(coneHeight));
		delta.set(c2).sub(c1);
		updateNode(cone,delta,c1,c2,delta.getLength());
	}
	
	public Cylinder getCylinder() {
		return this.cylinder;
	}
	
	public Sphere getSphere() {
		return this.sphere;
	}
	
	public void showMenu(double x, double y) {

		menu.show(this, x, y);
        }
	

	
	

}

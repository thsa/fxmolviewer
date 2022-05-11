package org.openmolecules.fx.viewer3d.nodes;

import org.openmolecules.fx.viewer3d.V3DBindingSiteVolume;
import org.openmolecules.fx.viewer3d.V3DCustomizablePheSA;
import org.openmolecules.render.MoleculeBuilder;
import org.openmolecules.render.PharmacophoreBuilder;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.phesa.pharmacophore.pp.PPGaussian;

import javafx.scene.Group;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;

public class SimplePPSphere extends Group implements IPPNode{
	
	private Sphere sphere;
	private PhongMaterial material;
	private double sphereRadius;
	private PPGaussian ppg;
	
	public SimplePPSphere(PPGaussian ppg, PhongMaterial material) {
		this.material = material;
		this.ppg = ppg;
		construct();
	}

	@Override
	public void showMenu(double x, double y) {
		return;
		
	}
	
	public void updateSphere(Coordinates c) {
		sphere.setTranslateX(c.x);
		sphere.setTranslateY(c.y);
		sphere.setTranslateZ(c.z);
	}

	@Override
	public void construct() {
		double scalingFactor = getScalingFactor();
		sphereRadius = PharmacophoreBuilder.PP_RADIUS*scalingFactor;
		Coordinates sphereCenter = ppg.getCenter();
		sphere = new Sphere(sphereRadius,20);
		sphere.setMaterial(material);
		updateSphere(sphereCenter);
		sphere.setUserData(new NodeDetail(material, MoleculeBuilder.ROLE_IS_PHARMACOPHORE , false));
		getChildren().add(sphere);
		
	}

	@Override
	public void cleanup() {
		V3DBindingSiteVolume parentPharmacophore = (V3DBindingSiteVolume) this.getParent();
		parentPharmacophore.getShapeVolume().getPPGaussians().remove(ppg);
		parentPharmacophore.getChildren().remove(this);
		
	}

	@Override
	public void update() {
		Coordinates sphereCenter = ppg.getCenter();
		updateSphere(sphereCenter);
	}

	@Override
	public double getScalingFactor() {
		double scalingFactor = 1.0;
		if(ppg.getWeight()>=1.0)
			scalingFactor = 1 + (ppg.getWeight()-1.0)*(1.0/9.0);
			
		else
			scalingFactor = 1 + (ppg.getWeight()-1.0)*(10.0/18.0);
		
		return scalingFactor;
	}

}

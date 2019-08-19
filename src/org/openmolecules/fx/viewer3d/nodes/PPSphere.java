package org.openmolecules.fx.viewer3d.nodes;


import org.openmolecules.render.PharmacophoreBuilder;
import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.phesa.pharmacophore.PPGaussian;

import javafx.scene.effect.Reflection;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Sphere;


public class PPSphere extends AbstractPPNode{
	
	protected Sphere sphere;
	protected double sphereRadius;
	protected IcosahedronMesh icosahedron;
	protected PhongMaterial icoMaterial;

	
	public PPSphere(PPGaussian ppg,PhongMaterial material, PhongMaterial icoMaterial, int role) {
		super(ppg,material,role);		
		this.icoMaterial = icoMaterial;
		construct();
	}
	

	@Override
	public void construct() {
		double scalingFactor = getScalingFactor();
		sphereRadius = PharmacophoreBuilder.PP_RADIUS_CHARGE*scalingFactor;
		icosahedron = new IcosahedronMesh((float)sphereRadius,3);
		icosahedron.setDrawMode(DrawMode.LINE);
		icosahedron.setMaterial(icoMaterial);
		Coordinates sphereCenter = ppg.getPharmacophorePoint().getCenter();
		sphere = new Sphere(sphereRadius,20);
		sphere.setMaterial(material);
		icosahedron.setCullFace(CullFace.NONE);

		updateSphere(sphereCenter);
		sphere.setUserData(new NodeDetail(material, role , false));
		getChildren().add(sphere);
		getChildren().add(icosahedron);

	}
	
	public void updateSphere(Coordinates p1) {
		sphere.setTranslateX(p1.x);
		sphere.setTranslateY(p1.y);
		sphere.setTranslateZ(p1.z);
		icosahedron.setTranslateX(p1.x);
		icosahedron.setTranslateY(p1.y);
		icosahedron.setTranslateZ(p1.z);
		
	}

	
	
	

	@Override
	public void update() {
		Coordinates sphereCenter = ppg.getPharmacophorePoint().getCenter();
		updateSphere(sphereCenter);
		
	}
	

	public Sphere getSphere() {
		return this.sphere;
	}
	




	
	

}

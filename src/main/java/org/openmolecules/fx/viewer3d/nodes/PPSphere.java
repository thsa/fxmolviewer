package org.openmolecules.fx.viewer3d.nodes;


import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.phesa.Gaussian3D;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Sphere;
import org.openmolecules.render.PharmacophoreBuilder;
import org.openmolecules.render.RoleHelper;


public class PPSphere extends AbstractPPNode{
	
	protected Sphere sphere;
	protected double sphereRadius;
	protected IcosahedronMesh icosahedron;
	protected PhongMaterial icoMaterial;

	
	public PPSphere(Gaussian3D ppg,PhongMaterial material, PhongMaterial icoMaterial) {
		super(ppg, material, RoleHelper.createPharmacophoreRole());
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
		Coordinates sphereCenter = ppg.getCenter();
		sphere = new Sphere(sphereRadius,20);
		sphere.setMaterial(material);
		icosahedron.setCullFace(CullFace.NONE);

		updateSphere(sphereCenter);
		sphere.setUserData(new NodeDetail(material, RoleHelper.createPharmacophoreRole(), false));
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
		Coordinates sphereCenter = ppg.getCenter();
		updateSphere(sphereCenter);
		
	}
	

	public Sphere getSphere() {
		return this.sphere;
	}
	




	
	

}

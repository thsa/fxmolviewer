package org.openmolecules.fx.viewer3d.nodes;



import org.openmolecules.mesh.Cone;
import org.openmolecules.render.MoleculeBuilder;
import org.openmolecules.render.PharmacophoreBuilder;
import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.phesa.pharmacophore.PPGaussian;
import javafx.geometry.Point3D;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;


public class PPArrow extends AbstractPPNode {
	
	private Cylinder cylinder;
	private double arrowLength;
	private Cone cone;
	private static final double CONE_HEIGHT_RATIO = 0.2;
	private static final double CONE_WIDTH_RATIO = 2.0;
	private static final Coordinates YAXIS = new Coordinates(0.0,1.0,0.0);
	
	public PPArrow(PPGaussian ppg,PhongMaterial material) {
		super(ppg,material,MoleculeBuilder.ROLE_IS_PHARMACOPHORE);
		construct();
	}
	
	@Override
	public void construct() {
		double scalingFactor = getScalingFactor();
		arrowLength = PharmacophoreBuilder.VECTOR_LENGTH*scalingFactor;
		double cylinderRadius = PharmacophoreBuilder.CYLINDER_RADIUS*scalingFactor;
		Coordinates center = ppg.getPharmacophorePoint().getCenter();
		Coordinates arrowCenter = center.addC(ppg.getPharmacophorePoint().getDirectionality().scaleC(0.5));
		Coordinates endPoint = arrowCenter.addC(ppg.getPharmacophorePoint().getDirectionality().scaleC(arrowLength));
		constructArrow(arrowCenter,endPoint, cylinderRadius);
		cylinder.setUserData(new NodeDetail(material, MoleculeBuilder.ROLE_IS_PHARMACOPHORE , false));
		cone.setUserData(new NodeDetail(material, MoleculeBuilder.ROLE_IS_PHARMACOPHORE , false));
		getChildren().add(cylinder);
		getChildren().add(cone);
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
	
	@Override
	public void update() {
		Coordinates center = ppg.getPharmacophorePoint().getCenter();
		Coordinates arrowCenter = center.addC(ppg.getPharmacophorePoint().getDirectionality().scaleC(0.5));
		Coordinates endPoint = arrowCenter.addC(ppg.getPharmacophorePoint().getDirectionality().scaleC(arrowLength));
		Coordinates delta = new Coordinates();
		delta.set(endPoint).sub(arrowCenter);
		double d = delta.getLength();
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


	

	

	
	

}

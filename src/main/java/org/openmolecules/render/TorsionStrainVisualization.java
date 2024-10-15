package org.openmolecules.render;

import java.util.ArrayList;
import java.util.List;

import org.openmolecules.fx.viewer3d.nodes.NodeDetail;
import org.openmolecules.fx.viewer3d.torsionstrain.V3DTorsionStrainAnalyzer;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.conf.TorsionDB;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;


public class TorsionStrainVisualization implements TorsionStrainVisBuilder {
	private V3DTorsionStrainAnalyzer analyzer;
	private TorsionStrainVisArchitect architect = new TorsionStrainVisArchitect(this);
	private static final  int CYLINDER_DIVISIONS = 10;
	public static final double LENGTH_SCALE = 0.4;
	private static PhongMaterial sGreenTorsionMat;
	private static PhongMaterial sOrangeTorsionMat;
	private static PhongMaterial sVioletTorsionMat;
	private static PhongMaterial sGreyTorsionMat;
	private static BooleanProperty isVisible;
	
	static {
		sGreenTorsionMat = new PhongMaterial();
		Color greenColor = Color.DARKGREEN;
		sGreenTorsionMat.setSpecularColor(new Color(greenColor.getRed(), greenColor.getGreen(), greenColor.getBlue(),0.8));
		sGreenTorsionMat.setDiffuseColor(new Color(greenColor.getRed(), greenColor.getGreen(), greenColor.getBlue(),0.8).darker());
		
		sOrangeTorsionMat = new PhongMaterial();
		Color orangeColor = Color.ORANGE;
		sOrangeTorsionMat.setSpecularColor(new Color(orangeColor.getRed(), orangeColor.getGreen(), orangeColor.getBlue(),0.8));
		sOrangeTorsionMat.setDiffuseColor(new Color(orangeColor.getRed(), orangeColor.getGreen(), orangeColor.getBlue(),0.8).darker());
		
		sVioletTorsionMat = new PhongMaterial();
		Color violetColor = Color.DARKVIOLET;
		sVioletTorsionMat.setSpecularColor(new Color(violetColor.getRed(), violetColor.getGreen(), violetColor.getBlue(),0.8));
		sVioletTorsionMat.setDiffuseColor(new Color(violetColor.getRed(), violetColor.getGreen(), violetColor.getBlue(),0.8).darker());
		
		sGreyTorsionMat = new PhongMaterial();
		Color greyColor = Color.LIGHTGRAY;
		sGreyTorsionMat.setSpecularColor(new Color(greyColor.getRed(), greyColor.getGreen(), greyColor.getBlue(),0.8));
		sGreyTorsionMat.setDiffuseColor(new Color(greyColor.getRed(), greyColor.getGreen(), greyColor.getBlue(),0.8).darker());
	}
	
	
	
	public TorsionStrainVisualization(V3DTorsionStrainAnalyzer analyzer) {
		this.analyzer = analyzer;
		isVisible = new SimpleBooleanProperty();
		isVisible.set(true);
	}
	
	public void build() {
		architect.buildTorsionStrainColors(analyzer);
	}
	
	public V3DTorsionStrainAnalyzer getTorsionAnalyzer() {
		return analyzer;
	}
	
	public static PhongMaterial getStrainMaterial(int strain) {
		PhongMaterial material;
		switch(strain) {
		case TorsionDB.TORSION_GREEN:
			material = sGreenTorsionMat;
			break;
		case TorsionDB.TORSION_YELLOW:
			material = sOrangeTorsionMat;
			break;
		case TorsionDB.TORSION_RED:
			material = sVioletTorsionMat;
			break;
		default: 
			material = sGreyTorsionMat;
			break;
		}
		return material;
			
	}

	@Override
	public void addTorsionCylinder(int role, double radius, double length, Coordinates center, double rotationY, double rotationZ, int strain) {
		boolean isOverridable = false;
		PhongMaterial material = getStrainMaterial(strain);
		length*=LENGTH_SCALE;
		Cylinder cylinder = new Cylinder(radius, length, CYLINDER_DIVISIONS);
		cylinder.setMaterial(material);
		cylinder.setTranslateX(center.x);
		cylinder.setTranslateY(center.y);
		cylinder.setTranslateZ(center.z);

		Transform r1 = new Rotate(90+180/Math.PI*rotationY, Rotate.X_AXIS);
		Transform r2 = new Rotate(90+180/Math.PI*rotationZ, Rotate.Z_AXIS);
		cylinder.getTransforms().add(r2);
		cylinder.getTransforms().add(r1);
		cylinder.setUserData(new NodeDetail(material, role, isOverridable));
		analyzer.getV3DMolecule().getChildren().add(cylinder);
		
	}
	
	public void toggleVisibility() {
		boolean isVis = isVisible.get();
		isVisible.set(!isVis);
		for (Node node : analyzer.getV3DMolecule().getChildren()) {
			NodeDetail detail = (NodeDetail)node.getUserData();
			if(detail != null && detail.isTorsion())
				node.setVisible(isVisible.get());
		}
	}
	
	public boolean isVisible() {
		return isVisible.get();
	}
	
	public void cleanup() {
		List<Node> toBeRemoved = new ArrayList<Node>();
		for (Node node:analyzer.getV3DMolecule().getChildren()) {
			NodeDetail detail = (NodeDetail)node.getUserData();
			if (detail != null) {
				if (detail.isPharmacophore()) {
					toBeRemoved.add(node);
				}
				else if (detail.isExclusion()) {
					toBeRemoved.add(node);
				}
			}
		}
		Platform.runLater(() -> {
			analyzer.getV3DMolecule().getChildren().removeAll(toBeRemoved);
		});
		
	}
	


}

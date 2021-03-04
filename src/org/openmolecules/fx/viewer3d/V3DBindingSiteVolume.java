package org.openmolecules.fx.viewer3d;

import org.openmolecules.fx.viewer3d.nodes.FXColorHelper;
import org.openmolecules.fx.viewer3d.nodes.PPSphere;
import org.openmolecules.render.BindingSiteVolumeArchitect;
import org.openmolecules.render.BindingSiteVolumeBuilder;

import com.actelion.research.chem.phesa.AtomicGaussian;
import com.actelion.research.chem.phesa.BindingSiteVolume;
import com.actelion.research.chem.phesa.pharmacophore.pp.IPharmacophorePoint.Functionality;
import com.actelion.research.chem.phesa.pharmacophore.pp.PPGaussian;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

public class V3DBindingSiteVolume extends V3DMolGroup implements BindingSiteVolumeBuilder {
	
	private BindingSiteVolume bsVol;
	
	public static PhongMaterial sShapeMaterial;
	public static PhongMaterial sShapeMaterialFrame;
	public static PhongMaterial sDonorMaterial;
	public static PhongMaterial sDonorMaterialFrame;
	public static PhongMaterial sAcceptorMaterial;
	public static PhongMaterial sAcceptorMaterialFrame;
	public static PhongMaterial sPosChargeMaterial;
	public static PhongMaterial sPosChargeMaterialFrame;
	public static PhongMaterial sNegChargeMaterial;
	public static PhongMaterial sNegChargeMaterialFrame;
	
	static {
		
		sShapeMaterial = new PhongMaterial();
		sShapeMaterial .setSpecularColor(FXColorHelper.changeOpacity(Color.GREY, 0.1));
		sShapeMaterial .setDiffuseColor(FXColorHelper.changeOpacity(Color.GREY, 0.1).darker());
		
		sShapeMaterialFrame = new PhongMaterial();
		sShapeMaterialFrame .setSpecularColor(FXColorHelper.changeOpacity(Color.GREY, 0.5));
		sShapeMaterialFrame .setDiffuseColor(FXColorHelper.changeOpacity(Color.GREY, 0.5).darker());
		
		sAcceptorMaterialFrame = new PhongMaterial();
		sAcceptorMaterialFrame.setSpecularColor(Color.TRANSPARENT);
		sAcceptorMaterialFrame.setDiffuseColor(Color.TRANSPARENT);
		
		sDonorMaterial = new PhongMaterial();
		sDonorMaterial .setSpecularColor(FXColorHelper.changeOpacity(Color.ROYALBLUE, 0.5));
		sDonorMaterial .setDiffuseColor(FXColorHelper.changeOpacity(Color.ROYALBLUE, 0.5).darker());
		
		sDonorMaterialFrame = new PhongMaterial();
		sDonorMaterialFrame.setSpecularColor(Color.TRANSPARENT);
		sDonorMaterialFrame.setDiffuseColor(Color.TRANSPARENT);
		
		sAcceptorMaterial = new PhongMaterial();
		sAcceptorMaterial .setSpecularColor(FXColorHelper.changeOpacity(Color.CRIMSON, 0.5));
		sAcceptorMaterial .setDiffuseColor(FXColorHelper.changeOpacity(Color.CRIMSON, 0.5).darker());
		
		sAcceptorMaterialFrame = new PhongMaterial();
		sAcceptorMaterialFrame.setSpecularColor(Color.TRANSPARENT);
		sAcceptorMaterialFrame.setDiffuseColor(Color.TRANSPARENT);
		
		sNegChargeMaterial = new PhongMaterial();
		sNegChargeMaterial .setSpecularColor(FXColorHelper.changeOpacity(Color.BLUEVIOLET, 0.001));
		sNegChargeMaterial .setDiffuseColor(FXColorHelper.changeOpacity(Color.BLUEVIOLET, 0.001).darker());
		
		sNegChargeMaterialFrame = new PhongMaterial();
		sNegChargeMaterialFrame.setSpecularColor(FXColorHelper.changeOpacity(Color.BLUEVIOLET, 0.5));
		sNegChargeMaterialFrame.setDiffuseColor(FXColorHelper.changeOpacity(Color.BLUEVIOLET, 0.5));
		
		sPosChargeMaterial = new PhongMaterial();
		sPosChargeMaterial .setSpecularColor(FXColorHelper.changeOpacity(Color.DARKTURQUOISE,0.001));
		sPosChargeMaterial .setDiffuseColor(FXColorHelper.changeOpacity(Color.DARKTURQUOISE,0.001));
		
		sPosChargeMaterialFrame = new PhongMaterial();
		sPosChargeMaterialFrame.setSpecularColor(FXColorHelper.changeOpacity(Color.DARKTURQUOISE, 0.5));
		sPosChargeMaterialFrame.setDiffuseColor(FXColorHelper.changeOpacity(Color.DARKTURQUOISE, 0.5));

	}

	public V3DBindingSiteVolume(BindingSiteVolume bsVol) {
		super("Binding Site Volume");
		this.bsVol = bsVol;
		
	}

	@Override
	public void addSimplePharmacophorePoint(PPGaussian pp) {
		PhongMaterial material;
		PhongMaterial frameMaterial;
		int f = pp.getPharmacophorePoint().getFunctionalityIndex();
		if(f==Functionality.DONOR.getIndex()) {
			material = sDonorMaterial;
			frameMaterial = sDonorMaterialFrame;
			PPSphere ppNode = new PPSphere(pp, material, frameMaterial); 
			getChildren().add(ppNode);}
		else if(f==Functionality.ACCEPTOR.getIndex()) {
			material = sAcceptorMaterial;
			frameMaterial = sAcceptorMaterialFrame;
			PPSphere ppNode = new PPSphere(pp, material, frameMaterial); 
			getChildren().add(ppNode);}
		else if (f==Functionality.NEG_CHARGE.getIndex()) {
			material = sNegChargeMaterial;
			frameMaterial = sNegChargeMaterialFrame;
			PPSphere ppNode = new PPSphere(pp, material, frameMaterial); 
			getChildren().add(ppNode);
			}
		
		else if(f==Functionality.POS_CHARGE.getIndex()){
			material = sPosChargeMaterial;
			frameMaterial = sPosChargeMaterialFrame;
			PPSphere ppNode = new PPSphere(pp, material, frameMaterial); 
			getChildren().add(ppNode);
		}		
	}
	
	public void buildVolume() {
		BindingSiteVolumeArchitect architect = new BindingSiteVolumeArchitect(this);
		architect.buildPharmacophore(bsVol);
		}
	
	
	public BindingSiteVolume getBindingSiteVolume() {
		return bsVol;
	}

	@Override
	public void addShapePoint(AtomicGaussian ag) {

		PhongMaterial material = sShapeMaterial;
		PhongMaterial frameMaterial = sShapeMaterialFrame;
		PPSphere agNode = new PPSphere(ag, material, frameMaterial); 
		getChildren().add(agNode);
		
	}

}

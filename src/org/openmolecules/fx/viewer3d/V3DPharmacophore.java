package org.openmolecules.fx.viewer3d;

import java.util.ArrayList;

import org.openmolecules.fx.viewer3d.nodes.ExclusionSphere;
import org.openmolecules.fx.viewer3d.nodes.FXColorHelper;
import org.openmolecules.fx.viewer3d.nodes.NodeDetail;
import org.openmolecules.fx.viewer3d.nodes.PPArrow;
import org.openmolecules.fx.viewer3d.nodes.PPSphere;
import org.openmolecules.render.MoleculeBuilder;
import org.openmolecules.render.PharmacophoreArchitect;
import org.openmolecules.render.PharmacophoreBuilder;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.phesa.ExclusionGaussian;
import com.actelion.research.chem.phesa.Gaussian3D;
import com.actelion.research.chem.phesa.MolecularVolume;
import com.actelion.research.chem.phesa.pharmacophore.AcceptorPoint;
import com.actelion.research.chem.phesa.pharmacophore.ChargePoint;
import com.actelion.research.chem.phesa.pharmacophore.DonorPoint;
import com.actelion.research.chem.phesa.pharmacophore.IPharmacophorePoint;
import com.actelion.research.chem.phesa.pharmacophore.PPGaussian;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

import java.util.Random;



public class V3DPharmacophore extends Group implements MolCoordinatesChangeListener, PharmacophoreBuilder{
	
	public static PhongMaterial sDonorMaterial;
	public static PhongMaterial sAcceptorMaterial;
	public static PhongMaterial sPosChargeMaterial;
	public static PhongMaterial sPosChargeMaterialFrame;
	public static PhongMaterial sNegChargeMaterial;
	public static PhongMaterial sNegChargeMaterialFrame;
	public static Color crimson;
	

	
	private MolecularVolume mMolVol;
	private V3DMolecule mFXMol;
	
	public V3DPharmacophore(V3DMolecule fxMol) {
		this(fxMol,new MolecularVolume(fxMol.getMolecule()));
	}
	
	public V3DPharmacophore(V3DMolecule fxMol, MolecularVolume molVol) {
		fxMol.addMoleculeCoordinatesChangeListener(this);
		this.mFXMol = fxMol;
		mMolVol = molVol;	
		sAcceptorMaterial = new PhongMaterial();
		sAcceptorMaterial.setSpecularColor(new Color(1.0,0.2,0.2,0.01));
		sAcceptorMaterial.setDiffuseColor(new Color(1.0,0.2,0.2,0.01).darker());
		sDonorMaterial = new PhongMaterial();
		sDonorMaterial.setSpecularColor(new Color(0.2,0.2,1.0,0.01));
		sDonorMaterial.setDiffuseColor(new Color(0.2,0.2,1.0,0.01).darker());
		sNegChargeMaterial = new PhongMaterial();
		sNegChargeMaterial .setSpecularColor(FXColorHelper.changeOpacity(Color.CRIMSON, 0.001));
		sNegChargeMaterial .setDiffuseColor(FXColorHelper.changeOpacity(Color.CRIMSON, 0.001).darker());
		sPosChargeMaterial = new PhongMaterial();
		sPosChargeMaterial .setSpecularColor(FXColorHelper.changeOpacity(Color.ROYALBLUE,0.001));
		sPosChargeMaterial .setDiffuseColor(FXColorHelper.changeOpacity(Color.ROYALBLUE,0.001));
		sNegChargeMaterialFrame = new PhongMaterial();
		sNegChargeMaterialFrame.setSpecularColor(FXColorHelper.changeOpacity(Color.CRIMSON, 0.5));
		sNegChargeMaterialFrame.setDiffuseColor(FXColorHelper.changeOpacity(Color.CRIMSON, 0.5));
		sPosChargeMaterialFrame = new PhongMaterial();
		sPosChargeMaterialFrame.setSpecularColor(FXColorHelper.changeOpacity(Color.ROYALBLUE, 0.5));
		sPosChargeMaterialFrame.setDiffuseColor(FXColorHelper.changeOpacity(Color.ROYALBLUE, 0.5));
	
	}

	@Override
	public void coordinatesChanged() {
		/*TODO
		 * add hydrogens?
		 */
		mMolVol.update(mFXMol.getMolecule());
		mMolVol.updateCOM();
			
	}
	

	

	
	public void buildPharmacophore() {
		PharmacophoreArchitect architect = new PharmacophoreArchitect(this);
		architect.buildPharmacophore(mMolVol, 0);
		}
	
	public MolecularVolume getMolVol() {
		return mMolVol;
	}
	
	public void cleanup() {
		this.mFXMol.removeMoleculeCoordinatesChangeListener(this);
		ArrayList<Node> toBeRemoved = new ArrayList<Node>();
		for (Node node:mFXMol.getChildren()) {
			int role = node.getUserData() == null ? 0 : ((NodeDetail)node.getUserData()).getRole();
			if ((role &  MoleculeBuilder.ROLE_IS_PHARMACOPHORE)!= 0) {
				toBeRemoved.add(node);
			}
		}
		Platform.runLater(() -> {getChildren().removeAll(toBeRemoved);
			this.mFXMol.getChildren().remove(this);
		});
		
	}
	

	@Override
	public void addPharmacophorePoint(int role, PPGaussian ppg) {
		IPharmacophorePoint pp = ppg.getPharmacophorePoint();
		PhongMaterial material;
		PhongMaterial frameMaterial;
		if(pp instanceof DonorPoint) {
			material = sDonorMaterial;
			PPArrow ppNode = new PPArrow(ppg, material, role); 
			getChildren().add(ppNode);}
		else if(pp instanceof AcceptorPoint) {
			material = sAcceptorMaterial;
			PPArrow ppNode = new PPArrow(ppg, material, role); 
			getChildren().add(ppNode);}
		else if (pp instanceof ChargePoint) {
			ChargePoint cp = (ChargePoint) pp;
			if(cp.getCharge()<0) {
				material = sNegChargeMaterial;
				frameMaterial = sNegChargeMaterialFrame;
			}
			else {
				material = sPosChargeMaterial;
				frameMaterial = sPosChargeMaterialFrame;
			}
			PPSphere ppNode = new PPSphere(ppg, material,frameMaterial, role);
			getChildren().add(ppNode);
		}		
	}

	@Override
	public void addExclusionSphere(int role, ExclusionGaussian eg) {
		ExclusionSphere es = new ExclusionSphere(eg, role);
		getChildren().add(es);
	}
	
	public void placeExclusionSphere() {
		Random random = new Random();
		int atom = random.nextInt(mFXMol.getMolecule().getAllAtoms());
		Coordinates shift = new Coordinates(3*(2*random.nextDouble()-1),3*(2*random.nextDouble()-1),3*(2*random.nextDouble()-1));
		ExclusionGaussian eg = new ExclusionGaussian(atom, 6, mFXMol.getMolecule().getCoordinates(atom), shift);
		mMolVol.getExclusionGaussians().add(eg);
		int role = PharmacophoreArchitect.exclusionRole(eg);
		addExclusionSphere(role,eg);
	}

}

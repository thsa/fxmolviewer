package org.openmolecules.fx.viewer3d;

import java.util.ArrayList;

import org.openmolecules.render.MoleculeBuilder;
import org.openmolecules.render.PharmacophoreArchitect;
import org.openmolecules.render.PharmacophoreBuilder;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.phesa.DonorPoint;
import com.actelion.research.chem.phesa.ExclusionGaussian;
import com.actelion.research.chem.phesa.AcceptorPoint;
import com.actelion.research.chem.phesa.Gaussian3D;
import com.actelion.research.chem.phesa.IPharmacophorePoint;
import com.actelion.research.chem.phesa.MolecularVolume;
import com.actelion.research.chem.phesa.PPGaussian;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;

import java.util.Random;



public class V3DPharmacophore implements MoleculeChangeListener, PharmacophoreBuilder{
	
	public static PhongMaterial sDonorMaterial;
	public static PhongMaterial sAcceptorMaterial;
	private int exclusionDetail;

	
	private MolecularVolume molVol;
	private V3DMolecule fxMol;
	
	public V3DPharmacophore(V3DMolecule fxMol) {
		fxMol.addMoleculeChangeListener(this);
		this.fxMol = fxMol;
		molVol = new MolecularVolume(fxMol.getMolecule());			
		sDonorMaterial = new PhongMaterial();
		sDonorMaterial.setSpecularColor(new Color(1.0,0.0,0.0,0.1));
		sDonorMaterial.setDiffuseColor(new Color(1.0,0.0,0.0,0.1).darker());
		sAcceptorMaterial = new PhongMaterial();
		sAcceptorMaterial.setSpecularColor(new Color(0.0,0.0,1.0,0.1));
		sAcceptorMaterial.setDiffuseColor(new Color(0.0,0.0,1.0,0.1).darker());		
		exclusionDetail = 3;
	
	}

	@Override
	public void coordinatesChanged() {
		updateCoordinates(molVol.getAtomicGaussians(),fxMol.getMolecule());
		updateCoordinates(molVol.getPPGaussians(),fxMol.getMolecule());
			
	}
	
	public void updateCoordinates(ArrayList<? extends Gaussian3D> gaussians, StereoMolecule mol) {
		for(Gaussian3D gaussian : gaussians) {
			gaussian.updateCoordinates(mol);
		}
		
	}
	

	
	public void buildPharmacophore() {
		PharmacophoreArchitect architect = new PharmacophoreArchitect(this);
		architect.buildPharmacophore(molVol, 0);
		}
	
	public MolecularVolume getMolVol() {
		return molVol;
	}
	
	public void cleanup() {
		this.fxMol.removeMoleculeChangeListener(this);
		ArrayList<Node> toBeRemoved = new ArrayList<Node>();
		for (Node node:fxMol.getChildren()) {
			int role = node.getUserData() == null ? 0 : ((NodeDetail)node.getUserData()).getRole();
			if ((role &  MoleculeBuilder.ROLE_IS_PHARMACOPHORE)!= 0) {
				toBeRemoved.add(node);
			}
		}
		Platform.runLater(() -> this.fxMol.getChildren().removeAll(toBeRemoved));
		
	}
	

	@Override
	public void addPharmacophorePoint(int role, PPGaussian ppg) {
		IPharmacophorePoint pp = ppg.getPharmacophorePoint();
		PhongMaterial material;
		if(pp instanceof DonorPoint)
			material = sDonorMaterial;
		else if(pp instanceof AcceptorPoint)
			material = sAcceptorMaterial;
		else 
			return;

		SphereWith3DArrow ppNode = new SphereWith3DArrow(ppg, material, role); 
		fxMol.getChildren().add(ppNode);
		
		
	}

	@Override
	public void addExclusionSphere(int role, ExclusionGaussian eg) {
		ExclusionSphere es = new ExclusionSphere(eg, role);
		fxMol.getChildren().add(es);
	}
	
	public void placeExclusionSphere() {
		Random random = new Random();
		int atom = random.nextInt(fxMol.getMolecule().getAllAtoms());
		Coordinates shift = new Coordinates(3*(2*random.nextDouble()-1),3*(2*random.nextDouble()-1),3*(2*random.nextDouble()-1));
		ExclusionGaussian eg = new ExclusionGaussian(atom, 6, fxMol.getMolecule().getCoordinates(atom), shift);
		molVol.getExclusionGaussians().add(eg);
		int role = (exclusionDetail++ << MoleculeBuilder.ROLE_DETAIL_SHIFT) |MoleculeBuilder.ROLE_IS_PHARMACOPHORE | atom;
		addExclusionSphere(role,eg);
	}


	
	
	
	
	
	

}

package org.openmolecules.fx.viewer3d;

import java.util.ArrayList;

import org.openmolecules.fx.viewer3d.nodes.VolumeSphere;
import org.openmolecules.fx.viewer3d.nodes.FXColorHelper;
import org.openmolecules.fx.viewer3d.nodes.PPArrow;
import org.openmolecules.fx.viewer3d.nodes.PPSphere;
import org.openmolecules.render.PharmacophoreArchitect;
import org.openmolecules.render.PharmacophoreBuilder;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.alignment3d.transformation.Rotation;
import com.actelion.research.chem.alignment3d.transformation.TransformationSequence;
import com.actelion.research.chem.alignment3d.transformation.Translation;
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.chem.conf.ConformerSet;
import com.actelion.research.chem.conf.ConformerSetGenerator;
import com.actelion.research.chem.phesa.VolumeGaussian;
import com.actelion.research.chem.phesa.MolecularVolume;
import com.actelion.research.chem.phesa.PheSAMolecule;
import com.actelion.research.chem.phesa.pharmacophore.pp.AcceptorPoint;
import com.actelion.research.chem.phesa.pharmacophore.pp.AromRingPoint;
import com.actelion.research.chem.phesa.pharmacophore.pp.ChargePoint;
import com.actelion.research.chem.phesa.pharmacophore.pp.DonorPoint;
import com.actelion.research.chem.phesa.pharmacophore.pp.IPharmacophorePoint;
import com.actelion.research.chem.phesa.pharmacophore.pp.PPGaussian;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

import java.util.Random;



public class V3DCustomizablePheSA extends V3DRotatableGroup implements MolCoordinatesChangeListener, PharmacophoreBuilder{
	
	public static PhongMaterial sDonorMaterial;
	public static PhongMaterial sAcceptorMaterial;
	public static PhongMaterial sPosChargeMaterial;
	public static PhongMaterial sPosChargeMaterialFrame;
	public static PhongMaterial sNegChargeMaterial;
	public static PhongMaterial sNegChargeMaterialFrame;
	public static PhongMaterial sAromRingMaterial;
	public static PhongMaterial sAromRingMaterialFrame;

	
	static {
		sAcceptorMaterial = new PhongMaterial();
		sAcceptorMaterial.setSpecularColor(new Color(1.0,0.2,0.2,0.01));
		sAcceptorMaterial.setDiffuseColor(new Color(1.0,0.2,0.2,0.01).darker());
		
		sDonorMaterial = new PhongMaterial();
		sDonorMaterial.setSpecularColor(new Color(0.2,0.2,1.0,0.01));
		sDonorMaterial.setDiffuseColor(new Color(0.2,0.2,1.0,0.01).darker());
		
		sNegChargeMaterial = new PhongMaterial();
		sNegChargeMaterial .setSpecularColor(FXColorHelper.changeOpacity(Color.BLUEVIOLET, 0.001));
		sNegChargeMaterial .setDiffuseColor(FXColorHelper.changeOpacity(Color.BLUEVIOLET, 0.001).darker());
		
		sPosChargeMaterial = new PhongMaterial();
		sPosChargeMaterial .setSpecularColor(FXColorHelper.changeOpacity(Color.DARKTURQUOISE,0.001));
		sPosChargeMaterial .setDiffuseColor(FXColorHelper.changeOpacity(Color.DARKTURQUOISE,0.001));
		
		sNegChargeMaterialFrame = new PhongMaterial();
		sNegChargeMaterialFrame.setSpecularColor(FXColorHelper.changeOpacity(Color.BLUEVIOLET, 0.5));
		sNegChargeMaterialFrame.setDiffuseColor(FXColorHelper.changeOpacity(Color.BLUEVIOLET, 0.5));
		
		sAromRingMaterial = new PhongMaterial();
		sAromRingMaterial .setSpecularColor(FXColorHelper.changeOpacity(Color.YELLOW,0.001));
		sAromRingMaterial .setDiffuseColor(FXColorHelper.changeOpacity(Color.YELLOW,0.001));

		sPosChargeMaterialFrame = new PhongMaterial();
		sPosChargeMaterialFrame.setSpecularColor(FXColorHelper.changeOpacity(Color.DARKTURQUOISE, 0.5));
		sPosChargeMaterialFrame.setDiffuseColor(FXColorHelper.changeOpacity(Color.DARKTURQUOISE, 0.5));
		

		sAromRingMaterialFrame = new PhongMaterial();
		sAromRingMaterialFrame.setSpecularColor(FXColorHelper.changeOpacity(Color.YELLOW, 0.5));
		sAromRingMaterialFrame.setDiffuseColor(FXColorHelper.changeOpacity(Color.YELLOW, 0.5));
	}

	
	private MolecularVolume mMolVol;
	private V3DMolecule mFXMol;
	
	public V3DCustomizablePheSA(V3DMolecule fxMol) {
		this(fxMol,new MolecularVolume(fxMol.getMolecule()));
	}
	
	public V3DCustomizablePheSA(V3DMolecule fxMol, MolecularVolume molVol) {
		super("PheSA model");
		fxMol.addMoleculeCoordinatesChangeListener(this);
		this.mFXMol = fxMol;
		mMolVol = molVol;	
		

	
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
	
	/*
	public void cleanup() {
		this.mFXMol.removeMoleculeCoordinatesChangeListener(this);
		ArrayList<Node> toBeRemoved = new ArrayList<Node>();
		for (Node node:mFXMol.getChildren()) {
			int role = node.getUserData() == null ? 0 : ((NodeDetail)node.getUserData()).getRole();
			if ((role &  MoleculeBuilder.ROLE_IS_PHARMACOPHORE)!= 0) {
				toBeRemoved.add(node);
			}
			else if ((role &  MoleculeBuilder.ROLE_IS_EXCLUSION)!= 0) {
				toBeRemoved.add(node);
			}
		}
		Platform.runLater(() -> {getChildren().removeAll(toBeRemoved);
			this.mFXMol.getChildren().remove(this);
		});
		
	}
	*/

	@Override
	public void addPharmacophorePoint(PPGaussian ppg) {
		IPharmacophorePoint pp = ppg.getPharmacophorePoint();
		PhongMaterial material;
		PhongMaterial frameMaterial;
		if(pp instanceof DonorPoint) {
			material = sDonorMaterial;
			PPArrow ppNode = new PPArrow(ppg, material); 
			getChildren().add(ppNode);}
		else if(pp instanceof AcceptorPoint) {
			material = sAcceptorMaterial;
			PPArrow ppNode = new PPArrow(ppg, material); 
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
			PPSphere ppNode = new PPSphere(ppg, material,frameMaterial);
			getChildren().add(ppNode);
		}		
		
		else if (pp instanceof AromRingPoint) {

			material = sAromRingMaterial;
			frameMaterial = sAromRingMaterialFrame;

			PPSphere ppNode = new PPSphere(ppg, material,frameMaterial);
			getChildren().add(ppNode);
		}	
	}

	@Override
	public void addExclusionSphere(VolumeGaussian eg) {
		VolumeSphere es = new VolumeSphere(eg);
		getChildren().add(es);
	}
	
	public void placeExclusionSphere(int function) {
		Random random = new Random();
		int atom = 0;
		Coordinates shift = new Coordinates(3*(2*random.nextDouble()-1),3*(2*random.nextDouble()-1),3*(2*random.nextDouble()-1));
		VolumeGaussian eg = new VolumeGaussian(atom, 6, new Coordinates(mFXMol.getMolecule().getCoordinates(atom)), shift,function);
		mMolVol.getVolumeGaussians().add(eg);
		addExclusionSphere(eg);
	}
	/**
	 * 
	 * @param generateConfs
	 * @param transformation: reverse of the transformation applied in the preprocessing, necessary to recalculate the original coordinates
	 * @return
	 */
	public PheSAMolecule getPheSAMolecule(boolean generateConfs,TransformationSequence transformation) {

		StereoMolecule origMol = ((V3DMolecule)(getParent())).getMolecule();
		MolecularVolume molVol = mMolVol;
		ArrayList<MolecularVolume> molVols = new ArrayList<MolecularVolume>();
		StereoMolecule mol = new StereoMolecule(origMol);
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);
		int[] hydrogens1 = new int[mol.getAllAtoms()-mol.getAtoms()];
		int k=0;
		for(int a=0;a<mol.getAtoms();a++) { 
			for(int j=mol.getConnAtoms(a);j<mol.getAllConnAtoms(a);j++) {
				hydrogens1[k] = mol.getConnAtom(a, j);
				k++;
			}
		}
		Canonizer can = new Canonizer(mol, Canonizer.COORDS_ARE_3D);
		StereoMolecule mol2 = can.getCanMolecule(true);
		mol2.ensureHelperArrays(Molecule.cHelperNeighbours);
		int[] heavyAtomMap = can.getGraphIndexes();
		int[] hydrogens2 = new int[mol.getAllAtoms()-mol.getAtoms()];
		k=0;
		for(int a : heavyAtomMap) {
			for(int j=mol2.getConnAtoms(a);j<mol2.getAllConnAtoms(a);j++) {
				hydrogens2[k] = mol2.getConnAtom(a, j);
				k++;
			}
		}
			
		int[] atomMap = new int[mol.getAllAtoms()];
		for(int i=0; i<hydrogens1.length;i++)
			atomMap[hydrogens1[i]] = hydrogens2[i];
		for(int i=0; i<heavyAtomMap.length;i++)
			atomMap[i] = heavyAtomMap[i];
		MolecularVolume molVolOut = new MolecularVolume(molVol);
		molVolOut.updateAtomIndeces(atomMap);
		Coordinates com = molVolOut.getCOM();
		Conformer conf = new Conformer(mol2);
		Rotation rot = molVolOut.preProcess(conf);
		transformation.addTransformation(rot.getInvert());
		transformation.addTransformation(new Translation(new double[] {com.x,com.y,com.z}));

		
		molVols.add(molVolOut);
		for(int a=0;a<mol2.getAllAtoms();a++) {
			Coordinates newCoords = new Coordinates(conf.getCoordinates(a));
			mol2.setAtomX(a, newCoords.x);
			mol2.setAtomY(a, newCoords.y);
			mol2.setAtomZ(a, newCoords.z);
		}
		if(generateConfs) {
			ConformerSetGenerator confSetGenerator = new ConformerSetGenerator(200);
			ConformerSet confs = confSetGenerator.generateConformerSet(mol2);
			for(Conformer conformer : confs) {
				MolecularVolume mv = new MolecularVolume(molVolOut);
				mv.update(conformer);
				molVols.add(mv);
			}
			
		}
		PheSAMolecule phesaMol = new PheSAMolecule(mol2,molVols);
		
		return phesaMol;
	}

}

package org.openmolecules.fx.tasks;

import com.actelion.research.calc.Matrix;
import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.chem.forcefield.ForceField;
import com.actelion.research.chem.forcefield.ForceFieldChangeListener;
import com.actelion.research.chem.forcefield.mmff.ForceFieldMMFF94;
import com.actelion.research.chem.phesa.DescriptorHandlerShape;
import com.actelion.research.chem.phesa.DescriptorHandlerShapeOneConf;
import com.actelion.research.chem.phesa.MolecularVolume;
import com.actelion.research.chem.phesa.PheSAAlignment;
import com.actelion.research.chem.phesa.PheSAMolecule;
import com.actelion.research.chem.phesa.VolumeGaussian;
import com.actelion.research.util.ArrayUtils;
import com.actelion.research.util.DoubleFormat;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;

import org.openmolecules.fx.viewer3d.CarbonAtomColorPalette;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DMoleculeUpdater;
import org.openmolecules.fx.viewer3d.V3DCustomizablePheSA;
import org.openmolecules.fx.viewer3d.V3DScene;
import org.openmolecules.mesh.MoleculeSurfaceAlgorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class V3DShapeAlignerInPlace implements IAlignmentTask {
	private V3DMolecule mFXRefMol;
	private List<V3DMolecule> mFitMols;
	private V3DScene mScene;
	public static Alert molSizeAlert  = new Alert(AlertType.ERROR);
	static {
	molSizeAlert.setTitle("Error");
	molSizeAlert.setHeaderText("Too many atoms");
	molSizeAlert.setContentText("PheSA alignment not possible for molecules with more than 100 heavy atoms");
	}
	private Map<V3DMolecule, PheSAMolecule> mPheSAMap;
	private double ppWeight;

	/**
	 *
	 * @param scene3D
	 * @param editor
	 * @param fxmol if null, then all visible molecules are minimized
	 */
	


	public V3DShapeAlignerInPlace(V3DScene scene3D, V3DMolecule fxRefMol, boolean generateConfs, double ppWeight)  {
		mScene = scene3D;
		mFXRefMol = fxRefMol;
		mPheSAMap = new HashMap<V3DMolecule,PheSAMolecule>();
		this.ppWeight = ppWeight;
		V3DMolecule fxmol;
		DescriptorHandlerShape dhs;
		if(generateConfs)
			dhs = new DescriptorHandlerShape(200,ppWeight);
		else
			dhs = new DescriptorHandlerShapeOneConf(200,ppWeight);
		mFitMols = new ArrayList<V3DMolecule>();
		if(mFXRefMol==null) { //no mol clicked in scene -> align all visible mols
			int counter = 0;
			for (Node node : scene3D.getWorld().getChildren()) {
				if (node instanceof V3DMolecule) {
						fxmol = (V3DMolecule)node;
						if(fxmol.getMolecule().getAtoms()>100) {
							molSizeAlert.showAndWait();
							return;
						}
						else if(fxmol.isVisible()) {
							if(counter==0) {
								mFXRefMol = fxmol;
								counter++;
							}
							else {
								mFitMols.add(fxmol);
								mPheSAMap.putIfAbsent(fxmol, dhs.createDescriptor(fxmol.getMolecule()));
							}
						}

				}
			}
		}
		
		else {
			for (Node node : scene3D.getWorld().getChildren()) {
				if (node instanceof V3DMolecule) {
					fxmol = (V3DMolecule)node;
					if(fxmol.getMolecule().getAtoms()>100) {
						molSizeAlert.showAndWait();
						return;
					}
					else if(fxmol.isIncluded()) {
						mFitMols.add(fxmol);
						mPheSAMap.putIfAbsent(fxmol, dhs.createDescriptor(fxmol.getMolecule()));
					}

			}
		}
		}


		for(V3DMolecule v3dMol : mFitMols) {
			for (int type = 0; type<MoleculeSurfaceAlgorithm.SURFACE_TYPE.length; type++)
				v3dMol.setSurfaceMode(type ,V3DMolecule.SurfaceMode.NONE);
				mScene.removeMeasurements(v3dMol);
		}
		
		if(mFXRefMol==null) {
			Alert alert = new Alert(AlertType.WARNING);
			alert.setTitle("Error");
			alert.setHeaderText("No Reference Structure Selected");
			alert.setContentText("Please Select a Reference");
			alert.showAndWait();
		}
		
	}
	
	@Override
	public void align() {
		Thread alignmentThread = new Thread(() -> run());
		alignmentThread.start();
	}

	private void run() {
		DescriptorHandlerShape dhs = new DescriptorHandlerShape(200,ppWeight);
		MolecularVolume refVol;
		PheSAMolecule refShape;
		StereoMolecule refMol;
		StereoMolecule fitMol;
		StereoMolecule alignedMol;
		if(mFXRefMol.getPharmacophore()==null) 
			mFXRefMol.addPharmacophore();
		mFXRefMol.getPharmacophore().setVisible(false);
		refVol = new MolecularVolume(mFXRefMol.getPharmacophore().getMolVol());
		Coordinates origCOM  = new Coordinates(refVol.getCOM());
		refMol = mFXRefMol.getMolecule();
		Conformer refConf = new Conformer(refMol);
		Matrix rotation = PheSAAlignment.preProcess(refConf, refVol).getTranspose();
		refShape = new PheSAMolecule(refMol,refVol);
		for(V3DMolecule fxFitMol : mPheSAMap.keySet()) {
			System.out.println(dhs.getSimilarity(refShape, mPheSAMap.get(fxFitMol)));
			try {
				alignedMol = dhs.getPreviousAlignment()[1];
				fitMol = fxFitMol.getMolecule();
				for(int a=0;a<fitMol.getAllAtoms();a++) {
					fitMol.setAtomX(a, alignedMol.getAtomX(a));
					fitMol.setAtomY(a, alignedMol.getAtomY(a));
					fitMol.setAtomZ(a, alignedMol.getAtomZ(a));
				}
			}
			catch(Exception e) {
				continue;
		}
		}
		//mFXRefMol.setMolecule(refConf.toMolecule());
		ObservableList<Transform> refTransforms = mFXRefMol.getTransforms();
		Transform refTransform = null;
		int nrTransforms = refTransforms.size();
		if(nrTransforms>0) {
			refTransform = refTransforms.get(0);
			for(int i=1;i<nrTransforms;i++) {
				refTransform = refTransform.createConcatenation(refTransforms.get(i));
			}
		}

		double refX = mFXRefMol.getTranslateX()	;
		double refY = mFXRefMol.getTranslateY();
		double refZ = mFXRefMol.getTranslateZ();

		//mFXRefMol.getMolecule().translate(-origCOM.x,-origCOM.y,-origCOM.z);
		//PheSAAlignment.rotateMol(mFXRefMol.getMolecule(), rotation);
		for(V3DMolecule fxFitMol : mPheSAMap.keySet()) {
			//TODO here: first translate into origin (COM), then rotate, then translate back! 
			// also do for alignment from file!
			// also check for alignment of two mols in place! (refmol and fitmol)
			PheSAAlignment.rotateMol(fxFitMol.getMolecule(), rotation);
			fxFitMol.getMolecule().translate(origCOM.x,origCOM.y,origCOM.z);

			if(refTransform!=null) {
				fxFitMol.setTransform(refTransform);
				fxFitMol.setTranslateX(refX);
				fxFitMol.setTranslateY(refY);
				fxFitMol.setTranslateZ(refZ);
				
			}
			
		}
		mFXRefMol.fireCoordinatesChange();
		Platform.runLater(() -> {
		V3DMoleculeUpdater refMolUpdater = new V3DMoleculeUpdater(mFXRefMol);
		refMolUpdater.update();
		for (V3DMolecule fxFitMol : mPheSAMap.keySet()) {
			fxFitMol.fireCoordinatesChange();
			V3DMoleculeUpdater fxMolUpdater = new V3DMoleculeUpdater(fxFitMol);
			fxMolUpdater.update();
		}
		});


	}





}

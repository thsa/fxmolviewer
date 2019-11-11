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
import com.actelion.research.chem.phesaflex.FlexibleShapeAlignment;
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
import org.openmolecules.fx.viewer3d.V3DPharmacophore;
import org.openmolecules.fx.viewer3d.V3DScene;
import org.openmolecules.mesh.MoleculeSurfaceAlgorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class V3DFlexiblePheSARefinement  {
	private V3DMolecule mFXRefMol;
	private List<V3DMolecule> mFitMols;
	private V3DScene mScene;


	/**
	 *
	 * @param scene3D
	 * @param editor
	 * @param fxmol if null, then all visible molecules are minimized
	 */
	


	private V3DFlexiblePheSARefinement(V3DScene scene3D, V3DMolecule fxRefMol)  {
		mScene = scene3D;
		mFXRefMol = fxRefMol;
		V3DMolecule fxmol;


		mFitMols = new ArrayList<V3DMolecule>();
		for (Node node : scene3D.getWorld().getChildren()) {
			if (node instanceof V3DMolecule) {
					fxmol = (V3DMolecule)node;
					if(fxmol.getMolecule().getAtoms()>100) {
						V3DShapeAlignerInPlace.molSizeAlert.showAndWait();
						return;
					}
					else if(fxmol.isIncluded()) {
						mFitMols.add(fxmol);
					}

			}
		}


		for(V3DMolecule v3dMol : mFitMols) {
			for (int type = 0; type<MoleculeSurfaceAlgorithm.SURFACE_TYPE.length; type++)
				v3dMol.setSurfaceMode(type ,V3DMolecule.SURFACE_NONE);
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
	

	public static void align(V3DScene scene3D, V3DMolecule fxRefMol) {
		V3DFlexiblePheSARefinement instance = new V3DFlexiblePheSARefinement(scene3D, fxRefMol);
		Thread alignmentThread = new Thread(() -> instance.run());
		alignmentThread.start();
	}

	private void run() {
		MolecularVolume refVol;
		MolecularVolume fitVol;
		StereoMolecule refMol;
		if(mFXRefMol.getPharmacophore()==null) 
			mFXRefMol.addPharmacophore();
		refVol = mFXRefMol.getPharmacophore().getMolVol();
		refMol = mFXRefMol.getMolecule();
		Conformer refConf = new Conformer(refMol);
		//Coordinates origCOM  = refVol.getCOM();
		//Matrix rotation = PheSAAlignment.preProcess(refConf, refVol);
		//refConf.toMolecule(refMol);
		//rotation = rotation.getTranspose();
		for(V3DMolecule fxFitMol : mFitMols) {
			if(fxFitMol.getPharmacophore()==null) 
				fxFitMol.addPharmacophore();
			StereoMolecule fitMol = fxFitMol.getMolecule();
			fitVol = fxFitMol.getPharmacophore().getMolVol();
			Conformer fitConf = new Conformer(fxFitMol.getMolecule());
			//PheSAAlignment.preProcess(fitConf, fitVol);
			//fitConf.toMolecule(fitMol);
			FlexibleShapeAlignment flexAlign = new FlexibleShapeAlignment(refMol,fitMol);
			flexAlign.align();
		}
		
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
		
		//PheSAAlignment.rotateMol(mFXRefMol.getMolecule(), rotation);
		//mFXRefMol.getMolecule().translate(origCOM.x,origCOM.y,origCOM.z);
		for(V3DMolecule fitMol : mFitMols) {
			//PheSAAlignment.rotateMol(fitMol.getMolecule(), rotation);
			//fitMol.getMolecule().translate(origCOM.x,origCOM.y,origCOM.z);

			if(refTransform!=null) {
				fitMol.setTransform(refTransform);
				fitMol.setTranslateX(refX);
				fitMol.setTranslateY(refY);
				fitMol.setTranslateZ(refZ);
				
			}
			
		}

		mFXRefMol.fireCoordinatesChange();
		Platform.runLater(() -> {
		for (V3DMolecule fitMol : mFitMols) {
			fitMol.fireCoordinatesChange();
			V3DMoleculeUpdater mFXMolUpdater = new V3DMoleculeUpdater(fitMol);
			mFXMolUpdater.update();
		}
		});

	}





}

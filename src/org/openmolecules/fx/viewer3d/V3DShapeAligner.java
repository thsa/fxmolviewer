package org.openmolecules.fx.viewer3d;

import com.actelion.research.calc.Matrix;
import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.forcefield.ForceField;
import com.actelion.research.chem.forcefield.ForceFieldChangeListener;
import com.actelion.research.chem.forcefield.mmff.ForceFieldMMFF94;
import com.actelion.research.chem.phesa.MolecularVolume;
import com.actelion.research.chem.phesa.ShapeAlignment;
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

import org.openmolecules.mesh.MoleculeSurfaceAlgorithm;

import java.util.ArrayList;

public class V3DShapeAligner {
	private static V3DShapeAligner sInstance;
	private V3DMolecule[] mFXMol;
	private V3DScene mScene;
	private Alert mMolSizeAlert;


	/**
	 *
	 * @param scene3D
	 * @param editor
	 * @param fxmol if null, then all visible molecules are minimized
	 */
	

	public static void align(V3DScene scene3D) {
		sInstance = new V3DShapeAligner(scene3D);
		sInstance.align();
	}

	private V3DShapeAligner(V3DScene scene3D)  {
		mMolSizeAlert = new Alert(AlertType.ERROR);
		mMolSizeAlert.setTitle("Error");
		mMolSizeAlert.setHeaderText("Too many atoms");
		mMolSizeAlert.setContentText("PheSA alignment not possible for molecules with more than 100 heavy atoms");
		mScene = scene3D;
		V3DMolecule fxmol;
		ArrayList<V3DMolecule> fxmolList = new ArrayList<V3DMolecule>();
		for (Node node : scene3D.getWorld().getChildren()) {
			if (node instanceof V3DMolecule && node.isVisible()) {
				if (node.isVisible()) {
					fxmol = (V3DMolecule)node;
					if(fxmol.getMolecule().getAtoms()>100) {
						mMolSizeAlert.showAndWait();
						return;
					}
					fxmolList.add(fxmol);
				}
			}
		}

		mFXMol = fxmolList.toArray(new V3DMolecule[0]);
		for(V3DMolecule v3dMol : mFXMol) {
			for (int type = 0; type<MoleculeSurfaceAlgorithm.SURFACE_TYPE.length; type++)
				v3dMol.setSurfaceMode(type ,V3DMolecule.SURFACE_NONE);
				mScene.removeMeasurements(v3dMol);
		}
		
	}
	
	private void align() {
		Thread alignmentThread = new Thread(() -> run());
		alignmentThread.start();
	}

	private void run() {
		if (mFXMol.length == 0)
			return;
		//MolecularVolume[] molVols = new MolecularVolume[mFXMol.length];
		if(mFXMol[0].getPharmacophore()==null)
			mFXMol[0].addPharmacophore();
		V3DPharmacophore refPP = mFXMol[0].getPharmacophore();
		refPP.setVisible(false);
		MolecularVolume refVol = refPP.getMolVol();
		Coordinates origCOM  = refVol.getCOM();
		//Point3D origCOMglobal = mFXMol[0].localToParent(0, 0, 0);
		Matrix rotation = ShapeAlignment.preProcess(mFXMol[0].getMolecule(), refVol);
		rotation = rotation.getTranspose();
		//Coordinates[] corrections = new Coordinates[mFXMol.length-1];
		for(int i=1;i<mFXMol.length;i++) {
			if(mFXMol[i].getPharmacophore()==null)
				mFXMol[i].addPharmacophore();
			V3DPharmacophore fitPP = mFXMol[i].getPharmacophore();
			fitPP.setVisible(false);
			MolecularVolume molVol = fitPP.getMolVol();
			ShapeAlignment.preProcess(mFXMol[i].getMolecule(), molVol);
			ShapeAlignment alignment = new ShapeAlignment(refVol,molVol);
			double[] result = alignment.findAlignment(ShapeAlignment.initialTransform(2));
			ShapeAlignment.rotateMol(mFXMol[i].getMolecule(), new double[] {result[1], result[2],
					result[3], result[4], result[5], result[6], result[7]});
		}

	
		
		ObservableList<Transform> refTransforms = mFXMol[0].getTransforms();
		Transform refTransform = null;
		int nrTransforms = refTransforms.size();
		if(nrTransforms>0) {
			refTransform = refTransforms.get(0);
			for(int i=1;i<nrTransforms;i++) {
				refTransform = refTransform.createConcatenation(refTransforms.get(i));
			}
		}
			
		double refX = mFXMol[0].getTranslateX()	;
		double refY = mFXMol[0].getTranslateY();
		double refZ = mFXMol[0].getTranslateZ();
		for(int i=0;i<mFXMol.length;i++) {
			ShapeAlignment.rotateMol(mFXMol[i].getMolecule(), rotation);
			mFXMol[i].getMolecule().translate(origCOM.x,origCOM.y,origCOM.z);
			if(i>0) {
				if(refTransform!=null) {
					mFXMol[i].setTransform(refTransform);
					mFXMol[i].setTranslateX(refX);
					mFXMol[i].setTranslateY(refY);
					mFXMol[i].setTranslateZ(refZ);
				}
			}
			
		}


		Platform.runLater(() -> {
		for (int i=0; i<mFXMol.length; i++) {
			mFXMol[i].fireCoordinatesChange();
			V3DMoleculeUpdater mFXMolUpdater = new V3DMoleculeUpdater(mFXMol[i]);
			mFXMolUpdater.update();
		}
		
		});
		


	}




}

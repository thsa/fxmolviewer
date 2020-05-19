package org.openmolecules.fx.tasks;

import org.openmolecules.fx.viewer3d.CarbonAtomColorPalette;
import org.openmolecules.fx.viewer3d.V3DCustomizablePheSA;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DMoleculeUpdater;
import org.openmolecules.fx.viewer3d.V3DScene;

import com.actelion.research.calc.Matrix;
import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.chem.phesa.DescriptorHandlerShape;
import com.actelion.research.chem.phesa.MolecularVolume;
import com.actelion.research.chem.phesa.PheSAAlignment;
import com.actelion.research.chem.phesa.PheSAMolecule;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.transform.Transform;

import java.util.ArrayList;
import java.util.List;


public class V3DShapeAlignerFromFile implements IAlignmentTask {
		
	private V3DScene mScene;
	private MolecularVolume mRefVol;
	private V3DMolecule mRefFXMol;
	private List<PheSAMolecule> mFitShapes;
	private double ppWeight;
	
	
	
	public V3DShapeAlignerFromFile(V3DScene scene3D, V3DCustomizablePheSA refModel,  List<PheSAMolecule> fitShapes, double ppWeight)  {
		this(scene3D, ((V3DMolecule)(refModel.getParent())), new MolecularVolume(refModel.getMolVol()), fitShapes,ppWeight);
	}
	
	public V3DShapeAlignerFromFile(V3DScene scene3D, V3DMolecule refFXMol,  List<PheSAMolecule> fitShapes, double ppWeight) {
		this(scene3D,refFXMol, new MolecularVolume(refFXMol.getMolecule()), fitShapes,ppWeight);
	}

	public V3DShapeAlignerFromFile(V3DScene scene3D,V3DMolecule refFXMol, MolecularVolume refVol, List<PheSAMolecule> fitShapes, double ppWeight)  {
		mScene = scene3D;
		mRefFXMol = refFXMol;
		mFitShapes = fitShapes;
		this.ppWeight = ppWeight;
	}
	
	private void run() {
		DescriptorHandlerShape dhs = new DescriptorHandlerShape(200,ppWeight);
		List<V3DMolecule> fittedFXMols = new ArrayList<V3DMolecule>();
		PheSAMolecule refShape;
		StereoMolecule refMol;
		Coordinates origCOM  = mRefVol.getCOM();
		refMol = mRefFXMol.getMolecule();
		Conformer refConf = new Conformer(refMol);
		Matrix rotation = PheSAAlignment.preProcess(refConf, mRefVol);
		rotation = rotation.getTranspose();
		refShape = new PheSAMolecule(refMol,mRefVol);
		for(PheSAMolecule fitShape : mFitShapes) {
			dhs.getSimilarity(refShape, fitShape);
			try {
				fittedFXMols.add(new V3DMolecule(dhs.getPreviousAlignment()[1], V3DMolecule.getNextID(),V3DMolecule.MoleculeRole.LIGAND, mScene.mayOverrideHydrogenColor()));
			}
			catch(Exception e) {
				continue;
		}
		}
		
		ObservableList<Transform> refTransforms = mRefFXMol.getTransforms();
		Transform refTransform = null;
		int nrTransforms = refTransforms.size();
		if(nrTransforms>0) {
			refTransform = refTransforms.get(0);
			for(int i=1;i<nrTransforms;i++) {
				refTransform = refTransform.createConcatenation(refTransforms.get(i));
			}
		}
			
		double refX = mRefFXMol.getTranslateX()	;
		double refY = mRefFXMol.getTranslateY();
		double refZ = mRefFXMol.getTranslateZ();
		

		for(int i=0;i<fittedFXMols.size();i++) {
			PheSAAlignment.rotateMol(fittedFXMols.get(i).getMolecule(), rotation);
			fittedFXMols.get(i).getMolecule().translate(origCOM.x,origCOM.y,origCOM.z);
			if(refTransform!=null) {
					fittedFXMols.get(i).setTransform(refTransform);
					fittedFXMols.get(i).setTranslateX(refX);
					fittedFXMols.get(i).setTranslateY(refY);
					fittedFXMols.get(i).setTranslateZ(refZ);
			}
			
		}

		mRefFXMol.fireCoordinatesChange();
		V3DMoleculeUpdater refMolUpdater = new V3DMoleculeUpdater(mRefFXMol);
		refMolUpdater.update();
		Platform.runLater(() -> {
		for (int i=0; i<fittedFXMols.size(); i++) {
			mScene.addMolecule(fittedFXMols.get(i));
			fittedFXMols.get(i).setColor(CarbonAtomColorPalette.getColor(fittedFXMols.get(i).getID()));
			fittedFXMols.get(i).fireCoordinatesChange();
			V3DMoleculeUpdater fxMolUpdater = new V3DMoleculeUpdater(fittedFXMols.get(i));
			fxMolUpdater.update();
		}
		
		});
	}
		


	@Override
	public void align() {
		Thread alignmentThread = new Thread(() -> run());
		alignmentThread.start();
	}

	
	
	

}

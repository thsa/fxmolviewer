package org.openmolecules.fx.tasks;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.alignment3d.transformation.Rotation;
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.chem.phesa.DescriptorHandlerShape;
import com.actelion.research.chem.phesa.MolecularVolume;
import com.actelion.research.chem.phesa.PheSAMolecule;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.transform.Transform;
import org.openmolecules.fx.viewer3d.CarbonAtomColorPalette;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DMoleculeUpdater;
import org.openmolecules.fx.viewer3d.V3DScene;

import java.util.ArrayList;
import java.util.List;


public class V3DShapeAlignment {
		
	private V3DScene mScene;
	private MolecularVolume mRefVol;
	private V3DMolecule mRefFXMol;
	private List<StereoMolecule> mFitMols;
	


	public V3DShapeAlignment(V3DScene scene3D,V3DMolecule refFXMol, MolecularVolume refVol, List<StereoMolecule> fitMols)  {
		mScene = scene3D;
		mRefFXMol = refFXMol;
		mFitMols = fitMols;
		mRefVol = refVol;
	}
	
	private void run() {
		DescriptorHandlerShape dhs = new DescriptorHandlerShape(500,0.5);
		dhs.setFlexible(true);
		List<V3DMolecule> fittedFXMols = new ArrayList<V3DMolecule>();
		PheSAMolecule refShape;
		StereoMolecule refMol;
		Coordinates origCOM  = mRefVol.getCOM();
		refMol = mRefFXMol.getMolecule();
		Conformer refConf = new Conformer(refMol);
		Rotation rotation = mRefVol.preProcess(refConf);
		rotation = rotation.getInvert();
		refShape = new PheSAMolecule(refMol,mRefVol);
		for(StereoMolecule mol : mFitMols) {
			PheSAMolecule fitShape = dhs.createDescriptor(mol);
			dhs.getSimilarity(refShape, fitShape);

			try {
				fittedFXMols.add(new V3DMolecule(dhs.getPreviousAlignment()[1], V3DMolecule.getNextID(),V3DMolecule.MoleculeRole.LIGAND, mScene.isOverrideHydrogenColor(), false));
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
			rotation.apply(fittedFXMols.get(i).getMolecule());
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
			mScene.addMolecule(fittedFXMols.get(i), true);
			fittedFXMols.get(i).setColor(CarbonAtomColorPalette.getColor(fittedFXMols.get(i).getID()));
			fittedFXMols.get(i).fireCoordinatesChange();
			V3DMoleculeUpdater fxMolUpdater = new V3DMoleculeUpdater(fittedFXMols.get(i));
			fxMolUpdater.update();
		}
		
		});
	}
		


	public void align(boolean async) {
		if(async) { 
			Thread alignmentThread = new Thread(() -> run());
			alignmentThread.start();
		}
		else 
			run();
	}

	
	
	

}

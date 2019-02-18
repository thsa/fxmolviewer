package org.openmolecules.fx.viewer3d;

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.forcefield.ForceField;
import com.actelion.research.chem.forcefield.ForceFieldChangeListener;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Point3D;
import mmff.ForceFieldMMFF94;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class V3DMinimizationHandler implements ForceFieldChangeListener {
	private ArrayList<V3DMolecule> mMolecularScenery;
	private int[] mIndividualMolSizes;
	private ForceField mForceField;
	private V3DSceneWithToolsPane mScene;
	
	public V3DMinimizationHandler(ArrayList<V3DMolecule> molecularScenery, V3DSceneWithToolsPane scene)  {
		mMolecularScenery = molecularScenery;
		mScene = scene;
    	StereoMolecule molScenery = new StereoMolecule();
    	mIndividualMolSizes = new int[molecularScenery.size()];
    	int counter = 0;

    	for(V3DMolecule v3dMol : molecularScenery) {
    		StereoMolecule mol = new StereoMolecule(v3dMol.getConformer().getMolecule());
    		for(int a=0;a<mol.getAllAtoms();a++) {
    			Point3D globalCoords = v3dMol.localToScene(mol.getAtomX(a),mol.getAtomY(a),mol.getAtomZ(a));
    			mol.setAtomX(a, globalCoords.getX());
    			mol.setAtomY(a, globalCoords.getY());
    			mol.setAtomZ(a, globalCoords.getZ());
    		}
    		molScenery.addMolecule(mol);
    		mIndividualMolSizes[counter] = mol.getAllAtoms();
    		counter++;
    	}

		ForceFieldMMFF94.initialize(ForceFieldMMFF94.MMFF94SPLUS);
		mForceField = new ForceFieldMMFF94(molScenery, ForceFieldMMFF94.MMFF94SPLUS);
		mForceField.addListener(this);
	}
    	
	public void minimise() {
		MinimizerThread minimizerTask = new MinimizerThread(mForceField);

		ExecutorService executorService = Executors.newSingleThreadExecutor();
		/*
		mScene.setOnMouseClicked(new EventHandler<MouseEvent>() {
	        @Override
	        public void handle(MouseEvent event) {
	        	mForceField.interrupt();
	        }
		});
		
		mScene.setOnKeyPressed(new EventHandler<KeyEvent>() {
	        @Override
	        public void handle(KeyEvent event) {
	        	mForceField.interrupt();
	        }
		});
		*/
	        
		
		executorService.submit(minimizerTask);
		executorService.shutdown();
		}

	public class MinimizerThread extends Task<Void> {
		private ForceField mForceField;
		
		public MinimizerThread(ForceField forceField) {
			mForceField = forceField;
		}
		@Override
		protected Void call() throws Exception {
			mForceField.minimise();
			return null;
		}
	}

	@Override
	public void stateChanged() {
		int firstIndex = 0;
		int counter = 0;
		int lastIndex = mIndividualMolSizes[counter];
		double[] pos_ = mForceField.getCurrentPositionsMapped();
		double energy = mForceField.getTotalEnergy(mForceField.getCurrentPositions());
		Platform.runLater(new Runnable(){
			@Override public void run() {
		//		//v3dMol.updateCoordinates(pos);}
				mScene.createOutput("energy: " + Double.toString(energy) + " kcal/mol");}
		});
		for(V3DMolecule v3dMol : mMolecularScenery) {
			//final int innerFirstIndex = firstIndex;
			int len = lastIndex - firstIndex;
			double[] pos = Arrays.copyOfRange(pos_, 3*firstIndex, 3*lastIndex);
			Platform.runLater(new Runnable(){
				@Override public void run() {
			//		//v3dMol.updateCoordinates(pos);}
					mScene.getScene3D().coordinatesChanged(v3dMol, pos);}
			});
			firstIndex = lastIndex;
			counter++;
			if(counter==mMolecularScenery.size()) break;
			lastIndex = firstIndex + mIndividualMolSizes[counter];
			}
	}
}

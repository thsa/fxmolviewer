package org.openmolecules.fx.tasks;

import java.util.Arrays;
import java.util.List;

import org.openmolecules.fx.viewer3d.V3DBindingSite;
import org.openmolecules.fx.viewer3d.V3DMolGroup;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DMoleculeUpdater;
import org.openmolecules.fx.viewer3d.V3DScene;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.docking.DockingEngine;
import com.actelion.research.chem.docking.DockingEngine.DockingResult;
import com.actelion.research.chem.docking.DockingFailedException;

import javafx.application.Platform;
import javafx.geometry.Point3D;

public class V3DDockingEngine {
	
	private V3DBindingSite bindingSite;
	private V3DScene scene3D;

	
	public V3DDockingEngine(V3DScene scene, V3DBindingSite bindingSite) {
		this.bindingSite = bindingSite;
		this.scene3D = scene;
		
		
	}
	
	public double refineNativePose() throws DockingFailedException {
		V3DMoleculeUpdater fxMolUpdater = new V3DMoleculeUpdater(bindingSite.getNativeLigand());
		StereoMolecule newReceptor = getMolWithSceneCoords(bindingSite.getReceptor());
		StereoMolecule newLig = getMolWithSceneCoords(bindingSite.getNativeLigand());
		
		DockingEngine engine = new DockingEngine(newReceptor, newLig); 
		double[] newPos = new double[3*newLig.getAllAtoms()];
		double energy = engine.refineNativePose(0.4, newPos);

		updateCoords(this.bindingSite.getNativeLigand(),newPos);
		newReceptor =  getMolWithSceneCoords(bindingSite.getReceptor());
		newLig =  getMolWithSceneCoords(bindingSite.getNativeLigand());
		Platform.runLater(() -> {
			fxMolUpdater.update();
			this.bindingSite.getNativeLigand().fireCoordinatesChange();
		});
		return energy;
	}
	
	public void reDock() throws DockingFailedException {
		StereoMolecule newReceptor =  getMolWithSceneCoords(bindingSite.getReceptor());
		StereoMolecule newLig =  getMolWithSceneCoords(bindingSite.getNativeLigand());
		
		DockingEngine engine = new DockingEngine(newReceptor, newLig); 
		StereoMolecule mol = new StereoMolecule(newLig);
		mol.ensureHelperArrays(Molecule.cHelperCIP);
		try {
			DockingResult result = engine.dockMolecule(mol);
			StereoMolecule docked = result.getPose();

			V3DMolGroup dockedGroup = new V3DMolGroup("DOCKED");
			Platform.runLater(() -> {
				scene3D.addMolGroup(dockedGroup);
				V3DMolecule fxmol = new V3DMolecule(docked);
				scene3D.addMolecule(fxmol, dockedGroup);
			});
			System.out.println(result.getScore());
		} catch (DockingFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void dockLibrary(List<StereoMolecule> library) throws DockingFailedException {
		StereoMolecule newReceptor = getMolWithSceneCoords(bindingSite.getReceptor());
		StereoMolecule newLig = getMolWithSceneCoords(bindingSite.getNativeLigand());
		
		DockingEngine engine = new DockingEngine(newReceptor, newLig); 
		StereoMolecule mol = new StereoMolecule(newLig);
		mol.ensureHelperArrays(Molecule.cHelperCIP);
		
		V3DMolGroup dockedGroup = new V3DMolGroup("DOCKED");
		Platform.runLater(() -> {
			scene3D.addMolGroup(dockedGroup);
		});
		for(StereoMolecule toDock : library) {
			try {
				DockingResult result = engine.dockMolecule(toDock);
				StereoMolecule docked = result.getPose();
				Platform.runLater(() -> {
					V3DMolecule fxmol = new V3DMolecule(docked);
					scene3D.addMolecule(fxmol, dockedGroup);
				});
				System.out.println(result.getScore());
			} catch (DockingFailedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void updateCoords(V3DMolecule fxmol, double[] globalPos) {
		StereoMolecule mol = fxmol.getMolecule();
		for (int atom=0; atom<mol.getAllAtoms(); atom++) {
			//Point3D local = fxmol.sceneToLocal(new Point3D(globalPos[3*atom], globalPos[3*atom+1], globalPos[3*atom+2] ));
			Coordinates local = fxmol.getWorldToLocalCoordinates(scene3D, new Coordinates(globalPos[3*atom], globalPos[3*atom+1], globalPos[3*atom+2]));
			mol.setAtomX(atom, local.x);
			mol.setAtomY(atom, local.y);
			mol.setAtomZ(atom, local.z);
		}
	}
	
	private StereoMolecule getMolWithSceneCoords(V3DMolecule fxmol) {
		StereoMolecule origMol = fxmol.getMolecule(); 
		StereoMolecule newMol = new StereoMolecule(origMol);
		newMol.ensureHelperArrays(Molecule.cHelperCIP);
		/*
		 * transform to global coordinate system
		 */
		for(int a=0;a<origMol.getAllAtoms();a++) {
			//Point3D globalCoords = fxmol.localToScene(new Point3D(origMol.getCoordinates(a).x, origMol.getCoordinates(a).y, origMol.getCoordinates(a).z));
			Coordinates globalCoords = fxmol.getWorldCoordinates(scene3D, origMol.getCoordinates(a));
			newMol.setAtomX(a, globalCoords.x);
			newMol.setAtomY(a, globalCoords.y);
			newMol.setAtomZ(a, globalCoords.z);
		}
		return newMol;
	}
	
	
	
	
	
	

}

package org.openmolecules.fx.tasks;

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.forcefield.ForceField;
import com.actelion.research.chem.forcefield.ForceFieldChangeListener;
import com.actelion.research.chem.forcefield.mmff.ForceFieldMMFF94;
import com.actelion.research.util.DoubleFormat;
import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.Node;

import org.openmolecules.fx.viewer3d.V3DBindingSite;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DMoleculeUpdater;
import org.openmolecules.fx.viewer3d.V3DScene;
import org.openmolecules.fx.viewer3d.V3DSceneEditor;
import org.openmolecules.mesh.MoleculeSurfaceAlgorithm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class V3DMinimizer implements ForceFieldChangeListener {
	private static V3DMinimizer sInstance;
	private V3DMolecule[] mFXMol;
	private V3DMoleculeUpdater[] mFXMolUpdater;
	private ForceField mForceField;
	private V3DSceneEditor mEditor;
	private volatile Thread mMinimizationThread;
	private boolean mHydrogensOnly;
	private List<Integer> mRigidAtoms;

	/**
	 *
	 * @param scene3D
	 * @param editor
	 * @param fxmol if null, then all visible molecules are minimized
	 */
	public static void minimize(V3DSceneEditor editor, V3DBindingSite bdsHelper, boolean hydrogensOnly) {
		stopMinimization();
		sInstance = new V3DMinimizer(editor, bdsHelper, hydrogensOnly);
		sInstance.minimize();
	}
	
	public static void minimize(V3DScene scene3D, V3DSceneEditor editor, V3DMolecule fxmol, boolean hydrogensOnly) {
		stopMinimization();
		sInstance = new V3DMinimizer(scene3D, editor, fxmol,hydrogensOnly);
		sInstance.minimize();
	}

	private V3DMinimizer(V3DScene scene3D, V3DSceneEditor editor, V3DMolecule fxmol, boolean hydrogensOnly)  {
		mRigidAtoms = new ArrayList<>();
		mEditor = editor;
		mHydrogensOnly = hydrogensOnly;
		if (fxmol != null) {
			mFXMol = new V3DMolecule[1];
			mFXMol[0] = fxmol;
			mFXMol[0].addImplicitHydrogens();
		}
		else {
			ArrayList<V3DMolecule> fxmolList = new ArrayList<V3DMolecule>();
			for (Node node : scene3D.getWorld().getAllAttachedRotatableGroups()) {
				if (node instanceof V3DMolecule && node.isVisible()) {
					if (node.isVisible()) {
						fxmol = (V3DMolecule)node;
						fxmol.addImplicitHydrogens();
						fxmolList.add(fxmol);
					}
				}
			}

			mFXMol = fxmolList.toArray(new V3DMolecule[0]);
		}
	}
	
	private V3DMinimizer(V3DSceneEditor editor, V3DBindingSite bdsHelper, boolean hydrogensOnly) {
		mEditor = editor;
		mHydrogensOnly = hydrogensOnly;
		ArrayList<V3DMolecule> fxmolList = new ArrayList<V3DMolecule>();
		fxmolList.add(bdsHelper.getReceptor());
		fxmolList.add(bdsHelper.getNativeLigand());
		mFXMol = fxmolList.toArray(new V3DMolecule[0]);
		mRigidAtoms = new ArrayList<>();
		Set<Integer> bindingSiteAtoms = bdsHelper.getBindingSiteAtoms();
		StereoMolecule receptor = bdsHelper.getReceptor().getMolecule();
		for(int a=0;a<receptor.getAllAtoms();a++) {
			if(!bindingSiteAtoms.contains(a))
				mRigidAtoms.add(a);
		}
		
		
	}

	private void minimize() {
		if (mFXMol.length == 0)
			return;

		mFXMolUpdater = new V3DMoleculeUpdater[mFXMol.length];
		int totalAtomCount = 0;
		for (int i=0; i<mFXMol.length; i++) {
			mFXMolUpdater[i] = new V3DMoleculeUpdater(mFXMol[i]);
			totalAtomCount += mFXMol[i].getMolecule().getAllAtoms();
		}

		StereoMolecule molScenery = new StereoMolecule();

		int atom = 0;
		int fixedAtomCount = 0;
		Set<Integer> fixedAtoms = new HashSet<>();
		/*we have to correct the indeces of the fixed atoms, since they are rearranged when the helper arrays are called
		 * first the molecules are concatenated into one molecule, so the indeces have to be corrected by the offset, then in the
		 * second step we can map to the correct indeces using the hydrogen map
		*/
		int atomIndexOffset = 0;
		for(V3DMolecule fxmol : mFXMol) {
			// remove any surfaces
			for (int type = 0; type<MoleculeSurfaceAlgorithm.SURFACE_TYPE.length; type++)
				fxmol.setSurfaceMode(type ,V3DMolecule.SurfaceMode.NONE);

			StereoMolecule mol = fxmol.getMolecule();
			molScenery.addMolecule(mol);

			for(int a=0;a<mol.getAllAtoms();a++) {
				Point3D globalCoords = fxmol.localToScene(mol.getAtomX(a), mol.getAtomY(a), mol.getAtomZ(a));
				molScenery.setAtomX(atom, globalCoords.getX());
				molScenery.setAtomY(atom, globalCoords.getY());
				molScenery.setAtomZ(atom, globalCoords.getZ());
				if(mHydrogensOnly && mol.getAtomicNo(a)!=1)
					fixedAtoms.add(a+atomIndexOffset);
				else if (mol.isMarkedAtom(a))
					fixedAtoms.add(a+atomIndexOffset);
				if (mol.getAtomicNo(a) == 0)
					molScenery.setAtomicNo(atom, 6);
				atom++;
			}
			atomIndexOffset+=mol.getAllAtoms();
		}
		ForceFieldMMFF94.initialize(ForceFieldMMFF94.MMFF94SPLUS);
		mForceField = new ForceFieldMMFF94(molScenery, ForceFieldMMFF94.MMFF94SPLUS);
		mForceField.addListener(this);
		if (!fixedAtoms.isEmpty()) {
			fixedAtoms.addAll(mRigidAtoms);
			int[] fixedAtomsMapped = new int[fixedAtoms.size()];
			int[] map = molScenery.getHandleHydrogenMap();
			int i=0;
			for(int fixedAtom : fixedAtoms) {
				fixedAtomsMapped[i] = map[fixedAtom];
				i++;
			}
			mForceField.setFixedAtoms(fixedAtomsMapped);
		}

		mMinimizationThread = new Thread(() -> mForceField.minimise());
		mMinimizationThread.start();
	}

	public static void stopMinimization() {
		if (sInstance != null && sInstance.mMinimizationThread != null) {
			sInstance.mMinimizationThread = null;
			sInstance.mForceField.interrupt();
			sInstance = null;
		}
	}

	@Override
	public void stateChanged() {
		if (mMinimizationThread != null) {
			final double[] pos = mForceField.getCurrentPositions();
			final double energy = mForceField.getTotalEnergy();
			Platform.runLater(() -> {
				if (mMinimizationThread != null) {
					int posIndex = 0;
					for (int i=0; i<mFXMol.length; i++) {
						StereoMolecule mol = mFXMol[i].getMolecule();
						for (int atom=0; atom<mol.getAllAtoms(); atom++) {
							Point3D p = mFXMol[i].parentToLocal(pos[posIndex], pos[posIndex+1], pos[posIndex+2]);
							mol.setAtomX(atom, p.getX());
							mol.setAtomY(atom, p.getY());
							mol.setAtomZ(atom, p.getZ());
							posIndex += 3;
						}
						mFXMol[i].fireCoordinatesChange();
						mFXMolUpdater[i].update();
					}

					if (mEditor != null)
						mEditor.createOutput("energy: " + Double.toString(energy) + " kcal/mol");
					else
						System.out.println("MMFF-Energy: "+ DoubleFormat.toString(energy) + " kcal/mol");
				}
			});
		}
	}
}

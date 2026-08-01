package org.openmolecules.fx.viewer3d;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.interactions.statistics.WaterInteractionHelper;
import javafx.collections.ListChangeListener;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import org.openmolecules.fx.viewer3d.nodes.DashedRod;
import org.openmolecules.fx.viewer3d.nodes.NodeDetail;

import java.util.ArrayList;
import java.util.List;

public class V3DWaterBridgeHandler implements ListChangeListener<V3DRotatableGroup>,MolCoordinatesChangeListener {
	private final V3DScene mScene3D;
	private final List<V3DWaterBridge> mWaterBridgeList;

	public V3DWaterBridgeHandler(V3DScene scene) {
		mScene3D = scene;
		mScene3D.getWorld().addListener(this);
		mWaterBridgeList = new ArrayList<>();
		addAllWaterBridges();
	}

	public void cleanup() {
		mScene3D.getWorld().removeListener(this);
	}

	public void setVisible(boolean visible) {
		for (V3DWaterBridge bridge: mWaterBridgeList)
			bridge.setVisible(visible);
	}

	public void _addAllWaterBridges() {
		List<V3DMolecule> fxmols = mScene3D.getMolsInScene();
		for (V3DMolecule fxmol : fxmols)
			fxmol.getMolecule().ensureHelperArrays(Molecule.cHelperNeighbours);

		for (int m1=0; m1<fxmols.size(); m1++) {
			V3DMolecule fxmol1 = fxmols.get(m1);
			StereoMolecule mol1 = fxmol1.getMolecule();
			for (int water=0; water<mol1.getAtoms(); water++) {
				if (mol1.getAtomicNo(water) == 8 && mol1.getConnAtoms(water) == 0) {
					for (int hetero=0; hetero<mol1.getAtoms(); hetero++)
						if (mol1.isElectronegative(hetero) && hetero != water)
							tryAddWaterBridge(fxmol1, water, fxmol1, hetero);

					for (int m2=0; m2<fxmols.size(); m2++) {
						if (m2 != m1) {
							V3DMolecule fxmol2 = fxmols.get(m2);
							StereoMolecule mol2 = fxmol2.getMolecule();
							for (int hetero=0; hetero<mol2.getAtoms(); hetero++)
								// make sure not to handle water-water connections twice
								if (mol2.isElectronegative(hetero)
								 && (mol2.getAtomicNo(hetero) != 8 || mol2.getConnAtoms(hetero) != 0 || m2>m1))
									tryAddWaterBridge(fxmol1, water, fxmol2, hetero);
						}
					}
				}
			}
		}
	}

	public void addAllWaterBridges() {
		List<V3DMolecule> fxmols = mScene3D.getMolsInScene();
		for (V3DMolecule fxmol : fxmols)
			fxmol.getMolecule().ensureHelperArrays(Molecule.cHelperNeighbours);

		for (int m1=0; m1<fxmols.size(); m1++) {
			V3DMolecule fxmol1 = fxmols.get(m1);
			StereoMolecule mol1 = fxmol1.getMolecule();
			for (Node node1: fxmol1.getChildren()) {
				NodeDetail detail1 = (NodeDetail)node1.getUserData();
				if (detail1 != null && detail1.isAtom() && node1.isVisible()) {
					int water = detail1.getAtom();
					if (mol1.getAtomicNo(water) == 8 && mol1.getConnAtoms(water) == 0) {
						for (int hetero=0; hetero<mol1.getAtoms(); hetero++)
							if (mol1.isElectronegative(hetero) && hetero != water)
								tryAddWaterBridge(fxmol1, water, fxmol1, hetero);

						for (int m2=0; m2<fxmols.size(); m2++) {
							if (m2 != m1) {
								V3DMolecule fxmol2 = fxmols.get(m2);
								StereoMolecule mol2 = fxmol2.getMolecule();
								for (Node node2: fxmol2.getChildren()) {
									NodeDetail detail2 = (NodeDetail)node2.getUserData();
									if (detail2 != null && detail2.isAtom() && node2.isVisible()) {
										int hetero = detail2.getAtom();
										// make sure not to handle water-water connections twice
										if (mol2.isElectronegative(hetero)
										 && (mol2.getAtomicNo(hetero) != 8 || mol2.getConnAtoms(hetero) != 0 || m2>m1))
											tryAddWaterBridge(fxmol1, water, fxmol2, hetero);
									}
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * @param fxmol1 fxmol for water atom
	 * @param atom1 water atom
	 * @param fxmol2 fxmol for any hetero atom
	 * @param atom2 any hetero atom
	 */
	private void tryAddWaterBridge(V3DMolecule fxmol1, int atom1, V3DMolecule fxmol2, int atom2) {
		StereoMolecule mol1 = fxmol1.getMolecule();
		StereoMolecule mol2 = fxmol2.getMolecule();
		Coordinates c1 = mol1.getAtomCoordinates(atom1);
		Coordinates c2 = mol2.getAtomCoordinates(atom2);
		Point3D p1 = fxmol1.localToParent(c1.x, c1.y, c1.z);
		Point3D p2 = fxmol2.localToParent(c2.x, c2.y, c2.z);
		double distance = p1.distance(p2);
		if (WaterInteractionHelper.qualifiesAsWaterNeighbour(mol2.getAtomicNo(atom2), distance))
			mWaterBridgeList.add(new V3DWaterBridge(mScene3D, p1, p2));
	}

	public void update() {
		removeAllWaterBridges();
		addAllWaterBridges();
	}

	@Override
	public void onChanged(Change<? extends V3DRotatableGroup> c) {
		update();
	}

	private void removeAllWaterBridges() {
		for(V3DWaterBridge waterBridge : mWaterBridgeList)
			waterBridge.cleanup();

		mWaterBridgeList.clear();
	}

	@Override
	public void coordinatesChanged() {
		update();
	}
}

class V3DWaterBridge {
	private static final float RADIUS = 0.06f;
	private static final float DASH_LENGTH = 0.1f;
	private static final float GAP_LENGTH = 0.15f;

	private final DashedRod mRod;
	private final Group mParent;

	public V3DWaterBridge(V3DScene scene, Point3D p1, Point3D p2) {
		mRod = new DashedRod(p1, p2, Color.ROYALBLUE, RADIUS, DASH_LENGTH, GAP_LENGTH);
		mParent = scene.getWorld();
		mParent.getChildren().addFirst(mRod);
	}

	public void setVisible(boolean visible) {
		mRod.setVisible(visible);
	}

	public void cleanup() {
		mParent.getChildren().remove(mRod);
	}
}

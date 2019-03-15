package org.openmolecules.fx.viewer3d;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.conf.Conformer;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import org.openmolecules.render.MoleculeArchitect;
import org.openmolecules.render.MoleculeBuilder;

import java.util.TreeMap;

/**
 * The V3DMoleculeUpdater uses the MoleculeArchitect with the same mode that has been used earlier to construct the V3DMolecule.
 * This class, however, does not build a molecule. It uses the coordinates of the stream of sphere and cylinder build instructions
 * to update the coordinates of the nodes of an existing V3DMolecule.
 * This class assumes that the order of instructions matches the constructions during build and, thus, can be used
 * to update the coordinates of all V3DMolecule's nodes, which were put into the children's list in build order.
 */
public class V3DMoleculeUpdater implements MoleculeBuilder {
	private MoleculeArchitect mArchitect;
	private V3DMolecule mV3DMolecule;
	private TreeMap<Integer,Node> mNodeMap;

	public V3DMoleculeUpdater(V3DMolecule fxmol) {
		mArchitect = new MoleculeArchitect(this);
		mArchitect.setConstructionMode(fxmol.getConstructionMode());
		mV3DMolecule = fxmol;

		mNodeMap = new TreeMap<Integer,Node>();
		for (Node node:fxmol.getChildren()) {
			int role = node.getUserData() == null ? 0 : ((NodeDetail)node.getUserData()).getRole();
			if ((role & (MoleculeBuilder.ROLE_IS_ATOM | MoleculeBuilder.ROLE_IS_BOND)) != 0)
				mNodeMap.put(role, node);
		}
	}

	public void update() {
		Conformer conformer = mV3DMolecule.getConformer();
		mArchitect.buildMolecule(conformer);

//		if (mArchitect.getConstructionMode() == MoleculeArchitect.CONSTRUCTION_MODE_STICKS)	// no atom picking for wires
//			for (int atom=0; atom<conformer.getSize(); atom++)
//				addSphere(atom, -1, conformer.getCoordinates(atom), -1, -1);
	}

	@Override
	public void init() {}

	@Override
	public void addSphere(int role, Coordinates c, double radius, int argb) {
		Node node = mNodeMap.get(role);

		if (node != null) {
			node.setTranslateX(c.x);
			node.setTranslateY(c.y);
			node.setTranslateZ(c.z);

			if (mArchitect.getConstructionMode() == MoleculeArchitect.CONSTRUCTION_MODE_STICKS && (role & MoleculeBuilder.ROLE_IS_ATOM) != 0) {
				// update coordinates of transparent spheres
				node = mNodeMap.get(role | 0x80000000);
				if (node != null) {
					node.setTranslateX(c.x);
					node.setTranslateY(c.y);
					node.setTranslateZ(c.z);
				}
			}
		}
	}

	@Override
	public void addCylinder(int role, double radius, double length, Coordinates center, double rotationY, double rotationZ, int argb) {
		Node node = mNodeMap.get(role);
		if (node != null) {
			((Cylinder)node).setHeight(length);
			node.setTranslateX(center.x);
			node.setTranslateY(center.y);
			node.setTranslateZ(center.z);

			Transform r1 = new Rotate(90+180/Math.PI*rotationY, Rotate.X_AXIS);
			Transform r2 = new Rotate(90+180/Math.PI*rotationZ, Rotate.Z_AXIS);
			node.getTransforms().clear();
			node.getTransforms().add(r2);
			node.getTransforms().add(r1);
		}
	}

	@Override
	public void done() {

	}
}

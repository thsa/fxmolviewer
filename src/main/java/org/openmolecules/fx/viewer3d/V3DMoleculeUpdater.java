package org.openmolecules.fx.viewer3d;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.phesa.VolumeGaussian;
import com.actelion.research.chem.phesa.pharmacophore.pp.PPGaussian;

import javafx.scene.Node;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;

import org.openmolecules.fx.viewer3d.nodes.NodeDetail;
import org.openmolecules.fx.viewer3d.nodes.VolumeSphere;
import org.openmolecules.fx.viewer3d.nodes.AbstractPPNode;
import org.openmolecules.render.*;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * The V3DMoleculeUpdater uses the MoleculeArchitect with the same mode that has been used earlier to construct the V3DMolecule.
 * This class, however, does not build a molecule. It uses the coordinates of the stream of sphere and cylinder build instructions
 * to update the coordinates of the nodes of an existing V3DMolecule.
 * This class assumes that the order of instructions matches the constructions during build and, thus, can be used
 * to update the coordinates of all V3DMolecule's nodes, which were put into the children's list in build order.
 */
public class V3DMoleculeUpdater implements MoleculeBuilder, PharmacophoreBuilder, TorsionStrainVisBuilder {
	private final MoleculeArchitect mArchitect;
	private final PharmacophoreArchitect mPPArchitect;
	private final TorsionStrainVisArchitect mTSVArchitect;
	private final V3DMolecule mV3DMolecule;
	private final TreeMap<Integer,Node> mNodeMap;
	private final Map<PPGaussian,AbstractPPNode> mPPNodeMap;
	private final Map<VolumeGaussian,VolumeSphere> mVolNodeMap ;

	public V3DMoleculeUpdater(V3DMolecule fxmol) {
		mArchitect = new MoleculeArchitect(this);
		mPPArchitect = new PharmacophoreArchitect(this);
		mArchitect.setConstructionMode(fxmol.getConstructionMode());
		mV3DMolecule = fxmol;
		mTSVArchitect = new TorsionStrainVisArchitect(this);
		mNodeMap = new TreeMap<Integer,Node>();
		mPPNodeMap = new HashMap<PPGaussian,AbstractPPNode>();
		mVolNodeMap = new HashMap<VolumeGaussian,VolumeSphere>();
		for (Node node:fxmol.getChildren()) {
			NodeDetail detail = (NodeDetail)node.getUserData();
			if (detail != null
			 && (detail.isAtom() || detail.isBond() || detail.isTorsion())) {
				mNodeMap.put(detail.getRole(), node);
			}
		}
		
		for(V3DRotatableGroup group : fxmol.getGroups()) {
			if(group instanceof V3DCustomizablePheSA) {
				V3DCustomizablePheSA pharmacophore = (V3DCustomizablePheSA)group;
				for (Node node : pharmacophore.getChildren()) {
					if (node instanceof AbstractPPNode)
						mPPNodeMap.put(((AbstractPPNode)node).getPPGaussian(), (AbstractPPNode)node);
					if (node instanceof VolumeSphere)
						mVolNodeMap.put(((VolumeSphere)node).getVolumeGaussian(), (VolumeSphere)node);
				}
			}
		}
	}

	public void update() {
		mArchitect.buildMolecule(mV3DMolecule.getMolecule());
		for(V3DRotatableGroup group : mV3DMolecule.getGroups()) {
			if(group instanceof V3DCustomizablePheSA) {
				V3DCustomizablePheSA pharmacophore = (V3DCustomizablePheSA) group;
				mPPArchitect.buildPharmacophore(pharmacophore.getMolVol(), 0);
			}
		}
		if(mV3DMolecule.getTorsionStrainVis()!=null && mV3DMolecule.getTorsionStrainVis().getTorsionAnalyzer()!=null)
			mTSVArchitect.buildTorsionStrainColors(mV3DMolecule.getTorsionStrainVis().getTorsionAnalyzer());
	}

//		if (mArchitect.getConstructionMode() == MoleculeArchitect.CONSTRUCTION_MODE_STICKS)	// no atom picking for wires
//			for (int atom=0; atom<conformer.getSize(); atom++)
//				addSphere(atom, -1, conformer.getCoordinates(atom), -1, -1);

	@Override
	public void init() {}

	@Override
	public void done() {}

	@Override
	public void addAtomSphere(int role, Coordinates c, double radius, int argb) {
		Node node = mNodeMap.get(role);

		if (node != null) {
			node.setTranslateX(c.x);
			node.setTranslateY(c.y);
			node.setTranslateZ(c.z);

			if (mArchitect.getConstructionMode() == MoleculeArchitect.CONSTRUCTION_MODE_STICKS && RoleHelper.isAtom(role)) {
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
	public void addBondCylinder(int role, double radius, double length, Coordinates center, double rotationY, double rotationZ, int argb) {
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
	public void addAtomCone(int role, double radius, double length, Coordinates center, double rotationY, double rotationZ, int argb) {
		Node node = mNodeMap.get(role);
		if (node != null) {
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
	public void addPharmacophorePoint(PPGaussian ppg) {
		AbstractPPNode node = mPPNodeMap.get(ppg);
		if(node!=null) {
			node.update();
		}
		// TODO Auto-generated method stub
	}

	@Override
	public void addExclusionSphere(VolumeGaussian eg) {
		VolumeSphere node = mVolNodeMap.get(eg);
		if(node!=null) {
			node.update();
		}
	}

	@Override
	public void addTorsionCylinder(int role, double radius, double length, Coordinates c, double rotationY,
			double rotationZ, int strain) {
		Node node = mNodeMap.get(role);
		length*=TorsionStrainVisualization.LENGTH_SCALE;
		if (node != null) {
			((Cylinder)node).setHeight(length);
			node.setTranslateX(c.x);
			node.setTranslateY(c.y);
			node.setTranslateZ(c.z);

			Transform r1 = new Rotate(90+180/Math.PI*rotationY, Rotate.X_AXIS);
			Transform r2 = new Rotate(90+180/Math.PI*rotationZ, Rotate.Z_AXIS);
			node.getTransforms().clear();
			node.getTransforms().add(r2);
			node.getTransforms().add(r1);
		}
		PhongMaterial mat = TorsionStrainVisualization.getStrainMaterial(strain);
		NodeDetail detail = (NodeDetail)node.getUserData();
		node.setUserData(new NodeDetail(mat, role, detail.mayOverrideMaterial()));
		((Cylinder)node).setMaterial(mat);
	}
}

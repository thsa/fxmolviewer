package org.openmolecules.fx.viewer3d.interactions.rf;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.util.DoubleFormat;
import javafx.geometry.Point3D;
import org.openmolecules.chem.interaction.AtomClassifier;
import org.openmolecules.chem.interaction.rf.*;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.interactions.V3DInteraction;
import org.openmolecules.fx.viewer3d.interactions.V3DInteractionCalculator;
import org.openmolecules.fx.viewer3d.interactions.V3DInteractionPoint;
import org.openmolecules.fx.viewer3d.interactions.V3DInteractionSite;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class RFInteractionCalculator implements V3DInteractionCalculator {
	@Override
	public String getInteractionTypeName() {
		return "RF";
	}

	@Override
	public List<V3DInteractionPoint> determineInteractionPoints(V3DMolecule fxmol) {
		AtomClassifier classifier = (fxmol.getRole() == V3DMolecule.MoleculeRole.MACROMOLECULE) ?
				new RFProteinAtomClassifier() : new RFLigandAtomClassifier();
		int[] type = classifier.classifyAtoms(fxmol.getMolecule());
		ArrayList<V3DInteractionPoint> list = new ArrayList<>();
		for (int atom=0; atom<type.length; atom++)
			list.add(new V3DInteractionPoint(fxmol, atom, type[atom]));
		return list;
	}

	@Override
	public void determineInteractions(V3DInteractionSite is1, V3DInteractionSite is2, TreeMap<Integer, ArrayList<V3DInteraction>> interactionMap) {
		interactionMap.clear();
//		interactionMap.put(0, new ArrayList<>());	// we don't distinguish interaction types

		V3DInteractionSite proteinSite;
		V3DInteractionSite ligandSite;
		if (is1.getFXMol().getRole() == V3DMolecule.MoleculeRole.MACROMOLECULE
		 && is2.getFXMol().getRole() == V3DMolecule.MoleculeRole.LIGAND) {
			proteinSite = is1;
			ligandSite = is2;
		} else if (is2.getFXMol().getRole() == V3DMolecule.MoleculeRole.MACROMOLECULE
				&& is1.getFXMol().getRole() == V3DMolecule.MoleculeRole.LIGAND) {
			proteinSite = is2;
			ligandSite = is1;
		} else {
			return;
		}
		StereoMolecule protein = proteinSite.getFXMol().getMolecule().getCompactCopy();
		for (int atom = 0; atom<protein.getAllAtoms(); atom++) {
			Coordinates c = protein.getAtomCoordinates(atom);
			Point3D p = proteinSite.getFXMol().localToParent(c.x, c.y, c.z);
			c.set(p.getX(), p.getY(), p.getZ());
		}
		protein.ensureHelperArrays(Molecule.cHelperNeighbours);
		StereoMolecule ligand = ligandSite.getFXMol().getMolecule().getCompactCopy();
		for (int atom = 0; atom<ligand.getAllAtoms(); atom++) {
			Coordinates c = ligand.getAtomCoordinates(atom);
			Point3D p = ligandSite.getFXMol().localToParent(c.x, c.y, c.z);
			c.set(p.getX(), p.getY(), p.getZ());
		}
		ligand.ensureHelperArrays(Molecule.cHelperNeighbours);

		RFInteractionList interactionList = new RFInteractionList(ligand, protein, false);

		V3DInteractionPoint[] ligandIP = new V3DInteractionPoint[ligand.getAtoms()];
		for (V3DInteractionPoint ip : ligandSite.getSites())
			ligandIP[ip.getAtom()] = ip;
		V3DInteractionPoint[] proteinIP = new V3DInteractionPoint[protein.getAtoms()];
		for (V3DInteractionPoint ip : proteinSite.getSites())
			proteinIP[ip.getAtom()] = ip;

		ArrayList<V3DInteraction> list = new ArrayList<>();
		for (RFInteraction interaction : interactionList) {
			double rf = RFKnowledgeBase.getRFValue(interaction.getLType(), interaction.getPType());
			if (rf > 0)
				list.add(new RFInteractionV3D(proteinIP[interaction.getPAtom()], ligandIP[interaction.getLAtom()], interaction, rf));
		}

		interactionMap.put(0, list);
	}

	@Override
	public String getInteractionInfo(V3DInteraction interaction, int remoteIndex, boolean isl2P) {
		RFInteraction rfi = ((RFInteractionV3D)interaction).getRFInteraction();
		return getAtomTypeName(interaction.getInteractionPoint(remoteIndex).getType(), isl2P)
		+ ", rf:" + DoubleFormat.toString(interaction.getValue(),3)
		+ ", dist:" + DoubleFormat.toString(interaction.getDistance(), 3)
		+ ", ang:" + Math.round(180*(isl2P ? rfi.getL2PAngle() : rfi.getP2LAngle())/Math.PI)
		+ ", tor:" + Math.round(180*(isl2P ? rfi.getL2PTorsion() : rfi.getP2LTorsion())/Math.PI);
	}

	@Override
	public String getAtomTypeName(int type, boolean isProtein) {
		return isProtein ? RFProteinAtomClassifier.getDefaultInstance().getAtomTypeName(type)
						 : RFLigandAtomClassifier.getDefaultInstance().getAtomTypeName(type);
	}
}

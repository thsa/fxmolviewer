package org.openmolecules.fx.viewer3d.interactions.rf;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;
import org.openmolecules.chem.interaction.AtomClassifier;
import org.openmolecules.chem.interaction.rf.RFLigandAtomClassifier;
import org.openmolecules.chem.interaction.rf.RFProteinAtomClassifier;
import org.openmolecules.chem.interaction.rf.RFInteractionList;
import org.openmolecules.chem.interaction.rf.RFKnowledgeBase;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.interactions.V3DInteraction;
import org.openmolecules.fx.viewer3d.interactions.V3DInteractionCalculator;
import org.openmolecules.fx.viewer3d.interactions.V3DInteractionPoint;
import org.openmolecules.fx.viewer3d.interactions.V3DInteractionSites;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class RFInteractionCalculator implements V3DInteractionCalculator {
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
	public void determineInteractions(V3DInteractionSites is1, V3DInteractionSites is2, TreeMap<Integer, ArrayList<V3DInteraction>> interactionMap) {
		interactionMap.clear();
		interactionMap.put(0, new ArrayList<>());	// we don't distinguish interaction types

		V3DInteractionSites proteinSites;
		V3DInteractionSites ligandSites;
		if (is1.getFXMol().getRole() == V3DMolecule.MoleculeRole.MACROMOLECULE
		 && is2.getFXMol().getRole() == V3DMolecule.MoleculeRole.LIGAND) {
			proteinSites = is1;
			ligandSites = is2;
		}
		else if (is2.getFXMol().getRole() == V3DMolecule.MoleculeRole.MACROMOLECULE
			  && is1.getFXMol().getRole() == V3DMolecule.MoleculeRole.LIGAND) {
			proteinSites = is2;
			ligandSites = is1;
		}
		else {
			return;
		}
		StereoMolecule protein = proteinSites.getFXMol().getMolecule().getCompactCopy();
		for (int atom=0; atom<protein.getAllAtoms(); atom++) {
			Coordinates c = protein.getAtomCoordinates(atom);
			Point3D p = proteinSites.getFXMol().localToParent(c.x, c.y, c.z);
			c.set(p.getX(), p.getY(), p.getZ());
		}
		protein.ensureHelperArrays(Molecule.cHelperNeighbours);
		StereoMolecule ligand = ligandSites.getFXMol().getMolecule().getCompactCopy();
		for (int atom=0; atom<ligand.getAllAtoms(); atom++) {
			Coordinates c = ligand.getAtomCoordinates(atom);
			Point3D p = ligandSites.getFXMol().localToParent(c.x, c.y, c.z);
			c.set(p.getX(), p.getY(), p.getZ());
		}
		ligand.ensureHelperArrays(Molecule.cHelperNeighbours);

		RFInteractionList interactionList = new RFInteractionList(ligand, protein, false);

		V3DInteractionPoint[] ligandIP = new  V3DInteractionPoint[ligand.getAtoms()];
		for (V3DInteractionPoint ip : ligandSites.getSites())
			ligandIP[ip.getAtom()] = ip;
		V3DInteractionPoint[] proteinIP = new  V3DInteractionPoint[protein.getAtoms()];
		for (V3DInteractionPoint ip : proteinSites.getSites())
			proteinIP[ip.getAtom()] = ip;

		ArrayList<V3DInteraction> list = new ArrayList<>();
		for (RFInteractionList.RFInteraction interaction : interactionList) {
			double rf = RFKnowledgeBase.getRFValue(interaction.lType, interaction.pType);
			double strength = 2*Math.abs(Math.log10(rf));
			if (strength < 0.9 || strength > 1.11) {
				Color color = (rf < 0.9) ? Color.RED.darker() : (rf < 1.1) ? Color.GRAY : Color.BLUE.brighter();
				double distance = ligand.getAtomCoordinates(interaction.lAtom).distance(protein.getAtomCoordinates(interaction.pAtom));
				list.add(new V3DInteraction(proteinIP[interaction.pAtom], ligandIP[interaction.lAtom], 0, distance, 0, strength, color));
			}
		}
		interactionMap.put(0, list);
	}
}

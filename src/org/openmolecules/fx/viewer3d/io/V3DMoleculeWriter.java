package org.openmolecules.fx.viewer3d.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DScene;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.MolfileCreator;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.io.DWARFileCreator;
import com.actelion.research.chem.phesa.DescriptorHandlerShape;
import com.actelion.research.chem.phesa.MolecularVolume;
import com.actelion.research.chem.phesa.PheSAMolecule;

import javafx.scene.Node;

public class V3DMoleculeWriter {
	
	public static void saveAllVisibleMols(String pathToFile, V3DScene scene) {
		V3DMolecule fxmol;
		ArrayList<V3DMolecule> fxmolList = new ArrayList<V3DMolecule>();
		for (Node node : scene.getWorld().getChildren()) {
			if (node instanceof V3DMolecule && node.isVisible()) {
				if (node.isVisible()) {
					fxmol = (V3DMolecule)node;
					fxmol.addImplicitHydrogens();
					fxmolList.add(fxmol);
				}
			}
		}
	Writer filewriter;
	try {
		filewriter = new FileWriter(pathToFile);
		for(int i=0;i<fxmolList.size();i++){
			try { 
			MolfileCreator mfc = new MolfileCreator(fxmolList.get(i).getMolecule());
			mfc.writeMolfile(filewriter);
			}
			catch(Exception e) {
				continue;
			}
		}
	} catch (IOException e1) {

	}

	}
	
	public static void savePhesaQueries(File file, List<V3DMolecule> fxmols) {
		try {
			DWARFileCreator creator = new DWARFileCreator(new BufferedWriter(new FileWriter(file)));
			int structureColumn = creator.addStructureColumn("Structure", "IDcode");
			int threeDColumn = creator.add3DCoordinatesColumn("Coordinates3D", structureColumn);
			int pheSAColumn = creator.addDescriptorColumn(DescriptorConstants.DESCRIPTOR_ShapeAlignSingleConf.shortName,
					DescriptorConstants.DESCRIPTOR_ShapeAlignSingleConf.version,
					structureColumn);
			creator.writeHeader(-1);
			DescriptorHandlerShape dhs = new DescriptorHandlerShape();
			for(V3DMolecule fxmol : fxmols) {
				if(fxmol.getPharmacophore()==null)
					fxmol.addPharmacophore();
				ArrayList<MolecularVolume> molVols = new ArrayList<MolecularVolume>();
				StereoMolecule mol = new StereoMolecule(fxmol.getMolecule());
				mol.ensureHelperArrays(Molecule.cHelperNeighbours);
				int[] hydrogens1 = new int[mol.getAllAtoms()-mol.getAtoms()];
				int k=0;
				for(int a=0;a<mol.getAtoms();a++) { 
					for(int j=mol.getConnAtoms(a);j<mol.getAllConnAtoms(a);j++) {
						hydrogens1[k] = mol.getConnAtom(a, j);
						k++;
					}
				}
				Canonizer can = new Canonizer(mol, Canonizer.COORDS_ARE_3D);
				StereoMolecule mol2 = can.getCanMolecule(true);
				mol2.ensureHelperArrays(Molecule.cHelperNeighbours);
				int[] heavyAtomMap = can.getGraphIndexes();
				int[] hydrogens2 = new int[mol.getAllAtoms()-mol.getAtoms()];
				k=0;
				for(int a : heavyAtomMap) {
					for(int j=mol2.getConnAtoms(a);j<mol2.getAllConnAtoms(a);j++) {
						hydrogens2[k] = mol2.getConnAtom(a, j);
						k++;
					}
				}
					
				int[] atomMap = new int[mol.getAllAtoms()];
				for(int i=0; i<hydrogens1.length;i++)
					atomMap[hydrogens1[i]] = hydrogens2[i];
				for(int i=0; i<heavyAtomMap.length;i++)
					atomMap[i] = heavyAtomMap[i];
				MolecularVolume molVolOut = new MolecularVolume(fxmol.getPharmacophore().getMolVol());
				molVols.add(molVolOut);
				molVolOut.updateAtomIndeces(atomMap);
				PheSAMolecule shapeMol = new PheSAMolecule(mol2,molVols);
				String PheSAString = dhs.encode(shapeMol);
				String idcoords = can.getEncodedCoordinates(true);
				String idcode = can.getIDCode();
				creator.setRowCoordinates(idcoords, threeDColumn );
				creator.setRowStructure(idcode, structureColumn);
				creator.setRowValue(PheSAString, pheSAColumn);
				creator.writeCurrentRow();
			}
			creator.writeEnd();
			
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

}

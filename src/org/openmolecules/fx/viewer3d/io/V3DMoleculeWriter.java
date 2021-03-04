package org.openmolecules.fx.viewer3d.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openmolecules.fx.viewer3d.V3DCustomizablePheSA;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DScene;

import com.actelion.research.calc.Matrix;
import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.MolfileCreator;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.io.DWARFileCreator;
import com.actelion.research.chem.phesa.DescriptorHandlerShape;
import com.actelion.research.chem.phesa.MolecularVolume;
import com.actelion.research.chem.phesa.PheSAAlignment;
import com.actelion.research.chem.phesa.PheSAMolecule;

import javafx.geometry.Point3D;
import javafx.scene.Node;

public class V3DMoleculeWriter {
	
	public static void saveMolList(String pathToFile, List<V3DMolecule> mols) {
		Writer filewriter;
		String delim = System.getProperty("line.separator");
		if(pathToFile.endsWith(".sdf")) {
			try {
				filewriter = new FileWriter(pathToFile);
				for(int i=0;i<mols.size();i++){
					try { 
						StereoMolecule mol = getMolWithGlobalCoordinates(mols.get(i));
						MolfileCreator mfc = new MolfileCreator(mol);
						mfc.writeMolfile(filewriter);
						filewriter.write(delim);
						filewriter.write("$$$$");
						filewriter.write(delim);
						filewriter.flush();
					}
					catch(Exception e) {
						continue;
					}
				}
			} catch (IOException e1) {
		
			}
		}
	
		}
	
	public static void savePhesaQueries(File file, V3DCustomizablePheSA pheSAModel) {
		try {
			StereoMolecule origMol = ((V3DMolecule)(pheSAModel.getParent())).getMolecule();
			DWARFileCreator creator = new DWARFileCreator(new BufferedWriter(new FileWriter(file)));
			int structureColumn = creator.addStructureColumn("Structure","IDcode");
			int threeDColumn = creator.add3DCoordinatesColumn("idcoordinates3D", structureColumn);
			int pheSAColumn = creator.addDescriptorColumn(DescriptorConstants.DESCRIPTOR_ShapeAlign.shortName,
					DescriptorConstants.DESCRIPTOR_ShapeAlignSingleConf.version,
					structureColumn);
			creator.writeHeader(-1);
			DescriptorHandlerShape dhs = new DescriptorHandlerShape();
			MolecularVolume molVol = pheSAModel.getMolVol();
			ArrayList<MolecularVolume> molVols = new ArrayList<MolecularVolume>();
			StereoMolecule mol = new StereoMolecule(origMol);
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
			MolecularVolume molVolOut = new MolecularVolume(molVol);
			molVolOut.updateAtomIndeces(atomMap);
			Conformer conf = new Conformer(mol2);
			PheSAAlignment.preProcess(conf, molVolOut);
			
			molVols.add(molVolOut);
			for(int a=0;a<mol2.getAllAtoms();a++) {
				Coordinates newCoords = new Coordinates(conf.getCoordinates(a));
				mol2.setAtomX(a, newCoords.x);
				mol2.setAtomY(a, newCoords.y);
				mol2.setAtomZ(a, newCoords.z);
			}
			PheSAMolecule shapeMol = new PheSAMolecule(mol2,molVols);
			String PheSAString = dhs.encode(shapeMol);
			can = new Canonizer(mol2, Canonizer.COORDS_ARE_3D);
			String idcoords = can.getEncodedCoordinates(true);
			String idcode = can.getIDCode();
			creator.setRowCoordinates(idcoords, threeDColumn );
			creator.setRowStructure(idcode, structureColumn);
			creator.setRowValue(PheSAString, pheSAColumn);
			creator.writeCurrentRow();
			creator.writeEnd();

			
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}
	
	/**
	 * transform molecule from local coordinates to scene coordinates
	 * @param fxmol
	 * @return
	 */
	private static StereoMolecule getMolWithGlobalCoordinates(V3DMolecule fxmol) {
		StereoMolecule origMol = fxmol.getMolecule();
		StereoMolecule mol = new StereoMolecule(origMol);
		mol.ensureHelperArrays(Molecule.cHelperCIP);
		for(int a=0;a<mol.getAllAtoms();a++) {
			Point3D pointLocal = new Point3D(mol.getAtomX(a), mol.getAtomY(a), mol.getAtomZ(a));
			Point3D point = fxmol.localToScene(pointLocal);
			mol.setAtomX(a, point.getX());
			mol.setAtomY(a, point.getY());
			mol.setAtomZ(a, point.getZ());
		}
		return mol;
	}

}

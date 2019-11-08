package org.openmolecules.fx.viewer3d.io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.openmolecules.chem.conf.gen.ConformerGenerator;
import org.openmolecules.fx.viewer3d.V3DMolecule;

import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.io.CompoundFileParser;
import com.actelion.research.chem.io.DWARFileParser;
import com.actelion.research.chem.io.DWARFileParser.SpecialField;
import com.actelion.research.chem.io.SDFileParser;
import com.actelion.research.chem.phesa.DescriptorHandlerShape;
import com.actelion.research.chem.phesa.DescriptorHandlerShapeOneConf;
import com.actelion.research.chem.phesa.MolecularVolume;
import com.actelion.research.chem.phesa.PheSAMolecule;

public class V3DMoleculeParser {
	
	public static enum Input {REQUIRE_3D, PREFER_3D, GENERATE_CONFS};
	
	private static ArrayList<StereoMolecule> parseFile(String file) {
		ArrayList<StereoMolecule> mols = new ArrayList<StereoMolecule>();
		CompoundFileParser parser;
		StereoMolecule mol;
		ConformerGenerator confGen = new ConformerGenerator();
		if(file.endsWith(".sdf")) {
			parser = new SDFileParser(file);
		}
		else if(file.endsWith(".dwar")) {
			parser = new DWARFileParser(file);
		}
		else {
			parser = null;
		}
		if(parser!=null) {
			boolean notDone = parser.next();
			while(notDone) {
				try {
					mol = parser.getMolecule();
					mol.ensureHelperArrays(Molecule.cHelperCIP);
					boolean has3Dcoordinates = V3DMoleculeParser.hasMolecule3DCoords(mol);
					if(!has3Dcoordinates) {
						mol = confGen.getOneConformerAsMolecule(mol);
					}
					mols.add(parser.getMolecule());
					notDone = parser.next();
				}
				catch(Exception e) {
					notDone = parser.next();
				}
			}
		}
			return mols;
	}


	public static List<V3DMolecule> readMolFile(String sdfile, int group) {
		List<StereoMolecule> mols = parseFile(sdfile);
		List<V3DMolecule> v3dMols = new ArrayList<V3DMolecule>();
		for(StereoMolecule mol: mols) {
			if(mol==null) 
				continue;
			mol.ensureHelperArrays(Molecule.cHelperRings);
			if(mol.getName()==null || mol.getName().equals(""))
				mol.setName("Molecule");
			v3dMols.add(new V3DMolecule(mol, V3DMolecule.getNextID(), group,V3DMolecule.MoleculeRole.LIGAND));
		}
		return v3dMols;
	}
	
	public static List<PheSAMolecule> readPhesaScreeningLib(File pheSAFile, boolean takeExisting3D) {
		if (pheSAFile.getName().endsWith(".sdf")) 
			return V3DMoleculeParser.readPhesaScreeningLibSDF(pheSAFile, takeExisting3D);
		else if (pheSAFile.getName().endsWith(".dwar")) 
			return V3DMoleculeParser.readPhesaScreeningLibDWAR(pheSAFile, takeExisting3D);
		else 
			return new ArrayList<PheSAMolecule>();
	}
	
	
	private static List<PheSAMolecule> readPhesaScreeningLibSDF(File pheSAFile, boolean takeExisting3D) {
		List<PheSAMolecule> screeningLibrary = new ArrayList<PheSAMolecule>();
		DescriptorHandlerShape dhs = new DescriptorHandlerShape();
		DescriptorHandlerShapeOneConf dhsOneConf = new DescriptorHandlerShapeOneConf();
		List<StereoMolecule> mols = V3DMoleculeParser.parseFile(pheSAFile.getAbsolutePath());
		PheSAMolecule shapeMol;
		for(StereoMolecule mol : mols ) {
			if(takeExisting3D)
				shapeMol = dhsOneConf.createDescriptor(mol);
			else
				shapeMol = dhs.createDescriptor(mol);
			screeningLibrary.add(shapeMol);
		}
		return screeningLibrary;
	}

	
	private static List<PheSAMolecule> readPhesaScreeningLibDWAR(File pheSAFile, boolean takeExisting3D) {
		List<PheSAMolecule> screeningLibrary = new ArrayList<PheSAMolecule>();
		DescriptorHandlerShape dhs = new DescriptorHandlerShape();
		DescriptorHandlerShapeOneConf dhsOneConf = new DescriptorHandlerShapeOneConf();
		DWARFileParser dwParser = new DWARFileParser(pheSAFile);
		PheSAMolecule shapeMol;
		boolean notDone = dwParser.next();
		while(notDone) {
			try {
				StereoMolecule mol = dwParser.getMolecule();
				if(takeExisting3D) {
					shapeMol = dhsOneConf.createDescriptor(mol);
				}
				else {
					SpecialField pheSAField = dwParser.getSpecialFieldMap().get(DescriptorConstants.DESCRIPTOR_ShapeAlign.shortName);
					if (pheSAField == null) {
						shapeMol = dhs.createDescriptor(mol);
					}
					else {
						String pheSAString = dwParser.getSpecialFieldData(pheSAField.fieldIndex);
						shapeMol = dhs.decode(pheSAString);
					}
				}
				screeningLibrary.add(shapeMol);
				notDone = dwParser.next();
			}
			catch(Exception e) {
				notDone = dwParser.next();
			}
		}
		return screeningLibrary;
	
	}
	
	public static List<V3DMolecule> readPheSAQuery(File pheSAFile, int group) {
		DescriptorHandlerShape dhs = new DescriptorHandlerShape();
		DWARFileParser dwParser = new DWARFileParser(pheSAFile, DWARFileParser.MODE_COORDINATES_REQUIRE_3D);
		List<V3DMolecule> fxMols = new ArrayList<V3DMolecule>();
		boolean notDone = dwParser.next();
		while(notDone) {
			try {
				StereoMolecule mol = dwParser.getMolecule();
				SpecialField pheSAField = dwParser.getSpecialFieldMap().get(DescriptorConstants.DESCRIPTOR_ShapeAlignSingleConf.shortName);
				String pheSAString = dwParser.getSpecialFieldData(pheSAField.fieldIndex);
				PheSAMolecule shapeMol = dhs.decode(pheSAString);
				if(shapeMol.getVolumes().size()==1) {
					MolecularVolume molVol = shapeMol.getVolumes().get(0);
					molVol.update(mol);
					V3DMolecule fxMol = new V3DMolecule(mol, V3DMolecule.getNextID(), group,V3DMolecule.MoleculeRole.LIGAND);
					fxMol.addPharmacophore(molVol);
					fxMols.add(fxMol);
				}
				notDone = dwParser.next();
			}
			catch(Exception e) {
				notDone = false;
			}
		}
		return fxMols;
	
	}
	
	private static boolean hasMolecule3DCoords(StereoMolecule mol) {
		boolean has3Dcoordinates = false;
		for (int atom=1; atom<mol.getAllAtoms(); atom++) {
			if (Math.abs(mol.getAtomZ(atom) - mol.getAtomZ(0)) > 0.1) {
				has3Dcoordinates = true;
				break;
			}
		}
		return has3Dcoordinates;
	}
	

}

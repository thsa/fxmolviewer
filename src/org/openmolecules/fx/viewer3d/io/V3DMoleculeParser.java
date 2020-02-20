package org.openmolecules.fx.viewer3d.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import com.actelion.research.chem.MolfileParser;
import org.openmolecules.chem.conf.gen.ConformerGenerator;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DScene;

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
		StereoMolecule mol;

		if(file.endsWith(".mol")) {
			try {
				mol = new MolfileParser().getCompactMolecule(new BufferedReader(new FileReader(file)));
				if (mol != null) {
					if (!mol.is3D())
						new ConformerGenerator().getOneConformerAsMolecule(mol);
					mols.add(mol);
				}
			}
			catch (FileNotFoundException fnfe) {}
		}
		else {
			CompoundFileParser parser = null;

			if(file.endsWith(".sdf"))
				parser = new SDFileParser(file);
			else if(file.endsWith(".dwar"))
				parser = new DWARFileParser(file);

			if(parser!=null) {
				ConformerGenerator confGen = new ConformerGenerator();
				while(parser.next()) {
					try {
						mol = parser.getMolecule();
						if (mol != null) {
							if(!mol.is3D())
								confGen.getOneConformerAsMolecule(mol);
							mols.add(mol);
						}
					}
					catch(Exception e) {}
				}
			}
		}
		return mols;
	}


	public static List<V3DMolecule> readMoleculeFile(V3DScene scene, String filename, int group) {
		List<StereoMolecule> mols = parseFile(filename);
		List<V3DMolecule> v3dMols = new ArrayList<V3DMolecule>();
		for(StereoMolecule mol: mols) {
			mol.ensureHelperArrays(Molecule.cHelperRings);
			if(mol.getName()==null || mol.getName().equals(""))
				mol.setName("Molecule");
			v3dMols.add(new V3DMolecule(mol, V3DMolecule.getNextID(), group,V3DMolecule.MoleculeRole.LIGAND,scene.mayOverrideHydrogenColor()));
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
	
	public static List<V3DMolecule> readPheSAQuery(V3DScene scene, File pheSAFile, int group) {
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
					V3DMolecule fxMol = new V3DMolecule(mol, V3DMolecule.getNextID(), group,V3DMolecule.MoleculeRole.LIGAND, scene.mayOverrideHydrogenColor());
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
}

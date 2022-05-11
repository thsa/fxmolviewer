package org.openmolecules.fx.viewer3d.io;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.MolfileParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.ConformerSet;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.io.CompoundFileParser;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.io.DWARFileParser;
import com.actelion.research.chem.io.DWARFileParser.SpecialField;
import com.actelion.research.chem.io.Mol2FileParser;
import com.actelion.research.chem.io.SDFileParser;
import com.actelion.research.chem.io.pdb.parser.PDBFileParser;
import com.actelion.research.chem.io.pdb.parser.StructureAssembler;
//import com.actelion.research.chem.phesa.ShapeVolume;
import com.actelion.research.chem.phesa.DescriptorHandlerShape;
import com.actelion.research.chem.phesa.MolecularVolume;
import com.actelion.research.chem.phesa.PheSAMolecule;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.openmolecules.chem.conf.gen.ConformerGenerator;
import org.openmolecules.fx.viewer3d.V3DMolGroup;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DScene;

import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class V3DMoleculeParser {
	
	public static enum Input {REQUIRE_3D, PREFER_3D, GENERATE_CONFS};
	public static final String CONFORMER_SET = "conformerSet";
	
	
	
	public static List<StereoMolecule> parseChemFile(String file) {
		List<StereoMolecule> mols = new ArrayList<>();
		if(file.endsWith(".mol") ) {
			try {
				StereoMolecule mol;
				mol = new MolfileParser().getCompactMolecule(new BufferedReader(new FileReader(file)));
				mol.ensureHelperArrays(Molecule.cHelperRings);
				if(mol.getName()==null || mol.getName().equals(""))
					mol.setName("Molecule");
				if (mol != null) {
					if (!mol.is3D())
						new ConformerGenerator().getOneConformerAsMolecule(mol);

						IntStream.range(0, mol.getAllAtoms()).forEach(a -> {
							Coordinates c = mol.getCoordinates(a);
							c.y = -c.y;
							c.z = -c.z;
						});
					
					mols.add(mol);
				}
				
			}
			catch (FileNotFoundException fnfe) {}
		}
		else if(file.endsWith(".mol2") ) {
			try {
				StereoMolecule mol;
				mol = new Mol2FileParser().load(file);
				mol.ensureHelperArrays(Molecule.cHelperRings);
				if(mol.getName()==null || mol.getName().equals(""))
					mol.setName("Molecule");
				if (mol != null) {
					mols.add(mol);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			CompoundFileParser parser = null;
	
			if(file.endsWith(".sdf"))
				parser = new SDFileParser(file);
			else if(file.endsWith(".dwar"))
				parser = new DWARFileParser(file,DWARFileParser.MODE_COORDINATES_PREFER_3D);
	
			if(parser!=null) {
				StereoMolecule mol;
				ConformerGenerator confGen = new ConformerGenerator();
				while(parser.next()) {
					try {
						mol = parser.getMolecule();
						if (mol != null) {
							mol.ensureHelperArrays(Molecule.cHelperRings);
							if(mol.getName()==null || mol.getName().equals(""))
								mol.setName("Molecule");
							if(!mol.is3D())
								confGen.getOneConformerAsMolecule(mol);
							mols.add(mol);
						}
					}
					catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return mols;
		
	}
	

	
	private static void parseFile(V3DScene scene,String file) {
		List<V3DMolGroup> molGroups = new ArrayList<V3DMolGroup>();

		if(file.endsWith(".mol") || file.endsWith(".mol2") || file.endsWith(".sdf") || file.endsWith(".dwar") 
				|| file.endsWith(".mol") ) {
			List<StereoMolecule> mols = parseChemFile(file);

			mols.stream().forEach(e -> scene.addMolecule(new V3DMolecule(e, V3DMolecule.getNextID(),V3DMolecule.MoleculeRole.LIGAND,false)));
		}
		else if(file.endsWith(".pdb")) {
			try {
				V3DMolGroup pdbGroup = new V3DMolGroup(new File(file).getName().split("\\.")[0]);
				scene.addMolGroup(pdbGroup);
				PDBFileParser parser = new PDBFileParser();
				parser.parse(new File(file)).extractMols().forEach((k,v) -> {
					List<V3DMolecule> groupMols = new ArrayList<V3DMolecule>();
					V3DMolecule.MoleculeRole role;
					boolean isProtein = false;
					if(k.equals(StructureAssembler.SOLVENT_GROUP)) 
						role = V3DMolecule.MoleculeRole.SOLVENT;
					else if(k.equals(StructureAssembler.LIGAND_GROUP)) 
						role = V3DMolecule.MoleculeRole.LIGAND;
					else {
						role =  V3DMolecule.MoleculeRole.MACROMOLECULE;
						isProtein = true;
					}
					V3DMolGroup molGroup = new V3DMolGroup(k.toUpperCase());
					v.forEach(e -> {
						if(role==V3DMolecule.MoleculeRole.SOLVENT) 
							e.setName("HOH" + " " + e.getAtomChainId(0));
						else if (role==V3DMolecule.MoleculeRole.LIGAND)
							e.setName(e.getAtomAmino(0) + " " + e.getAtomChainId(0));
						else 
							e.setName("PROT");
						V3DMolecule fxmol;
						e.ensureHelperArrays(Molecule.cHelperCIP);
						fxmol = new V3DMolecule(e, V3DMolecule.getNextID(),role,false);
						groupMols.add(fxmol);
						
					});
					pdbGroup.addMolGroup(molGroup);
					for(V3DMolecule fxmol : groupMols) {
						scene.addMolecule(fxmol, molGroup);
						if(isProtein) 
							Platform.runLater(() -> fxmol.setColor(Color.DARKGRAY));
						
					}
					
				molGroups.add(pdbGroup);
				});

			}
			catch (FileNotFoundException fnfe) {}
			catch (ParseException pe) {}
			catch (IOException ie) {}
		}
	
	}


	public static void readMoleculeFile(V3DScene scene, String filename) {
		parseFile(scene,filename);
	}
	
	
	public static List<V3DMolecule> readPheSAQuery(V3DScene scene, File pheSAFile, int group) {
		DescriptorHandlerShape dhs = new DescriptorHandlerShape();
		DWARFileParser dwParser = new DWARFileParser(pheSAFile, DWARFileParser.MODE_COORDINATES_REQUIRE_3D);
		List<V3DMolecule> fxMols = new ArrayList<V3DMolecule>();
		boolean notDone = dwParser.next();
		while(notDone) {
			try {
				StereoMolecule mol = dwParser.getMolecule();
				if(mol.getName()==null)
					mol.setName("Molecule");
				SpecialField pheSAField = dwParser.getSpecialFieldMap().get(DescriptorConstants.DESCRIPTOR_ShapeAlign.shortName);
				if(pheSAField!=null) {
					String pheSAString = dwParser.getSpecialFieldData(pheSAField.fieldIndex);
					PheSAMolecule shapeMol = dhs.decode(pheSAString);
					for(MolecularVolume molVol : shapeMol.getVolumes()) {
						StereoMolecule m = shapeMol.getConformer(molVol);
						m.setName(mol.getName());
						V3DMolecule fxMol = new V3DMolecule(m, V3DMolecule.getNextID(),V3DMolecule.MoleculeRole.LIGAND, scene.mayOverrideHydrogenColor());
						fxMol.addPharmacophore(molVol);
						fxMols.add(fxMol);
					}
				}
				else {
					SpecialField confField = dwParser.getSpecialFieldMap().get(CONFORMER_SET);
					if(confField!=null) {
						String confSetString = dwParser.getSpecialFieldData(confField .fieldIndex);
						ConformerSet confSet = new ConformerSet(confSetString);
						PheSAMolecule pheSAMol = dhs.createDescriptor(confSet);
						for(MolecularVolume molVol : pheSAMol.getVolumes()) {
							StereoMolecule m = pheSAMol.getConformer(molVol);
							m.setName(mol.getName());
							V3DMolecule fxMol = new V3DMolecule(m, V3DMolecule.getNextID(),V3DMolecule.MoleculeRole.LIGAND, scene.mayOverrideHydrogenColor());
							fxMol.addPharmacophore(molVol);
							fxMols.add(fxMol);
						}
					}
					
				}
				notDone = dwParser.next();
			}
			catch(Exception e) {
				notDone = false;
			}
		}
		return fxMols;
	
	}
	/*
	public static List<V3DMolecule> readNegReceptorImage(V3DScene scene, File imgFile, int group){
		DWARFileParser dwParser = new DWARFileParser(imgFile, DWARFileParser.MODE_COORDINATES_REQUIRE_3D);
		List<V3DMolecule> fxMols = new ArrayList<V3DMolecule>();
		boolean notDone = dwParser.next();
		while(notDone) {
			try {
				StereoMolecule mol = dwParser.getMolecule();
				if(mol.getName()==null)
					mol.setName("Molecule");
				SpecialField negRecImgField = dwParser.getSpecialFieldMap().get(CompoundTableConstants.cColumnTypeNegRecImage);
				String negRecImgString = dwParser.getSpecialFieldData(negRecImgField.fieldIndex);
				ShapeVolume bsVol = ShapeVolume.decode(negRecImgString);
				V3DMolecule fxMol = new V3DMolecule(mol, V3DMolecule.getNextID(),V3DMolecule.MoleculeRole.MACROMOLECULE, scene.mayOverrideHydrogenColor());
				fxMol.addNegativeReceptorImage(bsVol);
				fxMols.add(fxMol);
				
				notDone = dwParser.next();
			}
			catch(Exception e) {
				notDone = false;
			}
		}
		return fxMols;
		
		
	}
	*/
}

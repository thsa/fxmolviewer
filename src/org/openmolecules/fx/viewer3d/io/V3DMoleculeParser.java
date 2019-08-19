package org.openmolecules.fx.viewer3d.io;

import java.util.ArrayList;

import org.openmolecules.fx.viewer3d.V3DMolecule;

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.io.CompoundFileParser;
import com.actelion.research.chem.io.DWARFileParser;
import com.actelion.research.chem.io.SDFileParser;

public class V3DMoleculeParser {
	
	private static ArrayList<StereoMolecule> parseFile(String file) {
		ArrayList<StereoMolecule> mols = new ArrayList<StereoMolecule>();
		CompoundFileParser parser;
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
					mols.add(parser.getMolecule());
					notDone = parser.next();
				}
				catch(Exception e) {
					notDone = false;
				}
			}
		}
			return mols;
	}


	public static V3DMolecule[] readMolFile(String sdfile) {
		ArrayList<StereoMolecule> mols = parseFile(sdfile);
		V3DMolecule[] v3d_mols = new V3DMolecule[mols.size()];
		int i = 0;
		for(StereoMolecule mol: mols) {
			if(mol==null) continue;
			if(mol.getName()==null || mol.getName().equals(""))
				mol.setName("Molecule");
			v3d_mols[i] = new V3DMolecule(mol);
			i++;

		}
		return v3d_mols;
	}
	
	

}

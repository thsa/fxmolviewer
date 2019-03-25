package org.openmolecules.fx.viewer3d.tools;

import com.actelion.research.chem.io.SDFileParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.Conformer;
import java.util.ArrayList;
import org.openmolecules.fx.viewer3d.V3DMolecule;


public class MoleculeFileReader {
	
	public ArrayList<StereoMolecule> parseSDFile(String sdfile) {
		ArrayList<StereoMolecule> mols = new ArrayList<StereoMolecule>();
		SDFileParser sdfp = new SDFileParser(sdfile);
		boolean notDone = sdfp.next();
		while(notDone) {
			try {
				mols.add(sdfp.getMolecule());
				notDone = sdfp.next();
			}
			catch(Exception e) {
				notDone = false;
			}
		}
		return mols;
	}
	
	
	public V3DMolecule[] readMolFile(String sdfile) {
		ArrayList<StereoMolecule> mols = parseSDFile(sdfile);
		V3DMolecule[] v3d_mols = new V3DMolecule[mols.size()];
		int i = 0;
		for(StereoMolecule mol: mols) {
			v3d_mols[i] = new V3DMolecule(mol);
			i++;
			
		}
		return v3d_mols;
	}
}

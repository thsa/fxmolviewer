package org.openmolecules.fx.tasks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DScene;
import org.openmolecules.fx.viewer3d.io.V3DMoleculeParser;
import org.openmolecules.fx.viewer3d.io.V3DMoleculeWriter;

import com.actelion.research.calc.Matrix;
import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.io.DWARFileCreator;
import com.actelion.research.chem.phesa.DescriptorHandlerShape;
import com.actelion.research.chem.phesa.MolecularVolume;
import com.actelion.research.chem.phesa.PheSAAlignment;
import com.actelion.research.chem.phesa.PheSAMolecule;

import javafx.scene.Node;

public class V3DPheSAScreener {
	
	private List<PheSAMolecule> mBaseMols;
	private List<PheSAMolecule> mQueryMols;
	private List<V3DMolecule> mFXQueries;
	private File mInputDWFile;
	
	private V3DPheSAScreener(V3DScene scene, File dwarFile) {
		mQueryMols = new ArrayList<PheSAMolecule>();
		mFXQueries = new ArrayList<V3DMolecule>();
		mInputDWFile = dwarFile;
		V3DMolecule fxmol;
		MolecularVolume molVol;
		for (Node node : scene.getWorld().getChildren()) {
			if (node instanceof V3DMolecule) {
					fxmol = (V3DMolecule)node;
					if(fxmol.getMolecule().getAtoms()>100) {
						V3DShapeAlignerInPlace.molSizeAlert.showAndWait();
						return;
					}
					else if(fxmol.isIncluded()) {
						if(fxmol.getPharmacophore()==null) 
							fxmol.addPharmacophore();
						molVol = fxmol.getPharmacophore().getMolVol();
						PheSAAlignment.preProcess(fxmol.getMolecule(), molVol);
						mQueryMols.add(new PheSAMolecule(fxmol.getMolecule(),molVol));
						mFXQueries.add(fxmol);
					}
							
						
			}
		}
		
		
	}
	
	public static void screen(V3DScene scene, File dwarFile) {
		V3DPheSAScreener screener = new V3DPheSAScreener(scene,dwarFile);
		Thread thread = new Thread(() -> screener.run());
		thread.start();
	}
	
	private void run() {
		File queryFile = new File(mInputDWFile.getParentFile(),"queries.dwar");
		V3DMoleculeWriter.savePhesaQueries(queryFile, mFXQueries);
		mBaseMols = V3DMoleculeParser.readPhesaScreeningLib(mInputDWFile, false);
		int nQueries = mQueryMols.size();
		int nBases = mBaseMols.size();
		double[][] simis = new double[nQueries][nBases];
		StereoMolecule[][] alignedMols = new StereoMolecule[nQueries][nBases];
		DescriptorHandlerShape dhs = new DescriptorHandlerShape();
		for(int i=0; i<nQueries; i++){
			for (int j=0; j<nBases; j++) {
				PheSAMolecule query = mQueryMols.get(i);
				PheSAMolecule base = mBaseMols.get(j);
				simis[i][j] = dhs.getSimilarity(query, base);
				alignedMols[i][j] = dhs.getPreviousAlignment()[1];
			}
		}
		for(int i=0;i<nQueries;i++) {
			File resultFile = new File(mInputDWFile.getParentFile(),"PheSA_Screen" + Integer.toString(i) + ".dwar");
			writeResult(resultFile,mBaseMols,simis[i],alignedMols[i]);
		}
		
	}
	
	private void writeResult(File output, List<PheSAMolecule> baseMols, double[] simis, StereoMolecule[] alignments) {
		DWARFileCreator creator;
		try {
			creator = new DWARFileCreator(new BufferedWriter(new FileWriter(output)));
			int structureColumn = creator.addStructureColumn("Structure", "IDcode");
			int threeDColumn = creator.add3DCoordinatesColumn("Coordinates3D", structureColumn);
			int simiColumn = creator.addAlphanumericalColumn("Similarity");
			int nameColumn = creator.addAlphanumericalColumn("Molecule Name");
			creator.addColumnProperty(structureColumn, "idColumn", "Molecule Name");
			creator.writeHeader(-1);
			for(int i=0;i<baseMols.size();i++) {
				Canonizer can = new Canonizer(alignments[i]);
				String idcode = can.getIDCode();
				String idcoords = can.getEncodedCoordinates();
				double simi = simis[i];
				creator.setRowCoordinates(idcoords, threeDColumn );
				creator.setRowStructure(idcode, structureColumn);
				creator.setRowValue(Double.toString(simi),simiColumn);
				creator.setRowValue(baseMols.get(i).getMolecule().getName(),nameColumn);
				creator.writeCurrentRow();
			}
			creator.writeEnd();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		

	}
	
	
		
		

		


	
	
}

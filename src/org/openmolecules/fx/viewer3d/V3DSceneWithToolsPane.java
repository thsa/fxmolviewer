package org.openmolecules.fx.viewer3d;

import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.Conformer;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.openmolecules.chem.conf.gen.ConformerGenerator;
import org.openmolecules.fx.viewer3d.panel.ToolsPane;

import java.util.ArrayList;

public class V3DSceneWithToolsPane extends V3DSceneWithSidePane  {
	
	private ToolsPane mToolsPane;
	private Stage mPrimaryStage;
	private TextArea mOutputLog;
	
	public V3DSceneWithToolsPane(final Stage primaryStage) {
		super();
		mPrimaryStage = primaryStage;
		mToolsPane = new ToolsPane(mPrimaryStage,this);
		mOutputLog = new TextArea();
		mOutputLog.setEditable(false);
		setRight(mToolsPane);
		setBottom(mOutputLog);
	}
	
	public void minimizeVisibleMols() {
		createOutput("start minimization");
		ArrayList<V3DMolecule> toMinimize = new ArrayList<V3DMolecule>();
		for (Node node : super.getScene3D().getWorld().getChildren())
			if (node instanceof V3DMolecule) {
				if (node.isVisible()) {
					V3DMolecule v3dMol = (V3DMolecule) node;
					toMinimize.add(v3dMol);
				}
			}
		for(V3DMolecule mol : toMinimize) {
			Conformer conf = mol.getConformer();
			StereoMolecule minMol = conf.getMolecule();
			int oldAtoms = minMol.getAllAtoms();
			for(int i=0;i<minMol.getAllAtoms();i++) {
				minMol.setAtomX(i, conf.getX(i));
				minMol.setAtomY(i, conf.getY(i));
				minMol.setAtomZ(i, conf.getZ(i));
			}
			ConformerGenerator.addHydrogenAtoms(minMol);

			// TODO add hydrogens rather than replace molecule
			if (oldAtoms != minMol.getAllAtoms())
				mol.setConformer(new Conformer(minMol));
		}
		V3DMinimizationHandler minimizer = new V3DMinimizationHandler(toMinimize, this);
		minimizer.minimise();
	}
	
	public void createOutput(String output) {
		mOutputLog.appendText(output);
		mOutputLog.appendText("\n");
	}
	
	public V3DScene getScene3D() {
		return super.getScene3D();
	}
}

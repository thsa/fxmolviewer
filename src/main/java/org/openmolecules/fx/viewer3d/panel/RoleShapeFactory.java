package org.openmolecules.fx.viewer3d.panel;

import org.openmolecules.fx.viewer3d.V3DMolecule;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class RoleShapeFactory {
		
		private static final Color PROTEIN_COLOR = Color.rgb(0,134,173);
		private static final Color LIGAND_COLOR = Color.rgb(0,194,199);
		private static final Color COFACTOR_COLOR = Color.rgb(151,235,219);
		private static final Color SOLVENT_COLOR = Color.rgb(0,102,204);
		

			
		
		public static Node fromRole(V3DMolecule.MoleculeRole role) {
			Node shape;
			switch(role) {
				case MACROMOLECULE:
					shape = createProteinShape();
					break;
				case LIGAND:
					shape = createLigandShape();
					break;
				case COFACTOR:
					shape = createCofactorShape();
					break;
				case SOLVENT:
					shape = createSolventShape();
					break;
				default:
					shape = createLigandShape();
					break;
			}
			return shape;
		}
		
		private RoleShapeFactory() {}
		
		private static Node createProteinShape() {
			StackPane stackPane = new StackPane();
			Rectangle protein = new Rectangle();
			protein.setHeight(15);
			protein.setWidth(15);
			protein.setFill(PROTEIN_COLOR);
			Label label = new Label("P");
			stackPane.getChildren().addAll(protein,label);
			return stackPane;
			
		}
		
		private static Node createLigandShape() {
			StackPane stackPane = new StackPane();
			Rectangle ligand = new Rectangle();
			ligand.setHeight(15);
			ligand.setWidth(15);
			Label label = new Label("L");
			stackPane.getChildren().addAll(ligand,label);
			ligand.setFill(LIGAND_COLOR);
		    return stackPane;
			
		}
		
		private static Node createCofactorShape() {
			StackPane stackPane = new StackPane();
			Rectangle cofactor = new Rectangle();
			cofactor.setHeight(15);
			cofactor.setWidth(15);
			Label label = new Label("C");
			stackPane.getChildren().addAll(cofactor,label);
			cofactor.setFill(COFACTOR_COLOR);
	        return stackPane;
		}
		
		private static Node createSolventShape() {
			StackPane stackPane = new StackPane();
			Rectangle solvent = new Rectangle();
			solvent.setHeight(15);
			solvent.setWidth(15);
			Label label = new Label("S");
			label.setTextFill(Color.WHITE);
			stackPane.getChildren().addAll(solvent,label);
			solvent.setFill(SOLVENT_COLOR);
			return stackPane;
		}
					


}

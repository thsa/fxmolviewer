package org.openmolecules.fx.viewer3d.panel;

import org.openmolecules.fx.viewer3d.V3DMolecule;

import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

public class RoleShapeFactory {
		
		private static final Color PROTEIN_COLOR = Color.rgb(0,134,173);
		private static final Color LIGAND_COLOR = Color.rgb(0,194,199);
		private static final Color COFACTOR_COLOR = Color.rgb(151,235,219);
		private static final Color SOLVENT_COLOR = Color.rgb(218,248,227);
		

			
		
		public static Shape fromRole(V3DMolecule.MoleculeRole role) {
			Shape shape;
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
		
		private static Shape createProteinShape() {
			Arc protein = new Arc();
			protein.setRadiusX(7.5);
			protein.setRadiusY(7.5);
			protein.setStartAngle(90.0f); 
			protein.setLength(270.0f);
			protein.setFill(PROTEIN_COLOR);
			protein.setType(ArcType.ROUND);
		    return protein;
			
		}
		
		private static Shape createLigandShape() {
			Rectangle ligand = new Rectangle();
			ligand.setHeight(15);
			ligand.setWidth(15);
			ligand.setFill(LIGAND_COLOR);
		    return ligand;
			
		}
		
		private static Shape createCofactorShape() {
			Polygon cofactor = new Polygon();
			cofactor.getPoints().addAll(new Double[]{
	                0.0, 15.0,
	                15.0, 0.0,
	                15.0, 15.0 });
			cofactor.setFill(COFACTOR_COLOR);
	        return cofactor;
		}
		
		private static Shape createSolventShape() {
			Circle solvent = new Circle();
			solvent.setRadius(7.5);
			solvent.setFill(SOLVENT_COLOR);
			return solvent;
		}
					


}

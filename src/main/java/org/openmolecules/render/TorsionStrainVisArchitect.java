package org.openmolecules.render;

import org.openmolecules.fx.viewer3d.torsionstrain.V3DTorsionStrainAnalyzer;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.StereoMolecule;

public class TorsionStrainVisArchitect {
	
	private static final double CYLINDER_RADIUS = 0.4;
	private V3DTorsionStrainAnalyzer torsionStrainAnalyzer;
	private TorsionStrainVisBuilder builder;
	
	public TorsionStrainVisArchitect(TorsionStrainVisBuilder builder) {
		this.builder = builder;
	}
	
	public void buildTorsionStrainColors( V3DTorsionStrainAnalyzer torsionStrainAnalyzer) {
		this.torsionStrainAnalyzer = torsionStrainAnalyzer;
		StereoMolecule mol = torsionStrainAnalyzer.getV3DMolecule().getMolecule();
		for(int bond : torsionStrainAnalyzer.getRotBonds()) {
			int atom1 = mol.getBondAtom(0, bond);
			int atom2 = mol.getBondAtom(1, bond);
			Coordinates c1 = mol.getCoordinates(atom1);
			Coordinates c2 = mol.getCoordinates(atom2);
			Coordinates center = new Coordinates();
			Coordinates delta = new Coordinates();
			center.center(c1, c2);
			delta.set(c2).sub(c1);

			double d = delta.getLength();
			double dxy = Math.sqrt(delta.x * delta.x + delta.y * delta.y);
			double b = Math.asin(c2.z > c1.z ? dxy / d : -dxy / d);
			double c = (delta.x < 0.0) ? Math.atan(delta.y / delta.x) + Math.PI
					: (delta.x > 0.0) ? Math.atan(delta.y / delta.x)
					: (delta.y > 0.0) ? Math.PI / 2 : -Math.PI / 2;
			
			buildStickBond(bond,d,b,c);
		}
		
		
	}
	
	private void buildStickBond(int bond, double d, double b, double c) {
		StereoMolecule mol = torsionStrainAnalyzer.getV3DMolecule().getMolecule();
		int color = torsionStrainAnalyzer.getStrainColor(bond);
		int atom1 = mol.getBondAtom(0, bond);
		int atom2 = mol.getBondAtom(1, bond);

		Coordinates p1 = mol.getCoordinates(atom1);
		Coordinates p2 = mol.getCoordinates(atom2);

		buildStickBond(bond,color,p1,p2,CYLINDER_RADIUS,d,b,c);
		
		}
	
	private void buildStickBond(int bond, int color, Coordinates p1, Coordinates p2,
	                            double r, double d, double b, double c) {
		Coordinates center = new Coordinates();
		center.center(p1, p2);
		int role = RoleHelper.createTorsionRole(bond );
		builder.addTorsionCylinder(role, r, d, center, b, c, color);

			
		}

}

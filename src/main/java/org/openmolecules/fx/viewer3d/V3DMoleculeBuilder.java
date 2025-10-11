/*
 * FXMolViewer, showing and manipulating molecules and protein structures in 3D.
 * Copyright (C) 2019 Thomas Sander

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * @author Thomas Sander
 */

package org.openmolecules.fx.viewer3d;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.StereoMolecule;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import org.openmolecules.fx.viewer3d.nodes.NodeDetail;
import org.openmolecules.mesh.Cone;
import org.openmolecules.render.ConstructionFilter;
import org.openmolecules.render.MoleculeArchitect;
import org.openmolecules.render.MoleculeBuilder;
import org.openmolecules.render.RangeConstructionFilter;

import java.util.ArrayList;

public class V3DMoleculeBuilder extends V3DPrimitiveBuilder implements MoleculeBuilder {
	private static final double STICK_MODE_ATOM_PICK_RADIUS = 0.35;
	private static final double THINSTICK_MODE_ATOM_PICK_RADIUS = 0.25;
	private static final double WIRE_MODE_ATOM_PICK_RADIUS = 0.25;
	private final MoleculeArchitect mArchitect;
	private final V3DMolecule mV3DMolecule;
	private int mSphereDivisions,mCylinderDivisions;

	/**
	 * Constructor for building a molecule
	 * @param v3DMolecule
	 */
	public V3DMoleculeBuilder(V3DMolecule v3DMolecule) {
		super(v3DMolecule);
		mArchitect = new MoleculeArchitect(this);
		mArchitect.setHydrogenMode(v3DMolecule.getHydrogenMode());
		mArchitect.setConstructionMode(v3DMolecule.getConstructionMode());
		mArchitect.setSplitAllBonds(v3DMolecule.isSplitAllBonds());
		mV3DMolecule = v3DMolecule;
		calculateDivisions();
		}

	public void setConstructionMode(int mode) {
		mArchitect.setConstructionMode(mode);
		calculateDivisions();
		}

	public void setHydrogenMode(int mode) {
		mArchitect.setHydrogenMode(mode);
		}

	public void buildMolecule() {
		buildMolecule(0, 0);
	}

	public void buildMolecule(ConstructionFilter cf) {
		mArchitect.buildMolecule(mV3DMolecule.getMolecule(), cf);
	}

	public void buildMolecule(int fromAtom, int fromBond) {
		StereoMolecule mol = mV3DMolecule.getMolecule();
		mArchitect.buildMolecule(mol, new RangeConstructionFilter(fromAtom, mol.getAllAtoms(), fromBond, mol.getAllBonds()));
		}
	
	public void buildMolecule(ArrayList<Integer> atoms, ArrayList<Integer> bonds) {
		mArchitect.buildMolecule(mV3DMolecule.getMolecule(), atoms, bonds);
		}

	private void addTransparentSphere(int role, Coordinates c, double radius) {
		Sphere sphere = (Sphere)super.addSphere(c, radius, 0, mSphereDivisions);
		sphere.setUserData(new NodeDetail((PhongMaterial)sphere.getMaterial(), role | 0x80000000, false));
		}

	@Override
	public void addAtomSphere(int role, Coordinates c, double radius, int argb) {
		Sphere sphere = (Sphere)super.addSphere(c, radius, argb, mSphereDivisions);
		boolean isOverridable = mV3DMolecule.overrideHydrogens() ?
							argb == MoleculeArchitect.getAtomicNoARGB(1)
						 || argb == MoleculeArchitect.getAtomicNoARGB(6)
						  : argb == MoleculeArchitect.getAtomicNoARGB(6);
		NodeDetail detail = new NodeDetail((PhongMaterial)sphere.getMaterial(), role, isOverridable);
		sphere.setUserData(detail);

		// dotted bonds also use addAtomSphere()...
		if (detail.isAtom()) {
			if (mV3DMolecule.getMolecule().isSelectedAtom(detail.getAtom())) {
				((NodeDetail)sphere.getUserData()).setSelected(true);
				mV3DMolecule.updateAppearance(sphere);
				}
			if (mArchitect.getConstructionMode() == MoleculeArchitect.CONSTRUCTION_MODE_STICKS)
				addTransparentSphere(role, c, STICK_MODE_ATOM_PICK_RADIUS);
			else if (mArchitect.getConstructionMode() == MoleculeArchitect.CONSTRUCTION_MODE_THINSTICKS)
				addTransparentSphere(role, c, THINSTICK_MODE_ATOM_PICK_RADIUS);
			}
		}

	@Override
	public void addBondCylinder(int role, double radius, double length, Coordinates center, double rotationY, double rotationZ, int argb) {
		Cylinder cylinder = (Cylinder)super.addCylinder(radius, length, center, rotationY, rotationZ, argb, mCylinderDivisions);
		boolean isOverridable = mV3DMolecule.overrideHydrogens() ?
							argb == MoleculeArchitect.getAtomicNoARGB(1)
						 || argb == MoleculeArchitect.getAtomicNoARGB(6)
						 || argb == MoleculeArchitect.BALL_AND_STICK_STICK_COLOR
						 :  argb == MoleculeArchitect.getAtomicNoARGB(6)
						 || argb == MoleculeArchitect.BALL_AND_STICK_STICK_COLOR;
		NodeDetail detail = new NodeDetail((PhongMaterial)cylinder.getMaterial(), role, isOverridable);
		cylinder.setUserData(detail);
		StereoMolecule mol = mV3DMolecule.getMolecule();
		int bond = detail.getBond();
		int bondAtom = detail.getBondAtom(mol);
		if ((bondAtom != -1 && mol.isSelectedAtom(bondAtom))
		 || (mol.isSelectedAtom(mol.getBondAtom(0, bond)) && mol.isSelectedAtom(mol.getBondAtom(1, bond)))) {
			detail.setSelected(true);
			mV3DMolecule.updateAppearance(cylinder);
			}
		}

	@Override
	public void addAtomCone(int role, double radius, double height, Coordinates center, double rotationY, double rotationZ, int argb) {
		Cone cone = (Cone)super.addCone(radius, height, center, rotationY, rotationZ, argb, 36);
		boolean isOverridable = mV3DMolecule.overrideHydrogens() ?
							argb == MoleculeArchitect.getAtomicNoARGB(1)
						 || argb == MoleculeArchitect.getAtomicNoARGB(6)
						 || argb == MoleculeArchitect.BALL_AND_STICK_STICK_COLOR
						 :  argb == MoleculeArchitect.getAtomicNoARGB(6)
						 || argb == MoleculeArchitect.BALL_AND_STICK_STICK_COLOR;
		NodeDetail detail = new NodeDetail((PhongMaterial)cone.getMaterial(), role, isOverridable);
		cone.setUserData(detail);
		if (detail.isAtom())
			detail.setSelected(mV3DMolecule.getMolecule().isSelectedAtom(detail.getAtom()));
	}

	private void calculateDivisions() {
		mSphereDivisions = (mArchitect.getConstructionMode() == MoleculeArchitect.CONSTRUCTION_MODE_BALLS) ? 64
				   : (mArchitect.getConstructionMode() == MoleculeArchitect.CONSTRUCTION_MODE_BALL_AND_STICKS) ? 32
				   : (mArchitect.getConstructionMode() == MoleculeArchitect.CONSTRUCTION_MODE_STICKS) ? 16 : 8;
		mCylinderDivisions = (mArchitect.getConstructionMode() == MoleculeArchitect.CONSTRUCTION_MODE_BALLS) ? 10
				: (mArchitect.getConstructionMode() == MoleculeArchitect.CONSTRUCTION_MODE_BALL_AND_STICKS) ? 10
				: (mArchitect.getConstructionMode() == MoleculeArchitect.CONSTRUCTION_MODE_STICKS) ? 10 : 5;
		}
	}

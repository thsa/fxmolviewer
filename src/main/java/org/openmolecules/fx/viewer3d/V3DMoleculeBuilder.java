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
import org.openmolecules.render.MoleculeArchitect;
import org.openmolecules.render.MoleculeBuilder;

import java.util.ArrayList;

public class V3DMoleculeBuilder extends V3DPrimitiveBuilder implements MoleculeBuilder {
	private static final double STICK_MODE_ATOM_PICK_RADIUS = 0.35;
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
		mArchitect.setShowSelection(true);
		mV3DMolecule = v3DMolecule;
		calculateDivisions();
		}

	public void setConstructionMode(int mode) {
		mArchitect.setConstructionMode(mode);
		calculateDivisions();
		}

	public void setHydrogenMode(MoleculeArchitect.HydrogenMode mode) {
		mArchitect.setHydrogenMode(mode);
		}

	public void buildMolecule() {
		buildMolecule(0, 0);
	}

	public void buildMolecule(int fromAtom, int fromBond) {
		mArchitect.buildMolecule(mV3DMolecule.getMolecule(), fromAtom, fromBond);
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
		sphere.setUserData(new NodeDetail((PhongMaterial)sphere.getMaterial(), role, isOverridable));

		// dotted bonds also use addAtomSphere()...
		if ((role & MoleculeBuilder.ROLE_IS_ATOM) != 0) {
			if (mV3DMolecule.getMolecule().isSelectedAtom(role & MoleculeBuilder.ROLE_INDEX_BITS)) {
				((NodeDetail)sphere.getUserData()).setSelected(true);
				mV3DMolecule.updateAppearance(sphere);
				}
			if (mArchitect.getConstructionMode() == MoleculeArchitect.CONSTRUCTION_MODE_STICKS)
				addTransparentSphere(role, c, STICK_MODE_ATOM_PICK_RADIUS);
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
		cylinder.setUserData(new NodeDetail((PhongMaterial)cylinder.getMaterial(), role, isOverridable));
		StereoMolecule mol = mV3DMolecule.getMolecule();
		int bond = role & MoleculeBuilder.ROLE_INDEX_BITS;
		if (mol.isSelectedAtom(mol.getBondAtom(0, bond)) && mol.isSelectedAtom(mol.getBondAtom(1, bond))) {
			((NodeDetail)cylinder.getUserData()).setSelected(true);
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
		cone.setUserData(new NodeDetail((PhongMaterial)cone.getMaterial(), role, isOverridable));
		if ((role & MoleculeBuilder.ROLE_IS_ATOM) != 0)
			((NodeDetail)cone.getUserData()).setSelected(mV3DMolecule.getMolecule().isSelectedAtom(role & MoleculeBuilder.ROLE_INDEX_BITS));
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

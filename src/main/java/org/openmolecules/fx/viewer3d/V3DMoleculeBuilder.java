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
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import org.openmolecules.fx.viewer3d.nodes.NodeDetail;
import org.openmolecules.mesh.Cone;
import org.openmolecules.render.MoleculeArchitect;
import org.openmolecules.render.MoleculeBuilder;

import java.util.ArrayList;
import java.util.TreeMap;

public class V3DMoleculeBuilder implements MoleculeBuilder {
	public static PhongMaterial sTransparentMaterial;
	private static final double STICK_MODE_ATOM_PICK_RADIUS = 0.35;
	private static final double WIRE_MODE_ATOM_PICK_RADIUS = 0.25;
	private static TreeMap<Integer,PhongMaterial> sMaterialMap;
	private MoleculeArchitect mArchitect;
	private V3DMolecule mV3DMolecule;
	private int mSphereDivisions,mCylinderDivisions;

	public V3DMoleculeBuilder(V3DMolecule v3DMolecule) {
		mArchitect = new MoleculeArchitect(this);
		mArchitect.setHydrogenMode(v3DMolecule.getHydrogenMode());
		mArchitect.setConstructionMode(v3DMolecule.getConstructionMode());
		calculateDivisions();
		mV3DMolecule = v3DMolecule;
		if (sMaterialMap == null) {
			sMaterialMap = new TreeMap<Integer, PhongMaterial>();
			sTransparentMaterial = new PhongMaterial();
			sTransparentMaterial.setDiffuseColor(Color.rgb(0, 0, 0, 0.0));
			}
		}

	public void setConstructionMode(MoleculeArchitect.ConstructionMode mode) {
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
		Sphere sphere = new Sphere(radius, mSphereDivisions);
		sphere.setMaterial(sTransparentMaterial);
		sphere.setTranslateX(c.x);
		sphere.setTranslateY(c.y);
		sphere.setTranslateZ(c.z);
		sphere.setUserData(new NodeDetail(sTransparentMaterial, role | 0x80000000, false));
		mV3DMolecule.getChildren().add(sphere);
		}

	@Override
	public void init() {}

	@Override
	public void addSphere(int role, Coordinates c, double radius, int argb) {
		boolean isOverridable;
		if(mV3DMolecule.overrideHydrogens()) 
			isOverridable = (argb == MoleculeArchitect.getAtomARGB(1)
						  || argb == MoleculeArchitect.getAtomARGB(6));
		else
			isOverridable = (argb == MoleculeArchitect.getAtomARGB(6));
		PhongMaterial material = getMaterial(argb);
		Sphere sphere = new Sphere(radius, mSphereDivisions);
		sphere.setMaterial(material);
		sphere.setTranslateX(c.x);
		sphere.setTranslateY(c.y);
		sphere.setTranslateZ(c.z);
		sphere.setUserData(new NodeDetail(material, role, isOverridable));
		mV3DMolecule.getChildren().add(sphere);

		if ((role & MoleculeBuilder.ROLE_IS_ATOM) != 0
		 && mArchitect.getConstructionMode() == MoleculeArchitect.ConstructionMode.STICKS)
			addTransparentSphere(role, c, STICK_MODE_ATOM_PICK_RADIUS);
		}

	@Override
	public void addCylinder(int role, double radius, double length, Coordinates center, double rotationY, double rotationZ, int argb) {
		boolean isOverridable;
		if(mV3DMolecule.overrideHydrogens()) 
			isOverridable = (argb == MoleculeArchitect.getAtomARGB(1)
							  || argb == MoleculeArchitect.getAtomARGB(6)
							  || argb == MoleculeArchitect.BALL_AND_STICK_STICK_COLOR);
		else
			isOverridable = (argb == MoleculeArchitect.getAtomARGB(6)
							  || argb == MoleculeArchitect.BALL_AND_STICK_STICK_COLOR);
		PhongMaterial material = getMaterial(argb);
		Cylinder cylinder = new Cylinder(radius, length, mCylinderDivisions);
		cylinder.setMaterial(material);
		cylinder.setTranslateX(center.x);
		cylinder.setTranslateY(center.y);
		cylinder.setTranslateZ(center.z);

		Transform r1 = new Rotate(90+180/Math.PI*rotationY, Rotate.X_AXIS);
		Transform r2 = new Rotate(90+180/Math.PI*rotationZ, Rotate.Z_AXIS);
		cylinder.getTransforms().add(r2);
		cylinder.getTransforms().add(r1);
		cylinder.setUserData(new NodeDetail(material, role, isOverridable));
		mV3DMolecule.getChildren().add(cylinder);
		}

	@Override
	public void addCone(int role, double radius, double length, Coordinates center, double rotationY, double rotationZ, int argb) {
		boolean isOverridable;
		if(mV3DMolecule.overrideHydrogens()) 
			isOverridable = (argb == MoleculeArchitect.getAtomARGB(1)
						  || argb == MoleculeArchitect.getAtomARGB(6));
		else
			isOverridable = (argb == MoleculeArchitect.getAtomARGB(6));
		PhongMaterial material = getMaterial(argb);
		Cone cone = new Cone(radius, length);
		cone.setMaterial(material);
		cone.setTranslateX(center.x);
		cone.setTranslateY(center.y);
		cone.setTranslateZ(center.z);

		Transform r1 = new Rotate(90+180/Math.PI*rotationY, Rotate.X_AXIS);
		Transform r2 = new Rotate(90+180/Math.PI*rotationZ, Rotate.Z_AXIS);
		cone.getTransforms().add(r2);
		cone.getTransforms().add(r1);
		cone.setUserData(new NodeDetail(material, role, isOverridable));
		mV3DMolecule.getChildren().add(cone);
	}

	@Override
	public void done() {}

	private void calculateDivisions() {
		mSphereDivisions = (mArchitect.getConstructionMode() == MoleculeArchitect.ConstructionMode.BALLS) ? 64
				   : (mArchitect.getConstructionMode() == MoleculeArchitect.ConstructionMode.BALL_AND_STICKS) ? 32
				   : (mArchitect.getConstructionMode() == MoleculeArchitect.ConstructionMode.STICKS) ? 16 : 8;
		mCylinderDivisions = (mArchitect.getConstructionMode() == MoleculeArchitect.ConstructionMode.BALLS) ? 10
				: (mArchitect.getConstructionMode() == MoleculeArchitect.ConstructionMode.BALL_AND_STICKS) ? 10
				: (mArchitect.getConstructionMode() == MoleculeArchitect.ConstructionMode.STICKS) ? 10 : 5;
		}

	public static PhongMaterial getMaterial(int argb) {
		PhongMaterial material = sMaterialMap.get(argb);
		if (material == null) {
			Color color = Color.rgb((argb & 0x00FF0000) >> 16, (argb & 0x0000FF00) >> 8, argb & 0x000000FF);
			material = new PhongMaterial();
			material.setDiffuseColor(color.darker());
			material.setSpecularColor(color);
			sMaterialMap.put(argb, material);
			}
		return material;
		}
	}

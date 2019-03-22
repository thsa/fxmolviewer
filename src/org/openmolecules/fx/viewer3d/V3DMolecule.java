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
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.Molecule3D;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.AtomAssembler;
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.util.DoubleFormat;
import javafx.collections.ObservableFloatArray;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import org.openmolecules.fx.surface.PolygonSurfaceCutter;
import org.openmolecules.fx.surface.RemovedAtomSurfaceCutter;
import org.openmolecules.fx.surface.SurfaceCutter;
import org.openmolecules.fx.surface.SurfaceMesh;
import org.openmolecules.mesh.MoleculeSurfaceAlgorithm;
import org.openmolecules.render.MoleculeArchitect;

import java.util.ArrayList;

import static org.openmolecules.fx.surface.SurfaceMesh.SURFACE_COLOR_PLAIN;

public class V3DMolecule extends RotatableGroup {
	private static final float HIGHLIGHT_SCALE = 1.2f;

	private static final double DEFAULT_SURFACE_TRANSPARENCY = 0.1;
	private static final int DEFAULT_SURFACE_COLOR_MODE = SURFACE_COLOR_PLAIN;

	public static final int SURFACE_NONE = 0;
	public static final int SURFACE_WIRES = 1;
	public static final int SURFACE_FILLED = 2;

	private static final Color HIGHLIGHT_COLOR = Color.RED;
	private static final Color PICKED_COLOR = Color.BLUEVIOLET;
	private static final Color DEFAULT_INHERITED_SURFACE_COLOR = Color.LIGHTGRAY;
	private static final Color DEFAULT_SURFACE_COLOR = Color.ROYALBLUE;
	private static final Color DISTANCE_COLOR = Color.TURQUOISE;
	private static final Color ANGLE_COLOR = Color.YELLOWGREEN;
	private static final Color TORSION_COLOR = Color.VIOLET;

	public enum MEASUREMENT { NONE, DISTANCE, ANGLE, TORSION }

	private static PhongMaterial sSolidHighlightedMaterial,sTransparentHighlightedMaterial,
			sPickedMaterial,sSelectedMaterial;

	private StereoMolecule	mMol;
	private Node			mLastPickedNode;
	private Shape3D			mHighlightedShape;
	private PhongMaterial	mOverrideMaterial;
	private MeshView[]		mSurface;
	private SurfaceMesh[]   mSurfaceMesh;
	private Color[]			mSurfaceColor;
	private int[]           mSurfaceMode,mSurfaceColorMode;
	private int				mConstructionMode,mHydrogenMode;
	private MEASUREMENT     mMeasurementMode;
	private ArrayList<Sphere> mPickedAtomList;
	private ArrayList<NonRotatingLabel> mLabelList;
	private boolean			mIsMouseDown,mOverrideCarbonOnly;
	private double[]        mSurfaceTransparency;

	/**
	 * Creates a V3DMolecule from the given molecule with the following default specification:<br>
	 * - construction mode: sticks<br>
	 * - default hydrogen mode, i.e. shows all explicit hydrogens<br>
	 * - shows no surface
	 * @param mol
	 */
	public V3DMolecule(StereoMolecule mol) {
		this(mol, MoleculeArchitect.CONSTRUCTION_MODE_STICKS);
		}

	/**
	 * Creates a V3DMolecule from the given molecule with the following default specification:<br>
	 * - default hydrogen mode, i.e. shows all explicit hydrogens<br>
	 * - shows no surface
	 * @param mol
	 * @param constructionMode one of MoleculeArchitect.CONSTRUCTION_MODE_ options
	 */
	public V3DMolecule(StereoMolecule mol, int constructionMode) {
		this(mol, constructionMode, MoleculeArchitect.HYDROGEN_MODE_DEFAULT);
	}

	/**
	 * Creates a V3DMolecule from the given molecule with generating and showing any surface
	 * @param mol
	 * @param constructionMode one of MoleculeArchitect.CONSTRUCTION_MODE_ options
	 * @param hydrogenMode one of MoleculeArchitect.HYDROGEN_MODE_ options
	 */
	public V3DMolecule(StereoMolecule mol, int constructionMode, int hydrogenMode) {
		this(mol, constructionMode, hydrogenMode, SURFACE_NONE,
				DEFAULT_SURFACE_COLOR_MODE, null, DEFAULT_SURFACE_TRANSPARENCY);
		}

	/**
	 * Creates a V3DMolecule from the given molecule with the given specification.
	 * @param mol
	 * @param constructionMode one of MoleculeArchitect.CONSTRUCTION_MODE_ options
	 * @param hydrogenMode one of MoleculeArchitect.HYDROGEN_MODE_ options
	 * @param surfaceMode SURFACE_NONE, SURFACE_WIRES, or SURFACE_FILLED
	 * @param surfaceColorMode DEFAULT_SURFACE_COLOR_MODE or one of the SurfaceMesh.SURFACE_COLOR_xxx modes
	 * @param surfaceColor null or explicit surface color used for some color modes
	 * @param transparency
	 */
	public V3DMolecule(StereoMolecule mol, int constructionMode, int hydrogenMode,
						int surfaceMode, int surfaceColorMode, Color surfaceColor, double transparency) {
		mMol = mol;
		mMeasurementMode = MEASUREMENT.NONE;
		mPickedAtomList = new ArrayList<>();
		mLabelList = new ArrayList<>();
		mConstructionMode = constructionMode;
		mHydrogenMode = hydrogenMode;

		int surfaceCount = MoleculeSurfaceAlgorithm.SURFACE_TYPE.length;
		mSurface = new MeshView[surfaceCount];
		mSurfaceMesh = new SurfaceMesh[surfaceCount];
		mSurfaceMode = new int[surfaceCount];
		mSurfaceColor = new Color[surfaceCount];
		mSurfaceColorMode = new int[surfaceCount];
		mSurfaceTransparency = new double[surfaceCount];

		mSurfaceMode[0] = surfaceMode;
		mSurfaceColor[0] = (surfaceColor == null) ? DEFAULT_SURFACE_COLOR : surfaceColor;
		mSurfaceColorMode[0] = surfaceColorMode;
		mSurfaceTransparency[0] = transparency;

		for (int i=1; i<surfaceCount; i++) {
			mSurfaceMode[i] = SURFACE_NONE;
			mSurfaceColor[i] = DEFAULT_SURFACE_COLOR;
			mSurfaceColorMode[i] = DEFAULT_SURFACE_COLOR_MODE;
			mSurfaceTransparency[i] = DEFAULT_SURFACE_TRANSPARENCY;
			}

		constructMaterials();
		V3DMoleculeBuilder builder = new V3DMoleculeBuilder(this);
// we cannot center pre-alligned conformers			builder.centerMolecule(conformer);
		builder.buildMolecule();

		if (surfaceMode != SURFACE_NONE) {
			mSurfaceMesh[0] = new SurfaceMesh(mMol, 0, surfaceColorMode, getNeutralColor(0), 1.0 - transparency, createSurfaceCutter());
			updateSurfaceFromMesh(0);
			}
		}

	public boolean addImplicitHydrogens() {
		int oldAtoms = mMol.getAllAtoms();
		int oldBonds = mMol.getAllBonds();

		int count = new AtomAssembler(mMol).addImplicitHydrogens();
		if (count == 0)
			return false;

		// mark new hydrogen atoms, if their neighbour atom is also marked
		mMol.ensureHelperArrays(Molecule.cHelperNeighbours);
		for (int i=oldAtoms; i<mMol.getAllAtoms(); i++)
			if (mMol.isMarkedAtom(mMol.getConnAtom(i, 0)))
				mMol.setAtomMarker(i, true);

		V3DMoleculeBuilder builder = new V3DMoleculeBuilder(this);
		builder.buildMolecule(oldAtoms, oldBonds);

		return true;
	}

	public StereoMolecule getMolecule() {
		return mMol;
	}

	/**
	 * @return center of gravity in local coordinate system
	 */
	private Point3D getCenterOfGravity() {
		double x = 0.0;
		double y = 0.0;
		double z = 0.0;
		int atomCount = 0;
		for (Node node:getChildren()) {
			NodeDetail detail = (NodeDetail)node.getUserData();
			if (detail != null && detail.isAtom()) {
				x += node.getTranslateX();
				y += node.getTranslateY();
				z += node.getTranslateZ();
				atomCount++;
			}
		}

		return new Point3D(x / atomCount, y / atomCount, z / atomCount);
	}

	@Override
	public void rotate(Rotate r) {
		if (mHighlightedShape != null) {
			double x = mHighlightedShape.getTranslateX();
			double y = mHighlightedShape.getTranslateY();
			double z = mHighlightedShape.getTranslateZ();

			Point3D p1 = getRotation().transform(x, y, z);
			Point3D p2 = r.transform(p1.getX(), p1.getY(), p1.getZ());

			super.rotate(r);

			setTranslateX(getTranslateX()+p1.getX()-p2.getX());
			setTranslateY(getTranslateY()+p1.getY()-p2.getY());
			setTranslateZ(getTranslateZ()+p1.getZ()-p2.getZ());
			}
		else {
			Point3D cog = getCenterOfGravity();
			Point3D p1 = getRotation().transform(cog);
			Point3D p2 = r.transform(p1.getX(), p1.getY(), p1.getZ());

			super.rotate(r);

			setTranslateX(getTranslateX()+p1.getX()-p2.getX());
			setTranslateY(getTranslateY()+p1.getY()-p2.getY());
			setTranslateZ(getTranslateZ()+p1.getZ()-p2.getZ());
			}
		}

	public int getConstructionMode() {
		return mConstructionMode;
		}

	public int getHydrogenMode() {
		return mHydrogenMode;
	}

	public void setConstructionMode(int mode) {
		setMode(mode, mHydrogenMode);
	}

	public void setHydrogenMode(int mode) {
		setMode(mConstructionMode, mode);
	}

	/**
	 * Defines the molecule construction mode (balls, sticks, balls & sticks, etc)
	 * @param constructionMode one of the MoleculeArchitect.CONSTRUCTION_MODE... options
	 * @param hydrogenMode one of the MoleculeArchitect.HYDROGEN_MODE... options
	 */
	public void setMode(int constructionMode, int hydrogenMode) {
		if (constructionMode != mConstructionMode
		 || hydrogenMode != mHydrogenMode) {
			mConstructionMode = constructionMode;
			mHydrogenMode = hydrogenMode;

			for (int i=getChildren().size()-1; i>=0; i--)
				if (!(getChildren().get(i) instanceof DashedRod)
				 && !(getChildren().get(i) instanceof MeshView))
					getChildren().remove(i);
			V3DMoleculeBuilder builder = new V3DMoleculeBuilder(this);
			builder.setConstructionMode(constructionMode);
			builder.setHydrogenMode(hydrogenMode);
			builder.buildMolecule();

			// rebuild surfaces after creation of molecule primitives, because surface transparency depends on creation time of triangles
			for (int i=0; i<mSurfaceMode.length; i++)
				if (mSurfaceMode[i] != SURFACE_NONE)
					updateSurfaceFromMesh(i);
			}
		}

	public MEASUREMENT getMeasurementMode() {
		return mMeasurementMode;
	}

	public void setMeasurementMode(MEASUREMENT measurement) {
		clearPickedAtomList();
		mMeasurementMode = measurement;
		}

	public Color getColor() {
		return (mOverrideMaterial == null) ? null : mOverrideMaterial.getDiffuseColor();
		}

	public void setColor(Color color, boolean carbonOnly) {
		if (color == null) {
			mOverrideMaterial = null;
			}
		else {
			mOverrideMaterial = new PhongMaterial();
			mOverrideMaterial.setDiffuseColor(color);
			mOverrideMaterial.setSpecularColor(color.darker());
			mOverrideCarbonOnly = carbonOnly;
			}

		for (int i=0; i<mSurface.length; i++) {
			if (mSurfaceMesh[i] != null
			 && (mSurfaceColorMode[i] == SurfaceMesh.SURFACE_COLOR_INHERIT
			  || mSurfaceColorMode[i] == SurfaceMesh.SURFACE_COLOR_ATOMIC_NOS)) {
				double opacity = 1.0 - mSurfaceTransparency[i];
				mSurfaceMesh[i].updateTexture(mMol, mSurfaceColorMode[i], color, opacity);

				PhongMaterial material;
				if (mSurfaceColorMode[i] == SurfaceMesh.SURFACE_COLOR_INHERIT) {
					material = createMaterial(getNeutralColor(i), opacity);
				}
				else {
					material = new PhongMaterial();
					material.setDiffuseMap(mSurfaceMesh[i].getTexture().getImage());
				}

				mSurface[i].setUserData(new NodeDetail(material, 0, true));
				mSurface[i].setMaterial(material);
			}
		}

		for (Node node:getChildren())
			updateAppearance(node);
		}

	public Color getSurfaceColor(int surfaceType) {
		return mSurfaceColor[surfaceType];
	}

		/**
		 * Updates the surface color mode to PLAIN and updates the surface to the given color.
		 * If surfaceMode==NONE the surfaceMode is set to FILL.
		 * @param surfaceType
		 * @param color
		 */
	public void setSurfaceColor(int surfaceType, Color color) {
		boolean colorChanged = !mSurfaceColor[surfaceType].equals(color);
		mSurfaceColor[surfaceType] = color;

		if (mSurfaceMode[surfaceType] == SURFACE_NONE)
			setSurface(surfaceType, SURFACE_FILLED, SURFACE_COLOR_PLAIN, mSurfaceTransparency[surfaceType]);
		else if (mSurfaceColorMode[surfaceType] != SURFACE_COLOR_PLAIN)
			setSurface(surfaceType, mSurfaceMode[surfaceType], SURFACE_COLOR_PLAIN, mSurfaceTransparency[surfaceType]);
		else if (colorChanged)
			updateSurfaceFromMesh(surfaceType);
	}

	public int getSurfaceColorMode(int surfaceType) {
		return mSurfaceColorMode[surfaceType];
	}

	/**
	 * Updates the surface mode keeping previously values for colorMode and transparency.
	 * @param surfaceType
	 * @param mode
	 */
	public void setSurfaceMode(int surfaceType, int mode) {
		if (mSurfaceMode[surfaceType] != mode)
			setSurface(surfaceType, mode, mSurfaceColorMode[surfaceType], mSurfaceTransparency[surfaceType]);
	}

	/**
	 * Updates the surface color mode and (if a surface exists) updates the surface.
	 * @param surfaceType
	 * @param colorMode
	 */
	public void setSurfaceColorMode(int surfaceType, int colorMode) {
		if (mSurfaceColorMode[surfaceType] != colorMode) {
			if (mSurfaceMode[surfaceType] == SURFACE_NONE)
				mSurfaceColorMode[surfaceType] = colorMode;
			else
				setSurface(surfaceType, mSurfaceMode[surfaceType], colorMode, mSurfaceTransparency[surfaceType]);
		}
	}

	public double getSurfaceTransparency(int surfaceType) {
		return mSurfaceTransparency[surfaceType];
	}

	public void setSurfaceTransparency(int surfaceType, double transparency) {
		if (mSurfaceTransparency[surfaceType] != transparency) {
			if (mSurface[surfaceType] != null) {
				mSurfaceMesh[surfaceType].updateTexture(mMol, mSurfaceColorMode[surfaceType], getNeutralColor(surfaceType), 1.0 - transparency);
				updateSurfaceFromMesh(surfaceType);
			}
			mSurfaceTransparency[surfaceType] = transparency;
		}
	}

	public int getSurfaceMode(int surfaceType) {
		return mSurfaceMode[surfaceType];
		}

	/**
	 * Use this method to create or remove a molecules surface
	 * @param surfaceType MoleculeSurfaceAlgorithm.CONNOLLY or LEE_RICHARDS
	 * @param surfaceMode SURFACE_NONE,SURFACE_WIRES, or SURFACE_FILLED
	 * @param colorMode one of the SurfaceMesh.SURFACE_COLOR_... options
	 * @param transparency
	 */
	public void setSurface(int surfaceType, int surfaceMode, int colorMode, double transparency) {
		if (surfaceMode == mSurfaceMode[surfaceType]
		 && colorMode == mSurfaceColorMode[surfaceType]
		 && transparency == mSurfaceTransparency[surfaceType])
			return;

		mSurfaceMode[surfaceType] = surfaceMode;
		mSurfaceColorMode[surfaceType] = colorMode;
		mSurfaceTransparency[surfaceType] = transparency;

		if (surfaceMode == SURFACE_NONE) {
			if (mSurface[surfaceType] != null) {
				getChildren().remove(mSurface[surfaceType]);
				mSurface[surfaceType] = null;
				mSurfaceMesh[surfaceType] = null;
			}
		} else {
			double opacity = 1.0 - transparency;
			if (mSurfaceMesh[surfaceType] == null) {
				mSurfaceMesh[surfaceType] = new SurfaceMesh(mMol, surfaceType, colorMode, getNeutralColor(surfaceType), opacity, createSurfaceCutter());
				}
			else
				mSurfaceMesh[surfaceType].updateTexture(mMol, colorMode, getNeutralColor(surfaceType), opacity);

			updateSurfaceFromMesh(surfaceType);
//showSurfaceNormals();
		}
	}

	/**
	 * If we have cropped the molecule/protein and therefore have open valences filled with
	 * pseudo atoms (atomicNo=0), then we don't want those molecule parts to be covered
	 * by the surface mesh. We use a surface cutter to remove this after complete surface creation.
	 * @return null or SurfaceCutter to be used by SurfaceMesh after fresh mesh creation
	 */
	private SurfaceCutter createSurfaceCutter() {
		for (int atom=0; atom<mMol.getAllAtoms(); atom++)
			if (mMol.getAtomicNo(atom) == 0)
				return new RemovedAtomSurfaceCutter(this);

		return null;
	}

	private Color getNeutralColor(int surfaceType) {
		if (mSurfaceColorMode[surfaceType] == SURFACE_COLOR_PLAIN)
			return mOverrideMaterial != null ? mOverrideMaterial.getDiffuseColor() : mSurfaceColor[surfaceType];

		if (mSurfaceColorMode[surfaceType] == SurfaceMesh.SURFACE_COLOR_INHERIT)
			return mOverrideMaterial != null ? mOverrideMaterial.getDiffuseColor() : DEFAULT_INHERITED_SURFACE_COLOR;

		return mOverrideMaterial != null ? mOverrideMaterial.getDiffuseColor() : mSurfaceColor[surfaceType];

// construct and return a less saturated color for other color modes
//		return Color.hsb(color.getHue(), color.getSaturation() / 3.0, color.getBrightness());
	}

	private void updateSurfaceFromMesh(int surfaceType) {
		if (mSurface[surfaceType] != null)
			getChildren().remove(mSurface[surfaceType]);

		mSurface[surfaceType] = new MeshView(mSurfaceMesh[surfaceType]);
		mSurface[surfaceType].setCullFace(CullFace.NONE);

		PhongMaterial material;
		if (mSurfaceColorMode[surfaceType] == SurfaceMesh.SURFACE_COLOR_INHERIT) {
			material = createMaterial(getNeutralColor(surfaceType), 1.0 - mSurfaceTransparency[surfaceType]);
		}
		else if (mSurfaceColorMode[surfaceType] == SURFACE_COLOR_PLAIN) {
			material = createMaterial(mSurfaceColor[surfaceType], 1.0 - mSurfaceTransparency[surfaceType]);
		} else {
			material = new PhongMaterial();
			material.setDiffuseMap(mSurfaceMesh[surfaceType].getTexture().getImage());
		}
		mSurface[surfaceType].setUserData(new NodeDetail(material, 0, true));
		mSurface[surfaceType].setMaterial(material);
//			mSurface.setMaterial(mSurfaceOverrideMaterial != null ? mSurfaceOverrideMaterial : material);

		if (mSurfaceMode[surfaceType] == SURFACE_WIRES)
			mSurface[surfaceType].setDrawMode(DrawMode.LINE);

		getChildren().add(mSurface[surfaceType]);
		}

	private void showSurfaceNormals(int surfaceType) {
		SurfaceMesh mesh = (SurfaceMesh)mSurface[surfaceType].getMesh();
		ObservableFloatArray points = mesh.getPoints();
		ObservableFloatArray normals = mesh.getNormals();
		for (int i=0; i<points.size(); i+=3) {
			Point3D p1 = new Point3D(points.get(i), points.get(i+1), points.get(i+2));
			Point3D p2 = p1.add(0.3*normals.get(i), 0.3*normals.get(i+1), 0.3*normals.get(i+2));
			getChildren().add(new Rod(p1, p2, Color.AQUA));
		}
	}

	public SurfaceMesh getSurfaceMesh(int surfaceType) {
		return (mSurface[surfaceType] == null) ? null : (SurfaceMesh)mSurface[surfaceType].getMesh();
		}

	private void constructMaterials() {
		if (sSolidHighlightedMaterial == null)
			sSolidHighlightedMaterial = createMaterial(HIGHLIGHT_COLOR, 1.0);
		if (sTransparentHighlightedMaterial == null)
			sTransparentHighlightedMaterial = createMaterial(HIGHLIGHT_COLOR, DEFAULT_SURFACE_TRANSPARENCY);
		if (sPickedMaterial == null)
			sPickedMaterial = createMaterial(PICKED_COLOR, 1.0);
		if (sSelectedMaterial == null)
			sSelectedMaterial = createMaterial(V3DScene.SELECTION_COLOR, 1.0);
		}

	public void activateEvents() {
		setOnMousePressed(me -> {
				//System.out.println("mouse pressed isPrimaryButtonDown:"+me.isPrimaryButtonDown()+" isMiddleButtonDown:"+me.isMiddleButtonDown());
				if (me.getButton() == MouseButton.PRIMARY)
					pickShape(me);

				// clicking the mouse wheel causes a MouseExited followed by a MousePressed event
				if (me.getButton() == MouseButton.MIDDLE)
					trackHiliting(me);
				mIsMouseDown = true;
			} );
		setOnMouseReleased(me -> {
//System.out.println("mouse released");
				mIsMouseDown = false;
			});
		setOnMouseMoved(me -> {
//System.out.println("mouse moved");
			if (!mIsMouseDown)	// when dragging (rotating), we need to keep the highlighted rotation root
				trackHiliting(me);
			});
		setOnMouseExited(me -> {
//System.out.println("mouse exited, mIsMouseDown:"+mIsMouseDown);
			if (!mIsMouseDown)	// when dragging (rotating), we need to keep the highlighted rotation root
				setHighlightedShape(null);
			});
		}

	public void deactivateEvents() {
		setOnMousePressed(null);
		setOnMouseReleased(null);
		setOnMouseClicked(null);
		setOnMouseMoved(null);
		setOnMouseExited(null);
	}

	private void trackHiliting(MouseEvent me) {
		PickResult result = me.getPickResult();
		Node node = result.getIntersectedNode();
		if (node instanceof Shape3D)
			setHighlightedShape((Shape3D)node);
		}

	public void removeHilite() {
		setHighlightedShape(null);
		}

	public Node getLastPickedNode() {
		return mLastPickedNode;
		}

	private void pickShape(MouseEvent me) {
		mLastPickedNode = null;
		if (mMeasurementMode != MEASUREMENT.NONE) {
			PickResult result = me.getPickResult();
			mLastPickedNode = result.getIntersectedNode();
			if (mLastPickedNode instanceof Sphere) {
				NodeDetail detail = (NodeDetail)mLastPickedNode.getUserData();
				if (detail != null && detail.isAtom()) {
					Sphere atomShape = (Sphere)mLastPickedNode;
					if (mPickedAtomList.contains(atomShape))
						mPickedAtomList.remove(atomShape);
					else
						mPickedAtomList.add(atomShape);
					updateAppearance(atomShape);
					}
				}
			tryAddMeasurement();
			}
		}

	private void tryAddMeasurement() {
		if (mMeasurementMode == MEASUREMENT.DISTANCE) {
			if (mPickedAtomList.size() < 2)
				return;

			int atom1 = ((NodeDetail)mPickedAtomList.get(0).getUserData()).getAtom();
			int atom2 = ((NodeDetail)mPickedAtomList.get(1).getUserData()).getAtom();
			addMeasurementNodes(mPickedAtomList.get(0), mPickedAtomList.get(1), DISTANCE_COLOR,
								DoubleFormat.toString(calculateAtomDistance(atom1, atom2), 3));

			clearPickedAtomList();
			return;
			}

		if (mMeasurementMode == MEASUREMENT.ANGLE) {
			if (mPickedAtomList.size() < 3)
				return;

			if (pickedAtomsAreStrand()) {
				int atom1 = ((NodeDetail) mPickedAtomList.get(0).getUserData()).getAtom();
				int atom2 = ((NodeDetail) mPickedAtomList.get(1).getUserData()).getAtom();
				int atom3 = ((NodeDetail) mPickedAtomList.get(2).getUserData()).getAtom();
				String angle = Integer.toString((int)Math.round(180*calculateAtomAngle(atom2, atom1, atom3)/Math.PI));
				addMeasurementNodes(mPickedAtomList.get(0), mPickedAtomList.get(2), ANGLE_COLOR, angle);
				}

			clearPickedAtomList();
			return;
			}

		if (mMeasurementMode == MEASUREMENT.TORSION) {
			if (mPickedAtomList.size() < 4)
				return;

			if (pickedAtomsAreStrand()) {
				int[] atom = new int[4];
				for (int i = 0; i < 4; i++)
					atom[i] = ((NodeDetail)mPickedAtomList.get(i).getUserData()).getAtom();

				String torsion = Integer.toString((int)Math.round(180*mMol.calculateTorsion(atom)/Math.PI));
				addMeasurementNodes(mPickedAtomList.get(0), mPickedAtomList.get(3), TORSION_COLOR, torsion);
				}

			clearPickedAtomList();
			return;
			}
		}

	private boolean pickedAtomsAreStrand() {
		int atom1 = ((NodeDetail)mPickedAtomList.get(0).getUserData()).getAtom();
		for (int i=1; i<mPickedAtomList.size(); i++) {
			int atom2 = ((NodeDetail)mPickedAtomList.get(i).getUserData()).getAtom();
			if (mMol.getBond(atom1, atom2) == -1)
				return false;
			atom1 = atom2;
			}
		return true;
		}

	/**
	 *
	 * @param isSelect if false this is a deselection
	 */
	public void select(boolean isSelect) {
		for (Node node:getChildren()) {
			NodeDetail detail = (NodeDetail)node.getUserData();
			if (detail != null && !detail.isTransparent()) {
				detail.setSelected(isSelect);
				updateAppearance(node);
				}
			}
		}

	/**
	 * @param polygon screen coordinate polygon defining the selection
	 * @param mode 0: normal, 1:add, 2:subtract
	 * @param paneOnScreen top let point of parent pane on screen
	 */
	public void select(Polygon polygon, int mode, Point2D paneOnScreen) {
		for (Node node:getChildren()) {
			NodeDetail detail = (NodeDetail)node.getUserData();
			if (detail != null && !detail.isTransparent()) {
				boolean isSelected = (polygon == null)
						|| polygon.contains(node.localToScreen(0, 0, 0).subtract(paneOnScreen));
				if (mode == 1)
					isSelected |= detail.isSelected();
				else if (mode == 2)
					isSelected = detail.isSelected() && !isSelected;
				if (isSelected != detail.isSelected()) {
					detail.setSelected(isSelected);
					updateAppearance(node);
					}
				}
			}
		}

	// delete all flagged atoms except those that have a direct connection
	// to an atom not to be deleted. In this case change the atom's atomic no
	// to 0 unless it is a hydrogen, which is not touched.
	public void deleteAtoms(boolean[] isToBeDeleted) {
		mMol.ensureHelperArrays(Molecule.cHelperNeighbours);

			// delete also all hydrogens that are connected to an atom destined to be deleted
		for (int atom=mMol.getAtoms(); atom<mMol.getAllAtoms(); atom++)
			if (isToBeDeleted[mMol.getConnAtom(atom, 0)])
				isToBeDeleted[atom] = true;

			// convert atomic no to 0 for first layer of deleted atoms
		// (unless for hydrogen) and unset their deletion flag
		int[] borderAtom = new int[mMol.getAllAtoms()];
		int borderAtomCount = 0;
		for (int bond=0; bond<mMol.getAllBonds(); bond++) {
			int atom1 = mMol.getBondAtom(0, bond);
			int atom2 = mMol.getBondAtom(1, bond);
			if (isToBeDeleted[atom1] ^ isToBeDeleted[atom2]) {
				borderAtom[borderAtomCount++] = isToBeDeleted[atom1] ? atom1 : atom2;
			}
		}
		for (int i=0; i<borderAtomCount; i++) {
			int atom = borderAtom[i];
			if (mMol.getAtomicNo(atom) != 1) {
				mMol.setAtomicNo(atom, 0);
				mMol.setAtomMarker(atom, true);
			}
			isToBeDeleted[atom] = false;
		}

		int[] oldAtomToNew = new int[mMol.getAllAtoms()];
		int index = 0;
		for (int i=0; i<oldAtomToNew.length; i++) {
			if (isToBeDeleted[i])
				oldAtomToNew[i] = -1;
			else
				oldAtomToNew[i] = index++;
			}

		int[] oldBondToNew = new int[mMol.getAllBonds()];
		index = 0;
		for (int i=0; i<oldBondToNew.length; i++) {
			if (isToBeDeleted[mMol.getBondAtom(0, i)]
			 || isToBeDeleted[mMol.getBondAtom(1, i)])
				oldBondToNew[i] = -1;
			else
				oldBondToNew[i] = index++;
			}

		ArrayList<Node> nodesToBeDeleted = new ArrayList<>();
		for (Node node:getChildren()) {
			NodeDetail detail = (NodeDetail)node.getUserData();
			if (detail != null) {
				if (detail.isAtom()) {
					if (oldAtomToNew[detail.getAtom()] == -1)
						nodesToBeDeleted.add(node);
					else
						detail.setIndex(oldAtomToNew[detail.getAtom()]);
				} else if (detail.isBond()) {
					if (oldBondToNew[detail.getBond()] == -1)
						nodesToBeDeleted.add(node);
					else
						detail.setIndex(oldBondToNew[detail.getBond()]);
				}
			}
		}
		getChildren().removeAll(nodesToBeDeleted);

		mMol.deleteAtoms(isToBeDeleted);
		}

	/**
	 * @param polygon screen coordinate polygon defining the selection
	 * @param mode one of SURFACE_CUT_xxx
	 * @param paneOnScreen top let point of parent pane on screen
	 */
	public void cutSurface(Polygon polygon, int mode, Point2D paneOnScreen) {
		for (int type = 0; type<MoleculeSurfaceAlgorithm.SURFACE_TYPE.length; type++) {
			if (mSurface[type] != null) {
				PolygonSurfaceCutter cutter = new PolygonSurfaceCutter(polygon, this, mode, paneOnScreen);
				cutSurface(type, cutter);
				}
			}
		}

	private double calculateAtomDistance(int atom1, int atom2) {
		return mMol.getCoordinates(atom2).distance(mMol.getCoordinates(atom1));
		}

	private double calculateAtomAngle(int atom, int conn1, int conn2) {
		Coordinates v1 = mMol.getCoordinates(atom).subC(mMol.getCoordinates(conn1));
		Coordinates v2 = mMol.getCoordinates(atom).subC(mMol.getCoordinates(conn2));
		return v1.getAngle(v2);
		}

	private void clearPickedAtomList() {
		// clear picked atoms
		Sphere[] pickedAtom = mPickedAtomList.toArray(new Sphere[0]);
		mPickedAtomList.clear();
		for (Sphere atomShape:pickedAtom)
			updateAppearance(atomShape);
		}

	private void addMeasurementNodes(Sphere s1, Sphere s2, Color color, String text) {
		Point3D p1 = s1.localToParent(0, 0, 0);
		Point3D p2 = s2.localToParent(0, 0, 0);

		DashedRod line = new DashedRod(p1, p2, color);
		getChildren().add(line);

		NonRotatingLabel label = NonRotatingLabel.create(this, text, p1, p2, color);
		mLabelList.add(label);
		}

/*
	public void setConformer(Conformer conf) { //added by JW
		mConformer = conf;
		V3DMoleculeBuilder builder = new V3DMoleculeBuilder(this);
		// we cannot center pre-alligned conformers			builder.centerMolecule(conformer);
		ArrayList<Node> nodesToBeDeleted = new ArrayList<Node>();
		for(Node node : getChildren()) {
			NodeDetail detail = (NodeDetail)node.getUserData();
			if (detail != null) {
				if (detail.isAtom()) {
					nodesToBeDeleted.add(node);
				}
				
				else if(detail.isBond()) {
					nodesToBeDeleted.add(node);
				}
			}
		}
		builder.buildMolecule(mConformer);
		getChildren().removeAll(nodesToBeDeleted);
	}*/

	public double getHighlightedZ() {
		return (mHighlightedShape == null) ? 0.0 : mHighlightedShape.localToScene(0, 0, 0).getZ();
		}

	public void cutSurface(int surfaceType, SurfaceCutter cutter) {
		if (mSurface[surfaceType] != null) {
			int result = cutter.cut(mSurfaceMesh[surfaceType]);
			switch (result) {
			case 0: // no change
				break;
			case 1: // partially removed
				updateSurfaceFromMesh(surfaceType);
				break;
			case 2: // completely removed
				setSurfaceMode(surfaceType, SURFACE_NONE);
				break;
				}
			}
		}

	public void removeMeasurements() {
		for (int i=getChildren().size()-1; i>=0; i--)
			if (getChildren().get(i) instanceof DashedRod)
				getChildren().remove(i);
		for (NonRotatingLabel label:mLabelList)
			label.remove(this);
		mLabelList.clear();
		}

	/**
	 * @return center of molecule or location of picked Shape in scene as Point3D
	 */
	public Point3D getHighlightedPointInScene() {
		return (mHighlightedShape == null) ? localToScene(0, 0, 0) : mHighlightedShape.localToScene(0, 0, 0);
		}

	/**
	 * @return (0,0,0) or location of picked Shape in molecule coordinates as Point3D
	 */
	public Point3D getHighlightedPointLocal() {
		return (mHighlightedShape == null) ? new Point3D(0, 0, 0) : mHighlightedShape.localToParent(0, 0, 0);
		}

	private void updateAppearance(Node node) {
		if (!(node instanceof Shape3D))
			return;

		if (node instanceof MeshView)
			return;

		Shape3D shape = (Shape3D)node;

		boolean isInvisibleShape = (shape.getUserData() != null
				&& ((NodeDetail)shape.getUserData()).getMaterial().getDiffuseColor().getOpacity() == 0.0);

		if (!isInvisibleShape) {
			float scale = (shape == mHighlightedShape || mPickedAtomList.contains(shape)) ? HIGHLIGHT_SCALE : 1.0f;
			if (shape.getScaleX() != scale) {
				shape.setScaleX(scale);
				shape.setScaleY(scale);
				shape.setScaleZ(scale);
				}
			}

		if (mPickedAtomList.contains(shape)) {
			if (shape.getMaterial() != sPickedMaterial)
				shape.setMaterial(sPickedMaterial);
			return;
			}

		if (shape == mHighlightedShape) {
			Material hiliteMaterial = getHiliteMaterial(shape);
			if (shape.getMaterial() != hiliteMaterial)
				shape.setMaterial(hiliteMaterial);
			return;
			}

		NodeDetail detail = (NodeDetail)shape.getUserData();
		if (detail.isSelected()) {
			if (shape.getMaterial() != sSelectedMaterial)
				shape.setMaterial(sSelectedMaterial);
			return;
			}

		Material material = getOverrideMaterial(shape);
		if (detail != null) {
			if (mOverrideMaterial == null
			 || isInvisibleShape
			 || (mOverrideCarbonOnly
			  && !detail.mayOverrideMaterial()))
				material = detail.getMaterial();
			}

		if (shape.getMaterial() != material)
			shape.setMaterial(material);
		}

	private Material getHiliteMaterial(Shape3D shape) {
		if (shape instanceof MeshView) {
			NodeDetail detail = (NodeDetail)shape.getUserData();
			if (detail.getMaterial().getDiffuseColor().getOpacity() == 1.0)
				return sSolidHighlightedMaterial;
			else
				return sTransparentHighlightedMaterial;
			}
		else {
			return sSolidHighlightedMaterial;
			}
		}

	private PhongMaterial getOverrideMaterial(Shape3D shape) {
		if (!(shape instanceof MeshView))
			return mOverrideMaterial;
		if (mOverrideMaterial == null)
			return null;

		NodeDetail detail = (NodeDetail)shape.getUserData();
		double opacity = detail.getMaterial().getDiffuseColor().getOpacity();
		return createMaterial(mOverrideMaterial.getDiffuseColor(), opacity);
	}
	

	private void setHighlightedShape(Shape3D shape) {
		if (mHighlightedShape != shape) {
			Shape3D previousShape = mHighlightedShape;
			mHighlightedShape = shape;
			if (previousShape != null)
				updateAppearance(previousShape);
			if (shape != null)
				updateAppearance(shape);
			}
		}
	}

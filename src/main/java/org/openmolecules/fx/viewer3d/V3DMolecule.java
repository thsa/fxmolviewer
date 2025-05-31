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

import com.actelion.research.chem.*;
import com.actelion.research.chem.conf.HydrogenAssembler;
import com.actelion.research.chem.conf.BondRotationHelper;
import com.actelion.research.chem.conf.VDWRadii;
import com.actelion.research.chem.phesa.MolecularVolume;
import com.actelion.research.chem.phesa.ShapeVolume;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.ObservableFloatArray;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import org.openmolecules.fx.surface.MarkedAtomSurfaceCutter;
import org.openmolecules.fx.surface.PolygonSurfaceCutter;
import org.openmolecules.fx.surface.SurfaceCutter;
import org.openmolecules.fx.surface.SurfaceMesh;
import org.openmolecules.fx.viewer3d.nodes.*;
import org.openmolecules.fx.viewer3d.torsionstrain.V3DTorsionStrainAnalyzer;
import org.openmolecules.mesh.Cone;
import org.openmolecules.mesh.MoleculeSurfaceAlgorithm;
import org.openmolecules.render.RibbonSideChainConstructionFilter;
import org.openmolecules.render.MoleculeArchitect;
import org.openmolecules.render.RoleHelper;
import org.openmolecules.render.TorsionStrainVisualization;

import java.util.*;
import java.util.stream.IntStream;

import static org.openmolecules.fx.surface.SurfaceMesh.SURFACE_COLOR_PLAIN;

public class V3DMolecule extends V3DRotatableGroup {
	private static final float HIGHLIGHT_SCALE = 1.2f;
	private static int MAX_ID = 0;
	private static final double DEFAULT_SURFACE_TRANSPARENCY = 0.1;
	private static final int DEFAULT_SURFACE_COLOR_MODE = SURFACE_COLOR_PLAIN;

	public static final int SURFACE_MODE_NONE = 0;
	public static final int SURFACE_MODE_WIRES = 1;
	public static final int SURFACE_MODE_FILLED = 2;
	public static final String[] SURFACE_MODE_CODE = { "none", "wires", "filled" };

	private static final Color HIGHLIGHT_COLOR = Color.RED;
	private static final Color PICKED_COLOR = Color.BLUEVIOLET;
	private static final Color DEFAULT_INHERITED_SURFACE_COLOR = Color.LIGHTGRAY;
	private static final Color DEFAULT_SURFACE_COLOR = Color.ROYALBLUE;

	private static PhongMaterial sSolidHighlightedMaterial,sTransparentHighlightedMaterial,sPickedMaterial,sSelectedMaterial;

	private StereoMolecule mMol;
	private ArrayList<StereoMolecule> mLigands;
	private Node mLastPickedNode;
	private Shape3D mHighlightedShape;
	private PhongMaterial mOverrideMaterial,mHydrogenMaterial;
	private Ribbons mRibbons;
	private final MeshView[] mSurface;
	private final SurfaceMesh[] mSurfaceMesh;
	private final Color[] mSurfaceColor;
	private final int[] mSurfaceColorMode;
	private final int[] mSurfaceMode;
	private int mConstructionMode,mRibbonMode;
	private int mHydrogenMode;
	private final LinkedList<Sphere> mPickedAtomList;
	private final ArrayList<Sphere> mTemporaryAtomSpheres;
	private boolean mOverrideHydrogens;
	private final double[] mSurfaceTransparency;
	private final Set<MolCoordinatesChangeListener> mListeners;
	private final Set<MolStructureChangeListener> mStructureListeners;
	private Point3D mRotationCenter;
	private final ObjectProperty<MoleculeRole> mRoleProperty;
	private final IntegerProperty mIDProperty;
	private final BooleanProperty mSelectedProperty;
	private Color mCarbonColor;
	private BondRotationHelper mBondRotationHelper;
	private TorsionStrainVisualization torsionStrainVis;
	private ArrayList<AtomIndexLabel> mLabelList;
	private int mnUnconnectedFragments;
	private boolean mSplitAllBonds;

	public enum MoleculeRole{
		LIGAND { public String toString(){
            return "L";
        } },
		COFACTOR { public String toString(){
			return "C";
    } },
		MACROMOLECULE { public String toString(){
			return "P";
		} },
		SOLVENT {public String toString(){
        return "S";
    } },
	}
	private Coordinates[] mInitialCoordinates;

	public static Color getDefaultAtomColor(int atomicNo) {
		return MoleculeArchitect.getAtomColor(atomicNo, 1.0);
	}

	/**
	 * Creates a V3DMolecule from the given molecule with the following default specification:<br>
	 * - construction mode: sticks<br>
	 * - default hydrogen mode, i.e. shows all explicit hydrogens<br>
	 * - shows no surface<br>
	 * - id = 0<br>
	 * - group = 0<br>
	 * - role = LIGAND
	 * @param mol
	 */
	public V3DMolecule(StereoMolecule mol) {
		this(mol, MoleculeArchitect.CONSTRUCTION_MODE_STICKS, MoleculeArchitect.HYDROGEN_MODE_DEFAULT, 0, MoleculeRole.LIGAND );
	}

	/**
	 * Creates a V3DMolecule from the given molecule with the following default specification:<br>
	 * - construction mode: sticks<br>
	 * - default hydrogen mode, i.e. shows all explicit hydrogens<br>
	 * - shows no surface<br>
	 * - id = 0<br>
	 * - group = 0<br>
	 * - role = LIGAND
	 * @param mol
	 * @param splitAllBonds
	 */
	public V3DMolecule(StereoMolecule mol, boolean overrideHydrogen, boolean splitAllBonds) {
		this(mol, MoleculeArchitect.CONSTRUCTION_MODE_STICKS, MoleculeArchitect.HYDROGEN_MODE_DEFAULT, 0, MoleculeRole.LIGAND, overrideHydrogen, splitAllBonds );
	}

	/**
	 * Creates a V3DMolecule from the given molecule with the following default specification:<br>
	 * - construction mode: sticks<br>
	 * - default hydrogen mode, i.e. shows all explicit hydrogens<br>
	 * - shows no surface
	 * @param mol
	 */
	public V3DMolecule(StereoMolecule mol, int id, MoleculeRole role) {
		this(mol, MoleculeArchitect.CONSTRUCTION_MODE_STICKS, MoleculeArchitect.HYDROGEN_MODE_DEFAULT, id, role );
		}
	
	public V3DMolecule(StereoMolecule mol, int id, MoleculeRole role, boolean overrideHydrogen, boolean splitAllBonds) {
		this(mol, MoleculeArchitect.CONSTRUCTION_MODE_STICKS, MoleculeArchitect.HYDROGEN_MODE_DEFAULT, id, role, overrideHydrogen, splitAllBonds);
		}

	/**
	 * Creates a V3DMolecule from the given molecule with the following default specification:<br>
	 * - default hydrogen mode, i.e. shows all explicit hydrogens<br>
	 * - shows no surface
	 * @param mol
	 * @param constructionMode one of MoleculeArchitect.CONSTRUCTION_MODE_ options
	 */
	public V3DMolecule(StereoMolecule mol, int constructionMode, int id, MoleculeRole role) {
		this(mol, constructionMode, MoleculeArchitect.HYDROGEN_MODE_DEFAULT, id, role);
	}

	/**
	 * Creates a V3DMolecule from the given molecule with generating and showing any surface
	 * @param mol
	 * @param constructionMode one of MoleculeArchitect.CONSTRUCTION_MODE_ options
	 * @param hydrogenMode one of MoleculeArchitect.HYDROGEN_MODE_ options
	 */
	public V3DMolecule(StereoMolecule mol, int constructionMode, int hydrogenMode, int id, MoleculeRole role) {
		this(mol, constructionMode, hydrogenMode, Ribbons.MODE_NONE, SURFACE_MODE_NONE,
				DEFAULT_SURFACE_COLOR_MODE, null, DEFAULT_SURFACE_TRANSPARENCY, id, role, true, false, null);
		}

	/**
	 * Creates a V3DMolecule from the given molecule with generating and showing any surface
	 * @param protein source for the V3DMolecule
	 * @param ligands used to determine protein atom-to-ligand distance to only show close protein atoms in ribbon mode
	 * @param constructionMode one of MoleculeArchitect.CONSTRUCTION_MODE_ options
	 * @param hydrogenMode one of MoleculeArchitect.HYDROGEN_MODE_ options
	 * @param ribbonMode one of Ribbon.MODE_ options
	 * @param surfaceMode SURFACE_NONE, SURFACE_WIRES, or SURFACE_FILLED
	 * @param surfaceColorMode one of SurfaceMesh.SURFACE_COLOR_...
	 * @param surfaceColor null or color to be used for color modes PLAIN or ATOMIC_NO
	 * @param surfaceTransparency SURFACE_NONE, SURFACE_WIRES, or SURFACE_FILLED
	 * @param id
	 */
	public V3DMolecule(StereoMolecule protein, ArrayList<StereoMolecule> ligands, int constructionMode, int hydrogenMode,
					   int ribbonMode, int surfaceMode, int surfaceColorMode, Color surfaceColor, double surfaceTransparency, int id) {
		this(protein, constructionMode, hydrogenMode, ribbonMode, surfaceMode, surfaceColorMode, surfaceColor, surfaceTransparency,
				id, MoleculeRole.MACROMOLECULE, true, false, ligands);
	}

	public V3DMolecule(StereoMolecule mol, int constructionMode, int hydrogenMode, int id, MoleculeRole role, boolean overrideHydrogen, boolean splitAllBonds) {
		this(mol, constructionMode, hydrogenMode, Ribbons.MODE_NONE, SURFACE_MODE_NONE,
				DEFAULT_SURFACE_COLOR_MODE, null, DEFAULT_SURFACE_TRANSPARENCY, id,  role, overrideHydrogen, splitAllBonds, null);
		}

	/**
	 * Creates a V3DMolecule from the given molecule with the given specification.
	 * @param mol
	 * @param constructionMode one of MoleculeArchitect.CONSTRUCTION_MODE_ options
	 * @param hydrogenMode one of MoleculeArchitect.HYDROGEN_MODE_ options
	 * @param ribbonMode one of Ribbon.MODE_ options
	 * @param surfaceMode SURFACE_NONE, SURFACE_WIRES, or SURFACE_FILLED
	 * @param surfaceColorMode DEFAULT_SURFACE_COLOR_MODE or one of the SurfaceMesh.SURFACE_COLOR_xxx modes
	 * @param surfaceColor null or explicit surface color used for some color modes
	 * @param surfaceTransparency
	 * @param id
	 * @param role
	 * @param overrideHydrogens whether hydrogen atoms are drawn in the molecule override color
	 * @param splitAllBonds whether all bonds shall be constructed from two cylinders (allowing better selection coloring)
	 */
	public V3DMolecule(StereoMolecule mol, int constructionMode, int  hydrogenMode, int ribbonMode,
					   int surfaceMode, int surfaceColorMode, Color surfaceColor, double surfaceTransparency,
					   int id, MoleculeRole role, boolean overrideHydrogens, boolean splitAllBonds, ArrayList<StereoMolecule> ligands) {
		super(mol.getName());
		mMol = mol;
		mnUnconnectedFragments = mMol.getFragmentNumbers(new int[mMol.getAllAtoms()], false, true);
		mPickedAtomList = new LinkedList<>();
		mTemporaryAtomSpheres = new ArrayList<>();
		mConstructionMode = constructionMode;
		mHydrogenMode = hydrogenMode;
		mRoleProperty = new SimpleObjectProperty<>(role);
		mIDProperty = new SimpleIntegerProperty(id);
		mSelectedProperty = new SimpleBooleanProperty(false);
		mSelectedProperty.addListener((v,ov,nv) -> updateSelectionAppearance());
		mOverrideHydrogens = overrideHydrogens;
		mSplitAllBonds = splitAllBonds;
		int surfaceCount = MoleculeSurfaceAlgorithm.SURFACE_TYPE.length;
		mSurface = new MeshView[surfaceCount];
		mSurfaceMesh = new SurfaceMesh[surfaceCount];
		mSurfaceMode = new int[surfaceCount];
		mSurfaceColor = new Color[surfaceCount];
		mSurfaceColorMode = new int[surfaceCount];
		mSurfaceTransparency = new double[surfaceCount];
		mBondRotationHelper = new BondRotationHelper(mol);
		mListeners = new HashSet<>();
		mStructureListeners = new HashSet<>();
		mLigands = ligands;

		mSurfaceMode[0] = surfaceMode;
		mSurfaceColor[0] = (surfaceColor == null) ? DEFAULT_SURFACE_COLOR : surfaceColor;
		mSurfaceColorMode[0] = surfaceColorMode;
		mSurfaceTransparency[0] = surfaceTransparency;
		mInitialCoordinates = new Coordinates[mol.getAllAtoms()];
		IntStream.range(0,mol.getAllAtoms()).forEach(i -> {
			mInitialCoordinates[i] = new Coordinates(mol.getCoordinates(i));
		});

		for (int i=1; i<surfaceCount; i++) {
			mSurfaceMode[i] = SURFACE_MODE_NONE;
			mSurfaceColor[i] = DEFAULT_SURFACE_COLOR;
			mSurfaceColorMode[i] = DEFAULT_SURFACE_COLOR_MODE;
			mSurfaceTransparency[i] = DEFAULT_SURFACE_TRANSPARENCY;
			}

		if (ribbonMode != Ribbons.MODE_NONE && role == MoleculeRole.MACROMOLECULE) {
			mRibbons = new Ribbons(mMol, this);
			mRibbons.draw(ribbonMode);
			mRibbonMode = ribbonMode;
		}

		constructMaterials();
		V3DMoleculeBuilder builder = new V3DMoleculeBuilder(this);
		if (ribbonMode != Ribbons.MODE_NONE && mLigands != null)
			builder.buildMolecule(new RibbonSideChainConstructionFilter(mMol, mLigands, mRibbons.getBackboneMask()));
		else
			builder.buildMolecule();

		if (surfaceMode != SURFACE_MODE_NONE) {
			SurfaceCutter cutter = (role == MoleculeRole.MACROMOLECULE) ? createSurfaceCutter() : null;
			mSurfaceMesh[0] = new SurfaceMesh(mMol, 0, surfaceColorMode, getNeutralColor(0), 1.0 - surfaceTransparency, cutter);
			updateSurfaceFromMesh(0);
		}
	}

	public void setInitialCoordinates() {
		mInitialCoordinates = new Coordinates[mMol.getAllAtoms()];
		IntStream.range(0,mMol.getAllAtoms()).forEach(i -> {
			mInitialCoordinates[i] = new Coordinates(mMol.getCoordinates(i));
		});
	}

	public void resetCoordinates() {
		if(mInitialCoordinates!=null && mInitialCoordinates.length==mMol.getAllAtoms()) {
			IntStream.range(0,mMol.getAllAtoms()).forEach(i -> {
				mMol.setAtomX(i, mInitialCoordinates[i].x);
				mMol.setAtomY(i, mInitialCoordinates[i].y);
				mMol.setAtomZ(i, mInitialCoordinates[i].z);
			});
		}
		Platform.runLater(() -> {
			fireCoordinatesChange();
			V3DMoleculeUpdater mFXMolUpdater = new V3DMoleculeUpdater(this);
			mFXMolUpdater.update();
		});
	}

	public boolean hasAtomIndexLabels() {
		return mLabelList != null;
	}

	public void addAtomIndexLabels(V3DRotatableGroup world) {
		if (mLabelList == null) {
			mLabelList = new ArrayList<>();
			for (Node node:getChildren()) {
				NodeDetail detail = (NodeDetail)node.getUserData();
				if (detail != null && detail.isAtom()) {
					AtomIndexLabel label  = new AtomIndexLabel(world,this, detail);
					mLabelList.add(label);
				}
			}
		}
	}

	public void removeAtomIndexLabels() {
		if (mLabelList != null) {
			for (AtomIndexLabel l : mLabelList)
				l.remove();

			mLabelList = null;
		}
	}

	public void addTorsionStrainVisualization() {
		V3DTorsionStrainAnalyzer torsionAnalyzer = new V3DTorsionStrainAnalyzer(this);
		torsionAnalyzer.init();
		torsionStrainVis = new TorsionStrainVisualization(torsionAnalyzer);
		torsionStrainVis.build();
	}

	public void addPharmacophore() {
		V3DCustomizablePheSA pharmacophore = new V3DCustomizablePheSA(this);
		constructPharmacophore(pharmacophore);
	}
	
	public void addPharmacophore(MolecularVolume molVol) {
		V3DCustomizablePheSA pharmacophore = new V3DCustomizablePheSA(this,molVol);
		constructPharmacophore(pharmacophore);
	}
	
	public void addNegativeReceptorImage(ShapeVolume bsVol) {
		V3DBindingSiteVolume bsVolume = new V3DBindingSiteVolume(bsVol);
		constructNegativeReceptorImage(bsVolume);
	}

	private void constructNegativeReceptorImage(V3DBindingSiteVolume bsVolume) {
		bsVolume.buildVolume();
		this.addGroup(bsVolume);
	}
	
	private void constructPharmacophore(V3DCustomizablePheSA pharmacophore) {
		pharmacophore.buildPharmacophore();
		this.addGroup(pharmacophore);
		//Platform.runLater(() -> getChildren().add(mPharmacophore));
		mListeners.add(pharmacophore);
	}
	
	public List<V3DCustomizablePheSA> getPharmacophoreModels() {
		List<V3DCustomizablePheSA> phesaModels = new ArrayList<>();
		for(V3DRotatableGroup customizablePheSA : this.getGroups()) {
			if(customizablePheSA instanceof V3DCustomizablePheSA) {
				phesaModels.add((V3DCustomizablePheSA)customizablePheSA);
			}
		}
		return phesaModels;
	}
	
	public void assignLikelyProtonationStates() {
		for(int a=0;a<mMol.getAtoms();a++) {
			if(mMol.getAtomicNo(a)==7) {
				if (AtomFunctionAnalyzer.isBasicNitrogen(mMol, a))
					mMol.setAtomCharge(a, +1);
					
			}
			if(mMol.getAtomicNo(a)==8) {
				if (AtomFunctionAnalyzer.isAcidicOxygen(mMol, a))
					mMol.setAtomCharge(a, -1);
					
			}
		}
		addImplicitHydrogens();
	}
	
	
	public boolean addImplicitHydrogens() {
		int oldAtoms = mMol.getAllAtoms();
		int oldBonds = mMol.getAllBonds();

		int count = new HydrogenAssembler(mMol).addImplicitHydrogens();
		if (count == 0)
			return false;

		mMol.ensureHelperArrays(Molecule.cHelperNeighbours);
		V3DMoleculeBuilder builder = new V3DMoleculeBuilder(this);
		builder.buildMolecule(oldAtoms, oldBonds);
		setInitialCoordinates();
		updateColor();
		return true;
	}
	
	public void addMoleculeCoordinatesChangeListener(MolCoordinatesChangeListener listener) {
		mListeners.add(listener);
	}
	
	public void removeMoleculeCoordinatesChangeListener(MolCoordinatesChangeListener listener) {
		mListeners.remove(listener);
	}
	
	public void addMoleculeStructureChangeListener(MolStructureChangeListener listener) {
		mStructureListeners.add(listener);
	}
	
	public void removeMoleculeStructureChangeListener(MolStructureChangeListener listener) {
		mStructureListeners.remove(listener);
	}

	public StereoMolecule getMolecule() {
		return mMol;
	}
	
	public void setMolecule(StereoMolecule mol) {
		mMol = mol;
	}
	
	public BondRotationHelper getBondRotationHelper() {
		return mBondRotationHelper;
	}

	private boolean isSurface(Node node) {
		return node instanceof MeshView && !(node instanceof Cone);
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

	/**
	 * @param p rotation center to be used by rotate(); if null, then rotate() uses highlighted shape or center of gravity
	 */
	public void setCenterOfRotation(Point3D p) {
		mRotationCenter = p;
	}

	@Override
	public void rotate(Rotate r) {
		Point3D cor = (mRotationCenter != null) ? mRotationCenter
					: (mHighlightedShape != null) ? new Point3D(mHighlightedShape.getTranslateX(),
																mHighlightedShape.getTranslateY(),
																mHighlightedShape.getTranslateZ())
					: getCenterOfGravity();

		Point3D p1 = getRotation().transform(cor);
		Point3D p2 = r.transform(p1.getX(), p1.getY(), p1.getZ());

		super.rotate(r);

		setTranslateX(getTranslateX()+p1.getX()-p2.getX());
		setTranslateY(getTranslateY()+p1.getY()-p2.getY());
		setTranslateZ(getTranslateZ()+p1.getZ()-p2.getZ());
		fireCoordinatesChange();
		}

	public int getConstructionMode() {
		return mConstructionMode;
		}

	public int getHydrogenMode() {
		return mHydrogenMode;
	}

	public int getRibbonMode() {
		return mRibbonMode;
	}

	public void setConstructionMode(int mode) {
		setMode(mode, mHydrogenMode, mRibbonMode);
	}

	public void setHydrogenMode(int mode) {
		setMode(mConstructionMode, mode, mRibbonMode);
	}

	public void setRibbonMode(int mode) {
		setMode(mConstructionMode, mHydrogenMode, mode);
	}

	/**
	 * Defines the molecule construction mode (balls, sticks, balls & sticks, etc)
	 * @param constructionMode one of the MoleculeArchitect.CONSTRUCTION_MODE... options
	 * @param hydrogenMode one of the MoleculeArchitect.HYDROGEN_MODE... options
	 * @param ribbonMode one of the Ribbons.MODE... options
	 */
	public void setMode(int constructionMode, int hydrogenMode, int ribbonMode) {
		if (constructionMode == mConstructionMode
		 && hydrogenMode == mHydrogenMode
		 && ribbonMode == mRibbonMode)
			return;

		boolean atomVisibilityChanged = false;
		if (ribbonMode != mRibbonMode) {
			if (mRibbons != null) {
				mRibbons.removeRibbon();
				mRibbons = null;
			}
			if (ribbonMode != Ribbons.MODE_NONE) {
				mRibbons = new Ribbons(mMol, this);
				mRibbons.draw(ribbonMode);
			}
			atomVisibilityChanged = mLigands != null && (ribbonMode != Ribbons.MODE_NONE ^ mRibbonMode != Ribbons.MODE_NONE);
			mRibbonMode = ribbonMode;
		}

		if (constructionMode != mConstructionMode
		 || hydrogenMode != mHydrogenMode
		 || atomVisibilityChanged) {
			mConstructionMode = constructionMode;
			mHydrogenMode = hydrogenMode;

			for (int i = getChildren().size() - 1; i >= 0; i--)
				if (!isSurface(getChildren().get(i)))
					getChildren().remove(i);

			V3DMoleculeBuilder builder = new V3DMoleculeBuilder(this);
			builder.setConstructionMode(constructionMode);
			builder.setHydrogenMode(hydrogenMode);
			if (ribbonMode != Ribbons.MODE_NONE && mLigands != null)
				builder.buildMolecule(new RibbonSideChainConstructionFilter(mMol, mLigands, mRibbons.getBackboneMask()));
			else
				builder.buildMolecule();
		}

		// rebuild surfaces after creation of molecule primitives, because surface transparency depends on creation time of triangles
		for (int i=0; i<mSurfaceMode.length; i++)
			if (mSurfaceMode[i] != SURFACE_MODE_NONE)
				updateSurfaceFromMesh(i);

		updateColor();
		}
	
	public void reconstruct() {
		for (int i=getChildren().size()-1; i>=0; i--)
			if (!isSurface(getChildren().get(i)))
				getChildren().remove(i);
		V3DMoleculeBuilder builder = new V3DMoleculeBuilder(this);
		builder.setConstructionMode(mConstructionMode);
		builder.setHydrogenMode(mHydrogenMode);
		builder.buildMolecule();
	}

	/**
	 * @return explicit carbon atom color, if defined, or null
	 */
	public Color getColor() {
		return (mOverrideMaterial == null) ? null : mOverrideMaterial.getDiffuseColor();
		}

	
	public void updateColor() {
		if(mCarbonColor!=null)
			setColor(mCarbonColor);
	}
	
	public void setColor(Color color) {
		mCarbonColor = color;

		Color hydrogenColor = getDefaultAtomColor(1);
		mHydrogenMaterial = createMaterial(color == null ? hydrogenColor : color.interpolate(hydrogenColor, 0.5), 1.0);

		if (color == null) {
			mOverrideMaterial = null;
			}
		else {
			mOverrideMaterial = new PhongMaterial();
			mOverrideMaterial.setDiffuseColor(color);
			mOverrideMaterial.setSpecularColor(color.darker());
			}

		if (mRibbonMode != Ribbons.MODE_NONE)
			mRibbons.updateColor();

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
					material.setSpecularColor(Color.GRAY);
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

		if (mSurfaceMode[surfaceType] == SURFACE_MODE_NONE)
			setSurface(surfaceType, SURFACE_MODE_FILLED, SURFACE_COLOR_PLAIN, mSurfaceTransparency[surfaceType]);
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
	 * @param colorMode one of SurfaceMesh.SURFACE_COLOR_xxx
	 */
	public void setSurfaceColorMode(int surfaceType, int colorMode) {
		if (mSurfaceColorMode[surfaceType] != colorMode) {
			if (mSurfaceMode[surfaceType] == SURFACE_MODE_NONE)
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
	
	public static int getNextID() {
		return MAX_ID++;
	}
	
	public int getID() {
		return mIDProperty.get();
	}
	
	
	public IntegerProperty IDProperty() {
		return mIDProperty;
	}
	
	public boolean isSelected() {
		return mSelectedProperty.get();
	}

	public boolean isSplitAllBonds() {
		return mSplitAllBonds;
	}

	public boolean overrideHydrogens() {
		return mOverrideHydrogens;
	}
	
	public void setOverrideHydrogens(boolean override) {
		mOverrideHydrogens = override;
	}

	public void setMoleculeRole(MoleculeRole role) {
		mRoleProperty.set(role);
	}
	
	public void setID(int id) {
		mIDProperty.set(id);
	}

	/**
	 * Use this method to create or remove a molecules surface
	 * @param surfaceType MoleculeSurfaceAlgorithm.CONNOLLY or LEE_RICHARDS
	 * @param surfaceMode SURFACE_NONE,SURFACE_WIRES, or SURFACE_FILLED
	 * @param colorMode one of the SurfaceMesh.SURFACE_COLOR_... options
	 * @param transparency
	 */
	public void setSurface(int surfaceType, int surfaceMode, int colorMode, double transparency) {
		if (mMol.getAllAtoms() == 0)
			return;

		if (surfaceMode == mSurfaceMode[surfaceType]
		 && colorMode == mSurfaceColorMode[surfaceType]
		 && transparency == mSurfaceTransparency[surfaceType])
			return;

		mSurfaceMode[surfaceType] = surfaceMode;
		mSurfaceColorMode[surfaceType] = colorMode;
		mSurfaceTransparency[surfaceType] = transparency;

		if (surfaceMode == SURFACE_MODE_NONE) {
			if (mSurface[surfaceType] != null) {
				getChildren().remove(mSurface[surfaceType]);
				mSurface[surfaceType] = null;
				mSurfaceMesh[surfaceType] = null;
			}
		} else {
			double opacity = 1.0 - transparency;
			if (mSurfaceMesh[surfaceType] == null) {
				SurfaceCutter cutter = (getRole() == MoleculeRole.MACROMOLECULE) ? createSurfaceCutter() : null;
				mSurfaceMesh[surfaceType] = new SurfaceMesh(mMol, surfaceType, colorMode, getNeutralColor(surfaceType), opacity, cutter);
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
				return new MarkedAtomSurfaceCutter(this);

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
			material.setSpecularColor(Color.GRAY);
			material.setDiffuseMap(mSurfaceMesh[surfaceType].getTexture().getImage());
		}
		mSurface[surfaceType].setUserData(new NodeDetail(material, 0, true));
		mSurface[surfaceType].setMaterial(material);
//			mSurface.setMaterial(mSurfaceOverrideMaterial != null ? mSurfaceOverrideMaterial : material);

		if (mSurfaceMode[surfaceType] == SURFACE_MODE_WIRES)
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

/*	public void activateEvents() {
		setOnMousePressed(me -> {
				//System.out.println("mouse pressed isPrimaryButtonDown:"+me.isPrimaryButtonDown()+" isMiddleButtonDown:"+me.isMiddleButtonDown());
				//if (me.getButton() == MouseButton.PRIMARY) {
				//	pickShape(me);
			//	}
				// clicking the mouse wheel causes a MouseExited followed by a MousePressed event
				if (me.getButton() == MouseButton.MIDDLE) {
					trackHiliting(me);
				mIsMouseDown = true;
				}
			} );
		setOnMouseReleased(me -> {
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
*/
	public Node getLastPickedNode() {
		return mLastPickedNode;
		}

	public boolean pickShape(MouseEvent me) {
		PickResult result = me.getPickResult();
		mLastPickedNode = result.getIntersectedNode();
		if (mLastPickedNode instanceof Sphere) {
			NodeDetail detail = (NodeDetail)mLastPickedNode.getUserData();
			if (detail != null && detail.isAtom()) {
				Sphere atomShape = (Sphere)mLastPickedNode;
				if (mPickedAtomList.contains(atomShape)) {
					mPickedAtomList.remove(atomShape);
					updateAppearance(atomShape);
					return false;
				}
				else {
					mPickedAtomList.add(atomShape);
					updateAppearance(atomShape);
					return true;
				}
			}
		}
		return false;
	}
	
	public void fireCoordinatesChange() {
		for(MolCoordinatesChangeListener listener : mListeners) {
			listener.coordinatesChanged();
		}
	}
	
	public void fireStructureChange() {
		removeAllPharmacophores();
		if(torsionStrainVis!=null) {
			torsionStrainVis.cleanup();
			torsionStrainVis=null;
		}
		mBondRotationHelper = new BondRotationHelper(mMol);
		for(MolStructureChangeListener listener : mStructureListeners) {
			listener.structureChanged();
		}
		mnUnconnectedFragments = mMol.getFragmentNumbers(new int[mMol.getAllAtoms()], false, true); 
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

	public void toggleSelection() {
        mSelectedProperty.set(!mSelectedProperty.get());
		}

	/**
	 * Updates the selection of the entire molecule
	 */
	private void updateSelectionAppearance() {
		// Complete molecule selection
		for (Node node:getChildren()) {
			NodeDetail detail = (NodeDetail)node.getUserData();
			if (detail != null && !detail.isTransparent()) {
				detail.setSelected(mSelectedProperty.get());
				updateAppearance(node);
				}
			}

		for (int atom=0; atom<mMol.getAllAtoms(); atom++)
			mMol.setAtomSelection(atom, mSelectedProperty.get());
	}

	/**
	 * @param polygon screen coordinate polygon defining the selection
	 * @param mode 0: normal, 1:add, 2:subtract
	 * @param paneOnScreen top let point of parent pane on screen
	 */
	public void select(Polygon polygon, int mode, Point2D paneOnScreen) {
		boolean selectionChanged = false;
		for (Node node:getChildren()) {
			NodeDetail detail = (NodeDetail)node.getUserData();
			if (detail != null && !detail.isTransparent() && detail.isAtom()) {
				boolean isSelected = (polygon != null)
					&& polygon.contains(node.localToScreen(0, 0, 0).subtract(paneOnScreen));
				if (mode == 1)
					isSelected |= detail.isSelected();
				else if (mode == 2)
					isSelected = detail.isSelected() && !isSelected;
				if (isSelected != detail.isSelected()) {
					detail.setSelected(isSelected);
					updateAppearance(node);
					mMol.setAtomSelection(detail.getAtom(), isSelected);
					selectionChanged = true;
				}
			}
		}
		if (selectionChanged) {
			for (Node node:getChildren()) {
				NodeDetail detail = (NodeDetail)node.getUserData();
				if (detail != null && !detail.isTransparent()) {
					if (detail.isBond()) {
						int bond = detail.getBond();
						int bondAtom = detail.getBondAtom(mMol);
						boolean isSelected = (bondAtom != -1) ? mMol.isSelectedAtom(bondAtom)
								 : mMol.isSelectedAtom(mMol.getBondAtom(0, bond))
								&& mMol.isSelectedAtom(mMol.getBondAtom(1, bond));
						if (isSelected != detail.isSelected()) {
							detail.setSelected(isSelected);
							updateAppearance(node);
						}
					}
				}
			}
		}

/*		mSelectedProperty.set(!mSelectedProperty.get());
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
					int atom = detail.getAtom();
					if (atom != -1)
						mMol.setAtomSelection(atom, isSelected);
					}
				}
			}*/
		}

	// Delete all flagged atoms except those that have a direct connection
	// to an atom not to be deleted. In this case change the atom's atomic no
	// to 0 unless it is a hydrogen, which is not touched.
	// Note: This method sets the atom marker for all non-hydrogen atoms that
	// were not deleted and set to atomicNo=0 (exit vectors).
	public void deleteAtoms(boolean[] isToBeDeleted) {
		mMol.ensureHelperArrays(Molecule.cHelperNeighbours);

		// delete also all hydrogens that are connected to an atom destined to be deleted
		for (int atom=mMol.getAtoms(); atom<mMol.getAllAtoms(); atom++)
			if (isToBeDeleted[mMol.getConnAtom(atom, 0)])
				isToBeDeleted[atom] = true;

		// Set atomicNo to 0 for first layer of deleted non-H atoms.
		// They stay (deletion flag is purged), but bonds between them go.
		int[] borderAtom = new int[mMol.getAllAtoms()];
		int borderAtomCount = 0;
		for (int bond=0; bond<mMol.getAllBonds(); bond++) {
			int atom1 = mMol.getBondAtom(0, bond);
			int atom2 = mMol.getBondAtom(1, bond);
			if (isToBeDeleted[atom1] ^ isToBeDeleted[atom2])
				borderAtom[borderAtomCount++] = isToBeDeleted[atom1] ? atom1 : atom2;

			// Make sure that bond between two atoms for deletion is deleted
			if (isToBeDeleted[atom1] && isToBeDeleted[atom2])
				mMol.markBondForDeletion(bond);
		}
		for (int i=0; i<borderAtomCount; i++) {
			int atom = borderAtom[i];
			if (mMol.getAtomicNo(atom) != 1) {
				mMol.setAtomicNo(atom, 0);
				mMol.setAtomMarker(atom, true); // fixed atom for minimizer
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
		
		setInitialCoordinates();
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



	public void clearPickedAtomList() {
		// clear picked atoms
		Sphere[] pickedAtom = mPickedAtomList.toArray(new Sphere[0]);
		mPickedAtomList.clear();
		for (Sphere atomShape:pickedAtom)
			updateAppearance(atomShape);
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

	public Shape3D getHighlightedShape() {
		return mHighlightedShape;
	}
	
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
				setSurfaceMode(surfaceType, SURFACE_MODE_NONE);
				break;
				}
			}
		}



	/**
	 * @return center of molecule or location of picked Shape in scene as Point3D
	 */
	public Point3D getHighlightedPointInScene() {
		return (mHighlightedShape == null) ? localToScene(0, 0, 0) : mHighlightedShape.localToScene(0, 0, 0);
		}
	
	public LinkedList<Sphere> getPickedAtoms() {
		return mPickedAtomList;
	}

	/**
	 * @return (0,0,0) or location of picked Shape in molecule coordinates as Point3D
	 */
	public Point3D getHighlightedPointLocal() {
		return (mHighlightedShape == null) ? new Point3D(0, 0, 0) : mHighlightedShape.localToParent(0, 0, 0);
		}

	public void updateAppearance(Node node) {
		
		if(node.getParent() instanceof PPArrow)
			node = ((PPArrow)node.getParent()).getCylinder();
		if(node.getParent() instanceof PPSphere)
			node = ((PPSphere)node.getParent()).getSphere();
		if(node.getParent() instanceof VolumeSphere)
			node = ((VolumeSphere)node.getParent()).getSphere();

		if (!(node instanceof Shape3D))
			return;

		if (isSurface(node))
			return;

		Shape3D shape = (Shape3D)node;

		boolean isInvisibleShape = (shape.getUserData() != null
				&& ((NodeDetail)shape.getUserData()).getMaterial().getDiffuseColor().getOpacity() == 0.0);

		if (!isInvisibleShape) {
			float scale = (shape == mHighlightedShape || mPickedAtomList.contains(shape) /*|| isSelectedAtom*/) ? HIGHLIGHT_SCALE : 1.0f;
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
		if (detail != null && detail.isSelected()) {
			if (shape.getMaterial() != sSelectedMaterial)
				shape.setMaterial(sSelectedMaterial);
			return;
			}

		Material material = getOverrideMaterial(shape);
		if (detail != null) {
			if (mOverrideMaterial == null
			 || isInvisibleShape
			 || (!detail.mayOverrideMaterial())) {
				material = detail.getMaterial();}
			}
;
		if (shape.getMaterial() != material) {
			shape.setMaterial(material);
		}
		}

	private Material getHiliteMaterial(Shape3D shape) {
		if (isSurface(shape)) {
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
		if (!isSurface(shape)) {
			NodeDetail detail = (NodeDetail)shape.getUserData();
			if (detail != null
			 && detail.getMaterial() == V3DMoleculeBuilder.getMaterial(MoleculeArchitect.getAtomicNoARGB(1)))
				return mHydrogenMaterial;
			else
				return mOverrideMaterial;
		}
		if (mOverrideMaterial == null)
			return null;

		NodeDetail detail = (NodeDetail)shape.getUserData();
		double opacity = detail.getMaterial().getDiffuseColor().getOpacity();
		return createMaterial(mOverrideMaterial.getDiffuseColor(), opacity);
	}

	public void setHighlightedShape(Shape3D shape) {
		if(shape!=null && shape.getParent() instanceof PPSphere) {
			shape = ((PPSphere)shape.getParent()).getSphere();
		}
		if(shape!=null && shape.getParent() instanceof VolumeSphere) {
			shape = ((VolumeSphere)shape.getParent()).getSphere();
		}
		if (mHighlightedShape != shape) {
			Shape3D previousShape = mHighlightedShape;
			mHighlightedShape = shape;
			if (previousShape != null)
				updateAppearance(previousShape);
			if (shape != null)
				updateAppearance(shape);

			updateTemporaryAtomSpheres();
			}
		}

	private void updateTemporaryAtomSpheres() {
		for (int i=mTemporaryAtomSpheres.size()-1; i>=0; i--) {
			Sphere sphere = mTemporaryAtomSpheres.get(i);
			if (sphere != mHighlightedShape && !mPickedAtomList.contains(sphere)) {
				getChildren().remove(sphere);
				mTemporaryAtomSpheres.remove(i);
				}
			}

		if (mHighlightedShape instanceof Cylinder
		 && mConstructionMode == MoleculeArchitect.CONSTRUCTION_MODE_WIRES) {
			NodeDetail detail = (NodeDetail)mHighlightedShape.getUserData();
			if (detail != null && detail.isBond()) {
				int bond = detail.getBond();
				for (int i=0; i<2; i++) {
					int atom = mMol.getBondAtom(i, bond);
					int atomicNo = mMol.getAtomicNo(atom);
					Sphere sphere = new Sphere(VDWRadii.getVDWRadius(atomicNo)/8, 16);

					int argb = MoleculeArchitect.getAtomicNoARGB(atomicNo);
					boolean isOverridable = overrideHydrogens() ?
						  (argb == MoleculeArchitect.getAtomicNoARGB(1)
						|| argb == MoleculeArchitect.getAtomicNoARGB(6))
						: (argb == MoleculeArchitect.getAtomicNoARGB(6));
					PhongMaterial material = V3DMoleculeBuilder.getMaterial(argb);

					sphere.setMaterial(material);
					sphere.setTranslateX(mMol.getAtomX(atom));
					sphere.setTranslateY(mMol.getAtomY(atom));
					sphere.setTranslateZ(mMol.getAtomZ(atom));
					sphere.setUserData(new NodeDetail(material, RoleHelper.createAtomRole(atom), isOverridable));
					getChildren().add(sphere);
					mTemporaryAtomSpheres.add(sphere);
					}
				}
			}
		}

	public void removeAllSurfaces() {
		for (int type=0; type<MoleculeSurfaceAlgorithm.SURFACE_TYPE.length; type++)
			setSurfaceMode(type, SURFACE_MODE_NONE);
	}
	
	
	public void removeAllPharmacophores() {
		List<V3DRotatableGroup> allChildren = this.getAllAttachedRotatableGroups();
		for(V3DRotatableGroup child : allChildren) {
			if(child instanceof V3DCustomizablePheSA)
				removePharmacophore((V3DCustomizablePheSA)child);
		}
		
	}
	
	public void removePharmacophore(V3DCustomizablePheSA pharmacophore) {
		this.deleteGroup(pharmacophore);
		//Platform.runLater(() -> getChildren().add(mPharmacophore));
		mListeners.remove(pharmacophore);

	}
	
	public MoleculeRole getRole() {
		return mRoleProperty.get();
	}
	
	public ObjectProperty<MoleculeRole> RoleProperty() {
		return mRoleProperty;
	}

	public BooleanProperty SelectionProperty() {
		return mSelectedProperty;
	}
	
	public void setRole(MoleculeRole role) {
		mRoleProperty.set(role);
	}

	public int getUnconnectedFragmentNo() {
		return mnUnconnectedFragments;
	}

	@Override
	public String getName() {
		return mMol.getName();
	}

	public TorsionStrainVisualization getTorsionStrainVis() {
		return torsionStrainVis;
	}
}

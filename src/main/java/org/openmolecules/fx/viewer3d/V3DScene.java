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
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.chem.coords.CoordinateInventor;
import com.actelion.research.chem.dnd.ChemistryDataFormats;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.gui.clipboard.TextClipboardHandler;
import com.actelion.research.util.DoubleFormat;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Sphere;
import javafx.stage.Screen;
import org.openmolecules.chem.conf.gen.ConformerGenerator;
import org.openmolecules.fx.viewer3d.interactions.InteractionHandler;
import org.openmolecules.fx.viewer3d.interactions.plip.PLIPInteractionCalculator;
import org.openmolecules.fx.viewer3d.nodes.DashedRod;
import org.openmolecules.fx.viewer3d.nodes.NodeDetail;
import org.openmolecules.fx.viewer3d.nodes.NonRotatingLabel;
import org.openmolecules.mesh.MoleculeSurfaceAlgorithm;

import java.util.*;

import static org.openmolecules.fx.viewer3d.V3DStereoPane.MODE_HOU;
import static org.openmolecules.fx.viewer3d.V3DStereoPane.MODE_HSBS;


public class V3DScene extends SubScene implements LabelDeletionListener {
	private final ClipboardHandler mClipboardHandler;
	private final Group mRoot;                  	// not rotatable, contains light and camera
	private final V3DRotatableGroup mWorld;		// rotatable, not movable, root in center of scene, contains all visible objects
	private final List<V3DSceneListener> mSceneListeners;
	private int mSurfaceCutMode;
	private V3DMolecule mSurfaceCutMolecule;
	private V3DMoleculeEditor mEditor;
	private boolean mMouseDragged; //don't place molecule fragments if mouse is released after a drag event
	private final ArrayList<V3DMolecule> mPickedMolsList;
	private MEASUREMENT     mMeasurementMode;
	private final ArrayList<V3DMeasurement> mMeasurements;
	private V3DMolecule mCopiedMol;
	private volatile V3DPopupMenuController mPopupMenuController;
	private final EnumSet<ViewerSettings> mSettings;
	private boolean mOverrideHydrogens;
	private int mMoleculeColorID;
	private V3DBindingSite mBindingSiteHelper;
	private InteractionHandler mInteractionHandler;
	private final ObjectProperty<XYChart<Number,Number>> mChartProperty; //for graphs and charts that are created by interaction with the scene (e.g. hovering over a torsion angle)
	private PointLight mLight;
	private PerspectiveCamera mCamera;
	private double mDepthCuingIntensity;


	public static final Color SELECTION_COLOR = Color.DARKCYAN;
	protected static final double CAMERA_INITIAL_Z = -45;
	protected static final double CAMERA_FIELD_OF_VIEW = 30.0;	// default field of view
	protected static final double CAMERA_NEAR_CLIP = 1.0;
	protected static final double CAMERA_FAR_CLIP = 1000.0;
	protected static final double CAMERA_MIN_CLIP_THICKNESS = 2.0;

	private static final double AMBIENT_LIGHT = 0.1;
	private static final double LIGHT_X_OFFSET = 0;   // Main light position in regard to camera
	private static final double LIGHT_Y_OFFSET = 0;
	private static final double LIGHT_Z_OFFSET = -20;
	private static final double LIGHT_NEAR = 1.0;  // attenuation at camera (for depth cueing)
	private static final double LIGHT_FAR = 0.1;  // attenuation behind objects (for depth cueing, must be > 0.0)
	private static final double CLIP_ATOM_PADDING = 3.0;

	public enum MEASUREMENT { NONE(0), DISTANCE(2), ANGLE(3), TORSION(4);
		private final int requiredAtoms;
		MEASUREMENT(int requiredAtoms){
			this.requiredAtoms = requiredAtoms;
		}
		public int getRequiredAtoms() {
			return requiredAtoms;
		}
	}
	
	public enum ViewerSettings {
		 EDITING, SMALL_MOLS, SIDEPANEL, UPPERPANEL, WHITE_HYDROGENS, WHITE_BACKGROUND, BLUE_BACKGROUND, BLACK_BACKGROUND,
		 ROLE, ALLOW_PHARMACOPHORES, ATOM_INDEXES, INDIVIDUAL_ROTATION, ATOM_LEVEL_SELECTION
	}

	public static final EnumSet<ViewerSettings> CONFORMER_VIEW_MODE = EnumSet.of(ViewerSettings.BLUE_BACKGROUND, ViewerSettings.SMALL_MOLS, ViewerSettings.SIDEPANEL, ViewerSettings.ALLOW_PHARMACOPHORES);
	public static final EnumSet<ViewerSettings> VISUALIZATION_MINIMALIST_MODE = EnumSet.of(ViewerSettings.BLUE_BACKGROUND, ViewerSettings.SMALL_MOLS);
	public static final EnumSet<ViewerSettings> VISUALIZATION_EXTENDED_MODE = EnumSet.of(ViewerSettings.BLUE_BACKGROUND, ViewerSettings.SMALL_MOLS, ViewerSettings.EDITING);

	public static final EnumSet<ViewerSettings> GENERAL_MODE = EnumSet.of(
			ViewerSettings.EDITING, ViewerSettings.SIDEPANEL, ViewerSettings.WHITE_HYDROGENS,
			ViewerSettings.BLACK_BACKGROUND, ViewerSettings.UPPERPANEL,ViewerSettings.ROLE);
	
	private static final Color DISTANCE_COLOR = Color.TURQUOISE;
	private static final Color ANGLE_COLOR = Color.YELLOWGREEN;
	private static final Color TORSION_COLOR = Color.VIOLET;

	public V3DScene(Group root, double width, double height, EnumSet<V3DScene.ViewerSettings> settings) {
		super(root, width, height, true, SceneAntialiasing.BALANCED);
		mRoot = root;
		mBindingSiteHelper = null;
		mSettings = settings;
		mWorld = new V3DRotatableGroup("world");
		mEditor = new V3DMoleculeEditor();
		mDepthCuingIntensity = 0.8;
		mRoot.getChildren().add(mWorld);
		mRoot.setDepthTest(DepthTest.ENABLE);
		// gradients work well in a Scene, but don't seem to work in SubScenes
//		Stop[] stops = new Stop[] { new Stop(0, Color.MIDNIGHTBLUE), new Stop(1, Color.MIDNIGHTBLUE.darker().darker().darker())};
//		LinearGradient gradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, stops);

		setFill(Color.BLACK);
		buildLight();
		buildMainCamera();  // light first, because the camera positions the light
		mMeasurements = new ArrayList<V3DMeasurement>();
		new V3DMouseHandler(this);
		new V3DKeyHandler(this);
		mClipboardHandler = new ClipboardHandler();
		mMouseDragged = false;
		mMeasurementMode = MEASUREMENT.NONE;
		mPickedMolsList = new ArrayList<>();
		mOverrideHydrogens = true;
		mMoleculeColorID = 0;
		applySettings();
		mSceneListeners = new ArrayList<>();
		mChartProperty = new SimpleObjectProperty<XYChart<Number,Number>>();
		initializeDragAndDrop();
	}

	private void initializeDragAndDrop() {
		Platform.runLater(() -> {
			getScene().setOnDragOver((DragEvent event) -> {
				if (!event.getDragboard().getContentTypes().isEmpty())
					event.acceptTransferModes(TransferMode.ANY);

				event.consume();
			});

			getScene().setOnDragDropped((DragEvent event) -> {
				StereoMolecule mol = null;
				Object o = event.getDragboard().getContent(ChemistryDataFormats.DF_SERIALIZED_OBJECT);
				if (o instanceof StereoMolecule)
					mol = (StereoMolecule)o;

				if (mol == null) {
					o = event.getDragboard().getContent(ChemistryDataFormats.DF_IDCODE);
					if (o instanceof String)
						mol = new IDCodeParser(false).getCompactMolecule((String)o);
					}

				if (mol == null) {
					o = event.getDragboard().getContent(ChemistryDataFormats.DF_MDLMOLFILEV3);
					if (o == null)
						o = event.getDragboard().getContent(ChemistryDataFormats.DF_MDLMOLFILE);
					if (o instanceof String)
						new MolfileParser().getCompactMolecule((String)o);
					}

				if (mol != null && !mol.is3D())
					new ConformerGenerator().getOneConformerAsMolecule(mol);

				if (mol != null)
					addMolecule(new V3DMolecule(mol), true);

				event.consume();
			});
		} );
	}

	public void showMessage(String msg) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("Information");
		alert.setHeaderText(null);
		alert.setContentText(msg);
		alert.showAndWait();
	}

	public V3DPopupMenuController getPopupMenuController() {
		return mPopupMenuController;
	}

	public void setPopupMenuController(V3DPopupMenuController controller) {
		mPopupMenuController = controller;
	}

	public void addSceneListener(V3DSceneListener sl) {
		mSceneListeners.add(sl);
		}
	
	
	public boolean isOverrideHydrogenColor() {
		return mOverrideHydrogens;
	}

	public boolean isSplitAllBonds() {
		return mSettings.contains(ViewerSettings.ATOM_LEVEL_SELECTION);
	}
	
	public MEASUREMENT getMeasurementMode() {
		return mMeasurementMode;
	}
	
	public ArrayList<V3DMolecule> getPickedMolsList() {
		return mPickedMolsList;
	}

	public void setMeasurementMode(MEASUREMENT measurement) {
		for(V3DMolecule fxmol : mPickedMolsList)
			fxmol.clearPickedAtomList();
		mPickedMolsList.clear();
		mMeasurementMode = measurement;
		}
	
	public void cut(V3DMolecule fxmol) {
		copy3D(fxmol);
		delete(fxmol);
		}

	public void copy3D(V3DMolecule fxmol) {
		mCopiedMol = fxmol;
		mClipboardHandler.copyMolecule(fxmol.getMolecule());
		}

	public void copy2D(V3DMolecule fxmol) {
		mCopiedMol = fxmol;
		StereoMolecule mol = fxmol.getMolecule().getCompactCopy();
		mol.ensureHelperArrays(Molecule.cHelperParities);
		new CoordinateInventor().invent(mol);
		mClipboardHandler.copyMolecule(mol);
		}

	public void copyAsIDCode(V3DMolecule fxmol) {
		mCopiedMol = fxmol;
		Canonizer canonizer = new Canonizer(fxmol.getMolecule());
		TextClipboardHandler.copyText(canonizer.getIDCode()+" "+canonizer.getEncodedCoordinates());
		}


	public void copyAsMolfileV2(V3DMolecule fxmol) {
		mCopiedMol = fxmol;
		TextClipboardHandler.copyText(new MolfileCreator(fxmol.getMolecule()).getMolfile());
		}


	public void copyAsMolfileV3(V3DMolecule fxmol) {
		mCopiedMol = fxmol;
		TextClipboardHandler.copyText(new MolfileV3Creator(fxmol.getMolecule()).getMolfile());
		}


	public void copyAsSmiles(V3DMolecule fxmol) {
		mCopiedMol = fxmol;
		TextClipboardHandler.copyText(new IsomericSmilesCreator(fxmol.getMolecule()).getSmiles());
		}


	public void paste() {
		StereoMolecule mol = mClipboardHandler.pasteMolecule(false, SmilesParser.SMARTS_MODE_IS_SMILES);
		if (mol == null) {
			showMessage("No molecule on clipboard!");
			return;
			}

		boolean is3D = false;
		for (int atom=1; atom<mol.getAllAtoms(); atom++) {
			if (Math.abs(mol.getAtomZ(atom) - mol.getAtomZ(0)) > 0.1) {
				is3D = true;
				break;
				}
			}

		if (!is3D) {
			Conformer conformer = new ConformerGenerator().getOneConformer(mol);
			if (conformer == null) {
				showMessage("Conformer generation failed!");
				return;
				}
			conformer.toMolecule(mol);	// copy atom coordinates to molecule
			}

		V3DMolecule.MoleculeRole role = V3DMolecule.MoleculeRole.LIGAND;

		// if the previous molecule copied within this viewer has the same size as the clipboard molecule,
		// we assume that the clipboard molecule was previously copied from this viewer and use group & id from it
		if (is3D && mCopiedMol != null && mCopiedMol.getMolecule().getAllAtoms() == mol.getAllAtoms())
			role = mCopiedMol.getRole();

		V3DMolecule fxmol = new V3DMolecule(mol, V3DMolecule.getNextID(), role);
		mCopiedMol = null;
		addMolecule(fxmol, !getMolsInScene().isEmpty());
		optimizeView(fxmol);
		}
	

	public void delete(V3DRotatableGroup group) {
		if(group instanceof V3DMolecule) {
			removeMeasurements((V3DMolecule)group);
			((V3DMolecule)group).removeAllPharmacophores();
			((V3DMolecule)group).removeAtomIndexLabels();
		}
//		fxmol.deactivateEvents();
		mWorld.deleteGroup(group);
		for(V3DSceneListener listener : mSceneListeners)
			listener.removeGroup(group);
		}
	
	public void delete(List<? extends V3DRotatableGroup> groups) {
		for(V3DRotatableGroup fxmol:groups)
			delete(fxmol);
	}

	
	public void removeMeasurements(V3DMolecule fxmol) {
		ArrayList<V3DMeasurement> toDelete = new ArrayList<V3DMeasurement>();
		for(V3DMeasurement measurement : mMeasurements) {
			if (measurement.getV3DMolecules().contains(fxmol)){
				toDelete.add(measurement);
				measurement.cleanup();
				fxmol.removeMoleculeCoordinatesChangeListener(measurement);
			}
		}
		mMeasurements.removeAll(toDelete);
	}
	
	
	public void deleteInvisibleMolecules() {
		ArrayList<V3DMolecule> list = new ArrayList<>();
		for(V3DRotatableGroup fxmol : mWorld.getAllAttachedRotatableGroups()) {
			if(fxmol instanceof V3DMolecule &&!fxmol.isVisible())
				list.add((V3DMolecule)fxmol);
		}
		

		for (V3DMolecule fxmol:list)
			delete(fxmol);
	}

	public void deleteAllMolecules() {
		mMoleculeColorID = 0;
		ArrayList<V3DMolecule> list = new ArrayList<>();
		for(V3DRotatableGroup fxmol : mWorld.getAllAttachedRotatableGroups()) {
			if(fxmol instanceof V3DMolecule)
				list.add((V3DMolecule) fxmol);
		}
		
		for (V3DMolecule fxmol:list)
			delete(fxmol);
	}

	public void setAllVisible(boolean visible) {
		for(V3DRotatableGroup fxmol : mWorld.getAllAttachedRotatableGroups()) {
			fxmol.setVisible(visible);
		}
	}

	public void clearAll() {
		mMoleculeColorID = 0;
		for(V3DRotatableGroup group : mWorld.getAllAttachedRotatableGroups()) {
			delete(group);
			for(V3DSceneListener listener : mSceneListeners)
				listener.removeGroup(group);
		}
		for(V3DSceneListener listener : mSceneListeners)
			listener.initialize();
		mWorld.getChildren().clear();	// this does not remove the measurements
	}

	public void optimizeView() {
		optimizeView(mWorld);
	}
	
	/**
	 * Moves the camera such that x and y are at all atom's COG and that all atoms of visible molecules are just within the field of view.
	 */
	public void optimizeView(V3DRotatableGroup group) {
		Point3D cog = getCOGInScene(group);
		double cameraZ = 50;

		double hFOV = mCamera.getFieldOfView();
		double vFOV;
		if (mCamera.isVerticalFieldOfView()) {
			vFOV = hFOV;
			hFOV *= getWidth() / getHeight();
		}
		else {
			vFOV = hFOV * getHeight() / getWidth();
		}

		if (hFOV != 0 && vFOV != 0) {
			double tanH = 1.0 / Math.tan(0.9 * Math.PI * hFOV / 360);	// we need half FOV in radians and want the molecule to fill not more than 90%
			double tanV = 1.0 / Math.tan(0.9 * Math.PI * vFOV / 360);

			cameraZ = 10000;

			for(V3DRotatableGroup fxmol : mWorld.getAllAttachedRotatableGroups()) {
				if (fxmol.isVisible()) {
					for (Node node2:fxmol.getChildren()) {
						NodeDetail detail = (NodeDetail)node2.getUserData();
						if (detail != null) {
							if (detail.isAtom() || detail.isBond()) {
								Point3D p = node2.localToScene(0.0, 0.0, 0.0);
								cameraZ = Math.min(cameraZ, p.getZ() - tanH * Math.abs(p.getX()-cog.getX()));
								cameraZ = Math.min(cameraZ, p.getZ() - tanV * Math.abs(p.getY()-cog.getY()));
							}
						}
					}
				}
			}
		}
		setCameraXY(cog.getX(), cog.getY());
		setCameraZ(cameraZ);
	}

	public double[] getVisibleZRange() {
		double[] zr = new double[2];
		zr[0] = Double.MAX_VALUE;
		zr[1] = Double.MIN_VALUE;

		for(V3DRotatableGroup fxmol : mWorld.getAllAttachedRotatableGroups()) {
			if (fxmol.isVisible()) {
				for (Node node2:fxmol.getChildren()) {
					NodeDetail detail = (NodeDetail)node2.getUserData();
					if (detail != null) {
						if (detail.isAtom()) {
							Point3D p = node2.localToScene(0.0, 0.0, 0.0);
							if (zr[0] > p.getZ())
								zr[0] = p.getZ();
							if (zr[1] < p.getZ())
								zr[1] = p.getZ();
						}
					}
				}
			}
		}

		if (zr[0] == Double.MAX_VALUE) {
			zr[0] = CAMERA_NEAR_CLIP;
			zr[1] = CAMERA_FAR_CLIP;
		}
		else {
			zr[0] -= CLIP_ATOM_PADDING;
			zr[1] += CLIP_ATOM_PADDING;
		}

		return zr;
	}

	public double getMeanZ() {
		double meanZ = 0;
		int count = 0;

		for(V3DRotatableGroup fxmol : mWorld.getAllAttachedRotatableGroups()) {
			if (fxmol.isVisible()) {
				for (Node node:fxmol.getChildren()) {
					NodeDetail detail = (NodeDetail)node.getUserData();
					if (detail != null && detail.isAtom()) {
						Point3D p = node.localToScene(0.0, 0.0, 0.0);
						meanZ += p.getZ();
						count++;
					}
				}
			}
		}

		return (count == 0) ? 0.0 : meanZ / count;
	}

	public double[] getMinAndMaxZ() {
		double[] minAndMaxZ = new double[2];
		minAndMaxZ[0] = 1000000;
		minAndMaxZ[1] = -1000000;

		for(V3DRotatableGroup group : mWorld.getGroups())
			checkMinAndMax(group, minAndMaxZ);

		return minAndMaxZ[0] == 1000000 ? new double[2] : minAndMaxZ;
	}

	private void checkMinAndMax(V3DRotatableGroup group, double[] minAndMaxZ) {
		if (group.isVisible()) {
			if (group instanceof V3DMolecule) {
				StereoMolecule mol = ((V3DMolecule)group).getMolecule();
				for (int atom=0; atom<mol.getAllAtoms(); atom++) {
					Coordinates c = mol.getAtomCoordinates(atom);
					Point3D p = group.localToScene(c.x, c.y, c.z);
					if (minAndMaxZ[0] > p.getZ())
						minAndMaxZ[0] = p.getZ();
					else if (minAndMaxZ[1] < p.getZ())
						minAndMaxZ[1] = p.getZ();
				}
			}
			else {
				for (V3DRotatableGroup g : group.getGroups())
					checkMinAndMax(g, minAndMaxZ);
			}
		}
	}

	public Point3D getCOGInGroup(V3DRotatableGroup group) {
		int count = 0;
		double x = 0.0;
		double y = 0.0;
		double z = 0.0;

		for(V3DRotatableGroup rg : group.getAllAttachedRotatableGroups()) {
			if (rg.isVisible()) {
				if (rg instanceof V3DMolecule) {
					StereoMolecule mol = ((V3DMolecule)rg).getMolecule();
					if (mol.getAllAtoms() != 0) {
						Coordinates c = mol.getCenterOfGravity();
						Point3D p = new Point3D(c.x, c.y, c.z);
						Node owner = rg;
						while (owner != group) {   // some of the groups may be deeper in the tree
							p = owner.localToParent(p);
							owner = owner.getParent();
						}
						x += mol.getAllAtoms() * p.getX();
						y += mol.getAllAtoms() * p.getY();
						z += mol.getAllAtoms() * p.getZ();
						count += mol.getAllAtoms();
					}
				}
			}
		}

		return count == 0 ? new Point3D(0, 0, 0) : new Point3D(x/count, y/count, z/count);
	}

	/**
	 * Calculates and returns the center of gravity in scene coordinates
	 * of all atoms and bonds withing the given group not considering any node weights.
	 * @param group
	 * @return
	 */
	public Point3D getCOGInScene(V3DRotatableGroup group) {
		return group.localToScene(getCOGInGroup(group));
		}

	public void crop(V3DMolecule refMolFX, double distance) {
		Bounds refBounds = refMolFX.localToScene(refMolFX.getBoundsInLocal());
		ArrayList<V3DMolecule> moleculesToBeDeleted = new ArrayList<>();
		for (V3DRotatableGroup fxmol : mWorld.getAllAttachedRotatableGroups()) {
			if (fxmol instanceof V3DMolecule
			 && fxmol != refMolFX) {
				Bounds bounds = fxmol.localToScene(fxmol.getBoundsInLocal());
				if (refBounds.getMinX() - distance > bounds.getMaxX()
				 || refBounds.getMinY() - distance > bounds.getMaxY()
				 || refBounds.getMinZ() - distance > bounds.getMaxZ()
				 || refBounds.getMaxX() + distance < bounds.getMinX()
				 || refBounds.getMaxY() + distance < bounds.getMinY()
				 || refBounds.getMaxZ() + distance < bounds.getMinZ()) {
					moleculesToBeDeleted.add((V3DMolecule)fxmol);
				}
				else {
					StereoMolecule refMol = refMolFX.getMolecule();
					Point3D[] refPoint = new Point3D[refMol.getAllAtoms()];
					for (int atom=0; atom<refMol.getAllAtoms(); atom++) {
						Coordinates c = refMol.getCoordinates(atom);
						refPoint[atom] = fxmol.localToScene(c.x, c.y, c.z);
					}
					V3DMoleculeCropper cropper = new V3DMoleculeCropper((V3DMolecule)fxmol, distance, refPoint, refBounds);
					removeMeasurements((V3DMolecule)fxmol);
					cropper.crop();
					for (int type = 0; type<MoleculeSurfaceAlgorithm.SURFACE_TYPE.length; type++)
						((V3DMolecule)fxmol).cutSurface(type, cropper);
				}
			}
		}
		
		for (V3DMolecule fxmol:moleculesToBeDeleted)
			delete(fxmol);

		optimizeView();
	}
	
	public void addGroup(V3DRotatableGroup group, V3DRotatableGroup parent) {
		parent.addGroup(group);
	}

	public void addGroup(V3DRotatableGroup group) {
		addGroup(group, mWorld);
	}

	public void addMolecule(V3DMolecule fxmol, boolean assignIndividualColor) {
		addMolecule(fxmol, mWorld, assignIndividualColor);
	}

	public void addMolecule(V3DMolecule fxmol, V3DRotatableGroup group, boolean assignIndividualColor) {
		fxmol.setOverrideHydrogens(mOverrideHydrogens);
		Color color = CarbonAtomColorPalette.getColor(mMoleculeColorID++);
		if (fxmol.getColor() == null && assignIndividualColor)
			fxmol.setColor(color);
		group.addGroup(fxmol);
		updateDepthCueing();
		for(V3DSceneListener listener : mSceneListeners)
			listener.addGroup(fxmol);
	}
	
	public void applySettings() {
		if(mSettings.contains(ViewerSettings.WHITE_HYDROGENS))
			setOverrideHydrogens(false);
		if(mSettings.contains(ViewerSettings.WHITE_BACKGROUND))
			setFill(Color.WHITE);
		if(mSettings.contains(ViewerSettings.BLACK_BACKGROUND))
			setFill(Color.BLACK);
		if(mSettings.contains(ViewerSettings.BLUE_BACKGROUND))
			setFill(Color.MIDNIGHTBLUE);
	}
		
	public EnumSet<ViewerSettings> getSettings() {
		return mSettings;
	}

/*	public double getDistanceToScreenFactor(double z) {
		double fieldOfView = mCamera.getFieldOfView();
		double screenSize = mCamera.isVerticalFieldOfView() ? getHeight() : getWidth();
		double sizeAtZ0 = -mCamera.getTranslateZ() * Math.tan(Math.PI*fieldOfView/90);
		return 20;	// TODO calculate something reasonable
		}*/

	public V3DRotatableGroup getWorld() {
		return mWorld;
		}
	
	public void setMouseDragged(boolean mouseDragged) {
		mMouseDragged = mouseDragged;
	}
	
	public boolean isMouseDragged() {
		return mMouseDragged;
	}
	
	public void setOverrideHydrogens(boolean override) {
		mOverrideHydrogens = override;
	}

	public double getFieldOfView() {
		return mCamera.getFieldOfView();
		}

	/**
	 * @param polygon
	 * @param mode 0: normal, 1:add, 2:subtract
	 * @param paneOnScreen top let point of parent pane on screen
	 */
	public void select(Polygon polygon, int mode, Point2D paneOnScreen) {
		if (mSurfaceCutMode != 0) {
			if (mSurfaceCutMolecule != null) {
				mSurfaceCutMolecule.cutSurface(polygon, mSurfaceCutMode, paneOnScreen);
				}
			else {
				for(V3DRotatableGroup fxmol : mWorld.getAllAttachedRotatableGroups())
					if(fxmol instanceof V3DMolecule)
						((V3DMolecule)fxmol).cutSurface(polygon, mSurfaceCutMode, paneOnScreen);
			}
				
			mSurfaceCutMolecule = null;
			mSurfaceCutMode = 0;
			return;
		}

		for(V3DRotatableGroup fxmol : mWorld.getAllAttachedRotatableGroups())
			if(fxmol instanceof V3DMolecule)
				((V3DMolecule)fxmol).select(polygon, mode, paneOnScreen);
	}
		
	public void activateSurfaceCutter(int mode, V3DMolecule mol3D) {
		mSurfaceCutMode = mode;
		mSurfaceCutMolecule = mol3D;
		}

	public int getSurfaceCutMode() {
		return mSurfaceCutMode;
		}

	/**
	 * @param mol3D
	 * @param mode 0: normal, 1:add, 2:subtract
	 */
	public void selectMolecule(V3DMolecule mol3D, int mode) {
		if (mode == 0) {
			for(V3DRotatableGroup fxmol : mWorld.getAllAttachedRotatableGroups())
				if(fxmol instanceof V3DMolecule)
					if(((V3DMolecule)fxmol).isSelected() || fxmol==mol3D)
						((V3DMolecule)fxmol).toggleSelection();
		}
		else {
			if (mol3D != null)
				mol3D.toggleSelection();
			}
		}

	private void buildLight() {
		AmbientLight ambient = new AmbientLight(new Color(AMBIENT_LIGHT, AMBIENT_LIGHT, AMBIENT_LIGHT, 1.0));
		ambient.getScope().addAll(mWorld);

		mLight = new PointLight(new Color(0.8, 0.8, 0.8, 1.0));
		mLight.setTranslateX(LIGHT_X_OFFSET);
		mLight.setTranslateY(LIGHT_Y_OFFSET);
		mLight.setTranslateZ(CAMERA_INITIAL_Z + LIGHT_Z_OFFSET);
		mLight.getScope().addAll(mWorld);

		Group lightGroup = new Group();
		lightGroup.getChildren().addAll(ambient , mLight);
		mRoot.getChildren().addAll(lightGroup);
	}

	private void buildMainCamera() {
		mCamera = new PerspectiveCamera(true);
		mCamera.setNearClip(CAMERA_NEAR_CLIP);
		mCamera.setFarClip(CAMERA_FAR_CLIP);
		setCameraZ(CAMERA_INITIAL_Z);
		setCamera(mCamera);
		mRoot.getChildren().add(mCamera);
	}

	public void setCameraXY(double x, double y) {
		mCamera.setTranslateX(x);
		mCamera.setTranslateY(y);
		mLight.setTranslateX(x + LIGHT_X_OFFSET);
		mLight.setTranslateY(y + LIGHT_Y_OFFSET);
	}

	public void setCameraZ(double z) {
		mCamera.setTranslateZ(z);
		mLight.setTranslateZ(z + LIGHT_Z_OFFSET);
		updateDepthCueing();
	}

	public void updateDepthCueing() {
		if (mDepthCuingIntensity == 0) {
			mLight.setConstantAttenuation(1);
			mLight.setLinearAttenuation(0);
			mLight.setQuadraticAttenuation(0);
			return;
		}

		double camZ = mCamera.getTranslateZ();

		double[] minAndMaxZ = getMinAndMaxZ();

		// near and far points for which we assign known attenuation
		// values to then calculate the constants for the attenuation equation
		double width = minAndMaxZ[1] - minAndMaxZ[0];
		double nearZ = Math.max(camZ, minAndMaxZ[0] - 0.2 * width);    // must be >= camZ
		double farZ = Math.max(nearZ + 5, Math.max(nearZ + 50, minAndMaxZ[1]));

		double d_near = nearZ - camZ - LIGHT_Z_OFFSET;  // distances from light for these two points
		double d_far = farZ - camZ- LIGHT_Z_OFFSET;

		double dciFactor = Math.pow(4, mDepthCuingIntensity);
		double attnNear = Math.sqrt(dciFactor);  // desired attenuation values at near and far points
//		double attnNear = 1;  // desired attenuation values at near and far points
		double attnFar = 1.0 / dciFactor;

		// Attenuation equation in JFX for depth cuing: attn(d) = 1 / (c + l * d + q * d^2); d:distance c,l,q are constants

		// Calculate parameters for linear attenuation
//		double l = (1.0 / ln - 1.0 / lf) / (d_near - d_far);
//		double c = 1.0 / ln - d_near * l;
//		double q = 0.0;

		// Calculate parameters for quadratic attenuation
		double q = (1.0 / attnNear - 1.0 / attnFar) / (d_near * d_near - d_far * d_far);
		double c = 1.0 / attnNear - d_near * d_near * q;
		double l = 0.0;

		mLight.setConstantAttenuation(c);
		mLight.setLinearAttenuation(l);
		mLight.setQuadraticAttenuation(q);
/*
double dmin = (minAndMaxZ[0] - camZ - LIGHT_Z_OFFSET); // half distance to object front
double dmax = (minAndMaxZ[1] - camZ - LIGHT_Z_OFFSET); // half distance to object front
double lnear = 1 / (c + l * d_near + q * d_near * d_near);
double lfar = 1 / (c + l * d_far + q * d_far * d_far);
double lmin = 1 / (c + l * dmin + q * dmin * dmin);
double lmax = 1 / (c + l * dmax + q * dmax * dmax);
System.out.println();
System.out.println("Positions: lightZ:"+DoubleFormat.toString(mLight.getTranslateZ())+" camZ:"+DoubleFormat.toString(camZ)
		+" nearZ:"+DoubleFormat.toString(nearZ)+" farZ:"+DoubleFormat.toString(farZ)
		+" objNear:"+DoubleFormat.toString(minAndMaxZ[0])+" objFar:"+DoubleFormat.toString(minAndMaxZ[1]));
System.out.println("Distances: d_cam:"+DoubleFormat.toString(-LIGHT_Z_OFFSET)
		+" d_near:"+DoubleFormat.toString(d_near)+" d_far:"+DoubleFormat.toString(d_far)
		+" d_objNear:"+DoubleFormat.toString(dmin)+" d_objFar:"+DoubleFormat.toString(dmax));
System.out.println("Attenuations: attnNear:"+attnNear+" attnFar:"+attnFar);
System.out.println("Calculated q:"+DoubleFormat.toString(q)+" l:"+DoubleFormat.toString(l)+" c:"+DoubleFormat.toString(c)
		+" l_near:"+DoubleFormat.toString(lnear)+" l_far:"+DoubleFormat.toString(lfar)
		+" l_objNear:"+DoubleFormat.toString(lmin)+" l_objFar:"+DoubleFormat.toString(lmax));
*/
}

	public OneEyeView buildOneEyeView(final double eyeShift, int stereoMode, Screen targetScreen) {
		final PerspectiveCamera camera = new PerspectiveCamera(true);
		if (stereoMode == MODE_HSBS) {
			camera.setScaleX(2.0);
		}
		else if (stereoMode == MODE_HOU) {
			camera.setScaleX(0.5);
		}

		camera.setTranslateX(mCamera.getTranslateX() + eyeShift);
		camera.setTranslateY(mCamera.getTranslateY());
		camera.setTranslateZ(mCamera.getTranslateZ());
		camera.setNearClip(mCamera.getNearClip());
		camera.setFarClip(mCamera.getFarClip());

		mCamera.translateXProperty().addListener((observableValue, number, t1) -> camera.setTranslateX(mCamera.getTranslateX() + eyeShift));
		mCamera.translateYProperty().addListener((observableValue, number, t1) -> camera.setTranslateY(mCamera.getTranslateY()));
		mCamera.translateZProperty().addListener((observableValue, number, t1) -> camera.setTranslateZ(mCamera.getTranslateZ()));
		mCamera.nearClipProperty().addListener((observableValue, number, t1) -> camera.setNearClip(mCamera.getNearClip()));
		mCamera.farClipProperty().addListener((observableValue, number, t1) -> camera.setFarClip(mCamera.getFarClip()));

		return new OneEyeView(this, camera, targetScreen);
	}


	/*
	public void updateEditorAction(AbstractV3DEditorAction action) {
		for (Node node : mWorld.getChildren())
			if (node instanceof V3DMolecule) {
				((V3DMolecule) node).setEditorAction(action);
			}
	}
	*/
	
	public void setEditor(V3DMoleculeEditor editor) {
		mEditor = editor;
	}
	
	public V3DMoleculeEditor getEditor() {
		return mEditor;
	}
	
	public List<V3DMolecule> getMolsInScene() {
		V3DMolecule fxmol;
		ArrayList<V3DMolecule> fxmols = new ArrayList<V3DMolecule>();
		for (Node node : getWorld().getAllAttachedRotatableGroups()) {
			if (node instanceof V3DMolecule) {
				fxmol = (V3DMolecule)node;
				fxmols.add(fxmol);
			}
		}
		return fxmols;
	}
	
	public void tryAddMeasurement() {
		Set<V3DMolecule> mols = new HashSet<V3DMolecule>(mPickedMolsList);
		int pickedAtoms = 0;
		for(V3DMolecule fxmol : mols) {
			pickedAtoms += fxmol.getPickedAtoms().size();
		}
		if(pickedAtoms >= mMeasurementMode.getRequiredAtoms()) {
			Sphere[] pickedAtomList = new Sphere[mMeasurementMode.getRequiredAtoms()];
			Coordinates[] coords = new Coordinates[mMeasurementMode.getRequiredAtoms()];
			ArrayList<Integer> atIds = new ArrayList<Integer>();
			ArrayList<V3DMolecule> fxmols = new ArrayList<V3DMolecule>();
			int counter=0;
			for(V3DMolecule fxmol : mPickedMolsList) {
				pickedAtomList[counter] = fxmol.getPickedAtoms().removeFirst();
				fxmol.updateAppearance(pickedAtomList[counter]);
				int atid = ((NodeDetail) pickedAtomList[counter].getUserData()).getAtom();
				atIds.add(atid);
				fxmols.add(fxmol);
				Coordinates c = fxmol.getMolecule().getCoordinates(atid);
				//Point3D globalCoords = fxmol.localToParent(c.x,c.y,c.z);
				Coordinates worldPoint =  fxmol.getWorldCoordinates(this, c);
				Point3D globalCoords = new Point3D(worldPoint.x, worldPoint.y, worldPoint.z);
				coords[counter] = new Coordinates(globalCoords.getX(),globalCoords.getY(),globalCoords.getZ());
				counter++;
			}		
			if (mMeasurementMode == MEASUREMENT.DISTANCE) {
				double dist = coords[0].distance(coords[1]);
				addMeasurementNodes(coords[0],coords[1], DISTANCE_COLOR,
								DoubleFormat.toString(dist,3),atIds,fxmols);
			}
			else if(mMeasurementMode == MEASUREMENT.ANGLE) {
				Coordinates v1 = coords[0].subC(coords[1]);
				Coordinates v2 = coords[2].subC(coords[1]);
				double angle = v1.getAngle(v2);
				angle = 180*angle/Math.PI;
				addMeasurementNodes(coords[0], coords[2], ANGLE_COLOR, DoubleFormat.toString(angle,3),atIds,fxmols);
			}
			
			else if(mMeasurementMode == MEASUREMENT.TORSION) {
				double dihedral = Coordinates.getDihedral(coords[0],coords[1],coords[2],coords[3]);
				dihedral = 180*dihedral/Math.PI;
				addMeasurementNodes(coords[0], coords[3], TORSION_COLOR, DoubleFormat.toString(dihedral,3),atIds,fxmols);
			}
	
			mPickedMolsList.clear();
		}
	}
	
	private void addMeasurementNodes(Coordinates c1, Coordinates c2, Color color, String text, ArrayList<Integer> atoms, ArrayList<V3DMolecule> fxmols) {
		Point3D p1 = new Point3D(c1.x,c1.y,c1.z);
		Point3D p2 = new Point3D(c2.x,c2.y,c2.z);
		DashedRod line = new DashedRod(p1, p2, color);
		NonRotatingLabel label = NonRotatingLabel.create(mWorld, text, p1.midpoint(p2), color);
		label.setLabelDeletionListener(this);
		V3DMeasurement measurement = new V3DMeasurement(this,atoms,fxmols,line,label,mWorld);
		mMeasurements.add(measurement);
		}
	
	public ArrayList<V3DMeasurement> getMeasurements() {
		return mMeasurements;
	}

	public void removeMeasurements() {
		for(V3DMeasurement measurement: mMeasurements)
			measurement.cleanup();

		mMeasurements.clear();
	}

	public void moveToGroup(List<V3DRotatableGroup> toMove, V3DRotatableGroup target) {
		List<V3DRotatableGroup> targetChildren = target.getAllAttachedRotatableGroups();
		List<V3DRotatableGroup> notToMove = new ArrayList<V3DRotatableGroup>(); //MolGroups that are subGroups of other groups that will be moved, shouldn't be moved separately
		for(V3DRotatableGroup group1 : toMove) {
			if(targetChildren.contains(group1))
				notToMove.add(group1);
			List<V3DRotatableGroup> group1Children = group1.getAllAttachedRotatableGroups();
			for(V3DRotatableGroup group2 : toMove) {
				if(group1==group2)
					continue;
				if(group1Children.contains(group2))
					notToMove.add(group2);
			}
		}
		toMove.removeAll(notToMove);
		this.delete(toMove);
		for(V3DRotatableGroup group : toMove)
			target.addGroup(group);
		
	}
	
	public V3DRotatableGroup getParent(V3DRotatableGroup child) {
		boolean foundParent = false;
		V3DRotatableGroup parent = null;
		LinkedList<V3DRotatableGroup> queue = new LinkedList<>();
		queue.add(mWorld);
		Set<V3DRotatableGroup> visited = new HashSet<V3DRotatableGroup>();
		while(!queue.isEmpty() && !foundParent ) {
			V3DRotatableGroup candidate = queue.poll();
			if(visited.contains(candidate))
				continue;
			else 
				visited.add(candidate);
			if(candidate.getGroups().contains(child)) {
				parent = candidate;
				foundParent = true;
			}
			else {
				candidate.getGroups().stream().forEach(e -> queue.add(e));
			}
		}
		return parent;
		
		
	}

	@Override
	public void labelDeleted(Label l) {
		ArrayList<V3DMeasurement> toBeRemoved = new ArrayList<V3DMeasurement>();
		for(V3DMeasurement measurement: mMeasurements) {
			if(measurement.getLabel().equals(l)) {
				measurement.cleanup();
				toBeRemoved.add(measurement);
				for(V3DRotatableGroup fxmol : mWorld.getAllAttachedRotatableGroups())
					if(fxmol instanceof V3DMolecule)
						((V3DMolecule)fxmol).removeMoleculeCoordinatesChangeListener(measurement);
			}
		}

		mMeasurements.removeAll(toBeRemoved);
	}
	
	public ObjectProperty<XYChart<Number,Number>> chartProperty() {
		return mChartProperty;
	}

	public boolean isShowInteractions() {
		return mInteractionHandler != null && mInteractionHandler.isVisible();
	}

	public void setShowInteractions(boolean b) {
		if (b != isShowInteractions()) {
			if (mInteractionHandler == null)
				mInteractionHandler = new InteractionHandler(this, new PLIPInteractionCalculator());    // PLIP is currently the only supported option
			mInteractionHandler.setVisibible(b);
		}
	}

	public double getDepthCueingIntensity() {
		return mDepthCuingIntensity;
	}

	public void setDepthCueingIntensity(double dci) {
		mDepthCuingIntensity = dci;
		updateDepthCueing();
	}

	public V3DBindingSite getBindingSiteHelper() {
		return mBindingSiteHelper;
	}

	public void setBindingSiteHelper(V3DBindingSite mBindingSiteHelper) {
		this.mBindingSiteHelper = mBindingSiteHelper;
	}
}

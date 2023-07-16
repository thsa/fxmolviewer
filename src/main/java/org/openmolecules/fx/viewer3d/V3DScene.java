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
import org.openmolecules.chem.conf.gen.ConformerGenerator;
import org.openmolecules.fx.viewer3d.interactions.V3DInteractionHandler;
import org.openmolecules.fx.viewer3d.nodes.DashedRod;
import org.openmolecules.fx.viewer3d.nodes.NodeDetail;
import org.openmolecules.fx.viewer3d.nodes.NonRotatingLabel;
import org.openmolecules.mesh.MoleculeSurfaceAlgorithm;

import java.util.*;


public class V3DScene extends SubScene implements LabelDeletionListener {
	private ClipboardHandler mClipboardHandler;
	private Group mRoot;                  	// not rotatable, contains light and camera
	private V3DMolGroup mWorld;		// rotatable, not movable, root in center of scene, contains all visible objects
	private List<V3DSceneListener> mSceneListeners;
	private int mSurfaceCutMode;
	private V3DMolecule mSurfaceCutMolecule;
	private V3DMoleculeEditor mEditor;
	private boolean mMouseDragged; //don't place molecule fragments if mouse is released after a drag event
	private ArrayList<V3DMolecule> mPickedMolsList;
	private MEASUREMENT     mMeasurementMode;
	private ArrayList<V3DMeasurement> mMeasurements;
	private V3DMolecule mCopiedMol;
	private volatile V3DPopupMenuController mPopupMenuController;
	private EnumSet<ViewerSettings> mSettings;
	private boolean mMayOverrideHydrogens;
	private int mMoleculeColorID;
	private V3DBindingSite mBindingSiteHelper;
	private V3DInteractionHandler mInteractionHandler;
	private ObjectProperty<XYChart<Number,Number>> mChartProperty; //for graphs and charts that are created by interaction with the scene (e.g. hovering over a torsion angle) 


	public static final Color SELECTION_COLOR = Color.TURQUOISE;
	protected static final double CAMERA_INITIAL_DISTANCE = 45;
	protected static final double CAMERA_FIELD_OF_VIEW = 30.0;	// default field of view
	protected static final double CAMERA_NEAR_CLIP = 1.0;
	protected static final double CAMERA_FAR_CLIP = 1000.0;
	protected static final double CAMERA_MIN_CLIP_THICKNESS = 2.0;
	private static final double EYE_DISTANCE = 0.5;
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
		 ROLE, ALLOW_PHARMACOPHORES, ATOM_INDEXES, INDIVIDUAL_ROTATION, STEREO_HSBS, STEREO_HOU
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
		mWorld = new V3DMolGroup("world");
		mEditor = new V3DMoleculeEditor();
		mRoot.getChildren().add(mWorld);
		mRoot.setDepthTest(DepthTest.ENABLE);
		// gradients work well in a Scene, but don't seem to work in SubScenes
//		Stop[] stops = new Stop[] { new Stop(0, Color.MIDNIGHTBLUE), new Stop(1, Color.MIDNIGHTBLUE.darker().darker().darker())};
//		LinearGradient gradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, stops);

		setFill(Color.BLACK);
		buildLight();
		buildCamera();
		mMeasurements = new ArrayList<V3DMeasurement>();
		new V3DMouseHandler(this);
		new V3DKeyHandler(this);
		mClipboardHandler = new ClipboardHandler();
		mMouseDragged = false;
		mMeasurementMode = MEASUREMENT.NONE;
		mPickedMolsList = new ArrayList<>();
		mMayOverrideHydrogens = true;
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
				if (o != null && o instanceof StereoMolecule)
					mol = (StereoMolecule)o;

				if (mol == null) {
					o = event.getDragboard().getContent(ChemistryDataFormats.DF_IDCODE);
					if (o != null && o instanceof String)
						mol = new IDCodeParser(false).getCompactMolecule((String)o);
					}

				if (mol == null) {
					o = event.getDragboard().getContent(ChemistryDataFormats.DF_MDLMOLFILEV3);
					if (o == null)
						o = event.getDragboard().getContent(ChemistryDataFormats.DF_MDLMOLFILE);
					if (o != null && o instanceof String)
						new MolfileParser().getCompactMolecule((String)o);
					}

				if (mol != null && !mol.is3D())
					new ConformerGenerator().getOneConformerAsMolecule(mol);

				if (mol != null)
					addMolecule(new V3DMolecule(mol));

				event.consume();
			});
		} );
	}

	private void showMessage(String msg) {
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
	
	
	public boolean mayOverrideHydrogenColor() {
		return mMayOverrideHydrogens;
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
		addMolecule(fxmol);
		}
	

	public void delete(V3DMolGroup fxmol) {
		if(fxmol instanceof V3DMolecule) {
			removeMeasurements((V3DMolecule)fxmol);
			((V3DMolecule)fxmol).removeAllPharmacophores();
			((V3DMolecule)fxmol).removeAtomIndexLabels();
		}
//		fxmol.deactivateEvents();
		mWorld.deleteMolecule(fxmol);
		for(V3DSceneListener listener : mSceneListeners)
			listener.removeMolecule(fxmol);
		}
	
	public void delete(List<? extends V3DMolGroup> fxmols) {
		for(V3DMolGroup fxmol:fxmols)
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
		for(V3DMolGroup fxmol : mWorld.getAllAttachedMolGroups()) {
			if(fxmol instanceof V3DMolecule &&!fxmol.isVisible())
				list.add((V3DMolecule)fxmol);
		}
		

		for (V3DMolecule fxmol:list)
			delete(fxmol);
	}

	public void deleteAllMolecules() {
		mMoleculeColorID = 0;
		ArrayList<V3DMolecule> list = new ArrayList<>();
		for(V3DMolGroup fxmol : mWorld.getAllAttachedMolGroups()) {
			if(fxmol instanceof V3DMolecule)
				list.add((V3DMolecule) fxmol);
		}
		
		for (V3DMolecule fxmol:list)
			delete(fxmol);
	}

	public void setAllVisible(boolean visible) {
		for(V3DMolGroup fxmol : mWorld.getAllAttachedMolGroups()) {
			fxmol.setVisible(visible);
		}
	}

	public void clearAll() {
		mMoleculeColorID = 0;
		for(V3DMolGroup fxmol : mWorld.getAllAttachedMolGroups()) {
			delete(fxmol);
			for(V3DSceneListener listener : mSceneListeners)
				listener.removeMolecule(fxmol);
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
	public void optimizeView(V3DMolGroup group) {
		Point3D cog = getCOGInScene(group);
		double cameraZ = 50;

		double hFOV = ((PerspectiveCamera)getCamera()).getFieldOfView();
		double vFOV;
		if (((PerspectiveCamera)getCamera()).isVerticalFieldOfView()) {
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

			for(V3DMolGroup fxmol : mWorld.getAllAttachedMolGroups()) {
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

		getCamera().setTranslateX(cog.getX());
		getCamera().setTranslateY(cog.getY());
		getCamera().setTranslateZ(cameraZ);
	}

	public double[] getVisibleZRange() {
		double[] zr = new double[2];
		zr[0] = Double.MAX_VALUE;
		zr[1] = Double.MIN_VALUE;

		for(V3DMolGroup fxmol : mWorld.getAllAttachedMolGroups()) {
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

	public Point3D getCOGInGroup(V3DMolGroup group) {
		int count = 0;
		double x = 0.0;
		double y = 0.0;
		double z = 0.0;

		for(V3DMolGroup fxmol : group.getAllAttachedMolGroups()) {
			if (fxmol.isVisible()) {
				for (Node node2:fxmol.getChildren()) {
					NodeDetail detail = (NodeDetail)node2.getUserData();
					if (detail != null) {
						if (detail.isAtom() || detail.isBond()) {
							Point3D p = node2.localToParent(0.0, 0.0, 0.0);
							x += p.getX();
							y += p.getY();
							z += p.getZ();
							count++;
							}
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
	public Point3D getCOGInScene(V3DMolGroup group) {
		return group.localToScene(getCOGInGroup(group));
		}

	public void crop(V3DMolecule refMolFX, double distance) {
		Bounds refBounds = refMolFX.localToScene(refMolFX.getBoundsInLocal());
		ArrayList<V3DMolecule> moleculesToBeDeleted = new ArrayList<>();
		for (V3DMolGroup fxmol : mWorld.getAllAttachedMolGroups()) {
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
	
	public void addMolGroup(V3DMolGroup group, V3DMolGroup parent) {
		parent.addMolGroup(group);
	}

	public void addMolGroup(V3DMolGroup group) {
		addMolGroup(group, mWorld);
	}

	public void addMolecule(V3DMolecule fxmol) {
		addMolecule(fxmol, mWorld);
	}

	public void addMolecule(V3DMolecule fxmol, V3DMolGroup group) {
		fxmol.setOverrideHydrogens(mMayOverrideHydrogens);
		Color color = CarbonAtomColorPalette.getColor(mMoleculeColorID++);
		if (fxmol.getColor() == null)
			fxmol.setColor(color);
		group.addMolGroup(fxmol);
		for(V3DSceneListener listener : mSceneListeners)
			listener.addMolecule(fxmol);
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
		PerspectiveCamera camera = (PerspectiveCamera)getCamera();
		double fieldOfView = camera.getFieldOfView();
		double screenSize = camera.isVerticalFieldOfView() ? getHeight() : getWidth();
		double sizeAtZ0 = -camera.getTranslateZ() * Math.tan(Math.PI*fieldOfView/90);
		return 20;	// TODO calculate something reasonable
		}*/

	public V3DMolGroup getWorld() {
		return mWorld;
		}
	
	public void setMouseDragged(boolean mouseDragged) {
		mMouseDragged = mouseDragged;
	}
	
	public boolean isMouseDragged() {
		return mMouseDragged;
	}
	
	public void setOverrideHydrogens(boolean override) {
		mMayOverrideHydrogens = override;
	}

	public double getFieldOfView() {
		return ((PerspectiveCamera)getCamera()).getFieldOfView();
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
				for(V3DMolGroup fxmol : mWorld.getAllAttachedMolGroups()) {
					if(fxmol instanceof V3DMolecule)
						((V3DMolecule)fxmol).cutSurface(polygon, mSurfaceCutMode, paneOnScreen);
				}
				
			mSurfaceCutMolecule = null;
			mSurfaceCutMode = 0;
			return;
			}

			for(V3DMolGroup fxmol : mWorld.getAllAttachedMolGroups()) {
				if(fxmol instanceof V3DMolecule)
					((V3DMolecule)fxmol).select(polygon, mode, paneOnScreen);
			}
		}
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
			for(V3DMolGroup fxmol : mWorld.getAllAttachedMolGroups())
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
		AmbientLight light1=new AmbientLight(new Color(0.3, 0.3, 0.3, 1.0));
		light1.getScope().addAll(mWorld);

		PointLight light2=new PointLight(new Color(0.8, 0.8, 0.8, 1.0));
		light2.setTranslateX(-100);
		light2.setTranslateY(-100);
		light2.setTranslateZ(-200);
		light2.getScope().addAll(mWorld);

		Group lightGroup = new Group();
		lightGroup.getChildren().addAll(light1, light2);
		mRoot.getChildren().addAll(lightGroup);
		}

	private void buildCamera() {
		PerspectiveCamera camera = new PerspectiveCamera(true);
		camera.setNearClip(CAMERA_NEAR_CLIP);
		camera.setFarClip(CAMERA_FAR_CLIP);
		camera.setTranslateZ(-CAMERA_INITIAL_DISTANCE);
		if (mSettings.contains(ViewerSettings.STEREO_HSBS)) {
			camera.setScaleX(2.0);
			camera.setTranslateX(-EYE_DISTANCE/2);
		}
		else if (mSettings.contains(ViewerSettings.STEREO_HOU)) {
			camera.setScaleY(2.0);
			camera.setTranslateX(-EYE_DISTANCE/2);
		}
		setCamera(camera);
		mRoot.getChildren().add(camera);
		}

	public RightEyeView buildRightEyeView() {
		PerspectiveCamera camera = new PerspectiveCamera(true);
		camera.setNearClip(CAMERA_NEAR_CLIP);
		camera.setFarClip(CAMERA_FAR_CLIP);
		camera.setTranslateZ(-CAMERA_INITIAL_DISTANCE);
		if (mSettings.contains(ViewerSettings.STEREO_HSBS)) {
			camera.setScaleX(2.0);
		}
		else if (mSettings.contains(ViewerSettings.STEREO_HOU)) {
			camera.setScaleY(2.0);
		}
		camera.setTranslateX(EYE_DISTANCE/2);

		getCamera().translateXProperty().addListener((observableValue, number, t1) -> camera.setTranslateX(getCamera().getTranslateX() + EYE_DISTANCE));
		getCamera().translateYProperty().addListener((observableValue, number, t1) -> camera.setTranslateY(getCamera().getTranslateY()));
		getCamera().translateZProperty().addListener((observableValue, number, t1) -> camera.setTranslateZ(getCamera().getTranslateZ()));

		RightEyeView view = new RightEyeView(this, camera);

		view.fitWidthProperty().bind(widthProperty());
		view.fitHeightProperty().bind(heightProperty());

		return view;
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
		for (Node node : getWorld().getAllAttachedMolGroups()) {
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
		if(pickedAtoms<mMeasurementMode.getRequiredAtoms())
			return;
		else {
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

	public void moveToGroup(List<V3DMolGroup> toMove, V3DMolGroup target) {
		List<V3DMolGroup> targetChildren = target.getAllAttachedMolGroups();
		List<V3DMolGroup> notToMove = new ArrayList<V3DMolGroup>(); //MolGroups that are subGroups of other groups that will be moved, shouldn't be moved separately
		for(V3DMolGroup group1 : toMove) {
			if(targetChildren.contains(group1))
				notToMove.add(group1);
			List<V3DMolGroup> group1Children = group1.getAllAttachedMolGroups();
			for(V3DMolGroup group2 : toMove) {
				if(group1==group2)
					continue;
				if(group1Children.contains(group2))
					notToMove.add(group2);
			}
		}
		toMove.removeAll(notToMove);
		this.delete(toMove);
		for(V3DMolGroup group : toMove)
			target.addMolGroup(group);
		
	}
	
	public V3DMolGroup getParent(V3DMolGroup child) {
		boolean foundParent = false;
		V3DMolGroup parent = null;
		V3DMolGroup root = mWorld;
		LinkedList<V3DMolGroup> queue = new LinkedList<>();
		queue.add(root); 
		Set<V3DMolGroup> visited = new HashSet<V3DMolGroup>();
		while(!queue.isEmpty() && !foundParent ) {
			V3DMolGroup candidate = queue.poll();
			if(visited.contains(candidate))
				continue;
			else 
				visited.add(candidate);
			if(candidate.getMolGroups().contains(child)) {
				parent = candidate;
				foundParent = true;
			}
			else {
				candidate.getMolGroups().stream().forEach(e -> queue.add(e));
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
				for(V3DMolGroup fxmol : mWorld.getAllAttachedMolGroups())
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
		if(mInteractionHandler==null)
			mInteractionHandler = new V3DInteractionHandler(this);
		mInteractionHandler.setVisibible(b);
	}
	
	public V3DInteractionHandler getInteractionHandler() {
		return mInteractionHandler;
	}
	
	public void setInteractionHandler(V3DInteractionHandler handler) {
		mInteractionHandler = handler;
	}

	public V3DBindingSite getBindingSiteHelper() {
		return mBindingSiteHelper;
	}

	public void setBindingSiteHelper(V3DBindingSite mBindingSiteHelper) {
		this.mBindingSiteHelper = mBindingSiteHelper;
	}
}

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
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.chem.coords.CoordinateInventor;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.util.DoubleFormat;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Sphere;

import org.openmolecules.chem.conf.gen.ConformerGenerator;
import org.openmolecules.fx.viewer3d.nodes.DashedRod;
import org.openmolecules.fx.viewer3d.nodes.NodeDetail;
import org.openmolecules.fx.viewer3d.nodes.NonRotatingLabel;
import org.openmolecules.mesh.MoleculeSurfaceAlgorithm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class V3DScene extends SubScene implements LabelDeletionListener {
	private ClipboardHandler mClipboardHandler;
	private Group mRoot;                  	// not rotatable, contains light and camera
	private RotatableGroup mWorld;		// rotatable, not movable, root in center of scene, contains all visible objects
	private V3DMouseHandler mMouseHandler;
	private V3DKeyHandler mKeyHandler;
	private V3DSceneListener mSceneListener;
	private boolean mIsIndividualRotationModus;
	private int mSurfaceCutMode;
	private V3DMolecule mSurfaceCutMolecule;
	private V3DMoleculeEditor mEditor;
	private boolean mMouseDragged; //don't place molecule fragments if mouse is released after a drag event
	private ArrayList<V3DMolecule> mPickedMolsList;
	private MEASUREMENT     mMeasurementMode;
	private ArrayList<NonRotatingLabel> mLabelList;
	private ArrayList<V3DMeasurement> mMeasurements;
	
	public static final Color SELECTION_COLOR = Color.TURQUOISE;
	protected static final double CAMERA_INITIAL_DISTANCE = 45;
	protected static final double CAMERA_FIELD_OF_VIEW = 30.0;	// default field of view
	protected static final double CAMERA_NEAR_CLIP = 1.0;
	protected static final double CAMERA_FAR_CLIP = 1000.0;
	protected static final double CAMERA_MIN_CLIP_THICKNESS = 2.0;
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
	
	private static final Color DISTANCE_COLOR = Color.TURQUOISE;
	private static final Color ANGLE_COLOR = Color.YELLOWGREEN;
	private static final Color TORSION_COLOR = Color.VIOLET;


	public V3DScene(Group root, double width, double height) {
		super(root , width, height, true, SceneAntialiasing.BALANCED);
		mRoot = root;

		mWorld = new RotatableGroup();
		mEditor = new V3DMoleculeEditor();
		mRoot.getChildren().add(mWorld);
		mRoot.setDepthTest(DepthTest.ENABLE);

		// gradients work well in a Scene, but don't seem to work in SubScenes
//		Stop[] stops = new Stop[] { new Stop(0, Color.MIDNIGHTBLUE), new Stop(1, Color.MIDNIGHTBLUE.darker().darker().darker())};
//		LinearGradient gradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, stops);
		setFill(Color.MIDNIGHTBLUE.darker().darker());

		buildLight();
		buildCamera();
		mMeasurements = new ArrayList<V3DMeasurement>();
		mMouseHandler = new V3DMouseHandler(this);
		mKeyHandler = new V3DKeyHandler(this);
		mClipboardHandler = new ClipboardHandler();
		mMouseDragged = false;
		mMeasurementMode = MEASUREMENT.NONE;
		mPickedMolsList = new ArrayList<V3DMolecule>();
		mLabelList = new ArrayList<NonRotatingLabel>();
		}

	public void setSceneListener(V3DSceneListener sl) {
		mSceneListener = sl;
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
	
	public boolean isIndividualRotationModus() {
		return mIsIndividualRotationModus;
		}

	public void setIndividualRotationModus(boolean b) {
		mIsIndividualRotationModus = b;
		}

	public void cut(V3DMolecule fxmol) {
		copy3D(fxmol);
		delete(fxmol);
		}

	public void copy3D(V3DMolecule fxmol) {
		mClipboardHandler.copyMolecule(fxmol.getMolecule());
		}

	public void copy2D(V3DMolecule fxmol) {
		StereoMolecule mol = fxmol.getMolecule().getCompactCopy();
		new CoordinateInventor().invent(mol);
		mClipboardHandler.copyMolecule(mol);
		}

	public void paste() {
		StereoMolecule mol = mClipboardHandler.pasteMolecule(false);
		if (mol == null) {   // TODO interactive message
			System.out.println("No molecule on clipboard!");
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
			if (conformer == null) {    // TODO interactive message
				System.out.println("Conformer generation failed!");
				return;
				}
			conformer.toMolecule(mol);	// copy atom coordinates to molecule
			}

		V3DMolecule fxmol = new V3DMolecule(mol);
//		fxmol.activateEvents();
		addMolecule(fxmol);
		}

	public void delete(V3DMolecule fxmol) {
		removeMeasurements(fxmol);
		fxmol.removePharmacophore();
//		fxmol.deactivateEvents();
		mWorld.getChildren().remove(fxmol);
		if (mSceneListener != null)
			mSceneListener.removeMolecule(fxmol);
		}

	
	public void removeMeasurements(V3DMolecule fxmol) {
		ArrayList<V3DMeasurement> toDelete = new ArrayList<V3DMeasurement>();
		for(V3DMeasurement measurement : mMeasurements) {
			if (measurement.getV3DMolecules().contains(fxmol)){
				toDelete.add(measurement);
				measurement.cleanup();
				fxmol.removeMoleculeChangeListener(measurement);
			}
		}
		mMeasurements.removeAll(toDelete);
	}
	
	
	public void deleteInvisibleMolecules() {
		ArrayList<V3DMolecule> list = new ArrayList<>();
		for (Node node : mWorld.getChildren())
			if (node instanceof V3DMolecule && !node.isVisible())
				list.add((V3DMolecule) node);
		for (V3DMolecule fxmol:list)
			delete(fxmol);
	}

	public void deleteAllMolecules() {
		ArrayList<V3DMolecule> list = new ArrayList<>();
		for (Node node : mWorld.getChildren())
			if (node instanceof V3DMolecule)
				list.add((V3DMolecule) node);
		for (V3DMolecule fxmol:list)
			delete(fxmol);
	}

	public void setAllVisible(boolean visible) {
		for (Node node : mWorld.getChildren())
			if (node instanceof V3DMolecule)
				node.setVisible(visible);
		}

	public void clearAll(boolean isSmallMoleculeMode) {
		for (Node node:mWorld.getChildren()) {
			if (node instanceof V3DMolecule) {
				//((V3DMolecule) node).removeMeasurements();
				if (mSceneListener != null)
					mSceneListener.removeMolecule((V3DMolecule) node);
			}
		}
		if (mSceneListener != null)
			mSceneListener.initialize(isSmallMoleculeMode);
		mWorld.getChildren().clear();	// this does not remove the measurements
	}

	/**
	 * Moves all nodes such that the center of gravity of all atoms is world center (0,0,0).
	 * Moves the camera such that x=0, y=0 and z<0, such that all atoms of visible molecules are just within the field of view.
	 */
	public void optimizeView() {
		Point3D cog = getCenterOfGravity();
		for (Node n:mWorld.getChildren()) {
			Point3D p = mWorld.sceneToLocal(cog);
			n.setTranslateX(n.getTranslateX() - p.getX());
			n.setTranslateY(n.getTranslateY() - p.getY());
			n.setTranslateZ(n.getTranslateZ() - p.getZ());
		}

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
			double tanH = Math.tan(0.9 * Math.PI * hFOV / 360);	// we need half FOV in radians and want the molecule to fill not more than 90%
			double tanV = Math.tan(0.9 * Math.PI * vFOV / 360);

			cameraZ = 0;

			for (Node node1:mWorld.getChildren()) {
				if (node1 instanceof V3DMolecule) {
					V3DMolecule fxmol = (V3DMolecule)node1;
					if (fxmol.isVisible()) {
						for (Node node2:fxmol.getChildren()) {
							NodeDetail detail = (NodeDetail)node2.getUserData();
							if (detail != null) {
								if (detail.isAtom()) {
									Point3D p = node2.localToScene(0.0, 0.0, 0.0);
									cameraZ = Math.min(cameraZ, p.getZ() - Math.abs(p.getX()) / tanH);
									cameraZ = Math.min(cameraZ, p.getZ() - Math.abs(p.getY()) / tanV);
								}
							}
						}
					}
				}
			}
		}

		getCamera().setTranslateX(0);
		getCamera().setTranslateY(0);
		getCamera().setTranslateZ(cameraZ);
	}

	/**
	 *
	 * @return
	 */
	public double[] getVisibleZRange() {
		double[] zr = new double[2];
		zr[0] = Double.MAX_VALUE;
		zr[1] = Double.MIN_VALUE;
		for (Node node1:mWorld.getChildren()) {
			if (node1 instanceof V3DMolecule) {
				V3DMolecule fxmol = (V3DMolecule)node1;
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

	public Point3D getCenterOfGravity() {
		int atomCount = 0;
		double x = 0.0;
		double y = 0.0;
		double z = 0.0;
		for (Node node1:mWorld.getChildren()) {
			if (node1 instanceof V3DMolecule) {
				V3DMolecule fxmol = (V3DMolecule)node1;
				if (fxmol.isVisible()) {
					for (Node node2:fxmol.getChildren()) {
						NodeDetail detail = (NodeDetail)node2.getUserData();
						if (detail != null) {
							if (detail.isAtom()) {
								Point3D p = node2.localToScene(0.0, 0.0, 0.0);
								x += p.getX();
								y += p.getY();
								z += p.getZ();
								atomCount++;
							}
						}
					}
				}
			}
		}

		return new Point3D(x / atomCount, y / atomCount, z / atomCount);
	}

	public void crop(V3DMolecule refMolFX, double distance) {
		Bounds refBounds = refMolFX.localToScene(refMolFX.getBoundsInLocal());
		ArrayList<V3DMolecule> moleculesToBeDeleted = new ArrayList<>();
		for (Node node:mWorld.getChildren()) {
			if (node instanceof V3DMolecule && node != refMolFX) {
				V3DMolecule fxmol = (V3DMolecule) node;
				Bounds bounds = fxmol.localToScene(fxmol.getBoundsInLocal());
				if (refBounds.getMinX() - distance > bounds.getMaxX()
				 || refBounds.getMinY() - distance > bounds.getMaxY()
				 || refBounds.getMinZ() - distance > bounds.getMaxZ()
				 || refBounds.getMaxX() + distance < bounds.getMinX()
				 || refBounds.getMaxY() + distance < bounds.getMinY()
				 || refBounds.getMaxZ() + distance < bounds.getMinZ()) {
					moleculesToBeDeleted.add(fxmol);
				}
				else {
					StereoMolecule refMol = refMolFX.getMolecule();
					Point3D[] refPoint = new Point3D[refMol.getAllAtoms()];
					for (int atom=0; atom<refMol.getAllAtoms(); atom++) {
						Coordinates c = refMol.getCoordinates(atom);
						refPoint[atom] = fxmol.localToScene(c.x, c.y, c.z);
					}
					V3DMoleculeCropper cropper = new V3DMoleculeCropper(fxmol, distance, refPoint, refBounds);
					removeMeasurements(fxmol);
					cropper.crop();
					for (int type = 0; type<MoleculeSurfaceAlgorithm.SURFACE_TYPE.length; type++)
						fxmol.cutSurface(type, cropper);
				}
			}
		}
		for (V3DMolecule fxmol:moleculesToBeDeleted)
			delete(fxmol);

		optimizeView();
	}

	public void addMolecule(V3DMolecule fxmol) {
		mWorld.getChildren().add(fxmol);
		if (mSceneListener != null)
			mSceneListener.addMolecule(fxmol);
		}
	
/*	public double getDistanceToScreenFactor(double z) {
		PerspectiveCamera camera = (PerspectiveCamera)getCamera();
		double fieldOfView = camera.getFieldOfView();
		double screenSize = camera.isVerticalFieldOfView() ? getHeight() : getWidth();
		double sizeAtZ0 = -camera.getTranslateZ() * Math.tan(Math.PI*fieldOfView/90);
		return 20;	// TODO calculate something reasonable
		}*/

	public RotatableGroup getWorld() {
		return mWorld;
		}
	
	public void setMouseDragged(boolean mouseDragged) {
		mMouseDragged = mouseDragged;
	}
	
	public boolean isMouseDragged() {
		return mMouseDragged;
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
				for (Node node:mWorld.getChildren())
					if (node instanceof V3DMolecule)
						((V3DMolecule) node).cutSurface(polygon, mSurfaceCutMode, paneOnScreen);
				}

			mSurfaceCutMolecule = null;
			mSurfaceCutMode = 0;
			return;
			}

		for (Node node:mWorld.getChildren())
			if (node instanceof V3DMolecule)
				((V3DMolecule) node).select(polygon, mode, paneOnScreen);
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
			for (Node node : mWorld.getChildren())
				if (node instanceof V3DMolecule)
					((V3DMolecule)node).select(node == mol3D);
			}
		else {
			if (mol3D != null)
				mol3D.select(mode == 1);
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
		setCamera(camera);
		mRoot.getChildren().add(camera);
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
				Point3D globalCoords = fxmol.localToParent(c.x,c.y,c.z);
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
		NonRotatingLabel label = NonRotatingLabel.create(mWorld, text, p1, p2, color);
		label.setLabelDeletionListener(this);
		V3DMeasurement measurement = new V3DMeasurement(atoms,fxmols,line,label,mWorld);
		mMeasurements.add(measurement);

		}
	
	public ArrayList<V3DMeasurement> getMeasurements() {
		return mMeasurements;
	}
	
	public void removeMeasurements() {
		for(V3DMeasurement measurement: mMeasurements) {
			measurement.cleanup();
		}
		mMeasurements.clear();
	}

	@Override
	public void labelDeleted(Label l) {
		ArrayList<V3DMeasurement> toBeRemoved = new ArrayList<V3DMeasurement>();
		for(V3DMeasurement measurement: mMeasurements) {
			if(measurement.getLabel().equals(l)) {
				measurement.cleanup();
				toBeRemoved.add(measurement);
				for (Node node : mWorld.getChildren())
					if (node instanceof V3DMolecule) {
						V3DMolecule fxmol = (V3DMolecule) node;
						fxmol.removeMoleculeChangeListener(measurement);
					}
			}
		}

		mMeasurements.removeAll(toBeRemoved);
		// TODO Auto-generated method stub
	}
}

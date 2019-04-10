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
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import org.openmolecules.chem.conf.gen.ConformerGenerator;
import org.openmolecules.fx.viewer3d.editor.actions.V3DEditorAction;
import org.openmolecules.mesh.MoleculeSurfaceAlgorithm;

import java.util.ArrayList;

public class V3DScene extends SubScene {
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

	public static final Color SELECTION_COLOR = Color.TURQUOISE;
	protected static final double CAMERA_INITIAL_DISTANCE = 45;
	protected static final double CAMERA_FIELD_OF_VIEW = 30.0;	// default field of view
	protected static final double CAMERA_NEAR_CLIP = 10.0;
	protected static final double CAMERA_FAR_CLIP = 250.0;
	protected static final double CAMERA_MIN_CLIP_THICKNESS = 2;


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

		mMouseHandler = new V3DMouseHandler(this);
		mKeyHandler = new V3DKeyHandler(this);
		mClipboardHandler = new ClipboardHandler();
		}

	public void setSceneListener(V3DSceneListener sl) {
		mSceneListener = sl;
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
		fxmol.activateEvents();
		addMolecule(fxmol);
		}

	public void delete(V3DMolecule fxmol) {
		fxmol.removeMeasurements();
		fxmol.deactivateEvents();
		mWorld.getChildren().remove(fxmol);
		if (mSceneListener != null)
			mSceneListener.removeMolecule(fxmol);
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
				((V3DMolecule) node).removeMeasurements();
				if (mSceneListener != null)
					mSceneListener.removeMolecule((V3DMolecule) node);
			}
		mSceneListener.initialize(isSmallMoleculeMode);
		}
		mWorld.getChildren().clear();	// this does not remove the measurements
	}

	/**
	 * Moves all nodes such that the center of gravity of all atoms is world center (0,0,0).
	 * Moves the camera such that x=0, y=0 and z<0, such that all nodes are just within the field of view.
	 */
	public void optimizeView() {
		Point3D cog = getCenterOfGravity();
		for (Node n:mWorld.getChildren()) {
			Point3D p = mWorld.sceneToLocal(cog);
			n.setTranslateX(n.getTranslateX() - p.getX());
			n.setTranslateY(n.getTranslateY() - p.getY());
			n.setTranslateZ(n.getTranslateZ() - p.getZ());
		}

		getCamera().setTranslateX(0);
		getCamera().setTranslateY(0);
		getCamera().setTranslateZ(-50);
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

	}

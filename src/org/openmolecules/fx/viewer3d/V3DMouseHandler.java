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

import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;

import java.util.stream.IntStream;

import org.openmolecules.fx.viewer3d.nodes.ExclusionSphere;
import org.openmolecules.fx.viewer3d.nodes.IPPNode;
import org.openmolecules.fx.viewer3d.nodes.NonRotatingLabel;
import org.openmolecules.fx.viewer3d.nodes.PPArrow;
import org.openmolecules.mesh.MoleculeSurfaceAlgorithm;


import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Camera;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.scene.transform.Rotate;

public class V3DMouseHandler {
	private static final long POPUP_DELAY = 750;	// milli seconds delay right mouse click may be used for rotation
	private static final int SINGLE_MOL_KEY = 1;	// 0: Shift; 1: Ctrl; 2: none (single mol if highlighted)

	private static final long WHEEL_DELAY_LIMIT = 250;	// milli seconds; above this delay we have the smallest clip change
	private static final double WHEEL_MIN_FACTOR = 0.05;// smallest clip change = this factor * smallest mouse wheel delta
	private static final double WHEEL_MAX_FACTOR = 50;	// largest clip change * this factor * smallest clip change (when mouse wheel is rotated quickly)

	private volatile boolean mShowPopup;
	private V3DScene mScene;
	private double mMouseX,mMouseY;
	private long mRecentWheelMillis;
	private V3DMolecule mHighlightedMol,mAffectedMol;
	private long mMousePressedMillis;
	private Node mSelectedNode;
	private ExclusionSphere mHighlightedExclusionSphere, mAffectedExclusionSphere;


	public V3DMouseHandler(final V3DScene scene) {
		mScene = scene;

		scene.setOnScroll(se -> {
			// we modify the wheel delta depending on how quickly the wheel is rotated
			double delta = WHEEL_MIN_FACTOR * se.getDeltaY();	// delta is the smallest possible clip step
			long millis = System.currentTimeMillis();
			long delay = Math.max(1L, millis - mRecentWheelMillis);
			if (delay < WHEEL_DELAY_LIMIT)
				delta *= Math.pow(WHEEL_MAX_FACTOR, (1.0 - (double)delay / WHEEL_DELAY_LIMIT));
			mRecentWheelMillis = millis;

			if (V3DPopupMenu.sUseMouseWheelForClipping || se.isShiftDown()) {
				if (se.isControlDown()) {
					delta *= getScreenToObjectFactor (mScene.getCamera().farClipProperty().getValue());
					moveFarClip(delta, false);
				}
				else {
					delta *= getScreenToObjectFactor (mScene.getCamera().nearClipProperty().getValue());
					if (delta < 0)
						moveFarClip(moveNearClip(delta), true);
					else
						moveNearClip(moveFarClip(delta, true));
				}
			}
			else {
				if (mAffectedMol == null)
					translateCameraZ(-delta);    // this does not change the world rotation center
				else
					translateMolecule(mAffectedMol, 0, 0, delta);
			}
		} );
		scene.setOnMousePressed(me -> {
			mSelectedNode = me.getPickResult().getIntersectedNode();
			mMouseX = me.getScreenX();
			mMouseY = me.getScreenY();

			mAffectedMol = null;
			trackAffectedMol(isSingleMolecule(me));


			// setOnMouseClicked() is strangely often not triggered. Therefore we simulate...
			long millis = System.currentTimeMillis();
			boolean isDoubleClick = (millis - mMousePressedMillis < 300);
			mMousePressedMillis = millis;

			//System.out.println("mouse pressed isPrimaryButtonDown:"+me.isPrimaryButtonDown()+" isMiddleButtonDown:"+me.isMiddleButtonDown());
			if (me.getButton() == MouseButton.PRIMARY) {
				if (isDoubleClick) {
					mScene.selectMolecule(mHighlightedMol, me.isShiftDown() ? 1 : me.isControlDown() ? 2 : 0);

				}
				else {
					if(mScene.getMeasurementMode()!=V3DScene.MEASUREMENT.NONE) {
						Node parent = mSelectedNode;
						while (parent != null && !(parent instanceof V3DMolecule)) {
							parent = parent.getParent();
						}
						if(parent!=null) {
							V3DMolecule fxmol = (V3DMolecule) parent;
							boolean molPicked = fxmol.pickShape(me);
							if(molPicked) {
								mScene.getPickedMolsList().add(fxmol);
								mScene.tryAddMeasurement();
							}
						}
					}
				}
			}

			if (me.getButton() == MouseButton.SECONDARY) {
				mShowPopup = true;
				new Thread(() -> {
					try {
						Thread.sleep(POPUP_DELAY);
					} catch (InterruptedException ie) {}
					if (mShowPopup) {
						Platform.runLater(() -> {
							if (mShowPopup) {
								mShowPopup = false;
								createPopupMenu(mSelectedNode, me.getScreenX(), me.getScreenY());
							}
						});
					}
				}).start();
			}
		} );
		scene.setOnMouseReleased(me -> {
			if(mScene.isMouseDragged()==false) {
			if ((me.getButton() == MouseButton.PRIMARY)) {
				Node parent = mSelectedNode;
				while (parent != null && !(parent instanceof V3DMolecule)) {
					parent = parent.getParent();
				}
				if(mScene.getEditor().getAction()!=null && mScene.getMeasurementMode()==V3DScene.MEASUREMENT.NONE) {
					if (parent == null) {
						V3DMolecule fxmol = mScene.getEditor().sceneClicked(mScene);
						if(fxmol!=null) {
							RotatableGroup world = mScene.getWorld();
							double f = getScreenToObjectFactor(0.0);
							Point2D origin = world.localToScreen(0,0,0);
							double dx = me.getScreenX()-origin.getX();
							double dy = me.getScreenY()-origin.getY();
							Point3D p1 = world.parentToLocal(f*dx,f*dy , 0.0);
							
							IntStream.range(0,fxmol.getMolecule().getAllAtoms())
							.forEach(i -> {
								double x = fxmol.getMolecule().getAtomX(i) + p1.getX();
								double y = fxmol.getMolecule().getAtomY(i) + p1.getY();
								double z = fxmol.getMolecule().getAtomZ(i) + p1.getZ();
								fxmol.getMolecule().setAtomX(i,x);
								fxmol.getMolecule().setAtomY(i,y);
								fxmol.getMolecule().setAtomZ(i,z);
								fxmol.setInitialCoordinates();
							});
							Platform.runLater(() -> {
								fxmol.fireCoordinatesChange();
								V3DMoleculeUpdater mFXMolUpdater = new V3DMoleculeUpdater(fxmol);
								mFXMolUpdater.update();
							});
							
							//fxmol.setTranslateX(fxmol.getTranslateX() +  p1.getX());
							//fxmol.setTranslateY(fxmol.getTranslateY() +  p1.getY());
							//fxmol.setTranslateZ(fxmol.getTranslateZ() +  p1.getZ());
						}
					}
					else {
						V3DMolecule fxmol = (V3DMolecule)parent;
						mScene.getEditor().moleculeClicked(fxmol, mSelectedNode);
						for (int type = 0; type<MoleculeSurfaceAlgorithm.SURFACE_TYPE.length; type++)
							fxmol.setSurfaceMode(type ,V3DMolecule.SURFACE_NONE);
						mScene.removeMeasurements(fxmol);
						fxmol.fireStructureChange();
						fxmol.updateColor(true);
					}

					}
				}
			}
			if (mShowPopup) {
				mShowPopup = false;
				//Node node = me.getPickResult().getIntersectedNode();
				//createPopupMenu(node, me.getScreenX(), me.getScreenY());
				createPopupMenu(mSelectedNode, me.getScreenX(), me.getScreenY());
			}
			mScene.setMouseDragged(false);
		} );
		scene.setOnMouseMoved(me -> {
			trackHighlightedMol(me.getPickResult());
			trackAffectedMol(isSingleMolecule(me));
			
		} );
		scene.setOnMouseDragged(me -> {
			double oldMouseX = mMouseX;
			double oldMouseY = mMouseY;
			mMouseX = me.getScreenX();
			mMouseY = me.getScreenY();
			double dx = (mMouseX - oldMouseX);
			double dy = (mMouseY - oldMouseY);

			if (me.isMiddleButtonDown() || (me.isPrimaryButtonDown() && me.isMetaDown())) {
				if (mAffectedMol == null && mAffectedExclusionSphere == null)
					translateCameraXY(-dx, -dy);
				else if (mAffectedExclusionSphere != null){
					translateExclusionSphere(mAffectedExclusionSphere, dx, dy, 0);
				}
				else if (mAffectedMol != null){
					translateMolecule(mAffectedMol, dx, dy, 0);
					mAffectedMol.fireCoordinatesChange();
				}

			}
			else if (me.isSecondaryButtonDown()) {
				mShowPopup = false;
				rotate(dx, dy, me.isShiftDown());
			}
		} );
	}

	private void trackAffectedMol(boolean moleculeKeyIsDown) {
		if (!moleculeKeyIsDown) {
			mAffectedMol = null;
			mAffectedExclusionSphere = null;
		}
		
		else if (mHighlightedExclusionSphere != null) {
			mAffectedExclusionSphere = mHighlightedExclusionSphere;
		}
		else if (mHighlightedMol != null) {
			mAffectedMol = mHighlightedMol;
		}
		

		
	}

	private boolean isSingleMolecule(MouseEvent me) {
		if (SINGLE_MOL_KEY == 0 && me.isShiftDown())
			return true;
		if (SINGLE_MOL_KEY == 1 && me.isControlDown())
			return true;
		if (SINGLE_MOL_KEY == 2 && mHighlightedMol != null)
			return true;
		return false;
	}

	private boolean isSingleMolecule(ScrollEvent me) {
		if (SINGLE_MOL_KEY == 0 && me.isShiftDown())
			return true;
		if (SINGLE_MOL_KEY == 1 && me.isControlDown())
			return true;
		if (SINGLE_MOL_KEY == 2 && mHighlightedMol != null)
			return true;
		return false;
	}

	private void trackHighlightedMol(PickResult pr) {
		Node node = pr.getIntersectedNode();
		if(node instanceof ExclusionSphere) 
			mHighlightedExclusionSphere = (ExclusionSphere) node;
		else 
			mHighlightedExclusionSphere = null;
		
		//	V3DMolecule parentMol = (V3DMolecule) mHighlightedExclusionSphere.getParent();
		//	parentMol.setHighlightedShape(mHighlightedExclusionSphere);
		//	if(mHighlightedMol!=null) {
		//		mHighlightedMol.removeHilite();
		//		mHighlightedMol = null;
		//	}
		//	return;
		//}
		Node molecule = node;
		while (molecule != null && !(molecule instanceof V3DMolecule))
			molecule = molecule.getParent();

		if (mHighlightedMol != null && (molecule == null || molecule != mHighlightedMol))
			mHighlightedMol.removeHilite();

		mHighlightedMol = (V3DMolecule) molecule;
		if (mHighlightedMol != null && node instanceof Shape3D) {
			mHighlightedMol.setHighlightedShape((Shape3D)node); }
	}

	/**
	 * Moves the near clip layer by delta units preventing going beyond near clip limit
	 * and far clip layer minus the minimum distance betreen those layers.
	 * @param delta
	 * @return the amount of really moved clip
	 */
	private double moveNearClip(double delta) {
		double nearClip = mScene.getCamera().nearClipProperty().getValue();
		double farClip = mScene.getCamera().farClipProperty().getValue();
		if (delta < 0) {
			if (nearClip > V3DScene.CAMERA_NEAR_CLIP) {
				if (delta < V3DScene.CAMERA_NEAR_CLIP - nearClip)
					delta = V3DScene.CAMERA_NEAR_CLIP - nearClip;
				mScene.getCamera().nearClipProperty().setValue(nearClip + delta);
				return delta;
			}
		}
		else {
			if (nearClip < farClip - V3DScene.CAMERA_MIN_CLIP_THICKNESS) {
				if (delta > farClip - V3DScene.CAMERA_MIN_CLIP_THICKNESS - nearClip)
					delta = farClip - V3DScene.CAMERA_MIN_CLIP_THICKNESS - nearClip;
				mScene.getCamera().nearClipProperty().setValue(nearClip + delta);
				return delta;
			}
		}
		return 0;
	}

	private double moveFarClip(double delta, boolean neglectRearLimit) {
		double nearClip = mScene.getCamera().nearClipProperty().getValue();
		double farClip = mScene.getCamera().farClipProperty().getValue();
		if (delta < 0) {
			if (farClip > nearClip + V3DScene.CAMERA_MIN_CLIP_THICKNESS) {
				if (delta < nearClip + V3DScene.CAMERA_MIN_CLIP_THICKNESS - farClip)
					delta = nearClip + V3DScene.CAMERA_MIN_CLIP_THICKNESS - farClip;
				mScene.getCamera().farClipProperty().setValue(farClip + delta);
				return delta;
			}
		}
		else {
			if (farClip < V3DScene.CAMERA_FAR_CLIP || neglectRearLimit) {
				if (delta > V3DScene.CAMERA_FAR_CLIP - farClip && !neglectRearLimit)
					delta = V3DScene.CAMERA_FAR_CLIP - farClip;
				mScene.getCamera().farClipProperty().setValue(farClip + delta);
				return delta;
			}
		}
		return 0;
	}

	private void translateMolecule(V3DMolecule fxmol, double dx, double dy, double dz) {
		RotatableGroup world = mScene.getWorld();

		double f = getScreenToObjectFactor(fxmol.getHighlightedZ());

		// p0 and p1 are world coordinates
		Point3D p0 = fxmol.localToParent(fxmol.getHighlightedPointLocal());
		Point3D p1 = world.sceneToLocal(fxmol.getHighlightedPointInScene().subtract(f*dx, f*dy, f*dz));
		fxmol.setTranslateX(fxmol.getTranslateX() + p0.getX() - p1.getX());
		fxmol.setTranslateY(fxmol.getTranslateY() + p0.getY() - p1.getY());
		fxmol.setTranslateZ(fxmol.getTranslateZ() + p0.getZ() - p1.getZ());
	}
	
	private void translateExclusionSphere(ExclusionSphere eSphere, double dx, double dy, double dz) {
		RotatableGroup world = mScene.getWorld();
		V3DMolecule fxmol = (V3DMolecule) eSphere.getParent().getParent();
		double f = getScreenToObjectFactor(eSphere.localToScene(0, 0, 0).getZ());
		Point3D p0 = fxmol.localToParent(eSphere.localToParent(0, 0, 0));
		Point3D p1 = world.sceneToLocal(eSphere.localToScene(0, 0, 0).subtract(f*dx, f*dy, f*dz));
		eSphere.setTranslateX(eSphere.getTranslateX() + p0.getX() - p1.getX());
		eSphere.setTranslateY(eSphere.getTranslateY() + p0.getY() - p1.getY());
		eSphere.setTranslateZ(eSphere.getTranslateZ() + p0.getZ() - p1.getZ());
		
	}

	private void translateCameraXY(double dx, double dy) {
		double f = getScreenToObjectFactor(mScene.getWorld().getTranslateZ());
		Camera camera = mScene.getCamera();
		camera.setTranslateX(camera.getTranslateX() + 2*f*dx);
		camera.setTranslateY(camera.getTranslateY() + 2*f*dy);
	}

	private void translateCameraZ(double dz) {
		double f = getScreenToObjectFactor(mScene.getWorld().getTranslateZ());
		Camera camera = mScene.getCamera();
		camera.setTranslateZ(camera.getTranslateZ() + 2*f*dz);
	}

	private double getScreenToObjectFactor(double objectZ) {
		PerspectiveCamera camera = (PerspectiveCamera)mScene.getCamera();
		double f = -camera.getTranslateZ()
				* 2.0 * Math.tan(camera.getFieldOfView()*Math.PI/360)
				/ mScene.getWidth();

		double cameraZ = camera.getTranslateZ();
		// The following 2 compensates for the fact that
		// only about half of the field of view is visible.
		f *= 2 * (cameraZ - objectZ) / cameraZ;

		// it seems that the width/height ratio is about proportional to f
		f *= 0.5*mScene.getWidth()/mScene.getHeight();

		return f;
	}

	private void rotate(double dx, double dy, boolean aroundZAxis) {
		if (dx != 0 || dy != 0) {
			double f = 720 / (mScene.getWidth() + mScene.getHeight());	// about two rotations when dragging through full window
			double d = f * Math.sqrt(dx*dx+dy*dy);

			Point3D p1 = null;
			if (aroundZAxis) {
				Point2D origin = (mAffectedMol != null) ? mAffectedMol.localToScreen(0,0,0)
														: mScene.getWorld().localToScreen(0,0,0);
				double x2 = mMouseX - origin.getX();
				double x1 = x2 - dx;
				double y2 = mMouseY - origin.getY();
				double y1 = y2 - dy;
				d = 180 / Math.PI * angleDif(angle(x1, y1), angle(x2, y2));
				p1 = new Point3D(0, 0, 1);
			}
			else {
				p1 = new Point3D(dy, -dx, 0);
			}

			if (mAffectedMol != null || mScene.isIndividualRotationModus()) {
				RotatableGroup world = mScene.getWorld();
				Point3D p0 = world.sceneToLocal(new Point3D(0, 0, 0));
				Point3D p2 = world.sceneToLocal(p1).subtract(p0);
				Rotate r = new Rotate(d, p2);
				if (mAffectedMol != null)
					mAffectedMol.rotate(r);
				else
					for (Node node : mScene.getWorld().getChildren())
						if (node instanceof V3DMolecule)
							((V3DMolecule) node).rotate(r);
			}
			else {
				// world center of gravity:
				mScene.getWorld().rotate(new Rotate(d, p1));
			}
		}
	}

	private double angle(double dx, double dy) {
		if (dy == 0.0)
			return (dx > 0.0) ? Math.PI/2.0 : -Math.PI/2.0;

		double angle = Math.atan(dx/dy);
		if (dy < 0.0)
			return (dx < 0.0) ? angle - Math.PI : angle + Math.PI;

		return angle;
		}

	public static double angleDif(double angle1, double angle2) {
		double angleDif = angle1 - angle2;
		while (angleDif < -Math.PI)
			angleDif += 2 * Math.PI;
		while (angleDif > Math.PI)
			angleDif -= 2 * Math.PI;
		return angleDif;
		}
	

	private void createPopupMenu(Node node, double x, double y) {
		// if we have an active node, create a node specific popup
		if(node.getParent() instanceof NonRotatingLabel) {
			((NonRotatingLabel) node.getParent()).showMenu(x,y);
		}
		else if(node instanceof ExclusionSphere) {
			((ExclusionSphere)node).showMenu(x,y);
		}
		else if(node instanceof IPPNode) {
			((IPPNode)node).showMenu(x,y);
		}
		
		else if(node.getParent() instanceof IPPNode) {
			((IPPNode)node.getParent()).showMenu(x,y);
		}
		else {
		Node parent = node;
		while (parent != null && !(parent instanceof V3DMolecule)) {
			parent = parent.getParent();
		}
		if (parent == null)
			new V3DPopupMenu(mScene, null).show(mScene.getWorld(), x, y);
		else
			new V3DPopupMenu(mScene, (V3DMolecule)parent).show(node, x, y);
		}
	}
}

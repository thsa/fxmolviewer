package org.openmolecules.fx.viewer3d;

import javafx.animation.AnimationTimer;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SnapshotParameters;
import javafx.scene.SubScene;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;

public final class OneEyeView extends ImageView {
	private final SnapshotParameters mParams = new SnapshotParameters();
	private WritableImage mImage = null;
	private final Group mWorldRoot;
	private final AnimationTimer mTimer;


	public OneEyeView(SubScene scene, PerspectiveCamera camera) {
		mWorldRoot = (Group)scene.getRoot();

		mParams.setCamera(camera);
		mParams.setDepthBuffer(true);
		mParams.setFill(scene.getFill());

		mTimer = new AnimationTimer() {
			@Override
			public void handle(long now) {
				redraw();
			}
		};
	}

	public void startViewing() {
		mTimer.start();
	}

	public void pause() {
		mTimer.stop();
	}

	private void redraw() {
		mParams.setViewport(new Rectangle2D(0, 0, getFitWidth(), getFitHeight()));
		if (mImage == null
				|| mImage.getWidth() != getFitWidth() || mImage.getHeight() != getFitHeight()) {
			mImage = mWorldRoot.snapshot(mParams, null);
		} else {
			mWorldRoot.snapshot(mParams, mImage);
		}
		setImage(mImage);
	}
}
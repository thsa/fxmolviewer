package org.openmolecules.fx.viewer3d;

import javafx.animation.AnimationTimer;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.stage.Screen;

public final class OneEyeView extends ImageView {
	private final SnapshotParameters mParams = new SnapshotParameters();
	private WritableImage mImage = null;
	private final Group mWorldRoot;
	private final AnimationTimer mTimer;
	private final Screen mTagetScreen;


	public OneEyeView(V3DScene sourceScene, PerspectiveCamera camera, Screen targetScreen) {
		mWorldRoot = (Group)sourceScene.getRoot();
		mTagetScreen = targetScreen;

		mParams.setCamera(camera);
		mParams.setDepthBuffer(true);
		mParams.setFill(sourceScene.getFill());

		mTimer = new AnimationTimer() {
			@Override
			public void handle(long now) {
				redraw();
			}
		};
	}

	public void start() {
		mTimer.start();
	}

	public void stop() {
		mTimer.stop();
	}

	private void redraw() {
		int width = Math.round((float)mTagetScreen.getOutputScaleX() * (float)getFitWidth());
		int height = Math.round((float)mTagetScreen.getOutputScaleY() * (float)getFitHeight());

		mParams.setViewport(new Rectangle2D(0, 0, width, height));
		if (mImage == null
		 || mImage.getWidth() != width
		 || mImage.getHeight() != height)
			mImage = mWorldRoot.snapshot(mParams, null);
		else
			mWorldRoot.snapshot(mParams, mImage);

		setImage(mImage);
	}
}
package org.openmolecules.fx.viewer3d;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.awt.*;
import java.util.EnumSet;

public class V3DStereoPane extends GridPane {
	public static final int MODE_NONE = 0;
	public static final int MODE_SBS = 1;
	public static final int MODE_HSBS = 2;
	public static final int MODE_OU = 3;
	public static final int MODE_HOU = 4;

	private V3DScene mScene3D;
	private OneEyeView mLeftEyeView,mRightEyeView;
	private static Stage sFullScreenView;

	public static Stage getFullScreenView() {
		return sFullScreenView;
	}

	public static void closeFullSCreenView() {
		if (sFullScreenView != null) {
			sFullScreenView.close();
			sFullScreenView = null;
		}
	}

	/**
	 * This method creates a new window (Stage) containing a V3DStereoPane displaying the given
	 * V3DScene in the given stereo mode. It is meant as a second view for an already displayed
	 * V3DScene to be shown in full screen stereo on a stereo enabled screen device, e.g. a 3D-TV
	 * (LG C6 are the best) or XR-glasses like the Viture One or XReal Air.
	 * with different camera perspectives for the left and for the right eye.
	 * @param scene3D
	 * @param mode
	 * @return
	 */
	public static void createFullScreenView(V3DScene scene3D, int mode) {
		if (sFullScreenView != null) {
			sFullScreenView.close();
			sFullScreenView = null;
		}

		GraphicsDevice[] graphicsDevices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
		for (GraphicsDevice gd : graphicsDevices) {
			Rectangle bounds = gd.getDefaultConfiguration().getBounds();
			if (bounds.width > 2 * bounds.height) {
				createFullScreenView(scene3D, gd, mode, true);
				return;
			}
		}

		if (graphicsDevices.length != 0)
			createFullScreenView(scene3D, graphicsDevices[0], mode, false);
	}

	public static void createFullScreenView(V3DScene sourceScene3D, GraphicsDevice gd, int stereoMode, boolean fullScreen) {
		Rectangle bounds = gd.getDefaultConfiguration().getBounds();
		int initialWidth = (stereoMode == MODE_SBS) ? bounds.width : bounds.width/2;
		int initialHeight = (stereoMode == MODE_OU) ? bounds.height : bounds.height/2;

		V3DStereoPane view = new V3DStereoPane(sourceScene3D, stereoMode);

		Scene targetScene3D = new Scene(view, initialWidth, initialHeight, true, SceneAntialiasing.BALANCED);
//		String css = V3DStereoPane.class.getResource("/resources/molviewer.css").toExternalForm();
//		scene.getStylesheets().add(css);
		targetScene3D.widthProperty().addListener((observableValue, number, t1) -> {
				double width = targetScene3D.getWidth() / (stereoMode==MODE_HSBS || stereoMode==MODE_SBS ? 2 : 1);
				view.mLeftEyeView.setFitWidth(width);
				view.mRightEyeView.setFitWidth(width);
				} );
		targetScene3D.heightProperty().addListener((observableValue, number, t1) -> {
				double height = targetScene3D.getHeight() / (stereoMode==MODE_HOU || stereoMode==MODE_OU ? 2 : 1);
				view.mLeftEyeView.setFitHeight(height);
				view.mRightEyeView.setFitHeight(height);
				} );

		sFullScreenView = new Stage();
		sFullScreenView.setTitle("Stereo View");
		sFullScreenView.setScene(targetScene3D);
		sFullScreenView.setX(bounds.x + bounds.width / 4f);
		sFullScreenView.setY(bounds.y + bounds.height / 4f);
		sFullScreenView.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, e -> sFullScreenView = null );
		if (fullScreen)
			sFullScreenView.setFullScreen(true);
		sFullScreenView.show();
	}

	/**
	 * This constructor creates a V3DStereoPane by creating two ImageViews from the given V3DScene
	 * with different camera perspectives for the left and for the right eye.
	 * @param sourceScene3D scene for which to create stereo view
	 * @param stereoMode one of MODE_xxx
	 */
	private V3DStereoPane(V3DScene sourceScene3D, int stereoMode) {
		if (stereoMode == MODE_SBS || stereoMode == MODE_HSBS) {
			ColumnConstraints column1 = new ColumnConstraints();
			column1.setPercentWidth(50);
			ColumnConstraints column2 = new ColumnConstraints();
			column2.setPercentWidth(50);
			getColumnConstraints().addAll(column1, column2);
		}
		else {
			RowConstraints row1 = new RowConstraints();
			row1.setPercentHeight(50);
			RowConstraints row2 = new RowConstraints();
			row2.setPercentHeight(50);
			getRowConstraints().addAll(row1, row2);
		}

		mLeftEyeView = sourceScene3D.buildOneEyeView(false, stereoMode);
		add(mLeftEyeView, 0, 0);
		mRightEyeView = sourceScene3D.buildOneEyeView(true, stereoMode);
		if (stereoMode == MODE_HSBS || stereoMode == MODE_SBS)
			add(mRightEyeView, 1, 0);
		else
			add(mRightEyeView, 0, 1);

		mLeftEyeView.startViewing();
		mRightEyeView.startViewing();
	}

	/**
	 * This constructor creates a V3DStereoPane by creating V3DScene for the left eye and an
	 * ImageView with a different camera perspective on that V3DScene for the right eye.
	 * @param sceneMode
	 * @param stereoMode
	 * @param width
	 * @param height
	 */
	public V3DStereoPane(EnumSet<V3DScene.ViewerSettings> sceneMode, int stereoMode, int width, int height) {
		if (stereoMode == MODE_HSBS || stereoMode == MODE_SBS) {
			ColumnConstraints column1 = new ColumnConstraints();
			column1.setPercentWidth(50);
			ColumnConstraints column2 = new ColumnConstraints();
			column2.setPercentWidth(50);
			getColumnConstraints().addAll(column1, column2);
		}
		else {
			RowConstraints row1 = new RowConstraints();
			row1.setPercentHeight(50);
			RowConstraints row2 = new RowConstraints();
			row2.setPercentHeight(50);
			getRowConstraints().addAll(row1, row2);
		}

		mScene3D = new V3DScene(new Group(), stereoMode == MODE_HSBS ? width / 2f : width, stereoMode == MODE_HOU ? height / 2f : height, sceneMode);
		add(mScene3D, 0, 0);
		OneEyeView cameraView = mScene3D.buildOneEyeView(true, stereoMode);
		cameraView.fitWidthProperty().bind(mScene3D.widthProperty());
		cameraView.fitHeightProperty().bind(mScene3D.heightProperty());

		if (stereoMode == MODE_HSBS || stereoMode == MODE_SBS)
			add(cameraView, 1, 0);
		else
			add(cameraView, 0, 1);

		cameraView.startViewing();
	}

	/**
	 * In case this StereoView's left eye view is a V3DScene created by this view, then
	 * @return null self instantiated V3DScene used as left eye view
	 */
	public V3DScene getScene3D() {
		return mScene3D;
	}
}

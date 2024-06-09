package org.openmolecules.fx.viewer3d;

import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class V3DStereoPane extends GridPane {
	public static final int MODE_NONE = 0;
	public static final int MODE_SBS = 1;
	public static final int MODE_HSBS = 2;
	public static final int MODE_OU = 3;
	public static final int MODE_HOU = 4;
	private static final double EYE_DISTANCE = 0.5;

	private final OneEyeView mLeftEyeView,mRightEyeView;
	private final int mStereoMode;
	private static Stage sFullScreenView;

	public static Stage getFullScreenView() {
		return sFullScreenView;
	}

	public static void closeFullScreenView() {
		if (sFullScreenView != null) {
			((V3DStereoPane)sFullScreenView.getScene().getRoot()).stopTimers();
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
	 * Typically, gd is another device than the one where the original V3DScene is displayed.
	 * @param sourceScene3D
	 * @param screen
	 * @param stereoMode
	 * @return
	 */
	public static void createFullScreenView(V3DScene sourceScene3D, Screen screen, int stereoMode) {
		closeFullScreenView();

		Rectangle2D bounds = screen.getBounds();
		float wf = 0.6f;
		float hf = 0.6f;
		if (stereoMode == MODE_SBS) {
			wf = 0.8f;
			hf = 0.4f;
		}
		if (stereoMode == MODE_OU) {
			wf = 0.4f;
			hf = 0.8f;
		}
		int width =  Math.round(wf * (float)bounds.getWidth());
		int height = Math.round(hf * (float)bounds.getHeight());

		// We only do full-screen if on a different screen device
		Bounds sceneBounds = sourceScene3D.localToScreen(sourceScene3D.getBoundsInLocal());
		boolean fullScreen = !bounds.contains(sceneBounds.getMinX(), sceneBounds.getMinY());

		V3DStereoPane view = new V3DStereoPane(sourceScene3D, stereoMode);
		view.updateWidth(width);
		view.updateHeight(height);

		Scene scene = new Scene(view, width, height, true, SceneAntialiasing.BALANCED);
//		String css = V3DStereoPane.class.getResource("/resources/molviewer.css").toExternalForm();
//		scene.getStylesheets().add(css);
		scene.widthProperty().addListener((observableValue, number, t1) -> view.updateWidth(scene.getWidth()) );
		scene.heightProperty().addListener((observableValue, number, t1) -> view.updateHeight(scene.getHeight()) );

		setMouseEventHandling(sourceScene3D, scene);

		sFullScreenView = new Stage();
		sFullScreenView.setTitle("Stereo View");
		sFullScreenView.setScene(scene);
		sFullScreenView.setX(bounds.getMinX() + (bounds.getWidth() - width) / 2.0);
		sFullScreenView.setY(bounds.getMinY() + (bounds.getHeight() - height) / 2.0);
		sFullScreenView.setWidth(width);
		sFullScreenView.setHeight(height);
		sFullScreenView.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, e -> sFullScreenView = null );
		if (fullScreen)
			sFullScreenView.setFullScreen(true);
		sFullScreenView.show();
	}

	private void updateWidth(double width) {
		double w = width / (mStereoMode==MODE_HSBS || mStereoMode==MODE_SBS ? 2 : 1);
		mLeftEyeView.setFitWidth(w);
		mRightEyeView.setFitWidth(w);
	}

	private void updateHeight(double height) {
		double h = height / (mStereoMode==MODE_HOU || mStereoMode==MODE_OU ? 2 : 1);
		mLeftEyeView.setFitHeight(h);
		mRightEyeView.setFitHeight(h);
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

		mLeftEyeView = sourceScene3D.buildOneEyeView(-EYE_DISTANCE/2, stereoMode);
		add(mLeftEyeView, 0, 0);
		mRightEyeView = sourceScene3D.buildOneEyeView(EYE_DISTANCE/2, stereoMode);
		if (stereoMode == MODE_HSBS || stereoMode == MODE_SBS)
			add(mRightEyeView, 1, 0);
		else
			add(mRightEyeView, 0, 1);

		mStereoMode = stereoMode;
		mLeftEyeView.start();
		mRightEyeView.start();
	}

	private static void setMouseEventHandling(V3DScene sourceScene, Scene scene) {
		// to make it perfect, we would need to translate event coordinates from ImageViews to source scene!
		scene.setOnScroll(se -> sourceScene.fireEvent(se) );
		scene.setOnMousePressed(me -> sourceScene.fireEvent(me));
		scene.setOnMouseReleased(me -> sourceScene.fireEvent(me));
		scene.setOnMouseMoved(me -> sourceScene.fireEvent(me));
		scene.setOnMouseDragged(me -> sourceScene.fireEvent(me));
	}

	public void stopTimers() {
		mLeftEyeView.stop();
		mRightEyeView.stop();
	}
}

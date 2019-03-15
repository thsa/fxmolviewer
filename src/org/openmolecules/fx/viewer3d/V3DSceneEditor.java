package org.openmolecules.fx.viewer3d;

import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.openmolecules.fx.viewer3d.panel.ToolsPane;

public class V3DSceneEditor extends V3DSceneWithSidePane  {
	
	private ToolsPane mToolsPane;
	private Stage mPrimaryStage;
	private TextArea mOutputLog;
	
	public V3DSceneEditor(final Stage primaryStage) {
		super();
		mPrimaryStage = primaryStage;
		mToolsPane = new ToolsPane(mPrimaryStage,this);
		mOutputLog = new TextArea();
		mOutputLog.setEditable(false);
		setRight(mToolsPane);
		setBottom(mOutputLog);
	}
	
	public void minimizeVisibleMols() {
		createOutput("start minimization");
		V3DMinimizer.minimize(getScene3D(), this, null);
	}
	
	public void createOutput(String output) {
		mOutputLog.appendText(output);
		mOutputLog.appendText("\n");
	}
}

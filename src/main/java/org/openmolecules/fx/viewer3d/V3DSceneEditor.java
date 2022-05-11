package org.openmolecules.fx.viewer3d;

import javafx.scene.control.TextArea;

import org.openmolecules.fx.tasks.V3DMinimizer;
import org.openmolecules.fx.viewer3d.panel.EditorPane;

import java.util.EnumSet;

public class V3DSceneEditor extends V3DSceneWithSidePane  {
	
	private TextArea mOutputLog;

	public V3DSceneEditor(EnumSet<V3DScene.ViewerSettings> settings) {
		super(settings);
		mOutputLog = new TextArea();
		mOutputLog.setEditable(false);
		//mOutputLog.setStyle("-fx-control-inner-background:black");
		//setRight(mEditorPane);
		//setBottom(mOutputLog);
		EditorPane editorPane = new EditorPane(getScene3D());

		setRight(editorPane);

	}
	
	public void minimizeVisibleMols() {
		createOutput("start minimization");
		V3DMinimizer.minimize(getScene3D(), this, null , false);
	}
	
	public void createOutput(String output) {
		mOutputLog.appendText(output);
		mOutputLog.appendText("\n");
	}
}

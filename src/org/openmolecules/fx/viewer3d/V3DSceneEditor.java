package org.openmolecules.fx.viewer3d;


import javafx.scene.control.TextArea;
import org.openmolecules.fx.viewer3d.panel.EditorPane;

public class V3DSceneEditor extends V3DSceneWithSidePane  {
	
	private TextArea mOutputLog;
	
	public V3DSceneEditor() {
		super();
		//mEditorPane= new EditorPane(this);
		mOutputLog = new TextArea();
		mOutputLog.setEditable(false);
		//setRight(mEditorPane);
		setBottom(mOutputLog);
		V3DSceneWithSelection content = (V3DSceneWithSelection) getContent();
		EditorPane editorPane = new EditorPane(getScene3D());
		content.setRight(editorPane);
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

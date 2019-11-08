package org.openmolecules.fx.viewer3d;

import java.util.EnumSet;

public class V3DSceneCreator {
	
	
	private V3DSceneCreator() {};
	
	public static V3DSceneWithSidePane createScene(EnumSet<V3DScene.ViewerSettings> settings) {
		boolean showSidePanel = settings.contains(V3DScene.ViewerSettings.SIDEPANEL) ? true : false;
		V3DSceneWithSidePane scene = settings.contains(V3DScene.ViewerSettings.EDITING) ? new V3DSceneEditor(showSidePanel) : 
			new V3DSceneWithSidePane(showSidePanel);  
		boolean showStructure = settings.contains(V3DScene.ViewerSettings.STRUCTUREVIEW) ? true : false;
		if(scene.getMoleculePanel()!=null)
			scene.getMoleculePanel().setShowStructure(showStructure);
		scene.getScene3D().applySettings(settings);
		return scene;
		
	}

}

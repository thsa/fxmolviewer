package org.openmolecules.fx.viewer3d.editor.actions;

import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DScene;
import org.openmolecules.fx.viewer3d.nodes.NodeDetail;

import com.actelion.research.share.gui.editor.geom.IDrawContext;
import com.actelion.research.share.gui.editor.io.IKeyEvent;
import com.actelion.research.share.gui.editor.io.IMouseEvent;

import javafx.scene.Node;

public interface V3DEditorAction {


    /**
     * Handles the MouseUp event
     * @param ev
     * @return true if the action handles the event
     */
    
    boolean onMouseUp(V3DMolecule v3dMol, NodeDetail detail);
    
    boolean onMouseScrolled(V3DMolecule v3dMol, NodeDetail detail, double delta);
	
    
    V3DMolecule onMouseUp(V3DScene scene3d);
    
    //void onMouseUp(V3DMolecule v3dMol, int index1, int index2);
    
    //(int getMode();
  
}

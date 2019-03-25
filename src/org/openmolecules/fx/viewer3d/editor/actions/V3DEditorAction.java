package org.openmolecules.fx.viewer3d.editor.actions;

import org.openmolecules.fx.viewer3d.V3DMolecule;

import com.actelion.research.share.gui.editor.geom.IDrawContext;
import com.actelion.research.share.gui.editor.io.IKeyEvent;
import com.actelion.research.share.gui.editor.io.IMouseEvent;

public interface V3DEditorAction {
    /**
     * Handles Mouse down events
     * @param ev
     * @return true if the action handles the event
     */
    boolean onMouseDown();

    /**
     * Handles the MouseUp event
     * @param ev
     * @return true if the action handles the event
     */
    
    void onMouseUp(V3DMolecule v3dMol, int index);
    
    void onMouseUp(V3DMolecule v3dMol, int index1, int index2);
    
    int getMode();
  
}

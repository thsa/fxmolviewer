package org.openmolecules.fx.viewer3d;

import javafx.scene.control.ContextMenu;

public interface V3DPopupMenuController {
	public boolean allowEditing();		// whether editing of any molecules is allowed
	public boolean allowHiding();
	public void addExternalMenuItems(ContextMenu popup);
}

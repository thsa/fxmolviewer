package org.openmolecules.fx.viewer3d;

import javafx.scene.control.ContextMenu;

public interface V3DPopupMenuController {
	static final public int TYPE_FILE = 1;
	static final public int TYPE_EDIT = 2;
	static final public int TYPE_VIEW = 3;

	public void addExternalMenuItems(ContextMenu popup, int type);
}

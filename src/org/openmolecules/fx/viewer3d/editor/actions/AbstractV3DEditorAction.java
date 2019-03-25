package org.openmolecules.fx.viewer3d.editor.actions;

public abstract class AbstractV3DEditorAction implements V3DEditorAction{
	
	public static final int ATOM_ACTION = 1;
	public static final int BOND_ACTION = 2;
	public static final int DRAWING_ACTION = 3;
	protected int mMode;
	
	
	public AbstractV3DEditorAction(int mode) {
		mMode = mode;
	}
	
	public int getMode() {
		return mMode;
	}

}

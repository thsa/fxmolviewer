package org.openmolecules.fx.viewer3d.editor.actions;

import org.openmolecules.fx.viewer3d.NodeDetail;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DMoleculeModifier;
import org.openmolecules.fx.viewer3d.editor.actions.V3DEditorAction;

public class V3DAddFragmentAction implements V3DEditorAction {
	
	public static final String[] PHENYL = {"gOpHL@IToVD@@@",
	"#ql{MpmuAuLrSAbXoHXfGUWElEylyqqB}LzI|QBDJQMbT@h~WtFKhQSRyKckCKzkZwaT@EJhH}TYQl`~{nzQjDfi@@O@AD@@"};
	public static final String[] PHENYL_FUSION = {"daD@PL@J@DjYvxH`@@",
	"#q~W]@AfP}XrDfrP@@FrJ~XSEEDk\\QKjl|SoyfkoKC~[_iKi@|TbGnrQP@lbqCjVWjsMOwed]U~DFSlUedNiOxglNrLbhH@T@@T@@"};
	private String[] mIDCodes;
	private boolean mAllowFusion;
	
	public V3DAddFragmentAction(String[] idcodes, boolean allowFusion) {
		mIDCodes = idcodes;
		mAllowFusion = allowFusion;
	}

	@Override
	public boolean onMouseDown() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onMouseUp(V3DMolecule v3dMol, NodeDetail detail) {
		if(detail.isAtom()) {
			V3DMoleculeModifier.addFragment(v3dMol, detail.getAtom(), mIDCodes);
		}
		else if(detail.isBond() && mAllowFusion) {
			V3DMoleculeModifier.fuseRing(v3dMol, detail.getBond(), mIDCodes);
			
			
			
		}
		
	}
	
	

}

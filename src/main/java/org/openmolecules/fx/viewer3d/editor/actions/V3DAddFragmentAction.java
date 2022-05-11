package org.openmolecules.fx.viewer3d.editor.actions;

import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DMoleculeModifier;
import org.openmolecules.fx.viewer3d.V3DScene;
import org.openmolecules.fx.viewer3d.editor.actions.V3DEditorAction;
import org.openmolecules.fx.viewer3d.nodes.NodeDetail;

import com.actelion.research.chem.StereoMolecule;

public class V3DAddFragmentAction implements V3DEditorAction {
	
	
	public static final String[] PHENYL = {"daD@PL@J@DjYvxH`@@",
	"#q~W]@AfP}XrDfrP@@FrJ~XSEEDk\\QKjl|SoyfkoKC~[_iKi@|TbGnrQP@lbqCjVWjsMOwed]U~DFSlUedNiOxglNrLbhH@T@@T@@"};
	public static final String[] CYCLOPROPYL = {"gBPHL@Qxjh@",
			"#qUxLXSaW`DZT@@FZjIhTYjYf]d~PqrYRACrozq_F^sGHzBU{lYTHsXaizCALLJcPeOvh`@V@@x@@"};
	public static final String[] CYCLOBUTYL = {"gKPHL@IThuT@@",
			"#qw[wklihObPaekN|E]r{{`}mA@@CTYb\\M_RV~Yi{LiSWRgP~MsmWb{MENH\\swiWENadFrcM\\Zz\\YH]OM`}z@cMaeBah`@N@@T@D"};
	public static final String[] CYCLOPENTYL = {"gFpHL@ITimUP@",
	"#qYG]rL[cCvpVHp_G@HEhMcQRqIAmM}]mt_}^rVb^AUibWAuIg^f@[BgYRb{PkUu}xMYqgHoZSqiKSsZ@e{uR~sARkORYm|IudggWZ]bbzUkgF}Vtqip@\\@AX@@"};
	public static final String[] CYCLOHEXYL = {"gOpHL@IToWUU@@",
	"#q[VqVHBqGQWCduyQBHSvuyTNM[FSYdwym`SH~O_f\\AfI}h|YMtlbmjtBoOdwAwULsqz`mFBWPKlB`rLgVt`FgRfjO@oXI}[hhwcJrCE_u\\[qCHxpg`y_JjsqgPbN|PDJB][HKSIip@L@AD@D"};
	public static final String[] CYCLOHEPTYL = {"daD@`L@DjWVzjj`@",
	"#qMGGrMZC@w_~kMp~OMHyAbfiKmz|Wg[]Wv|ljwdDvF^Kg]DkEbxynWesYYrTLtc\\[vl~WzXLl{vUXfrVGrLkX|xLN~GQGvCsR_t_zYaZVmTzSYF`OFXmYNqYq}dbiQPDJR{Ag~GlxkxYF[`WDjOMA}oTLYy]SYhQiP@X@@B@D"};
	
	private String[] mIDCodes;
	private boolean mAllowFusion;
	
	public V3DAddFragmentAction(String[] idcodes, boolean allowFusion) {
		mIDCodes = idcodes;
		mAllowFusion = allowFusion;
	}



	@Override
	public boolean onMouseUp(V3DMolecule v3dMol, NodeDetail detail) {
		if(detail.isAtom()) {
			V3DMoleculeModifier.addFragment(v3dMol, detail.getAtom(), mIDCodes);
			v3dMol.setInitialCoordinates();
		
		}
		else if(detail.isBond() && mAllowFusion) {
			V3DMoleculeModifier.fuseRing(v3dMol, detail.getBond(), mIDCodes);	
			v3dMol.setInitialCoordinates();
		}
		return true;
		
		
	}
	
	@Override
	public V3DMolecule onMouseUp(V3DScene scene3D) {
		StereoMolecule mol = new StereoMolecule();
		mol.setName("Molecule");
		V3DMolecule v3dMol = new V3DMolecule(mol, V3DMolecule.getNextID(), V3DMolecule.MoleculeRole.LIGAND,scene3D.mayOverrideHydrogenColor());
//		v3dMol.activateEvents();
		V3DMoleculeModifier.placeFragment(v3dMol, mIDCodes);
		v3dMol.setInitialCoordinates();
		scene3D.addMolecule(v3dMol);
		return v3dMol;
	}
	
	@Override
	public boolean onMouseScrolled(V3DMolecule v3dMol, NodeDetail detail, double delta) {
		// TODO Auto-generated method stub
		return false;
	}
	
	

}

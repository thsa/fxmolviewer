/*
 * FXMolViewer, showing and manipulating molecules and protein structures in 3D.
 * Copyright (C) 2019 Thomas Sander

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * @author Thomas Sander
 */

package org.openmolecules.fx.viewerapp;

import com.actelion.research.chem.*;
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.chem.io.pdb.parser.PDBCoordEntryFile;
import com.actelion.research.chem.io.pdb.parser.PDBFileParser;
import com.actelion.research.chem.io.pdb.parser.StructureAssembler;
import com.actelion.research.util.Platform;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;
import org.openmolecules.chem.conf.gen.ConformerGenerator;
import org.openmolecules.render.MoleculeArchitect;
import org.openmolecules.fx.surface.SurfaceMesh;
import org.openmolecules.fx.viewer3d.V3DRotatableGroup;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DScene;
import org.openmolecules.pdb.MMTFParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

public class StartOptions {
	// home path used when loading pdb file from disk assuming OS is either Linux or MacOSX
	private static final String HOME_PATH = Platform.isLinux() ? "~/" : "~/Documents/";

	public static final String[] MODE_OPTIONS = {
			"Get from PDB",
			"Test small molecules",
			"Test molecules surfaces",
			"Test small fragments",
			"Test metal-organic molecules",
			"Test Conformers",
			"Test simple molecule",
//			"Test protein from MMTF file",
//			"Test surface from voxel data"
	};

	public static final int MODE_PDB_ENTRY = 0;
	public static final int MODE_SMALL_MOLECULES = 1;
	public static final int MODE_SMALL_MOLECULE_SURFACES = 2;
	public static final int MODE_SMALL_FRAGMENTS = 3;
	public static final int MODE_METAL_ORGANICS = 4;
	public static final int MODE_SMALL_COMFORMERS = 5;
	public static final int MODE_SIMPLE = 6;
	public static final int MODE_PROTEIN = 7;
	public static final int MODE_VOXEL_DATA = 8;

	private static final double POSITION_FACTOR = 16;
	private static final double FRAGMENT_POSITION_FACTOR = 16;
	private static final double ORGANO_METALLICS_POSITION_FACTOR = 32;

	private static final double[][] TEST_POSITIONS_13 = {
		{      0,      0,      0 },
		{      0,      1,  0.618 },
		{      0,      1, -0.618 },
		{      1,  0.618,      0 },
		{      1, -0.618,      0 },
		{      0,     -1, -0.618 },
		{      0,     -1,  0.618 },
		{  0.618,      0,      1 },
		{ -0.618,      0,      1 },
		{  0.618,      0,     -1 },
		{ -0.618,      0,     -1 },
		{     -1,  0.618,      0 },
		{     -1, -0.618,      0 }
		};

	private static final String[][] TEST_MOLECULES_13 = {
//		{ "#qRQurt[e[frwKHtHToTBxrTcqMGx@@ARFEQGrtnqKUGzdEz_UWhY`ej`xGdxAEtzAHenDjiW^vISQPZjmiwNlIQFm^{qC^xspvXuR~OUOfoZbQfRHCQHpXw]^oNV{TKErsdDjpAvo[U}C@gzYCdgna\\[itY^YuFqbXsI`ckzfNYK~YyIjQlH^h]p_LQcX{uep{Pu[vDzqRqd_}]q}JlV{]~Z^pApa[JG}FtMeEgHmedlq~`jPOmaVaGYQXQ_xumsGv@jkvaGHY^csVwgFRrnWunufe~i@mxBu`dUxzujGc^Eii[tv|\\ovwPY[EnSRG`D|RnMuBXV]rnr^Bh{s\\TRB]beo@bVAihvzFYLesYyIY]ERxo[F\\vQfeF^Iok]`OJpahZFAvv}WexzsP{F^SaIOKKVf}fE}E@MnA}Rs{\\R_W|SmTWeVFYHZfEPn@QFfJXx_Yy|uJGcgSZGAzoiiX}y]iv`BLAfXD", "ehRPJH@HKLgo`GCIIDhdhuEMEHkHmIXk@faeco`ZjjjZijjjjZjnJEhoHEBiuCh@`abNHDbRMXHNFQ`" },
		{ "#q|BGrxP@@ANyZ}tKz\\ZpcZkyFM]NellJZSV\\Os\\Hq_nE^hvo||XgD\\VH{[dNKmL}ZBAlcWaKePifsvZNKP~O\u007FVg{jynmr]m_Q\u007FnOeX]K]GREk_AQy]ZxD[jz\\TLGY@Xl|J^iZ^X{UuXEXqnS|QPTmb_D}vTOEeirnP}h}@\\RATP@", "dg]DHAUnCDRHrPhHrJPjPjIG]UKTt`@"},
		{ "#qXCi^QefLDgJtCf~`ozqoyfnbOP}Vb_k~kB|]U^vKc@FuRAfI}YCXxytEVPiffXKTlc~{XUV@CCdn]RJjBgfkRIOu{SgCaC{axRNqaosYeByVZuZgs@}A_njlZSQMdT[hAYosvHjNqbZyYV|HlfmX{PW@@@fX_XAdHcXjPW]Y[GevvYSufSEi]oTKij`vFjCLxQyES|}FmjNMCIT|zsRk\\udd|LMD{HlossWy@eBYq{ym}Xaz[MwdiDLyCQg]RwtoeXm^fglJhXZaaw\\}]kCnnW}foNOCj}Ao_TGj\\ZBL_dIA^@cg~fkvS|SyWxF]ESAvv[e^gN\\xKKDyfM]CMuCBT{rfSD{|qpnaoh^a`g\\[J{dBdV|]sYQ\\tl~a{OO{kPi`xjRvd[mWRfeSXN}C[EMiTscqpGa}OAqFOYDLhaBaG[fJhJ|kAH_]dR]C`UPg\\LM^[eidUXltHxVhcobs]LwKgYYZ}Ikeef]DrzP\\y~vUvZOHExL[q_ogLics[z_tQdogB@rMjZRG|_BrJNoWt[klCulsoTYoi{ujHlfdijxMB~fhz{Vksph\\e[EJGC[CeR\\tU|d}jPIiMbammHwJxQRyMEcTXMQTJ{hum{XDF^ZhONbTbR]T@DmMevPitkQYeHTj_pgiFnVUhi[[B_misPJaAvxD", "ehRPL@@@FildTrTrbVRfbQdtJbbXDTL|BRJjjZjjjjjjijjT`PbTQb\\`RRUTzVt@@" },
		{ "#q@melvX|NS}tHUZRPiQ{iwafs~dPwyvrVJoGidagGQGTQLgh{|U}a{cc~tgscZw@Ux@bvkXii@_GoPBYcZS\\i[TdOHmDoGPAzLYUPpJ{aASYYUVxl`[`@DhUOg~hoburlSPpNQ_BdQE@y@DXQxg]o{AJwEOzSgvgvNyaYLgX@@AGNbQ^h@JGnLlIBQtv[yPsNHhsfVXX^XAyGWxKpChviNHahyN{iWY{tzOdLW~qta|FbwJIXZbXOqw[h]OZ|WLGQmgQSgYKPxukvSSYVWHZJSLzjidaK|MGND|Mzdgt[^oh]aXqYoeCevIUlBcYRxmdlSblKUwdO\\smfJ[yFzeK\\]dw}Vb\\]jVIfFQwbPsOddQuZUKN\\`Z@ZezsJr^VRgkzwKRLCe`mc~YFNP{iycXb]dgxDeDbi_fhzChhSznjkWVwJbUDp@aMJL^LTYRzaC}Y[UZhlrhOwjktNVgSgyfBnKlUBbUVL\\hGCPpYmtqoai}Vd{WgoCCELUxo_K]`ekY@KrA`pD", "ehRPN@@@NCKHk`\\bbdTbbbdqfR|RrTbDt\\RzfNnZjjYjfjfZjfjhPfDXVb\\`@" },
		{ "!qbV^JTnBVpm^KV\\dmQ]E]a@DxbfomSG|jOtf{Ny_[_|jDhey^KyNXtm`draiyHQQM`|nayVg_dI_L]iNSkH{Q]Lyyr\\ARKa|nKEncHXKYeRvUk~ItmH{KjAHatsA{jAI^KEA{qrBBmbu~~IuRwOKXKXZmAUkiNSdwMQ]OtfDqM_[pm^tihdmmYSmSG}UbV^uk^BVNby]aG{GR_@nf@FsztaNHEsAtENXsR_kM]iyOnnSFEr\\BmtnCRKEb\\weiij@KJAP\\D", "ehRQB@BHBGOHdna`IImbId{jYnrJJJQJJUUJIYQQSSPaaspjiXzijjZZjVZjjjh@@" },
		{ "#q~OcaBhRvBisBNfeNVjA]GevFDufdQkopGJY[nXPOjxE}ZFuGy@@Leyvj@}GevFi~v~u]{bfAJAJlD]hpk[S]Sht|QTh|gOWHlbLWICncWCBisBNfeNcGZvz]a[VW@hiNll@{EIIb^Dik_WZqSWTSN\\ZnnVpv|]_OC^zFiitpNHkoQciQQIOJCboP|AEzVVCOq~OcaBhRvJ|hYoXz]wLkaoPd]UCUfPkEbpsWnPg[bY`wP}RuCF_KOBeJ|_SU@vULZFGIbUkpZ]}UcqU{Zcoextk{Y@lhIbselPXGKXDfYxt]jXOeBBh\\NbDePWjw~f~J[trknkabVTdLUARDK]F_znAMdenVamOhVHAQAuD@IMT^T^^rYZQa^R[wByoyQmii[sb~mtrPTUz^jhWo~\\qus@NokbEaeWk`AmNJvhZKUTueqWftjcJZueie`LhAm@@", "ehRRDLBDFNABJ@@c@pVBDNHzcaNrIJRQIQQJJJIQIkHxpIqH{yxEP@JfUfBBP@J@@cRloFI@" },
//		{ "#q~OcaBhRvBisBNfeNVjA]GevFDufdQkopGJY[nXPOjxE}ZFuGy@@Leyvj@}GevFi~v~u]{bfAJAJlD]hpk[S]Sht|QTh|gOWHlbLWICncWCBisBNfeNcGZvz]a[VW@hiNll@{EIIb^Dik_WZqSWTSN\\ZnnVpv|]_OC^zFiitpNHkoQciQQIOJCboP|AEzVVCOq~OcaBhRvJ|hYoXz]wLkaoPd]UCUfPkEbpsWnPg[bY`wP}RuCF_KOBeJ|_SU@vULZFGIbUkpZ]}UcqU{Zcoextk{Y@lhIbselPXGKXDfYxt]jXOeBBh\\NbDePWjw~f~J[trknkabVTdLUARDK]F_znAMdenVamOhVHAQAuD@IMT^T^^rYZQa^R[wByoyQmii[sb~mtrPTUz^jhWo~\\qus@NokbEaeWk`AmNJvhZKUTueqWftjcJZueie`LhAm@@", "ehRRDLDDFNABJ@@cApVBFIxgHdiIDeEDhhheDflcc@gDcog`UjjjYVXHI@@h@@@" },
		{ "#q^iY}UZqxnYaaKfjbcgmkDsaBARa[BaNMIIH@k_XxXxPF\\q{qNQ{LxCSY}_wV@{dLs~ccRltMnEwOZUrb`fltMx`_UuMgFb~HXzWxha`xU[OipnynTFIyVyBvroYfP}vYHwHtCqwhprWT_zKX}P_DmhJA]IimZH|TUZnY[BChfsnQctOkN|{sai}O`yMDes~MSclBaR\\bjTRQzKKUyULRukHryi{C`JX^jzdQd[EfyjwBVLV@RPEDs]@`Gxg{^ZVslDwezOTBmYHNilccff_q_cS`q[[NZB\\zFezHw|]oBLHyicUGC{^z^CHhiU^MsFSeewCEB_a_TGeZBSGSYfRUL}cchesQqgzEqSkVbU{lg]N[NRsqi~U[mUHRu}L{CjDy|Dyt@vqrBwE^MbMlrpG\\QfFyYG_O`GTkhmT^iZsHC@nBYJKhAkK^VeDnRKbmIh^hJ~XCuQj_@NbAf@D", "ehRRL@@DBfg`JLlbbRbTTTUbtVQTRVTt|RjZzNnZhHb@`dj@BhD@@@" },
		{ "#qi`NaxxHsCja|RdUY_geevm_p_hyT{D^aGLJ\\cSSjUjq_FADi[qgd]\\GEJRMWHPZEvOvHxYCZaSyKVtLvbOwDtTCo{D@@AUQmi`RxkkAiabxxfYASiyNHeXhNNvBcXSgVwNY^FH@OpoAue]u|~pFacJNO[MFfs_fGpEdU^GnuTTBLa^~ziwGQxvvGULf|h~nTw`aUWs^ELD`Gh^eICkyMazUhZG[[xm|~DPOqRQhBHAo~lBNLZpG\\UbBF_arZ[WNTjWr{QgzM\\c}yixYBs[^CnKbpgTv~Vv~AlMMV\\J\\vkXJFv[YrdRB|pkWh_ZC_WO}VPuOQVoLjzfrJoVTGRuZu_o[gqasr`JVyJ^^[By`@s|xwEP}CaobFsMElaVsvOaUaugQFcz^xLQQtKI[BMRIDDmAHahlpwJWfYPisoF`pa\\weFkLj^OmkBYWMC_bYgI^j|uhFyFZFKMfEYNc{lZVgzB{rAYZe{vm`S}fURfotBfnDFAgls_OTGrdSRh`s|OfwfHsxPMh}tGoMFgfYBj|LssQgC{OEl_tEOkJLxdQIrhSdcUuVyhu`OlAjXD", "ehRT@H@@ALJ@hbDaHQHRDQDRLaL`XRDRLa`SPIriXzh@bHHj`BHjh@@@" },
		{ "#qgyWg]fDpS\\C]d^yIs]TCzLsk}rMyy]lZ~V\\jbNJhjZmSkgm}s__bhRFufDTTzfWivm~H\\w^YtduMmbCLakCxWWA{BMHyudRjqHPBRbCIVnMT]PkgL|iQRXKHIm`S]KKJX~`|eFM}KSIoHKVyRAiDiOILZ^EKiLdL~HvOn@himX]lRWcz\\tXSjGruPTu}]t|mmL\\qkfOlJ]IjF@NmuhLtfOlQhdbnt_]UIfKXQJcmu}HqNAl]rlbYD]juMXkkickmvjuPVOFe`YFQThc@Yr`PQwr[Lyjm[CcExeh}HTi\\xMFQV[Z]MOXy^\\nH{Ju|\\T|ccGGuL[aY}rvDWIuoGwX[Yaoit}SWYZEse\\Eb\\sRfrDUVRQ[LIPDcE^AbOHGgAJiNFkwR{BjKMr}ltEPcHdNV^}LrEKpiSbUkMwgd[F_Vinx{|^fnVZ{BzacLrph[iysgiw_MueWYjz@Bp@QBD", "ehRTBH@L@hDNa`@cIIBhhhhhiETeLlxdcLjefao`UjBbHJ`Eh`@BDJEI@@" },
		{ "#qWkLUM}{EAdzImP@@GQTeLWNgNCGfHkT{dtX^}xNiZJ^@gAtKGX~gIMc@]wgYvdABw{WsQphuRVHQXUH{tV_HYZwH[zWg_mLX~vnz_T[BXq|DgLZFuizvoezIYkeMrCeCtLNt\\iOgdt`QAWoOfw@VZWYnt_pV]{wjEdZpM\\]RteBx_uZn[VUCnCl_X}}CycDMvjF~jydmuYzuFyWLU[Jy`BdTDPTKgetj_A@vZm\\Ghjv]\\XUuzhYogC_PmLZqsvBpLt\\KamWG_KIaQriivHe{VqSXlTtaZVMgOkRFDqONemDn]\\h|~aaRaWzhbjvMjOFejPGlFAq|Z]\\Iy_GzgQ`j}]jmhctfa[{mhnZuWcji_js|\\GogPiJc\\JciYXfXYTuIjQb{UWb}]nUmUpV\\YGF{cV_QjJRdE{slcR]YvV|OY^IHUunCWv^zPhlF_Um`egiWLAIOHv[ZnVP@T`JSICyej\\_ItQ}iin@AbAJ`D", "ehRTBH@LKCFac`@cIEEDddhmMdhhdddhRJEGLnegjefjZB@H`jJh@HFH" },
		{ "#q_|QWLdjqNdrfODeaZLHkQT|dipcbnncJCnX\\lmZLol[BfjzK_eX^JvMYjMVkVm\\pOOco[k\\MglqDedFAGDasyWY|SdAMjZX{~Moy]Cs@riAj}Ir[yJFnPwLin_VplYNptlWF_ggSx`Sqh~u_cKGabpZqVla~cUZpf\\jJmZanl}OTDUjn`Q`U{UZwOLr_]FwY@MvGfTgjG^}du[Xjp~DGYp]Xe~QfIBw^txS]HeQy_mKc]mPh`\\\\SPPksa@aYwZWH}BVh_DrWOZiijEcDnuRH]JkugtmqbO~d\\qEzjNWNlBaUqYTw`nlkyT\\ecnMy~UtPTnCOiU|AWWbYPzk[DNv`oTzmjUmTDhCrYTRexVd@uvqQeOAUFyDT]k[}eIrzJDtjNjnX]Wny~i{AdiCzHsb]ujyi~@V}At`D", "ehRTBL@EKBOJf`LRHHrqLbbbRVRTTTTLbqbRQXDRJNn~A``bjejiX@@@@@@@" },
		{ "#qnbzm]XCw~CfFwxsaBck{WA}[Mq_e[TljmwRNnv@y~AZnTOSVzGXZEZhhr{J[wg~RG]V@d@ZoJBtud}U`KvlE}PfLFORd`kKGHr`JpRE]qNvw^{TerVvBK^u@eQA_SJbCsht{@f[D^iyTQ}b{SNTaoUm]WtgvZ_M]WtfpZXIAR{hFYJ`tbb{mRx`vtT|SB|WeBJ|un\\JTKqxA@Tg~hyOOpjm`QkqkLzqQq{IYCDFh~LWmkDehxwgKnpPIxFI\\~YNLxtF[_IQzae]UhwwERebCAdlsNiy`yNnNlzf|Wd`cWk\\ZARDsi{NDtvqwDNK\\JIJ]abMeuJUd{]W[KCHuPqVID\\C\\uceaiu@JZAypD", "ehRTDF@DANa`IAbPLd@iCJPZdLbbbbbffbbrVTurbRDtlbjf~AijffPf`fJ@B`@pPSDI@" },
		{ "#qFa[X_sjgTbwognWhmpHvrezB`{XV{FsYKZaFpdsPNpevz~FNDOf_bBOUZYe^}nsgmv@ZKHIsJsyz^TwW`HHF{UriOGDzWWOWY[bGszYr|cZ[HLA{Q[Oly@k[FI{uwriVILs[WL[Hdg`rDIzIZagiRWEk]rjY}JFpX{KOPtOJvYSLbJo_OkmFDzQJ{ITAyRS[sIS~Azjo_RIc[A{rLPepvFT~NrCimlJXGxS[|cMAZHE[rJwitIj}bz~mTpoAgUK`ePigwj~`TClDPk_OTeVgEvpoZo^uSJeje`qyGTWqdBCtY[Zbt{Ahz{StthYFgQR@SjqnZT_MYy]ECdtSHmAsYfFcIT~wcFRIXul\\UUnEWi]|}FosHzVYNsSzW{iuLJCtdv~\\fWyp}Hy]XrjcuYQpEr@PrtxL[QlsD~]yMsnJebmJkBjCUliiE`MeAJXD", "ehRTFH@KDiMEmck`LSHhdieEMDdhdhdhhSjFCKO@igh@JjZjBjAfJIDNDi@@" },
		{ "#q]_X|VgKKqJPmHyFtOyC[sCe^rBJafBewZ~ueOgr_UnzoY]A{H^zWNm|dgdm^lUM`nJRXE]Ibr^SdYxGv\\s]s}OkZQ]ebgMi[}]iAgkE{lVIiTKgfGV}sEkJVKKCy]Ik\\Kgtgfek_Je}K^xN^Tt\\^CIx[XV^gz\\EsN\\]{BsfUnYBl@qeB^PnkqDhsxm`cPk[XaVCTInJGYjqXLGqv^~{~]z|JWK{ALmwHdnzlpEQLXmWPUzb~~rywgnv~yyu{WOqRwenQFDwGlqPPOHsi_vf{IwieuZp|ibFdAZphzQpznNiYz[htsNyizyHzDnADMMMjDnBwZmcdqCFZhD~jQJb@M[QdW^[yqhDV[~gGFg_MsiNHWw{VmIPoiExcvvIp[RVjmuQm{D{cm\\fwz]`eWyD}wKAPKqzozj`amyb^K|IUGIUYwyQ\\CImczgki}\\[qvQiWSqNNKJQYFizJM^h[Cne_h\\c\\yZqPz^DUWCSYho@GhASdD", "ehRTJH@COFNFnc`LQIfYgwYYvWUkTLBbRJfvfifjjZhXHjj``TdDB@" }
		};


	// 16 Fragments with exit vectors
	private static final double[][] TEST_POSITIONS_16 = {
			{   -0.6,   -0.6,      0 },
			{   -0.6,   -0.2,      0 },
			{   -0.6,    0.2,      0 },
			{   -0.6,    0.6,      0 },
			{   -0.2,   -0.6,      0 },
			{   -0.2,   -0.2,      0 },
			{   -0.2,    0.2,      0 },
			{   -0.2,    0.6,      0 },
			{    0.2,   -0.6,      0 },
			{    0.2,   -0.2,      0 },
			{    0.2,    0.2,      0 },
			{    0.2,    0.6,      0 },
			{    0.6,   -0.6,      0 },
			{    0.6,   -0.2,      0 },
			{    0.6,    0.2,      0 },
			{    0.6,    0.6,      0 },
	};

	private static final String[][] TEST_FRAGMENTS_16 = {
			{"!qFKEdFg\u007F\u007FOmAwP|dnOj\\@|}jZ{|aBSMVu{`acKKxUvMCR_WGVg}bkfd{CFukp@U@Ax@D", "daE@@@YJe~fjjh@_hdYU]U@"},
			{"!qbCC\u007FvQrI\\pD[yOPlevsxGFR|QgJ|ZvbSlwAMIfLzqs^E@dOjHJ@YdUCAijBltY@aKkiGQJMtX@jVZyFS}g{\u007FIP@@VIi`@C@@~@@", "dcm@@@iJYe_raVPhHh@GzICUVuP"},
			{"!qop{PpH@rshyyRg\u007F\u007F^MEIKnvHDkXjsNJs~wX@rFaAlzGGtud{GKp@gGrRC|l]kXgB@{Zv|FwfszG~Snixm}P\\HdZCH}Ir~ejttGUvCG^SUEj@@_`@x@D", "dg}H`LMPBLdTTTQfaUiuKULu@C}Dajjzh"},
			{"!qmb{{FmZ`GGQCdlI[{xlHg[rTuiRtiNGjPhn]z\u007FBMO}z\u007F^`D@auxWn`@@YaRTiCyZUrpqeHr\\Ja[o\\k]P\\VTqDZ\u007F@we`qMDsM\\\\iOOI~pnIit@O`@z@@", "dg~D@EBdin]V~E]hHBfb@GzIEUQuP"},
			{"!qTKe\u007F\u007FwTzHpKGnXidRxWoUlFx`LSRCVnqyrKJNS@tTBHSNPAtPbQNCYjIt}Fd[a|hIjkkR_sD\\]RcU^HjBvmPBlzRcMm^xsWnZEhP@T`AY@@", "dk^@`I@HrIQISJEneZ@Bfb@GzI@UQuP"},
			{"!qBmUFAGX\u007F@@AJgR~KgOSLt^SgfVzJtfgojwGWGGZoSb|~ioOutKCzZfI}gogKKO\\oNuhh@_`@S@D", "did@`@BDiWgBiYjBJgzIGUPuP"},
			{"!qxtJlOQbR\u007FbiKQ~tUuz[mnk^PuWg@UVeOTNrR@vK|GejFUDGkzzKM]xuy_\u007F~`b[Var{FYyTCo`HgbQRH_qNHPeq`iyKD~AX\\oDukp@DPAJ@D", "dkmH@DtDf[k_WVjjjh@_hTYUUUCUGU@"},
			{"!qGP_F~Xqydjd\\~yKC}D|kH`NF^TMTtpKdJf{A@JMjj~~zNif~DoJzsfG@^kI}Kob`|sqJcF@kkKA@c[Snm^{~@HuZf_x@@JqZEizF`CQjIizGK~fnPOpskFRK[G_UwKUqe`FHhNpZjwMrTYP[\u007F|WLYLb\\XA\u007FEPldA}AolCFVN]y_DJfBuwL]HMd\u007FCBkO{yNAzO}MOyBlzvl\\Zu`}Ccb]HaiOmyEiH@S@@t@@", "ec`TJ@@OD`DFEMdfyfyW]e]UDtl|rJjZfVZjjjjjZ`BjAcADRa\\HRVPA~aDHUSETyU[UP"},
			{"!q@@CiCfZes_Z\u007FnjUWvEV{D{wS[h`lCc|I[}XAAnFgbmXOajeWBJcSke\u007FIVHum_]RSxTb_KoxIljHtI{UahO^lmeraTGHZ|~~UT~HWmAQfJbbRmaqQW}a[_`LIReaC_o|I@tqR[]`eJEXQfz^U\u007FVKi]MSfPuhD@L@@f@D", "fb}PB@JI@DIKJrkNkLxYp\\qSUUUPPLTVDQdx`OtQ@JkUT"},
			{"!qdt|\u007F\\hlQIizGK~fn[PvELQx\u007Fm^{~@HuZj~~zNif~AkpkKGt[nEr\\[MYkJf{A@JMj|{ATwWqyf_x@@JqZ|sqJcF@kPOpskFRKEizF`CQje`FHhNpZ[G_UwKUqXA\u007FEPldAjwMrTYP[\u007F|WLYLb\\}AolCFVN]y_DJfBuwL]HMd\u007FCBkO{yNAzO}MOyBlzvl\\Zu`}Ccb]HaiOmYeh`@V`@b@D", "feoQ`@CQARZTwOKjkZjoAFlENR}GUUUUUSPAU@rDpfKbDkPC}BPbjiURjiuP"},
			{"!q~~[[cmX\u007FQXgvbDV\\mMy~\\Cg}sm[^}uk}HFZoPW\u007F\u007FKzYU`VW\u007FMjyI~A`kZMZi]t\\C\\jZ_mSgO{QY[^dV\u007FeXxV[NfowXXklrewucpMLx|{g]qPH@wVImEUSBJ_~nzU@fH_ex{Dg@O_ZQswHHJNEuqWPKrNBp{joAr\u007FX]ZX\u007FJE\u007FDaLxuFRTYJpZxCrN~YrNBFjV`j@wVRcJ]ejh@AP@h`D", "feoQ`@NV@NZTvmjlkjr{ARU[tTu{PAEUUUUUCTsTcDsJkNhC}LPvjGUSj`"},
			{"!qV_b}MWBQeFXVL~d[o\u007FcsClADpkBjKQCqocLEFQdGri@WR[budCe{J}_Xq_a_uORZ_WzTqW\u007F\u007F\u007Fwpsvpfcne`I{nvBGXcQ[_u^~]zknMlSIq{Hn`XxYoaKcVEszLJ}~{CRGXpFQESM@{PhsheTpEkb@B`Ar@@", "flupP@DLB@ugYEEEEDmCKDpTeF|uUUUMUTSDIAKt@\u007FQD\\jduP"},
			{"!qV_b}MWBQeFXVL~d[o\u007FcsClADpkBjKQCqocLEFQdGri@WR[budCe{J}_Xq_a_uORZ_WzTqW\u007F\u007F\u007Fwpsvpfcne`I{nvB@XOrXeQxGXcQ[_u^~]zknMlSIq{Hn`XxYoaKcVEszLJ}~{CRGXpFQESMeEkb@B`Ar@@", "flupP@DLbBsoYEEEEDdhXxpTeVBuUUUSUTSDIAHL@\u007FQDFjhuP"},
			{"!q^MeWBSZwP~]`bb\u007Fm[^qGJqmkX]ZX\u007FJE\u007FZJLhoxMq~YrNBFjVXgpSzuv{_WpxjqC{jGG[Xwp``j@wVRcJFuMeGtMqEnOpw{uqhbLowwHi~QFj\u007FQw`jgEid~YP^AuRo\u007F`qCUf`RdXpRIxg\u007F\u007FMnVRxjluu`RUDvAv_TxnddaSi@WyfPo`@@^JGXctE|brDActXBTEdj_ah@~AgD\\\\g@kEh`@^PAx@D", "fm\u007FQP@GQ@abeyEMEEMDbdhdlddKUgIrVhymUUUUTu@ETCLPbhnHTl`OtIBJieTjjWU@"},
			{"!qqSMnFHosQaIkCLHc_\u007F\u007Fu_mr^NTMCmD]}}\u007FvRCGd}Lu}Mwm}^@Ki\u007FyFBstUVCUN~Cv^_dxbSyFtA@MdTeMeDHVGXsbup\\W@wDN|UWkkNXfrcE\\jkEwIiP@Ip@E@D", "foAqB@BB]XDPdrmmoTXJTttmST@C}DQjj]U@"},
			{"!qQ[rb`}xbZphDu|Pug\\ZkV`@@\u007FHWQmQp\u007FEppMhzn\\R\\^EYYPt\u007FYkp@LPAj@D", "gNy@DDfYZj@_hhMUZh"},
	};

	private static final String[][] TEST_ORGANO_METALLICS_16 = {
			{"#qVKum_vVm|elR\\usl|xPbLQPxpInM@[oWwtPs@nXKsNWfBN~rSBh|{x]b~jkCuPS_yz~fu[[i@@B^k_Bq~jkCuPS_kpCRSQLyLlgKO`MtwtPs@nXKUyEolqwYkpCRSQLyj`FHUYV`TtZvCSY\u007FzEBYJhdVe_zwjji_tqki}~AMubguxjQbJFxPSFHfr][rGYn]SS[TpWrKZ|UcSi]iD\u007Fjsamgah\\ecQiOyiqfswl`ZgYVcQ^fnE|j~\\PAF{OE~LbuNRzvBTXKamjYohRPMpmX^qXCZN]gMASb~rQh||T}ngcX\\nZpFfNZLH]_eQb[R~d]AunUCCcBQ[@VL^\\X^ECh\\lZbVwWyUzRArytwGMdAQm}kxZczAwReaNk|eMEJ}kkt^bUdPWeorphDjEe~MzKKXrS~nbBW{eTE~xpxAsUJqZCVAcg~yhfj\\nnYQkUkl@Ep@q@@", "eejV@GGFIHlbj`EJARPBL@``HhBN@`PHTTmEHqR\\T``aHH@vCTFpXajFxXQeMttqWEBYIldddTbRrrbbqrTBDAFBeFeAgB`a`cf`QbS`SbUUUUUUUUUUUUT@@\u007FOxBAC@bac`RQSRspJIKHjY[Xzy{xFEGDd"},
			{"!q_y]\u007FzpJG@F`@E@JG_~l@ZGux@AQ\u007Fewux\u007Fyy\u007F\u007F{XI\u007FzD@@KXI_\u007F\u007F\u007FhXgv@@C\u007Fghgv@CQ\u007FHqgR_sM\u007FH}ec_|l@wAgR@Lp@wMec_rM\u007FrvXm_rL@sLZ\\@Mp@MFXm@Mq\u007FL|Z\\\u007F\u007F{~_P^qYch@@@@@@@@", "fdaHAbIZmxEq@HPDXBBADX`DqyJiUYfXjeVjj`@_fPDDX@"},
			{"#qMdD@@@WE_\u007F}Ioghz[SjMOFKHllLRqQtwzqCBdVMmwV{d{qrR|H{idfKRyfww\\Qtm~VDJYHhA]YozgkW~CiqAgdKjXaX@|\\ksOfEqXstU_px]z]TLjmxCSAwq_KNUHVHNCIOqIl{Rb{P\\Vb~FXzVtRuAy}mes[MDmj{r_xYBMo}P^@VaahAzTPz}rX_z@Jq^^PR{DJYGbv}X~tZx]^Sz}[lYbARV[DQdu`iDFp]f]iNyzvf[J@tt]zXqif|F[pdlAEAy[R^sboBynrZPRIbEWpeO~klkKBZ|euOZQMYCZJCuYwbpADSZCtYomIKYkx^L]iCvdOcS~XaY|lkNVe\\k@tZaf}`YNDUXMwrzjMY^YL^{q~RgrgAkx@Gh@C@@", "figHrFJLkvBLD|DCpA_\\GdjjUVYVYUyHRjmzNjiZYjjifj@A~[@PQ`e@"},
			{"#qp@I\u007F\u007FucQTM^ge][RBD|ZQcRjdWicBGjJpYS{@lH|vF]cd}]Qll|orkcYR]kfLBRykNBqVc^^Zu\\WJWw}\u007FjQ|g@Zm}_Xqa[@kCs[zoSv|Dmsw[iqsxUpNXQRLmPha\u007FP^QJ|{Wt~}fYLE|F\\WJ~wxNfh[jTeeWfj@^Et{epRtr\\hGNUSJIkhFIbghUrLU~noUYddk[NWl~c}F_ZURUWb{n\\^xMiKkF\\ma^]]VkX\\eU[geSmbtr`rivydpIKlWHmYsQtIjZ`WwUdUkJh\\hJJ|u]{e]nYnYu\u007FmUF{Rz{aQQexmk`@FP@F@@", "fd{@@c@RdHPtAZ@A\\NUvnMunJHbHbHbHbDatQaCL\\IP`jRXkbaZejjjjjj`@@@@GyvAAFBTYpTeZL"},
			{"#qzAX@@Cf}~HAio]Nz]cLJ]cU`ggLNf|Z{o_gOt|gWSvnKsvTjZBA|{YUC^kMhMFHgmA]unaR|M\\qQP{|L[hIeF@VUOadtUPpc_xj`K^RJXbhN@jTEvndkc\\K}tlfO}^~CB~LjyjTzqnTPH\u007FZf]I[PRkM|\u007FTvkrbdMF`Iv~U}U~LXcladbqMTco]ZzBdwER`@MrnYQRnc|k[qIzU@uLgk@@F@@v`@", "dcvLRbaLCRkpCxYFqb{IIJiDeUMUP@\u007FL`aL@"},
			{"#qvLlYfYY{XtD{xMIrEnPCTzVRJbmjh]_FgGZjDTjk\\WXRsjjTu_E_CMDelmZ@kjATwYJ_oZhE~WFs[`@@^oup\\[u]aPNk\\Zm}|Ij`Yp`\\gjF]EN\\NOQzyoJVBeLfE{jm{GksG{]NvZnHu^WTBnXDKZZG{m^tVaN@||Ij`Yp`\\U`xRdYz[~WFs[`@@upy|v`CdOSNjiX@YJTJ]YUeMVjDHMKLY]^kwPQUdUqI}`xvI|^ZswC]MkWOVzUqylmZ@kjATXTOXDlqI\\WXRsjjThUzbz~cqJbmjh]_FAPHOchJb]hsUZZvzj]vUVRLbLLG\\EIhAHewab[o|yYjksKjVZxJE_XG^EnPCTzVRpfv`PiWzs^e|BmMlBaWpofj[r`x`||{Z~]XVmS@lrODCIW|[hxfU{cUTJ_Em[jEdagyteixDWZKn]hPCzfVTLxUiEGvz`kxajsZzDYRDebJjies]BWOJeiIESs{CzzW~norTciRBJNtB_KIv}afLHtbrdhsIEbNFIUywrxsfUkvbfbZrPlrUVk\u007FfobIywguIKuvCp]WVghx`X_x}sNFFPdOamlk~uUjajwWOd[pUTJJ|OlhibSW~JbU^tqzyoSp^`]tFHPJveHkP[hOjhWE_go{B^Ajp@JP@j`@", "gbIdp@NBHBC@PXBBeShBQD`EphnKEqLdEdaldKd`|dOd`rdNT`JdIT`jdMT`ZnFtavdAt`ndMt`^dGta~d@LaAdDLaad@DxNX\\hfqMS`WFiJHaHRDaHQHRDRDaLSHaLbDaHrDbJbJaHQHQIrHSHaLbJbAE`pt\\^HbQDZIFkShMFKWj`fhJiZbjjjZBjjjVBjjbjjFi`@@_fX@PDD`qdJMMVGbI@"},
			{"#qvXjd@GtE@@AYmcLmy\u007FCqSlGkM\u007FJW\u007FOZhJ\u007FwvYGDqAzSwW`IhJqqAT|QeAzSwW`IhSoG|mNEMF|MeSly\\dy[`HdPz@pMr\u007FgrEM\u007FJW\u007FOZhYCpZl]FcLP{}R~zrWmstoklfLhfWk]au@@AYmcLmOggGWqD|UNL~kMnZG[Zi@eLJd]UNpdm^QgU]HkG^\u007FBfw@u]UU@Kqfp{N`zrMPZKz{KHSsWWq@wJfkTFIxiM[OoFESWZhTl^JxtulL`hNzbON`PdE_OpM@PMzkbhqOSRaEdeoGUNu_HvYTcyv{Vpdp_YzZ[XPxbqJNXhbwXxa\u007F]ZH\u007FBbjy]sQ_g[zkChBOfqedyygZh@FIlF|uVyFsWTpGnEithiOx^zVVSzCJaFyd|U}pQNZkFDXe[\u007Fy]@zXzjKfrKWx`XiiY}zFUPIFkpuDwcTIU@ZzH\\lU\\xhxoeHvEKD`i^{yL|UOgbmZaIELMZNYODGozg\\ECGUGPRwIaWgBFf@~B\u007FFgEYtYlwiLIYUQZty_VnDFSChpXURenvxsriqfP{{`EPczcHTsvjjnutk{_kVVFBFyjgvydOH{HTkvJ\u007FfEwmSjnh[]yQ\u007FAdEhD@[H@l@@", "eeVr@EAJNBi`JrBxPnLJp`HXBAKbrxRkLjpChIPgCaIleKlnrQRJZQQRJJSIQYJEHiFKQPHHXBVRZVIVYNAEU]@sKGXdl|ruUUPUAUATD@@PQUU@@@@OsR@`PpHhXxLbRrFeUum]@"},
			{"#qpCNhnEqNBJ^aTl_]PF`_KobSM^p~D\u007FNc@WMUrfddM]\\j}^fblacjN_mLPB}FYWBUsMrLe|PvVZ@ibhCNqvyebHvfrrlQ_^AnvSUirgu{cqJOxo]KMSKl`pJBCHfdTu{KOVpgsuLrbSSeKmvl\\`_b~gYr|YX@gCTkJ|sKrlFc|EvJlpz]zMLDcCAFDLgybB_NcJgVvWSWCha{R\\BN@maKa]uefvwMFqaji`LTziCK_@zOGr{bihevn`@@F_e`GlBtLySDF[Z~x{Uf@}vUp|[GhzgrX@hKGOBVAFmS}bJvJ}ydYaDBd\u007FECgP@zBXys_jM\\yde`}XsUamGRhrJNk^YOp`ZN_\u007FwbSc\\nvDJozZsAvPj}V__Is_DUz[zrZRz]xgp^{}V{hVreLyEsAXJJb]xvD`\u007FvlfUdDPMUI\\EVdd|vdUHOd__IHMedYkHuZ@ZwFm[Fr[hoeQPi|UUcJQUzYVekylGunSi|uQKqpZe}YlGuTMWeF^aFCzXPFk}zx[ydYiwMo^k\\embzcbvO{ctVDavO{mKQYdxu{_tQl`[i^ccELbxu{UmQ\u007FhyIsaJRlXY~^npea^F_iZcUXTYTRfwIhdZpfTRjKJfW~U[Ub]hxAWXFl]UYmoYzN~tqDVSN[LyjvTFMBFxjxnaaGet\u007F`]bAEs[EMTkzgM{SsYYfeMXhpP~mIxY]FdNFtau^S`xAQsZHhWymMaxlSTpfXZIX\u007FiL~wJgWwkfR@~KwT_tQI@dIRepEFqGosl\u007Fbwhg{BoTya@UvmW@I\u007FzG|{b}bx\u007Fjj@Hd@a@@", "efUt@GAGo`Ebb`XhNJ@R`dhEJCQpLhKGAqp|h@gBIXhycnNDzSeNLwsRbHbHbHaHQHQNbLbDbLbDaxaHbABBa@Q@s@cAPbPcrab`q`Q`qbKbSaScsbHPrSqSpGFjjjjjjjjjjjjjjjjjjjT`RBpaDhcDx`bdbb@_p`YOds|@`PpHhXxDdBn^ck@"},
			{"#q[@PC{AHQavc\u007FieOMseW|M\u007FXbajZGGtTZBEvn}I~kcwoheGKisMWBGRTH}es\u007FLpLLcRMKSoow_\u007F}\\efnQnJ[BiZp`ko^OZQpCG~ITFB{JzSSVQWykeNWVZXCII\u007FXYATFEBbXJNj~iyPEizbeFIvTXmdJVa[HZolUA{CXPR\\VUSCeATSBZYOZu^cMzfsEZ}^OAoPgW|kLQJm[]odNrMZXZy\\\\~LSG\\KePMJqip@A`@]@@", "fo[@BE@B`Cuqnmsnc\u007F\u007F`bEdNpSGBBhrdfJyhViFTZjjjjn`@@@@GynAAFBTYpTeZMX"},
			{"#qMRG_RwgGJ`y\u007F\u007F\u007FcI}PK}JZGOD@PZNtUNlf|l@er^xn][mhsTt@uDHKLa]~dH|ny`NRxztAx|]EoyJT@cvXOFBxyrXSy\\kIMk{^pjlYJBR{IbNRbxV}`XoBKVgvJq\u007FVzKOEq[nt{aTLQteUm]]~q~Ng}~Tka}S\\geCke@yEALJeQzp|KR}sixbf^W{@wMxTIeyRTOuSofSiyBfk@MoF[hjWWRi`hdngLYpqViBREuo~YCK\\|fxk[oxVt}k\u007Fgos_DvySHz{fMZ@WH^XZ\u007Fjg`xPz`uuagFoGPzY`BYYfgmULAj\u007Fd[CrBgFg{_gYRpTqR`}yhBW`F[lJVUHSYjKfsSIuvk_N\u007Ffd@FZbRmoX{}^ceuTxf}S|UhOVSvb_i_]IHNjsnj{i@@@D@^@@", "ea`PHFDL`MaPHtBC@apH|\\@Id{IIIIHdhfddeI@aA``PQYPqQppIqHIIUUUUUTuTl@@@C|y@HDLBJFNA@"},
			{"#q@@@@@O\u007F~|@\u007FpC\u007F\u007F~hlobDZR|xaFbrymCDcw~f{tYBEdCKETT\u007FilROXKf@rpHVRkksl\\t^Qlb_gd~jFXuprSm{ootMGgNqvS]{^{CIO`KOja~^QgJ]Uu`QR[zXDUuWUdEd_E{|xvIJ][XHZBLw~vbWJdaT~v~t[zyxaGz@iHRNiIMVhSyTDJQyhSyiFgqjiHRUcY~\u007F_BVhes_{Y[^vBDiui}s^\u007FNQ|[Ivd^YPPklF_ouVMo]i|ZjdZZwmomES{XEF~`KbDZwmSUhzdklF~EiP@E@@@@@", "do}@PlHOJH`Icd{IRnYeejzfjjjd@_f`P"},
			{"#q\u007F\u007Fx@@Coa@c^\u007F@j{A_\\b\u007F@j{Ayu]p}XfRzJap}XfR\u007F\u007FymCwQb`Rv{}eHEomJ{}eHEW{yK@hIAgXv@jUKzHDEK@hIAhgJ@jUKzc|t[ehjyuW{T\u007FS_~lCH[ehjyrhGT\u007FS_~RBubb]LFM}Ibb]LF\u007F\u007Fx|vaQq\u007F\u007F{dwFqI\u007F\u007FyJdgRQ\u007F\u007F{AeeU}Dzu_^kGQ[EI_^kGQA@G\u007Fr^sePPD~JjMaOox~JjMa^\u007F{\u007Fr^seS{yA}csjXbUVCj{IeJVZ}cTITsy~se^jG]iVCj{ILDEA}csjKLE~se^jjujZ}cTIWBVg|Ptvr@DiLPqUNRVK|^Zv[[zH|YJUu\u007FxiLPqUH}jg|PtvQmjK|^ZvDdFH|YJUCPDmuXc^l`GImf@Z\\oxmuXc^c_{Imf@Z\u007F\u007FzFqQ]Yn_{^coBU\\TTofVa^MdEd^dTfR[yd^dTfCkhofVa^a`G^coBUdEh@@Nh@e@@", "fiw`qBJpQ`eF@rL~@IsqQDQDIBPdIBPd@zHd\\dKRkF|zHTuUUUUUKUUT@C|v@`cAJ@"},
			{"#q}Le\u007F\u007FpJEMa`xFP^NuXEV^SUHCNkyEjOg_IxRBBpvFw_\\~jaa{y[GfQV|npjrwlV]ySp}ZUIf]itu`RRJBfrq\\WUaU\\TvqoQ]J@OKFl{AcpIEVar^PuEoGBjNDzOpA_wAJ^rUjQfnnNpLJ]CnbnylFYH\\wezKFUD\\cMjnKeQCJkjFSfiCCOxssDU~_c{LKFy~{QMv}bF^W|vwDl}N[jfcx}]qhoK`WoN]VCtZPTvJcJWyqbG]_gsXv`U^GhTEG@Wve}N\\|RvAwuLKlVJArbe`XrGqo[wGeog~FxxVxw}vYJvtulEJCndp{a~c@GsM^`\u007FNZcWIcgqIfoUSsaI^VzkqV\\`~ZhxnJTUJyd[[_gGEoTjVl]|v|}Tu`VUZouJg|QhaZGfYUh~eXKWxehmaNOHYen_^ioisa\\QakwI@l^A^xkyvB\\~JA^Yg}jvMDO{R\\j~AtQzIVcXuRcZ`]h`ruPv{TPY~]RVEfkR^LsEsMWWQsFwjLWTAVaih@JP@f@@", "eiPvBLLDLBJFIjV^QPCRBHIb@E`xWldddtTTTTRRRRRbvvRxLdxdDtl|RNJnZ^F~fVQqjj@@A`@@`@HAVj@H@@_fpA@a`QP"},
			{"#qDUcw]cDt[j_pbT{KArFhlEdICVqH`Wr[^MzWSr[v\\iLw_`MdGCqEyb^MuhE`SA@UX|LzFUarrWx_lv\u007FjAhZAIYVJQLz~}P@@NsFABg\u007F\u007FygO\\JrSq`LrjLvQUzXsCuElNosNUsAnjaMCCpZOznr\u007F\\OipER^k{t{|ID]W@`sGeMaWxKHCv[bk__DxZe^k[[Wjms_zAf\\QtXWh`CEbQt`F~YmnKjaWDd`URGhU_|r]nO{ySYbRJh{Vpsb@YxGEtYYYE[WHnTUUvPDDlfUmugDjOLU\u007Ff{xxKfjfzDhuQkbjI~EDKLh\u007Fi[@jrOeXFu}V^omle{OHs{aryD\u007FVMpRgy~zyts[@VrBjaP\\SZxpuLDVMFG@VfsnDUG[ggHV`jHdXqr[hZ__jGyomEX\u007FjYL^{jXd[hwa_UW[eNMhWe@`VxF_bz~@zVf^CrcLTzqUhyQgkr]YTaq@eS}ltmlsiENbWF~\u007FFiYn|Mv\u007FXlB]KRNXWubjk^PCkx@K@A``@", "eg`QFAALBJFGO@haiemck`MbBHPbMM`pHlBD`cHH@vCdNttskNVz{ldddJRCddbRRRdTg@dbaUSWUUUUUUUTlmU@@OsL@`PpHhTtLl\\|@"},
			{"#q{{dDm\u007FgopxcJSJC{iv\u007F|oZ\\DB@XT~@@@EQe`U|Znb\u007FxVCkdwnAaCvUqIM]EuaKamr@eckOeDTatx]ZpDDgGbU[Lg~YErW^ld|~gOjxS]m\\i^cINCGQH]jqdzyYQFnevmPqd{UzkqVAf]ytvq`Vcmp^SyaYbVHX{y^UFNoQBdMZIkjrbzy|XrePAiUNVbqbQXpHcLR[@~AiuT^zjycNdheBTSE\u007FTxJTuQr~pzDyNuRBekyFl}Dd_T{UZA~^[zygmHR\u007FZzB^jnYDe~CdpQZk[UP{hVSGtMh]Ieb~rNoaoNr~dpOKbqx\u007FX{OF}yIHOvfhS~R|TXY[lbwSWvPfBi[VkAOZMF`NEpddOb{QDbnTJc\u007FfEJu\\IBEhZRdaiUiVh|wZFV^xeRd{JaANrhxUKIvwIotSXM{jfx@xLNlTTBp\\d}LMF`_CGV{AjX@OH@N@@", "el\\UELNHDLBMCKGO@aiemkgo`XTPKcjHNb@EtfsgIB{[`^rRQQQIQJHkKXVRArPBQSRqpIJhijjfZBB@@UeZh@@Gyd@PHX@"},
			{"#q^ebEwmF_h^Y\u007F\u007F|Z{GhqKbTrlHbLKse]KaoLF^ZL||[{`jEuNuorDfSYKOCsJp`GbuHwgUNFMYC[rVpNB\\tOsWVAUQwIJt~i][qdDw\\YT^D^llPVMj`jTxSGrnwWnCf`MItw_BZvjtPDMw@W^cIimb^zmvjkwfdqZ{mgoxl`y^oruj^~ZB{kI]UXewheuYW~RJMtvtPHE~[{plzMjWbJyxX~NW\u007FECeVleuDV|_gSJsLjX@G@@Q@@", "fmgAhd\u007FAbgIZmzNXEbBpaXu@@VDhgGDzgL{gBzWJkWFzwN{tafLigLefl}d\\sdhbHbDbHaHaDQDQDqDeBdZjjjjk\u007F\u007Foz`@_fDDDXIQgARUhue{@"},
	};

	private static final double[][] TEST_POSITIONS_64 = {
			{-1.5, -1.5, -1.5},
			{-1.5, -1.5, -0.5},
			{-1.5, -1.5, 0.5},
			{-1.5, -1.5, 1.5},
			{-1.5, -0.5, -1.5},
			{-1.5, -0.5, -0.5},
			{-1.5, -0.5, 0.5},
			{-1.5, -0.5, 1.5},
			{-1.5, 0.5, -1.5},
			{-1.5, 0.5, -0.5},
			{-1.5, 0.5, 0.5},
			{-1.5, 0.5, 1.5},
			{-1.5, 1.5, -1.5},
			{-1.5, 1.5, -0.5},
			{-1.5, 1.5, 0.5},
			{-1.5, 1.5, 1.5},
			{-0.5, -1.5, -1.5},
			{-0.5, -1.5, -0.5},
			{-0.5, -1.5, 0.5},
			{-0.5, -1.5, 1.5},
			{-0.5, -0.5, -1.5},
			{-0.5, -0.5, -0.5},
			{-0.5, -0.5, 0.5},
			{-0.5, -0.5, 1.5},
			{-0.5, 0.5, -1.5},
			{-0.5, 0.5, -0.5},
			{-0.5, 0.5, 0.5},
			{-0.5, 0.5, 1.5},
			{-0.5, 1.5, -1.5},
			{-0.5, 1.5, -0.5},
			{-0.5, 1.5, 0.5},
			{-0.5, 1.5, 1.5},
			{0.5, -1.5, -1.5},
			{0.5, -1.5, -0.5},
			{0.5, -1.5, 0.5},
			{0.5, -1.5, 1.5},
			{0.5, -0.5, -1.5},
			{0.5, -0.5, -0.5},
			{0.5, -0.5, 0.5},
			{0.5, -0.5, 1.5},
			{0.5, 0.5, -1.5},
			{0.5, 0.5, -0.5},
			{0.5, 0.5, 0.5},
			{0.5, 0.5, 1.5},
			{0.5, 1.5, -1.5},
			{0.5, 1.5, -0.5},
			{0.5, 1.5, 0.5},
			{0.5, 1.5, 1.5},
			{1.5, -1.5, -1.5},
			{1.5, -1.5, -0.5},
			{1.5, -1.5, 0.5},
			{1.5, -1.5, 1.5},
			{1.5, -0.5, -1.5},
			{1.5, -0.5, -0.5},
			{1.5, -0.5, 0.5},
			{1.5, -0.5, 1.5},
			{1.5, 0.5, -1.5},
			{1.5, 0.5, -0.5},
			{1.5, 0.5, 0.5},
			{1.5, 0.5, 1.5},
			{1.5, 1.5, -1.5},
			{1.5, 1.5, -0.5},
			{1.5, 1.5, 0.5},
			{1.5, 1.5, 1.5},
	};

	private static final String[][] TEST_MOLECULES_64 = {
			{"#qt]SzCBTnHaCd~Wxla]D@@@ICb_KNDAh\\tecOk]f{H[cdFHZzbMi\\nCwgFvBSV[qtd[^ulgeTDziXzCl{}[V{[}ccKHS_[QnkJSpKlCmbkvjZXOv\\[[DhCr[DvAbsqSESzQKFP^IpHrdtPuBBWbml^hqvdMzE\\_[[]JsnFSrrBMU]_JQrnLZTFeEsTMp[jY{]ci[QmFUzM{E|W\\E\\BsMWGaNM`~O_Ubx]w\\VYwR`b|IqOY\\KUSvrE`Z}XkxqKGmuQQv@Sc_luERqtZmz@lrZDWYrFNLSCobYgDrl~[ubrIwjSFJtajgkGCFwuhK@HZAx`D", "fdeA`@@FCdTTRdTtTqYqQgQZjjjjfibHRTgHIJP@"},
			{"#qSOjPYWrdP|p|Rpa^NWJzjopolmeUBQnz`itm\\FxAhw\\rhtGePV]Vk_HMehVc~hz[M`mGSnnwKV]ZhDnmTRfmd{tblnQEkvAbYteUpoM_VHbozKNDgrqoLhyjOAq@\\ZhQyoO`@@ajIB@|NRj`Oc}JljJ~YhVXgQoJx^UdMIED{ZfVbCgaK`WSAaLSYgfUSCxAuyXcD^g[ju^X^|XNeUYgclZFVtBk`]nydQ\\~XQV{RaOiOQcghVeX_RZR[Y{NHEz_Nzt_@YYKxqbl\\b^UvZVMmaFjJ[WlvGFGl{Q[J~`JjafSngAkr@EhAkHD", "ffeqp@FR|ACdyKWHihhdmhhTh@ISPPPtuL@`j@"},
			{"#qQXNgB]zsIizwpgSOux_HByA~~K^Ey]}{bch}Zd}~Lx@@EVViKfIRQtn~\\AcDX~Ms[\\XkowJd|LK[dY}zZZBJsa}efqT}gfsNcj\\ntb~eqnXwo|d[[jQc_o~zo]JwGaWUs~KYQtyrZMlCVPb_N`U`RLWFzr}qBGLegyKe[xd|MZKXMR^eijkmYll^aKdcpDKfIrbIGJr{dqYAsCce}j`Lpi`FJBOzSZmbSLjJL[Y]yuUD[ig@L^bLqm^|iKUiS{Lx~ENJsd`PFk]Mfir~Z_[nMoULTNN`q]]Ihbe^W@EUr}rWQUiEiuj[TKALeH\\`aTvJPZVe\\CCU\\FwmPN\\dKMlGbXEi{Igi_gmAUIfa`xbD[dLubvA`myWcHV[ezqsoMKxHwAOUFE|J{ephU]QUjQ@CVAvXD", "ffc@`@@IrJJQIYIQJFQDxgNZmUUTmUUUTQBKd|lrFh"},
			{"#qnZ\\I}d~abLmmvvpifDtXRJBN]{jMetinUjZ^fCeOphaOKnH{_c}nLNMvN[ivTYz}[Wrmsww@vCAHBLkLi~xQxmSIJpQzdeUhNUF[]sSfta]jp_KvnRsjeUlCYpvU\\[DmEMYadK]V~PCy\\wjfs]a^GEM`@@nC`_AREvbUm|trSvlIg@L@ardjrfLYWfskvqCmeyIowmeIMkwe@bVBl`aFJymus`AZTLqJtzWKiZ]]jzwy_VkaQxZr}d{Sz}VwXoXZfrdZUNJ}OUI`qEqZb{H{jyKrKp{iZsFhbdzPdvryGw^kU`aqhybGjrKxrgVkIIF`xEV}LdN}bMkQRcFzWqNaQKZpRhf^nybupYTWYDu^ivbymhEorLManUE@{GCx_EtGUjh@\\DAbXD", "fa{@P@@LdwHhhheHdiMCDYqVoFmgkSSUUUUMUUJPTTkJJ@@"},
			{"#qjGvChcZMzVNWUNGbF}GTHVJ_ALzAXPPyu~lL]LEKUH]BJ]Icv|SBZQyyEUjiyeJJoEyDWFiW{EwjR[rOsQEtJsD}cpYIIQl~fiPry`XRrJG]Doyt{kk@rhOTIJtr@~ZxSbWpIjY^wHPEe|h\\OUZy_wTkXiSF[YrEobCZjjzUKans^vgQDsyfKfElk_sREhaPSyjWOAD\\vqt}MEdZhYSjZCWOEyTMdFhtz^RuhtllfTGFSXvmzcVFWhP}slNsVIhJN~zuQtjYfqqD@t\\QsyYrYaapVxytZlb\\VjWLcR^ia]iDOWRkz\\mj`@FnA^DD", "fde@P@@DEGHhiDeHULkrRc^BuUUUUUQDRbM@p@"},
			{"#qlnMtoVyZQonZBgHzD@yvKnISlto[`r~zpdb]KDMAJUZ}wI[_QoX]FgHZEs~KJwHbCtiiQyeZxouU^\\\\LB^n\\W\\y^JPdzBhmGtmKdaPX_K}}RcBOqUSj^dlEICvU[arfbjwX{zokZXsxQ]NsaetxjCNQcZXGZx~mwag|IEfhtw\\LPxtLdA}~Uhmlek}x]RLOl@BstTVH@]nXhVqpJ_vmYlX`~OCxMlCUn_DjonW]d]TpCeT`Hw^AWI`qVwPCO]|psvqOgaFItzY[f{RanB\\ctnDY{mDYs~F[HQHwZPltDVRW_pWCCn[K^{jI|ucEt{OSnZvMuySS^mThoB[i|^AiIzUSCz[Ee}Gm~UZjVrR}C_NMyy[HAccUG\\WLc|lr\\_aQFDRDZwQ\\]fmkq[OZ^coFXl\\_s{oT|{ki|T|KTKTcvB`YU{NXi{KkpKkil`SeXl}OA\\vuXfnYFyVCX[FVpiHFYA}KNgRcC\\^RIwCYZBu|ghZx}`aM]KAbjcJnckoZQj\\ylNt]XIQECpFFftfpneHabtSZ{rsjgmf_ALkGoSZzREBagkSOM[M\\AlNByRQYfXdc}Oq^GgvFskiRVYxlbtD~^FpE^eUnShw`YjDbOpuyCitB`JTiegbc_O[S|rMAjQEpuiQ|xmiV@LLAf@@", "el^PJ@@@DJID\\bbTbtflTbTQaRTRtfxD|RjzfV^~ZjjjjjjZjjjjjdp`dLed|WRfRr~`@"},
			{"#qskHLwD]MHIVMTEmImTai^RkMhnxEEgpnrisMT\\F|uKbEZSjFc]aR@~c`qQi}{@VudLXyzCgN@QRJUa[sIJVDT{IYdV_D[T[\\O[EBVRoePxshmZo^gUo[mfywcQ^NEBy{bsFloZcLEmDWBzxkVaBUxNDaXo``|xW]Fnh{Y`RZnMvUuXvm{MKpGvrKUxynCOol}fJS~~VJFj|u^}VikdgxAzrkkrrDazngWPp]WjuP^{nE@yZhmZHnR`\\FdOYCLsvRutXm^dENOew[MG~WK{ovYvlDvq~ojqy[sUi[[`e|\\GITVZLludA{EQISZtgSPCyZapnb]Oy_dhAljFjCcOKsm}V{[v[sUHQUzXOtT~lHFKpb_GMrZfPKBDQBcgZoB^YooX`LyhvUksOJiSNfbdadrTYIGsbUjpUzhz{s@{VvMLmRP]VgaIZc]PfSHFwoK_JCoYRmi[SQdrHmdqqM]DOj^dlpyt|\\dIiIpG|@wUu^zkdp`CyXVNBHSAejiw]lo~nsJi[tgYa|dyUOTMMurMWNnLiKotDDSSzvuibPHLAspD", "e`RPJ@@@EmkolbbdRebTJTfbqbbLw@ae`TQSVqsUUUUUUURuUMWADDRb|TTJat@@"},
			{"#qN`xa~P`GiZRgiaRCyEOFtwaCHreVqXbOJN]uN]Bz`dIY~XDWSz`Ni[i]PNNTj[JWHQ{uPPjCsygog@GI\\qo{OgGSLqZl[Hd[|FXxPl~n`JAml@aAXlY@oDEXARumqxIKat_aYYCOt^ftRT@{@MUcpT`aexLYZvf{XtMkDiSl|_V@adTftpA|NGbiN^rZtGeRm}wOEUY@LI~uz{vtXak]Q~^fpNfDJPpaVFoMhLJrB^gaLm_mwbLGIskNKi{FzcW_acr_[owie[MjKehf}jpblB^kNNIIsk~SZYj@Lpk\\oeY_RbNZ}E_zZRTAdCUhH@@`A]@D", "fluA`@@HcdTRbrbTTeaFZ\\[sTuMUUUQBQdUGH@"},
			{"!qLg_Uch`FG|h@@E@yc_s}FPnPFlsWMopSydumCD\\JIVRTbdzzaTcB{ebiCYRf@ai\\QIjjskmP}YtnLwij@i]^ZXTb}IP^ZpTXNt_cN]Jys[Bxf\\ErMlM^tkZec_^mDMY|TvBxCb}vWH_BlSVlgkyziagQKniUrdGiwayTYsTmuqIULKhrdji|aQfvWwKBYsAfEtK@QHQVn}H\\z]MwgIZy}VmrVVhf]chxbS^ILfY^NJPP^Yj{cIRK@ik@LlAR@D", "e`TPDN@HG@@aZPvdCiBzP^dOiCIIhhhddiMYdmEiEHiXYDuuT@D@U@PEUU@H@JQ@@"},
			{"#qMr\\g_K_yKXmeS\\iKwWEwLO}rTUZaspL]jt^_GO{E}D_zWGNedihyYwhBRQiZ{gXWDyTHuJ{mqltwUuZmUBLWeP_Q}BfalzcYJNqywlef]@uUzsH`fcPjGhqH|CHnk~|bzoExbSJK`vbmitsX]r]VhCyd]UuWACcBxAEtycT~J|abFbiEvXA[OmzMEW}MxFUkxGbu_E~yw\\lgFuBWRhvbwAHtNA|ceYVRg}KgfTvNR__n^QT`W~\\vhyXxn}ChWf~YG{[@yJd|qJeZA}_DtUE`v~gxdC\\TlrHqpxQfUkSLYzg`~NFxVXxvM_\\LE~S_Ulqh@@QLA\\XD", "ff}QB@BZTBPrJJIQZVJZIKLdVlDECUHP@@`y@"},
			{"#qu]Huo_z|DcjBMzCYUGiManI`m|fSEYgo}ne@nBU``fojsUCqfvvRICTdMMYS_}u@^STSZodAQD]WEiPmrrmict]RWat^ik^xHKyhCyMrC|PNh`DTCAQCoaazBMtSJ\\Q]xjb}URq{XqfSZMVrrx_UcyscnsH{^TSNXYwpAUPvTewV_qSXsSck~rMjm\\AkV{HXrGJ]]tmBkJzFNMKVm\\ybmMdR[XV\\]{sUHLA\\bUrxxoNhk{mtSht_PKeLfYixNzHXyou\\YUO`mtFdqVcHrKYGvIAem~]z{QiRTFqTSTC\\gFJRIp{nQmfVtFmtv\\zcOeNxj}R{PpqMe^IkvNwQd^Y]tiR}SOu]eearWAS@Yjf@DbA|TD", "fak@P@@H]gHhmDheDdUEDbjqkMzuTuMU@pEABQdTkJBh"},
			{"#qcY`@@DbvEkVK~DK\\IE@|XSP]@@p`aGNTgSifKjvUHUWzIw\\nZC\\WbpMRYaJiFMQVrJpXE]jjYfWxM`Kw]XH}FNPusiPuKSwgJSUhQIgJujKmLPXfPkrTgcJqX\\xtKu_cQ@`VBEOzszLU}RLzlNkaIyWkRd\\PdjNrMixPPbDY{uhop]cXOy`crfBOSxjaKQBEjx@FDAqDD", "f`yPb@DVYxDPdrmkK]NBeiqkPAEUTeVAJTU@@"},
			{"#qgYndJ\\glwwFM\\nhlUDMz\\wPY@jJICGKfWqLHj^p|GdhDa`^QoiWrVTDMXNWty^EtJ[uR]kLF^lwtWojJYXwYwckSKQPOpXCvJH|Sy\\Cv|hwGyakVX[ETDA}giOseV@@@rcgvQm|[G[pSHPOe^Be|qyQfwkF[eIF\\CbqZL|w[nMTvep{iXCEfwlomnjYHiYr[EJKpYFEd_mqVUS\\a|f|jzgvhcbzw{AP{rNHsti}oqYpaHV@]vUETe~FfYrqbIxgogYh]umx^Hf[kZyEPtMATNwtTongwhm\\Rqbufzr_FgqdlLhxYOnzjhjUwbB]YBiUJmhtXb|YHb`xxZAXIg~YieZXM[{vzIhN^@EQ_[lfRoZp^~CBMk\\@FVACDD", "fn}`a@DYj@HLxYEEDXeDdiDthyKUUXPUELPX`@"},
			{"#qo`JY@yXhbIH}~w|IZiE_|mAXiDkCSSNdk@E~KTgwbwI~KXaTq^sn{sjIzVDPrinXZfuMBvPI{QHRLciKEAGFh`@@pXUbx^MwJAQa[Kr^hGq}{K[]IRow{[ouBeZOflAgp\\KqeevXlDf|cnZwUpgr}oowdAP[sDzE]bmk[RnMhNUAtFZQykECubxwrJlbA`bZompSH}MIDlGgXn]ZyTEkIgno`XQi_\\Rh}WIsSfZ_YsuylrixQTdDf[rkzzL@pBJeHhvRcmUHewPYUXEj\\Ir@TRrbXXmz[KSj\\UIcNK^TiNpSiKzGGYit|b~Ncz{YbGSRYjM@N]AzPD", "fmw`S@ERzNXHcdQjHrJJIQQVZJIKYJI`gAsV@bFjA`bfb@@@"},
			{"#qjTiWRu}kE~KrMp}Z~fMvO\\ZGuajigq[L@U[jvpdsfPQF]Bd|{Bwd\\NCkHEfmgYdA}YkoeLZvjUs{}LXXnbH^uSvRuJpbNiwdSkcMoJddIuqcbJmApYjOYwmCyidKYy_Vf~yl}xhtt~tL}OnSZrtQvcTeGU|abTtWoLxtV~mvnLbueaWPdBUWF}zEQC@XewyubygcBUlFyJvYi}PzRPmwBfEpe}S]iJxwOGEFIS]tPXfHq]\\gzLt]Fjbx{[Aoe}UZiD]ctnc[~KMGy@bIdWSBny|dLG`ZKakFcDr@ZLSbmmrIBHUj`}ZUjyjQ@VDAPXD", "fjsa@@N@rQQQUQQQIIZBB\\hpNTejjj`@@BhDPbD@"},
			{"#q@@AagL@ExIGURCGy`yaQwZCnHkafdmtY~}`ChgAYz|apSWy^v|cxwCgi[irwc`wpjfqV[Pk{O^aapcUi]xOZWH]dUvukWddokqHdxY}P~nrCC_KKtpgxlFCESyGvcuPjGsvCgNvL^LTs\\mmWGRGZHT[_jmG|[nA`^lXH]\\iHSO{LR{IfJADS]Mii^zpWy[vn~\\epKB@Yhw[v\\sSzWm[KBsjvcBq{jggaR@jQsUlqPLGcKBxYPcO`nYu^^fM]uStAzPTmlW~@V\\sVCyAFqQ]ogeS]mt_`KirUJ\\DeYlRM]yPFfg]pcQUHzPZsx^eeFrgumUihJYEjp`@TAftD", "ffcaR@DJ]xLPQBSJ{L{LjtXgIZp@UUUUUEHxaH@"},
			{"#qDHOhCpev@@C}F_vqdJdY[ELhL`uIXapwRXuVpt{vohLLtwfZZDOrtMMluxcdMl}bMZpyfVE\\MZpZVDtbccilumORx~lVv]Y~l}dbu}oOsmJe_IMepvbVz|QclT[joIMepvbVoNghsBv}u}oOskJe`q[gLsIBe[YvT|^byCl\\SgdUhQmx[hbeumORxzlVgnPGd]]ZjddIkIa]DtbccilzfEhJvEM}bMZpyfVE\\MZpZVZZDOrtMMwhLLtwfJXuVpr{vDHOhCpevkuYfdtsWS_HvgROH_|ygqNrFzxeZkma[jpHfbQ}AYO~WubuQ[GaYwbcGjNpgIaxbNWsiR^@JMiGaF^JTZF`iPQcxxUy]GmuRjYtVEM]Ky^XgdzSLGlgROEzuVfKiBrUWJ`Jhaqq`jiRbGqv_VVmUxNhDHsx[ifJhv_uS^N|[EvLlzU}dxIs]Ejg{uLGhVYLs{cXepzyJjYtZ}MrmVfKezrBtDagP[ElxVqOPv^rngX^jH]{]rhLZmalGEjFlxRUkfy_Zon_upVxVyaeyFGZiTRndVOwQ]n|~dpA`J]kUkipGdABPD", "efUP@D@@GAEprRQUQIYQQQQII[PiYIRJZQYUS]KO_UW@A@@@A@@EuP@@@@"},
			{"#qQqR]KODyEI{pk[Ia_]]jhEsHHUAm`@@{ODrfIIM\\\\_Ag_lqIOL@LqZR[SxJlHKJX|_j[awUHFLtwCA]O|}K^[nYJfN\\ukvkbcZK_N|G[BXc[ZANjuIf_BgXspGq@DIt^f[Z[WPKOT~~LQGSjFKKYsoyG@AnvwMtTbQZ`{NYJPDoEAM@dwxp~f^eNMA`oABO{Fx}FequX[dRMLPRhlbfRMDgju@PUlmML_Qi]^ZPVHMXHRENEB[_GBCVvSHikNMlhfnc~RipVr@xdAaZfcodCSfmkmpjVEIjT@LTAbHD", "ffcib@LDeZLDU@DYIHXheThheEiJbgIUjjVZjeZ`Pt`"},
			{"#qUS]NWDxVGXX@@INQsTTOZUk@lfl\\nQdSB}EcYxGn_pYUeayWx_nBxocFU[IxXpJmr^gvII~u~GuqUQ`tltXx}hEGrN[UmnPGz]EfFSnhbtz[FxBUQfrIJAuraJChwmm@bZWCikGq|[fmaw{PuKyhtUMU~TaL\\TJ`mVUOlz]s{YFoKwV^p{FOViFjpV\\|coZcdgzHhjyPKY_oLMIAfLaxSWawGXZt[\\U{YQwXIFfdfuhgoy]mhw]]mECK[kskrCRUjd@FHAnpD", "fbu@c@JVADHbLQFQRFSQIJJUHwdyjj``X@@`R@@"},
			{"#q|vRKy_[zUOXeCvIqpRRJ_ODbi_@i]SPV^LiWPqqX|Nhlvc}PZwEsZNIyodZ_TtIWuyT~~LXZUxb]@`oBfS]kvagNWyVfblgg\\{EIfmxHsCfLmKQKRme@}oortFghzrkSjt]H^qYuGN_pmAQsQyUUqkbTMT`Bz^AhP@pfzSXnylZ{^DYip`MRAk@D", "dk}L@AdDOHhdhddZZUgVjjjZlIUFP@"},
			{"#qHqqy{~wUQ@JTOi\\kZEYf~RRU@jaVzWYoLzJ~JDCt_JzFwvytWGSfqNR\\`Rt`^p}jOMwg{KkbaIFckr`tgG{b_w\\~kjocp{S[Fu@SsHB_P|WPquccX@@OgetitZwtCLDw@DQUgeltkCHKrfipvucOSlkWEO`kW|wHDZ~Z}_iYQvBlYwuXLVBRYifsregqSr\\XfRCgigEVa_gzbhEhC`b]sgy{hgZJ\\iD{W[YZjWkR`skIBlOD`[HNMZZzpbRYF_DoTxzOsrmCb\\Ac}NktQmNiZzbPkgeCSW]LYyrvDYASZhEyt[QKZdUjHxL`cOEfLeG`~Kzj[elpWUCOrWSXlKjfmIsoGuvuZnn`\\BWnKw_fJM^mtINnRyZUkd@IrApTD", "few`a@AV\\CDTDYEEDeCDiDcDdcf\\tXm^c`HJiVhJ@baEEj@@"},
			{"#qZQ[X}jp@juUbX@cqrFk`bz~BmqdV^NXfD{xEthgdZJ[TC`@@NQYlaryYrxyjeXxH{dy~HPvJvh_{w`nSGDGV[mGJ^kGOTStH^xov`ALa^e[`juk~PFFyryuckAYv|abXXiBsU~qFg@imF}{aYxvTCB[\\froIFVuvv\\^_jeSJImWd}U~]TCi[SJkCscVXp~mQd[hTxc@yhl@GqAT`D", "fdyPa@KQeh@PpHrQQJIIQKJdhhuRsUWUr@@"},
			{"#q@@CZ|EHaVBY|DokK@_Qa]GcQbaKlXf`tHAY}LeRglrIQEH\\Mp{zxB[SpysSiCbvdtWbN^QWNs{gKU|PxHDGH|BHoie_NT[]KkIoIVkL{aB[MpjRWGf_PWC~kQkcK|xnUC|\\pCONVqgRKzIsVlo^XqbVjyxloyhGzEdUwLyxknpd\\nwjeLbTOcxnkwBnUJrCYVrd{]Yijw`GT@DhD", "f`i`B@L@kDeYfUWXKSk^fh@@@`ABD@"},
			{"#qi\\eTqukIdL\\VZ{bZRsP`q~KJlWRXkNHhR^|x[Zk[\\u^irf}G^[^sSpXgNu{ZOA~LVz\\ZzBGl`|GYd}}PCfjte~Xxcf\\tlZXuMGrpw~OLRvLZFZ~|nyyvvGX~rIj{]HHS^FkUKTeddHfBH_FYv|u^v[csjvWBjnBZidoY]^FN]bY`jYUEFs@IEw{FnoRwb^JhnV@]h]iqD}]~ptdIvg^cB]yvl|Iz@NLrOovuNGxtmC|igW[fi_a\\^fV^GLlAwZsFRPQzYKvHf[xr\\^G|y^FSr_wlj[NrXjDxu_{cF\\EmODIuJrT|X^zyVFGe]PS_~FOmOEqpcLcncPaa[zj{_RBsNK_piENHUiT@OHAZHD", "fleA@@@ILrrkoSIQgMUAPQUPaBU@@"},
			{"#qb}yYrD~yhYvGJjckVprfMId\\sPkcPeTNvGdpFgiYXpRcMvBWDpESCHBn|FHPGEiE_PqYB||rOG`RBmA]^Q~GUhBPR_cDRlvHhuR~HXtHSE`eLKRK\\ku_GLH[QermU}gby{FWl~QtdcykI__WgsNRd@@AGAnemhz@_oQev_F\\CQVM\\hv_Dq_w\\_yqlzEbbBOVASOGdzwy{jsXNcQuvAni]SnJ@APVbNlDGeCaqndwj@\\zHoKiTjNR`ui\\_RKuMOT^hnoMtGUb}_U}hL{F{W|\\`DWCMaNtFxzgCIeUsPXp[U|}aEiOSEpN]uFAMdH[bcHMTzs^^KHmxiNrKqidHSAKijVMl~~qJgNThmBW^QWjQHA~EgzWn|TXfNAzTnJYihTPKHA`@D", "feg`PBH@PkTIQrS\\kmlrrjAUGv[P@SUUAPPP`rl`@"},
			{"#qmQVp~TBdAjuhMHdtUzE~_pBFe|jOHD^a^^aMHzqesRcgmiilIgmCmOBZUjb{einlMDbpo\\kErXi{xALQLGyyRV[YpZNdSxqcHUAfiOHPnLXQtqteyyqTvLOXMt~Use{KEJtR\\LDcZp]gFKDGYRMddKNteCRyb~D[eMBN~FTdWzTTLdFfXRX|LD\\[WC@oAXtMTD[IMlLK~[P}i`\\gs|UMtYwsJNhmjm\\ybseCKvctdv[ZRfQv[j`Vu]Cpu|`oNBYZohsLU[ujI@HZAFTD", "dg|D@@MIeY]Y]tvjjjjlJJmP@"},
			{"#qpb\\qynLexPrPkIyAl~kg~vDcuwDpyP@@]|ajB]AISEMxlvkeqxmd_VTfAcq]NoEl@ijy\\_uk|[pvxTMDCqkUep`VCbfncbzO~SDFv\\uXMgEEvPEX^Vw~kceG_ZiTG]BSHUGxq\\[HXTPjnZG}kspznallU`rIvZfKwSgz|F}qIPzQ^cXwkoDWnPAHqxLJ[`]ZUWUrz_IMHQI^jbmIkDEJJUZ}qgp{^@j}Ypn\\\\diduvuU[DigXfK{JJqUNwPuYjvPDiCyofrdyYqPuQvkxZpH}L^FaOiPUFvWcj|BfmJuJfMW`BSmpWcPPCC\\DXYPxPNmEJDLeLAaTImjm]^pptQu`KRMweW[i[CQaq~lPGIBCTgeYTuYBihZaXjEnOBklBvQgXHDwCn}Uif@EBA`hD", "fakPb@LR``@QddabbbbRtRtQ]grTmVjAjBZJhiCHe@@"},
			{"#qgRl@@N|jG\\|rM{}^~oMm`ju[DlqXVCWrrarzPnwGytUqJNtlW_tRbKeuEVSST}eCmzIajKHBBraD]mH^{yU{r|xSeWSZBu_]PM|qYpI@`lRn~mhU{qtyywi|uVNSjB|fWeOgqLHsh}tuqusvsCe^]Bva}[{YT}{Lp]\\TOwRjp_ZyTEuXqiM]cYWXjPPl~XwidbeUGRzfWNurNyRMPUGQIkgV{A{yGQIAAQMb{BjeZkG]fSeLC^F|`xwqOCxQsaFjPqURZP[S^RkStbLu{WTawgyildDcc}j@@\\bAOpD", "f`iaB@MJ@DYHiheCCDf\\yIwjjjfjjbPfIR\\`@"},
			{"!q\\NarDW{i~HMsJ[ZwX~d@@Fo^`[nIqVU^avi`umH[QlH\\dcfldH^qlQJhlvT\\\\RkRvLl^KK`}V[aS~DZaT}F@VI}CY~z_]hbNy^BG@QFQkoUnL[WIM@ZqDfQuuB\\{[^FTncSlLgQSH|`INathu`pTuXhLjSdVYPEl`SN{IkwHY^dzISotk}vwekI@KxAGPD", "fa{@b@HM@DYIMEDeEEjidliNT[UbujB`jb`@hXHBXcIq@@"},
			{"!qoRp_pdAKO]\\vCMWzBJN]m~BkH]FSrdzp]aNF`}JtCw`D}WUHlNALa]{{td@@KEaA{n@OVxb[]Fnln]kWxDyJjvXDbxcsUjxlBl[Hv~bpN]mnPueQYhuQLhe]LshxjRUULLx|ob}JXcUVbhx@mhu]JWNYfC\\ThJEiD@H@@S@@", "fju`R@DPUkH@cIIeHiXbhdlhijjjY`@hPADTMP"},
			{"#qM|kTqru^\\RyWoTshM@jwHvfW\\|WTHa`HNWEzlnuO_vJnmt}NMovm^xtlZhct|`TBLdiRo\\GGADi}OYXgAv}fFVxu_Ud`J`@@QRUy_OMPB|JWm}^N_NmMgNh|tk^jwMVIVDQf]BOBnhb\\igPL|jndZwmB@wtfiNnKi`V|~jr[YXX]tVBArdKZGZTHUf|Y^l`XDWqtUMXRBY]fG`mDsDQ|CJ\\_xPyoCt~KAujoOaI]~Vd[KVQz~{dQUcL~JCTrF~]IKRPrbvNzjKxgrmnYFr~fjsCikIT{YnLpd[hs[RB{UckHzIU_hyy\\sCDFWWBlu|HeqVmg`m}^wbrjAr`SVyMVh}RThq{]irS^whBo\\J`RHKtbde{]TaUPI}kms_U\\gzF`d_gHurOZLzKYpAcFgxj]ztHdOWBmaEwgrTMPsZQPgJGERJcYXpeiRKDYkn@OdAGPD", "ek`XN@@J`DM@fielbbTTRTRrbtratJT\\lb|BjZZ`hJbjjjZbi`a\\PrVJJIP@"},
			{"#q`Depjb`Px`}NnLl}fRkF`jdl^}[ZdRbXAdkIGXgLnrTZ]ooLvWgFJks~jxIIhui\\XePdG`yXEPw[[WqANXftdoBfSelAD{svo]_qXJOH^c]KyNpDMF|V``[P|lED^XFMX|p[EtheDYGhakXpJ~A|ihRtT`vqEPMxfcnKhdmpeYpE]rtfBa^QweKt}_h^gCzRhH]vgtz|^Fv^CEs_]DVNdg}aPjbO`WZ@lV~siUc\\]vRtskSoPjip[YZZ`wMvCZkoCVif@Nw[gefd_Hgoe_uzqd__XHVMmpfDQ@YAqIshDAK[G~PtwzLTdFpo_EeV[L]j[|e@iyi[jeHSZspafYGVeSOSEF[yWi~kDmKehx`CdAkpD", "fnsPb@AAmdFIdTTRTLRbVRbkNF|gLAAUSUURUbdMOH@"},
			{"#qBApyAkOCwLxZU[UWR}R@aKuVulQMxoL]_Rt[aIdRzL[C~d~_r`QZMSWLoIgACgkTazOMuq~UrcR{Rm|@tAE{[]Sb`mIfcQYs~fK_Qf\\KmQ|vngSOu\\}kyDRVwL`RnmjIkt@ullUpvdBw\\pbgZ[[QFRppk]lnalw[~]sbHjOhdaqy^lWLiWyLfYIIgMrf`{uDwL}QcNpI]WnzEVECcCarJ^JkzPdRcCfSlmzSBEMdzleeFSpKCJA~Yv]NcZBjsevTuNu`~{zNq_~^~IYGbVSbBiW}UZ|o_NIrQSai`XBXqqMQp}ta[jbUhx@I@@Q@D", "fbmAP@@HhiNQQJQYJKQPhcKA}EjjjjZijYbDcHJFWh@"},
			{"#qsIQJPv[Zakx@zToWSIWgWmkCShQTPrHbaRa\\udENfSdD{JvDbSFDvlVWYiQlXHHXlikJS}`}CbrlpxVTPgGVBCL@@CV^ZiAzSVBssWJNIYGJUu`SQcKBhvsvrHvL_EDb|QZ_qi]hptzqC]dU@BDRAMqDmPqYJYRTyHCMruIMV]B`I|jqh}M_~cRsGZbqCU}BrlVcQegDm\\KeXZRLYYjUNTg|xShSDp@fnGGPRffSwzUUAkZecaa|kLYTQgHSnRKVakVB}MshhgSW|PeXm}HRUabJ@tUf\\HejD@B`AF@@", "fbca`@G@iEJYWUiUoENBLyrPyhHBjijhj@@"},
			{"#qXVIhIyWylQ|UtQanxiUii_CWdN@E^lsI{eFpNwwdrwVkE__wc}TYlAwZcn~qRZbA}Wz_UL@k|JhIyfrH_wwbq^lgQoEOb~f`uKL@XFV[[IUON{~smt^`fTUfjxFJFIpqnrgULPXgyGQhbmSyupkmhZhX`xKY{Ok~inr\\`r[CqfGl@ONrZZl^gtiT`uMih@l^ZQgNDrXNhmnmIdfYRfrU~dYJTBsODKuFcJ~Iyf{XPROWfJZ^aSf^XssiILMpUAWNvyOOqYj_auNAiUIbK\\gOTMjSBCW`sTHUpYZcfIGOYFZEGHwU[bzYVor]pcZCNjOmEzh@D^bo|cNyd^HSzBduiN`AJAFhD", "fdiAP@@XdjNRJQQEPkQY`jjjjjjbHcIrVP@"},
			{"#qjIW^~M{nKZh{AFoMBN_~ugEbd^`fjQXcOVdBxNyVEafVdwTX[E[{jKccZVFCbk`LNrKRiJx_}B}oxfnIb]PjONFPnuNa]KPFQycRTLsh}O`rzNcY[q~INcLEZI~HbcZHoCNmjKk[GOpu\\MM`Kc|XRRP[Y{\\{miw^G]MHb^cfHQmZLzwFeb[_@BQkW{nRk}wzJKzZUBnyNauWKajQka|cURC[dAsmgLrcung~uPrqirf`^VUwkejl]\\oZ}wGTM\\Yha@BTANPD", "fdea`@L@QSHheDeEEBcJrgAbuUUAPCBFHT@@"},
			{"#qYI{FeJec\\|dBz\\{oAch@@IjjcMEEnBT{qBdOfHe{wKttQb\\`Qd[FwYn_e@sUBKbF@dsYvIrzpaoshUd{s}AnS^rirzeAVDc{J|ZV[YTOCRYQ[\\xOwKCrcUXiVjPqPSwvU~lQIgaAtAn_Ilq~HyonHURQ~JZewPNBpQZcMFafhnSmFTNAfuT{heVxM]^niNVLIOdRBYxiX[CpVZAmeMeWAy|Y\\N_BKSct@pVS}gPq\\QR~qikYTi\\Bspykt`Gh@O@D", "f`qA`@@HddrsON}AN|uUTDAALHdiAP@"},
			{"#qD_UZvN`}{YijH@LFw{obRWH{~kaSYa@DhVwdzJw]mFuvZeCnwcJDa]M_ruZvRZPkYxRJvFEWm`b[~[vd|WTP}Ie}SbAtlI]fVz_uAEG|Jpi{iBmsmPFWs[PZRnqmYTAdUi_r~Y@bTSo~QYjbHCcwgBjP]HjQa[uOfZurHyjBSeYjGti|Hp]zAbvbXpmICV{`O^tXV}oFVsGFGe}JrvKic^PVVcwIpup`O}BUT}}YerJxvIQzsMIfSrScw@UR{bdjXtwWqCPAi\\@^TAShD", "fasQQBBFEKUV`d@rAIevQQQQQJ]QQZZGbde`bFF``ah@@@"},
			{"#qNmU[LN{g{\\kGHSawcgOkIA[rMOgD^@PKdbV@^EsSyKWOxP@@U]LAv@^ZxPjIpUA_fkLrMBdjT|IgwEHdBL\\I|vwFK^eBnFuKFqgC^FJKI~VedWO@fnWkdVJ`EgIKpPooe\\\\F\\s[FYkV_BUfPMKK[FBxpavaLjNNQwIPhsMYnLUHxTX\\mLfQ`mWUTCqwTiRiC~T}]XPCeXv`wEqf[_hBPVuKknjIbn|hlMx[]SVeoEgOM@]X~P|Z^jnVkVYBXILGVG`yF\\iWqkD@ApATDD", "fjmqABDXddDPVHeI^rJIQQJIYKEBgKA}FZhD@@J`@@"},
			{"#qtf^uP[OFMyeaI^AHkH\\aIgSyh~dMcbJsd}_tXj\\GYvNaoO^CleLVuKKRepv}ODZVD@@GYJYOogykJcmlaR{fbu]woNih]cs`Xdo{BixtSQiKXo|iTKKX}lt|Un~hjCZpfnmOSy[@pqILDpDDvTTOhfHxEFgF^PaoDsD\\XfGxOY[YvIgnkXOrdplN`JIHmbL_GFiHjqr\\tRgxx\\YokshwFog[zb\\pefcF@vQRt]tfyj@vZQFhNcgEUaHvzi|`NBc[RVouqLZTBZUryQpViKNjqmFhnNJaWIlSCl{oTSb`GMgQjv`AXAvXD", "fi{@H@@Hykr\\}rJIQRISQQFHyFgCQBUjBhbVFjjFAD@"},
			{"#qy@DGTP@@CrjGd|wjqmV^_BhrL}jI@pAmz`G^hSNDc`Dpx[uDbaQsxpo~eMVv_LXrc|cRWNMnSTdmOSow|PFYbxqxtWZiYmxxnnFvyGqmjkSAkQAEF|_|nZOaEBJ@juyaw]XPPoJK_uOyxpnEa[n@{YLTcnEJc[vpkdQ\\oTysdZhc\\]Ff{gYrehztxuHWo|Noylvo@RPzJ_AY~ACj{H{PTzRrw@AbOfhuJ}Fgp~dLwbNoRVO~HTnor^_YEh\\\\@C]yzgVczKqInNjNnWVEtLUlCRNWiTfG^pWDBUWCrRyDMnZy]Y@KL]es~scFky_XrXkGxA_X]SBTMDiS^coGa~_P]^_sZC`XA[\\FtIZb]K_Wem_^^IuzenmGbxfJ@^^\\q`WVd}X^~tO{Z~gib][DUjmRMRShT`LUAm`D", "fjuAA@B@qNNAPdIBHfQFQBQBCCAPPQUUUT@@@"},
			{"#q]\\\\IDXRaEovlVnv[VkMzs^LtAXhIoFskbpUFPEnob}g[mMgXyrzv}qGVLDUnvaJG}PP[Q|NIM`}co~V~w}xa|CxoK[IObA|BN^rz_IwNVdudAWJZoKXKkoJNoLlK|NqOWS}KlhgqTBWn@^KLcRH[{S{LzfWlDuvRorOBIGZ]}R~uyQ\\vjapxovtQ|QBpHEA^qEIiedjbe|H}E~BONLrEZcAm^@ejIAVr}FfggV^ucgnVl}ou\\LcVQYl[}HArwflPAJ{iBRiymfYi}@O|AuhD", "fb}a`@D@T{HhhhhdhhlUQfkFbGKUTCUAQRDpfKdu@@"},
			{"#qqVo]ibRlORjnIAuKiGderrmd@RNDZA]AavWxonWoh|PLCOnzy]}DZYIzK\\UkPjmH`EJySIwKEYIrKF|ka}MIqHkjsqTng\\QNMNLgSTeBhxdNbvaPRas@`F_|NvRJNVj}PnYzfbBejHjhpYIf]p`ihpPIA}CZ{DaUnwvTQeelYIJKveXkYDBhvmCuFgR@xB{|yv[^NKq}C^ZqntxfYXjtp|GGyhf]H@BngiFcv|MvagU`dfVI[yYwZZZdXQmfcz_voSZtY~fYVonmtRSfrc`KsHKtLecfHEIpoRDYBhh@V`H}[afib||Akb[xCEFnj@K|RjTZbkkbS\\s}OEw~DhKEiqYx]Aw^ygfw\\qZQTYgBbyIAzytzPjqnBdXTcJWN~^T`EY@B}dmLrQknzmDGFdrllqiY@SXAFxD", "eob\\@@@@LBLdTTRRTTVRRTtJRVT|tlbjrzVnN^Z`h@@@H@@@H@@`~h@"},
			{"#qjEl^J_WU~itcvwIfqptecLikwY\\jvQoedKJ]FQLO_gx^TqSeYzhKVzjsw~SE]SeW~EsQV\\dFr@oBsirjOIufavSV`^CI\\q_]bpjIaGi_LnY{ErY]yVZsEDIbnPaWCT[AkSqClg\\|`DOhcUoY~[fApUHVA_Iq`[~mWKrQLdA]kqsdjX[_nyLcQHjam^u^Th\\KYnZpRxdrVHLafDWhdpR\\AVLtbVUM}efD|{v^i[hz`^IzJrToIfauapZFmeoUpsWBFLxgvOnNcSwXsS\\M}mnN|r]Gi{mD]\\dwBvuFMqnNScYhb`OZALpD", "fj}Q`@DQ@kVQQJQQQIKIJlDZwlmUPMPUCPSBXcJt@@"},
			{"#qyrt@@MenV}lyMcuEyvJW{DSjaRU^Q@P^LZV[aOb^PIHuKFQjNnWevEgnrwogMlaz]FIFCKHY}UuMlrj]RWbAHN^A`WK~AOnjoWQWZHzJdJOSHpYWn_D]auzFSrzMNmindTfFKpvG|yT^e]tu@vMXhesb@UbQDVnCFRiV|{[yWVDb{ckYUJxlJScYGYiTX{nVwrCEPgyJUZcWDyKhJlPzVXWu}Sj}idyhg\\nLfGpJJtUMi@HeE]RyrVjFlH^fq]V{XWEmOXP^atbgJJUF_jyq_gWriXu]qyaYfjBnCYvTNqFzeX``QN\\nGEF\\}}JYhx@]y@kpD", "fegaA@KIAbSqLbbRrbbRbqfbRuFF]t\\@DDFUL@D@@@"},
			{"#qjjDACP@@SQukdavlzmJRvxKf}NDty[YXkZDTY_FXV}HdNwdfbjFfmVGHkHKMPtWFE}m_~le\\UteJluOdhChSSaNxJ^O\\NqhyLzdsBqNL~tHe~GFZeyfOdzVRghVb`hNIsIimq[]Mq~UzNh~rpCiRoTQvNOtOWbmUiDx`wLiFlkzksVBR|NNByaoaxqYw^R|Cf@v\\\\nZFikJbjSR[Cj[~li]HYjd@UTAo@D", "f`aaB@OBADILkmlwQd@DtuXBBh"},
			{"#qofYjMIEHLptSOIUjh}_akdn~|WljNIy]hTwrBmyxH\\~cDnwfU]dFfyirvoGfAoqRNZr@mYNmSlLQFGDzzzOgNiZTZxjMUH@JLbrZmt@plU{KvN`ZCsI^ZMXZAjYEqKsJ@wJFlEpW`A`awqz\\UpYDtrrJRPQCju}K[{FOUIItnjyDkNculP^kgp\\LiH~\\AvYYj[b^zwS@}YaVvrkvezvmtxVBscI{@voJhgu_{ziBWAthJTq@}qeBiz\\dUbt{tpr[gCyb]X^XSJsTGDZ{hAKnEN{d[iZP}jkoHoZyHFKSdeKk}WF_EayYkhy{ZTf{eiW@\\H@mxD", "flu@`@@IRYWVZ]~cMNCF@Bjjjh[@eIQ`@"},
			{"#qaCcxp`}YlkYxFZTo\\^D@@IvVk\\`FuUilKncmnfERRalyCOoidkrpjpI]esphb@uNwnTcKPKPi_YlLAEzwo[WtLkzHJS]WiZdvlyh{x}VV|vCW@~TuYoCDOi`FWiCR[haFV`cQWhHLtmWMsTrrOSzPRe_OZbEj~CSMspEjmZvev}DbUqDtW^l]hlm]h[sFbAhzKlwTsqz[kC}]t]aIDC}GtqnfxecLeVOvzdBrmNytVsBocbkIe_AXf\\UIj}@ErAK@D", "fluaA@A\\AHHdLbdtTbVTQVhKWhij`jB@@``Q@@"},
			{"#q@LXdsNu\\_Hrx\\\\Mlz_u`lCNA}GiGeGkt|yop}a^KTCvGrLZATUuHrK_~`MnXub~KbxLbdB@AGheyRlz`ICqmNGj}zQJ{TGoN[WkbhWgU[MYOZllDQ]P_KXZCnfaJRZOmeTZ{i]~{qMfNcmTqfERRyXv|bx]bPULv^dJQRNH}HHfO@NrO`QggBTPIOH~FqrAsYIPN]Z}\\siBPjtTHglWARSRg}wPvmLmXkH`vR^kusSjImx{Od^ye\\@lakohNbNi}WJIsyWH{OvRhp~wfEz{exGtXdkinqf~QCEU\\gtC[m\\VXwV^S_|E\\uY~jj}T\\L@ijMJkisGWeorPv{K\\`ZcHwWo`b@z_uJza_vbzdjJ~dLCtqhp`@DA@@D", "fmgQpBHJ@bNRu[BT]dTtRVTRbbfberWAF\\t@EUMLsUQPd`@"},
			{"#q^@M]pDySDlnY]JxBrNVclswhegydg\\xxu[_^MF[og_NYeVCwDjVRGKlCCYpj_tlEy@}qTRuH_h`kvuB@KzGxWapFjo@KNJumsym@~YOhSPJrAIWxQHm]I`zVvtobTSlIQaYVktHnf`IsIGXLn~]MdIGQOcvhaiVHkCT]c[WXsEy]BzGfxAbHv{gpTkOZozWvsJRAnFCvKVbUZ^|vkFjYHHRkwsSKDY@ZpXgnWe[InUzPvN[PD\\X`DKIs|{\\_O]kzj`f]a@WkBjPPuObElkbzP~wEC{EDXDnvdxrm{MVHMHCEXjiKxVqlgYWLuj|yYavuB~vxHQx]Bfx^YGdWjWvPftmBJzWojsgWJ~@yj`whGz]orgderfH_~OaAJscz`~r]CBNlKUUSUmanIiPu~aEz|h[hNCo{MWDmQI^twaYrLD_PudDjW}GcN{|\\g[GGfLwBvylOPZes_xM[PvjuE{TItAU\\uFOGcvhj_x~aDYC@HE\\aJkcWYY|nxynHf]jFoVF|`FKZxYBc[xarpM|RjlUx[FKcFWiyCgu_EuS]VAludFeS`ahYiz`evbMebDrKjSpngmDzlEFI[oBvejFdg[uV~vB`eU~~L}u[nusC_CLkwpGUNtCqnGebp~hn\\vtjyfgvgZmFeEPIgXSIxZqL|eg|x_IwvoJS{{{uN@ciPNHQdxCcDkJJjjM[SjtusBg|XkYFjtGWxkLTNSLKp`mHrFhIkOTplScAhB^lNsxeJyOLRfnuNUtpDyyMuN@tLibY`_nSvZGuvFh_Ls\\{HE|Y^qFgMFK}qupJtv^kGWCJJ{YCQ[YHbZXJMaKH{TeYVqgwNY`nAUcshFRDqu]jMqnh]vFxfUaEPIwayZhyXOqiAQMILxGLWLGYRQDi_jol@JF\\\\RElmtKYZhFqiKvqVnYiqKeJdnqeigk\\kQOfvhJaK`rUcnE~hdXJJCt|hV~uPdHzGlkUirJGilYzeezM^SvCZyupXZ[rCq{wa{jenxAWSguetdt_dZ^AEmVkABYcFWkIm~Ufy}FXn\\ODHF\\\\pUNytuN]fE~LjWg]ovwDOqfQpYB`Yjjvmyorn@TwelpDkoT@j\\iYvGLA}RhJiZ[rynNFPW\\VJrxTdv@qnUFBnevCPSwQ[mTjULgfMW~[XOsPYaZraHlWc\\rOXN~p_YZSyz]_ViUw]{SxThSsiDgoJmFPmDiPo|jiSphgeaE[uunHT{uxPHE_ijU`\\LCo`fzD]xBHlyu@YkM~gDmxfZZO^DFRoxMKRyRVmtqTZTu{pHkH^DUWwtwvkBv\\RJqFedVxoympzwz~_pWbX}nDowaqm^Vrly`WHfelWoYRyRJuTSokf}zVWHi[cRcQWgOg]}WorgqXFq]H{gW@Uf@VUhYVOR@_p}qRiq~sVGw_TqQTJrfXKZXIKZb`xVUTuWjhfehmeVQ[ABluuMqcEema[sv`y}U|IoQVFFeRkidoqVKT\\KSLIH[hvDX||VpLTyUn~MyhirRZBnLLjq_~AvW}{dO}x@HpfcdaXqWNYjibQ{qRN`q~Bb{xTtUAEfUvU`T}fEkH@EzAa@D", "gn^oapd@GDSyvLmUiaxABeWjLADHHcADDHaaD\\H`QDJHcQDYE@U@U@UDhhdhhikMeHhhdhhdmXlcHeDeDdXhldXmi@uEDdddQhdtNSKbjMZSAlwxcaxzWIfrmvuUUMP@AE@uU@EDPPUUTELDAPTSTuUT}uPPPT`AQl`@"},
			{"#q@@AiEF~V@QaFUVnnVqxA|EWnEjsVMGtvOVdNNzaBw@LIo\\NxsCmzPIvscJCJIV^gRkZjpDEdTKxKGgbnZWKyIGcEnRDmjs|AUTAYD]|NvU`VJ|zwbGj`QSweCeKXn}[upeU~dVpWiUVNISEUEV`vAGArC[HbqSA@puZWqnzaki}NW~quuA~YKBuUrbvo{JCqYt`pH]kgfch|wrZ[un_KM^cDqvQaFzwgFEYdAsH{FUJgQ\\NlekvmDS]Yhd@OP@h@@", "fbmiADDJ\\gL`XeLSLyEDhlhieCDbLMXJZZjZZZeh@@"},
			{"#q]r}RixQY`MZf^|E}Mv_rUdAJnrorLqlIm\\l~LplIvtZD^vJ]s~]}MbwrYtOQ]Hg}{_mAtQQKn]|@zIFjAdtQIAKGDXJ^yN|Vb]jheKfqfFGEWrv\\vW@qfEPUGmJ\\kZkpIl_qynkFhoK^jBBL\\KhizfnH]s[NEslz}[UyrqYf`q_zCGPIIFdDQhfD^A\\lsPCfz^mZCvcXqlfbJZvvZi~fuXWCE]t}RMhN@StAYXD", "fhyaa@GLMXFIHHrJIJYJJSFgEN|F@HjejJ`I@@"},
			{"#qmoESlOlyZv\\zl\\OhKkbfCQlyNf@iylGGHJWlR\\@]xLTpieO@@B@djH^TRlNgDEXYYHGFHGC[^|XMYVURm`C^XCkyWh~AMFbAj|R{]FnnhOeDeAWq[uithQWchDYuVKXX`V}@}AZClaoDmTfZ|JsTyevic|F\\NkAiciF[GKl_}@XzHQ^baf]O{Adf`z\\Q^KMUEr~xgRoFiSB\\y{[S[YQeYtvtLH|[ZlmzPn}ZVc[oDdYBOwRh~iUfDaVMp|yJHcjPgJPuvNTQP[Y[VNpg_f~_]ybGKHR]@[M@^UdWeDSiH{YCMVWNncD|EWxuWminqldco~qwfj_kx[vRy@fyXfogvmjfpRSGD^JpVZXppn`ZqMGANvUj`]\\Qi}@\\PAy|D", "fe{QP@L\\@`uayEDdhheDeLleboYkujj`f@HBJBED@"},
			{"#qiLkx_W_HN]kG^tAi~k[TbxGzGtkpTSGK_wzHTUGrIkZLb{GuvLuT^NgrmXvafgWz\\nnV]s[QibnDenmCAlsK[mNLiSRdqwiZ{[@G_V`GGCvW~zmhFJJMM`Dth{ymPx{tFDNfP{tA_v`]`rLh}yfmdcqTr|ZYrGFke~]VLIYPTAkrY]VmAC_~LaVwN}yshYRsFaHUkKMrIpgaK^S}rOkPlKueyhzDr}IkjOYQSiVZrIEu]\\E|mtore_zlUppnb|ijf{SCKBnmYi`@\\JAXxD", "fbmAB@M@bLbbRrTTTLrUFF\\gL@Dtu@@QATar@"},
			{"#qL^QQPXZrinKUzAARYyqHevcvJPQliVwl[tlAk@u|hIBRQoFeq{{xFP@@IcWgjxCtFzRGGUOClqfh{[{pDOH~uTR[@H^ftlmBHJqeUBk]txl|ElCTXQWniQZXJPzJb~fT[bmDmDvNgxLO`nxOPMQPV`DohosMGynZ`XXRbV}qgeJ@wMUaZNJ{b]UWojNkLPn[eN@FZufieKTOUgvybK\\RWBRl@jpUZMRtVpQU`CIbVWfK]DuG\\j}zoLRUgIMY_gMTgVbtjjUsHZh_RvjH]EQDammIjykk^[bLYwcTcknPSUdol}|ZyiT@OHAC@D", "fjsQ@@B^@dsLrzZlepRc^CdkUUTuP@TpQDpaRR@@"},
			{"#qQoOT\\cOUa~]WPZOr@LGf@`BWeBnIBkZsTqQjTY|S|T~[DezahxNBawXmutSsw^JfsNz`TagO{_rLS[OGHfjPPHRNx`s~_[QK_xfqrbEWYRSYxpBNnqkllnjw@tPrSiqTg^D~qEy[A{NXiKOU`sTL]lkIWQYL\\xkrqYuGUA{BPD}PhRTF\\Te^@ynHIXYUOMbQVD_m|SaFGXlVEzPUFuCibJZAkolAwi]kbtUaQ^VquE}y[sogxbvq|mrjDWdPPThsIsz_cEafkiOoIdFYASEZWQ[bXnDXv_HBHCNf_cNEMpLbr`VfLbrg[nmzWtOTfh}qpqzRE[D|zNxycGaArgqKeQ\\qt]hA}UIb`^NqlSpUakEVQoocQTKAC]fnXRZiFhE}pHt_kDJgotzJkmvFmwp\\]D]aipdkaVjcgpts`rWkfBt{XN@rMCdV@uSx[`kOgqZwozZ~uCxK{sZmqGascmknZ^jMi|XoenbX~[NzitQKNG\\CICEjp@JlApXD", "e`ZPB@@@DANlbbTRftTTTRRTRZrqCBUQRwRptussTuUPUPKTUUQUHPiEiCEDddLid|hBD"},
			{"#qE|EsTabu|_Gkq~GkMu[vHH@MkfCvYTZX_~rokqXkydPs~HLMuFYGvrlan|zCowl~jcybX]DFT|XKm|tdE@k~UDsbDwLgrqr`Nao{OAV^UzHQwKwti\\qsgYS@}qs`jt]XcXVU^}]X`XVU^}]UzHQwKc`yZGFXRp\\rQ\\SlMuN{xgDAbIKOFsgdMNfFgTpemyuDMxpA]V}{[_EzRleDK@pWrN\\gbHyFbitygSBgrrMZYdll\\ITOzZ`ub|DwODubg^O\\cXcqNhWeR}FvjA[oZmILmowJ\\eqUYasC\\k\\NbcyHpwJ]ARqkbViUZuh_PG@AapD", "fbeaaBAHqxDQ@HApipJREJKIQQH|DgJuUU@pDHYNH@"},
			{"#qcMlCAg_ZGRmbS`P}GQFJqc@`EakTHDmvBQ|nRRajjxMjK@kCuoFjEml@hzA`MeiEUDDJuiz`hj|VvUAJJS\\bQ^GRLOzyC`@@v\\xwMot@fY]Bhi^M}A]zAWLryk_KIZPROpwp_@sTiRuaI~qyNB\\hQUORn|]qaYCRklt~qqyyCCITsXtjAbvzte]e]afa\\|XIaOwGlVKN][o]GvNcnHwMXjjZYZgSE]VlOIy[_rZsa{nd_AKyIuGtlUEbIErr}xqg`ya}FQvMxHomTkz|VLyzVZZihH^}]MFDbD[BZAKM^x`mLknhsj\\wNVV}_ij]\\J]^pTObeMbmlWoHzziZBNdz\\ZoyiK@ZhAmHD", "ffcPPBHR@aghRcdfyi}Y~UdjwbuZjjjj`@hIaDRei@@"},
			{"#qwfuPsCjRLuWOhRZFVHvTGMJFddIRKIbCSorRxAq]|_}@`ZrwapROBqGrOaXNUeDiD}byfRJEo_HycoJAdcHFd{B~lO`FAb]Q`r_b@IAqinyR}eqPnaqeiwmlE]t~^MsElQ^YfPrNSGkxWkoTgvGBF~etykUdzrwWGaQne|KdCwvd^y_KMh@@@JVFuaRZ~`ncX{^d~pURDym{GMJfY[tW]PCLThiD|Qi@|UytRwtnwDx{GmjIvl{RVQ`VJvbfTMVSd_Tdc|S|W{OGMJSoQ^bV{zbKMK]jDF[IuOCUOrSkefjij\\fAICLmU|\\LCJQMBP[Zlszf@jMWL_`mIxHmJdPuLhOsha|ajJHJ}aU}OdwBpuQp^MqlkS^h|ijwnDlysILIcVC~wi\\wmctbKeVb}]qvXKDMkI]|yie`NtAHhD", "fi{Q`@FZAENQQQRIZIYFJXrDXivkMATEQSUUQHqNP@"},
			{"#qv[@G`|d[W|[vto`O~RJqirJjLQ\\K^^CHTPSkZhcYLKlYzdjIiKLVg|_mJQFq[aKE|o^W`VGawaUyxuI\\xKQXMxn}OSbYEvDWpP\\Qx[pBHuxou{ubz}[LCXC`hKJL}jkYzoqWZcpXFFNEtUWCgGTy|BdZiUzTWZjbtGZqtt|PtqN^HXeOlCtHqawgR}PoUaBrYqhJWIJY_tGwKQtjn]ZoNu~iimNQxxjtOiuRJqtl|MF}ZVkCzrbykF@MbA_tD", "fluPb@ANeXDQdTRTrdVTLUFBTEL@EUSMQLNP@"},
			{"#q_eJS`hgmvTcY]aFvEjWRwIrGzZkkQFlCyEss^ZA{lgcooJUyea~Zeebi\\JZIa@TLMdWNx@`N@mrfYLQmW~zfziG`g~ToAHfUhFAHF_kA}ImBcSmldPD@@Ep~k@~[ZkOAAVZWDaLzj|CVmcIBs`@BlKm_dna{Q`RnkWle@kmZsZEeJoCE]rwZVDUnQDNKGXEASWC]SvrHxfjZ_tsGNhgnVg|zzLR~oDfGKMP{ivY`kEBg}uVNqBfRIFFEINBkOyObYsTYDlSty|p|[UvIi}`miEb\\OBIm\\eTDEjyk~LBjs`ZQvnhQuT^SEJbaF|sL[frTwn}YAj]hfXhXCsrYinte@uUZvb\\iTyOyiW`IRA{@D", "fnk@P@@HhWHheHbdhhmDhRLhsIZmZjjijfiZhfHQLViCR@"},
			{"#qlTVi[^CkHfDXu]~Hms_H[}OFUkcwvxQvuOxNkPwoJO`klQPJgLRAaKW|CLbTQ\\X}WaPMQVyrTlYDoVxUvWzae^nUKsaF[qlBtg\\vkVuam{h}VktyVep^|znbcBPBWTtXIQFpcBuwNCNouGMiwATZhc|~UQEkq}Dao^~ZLwK_oAxIHhQkmBE\\k`\\QBSxRgouKt~NiuNUTmMcu{^xjT}fxv{YaiCg][Rr}_QvVm@tYQatUGvVoULKnOX]^dImXnXXYMf_phX^f\\BcMy{lejce{pMKYEqGGz]GD]|exRAhDL~UzzQjwtPpJ\\GRAqn]LWYRcQrUj\\`JzAaXD", "fb}@P@@DmyIfeYm}{baZL{pVjjjijjjhbTSHJZP@"},
			{"#qLlhueTqTCeapql~zeAtEfx||mMK|QQMHY{pqoQcLBo\\jn\\^I~XUh[uWv@@CYKglqzcWDSJWxnpwsNvDR[@t]f|SbQHHua\\eh_Qhs`IgV\\rvVGIIQhejmXuDmWYZ`cMaOHnSTbvh_[W}CQqoShPjZn\\TPoFDYj^nG[ySNJIPJkxuXQrS]M\\R[{Q^Pgnl{avgNfvBkIBL@rA{gPqZlev\\vmnioQUGoWaRbUCQceCT@UqoXMZEihB`C\\AX@D", "foAQ@@D\\@drsR}tyisTEPTQ@b@@"},
			{"#q_cOKrZiOKXFE\\GF~LaIcOGnrlnQbl[mIKsuMcsueLMucY{r{[wrKWRpMmrKX^nGle``Uf_aNa@RTSErF^[Jnx@hFVKJOd_ol^pj`VQP_pXlZVXTGNGHyd[IWRCTiraOZ@OG`ZIS[BoAsjRKsDv_oEZ_F[p~dUFL`QlIfG_Ek`ywUC_hXdeZMPgx}SVXm~WIUjMrGPJGKabjFWtCxQduyhEk\\hxbXPMdfauvgek`@^EABpD", "f`iQB@F\\T@HRe^UgTeIZ]zjji`B@@@"}
	};

	private static final String TEST_MOL_FOR_CONFORMERS = "ffm`r@AXT{rR`BLdTVbbbRbVaahttEPSPs@@@";
	//	private static final String TEST_MOL_FOR_CONFORMERS = "fleab@LJuD@PeLvwKnyFV}Ts@pQU@@@"; seed 24
//	private static final String TEST_MOL_FOR_CONFORMERS = "fikQPBHQ@`Q`peGYEMDeMeDeBeDerWQm@DUUMP@@@@";
//	private static final String TEST_MOL_FOR_CONFORMERS = "e`TZFH@NAEGFiJemgo`@cIEEEELjeLeMBhhbiPHhXdUSUSSPACUMSTHHSi@@";

	private static final String CAPIVERSATIB_FROM_4GV1 = "eghVDH@AOBfmccG@HcHhdlhdhmHTlheDePqdUMl]]@AUSUUT@QIEAq@@ #qAwJRVFEHdEA^xVuJEHmay@CjsMh@mL_dHMibmCedKPmGYAtjVbhByNAxjJbB_L@MC{ieHzZPSyDjIIDk?]iQuKQh_J]{Qo[ePqCXhjHuLD]UsHLz}^pWj`nhjZp|qqVcVEfBrF_KDWw~aYd_cOUMiW??CQ{iLFJC`{czF`?ccBGehHNyVEcWpw@ektRfDek}mfPxXbzCbvk\\Gyqn~GanPzEeWvSp|}HyXT`plWf}`TIShrFEqmmiwfze?|PMpT?ZTOvrwJczU[NCwNiVSySrZ]QFlLn?GTpFK{RE?X{IglY_WTRtVOPjpheybrYiTcc\\EWYDdx?bloSjvmeyEocnVjDioq{fZrWBSYNoPlKJMvR[DcFqAEKr\\xauqt_\\~__Vg_n~uQ^ngZFXUJr}abGD~F[IcrvvP~Zrn_SGcmQu^RiuFuzaXfkc@V@@[`D";

	private static final double SURFACE_BRIGHTNESS = 0.55;
	private static final double SURFACE_SATURATION = 0.15;   // must be less than 1.0 - SURFACE_BRIGHTNESS

	private final int mMode;
	private final String mPDBEntryCode, mPDBFile;
	private final boolean mCropLigand;

	public StartOptions(int mode, String pdbEntryCode, String pdbFile, boolean cropLigand) {
		mMode = mode;
		mPDBEntryCode = pdbEntryCode;
		mPDBFile = pdbFile;
		mCropLigand = cropLigand;
		}

	public int getMode() {
		return mMode;
		}

	public String getPDBEntryCode() {
		return mPDBEntryCode;
		}

	public boolean geCropLigand() {
		return mCropLigand;
		}

	public void initializeScene(V3DScene scene) {
		scene.clearAll();

		if (mMode == MODE_PDB_ENTRY)
			loadPDBEntry(scene);
		else if (mMode == MODE_SMALL_MOLECULES)
			testMolecules(scene);
		else if (mMode == MODE_SMALL_MOLECULE_SURFACES)
			testSurfaces(scene);
		else if (mMode == MODE_SMALL_FRAGMENTS)
			testFragments(scene);
		else if (mMode == MODE_METAL_ORGANICS)
			testOrganoMetallics(scene);
		else if (mMode == MODE_SMALL_COMFORMERS)
			testConformers(scene);
		else if (mMode == MODE_SIMPLE)
			testSimple(scene);
		else if (mMode == MODE_PROTEIN)
			testProtein(scene);
		else if (mMode == MODE_VOXEL_DATA)
			testVoxelData(scene);
	}

	private void loadPDBEntry(V3DScene scene) {
		try {
			PDBFileParser parser = new PDBFileParser();
			PDBCoordEntryFile entryFile = (mPDBFile != null) ?
					parser.parse(new File(mPDBFile))
					: (!mPDBEntryCode.isEmpty()) ? parser.getFromPDB(mPDBEntryCode) : null;

			if (entryFile == null) {
				scene.showMessage("Unexpectedly didn't get PDB entry.");
				return;
			}

			Map<String, List<Molecule3D>> map = entryFile.extractMols(false);
			List<Molecule3D> ligands = map.get(StructureAssembler.LIGAND_GROUP);
			if (ligands == null || ligands.isEmpty()) {
				map = entryFile.extractMols(true);
				ligands = map.get(StructureAssembler.LIGAND_GROUP);
				if (ligands != null && !ligands.isEmpty())
					scene.showMessage("Only covalent ligand(s) were found and disconnected from the protein structure.");
			}

			List<Molecule3D> proteins = map.get(StructureAssembler.PROTEIN_GROUP);
			if (proteins == null || proteins.isEmpty()) {
				scene.showMessage("No proteins found in file.");
				return;
			}

			Molecule3D ligand = null;

			if (ligands != null && !ligands.isEmpty()) {
				int index = -1;
				if (ligands.size() == 1) {
					index = 0;
				}
				else {
					String[] ligandName = new String[ligands.size()];
					for (int i=0; i<ligands.size(); i++) {
						String formula = new MolecularFormula(ligands.get(i)).getFormula();
						ligandName[i] = (i + 1) + ": " + formula + "; " + (ligands.get(i).getName() == null ? "Unnamed" : ligands.get(i).getName());
					}

					ChoiceDialog<String> dialog = new ChoiceDialog<>(ligandName[0], ligandName);
					dialog.titleProperty().set("Select one of multiple ligands:");
					dialog.showAndWait();
					String name = dialog.getSelectedItem();
					if (name != null)
						index = Integer.parseInt(name.substring(0, name.indexOf(':')))-1;
				}

				if ((index != -1))
					ligand = ligands.get(index);
			}

//					mMoleculePanel.setShowStructure(false);
//				MMTFParser.centerMolecules(mol);

			Color[] surfaceColor = new Color[proteins.size()];
			double inc = 2.0 * Math.PI / proteins.size();
			double shift = 2.0 * Math.PI / 3.0;
			for (int i=0; i<proteins.size(); i++) {
				double start = inc * i;
				surfaceColor[i] = new Color(SURFACE_BRIGHTNESS + SURFACE_SATURATION * Math.sin(start),
						SURFACE_BRIGHTNESS + SURFACE_SATURATION * Math.sin(start+shift),
						SURFACE_BRIGHTNESS + SURFACE_SATURATION * Math.sin(start+2*shift), 1.0);
			}

			V3DRotatableGroup complex = new V3DRotatableGroup(mPDBEntryCode);
			System.out.println(mPDBEntryCode);
			scene.addGroup(complex);

			for (int i=0; i<proteins.size(); i++) {
				V3DMolecule vm = new V3DMolecule(proteins.get(i),
						MoleculeArchitect.ConstructionMode.WIRES,
						MoleculeArchitect.HYDROGEN_MODE_DEFAULT,
						V3DMolecule.SurfaceMode.FILLED,
						SurfaceMesh.SURFACE_COLOR_DONORS_ACCEPTORS,
						surfaceColor[i], 0.5,
						V3DMolecule.getNextID(),
						V3DMolecule.MoleculeRole.MACROMOLECULE,
						true);
				vm.getMolecule().setName("Protein");
				scene.addMolecule(vm, complex);
			}

			V3DMolecule v3dligand = null;
			if (ligand != null) {
				v3dligand = new V3DMolecule(ligand,
						MoleculeArchitect.ConstructionMode.STICKS,
						MoleculeArchitect.HydrogenMode.ALL,
						V3DMolecule.getNextID(),
						V3DMolecule.MoleculeRole.LIGAND,
						true);
				v3dligand.getMolecule().setName("Ligand");
				scene.addMolecule(v3dligand, complex);
			}

			List<Molecule3D> solvents = map.get(StructureAssembler.SOLVENT_GROUP);
			for (Molecule3D mol : solvents) {
				V3DMolecule vm = new V3DMolecule(mol,
						MoleculeArchitect.ConstructionMode.STICKS,
						MoleculeArchitect.HydrogenMode.ALL,
						V3DMolecule.getNextID(),
						V3DMolecule.MoleculeRole.SOLVENT,
						true);
				mol.setName((mol.getAllAtoms()==1 && mol.getAtomicNo(0)==8 ? "Water " : "Solvent ")+mol.getAtomChainId(0));
				scene.addMolecule(vm, complex);
			}

			if (v3dligand != null && mCropLigand) {
				scene.crop(v3dligand, 10.0);
				scene.optimizeView();
			}
		} catch (FileNotFoundException fnfe) {
			scene.showMessage("File not found: "+fnfe.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void testMolecules(V3DScene scene) {
		for (int i = 0; i < TEST_POSITIONS_13.length; i++) {
			double[] coord = TEST_POSITIONS_13[i];
			String[] code = TEST_MOLECULES_13[i];

			double dx = POSITION_FACTOR * coord[0];
			double dy = POSITION_FACTOR * coord[1];
			double dz = POSITION_FACTOR * coord[2];

			StereoMolecule mol = new IDCodeParser(false).getCompactMolecule(code[1], code[0]);
			mol.center();
			mol.translate(dx, dy, dz);
			V3DMolecule vm = new V3DMolecule(mol, V3DMolecule.getNextID(),V3DMolecule.MoleculeRole.LIGAND);
			scene.addMolecule(vm);
		}
	}

	private void testFragments(V3DScene scene) {
		for (int i = 0; i < TEST_POSITIONS_16.length; i++) {
			double[] coord = TEST_POSITIONS_16[i];
			String[] code = TEST_FRAGMENTS_16[i];

			double dx = FRAGMENT_POSITION_FACTOR * coord[0];
			double dy = FRAGMENT_POSITION_FACTOR * coord[1];
			double dz = FRAGMENT_POSITION_FACTOR * coord[2];

			StereoMolecule mol = new IDCodeParser(false).getCompactMolecule(code[1], code[0]);
			mol.center();
			mol.translate(dx, dy, dz);
			V3DMolecule vm = new V3DMolecule(mol, V3DMolecule.getNextID(), V3DMolecule.MoleculeRole.LIGAND);
			scene.addMolecule(vm);
		}
	}

	private void testOrganoMetallics(V3DScene scene) {
		for (int i = 0; i < TEST_POSITIONS_16.length; i++) {
			double[] coord = TEST_POSITIONS_16[i];
			String[] code = TEST_ORGANO_METALLICS_16[i];

			double dx = ORGANO_METALLICS_POSITION_FACTOR * coord[0];
			double dy = ORGANO_METALLICS_POSITION_FACTOR * coord[1];
			double dz = ORGANO_METALLICS_POSITION_FACTOR * coord[2];

			StereoMolecule mol = new IDCodeParser(false).getCompactMolecule(code[1], code[0]);
			mol.center();
			mol.translate(dx, dy, dz);
			V3DMolecule vm = new V3DMolecule(mol, V3DMolecule.getNextID(),V3DMolecule.MoleculeRole.LIGAND);
			scene.addMolecule(vm);
		}
	}

	private void testSurfaces(V3DScene scene) {
		int count = 8;
		for (int i = 0; i < count*count; i++) {
			float shift = ((float)count-1)/2;
			double dx = 8.0 * (i/count - shift);
			double dy = 8.0 * (i%count - shift);
			double dz = 0.0;

			String[] code = TEST_MOLECULES_64[i];

			StereoMolecule mol = new IDCodeParser(false).getCompactMolecule(code[1], code[0]);
			mol.center();
			mol.translate(dx, dy, dz);
			V3DMolecule vm = new V3DMolecule(mol, V3DMolecule.getNextID(),V3DMolecule.MoleculeRole.LIGAND);
			double transparency = 0.20 + 0.1 * (i % 7);
//			vm.setMode(MoleculeArchitect.ConstructionMode.BALL_AND_STICKS, MoleculeArchitect.HYDROGEN_MODE_DEFAULT);
			vm.setSurface(0, V3DMolecule.SurfaceMode.FILLED, SurfaceMesh.SURFACE_COLOR_ATOMIC_NOS, transparency);

//			vm.activateEvents();
			scene.addMolecule(vm);
		}
	}

	private void testConformers(V3DScene scene) {
		StereoMolecule mol = new IDCodeParser(false).getCompactMolecule(TEST_MOL_FOR_CONFORMERS);

/*			if (TEST_MOL_FOR_CONFORMERS != null) {
				ConformerGenerator cg = new ConformerGenerator(1467967297811L, false);
				cg.initializeConformers(mol, ConformerGenerator.STRATEGY_LIKELY_RANDOM, 10000, false);
				int count = 0;
				while (cg.getNextConformer() != null)
					count++;
				System.out.println("Generated " + count + " conformers.");
				}*/


/*			for (int i=0; i<1000; i++) {
				ConformerGenerator cg = new ConformerGenerator(i, false);
				cg.initializeConformers(mol, ConformerGenerator.STRATEGY_LIKELY_RANDOM, 10000, false);
				int count = 0;
				while (cg.getNextConformer() != null)
					count ++;
				if (count == 0)
					System.out.println("Generated "+count+" conformers ("+i+").");
				else
				System.out.print(".");
				if (i % 80 == 79)
					System.out.println(" "+i);
				}*/

//			ConformerGenerator cg = new ConformerGenerator(1, false);
		ConformerGenerator cg = new ConformerGenerator(1467967297811L, false);
		cg.initializeConformers(mol, ConformerGenerator.STRATEGY_LIKELY_RANDOM, 10000, false);
		double hue = 0.0;
		int count = 0;
		Coordinates refCoord = null;
		int REF_ATOM = 5;
		while (true) {
			Conformer conformer = cg.getNextConformer();
			if (conformer == null)
				break;

			double dx = 0.0;
			double dy = 0.0;
			double dz = 0.0;
			if (count == 0) {
				conformer.center();
				refCoord = conformer.getCoordinates(REF_ATOM);
			}
			else {
				Coordinates coord = conformer.getCoordinates(REF_ATOM);
				dx = refCoord.x - coord.x;
				dy = refCoord.y - coord.y;
				dz = refCoord.z - coord.z;
				conformer.translate(dx, dy, dz);
			}

			V3DMolecule vm = new V3DMolecule(conformer.toMolecule(null), V3DMolecule.getNextID(),V3DMolecule.MoleculeRole.LIGAND);
			vm.setConstructionMode(MoleculeArchitect.ConstructionMode.WIRES);
			vm.setColor(Color.hsb(hue, 1.0, 0.5));
//			vm.activateEvents();
			scene.addMolecule(vm);
			hue += 37;
			count ++;
			if (count == 100)
				break;
		}
		System.out.println("Generated "+count+" conformers.");
	}

	private void testSimple(V3DScene scene) {
		StereoMolecule mol = new IDCodeParser(false).getCompactMolecule(CAPIVERSATIB_FROM_4GV1);
		mol = new ConformerGenerator(1467967297811L, false).getOneConformerAsMolecule(mol);
		V3DMolecule vm = new V3DMolecule(mol, V3DMolecule.getNextID(),V3DMolecule.MoleculeRole.LIGAND);
		vm.setConstructionMode(MoleculeArchitect.ConstructionMode.BALL_AND_STICKS);
		scene.addMolecule(vm);
	}

	private void testProteinFromMMTF(V3DScene scene) {
		String path = HOME_PATH + "data/pdb/";
		try {
			long millis = System.currentTimeMillis();
			System.out.print("downloading and parsing protein...  ");

//				Molecule3D[] mol = MMTFParser.getStructureFromFile(path, "1CRN", MMTFParser.MODE_SPLIT_CHAINS);
//				Molecule3D[] mol = MMTFParser.getStructureFromFile(path, "5OM7", MMTFParser.MODE_SPLIT_CHAINS);
//				Molecule3D[] mol = MMTFParser.getStructureFromFile(path, "1M19", MMTFParser.MODE_SPLIT_CHAINS);
			Molecule3D[] mols = MMTFParser.getStructureFromFile(path, "2I4Q", MMTFParser.MODE_SPLIT_CHAINS);
//				Molecule3D[] mol = MMTFParser.getStructureFromFile(path, "1JJ2", MMTFParser.MODE_SPLIT_CHAINS);

			MMTFParser.centerMolecules(mols);
			for (Molecule3D mol : mols) {
				millis = printDelay(millis);
				System.out.print("Creating V3DMolecule...  ");

				V3DMolecule vm = new V3DMolecule(mol, MoleculeArchitect.ConstructionMode.STICKS,
						V3DMolecule.getNextID(), V3DMolecule.MoleculeRole.LIGAND);
				//				V3DMolecule vm = new V3DMolecule(conformer, -1, MoleculeArchitect.HYDROGEN_MODE_DEFAULT,
				//						V3DMolecule.SURFACE_FILLED, SurfaceMesh.SURFACE_COLOR_ATOMIC_NOS, 0.5);

				millis = printDelay(millis);
				System.out.print("adding molecule to scene...  ");

				scene.addMolecule(vm);
				millis = printDelay(millis);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private long printDelay(long startMillis) {
		long millis = System.currentTimeMillis();
		System.out.println(Long.toString(millis-startMillis));
		return millis;
	}

	private void testVoxelData(V3DScene scene) {
		SurfaceMesh mesh = new SurfaceMesh();
		scene.getWorld().getChildren().add(new MeshView(mesh));
	}
}

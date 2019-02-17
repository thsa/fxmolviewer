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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.stage.Stage;
import org.openmolecules.fx.viewer3d.*;

import java.util.Optional;

public class ViewerApp extends Application {
	private static final String HOME_PATH = com.actelion.research.util.Platform.isLinux() ? "/home/thomas/" : "/Users/thomas/Documents/";

	private static final int TEST_MOLECULE_MIN = 0;    // between 0 and TEST_MOLECULE_MAX
	private static final int TEST_MOLECULE_MAX = 63;    // 63 or less

	// Set one of the following flags to true in order to use a test mode instead of showing the PDB dialog to load a protein
	private static final boolean TEST_MOLECULES = false;
	private static final boolean TEST_PROTEIN = false;
	private static final boolean TEST_CONFORMERS = false;
	private static final boolean TEST_MOLECULE_SURFACES = false;
	private static final boolean TEST_MRI_VOXEL_DATA = false;

	private static final boolean ASK_FOR_PDB_ENTRY = !TEST_MOLECULES && !TEST_PROTEIN && !TEST_CONFORMERS && !TEST_MOLECULE_SURFACES && !TEST_MRI_VOXEL_DATA;

	private static final double POSITION_FACTOR = 16;
/*	private static final double[][] IKOSAEDER = {
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
	private static final String[][] TEST_MOLECULE = {
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
*/

	private static final double[][] IKOSAEDER = {
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

	private static final String[][] TEST_MOLECULE = {
			{"#qt]SzCBTnHaCd~Wxla]D@@@ICb_KNDAh\\tecOk]f{H[cdFHZzbMi\\nCwgFvBSV[qtd[^ulgeTDziXzCl{}[V{[}ccKHS_[QnkJSpKlCmbkvjZXOv\\[[DhCr[DvAbsqSESzQKFP^IpHrdtPuBBWbml^hqvdMzE\\_[[]JsnFSrrBMU]_JQrnLZTFeEsTMp[jY{]ci[QmFUzM{E|W\\E\\BsMWGaNM`~O_Ubx]w\\VYwR`b|IqOY\\KUSvrE`Z}XkxqKGmuQQv@Sc_luERqtZmz@lrZDWYrFNLSCobYgDrl~[ubrIwjSFJtajgkGCFwuhK@HZAx`D", "fdeA`@@FCdTTRdTtTqYqQgQZjjjjfibHRTgHIJP@"},
			{"#qSOjPYWrdP|p|Rpa^NWJzjopolmeUBQnz`itm\\FxAhw\\rhtGePV]Vk_HMehVc~hz[M`mGSnnwKV]ZhDnmTRfmd{tblnQEkvAbYteUpoM_VHbozKNDgrqoLhyjOAq@\\ZhQyoO`@@ajIB@|NRj`Oc}JljJ~YhVXgQoJx^UdMIED{ZfVbCgaK`WSAaLSYgfUSCxAuyXcD^g[ju^X^|XNeUYgclZFVtBk`]nydQ\\~XQV{RaOiOQcghVeX_RZR[Y{NHEz_Nzt_@YYKxqbl\\b^UvZVMmaFjJ[WlvGFGl{Q[J~`JjafSngAkr@EhAkHD", "ffeqp@FR|ACdyKWHihhdmhhTh@ISPPPtuL@`j@"},
			{"#qQXNgB]zsIizwpgSOux_HByA~~K^Ey]}{bch}Zd}~Lx@@EVViKfIRQtn~\\AcDX~Ms[\\XkowJd|LK[dY}zZZBJsa}efqT}gfsNcj\\ntb~eqnXwo|d[[jQc_o~zo]JwGaWUs~KYQtyrZMlCVPb_N`U`RLWFzr}qBGLegyKe[xd|MZKXMR^eijkmYll^aKdcpDKfIrbIGJr{dqYAsCce}j`Lpi`FJBOzSZmbSLjJL[Y]yuUD[ig@L^bLqm^|iKUiS{Lx~ENJsd`PFk]Mfir~Z_[nMoULTNN`q]]Ihbe^W@EUr}rWQUiEiuj[TKALeH\\`aTvJPZVe\\CCU\\FwmPN\\dKMlGbXEi{Igi_gmAUIfa`xbD[dLubvA`myWcHV[ezqsoMKxHwAOUFE|J{ephU]QUjQ@CVAvXD", "ffc@`@@IrJJQIYIQJFQDxgNZmUUTmUUUTQBKd|lrFh"},
			{"#qnZ\\I}d~abLmmvvpifDtXRJBN]{jMetinUjZ^fCeOphaOKnH{_c}nLNMvN[ivTYz}[Wrmsww@vCAHBLkLi~xQxmSIJpQzdeUhNUF[]sSfta]jp_KvnRsjeUlCYpvU\\[DmEMYadK]V~PCy\\wjfs]a^GEM`@@nC`_AREvbUm|trSvlIg@L@ardjrfLYWfskvqCmeyIowmeIMkwe@bVBl`aFJymus`AZTLqJtzWKiZ]]jzwy_VkaQxZr}d{Sz}VwXoXZfrdZUNJ}OUI`qEqZb{H{jyKrKp{iZsFhbdzPdvryGw^kU`aqhybGjrKxrgVkIIF`xEV}LdN}bMkQRcFzWqNaQKZpRhf^nybupYTWYDu^ivbymhEorLManUE@{GCx_EtGUjh@\\DAbXD", "fa{@P@@LdwHhhheHdiMCDYqVoFmgkSSUUUUMUUJPTTkJJ@@"},
			{"#qjGvChcZMzVNWUNGbF}GTHVJ_ALzAXPPyu~lL]LEKUH]BJ]Icv|SBZQyyEUjiyeJJoEyDWFiW{EwjR[rOsQEtJsD}cpYIIQl~fiPry`XRrJG]Doyt{kk@rhOTIJtr@~ZxSbWpIjY^wHPEe|h\\OUZy_wTkXiSF[YrEobCZjjzUKans^vgQDsyfKfElk_sREhaPSyjWOAD\\vqt}MEdZhYSjZCWOEyTMdFhtz^RuhtllfTGFSXvmzcVFWhP}slNsVIhJN~zuQtjYfqqD@t\\QsyYrYaapVxytZlb\\VjWLcR^ia]iDOWRkz\\mj`@FnA^DD", "fde@P@@DEGHhiDeHULkrRc^BuUUUUUQDRbM@p@"},
			{"#qlnMtoVyZQonZBgHzD@yvKnISlto[`r~zpdb]KDMAJUZ}wI[_QoX]FgHZEs~KJwHbCtiiQyeZxouU^\\\\LB^n\\W\\y^JPdzBhmGtmKdaPX_K}}RcBOqUSj^dlEICvU[arfbjwX{zokZXsxQ]NsaetxjCNQcZXGZx~mwag|IEfhtw\\LPxtLdA}~Uhmlek}x]RLOl@BstTVH@]nXhVqpJ_vmYlX`~OCxMlCUn_DjonW]d]TpCeT`Hw^AWI`qVwPCO]|psvqOgaFItzY[f{RanB\\ctnDY{mDYs~F[HQHwZPltDVRW_pWCCn[K^{jI|ucEt{OSnZvMuySS^mThoB[i|^AiIzUSCz[Ee}Gm~UZjVrR}C_NMyy[HAccUG\\WLc|lr\\_aQFDRDZwQ\\]fmkq[OZ^coFXl\\_s{oT|{ki|T|KTKTcvB`YU{NXi{KkpKkil`SeXl}OA\\vuXfnYFyVCX[FVpiHFYA}KNgRcC\\^RIwCYZBu|ghZx}`aM]KAbjcJnckoZQj\\ylNt]XIQECpFFftfpneHabtSZ{rsjgmf_ALkGoSZzREBagkSOM[M\\AlNByRQYfXdc}Oq^GgvFskiRVYxlbtD~^FpE^eUnShw`YjDbOpuyCitB`JTiegbc_O[S|rMAjQEpuiQ|xmiV@LLAf@@", "el^PJ@@@DJID\\bbTbtflTbTQaRTRtfxD|RjzfV^~ZjjjjjjZjjjjjdp`dLed|WRfRr~`@"},
			{"#qskHLwD]MHIVMTEmImTai^RkMhnxEEgpnrisMT\\F|uKbEZSjFc]aR@~c`qQi}{@VudLXyzCgN@QRJUa[sIJVDT{IYdV_D[T[\\O[EBVRoePxshmZo^gUo[mfywcQ^NEBy{bsFloZcLEmDWBzxkVaBUxNDaXo``|xW]Fnh{Y`RZnMvUuXvm{MKpGvrKUxynCOol}fJS~~VJFj|u^}VikdgxAzrkkrrDazngWPp]WjuP^{nE@yZhmZHnR`\\FdOYCLsvRutXm^dENOew[MG~WK{ovYvlDvq~ojqy[sUi[[`e|\\GITVZLludA{EQISZtgSPCyZapnb]Oy_dhAljFjCcOKsm}V{[v[sUHQUzXOtT~lHFKpb_GMrZfPKBDQBcgZoB^YooX`LyhvUksOJiSNfbdadrTYIGsbUjpUzhz{s@{VvMLmRP]VgaIZc]PfSHFwoK_JCoYRmi[SQdrHmdqqM]DOj^dlpyt|\\dIiIpG|@wUu^zkdp`CyXVNBHSAejiw]lo~nsJi[tgYa|dyUOTMMurMWNnLiKotDDSSzvuibPHLAspD", "e`RPJ@@@EmkolbbdRebTJTfbqbbLw@ae`TQSVqsUUUUUUURuUMWADDRb|TTJat@@"},
			{"#qLny@Z`sWoz{CdtT{Q_z\\TFCI`zrs{nv|F{xJcawBAF~[DQdpNzPZcGwp\\zktp[kPTG_dBgitpGRLENqKGyybap_wCRUgHSEHB^dBUNGpeDFjkP_NE}Ohjejj}E|m]vP}@m]DeRfts~GAuOywl}TVDkF_AoYdInKbCo^SyeTg`XU|YBhudd^{wZ\\Qbk\\jSVk|Q{krtR_lJhFJpa_R|b]Vi\\EDc{SeDna_tm\\^^yxcpZekXclJoMMoWJaBbMUBvHSRADwxtec\\WD_z_i[QUqlMaOGUaYFSc[{R[iL|xzqNxUgd]YrhpjRtEuMr@UQDKMUeGrhXKz^TLMrW{tuOUA[IJ[VyEcWMMvUOq|f{WkGhEAQk}@@JALxD", "ei]PH@`H`I@bPXdAIARPlhNiBZPvdKi@FPQdLYBfPydAYCHhhiimheDhheEEEEimLl^IEhWbXHbJZj@JHbJJKh@BCR@"},
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
			{"#qO[Fu\\gyLcAtoFZ~k[oj[Zq{cse]egM]EmvhM^KGQKAM}_@^Ijopd{MCKkKS^E_SDZwRtj`hG}~wnhr~_lHEvaGwLUqe~lfW}Z|BIoUAlCfNsdhotdr]KBdJ~GMRWS}@@@lIWS~jnXZ@N|qFcMr_YhChsJeEAe}[gHseOC}yl|yQeRAl}sC[@GK^{aync_[lqnkHtJ@[CrsTHy`ctqagqwxKzyjzP~LYkbQfI\\ZRDEJrf^OjTjOU]Jl|ISv`VJEqtlXkIiINTPIViCgjFMuG|QfjpxUWZ~`Mdm@lpjiJtISX[ElCYoxZJLJf[b@ycU]^yfrwUI`}wXRlTZabq{Mb^z[WM_UkBAXLe{{d}sauIXlitz~JtAuY_URgheHgduPMDSTv\\eYfjcLbpmoljwd^j\\e[Je|l}ugddZXyQUavMlZeevHEjn`AxAXXD", "fcPbBL]`LEXCns`RbbdTRTrRRRVLxht\\mkOhvjjj@J@JJHHaIQFTX@"},
			{"#qxBazheOuNbZa`qOW[xPKGb\\TraT@@@ZUwxs^MBFakr|ZecJm]fr@ELC~IEd{|LJchZ`{TPZbWsvBnCcVRJtsnyicWObSeGxA~PPJQ@_aIr~w~l_uLx}mYfmM|dbO\\WjbSDuQ^tYSm{\\y{cUmpnOCZNdvazdY[NMEFD{bHubJk}j[xhdrlYhsdtoQT]FyGImUhzpqtXaZnVUG[GdN~LT\\W[FMbZIKphA}beiWckTBdkMgDMigANeH^cuUqjI`E@A[hD", "fbm`cBBXRBPaPHdH\\R\\bdrbbfRRqWAVCzMUR@@@U@@@"},
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
			{"!qx}kUypKL@cDRf@EThh^WbU{SymAw~m|S[`OxjZthx]udZSWHDIbzSlsR{z]ogMzy}rNTlHFiZcwQJzlRTsbbZbtpffzfMLiO\\VCH@BBZQjMznxAOLVZn|hu{~ulSXKChcNjg_GOS{PvnfR~^cvcCzZYqfpHYnSwASvSBaprB{HMXQqCBrJMRBFxRVZ^haSh\\LZivRDndBcuyRrQTPSPEhd`BFA@pD", "fcoab@BPQ`GaddbVbfRbbT\\RRVYKjy{jjjnjh@@@`PBT`@"},
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

	private V3DSceneWithSidePane mViewer;

	public static void main(String[] args) {
		launch(args);
		}

	@Override
	public void start(Stage primaryStage) {
		mViewer = ("editor".equals(System.getProperty("mode"))) ? new V3DSceneWithToolsPane(primaryStage) : new V3DSceneWithSidePane();
		String css = getClass().getResource("/resources/molviewer.css").toExternalForm();
		Scene scene = new Scene(mViewer, 1024, 768, true, SceneAntialiasing.BALANCED);
		scene.getStylesheets().add(css);
		mViewer.getScene3D().widthProperty().bind(scene.widthProperty());
		mViewer.getScene3D().heightProperty().bind(scene.heightProperty());

		primaryStage.setTitle("Molecule Viewer");
		primaryStage.setScene(scene);
		primaryStage.show();

		Platform.runLater(() -> showStartOptionDialog(mViewer.getScene3D()) );
	}

	private static void showStartOptionDialog(V3DScene scene) {
		Optional<StartOptions> result = new StartOptionDialog(scene.getScene().getWindow(), null).showAndWait();
		result.ifPresent(options -> {
			options.initializeScene(scene);
		} );
	}

}

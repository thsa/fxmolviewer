package org.openmolecules.fx.viewer3d.interactions.simple;

import com.actelion.research.chem.phesa.pharmacophore.pp.AcceptorPoint;
import com.actelion.research.chem.phesa.pharmacophore.pp.ChargePoint;
import com.actelion.research.chem.phesa.pharmacophore.pp.DonorPoint;
import com.actelion.research.chem.phesa.pharmacophore.pp.IPharmacophorePoint;
import javafx.geometry.Point3D;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DScene;
import org.openmolecules.fx.viewer3d.interactions.simple.V3DInteraction.Interaction;

import java.util.ArrayList;
import java.util.List;

public class V3DInteractingPair {
	
	public static final double CHARGE_CHARGE_UPPER_CUTOFF = 2.5;
	public static final double CHARGE_CHARGE_LOWER_CUTOFF = 1.5;
	public static final double HBOND_UPPER_CUTOFF = 2.5;
	public static final double HBOND_LOWER_CUTOFF = 1.5;
	public static final double CUTOFF_ANGLE_DEVIATION_HBOND = 45; //in degree
	public static final double CUTOFF_ANGLE_DEVIATION_HBOND_SP3O = 60; //in degree
	private final V3DMolecule fxmol1;
	private final V3DMolecule fxmol2;
	private V3DInteractionSites iSites1;
	private V3DInteractionSites iSites2;
	private List<V3DInteraction> interactions;
	
	public V3DInteractingPair(V3DMolecule fxmol1, V3DMolecule fxmol2, V3DInteractionSites iSites1,
			V3DInteractionSites iSites2, V3DScene scene) {
		this.fxmol1 = fxmol1;
		this.fxmol2 = fxmol2;
		this.iSites1 = iSites1;
		this.iSites2 = iSites2; 
		//fxmol1.addMoleculeCoordinatesChangeListener(this);
		//fxmol2.addMoleculeCoordinatesChangeListener(this);
		//fxmol1.addMoleculeCoordinatesChangeListener(this);
		//fxmol2.addMoleculeCoordinatesChangeListener(this);
		interactions = new ArrayList<>();
		iSites1.addListener((o) -> recalc());
		iSites2.addListener((o) -> recalc());
		fxmol1.visibleProperty().addListener((v,ov,nv) -> molVisibilityChanged(nv));
		fxmol2.visibleProperty().addListener((v,ov,nv) -> molVisibilityChanged(nv));
	}

	public void analyze() {
		for(IPharmacophorePoint p1: iSites1.getSites() ) {
			for (IPharmacophorePoint p2 : iSites2.getSites()) {
				Interaction interaction = getInteraction(p1, p2);
				if (interaction != Interaction.NONE)
					//TODO: make static????
					interactions.add(new V3DInteraction(p1,
							p2, interaction, fxmol1, fxmol2, fxmol1.getParent()));
			}
		}
	}
	
	public Interaction getInteraction(IPharmacophorePoint pp1, IPharmacophorePoint pp2) {
		if (pp1 instanceof ChargePoint && pp2 instanceof ChargePoint)
			return createIonicInteraction((ChargePoint) pp1, (ChargePoint) pp2);
		if (pp1 instanceof AcceptorPoint && pp2 instanceof DonorPoint)
			return createHBondInteraction((AcceptorPoint)pp1, (DonorPoint)pp2, fxmol1, fxmol2);
		if (pp2 instanceof AcceptorPoint && pp1 instanceof DonorPoint)
			return createHBondInteraction((AcceptorPoint)pp2, (DonorPoint)pp1, fxmol2, fxmol1);

		return Interaction.NONE;
	}


	public void cleanup() {
		for(V3DInteraction interaction: interactions) 
			interaction.cleanup();
			
		interactions.clear();
	}

	public void recalc() {
		cleanup();
		analyze();
	}
	
	public void setVisibility(boolean visible) {
		for(V3DInteraction interaction: interactions) 
			interaction.setVisibility(visible);
	}
	
	private void molVisibilityChanged(boolean visible) {
		for(V3DInteraction interaction: interactions) 
			interaction.setVisibility(visible);
	}
	
	public boolean containsMolecule3D(V3DMolecule fxmol) {
		return fxmol == fxmol1 || fxmol == fxmol2;
	}

	private Interaction createHBondInteraction(AcceptorPoint ppAcc, DonorPoint ppDon, V3DMolecule accMol, V3DMolecule donMol) {
		Point3D pAcc = accMol.localToParent(ppAcc.getCenter().x, ppAcc.getCenter().y, ppAcc.getCenter().z);
		Point3D pDon = donMol.localToParent(ppDon.getCenter().x, ppDon.getCenter().y, ppDon.getCenter().z);
		double dist = pAcc.distance(pDon);
		if(dist>HBOND_LOWER_CUTOFF && dist<HBOND_UPPER_CUTOFF) {
			Point3D endPointAcc = accMol.localToParent(ppAcc.getCenter().x + ppAcc.getDirectionality().x,
					ppAcc.getCenter().y + ppAcc.getDirectionality().y, ppAcc.getCenter().z + ppAcc.getDirectionality().z);
			Point3D endPointDon = donMol.localToParent(ppDon.getCenter().x + ppDon.getDirectionality().x,
					ppDon.getCenter().y + ppDon.getDirectionality().y, ppDon.getCenter().z + ppDon.getDirectionality().z);
			Point3D direcAcc = endPointAcc.subtract(pAcc);
			Point3D direcDon = endPointDon.subtract(pDon);
			double linearity = pDon.angle(pDon.add(direcDon.multiply(-1.0)), pAcc); //angle between D-H and A
			if(linearity<180+CUTOFF_ANGLE_DEVIATION_HBOND && linearity>180-CUTOFF_ANGLE_DEVIATION_HBOND) {
				double directionality = direcAcc.angle(pAcc.subtract(pDon));
				double directionalityCutoff = ppAcc.getAcceptorID()==0 ? CUTOFF_ANGLE_DEVIATION_HBOND_SP3O: CUTOFF_ANGLE_DEVIATION_HBOND;
				if(directionality<180+directionalityCutoff && directionality>180-directionalityCutoff)
					return Interaction.HBOND;
			}
		}
		return Interaction.NONE;
	}

	private Interaction createIonicInteraction(ChargePoint cp1, ChargePoint cp2) {
		if (cp1.getCharge() * cp2.getCharge()<0) {
			Point3D center1 = fxmol1.localToParent(cp1.getCenter().x, cp1.getCenter().y, cp1.getCenter().z);
			Point3D center2 = fxmol2.localToParent(cp2.getCenter().x, cp2.getCenter().y, cp2.getCenter().z);
			double dist = center1.distance(center2);
			if(dist>CHARGE_CHARGE_LOWER_CUTOFF && dist<CHARGE_CHARGE_UPPER_CUTOFF)
				return Interaction.IONIC;
		}
		return Interaction.NONE;
	}
}

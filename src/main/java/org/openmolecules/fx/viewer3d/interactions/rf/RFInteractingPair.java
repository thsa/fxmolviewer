package org.openmolecules.fx.viewer3d.interactions.rf;

import com.actelion.research.chem.conf.VDWRadii;
import com.actelion.research.chem.phesa.pharmacophore.pp.IPharmacophorePoint;
import javafx.geometry.Point3D;
import org.openmolecules.fx.viewer3d.V3DMolecule;

import java.util.ArrayList;
import java.util.List;

public class RFInteractingPair {

	public static final double CHARGE_CHARGE_UPPER_CUTOFF = 2.5;
	public static final double CHARGE_CHARGE_LOWER_CUTOFF = 1.5;
	public static final double HBOND_UPPER_CUTOFF = 2.5;
	public static final double HBOND_LOWER_CUTOFF = 1.5;
	public static final double CUTOFF_ANGLE_DEVIATION_HBOND = 45; //in degree
	public static final double CUTOFF_ANGLE_DEVIATION_HBOND_SP3O = 60; //in degree
	private final V3DMolecule fxmol1;
	private final V3DMolecule fxmol2;
	private final RFInteractionSites iSites1;
	private final RFInteractionSites iSites2;
	private final List<RFInteraction> interactions;

	public RFInteractingPair(V3DMolecule fxmol1, V3DMolecule fxmol2, RFInteractionSites iSites1,
	                         RFInteractionSites iSites2) {
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
			for(IPharmacophorePoint p2: iSites2.getSites() ) {
				RFInteraction.Interaction interaction = getInteraction(p1,p2);
				if(interaction!= RFInteraction.Interaction.NONE)
					//TODO: make static????
					interactions.add(new RFInteraction(p1, p2,interaction,fxmol1,fxmol2,fxmol1.getParent()));
			}
		}
	}

	public RFInteraction.Interaction getInteraction(IPharmacophorePoint pp1, IPharmacophorePoint pp2) {
		RFInteraction.Interaction interaction = RFInteraction.Interaction.NONE;



		Point3D center1 = fxmol1.localToParent(pp1.getCenter().x, pp1.getCenter().y, pp1.getCenter().z);
		Point3D center2 = fxmol2.localToParent(pp2.getCenter().x, pp2.getCenter().y, pp2.getCenter().z);
		double dist = center1.distance(center2);
		if(dist < VDWRadii.getVDWRadius(fxmol1.getMolecule().getAtomicNo(pp1.getCenterID()))
				+ VDWRadii.getVDWRadius(fxmol2.getMolecule().getAtomicNo(pp2.getCenterID())) + 1.5)
			interaction = RFInteraction.Interaction.OTHER;





/*		if(pp1 instanceof ChargePoint && pp2 instanceof ChargePoint) { //ionic interaction
			ChargePoint cp1 = (ChargePoint) pp1;
			ChargePoint cp2 = (ChargePoint) pp2;
			if(cp1.getCharge()*cp2.getCharge()<0) {
				Point3D center1 = fxmol1.localToParent(pp1.getCenter().x, pp1.getCenter().y, pp1.getCenter().z);
				Point3D center2 = fxmol2.localToParent(pp2.getCenter().x, pp2.getCenter().y, pp2.getCenter().z);
				double dist = center1.distance(center2);
				if(dist>CHARGE_CHARGE_LOWER_CUTOFF && dist<CHARGE_CHARGE_UPPER_CUTOFF)
					interaction = RFInteraction.Interaction.IONIC;
			}
		}
		else if((pp1 instanceof AcceptorPoint && pp2 instanceof DonorPoint) || (pp1 instanceof DonorPoint && pp2 instanceof AcceptorPoint)) { //hbond
			Point3D center1 = fxmol1.localToParent(pp1.getCenter().x, pp1.getCenter().y, pp1.getCenter().z);
			Point3D center2 = fxmol2.localToParent(pp2.getCenter().x, pp2.getCenter().y, pp2.getCenter().z);
			double dist = center1.distance(center2);
			if(dist>HBOND_LOWER_CUTOFF && dist<HBOND_UPPER_CUTOFF) {
				Point3D endPoint1 = fxmol1.localToParent(pp1.getCenter().x + pp1.getDirectionality().x,
						pp1.getCenter().y + pp1.getDirectionality().y, pp1.getCenter().z + pp1.getDirectionality().z);
				Point3D endPoint2 = fxmol2.localToParent(pp2.getCenter().x + pp2.getDirectionality().x,
						pp2.getCenter().y + pp2.getDirectionality().y, pp2.getCenter().z + pp2.getDirectionality().z);
				Point3D direc1 = endPoint1.subtract(center1);
				Point3D direc2 = endPoint2.subtract(center2);
				if(pp1 instanceof AcceptorPoint) { //acceptor-donor hbond
					double linearity = center2.angle(center2.add(direc2.multiply(-1.0)), center1); //angle between D-H and A
					if(linearity<180+CUTOFF_ANGLE_DEVIATION_HBOND && linearity>180-CUTOFF_ANGLE_DEVIATION_HBOND) {
						double directionality = direc1.angle(center1.subtract(center2));
						double directionalityCutoff = ((AcceptorPoint)pp1).getAcceptorID()==0 ? CUTOFF_ANGLE_DEVIATION_HBOND_SP3O: CUTOFF_ANGLE_DEVIATION_HBOND;
						if(directionality<180+directionalityCutoff && directionality>180-directionalityCutoff)
							interaction = RFInteraction.Interaction.HBOND;
					}
				}
				else { //donor-acceptor bond
					double linearity = center1.angle(center1.add(direc1.multiply(-1.0)), center2); //angle between D-H and A
					if(linearity<180+CUTOFF_ANGLE_DEVIATION_HBOND && linearity>180-CUTOFF_ANGLE_DEVIATION_HBOND) {
						double directionality = direc2.angle(center2.subtract(center1));
						double directionalityCutoff = ((AcceptorPoint)pp2).getAcceptorID()==0 ? CUTOFF_ANGLE_DEVIATION_HBOND_SP3O: CUTOFF_ANGLE_DEVIATION_HBOND;
						if(directionality<180+directionalityCutoff && directionality>180-directionalityCutoff)
							interaction = RFInteraction.Interaction.HBOND;
					}
				}
			}
		}*/
		return interaction;
	}

	public void cleanup() {
		for(RFInteraction interaction: interactions)
			interaction.cleanup();

		interactions.clear();
	}

	public void recalc() {
		cleanup();
		analyze();
	}

	public void setVisibility(boolean visible) {
		for(RFInteraction interaction: interactions)
			interaction.setVisibility(visible);
	}

	private void molVisibilityChanged(boolean visible) {
		for(RFInteraction interaction: interactions)
			interaction.setVisibility(visible);
	}
}
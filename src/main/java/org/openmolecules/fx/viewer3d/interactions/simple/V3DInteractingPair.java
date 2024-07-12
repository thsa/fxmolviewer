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
	private V3DMolecule fxmol1;
	private V3DMolecule fxmol2;
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
		interactions = new ArrayList<V3DInteraction>();
		iSites1.addListener((o) -> recalc());
		iSites2.addListener((o) -> recalc());
		fxmol1.visibleProperty().addListener((v,ov,nv) -> molVisibilityChanged(nv));
		fxmol2.visibleProperty().addListener((v,ov,nv) -> molVisibilityChanged(nv));
	}
		
	
	public void analyze() {
		for(IPharmacophorePoint p1: iSites1.getSites() ) {
			for(IPharmacophorePoint p2: iSites2.getSites() ) {
				Interaction interaction = getInteraction(p1,p2);
				if(interaction!=Interaction.NONE)
					//TODO: make static????
					interactions.add(new V3DInteraction(p1,
							p2,interaction,fxmol1,fxmol2,fxmol1.getParent()));
					
				}
					
			}
		
	}
	
	
	
	
	public Interaction getInteraction(IPharmacophorePoint pp1, IPharmacophorePoint pp2) {
		Interaction interaction = Interaction.NONE;
		if(pp1 instanceof ChargePoint && pp2 instanceof ChargePoint) { //ionic interaction
			ChargePoint cp1 = (ChargePoint) pp1;
			ChargePoint cp2 = (ChargePoint) pp2;
			if(cp1.getCharge()*cp2.getCharge()<0) {
				Point3D center1 = fxmol1.localToParent(pp1.getCenter().x, pp1.getCenter().y, pp1.getCenter().z);
				Point3D center2 = fxmol2.localToParent(pp2.getCenter().x, pp2.getCenter().y, pp2.getCenter().z);
				double dist = center1.distance(center2);
				if(dist>CHARGE_CHARGE_LOWER_CUTOFF && dist<CHARGE_CHARGE_UPPER_CUTOFF)
					interaction = Interaction.IONIC;
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
							interaction = Interaction.HBOND;
					}
				}
				else { //donor-acceptor bond
					double linearity = center1.angle(center1.add(direc1.multiply(-1.0)), center2); //angle between D-H and A
					if(linearity<180+CUTOFF_ANGLE_DEVIATION_HBOND && linearity>180-CUTOFF_ANGLE_DEVIATION_HBOND) {
						double directionality = direc2.angle(center2.subtract(center1));
						double directionalityCutoff = ((AcceptorPoint)pp2).getAcceptorID()==0 ? CUTOFF_ANGLE_DEVIATION_HBOND_SP3O: CUTOFF_ANGLE_DEVIATION_HBOND;
						if(directionality<180+directionalityCutoff && directionality>180-directionalityCutoff)
							interaction = Interaction.HBOND;
					}
				}
			}
			
		}
		return interaction;
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
		boolean contains;
		return contains = (fxmol==fxmol1 || fxmol==fxmol2) ? true : false;
	}
}

package org.openmolecules.fx.viewer3d;

import java.util.ArrayList;


import com.actelion.research.chem.Coordinates;
import com.actelion.research.util.DoubleFormat;

import javafx.geometry.Point3D;
import javafx.scene.Parent;
import javafx.scene.paint.Color;
import javafx.scene.Group;

public class V3DMeasurement implements MoleculeChangeListener {
	
	private ArrayList<Integer> mAtoms;
	private ArrayList<V3DMolecule> mFXmols;
	private DashedRod mRod;
	private NonRotatingLabel mLabel;
	private Point3D[] mPoints;
	private Parent mParent;
	
	public V3DMeasurement(ArrayList<Integer> atoms, ArrayList<V3DMolecule> fxMols, DashedRod rod, NonRotatingLabel label, Parent parent) {
		for(V3DMolecule fxmol:fxMols) {
			fxmol.addMoleculeChangeListener(this);
		}
		mAtoms = atoms;
		mFXmols = fxMols;
		mRod = rod;
		mLabel = label;
		mPoints = new Point3D[atoms.size()];
		mParent = parent;
		((Group)parent).getChildren().add(mRod);

		
	}
	
	public void cleanup() {
		((Group)mParent).getChildren().remove(mRod);
		mLabel.remove(mParent);
		for(V3DMolecule fxmol:mFXmols) {
			fxmol.removeMoleculeChangeListener(this);
		}
	}

	@Override
	public void coordinatesChanged() {
		for(int i=0;i<mPoints.length;i++) {
			Coordinates c = mFXmols.get(i).getMolecule().getCoordinates(mAtoms.get(i));
			mPoints[i]=mFXmols.get(i).localToParent(c.x,c.y,c.z);	
		}
		if(mPoints.length==2) {//distance
			double distance = mPoints[0].distance(mPoints[1]);
			String text = DoubleFormat.toString(distance,3);
			mLabel.update(mPoints[0], mPoints[1], text);
			Color color = mRod.getColor();
			((Group)mParent).getChildren().remove(mRod);
			mRod = new DashedRod(mPoints[0],mPoints[1],color);
			((Group)mParent).getChildren().add(mRod);
		}
		else if(mPoints.length==3) {
			Point3D v1 = mPoints[0].subtract(mPoints[1]);
			Point3D v2 = mPoints[2].subtract(mPoints[1]);
			double angle = v1.angle(v2);
			String text = DoubleFormat.toString(angle,3);
			mLabel.update(mPoints[0], mPoints[2], text);
			Color color = mRod.getColor();
			((Group)mParent).getChildren().remove(mRod);
			mRod = new DashedRod(mPoints[0],mPoints[2],color);
			((Group)mParent).getChildren().add(mRod);
		}
		else if(mPoints.length==4) {
			Coordinates c1 = new Coordinates(mPoints[0].getX(),mPoints[0].getY(),mPoints[0].getZ());
			Coordinates c2 = new Coordinates(mPoints[1].getX(),mPoints[1].getY(),mPoints[1].getZ());
			Coordinates c3 = new Coordinates(mPoints[2].getX(),mPoints[2].getY(),mPoints[2].getZ());
			Coordinates c4 = new Coordinates(mPoints[3].getX(),mPoints[3].getY(),mPoints[3].getZ());
			double dihedral = 180*Coordinates.getDihedral(c1, c2, c3, c4)/Math.PI;
			String text = DoubleFormat.toString(dihedral,3);
			mLabel.update(mPoints[0], mPoints[3], text);
			Color color = mRod.getColor();
			((Group)mParent).getChildren().remove(mRod);
			mRod = new DashedRod(mPoints[0],mPoints[3],color);
			((Group)mParent).getChildren().add(mRod);
		}
		// TODO Auto-generated method stub
		
	}
	
	
	public ArrayList<V3DMolecule> getV3DMolecules() {
		return mFXmols;
	}
	
	public NonRotatingLabel getLabel() {
		return mLabel;
	}






	



	
}

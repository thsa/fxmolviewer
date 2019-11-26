package org.openmolecules.fx.viewer3d.interactions;

import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.nodes.DashedRod;
import org.openmolecules.fx.viewer3d.nodes.NonRotatingLabel;
import com.actelion.research.chem.phesa.pharmacophore.IPharmacophorePoint;
import com.actelion.research.util.DoubleFormat;
import javafx.geometry.Point3D;
import javafx.scene.Parent;
import javafx.scene.paint.Color;
import javafx.scene.Group;

public class V3DInteraction  {
	
		public enum Interaction{NONE, HBOND, IONIC}; 
		
		private static final Color HBOND_COLOR = Color.YELLOW;
		private static final Color IONIC_COLOR = Color.ORANGERED;
		
		private static final float RADIUS = 0.07f;
		private static final float DASH_LENGTH = 0.2f;
		private static final float GAP_LENGTH = 0.2f;
		
		private IPharmacophorePoint mPP1;
		private IPharmacophorePoint mPP2;
		private V3DMolecule mFXmol1;
		private V3DMolecule mFXmol2;
		private DashedRod mRod;
		//private NonRotatingLabel mLabel;
		private Parent mParent;
		private Interaction mInteraction;
		
		public V3DInteraction(IPharmacophorePoint pp1,IPharmacophorePoint pp2, Interaction interaction, V3DMolecule fxmol1, V3DMolecule fxmol2, Parent parent) {
			mPP1 = pp1;
			mPP2 = pp2;
			mFXmol1 = fxmol1;
			mFXmol2 = fxmol2;
			mParent = parent;
			mInteraction = interaction;
			create();
			((Group)parent).getChildren().add(mRod);
			
		}
		
		private void create() {
			Color color;
			if(mInteraction == Interaction.HBOND)
				color = HBOND_COLOR;
			else 
				color = IONIC_COLOR;
			Point3D point1 = mFXmol1.localToParent(mPP1.getCenter().x,mPP1.getCenter().y,mPP1.getCenter().z);
			Point3D point2 = mFXmol2.localToParent(mPP2.getCenter().x,mPP2.getCenter().y,mPP2.getCenter().z);
			//double distance = point1.distance(point2);
			//String text = DoubleFormat.toString(distance,3);
			mRod = new DashedRod(point1, point2, color, RADIUS, DASH_LENGTH, GAP_LENGTH);
			//mLabel = NonRotatingLabel.create(mParent, text, point1, point2, color);
		}
		
		//public NonRotatingLabel getLabel() {
		//	return mLabel;
		//}

		public void cleanup() {

			((Group)mParent).getChildren().remove(mRod);
			//mLabel.remove(mParent);
		}
		
		public void setVisibility(boolean visible) {
			mRod.setVisible(visible);
		}




		



		
	}

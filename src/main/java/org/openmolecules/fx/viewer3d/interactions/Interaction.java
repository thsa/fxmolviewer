package org.openmolecules.fx.viewer3d.interactions;

import com.actelion.research.chem.Coordinates;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import org.openmolecules.fx.viewer3d.nodes.DashedRod;

public class Interaction {
	private static final float MAX_RADIUS = 0.08f;
	private static final float DASH_LENGTH = 0.2f;
	private static final float GAP_LENGTH = 0.25f;

	private final InteractionPoint mIP1,mIP2;
	private final double mDistance,mAngle,mStrength;
	private double mLength;
	private final int mType;
	private int mVisAtom1, mVisAtom2;
	private DashedRod mRod;
	private final Color mColor;

	public Interaction(InteractionPoint ip1, InteractionPoint ip2, int type, double distance, double angle, double strength, Color color) {
		mIP1 = ip1;
		mIP2 = ip2;
		mVisAtom1 = -1;
		mVisAtom2 = -1;
		mType = type;
		mDistance = distance;
		mAngle = angle;
		mStrength = strength;
		mColor = color;
	}

	public void setVisAtom(int no, int atom) {
		if (no == 0)
			mVisAtom1 = atom;
		else
			mVisAtom2 = atom;
	}

	public InteractionPoint getInteractionPoint(int no) {
		return no == 0 ? mIP1 : mIP2;
	}
	public double getDistance() {
		return mDistance;
	}

	public double getAngle() {
		return mAngle;
	}

	public double getStrength() {
		return mStrength;
	}

	public double getLength() {
		return mLength;
	}

	public int getType() {
		return mType;
	}

	public void create() {
		Coordinates c1 = (mVisAtom1 == -1) ? mIP1.getCenter() : mIP1.getMol().getCoordinates(mVisAtom1);
		Coordinates c2 = (mVisAtom2 == -1) ? mIP2.getCenter() : mIP2.getMol().getCoordinates(mVisAtom2);
		Point3D point1 = mIP1.getFXMol().localToParent(c1.x,c1.y,c1.z);
		Point3D point2 = mIP2.getFXMol().localToParent(c2.x,c2.y,c2.z);
		mLength = point1.distance(point2);
		//String text = DoubleFormat.toString(distance,3);
		mRod = new DashedRod(point1, point2, mColor, (float)mStrength*MAX_RADIUS, DASH_LENGTH, GAP_LENGTH);
		//mLabel = NonRotatingLabel.create(mParent, text, point1, point2, color);
		((Group)mIP1.getFXMol().getParent()).getChildren().add(mRod);
	}

	//public NonRotatingLabel getLabel() {
	//	return mLabel;
	//}

	public void cleanup() {
		((Group)mIP1.getFXMol().getParent()).getChildren().remove(mRod);
		//mLabel.remove(mParent);
	}

	public void setVisibility(boolean visible) {
		mRod.setVisible(visible);
	}
}


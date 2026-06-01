package org.openmolecules.fx.viewer3d.interactions.plip;

import javafx.scene.paint.Color;
import org.openmolecules.fx.viewer3d.interactions.V3DInteraction;
import org.openmolecules.fx.viewer3d.interactions.V3DInteractionPoint;

public class PLIPInteraction extends V3DInteraction {
	private final double mAngle;
	private final int mType;

	public PLIPInteraction(V3DInteractionPoint ip1, V3DInteractionPoint ip2, int type, double value, double distance, double angle, double strength, Color color) {
		super(ip1, ip2, value, distance, strength, color);
		mType = type;
		mAngle = angle;
	}

	public int getType() {
		return mType;
	}

	public double getAngle() {
		return mAngle;
	}
}

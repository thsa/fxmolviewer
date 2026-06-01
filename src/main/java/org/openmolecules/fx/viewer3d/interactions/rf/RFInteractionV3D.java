package org.openmolecules.fx.viewer3d.interactions.rf;

import javafx.scene.paint.Color;
import org.openmolecules.chem.interaction.rf.RFInteraction;
import org.openmolecules.fx.viewer3d.interactions.V3DInteraction;
import org.openmolecules.fx.viewer3d.interactions.V3DInteractionPoint;

public class RFInteractionV3D extends V3DInteraction {
	private final RFInteraction mInteraction;

	public RFInteractionV3D(V3DInteractionPoint ip1, V3DInteractionPoint ip2, RFInteraction interaction, double rf) {
		super(ip1, ip2, rf, interaction.getDistance(), 0.5 + Math.abs(Math.log10(rf)),
				(rf<0.9) ? Color.RED.darker() : (rf<1.1) ? Color.GRAY : Color.BLUE.brighter());
		mInteraction = interaction;
	}

	public RFInteraction getRFInteraction() {
		return mInteraction;
	}
}

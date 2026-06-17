package org.openmolecules.fx.viewer3d.interactions.rf;

import javafx.scene.paint.Color;
import org.openmolecules.chem.interaction.rf.RFInteraction;
import org.openmolecules.fx.viewer3d.interactions.V3DInteraction;
import org.openmolecules.fx.viewer3d.interactions.V3DInteractionPoint;

public class RFInteractionV3D extends V3DInteraction {
	private static final double LOW_NEUTRAL_RF = 0.5;
	private static final double HIGH_NEUTRAL_RF = 1.0;
	private final RFInteraction mInteraction;

	public RFInteractionV3D(V3DInteractionPoint ip1, V3DInteractionPoint ip2, RFInteraction interaction, double rf) {
		super(ip1, ip2, rf, interaction.getDistance(),
				0.5 + Math.abs(Math.log10(rf < LOW_NEUTRAL_RF ? LOW_NEUTRAL_RF/rf : rf < HIGH_NEUTRAL_RF ? 1.0 : rf/HIGH_NEUTRAL_RF)),
				(rf<LOW_NEUTRAL_RF) ? Color.RED.darker() : (rf<HIGH_NEUTRAL_RF) ? Color.GRAY : Color.BLUE.brighter());
		mInteraction = interaction;
	}

	public RFInteraction getRFInteraction() {
		return mInteraction;
	}
}

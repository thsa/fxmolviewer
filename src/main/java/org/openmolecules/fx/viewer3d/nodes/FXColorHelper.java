package org.openmolecules.fx.viewer3d.nodes;

import javafx.scene.paint.Color;


public class FXColorHelper {
	
	public static Color changeOpacity(Color col, double opacity) {
		Color color = new Color(col.getRed(),col.getGreen(),col.getBlue(), opacity);
		return color;
	}

}

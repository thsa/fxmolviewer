package org.sunflow.core.shader;

import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;

/**
 * Created by thomas on 19.04.16.
 */
public class MyGlassShader extends GlassShader {
	private ColorProvider mColorProvider;

	public void setColorProvider(ColorProvider cp) {
		mColorProvider = cp;
	}

	@Override
	public Color getColor(ShadingState state) {
		if (mColorProvider == null)
			return super.getColor(state);
		return mColorProvider.colorAtPoint(state.getPoint());
	}
}

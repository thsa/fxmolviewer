package org.sunflow.core.shader;

import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;

/**
 * Created by thomas on 19.04.16.
 */
public class MyShinyDiffuseShader extends ShinyDiffuseShader {
	private ColorProvider mColorProvider;

	public void setColorProvider(ColorProvider cp) {
		mColorProvider = cp;
	}

	@Override
	public Color getDiffuse(ShadingState state) {
		Color ret = (mColorProvider == null) ? super.getDiffuse(state)
			: mColorProvider.colorAtPoint(state.getPoint()).mul(0.2f); // we seem to need to darken significantly

		if (state.isBehind())
			ret.mul(0.25f);

		return ret;
	}
}

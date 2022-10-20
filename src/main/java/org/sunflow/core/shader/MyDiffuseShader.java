package org.sunflow.core.shader;

import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;

/**
 * Created by thomas on 19.04.16.
 */
public class MyDiffuseShader extends DiffuseShader {
	private ColorProvider mColorProvider;

	public void setColorProvider(ColorProvider cp) {
		mColorProvider = cp;
	}

	@Override
	public Color getDiffuse(ShadingState state) {
		if (mColorProvider == null) {
			Color c = super.getDiffuse(state);
			return c;
			}
		return mColorProvider.colorAtPoint(state.getPoint()).mul(0.2f); // we seem to need to darken significantly
	}
}

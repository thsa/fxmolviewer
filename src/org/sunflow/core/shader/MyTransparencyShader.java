package org.sunflow.core.shader;

import org.sunflow.SunflowAPI;
import org.sunflow.core.ParameterList;
import org.sunflow.core.Ray;
import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;
import org.sunflow.math.Vector3;

public class MyTransparencyShader implements Shader {
	private ColorProvider mColorProvider;
    private Color color;
	private Color diff;
	private float refl;
	private float transparency;

    public MyTransparencyShader() {
        color = Color.CYAN;
	    diff = Color.GRAY;
	    refl = 0.1f;
	    transparency = 0.8f;
    }

	public void setColorProvider(ColorProvider cp) {
		mColorProvider = cp;
	}

	public boolean update(ParameterList pl, SunflowAPI api) {
        color = pl.getColor("color", color);
	    diff = pl.getColor("diffuse", diff);
	    refl = pl.getFloat("shiny", refl);
	    transparency = pl.getFloat("transparency", transparency);
        return true;
    }

	public Color getRadiance(ShadingState state) {
        state.faceforward();

		Color shiny = getShinyDiffuse(state);

		float cos = state.getCosND();

		Color c = (mColorProvider == null) ? color.copy() : mColorProvider.colorAtPoint(state.getPoint());

		Color absorption = Color.mul((1f-transparency)*cos*cos, c).add(new Color(transparency, transparency, transparency));

//		Color absorption = Color.mul(transparency+(1f-transparency)*cos*cos, c);	// 0: opaque, 1:transparent

		Color ret = state.traceTransparency().copy();
		ret.mul(absorption);

		shiny.mul(0.5f);
		ret.add(shiny);
		ret.clamp(0f, 1f);

	    return ret;
    }

	private Color getShinyDiffuse(ShadingState state) {
		if (state.isBehind())
			return Color.BLACK;

		Color lr = Color.black();   // we don't use any diffuse light

/*		// direct lighting
		state.initLightSamples();
		state.initCausticSamples();
		Color lr = state.diffuse(diff);
		if (!state.includeSpecular())
			return lr;
*/

		float cos = state.getCosND();
		float dn = 2 * cos;
		Vector3 refDir = new Vector3();
		refDir.x = (dn * state.getNormal().x) + state.getRay().getDirection().x;
		refDir.y = (dn * state.getNormal().y) + state.getRay().getDirection().y;
		refDir.z = (dn * state.getNormal().z) + state.getRay().getDirection().z;
		Ray refRay = new Ray(state.getPoint(), refDir);
		// compute Fresnel term
		cos = 1 - cos;
		float cos2 = cos * cos;
		float cos5 = cos2 * cos2 * cos;

		Color ret = Color.white();
		Color r = diff.copy().mul(refl);
		ret.sub(r);
		ret.mul(cos5);
		ret.add(r);
		return lr.add(ret.mul(state.traceReflection(refRay, 0)));
	}

	public void scatterPhoton(ShadingState state, Color power) {
    }
}
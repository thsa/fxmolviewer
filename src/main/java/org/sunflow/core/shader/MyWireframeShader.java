package org.sunflow.core.shader;

import org.sunflow.SunflowAPI;
import org.sunflow.core.ParameterList;
import org.sunflow.core.Ray;
import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;
import org.sunflow.math.Matrix4;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

public class MyWireframeShader implements Shader {
	private Color lineColor;
	private Color color;
	private ColorProvider mColorProvider;
	private float width;
	private float cosWidth;
	private float refl;

	public MyWireframeShader() {
		lineColor = Color.BLACK;
		color = Color.WHITE;
		refl = 0.5f;
		// pick a very small angle - should be roughly the half the angular
		// width of a pixel
		width = (float) (Math.PI * 0.5 / 4096);
		cosWidth = (float) Math.cos(width);
	}

	public void setColorProvider(ColorProvider cp) {
		mColorProvider = cp;
	}

	public boolean update(ParameterList pl, SunflowAPI api) {
		lineColor = pl.getColor("line", lineColor);
		width = pl.getFloat("width", width);
		cosWidth = (float) Math.cos(width);
		return true;
	}

	public Color getLineColor(ShadingState state) {
		return (mColorProvider == null) ? lineColor : mColorProvider.colorAtPoint(state.getPoint()).mul(0.3f);  // reduce brightness
	}

	public Color getRadiance(ShadingState state) {
		Point3[] p = new Point3[3];
		if (!state.getTrianglePoints(p))
			return getStraightThroughRadiance(state);
		// transform points into camera space
		Point3 center = state.getPoint();
		Matrix4 w2c = state.getWorldToCamera();
		center = w2c.transformP(center);
		for (int i = 0; i < 3; i++)
			p[i] = w2c.transformP(state.transformObjectToWorld(p[i]));
		float cn = 1.0f / (float) Math.sqrt(center.x * center.x + center.y * center.y + center.z * center.z);
		for (int i = 0, i2 = 2; i < 3; i2 = i, i++) {
			// compute orthogonal projection of the shading point onto each
			// triangle edge as in:
			// http://mathworld.wolfram.com/Point-LineDistance3-Dimensional.html
			float t = (center.x - p[i].x) * (p[i2].x - p[i].x);
			t += (center.y - p[i].y) * (p[i2].y - p[i].y);
			t += (center.z - p[i].z) * (p[i2].z - p[i].z);
			t /= p[i].distanceToSquared(p[i2]);
			float projx = (1 - t) * p[i].x + t * p[i2].x;
			float projy = (1 - t) * p[i].y + t * p[i2].y;
			float projz = (1 - t) * p[i].z + t * p[i2].z;
			float n = 1.0f / (float) Math.sqrt(projx * projx + projy * projy + projz * projz);
			// check angular width
			float dot = projx * center.x + projy * center.y + projz * center.z;
			if (dot * n * cn >= cosWidth)
				return getShinyDiffuseRadiance(state);
		}
		return getStraightThroughRadiance(state);
	}

	private Color getShinyDiffuseRadiance(ShadingState state) {
		// make sure we are on the right side of the material
		state.faceforward();
		// direct lighting
		state.initLightSamples();
		state.initCausticSamples();
		Color d = getLineColor(state);
		Color lr = state.diffuse(d);
		if (!state.includeSpecular())
			return lr;
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
		Color r = d.copy().mul(refl);
		ret.sub(r);
		ret.mul(cos5);
		ret.add(r);
		return lr.add(ret.mul(state.traceReflection(refRay, 0)));
	}

	private Color getStraightThroughRadiance(ShadingState state) {
		state.faceforward();
		return state.traceTransparency();
	}

	public void scatterPhoton(ShadingState state, Color power) {
		Color diffuse;
		// make sure we are on the right side of the material
		state.faceforward();
		diffuse = getLineColor(state);
		state.storePhoton(state.getRay().getDirection(), power, diffuse);
		float d = diffuse.getAverage();
		float r = d * refl;
		double rnd = state.getRandom(0, 0, 1);
		if (rnd < d) {
			// photon is scattered
			power.mul(diffuse).mul(1.0f / d);
			OrthoNormalBasis onb = state.getBasis();
			double u = 2 * Math.PI * rnd / d;
			double v = state.getRandom(0, 1, 1);
			float s = (float) Math.sqrt(v);
			float s1 = (float) Math.sqrt(1.0 - v);
			Vector3 w = new Vector3((float) Math.cos(u) * s, (float) Math.sin(u) * s, s1);
			w = onb.transform(w, new Vector3());
			state.traceDiffusePhoton(new Ray(state.getPoint(), w), power);
		} else if (rnd < d + r) {
			float cos = -Vector3.dot(state.getNormal(), state.getRay().getDirection());
			power.mul(diffuse).mul(1.0f / d);
			// photon is reflected
			float dn = 2 * cos;
			Vector3 dir = new Vector3();
			dir.x = (dn * state.getNormal().x) + state.getRay().getDirection().x;
			dir.y = (dn * state.getNormal().y) + state.getRay().getDirection().y;
			dir.z = (dn * state.getNormal().z) + state.getRay().getDirection().z;
			state.traceReflectionPhoton(new Ray(state.getPoint(), dir), power);
		}
	}
}
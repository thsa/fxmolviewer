package org.sunflow.core.shader;

import org.sunflow.image.Color;
import org.sunflow.math.Point3;

/**
 * Created by thomas on 18.04.16.
 */
public interface ColorProvider {
	public Color colorAtPoint(Point3 p);
}

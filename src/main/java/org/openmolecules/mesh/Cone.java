package org.openmolecules.mesh;

import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

public class Cone extends MeshView {

	private static final int DIVISIONS = 36;

    public Cone(double radius, double height) {
		super();

		double segmentAngle = 2.0 * Math.PI / DIVISIONS;

		TriangleMesh mesh = new TriangleMesh();
		mesh.getPoints().addAll(0, (float)-height/2, 0);

		double angle = 0;
		for(int i=0; i<DIVISIONS; i++) {
			float x = (float)(radius * Math.cos(angle));
			float y = (float)(radius * Math.sin(angle));
			mesh.getPoints().addAll(x, (float)-height/2, y);
			angle += segmentAngle;
		}

		mesh.getPoints().addAll(0, (float)height/2, 0);

		mesh.getTexCoords().addAll(0,0);

		for(int i=0; i<DIVISIONS; i++) {
			int next = (i == DIVISIONS-1) ? 1 : i+2;
			mesh.getFaces().addAll(i+1, 0, next, 0, 0, 0,
					i+1, 0, DIVISIONS+1, 0, next, 0);
		}

		setMesh(mesh);
	}
}
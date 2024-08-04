package org.openmolecules.mesh;

import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;

public class Cone extends MeshView {

	private static final int DIVISIONS = 36;

/*    public Cone(double radius, double height) {
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
	}*/

	public Cone(double radius, double height) {
		super();

		double segmentAngle = 2.0 * Math.PI / DIVISIONS;

		TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);

		mesh.getPoints().addAll(0, (float)-height/2, 0);   // location center of base
		mesh.getNormals().addAll(0f, -1f, 0);              // normal for base triangles

		double angle = 0;
		for(int i=0; i<DIVISIONS; i++) {
			float x = (float)(radius * Math.cos(angle));
			float y = (float)(radius * Math.sin(angle));
			mesh.getPoints().addAll(x, (float)-height/2, y);    // edge of base
			mesh.getNormals().addAll(x, 0, y);                  // for simplicity, we use radial normals instead of perpendicular ones
			angle += segmentAngle;
		}

		mesh.getPoints().addAll(0, (float)height/2, 0);         // tip

		mesh.getTexCoords().addAll(0,0);

		for(int i=0; i<DIVISIONS; i++) {
			int next = (i == DIVISIONS-1) ? 1 : i+2;
			mesh.getFaces().addAll(i+1, 0, 0, next, 0, 0, 0, 0, 0);     // base triangle
			mesh.getFaces().addAll(i+1, i+1, 0, DIVISIONS+1, i+1, 0, next, next, 0);    // side triangle
			// after 'DIVISIONS+1' we should use the mean normal between 'i+1' and 'next', but that is not visible
		}

		setMesh(mesh);
	}
}
package org.openmolecules.fx.viewer3d.interactions;

import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.mesh.MeshBuilder;

public class V3DInteractionMesh extends TriangleMesh implements MeshBuilder {
	public V3DInteractionMesh() {
		super(VertexFormat.POINT_TEXCOORD);
		getTexCoords().addAll(0, 0);    // create one texture point to refer to
	}

	@Override
	public void getPoint(int index, float[] xyz) {
		index *= getPointElementSize();
		xyz[0] = getPoints().get(index);
		xyz[1] = getPoints().get(index+1);
		xyz[2] = getPoints().get(index+2);
	}

	@Override
	public int addPoint(float x, float y, float z) {
		getPoints().addAll(x, y, z);
		return getPoints().size()/getPointElementSize()-1;
	}

	@Override
	public void addTriangle(int i1, int i2, int i3) {
		getFaces().addAll(i3, 0, i2, 0, i1, 0);
	}

	public void update(V3DMolecule fxmol) {
		// TODO
	}
}

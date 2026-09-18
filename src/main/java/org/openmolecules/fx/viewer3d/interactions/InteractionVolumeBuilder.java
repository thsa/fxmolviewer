package org.openmolecules.fx.viewer3d.interactions;

public interface InteractionVolumeBuilder {
	float getExtend(int atom);
	void buildVolume(float[] grid, int xSize, int ySize, int zSize, float xOffset, float yOffset, float zOffset, int atom, int pType);
}

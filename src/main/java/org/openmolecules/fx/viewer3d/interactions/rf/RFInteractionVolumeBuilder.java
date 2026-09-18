package org.openmolecules.fx.viewer3d.interactions.rf;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.VDWRadii;
import org.openmolecules.chem.interaction.rf.*;
import org.openmolecules.fx.viewer3d.interactions.InteractionVolumeBuilder;
import org.openmolecules.fx.viewer3d.interactions.V3DInteractionMeshAlgorithm;

public class RFInteractionVolumeBuilder implements InteractionVolumeBuilder {
	private static final float EXTEND_SURPLUS = (float)(1.80f	// max protein side VDW-radius
					+ RFInteractionList.MAX_REL_DISTANCE		// max distance between VDW-radii to consider RF-interactions
					+ 2.0 * RFKnowledgeBase.OFF_SLOPE_WIDTH		// zone ramping down frequency values
					+ V3DInteractionMeshAlgorithm.VOXEL_SIZE);	// padding

	private final StereoMolecule mMol;

	public RFInteractionVolumeBuilder(StereoMolecule mol) {
		mMol = mol;
	}

	@Override
	public float getExtend(int atom) {
		return VDWRadii.getVDWRadius(mMol.getAtomicNo(atom))+EXTEND_SURPLUS;
	}

	@Override
	public void buildVolume(float[] grid, int xSize, int ySize, int zSize, float xOffset, float yOffset, float zOffset, int atom, int pType) {
		RFKnowledgeBase knowledgeBase = RFKnowledgeBase.getInstance();

		InteractionGeometry geometry = new InteractionGeometry(mMol, atom, null);
		int pAtomicNo = RFProteinAtomClassifier.getDefaultInstance().getContactAtomicNoFromType(pType);
		double vdwSum = VDWRadii.getVDWRadius(mMol.getAtomicNo(atom)) + VDWRadii.getVDWRadius(pAtomicNo);
		double minDistance = vdwSum - 1.0;
		double maxDistance = vdwSum + RFInteractionList.MAX_REL_DISTANCE + 2.0 * RFKnowledgeBase.OFF_SLOPE_WIDTH;
		RFKnowledgeBase.DensityMapsWithDistances densityMap = knowledgeBase.getDensityMap(geometry.getType(), true);

		int gridIndex = 0;
		Coordinates remoteP =  new Coordinates(xOffset, yOffset, zOffset);
		for (int ix=0; ix<xSize; ix++) {
			remoteP.y = yOffset;
			for (int iy=0; iy<ySize; iy++) {
				remoteP.z = zOffset;
				for (int iz=0; iz<zSize; iz++) {
					if (densityMap != null) {
						double dx = Math.abs(mMol.getAtomX(atom) - remoteP.x);
						if (dx < maxDistance) {
							double dy = Math.abs(mMol.getAtomY(atom) - remoteP.y);
							if (dy < maxDistance) {
								double dz = Math.abs(mMol.getAtomZ(atom) - remoteP.z);
								if (dz < maxDistance) {
									double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);
									if (distance > minDistance && distance < maxDistance) {
										geometry.updateAngleAndTorsion(remoteP);
										double density = densityMap.getDensity(geometry.getAngle(), geometry.getTorsion(), distance - vdwSum);
										if (grid[gridIndex] < density)
											grid[gridIndex] = (float)density;
									}
								}
							}
						}
					}
					gridIndex++;
					remoteP.z += V3DInteractionMeshAlgorithm.VOXEL_SIZE;
				}
				remoteP.y += V3DInteractionMeshAlgorithm.VOXEL_SIZE;
			}
			remoteP.x += V3DInteractionMeshAlgorithm.VOXEL_SIZE;
		}
	}
}

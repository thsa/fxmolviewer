package org.openmolecules.render;

import com.actelion.research.calc.SingularValueDecomposition;
import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.chem.conf.VDWRadii;
import org.sunflow.core.shader.ColorProvider;

import java.awt.*;
import java.util.Arrays;

public class SunflowMoleculeBuilder extends SunflowPrimitiveBuilder implements MoleculeBuilder {
	public static final int OVERRIDE_MODE_ALL = 0;
	public static final int OVERRIDE_MODE_CARBON = 1;
	public static final int OVERRIDE_MODE_CARBON_AND_HYDROGEN = 2;
	public static final int DEFAULT_ATOM_MATERIAL = MATERIAL_GLASS;
	public static final int DEFAULT_BOND_MATERIAL = MATERIAL_SHINY;
	public static final int DEFAULT_SURFACE_MATERIAL = SURFACE_TRANSPARENT;

	private static final int ARGB_NONE = 0x12345678;

	private StereoMolecule mMol;
	private int mRenderMode,mAtomMaterial,mBondMaterial,mOverrideMode,mOverrideARGB;
	private boolean mFlipFeaturesToFront,mFlipXAndZ;
	private double mXShift,mYShift,mZShift;
	private double[][] mEigenVectorsLeft;
	private Coordinates mRotationCenter;


	public SunflowMoleculeBuilder() {
		this(DEFAULT_CAMERA_X, DEFAULT_CAMERA_Z);
		}

	public SunflowMoleculeBuilder(float cameraDistance, float fieldOfView) {
		this(cameraDistance, DEFAULT_CAMERA_X, DEFAULT_CAMERA_Z, fieldOfView);
		}

	public SunflowMoleculeBuilder(float cameraDistance, float cameraX, float cameraZ, float fieldOfView) {
		super(cameraDistance, cameraX, cameraZ, fieldOfView);
		mOverrideARGB = ARGB_NONE;
		mAtomMaterial = DEFAULT_ATOM_MATERIAL;
		mMaxY = -Float.MAX_VALUE;
		mMinY = Float.MAX_VALUE;
	}

	public void setRenderMode(int renderMode) {
		mRenderMode = renderMode;
	}

	public void setAtomMaterial(int material) {
		mAtomMaterial = material;
	}

	public void setBondMaterial(int material) {
		mBondMaterial = material;
	}

	public void setOverrideMode(int mode) {
		mOverrideMode = mode;
	}

	public void setOverrideColor(Color c) {
		mOverrideARGB = (c == null) ? ARGB_NONE : 0xFF000000 | c.getRGB();
	}

	/**
	 * @param conformer with coordinates already translated to scene
	 * @param rotateToOptimum
	 * @param moveAndZoomToOptimum
	 * @param surfaceSurplus -1 or, if a surface is present, the amount that the surface is larger than the van-der-Waals radius
	 */
	public void drawMolecule(Conformer conformer, boolean rotateToOptimum, boolean moveAndZoomToOptimum, double surfaceSurplus) {
		mMol = conformer.getMolecule();
		mMol.ensureHelperArrays(Molecule.cHelperNeighbours);
		if (rotateToOptimum) {
			rotateIntoView(conformer);
			flipFeaturesToFront(conformer);
		}

		if (moveAndZoomToOptimum)
			scaleAndCenterForCamera(conformer, getWidth(), getHeight(), rotateToOptimum, surfaceSurplus);

		for (int atom=0; atom<mMol.getAllAtoms(); atom++) {
			float radius = getAtomRadius(mMol, atom, (float)surfaceSurplus);
			if (mFloorZ > conformer.getZ(atom) - radius)
				mFloorZ = (float)conformer.getZ(atom) - radius;
			if (mMaxY< conformer.getY(atom))
				mMaxY = (float)conformer.getY(atom);
			if (mMinY> conformer.getY(atom))
				mMinY = (float)conformer.getY(atom);
		}

		MoleculeArchitect architect = new MoleculeArchitect(new CachingMoleculeBuilder(this));
		architect.setConstructionMode(mRenderMode);
		architect.buildMolecule(conformer);
	}

	public void createSurfaceShader(int material, Color color, ColorProvider cp, float transparency, int surfaceIndex) {
		switch (material) {
			case SURFACE_WIRES:
				float width = 0.02f / (mCameraDistance + (mMaxY + mMinY) / 2f);
				Color cw = color != null ? color : new Color(0xFF0080FF);
				setMyWireframeShader("ssw"+surfaceShaderID(cw, cp, surfaceIndex), cw, width, cp);
				break;
			case SURFACE_TRANSPARENT:
//				setGlassShader("ssg", new Color(0xFF80A0A0), 1.01f, 20f, new Color(0xFFC0A080));
				Color ct = color != null ? color : new Color(0xFF80C0FF);
				setMyTransparencyShader("sst"+surfaceShaderID(ct, cp, surfaceIndex), ct, transparency, cp);
				break;
			case SURFACE_OPAQUE:
				Color co = color != null ? color : new Color(0xFF40A0A0);
				setMyDiffuseShader("sso"+surfaceShaderID(co, cp, surfaceIndex), co, cp);
				break;
			case SURFACE_SHINY:
				Color cs = color != null ? color : new Color(0xFF40A0A0);
				setMyShinyDiffuseShader("sss"+surfaceShaderID(cs, cp, surfaceIndex), cs, 0.4f, cp);
				break;
			case SURFACE_GLASS:
				Color cg = color != null ? color : new Color(0xFFC0E0E0);
				setMyGlassShader("ssg"+surfaceShaderID(cg, cp, surfaceIndex), cg, 1.1f, cp);
				break;
		}
	}

	private String surfaceShaderID(Color c, ColorProvider cp, int surfaceIndex) {
//		return (cp == null) ? Integer.toHexString(c.getRGB()) : "Mol"+surfaceIndex;
		return Integer.toString(surfaceIndex);
	}

	/**
	 * If drawMolecule() was performed with rotateToOptimum==true or moveAndZoomToOptimum==true
	 * then this method applies those transformations to Coordinate c, which were applied to
	 * the molecule in order to optimize rotation, zoom and position.
	 * @param c array containing x,y,z of a 3D point in this order
	 * @param index points to x in the coordinate array c
	 */
	public void optimizeCoordinate(float[] c, int index) {
		if (mEigenVectorsLeft != null) {
			c[index]   -= (float)mRotationCenter.x;
			c[index+1] -= (float)mRotationCenter.y;
			c[index+2] -= (float)mRotationCenter.z;

			float[] temp = new float[3];
			for (int j=0; j<3; j++) {
				temp[j] += c[index]   * (float)mEigenVectorsLeft[0][j];
				temp[j] += c[index+1] * (float)mEigenVectorsLeft[1][j];
				temp[j] += c[index+2] * (float)mEigenVectorsLeft[2][j];
			}
			c[index]   = temp[0];
			c[index+1] = temp[2];
			c[index+2] = -temp[1];
		}

		if (mFlipFeaturesToFront) {
			c[index+1] *= -1f;
			c[index+2] *= -1f;
		}

		if (mFlipXAndZ) {
			float temp = -c[index];
			c[index] = c[index+2];
			c[index+2] = temp;
		}

		c[index]   += (float)mXShift;
		c[index+1] += (float)mYShift;
		c[index+2] += (float)mZShift;
	}

	@Override
	public void addAtomSphere(int role, Coordinates c, double radius, int argb) {
		if (mOverrideARGB != ARGB_NONE
				&& (mOverrideMode == OVERRIDE_MODE_ALL
				|| (mOverrideMode == OVERRIDE_MODE_CARBON && argb == MoleculeArchitect.getAtomARGB(6))
				|| (mOverrideMode == OVERRIDE_MODE_CARBON_AND_HYDROGEN
				&& (argb == MoleculeArchitect.getAtomARGB(1) || argb == MoleculeArchitect.getAtomARGB(6)))))
			argb = mOverrideARGB;

		// if is bond -> stickbond or dotted bond
		boolean useBondMaterial = ((role & MoleculeBuilder.ROLE_IS_BOND) != 0
				|| mRenderMode == MoleculeArchitect.CONSTRUCTION_MODE_STICKS
				|| mRenderMode == MoleculeArchitect.CONSTRUCTION_MODE_WIRES);
		int material = useBondMaterial? mBondMaterial : mAtomMaterial;
		if (mLastRGB != argb || mLastMaterial != material) {
			mLastRGB = argb;
			mLastMaterial = material;

			Color color = createColor(argb, material);
			createShader((useBondMaterial ?"cs":"ss") + Integer.toHexString(argb), color, material);
		}

		drawSphere("s"+(mSphereNo++), (float)c.x, (float)c.y, (float)c.z, (float)radius);
	}

	@Override
	public void addBondCylinder(int role, double radius, double length, Coordinates c, double rotationY, double rotationZ, int argb) {
		if (mOverrideARGB != ARGB_NONE
				&& (mOverrideMode == OVERRIDE_MODE_ALL
				|| (mOverrideMode == OVERRIDE_MODE_CARBON && argb == MoleculeArchitect.getAtomARGB(6))
				|| (mOverrideMode == OVERRIDE_MODE_CARBON_AND_HYDROGEN
				&& (argb == MoleculeArchitect.getAtomARGB(1) || argb == MoleculeArchitect.getAtomARGB(6)))))
			argb = mOverrideARGB;

		if (mLastRGB != argb || mLastMaterial != mBondMaterial) {
			mLastRGB = argb;
			mLastMaterial = mBondMaterial;

			Color color = createColor(argb, mBondMaterial);
			createShader("cs" + Integer.toHexString(argb), color, mBondMaterial);
		}

		drawCylinder("c" + mCylinderNo++, (float) radius, (float) radius, (float) length / 2f,
				(float) c.x, (float) c.y, (float) c.z, 0f, (float) rotationY, (float) rotationZ);
	}

	@Override
	public void addAtomCone(int role, double radius, double height, Coordinates c, double rotationY, double rotationZ, int argb) {
		if (mOverrideARGB != ARGB_NONE)
			argb = mOverrideARGB;

		if (mLastRGB != argb || mLastMaterial != mAtomMaterial) {
			mLastRGB = argb;
			mLastMaterial = mAtomMaterial;

			Color color = createColor(argb, mAtomMaterial);
			createShader("cs" + Integer.toHexString(argb), color, mAtomMaterial);
		}

		addConeMesh((float)radius, (float)height, c, (float)rotationY, (float)rotationZ);
	}

	private void scaleAndCenterForCamera(Conformer conformer, int width, int height, boolean optimizePerspective, double surfaceSurplus) {
		// calculate simple size in three dimensions
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double minZ = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		double maxZ = Double.NEGATIVE_INFINITY;
		for (int i=0; i<conformer.getSize(); i++) {
			if (minX > conformer.getX(i))
				minX = conformer.getX(i);
			if (maxX < conformer.getX(i))
				maxX = conformer.getX(i);
			if (minY > conformer.getY(i))
				minY = conformer.getY(i);
			if (maxY < conformer.getY(i))
				maxY = conformer.getY(i);
			if (minZ > conformer.getZ(i))
				minZ = conformer.getZ(i);
			if (maxZ < conformer.getZ(i))
				maxZ = conformer.getZ(i);
		}

		if (optimizePerspective) {
			// rotate if width < height
			mFlipXAndZ = (maxZ - minZ > maxX - minX);
			if (mFlipXAndZ) {
				double temp = minX;
				minX = minZ;
				minZ = -maxX;
				maxX = maxZ;
				maxZ = -temp;
				for (int i=0; i<conformer.getSize(); i++) {
					temp = -conformer.getX(i);
					conformer.setX(i, conformer.getZ(i));
					conformer.setZ(i, temp);
				}
			}
		}

		// center on all axes
		mXShift = -(minX + maxX) / 2.0;
		mYShift = -(minY + maxY) / 2.0;
		mZShift = -(minZ + maxZ) / 2.0;
		for (int i=0; i<conformer.getSize(); i++)
			conformer.getCoordinates(i).add(mXShift, mYShift, mZShift);

		// Stepwise optimize molecule location concerning x-,y- and z-axis
		// to center and scale molecule in camera perspective.
		double xshift = 0.0;
		double yshift = 0.0;
		double zshift = 0.0;
		double dy = maxY - minY;
		double maxPerspectiveX = 0.68;
		while(true) {
			minX = Double.POSITIVE_INFINITY;
			minZ = Double.POSITIVE_INFINITY;
			maxX = Double.NEGATIVE_INFINITY;
			maxZ = Double.NEGATIVE_INFINITY;
			for (int i=0; i<conformer.getSize(); i++) {
				double cameraDistance = mCameraDistance + conformer.getY(i) + yshift;
				double x = (conformer.getX(i)+xshift) / cameraDistance;
				double z = (conformer.getZ(i)+zshift) / cameraDistance;
				double r = getAtomRadius(mMol, i, (float)surfaceSurplus) / cameraDistance;
				if (minX > x-r)
					minX = x-r;
				if (maxX < x+r)
					maxX = x+r;
				if (minZ > z-r)
					minZ = z-r;
				if (maxZ < z+r)
					maxZ = z+r;
			}

			double dx = maxX - minX;
			double dz = maxZ - minZ;
			double factor = Math.min(maxPerspectiveX / dx, maxPerspectiveX * height / (dz * width));
			if (Math.abs(1.0-factor) < 0.01
					&& Math.abs((minX+maxX) / dx) < 0.02
					&& Math.abs((minZ+maxZ) / dz) < 0.02)
				break;

			xshift -= (minX + maxX) * (mCameraDistance + yshift - dy/2.0) / 2.0;
			zshift -= (minZ + maxZ) * (mCameraDistance + yshift - dy/2.0) / 2.0;
			yshift += (mCameraDistance + yshift - dy/2.0) * (1.0/factor - 1.0);
		}

		for (int i=0; i<conformer.getSize(); i++)
			conformer.getCoordinates(i).add(xshift, yshift, zshift);

		mXShift += xshift;
		mYShift += yshift;
		mZShift += zshift;
	}

	public void rotateIntoView(Conformer conformer) {
		mRotationCenter = new Coordinates();	// center of gravity
		for (int i=0; i<conformer.getSize(); i++)
			mRotationCenter.add(conformer.getCoordinates(i));
		mRotationCenter.scale(1.0/conformer.getSize());

		double[][] squareMatrix = new double[3][3];
		for (int i=0; i<conformer.getSize(); i++) {
			conformer.getCoordinates(i).sub(mRotationCenter);
			squareMatrix[0][0] += conformer.getX(i) * conformer.getX(i);
			squareMatrix[0][1] += conformer.getX(i) * conformer.getY(i);
			squareMatrix[0][2] += conformer.getX(i) * conformer.getZ(i);
			squareMatrix[1][0] += conformer.getY(i) * conformer.getX(i);
			squareMatrix[1][1] += conformer.getY(i) * conformer.getY(i);
			squareMatrix[1][2] += conformer.getY(i) * conformer.getZ(i);
			squareMatrix[2][0] += conformer.getZ(i) * conformer.getX(i);
			squareMatrix[2][1] += conformer.getZ(i) * conformer.getY(i);
			squareMatrix[2][2] += conformer.getZ(i) * conformer.getZ(i);
		}

		SingularValueDecomposition svd = new SingularValueDecomposition(squareMatrix, null, null);
		mEigenVectorsLeft = svd.getU();

		double[] temp = new double[3];
		for (int i=0; i<conformer.getSize(); i++)
			rotateToOptimum(conformer.getCoordinates(i), temp);
	}

	private void rotateToOptimum(Coordinates c, double[] temp) {
		Arrays.fill(temp, 0);
		for (int j=0; j<3; j++) {
			temp[j] += c.x * mEigenVectorsLeft[0][j];
			temp[j] += c.y * mEigenVectorsLeft[1][j];
			temp[j] += c.z * mEigenVectorsLeft[2][j];
		}
		c.set(temp[0], temp[2], -temp[1]);
	}

	private void flipFeaturesToFront(Conformer conformer) {
		double carbonMeanY = 0f;
		double heteroMeanY = 0f;
		int carbonCount = 0;
		int heteroCount = 0;
		for (int atom=0; atom<mMol.getAtoms(); atom++) {
			if (mMol.getAtomicNo(atom) == 6) {
				carbonMeanY += conformer.getY(atom);
				carbonCount++;
			}
			else {
				heteroMeanY += conformer.getY(atom);
				heteroCount++;
			}
		}
		mFlipFeaturesToFront = (heteroMeanY / heteroCount < carbonMeanY / carbonCount);
		if (mFlipFeaturesToFront) {
			for (int i=0; i<conformer.getSize(); i++) {
				conformer.setY(i, conformer.getY(i) * -1);
				conformer.setZ(i, conformer.getZ(i) * -1);
			}
		}
	}

	private float getAtomRadius(StereoMolecule mol, int atom, float surfaceSurplus) {
		return (surfaceSurplus >= 0) ? VDWRadii.getVDWRadius(mol.getAtomicNo(atom)) + surfaceSurplus
				: (mRenderMode == MoleculeArchitect.CONSTRUCTION_MODE_BALLS) ? VDWRadii.getVDWRadius(mol.getAtomicNo(atom))
				: (mRenderMode == MoleculeArchitect.CONSTRUCTION_MODE_BALL_AND_STICKS) ? VDWRadii.getVDWRadius(mol.getAtomicNo(atom)) / 4f
				: (mRenderMode == MoleculeArchitect.CONSTRUCTION_MODE_STICKS) ? (float)MoleculeArchitect.STICK_SBOND_RADIUS
				: (float)MoleculeArchitect.WIRE_SBOND_RADIUS;
	}
}

package org.openmolecules.render;

import com.actelion.research.calc.SingularValueDecomposition;
import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.chem.conf.VDWRadii;
import javafx.scene.shape.TriangleMesh;
import org.sunflow.SunflowAPI;
import org.sunflow.core.shader.ColorProvider;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

import java.awt.*;
import java.util.Arrays;

public class SunflowMoleculeBuilder extends SunflowAPIAPI implements MoleculeBuilder {
	public static final float DEFAULT_CAMERA_DISTANCE = 12.5f;
	public static final float DEFAULT_CAMERA_X = 0f;
	public static final float DEFAULT_CAMERA_Z = 0.001f;	// exactly 0f rotates the camera for some reason...
	public static final float DEFAULT_FIELD_OF_VIEW = 40f;

	public static final Color DEFAULT_BACKGROUND = new Color(0.7f, 0.7f, 0.7f);
	public static final Color DEFAULT_FLOOR_COLOR = new Color(0.8f, 0.7f, 0.6f);

	public static final boolean DEFAULT_USE_BACKGROUND = false;
	public static final boolean DEFAULT_USE_FLOOR = true;
	public static final boolean DEFAULT_GLOSSY_FLOOR = false;
	public static final boolean DEFAULT_DEPTH_BLURRING = false;

	public static final String[] MATERIAL_TEXT = { "matt", "glossy", "glass", "mirror" };
	public static final int MATERIAL_MATT = 0;
	public static final int MATERIAL_SHINY = 1;
	public static final int MATERIAL_GLASS = 2;
	public static final int MATERIAL_MIRROR = 3;

	public static final String[] SURFACE_TEXT = { "wires", "transparent", "opaque", "glossy", "glass" };
	public static final int SURFACE_WIRES = 0;
	public static final int SURFACE_TRANSPARENT = 1;
	public static final int SURFACE_OPAQUE = 2;
	public static final int SURFACE_SHINY = 3;
	public static final int SURFACE_GLASS = 4;

	public static final int DEFAULT_ATOM_MATERIAL = MATERIAL_GLASS;
	public static final int DEFAULT_BOND_MATERIAL = MATERIAL_SHINY;
	public static final int DEFAULT_SURFACE_MATERIAL = SURFACE_TRANSPARENT;

	private static final int ARGB_NONE = 0x12345678;

	private float	mCameraDistance,mCameraX, mCameraZ,mFieldOfView, mMaxAtomY, mMinAtomY, mBrightness;
	private double  mFloorZ,mXShift,mYShift,mZShift;
	private int     mRenderMode,mMeshNo,mCylinderNo,mLastRGB,mLastMaterial,mSphereNo,mAtomMaterial,mBondMaterial,mOverrideARGB;
	private boolean mFlipFeaturesToFront,mFlipXAndZ,mIsGlossyFloor;
	private Color   mBackgroundColor,mFloorColor;
	private StereoMolecule mMol;
	private Coordinates mRotationCenter;
	private double[][] mEigenVectorsLeft;

	public SunflowMoleculeBuilder() {
		this(DEFAULT_CAMERA_DISTANCE, DEFAULT_FIELD_OF_VIEW);
		}

	public SunflowMoleculeBuilder(float cameraDistance, float fieldOfView) {
		this(cameraDistance, DEFAULT_CAMERA_X, DEFAULT_CAMERA_Z, fieldOfView);
		}

	public SunflowMoleculeBuilder(float cameraDistance, float cameraX, float cameraZ, float fieldOfView) {
		mCameraDistance = cameraDistance;
		mCameraX = cameraX;
		mCameraZ = cameraZ;
		mYShift = 0f;
		mFieldOfView = fieldOfView;
		mMaxAtomY = -Float.MAX_VALUE;
		mMinAtomY = Float.MAX_VALUE;
		mBrightness = 1f;

		mOverrideARGB = ARGB_NONE;
		mAtomMaterial = DEFAULT_ATOM_MATERIAL;
		mBackgroundColor = DEFAULT_USE_BACKGROUND ? DEFAULT_BACKGROUND : null;
		mFloorColor = DEFAULT_USE_FLOOR ? DEFAULT_FLOOR_COLOR : null;
		mIsGlossyFloor = DEFAULT_GLOSSY_FLOOR;
		}

/*	public void setRandomSeed(long seed) {
	  	mRandomSeed = seed;
		}*/

	public void setOverrideColor(Color c) {
		mOverrideARGB = (c == null) ? ARGB_NONE : 0xFF000000 | c.getRGB();
		}

	public void setBackgroundColor(Color c) {
		mBackgroundColor = c;
		}

	/**
	 * @param r -1 to remove background
	 * @param g -1 to remove background
	 * @param b -1 to remove background
	 */
	public void setBackgroundColor(float r, float g, float b) {
		mBackgroundColor = (r < 0) ? null : new Color(r, g, b);
		}

	public void setBrightness(float b) {
		mBrightness = b;
		}

	public void setFloorColor(Color c) {
		mFloorColor = c;
		}

	/**
	 * @param r -1 to remove background
	 * @param g -1 to remove background
	 * @param b -1 to remove background
	 */
	public void setFloorColor(float r, float g, float b) {
		mFloorColor = (r < 0) ? null : new Color(r, g, b);
		}

	public void setGlossyFloor(boolean isGlossy) {
		mIsGlossyFloor = isGlossy;
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

	public void initializeScene(int width, int height) {
		setWidth(width);
		setHeight(height);
		mFloorZ = Float.MAX_VALUE;

//		setPathTracingGIEngine(64);
		setAmbientOcclusionEngine(Color.LIGHT_GRAY, Color.DARK_GRAY, 64, 1f);

//		caustics needs a photon source, e.g. a mesh light (sun light doesn't suffice)
/*	    getAPI().parameter("caustics.emit", 10000000);
	    getAPI().parameter("caustics", "kd");
	    getAPI().parameter("caustics.gather", 64);
	    getAPI().parameter("caustics.radius", 0.5f);
*/
		getAPI().parameter("bucket.size", 64);

		// this doubles the depth values from 4 to 8
//		getAPI().parameter("depths.diffuse", 2);
		getAPI().parameter("depths.reflection", 10);
		getAPI().parameter("depths.refraction", 10);

		getAPI().options(SunflowAPI.DEFAULT_OPTIONS);    // done to process above parameters

		// construct a sun light with custom brightness
		getAPI().parameter("up", new Vector3(0, 0, 1));
		getAPI().parameter("east", new Vector3(0, 1, 0));
		getAPI().parameter("sundir", new Vector3(1, -1, 0.5f));
		getAPI().parameter("brightness", mBrightness * (mFloorColor==null ? 0.5f : 1f));	// if we have no floor and if ground.extendsky==true, we need to darken
		getAPI().parameter("turbidity", 2f);	// smaller values that 2 give strange results
		getAPI().parameter("samples", 16);
		getAPI().parameter("ground.color", COLORSPACE_SRGB_NONLINEAR, 0.8f, 0.8f, 0.8f); // relevant only if !ground.extendsky
//		getAPI().parameter("ground.extendsky", true);
		getAPI().light("mySun", LIGHT_SUNSKY);

//        setSunSkyLight("sun");

//	    setDirectionalLight("light", new Point3(-10f, 5f, 10f), new Vector3(10f, -5f, -10f), 1f, Color.white, 100f);
//		setPointLight("pointLight", new Point3(-10f, 5f, 10f),Color.WHITE, 100000f);
//		setSphereLight("sphereLight", new Point3(-10f, 5f, 10f), Color.white, 1000f, 16, 2f);
//		createMeshLight();
		}

	/**
	 * @param focus relative value 0.0 (front atom) to 1.0 (rear atom) or -1.0 for no depth blurring
	 */
	public void finalizeScene(float focus) {
		setCameraPosition(mCameraX, -mCameraDistance, mCameraZ);
		setCameraTarget(0, 0, 0);

		float aspect = (float)getWidth()/(float)getHeight();
		if (focus != -1f && mMinAtomY != Float.MAX_VALUE)
			setThinlensCamera("thinLensCamera", mFieldOfView, aspect, 0f, 0f,
					mCameraDistance+mMinAtomY+focus*(mMaxAtomY-mMinAtomY), 0.15f, 0, 0f);
		else
			setThinlensCamera("thinLensCamera", mFieldOfView, aspect);

		if (mBackgroundColor != null) {
			setDiffuseShader("blackBackgroundShader", mBackgroundColor);
			drawPlane("blackBackground", new Point3(0f, 1000f, 0f), new Vector3(0f, 1f, 0f));
		}
		if (mFloorColor != null) {
			if (mIsGlossyFloor)
				setShinyDiffuseShader("groundShader", mFloorColor, 0.5f);
			else
				setDiffuseShader("groundShader", mFloorColor);

			drawPlane("ground", new Point3(0f, 0f, (float)mFloorZ-0.01f), new Vector3(0f, 0f, 1f));
			}
		}

	/**
	 * @param conformer
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
			double radius = getAtomRadius(mMol, atom, surfaceSurplus);
			if (mFloorZ > conformer.getZ(atom) - radius)
				mFloorZ = conformer.getZ(atom) - radius;
			if (mMaxAtomY < conformer.getY(atom))
				mMaxAtomY = (float)conformer.getY(atom);
			if (mMinAtomY > conformer.getY(atom))
				mMinAtomY = (float)conformer.getY(atom);
		}

		MoleculeArchitect architect = new MoleculeArchitect(new CachingMoleculeBuilder(this));
		architect.setConstructionMode(mRenderMode);
		architect.buildMolecule(conformer);
		}

	public void createSurfaceShader(int material, Color color, ColorProvider cp, float transparency, int surfaceIndex) {
		switch (material) {
			case SURFACE_WIRES:
				float width = 0.02f / (mCameraDistance + (mMaxAtomY + mMinAtomY) / 2f);
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
			c[index]   -= mRotationCenter.x;
			c[index+1] -= mRotationCenter.y;
			c[index+2] -= mRotationCenter.z;

			float[] temp = new float[3];
			for (int j=0; j<3; j++) {
				temp[j] += c[index]   * mEigenVectorsLeft[0][j];
				temp[j] += c[index+1] * mEigenVectorsLeft[1][j];
				temp[j] += c[index+2] * mEigenVectorsLeft[2][j];
			}
			c[index]   = temp[0];
			c[index+1] = temp[1];
			c[index+2] = -temp[2];
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

		c[index]   += mXShift;
		c[index+1] += mYShift;
		c[index+2] += mZShift;
		}

	private void createMeshLight() {
		float[] MESH1_POINTS = {-1.79750967026f, -6.22097349167f, 5.70054674149f,
								-2.28231739998f, -7.26064729691f, 4.06224298477f,
								-4.09493303299f, -6.41541051865f, 4.06224298477f,
								-3.61012482643f, -5.37573671341f, 5.70054721832f };
		int[] MESH1_TRIANGLES = {0, 1, 2, 0, 2, 3};
		
		float[] MESH2_POINTS = {-4.25819396973f, -4.8784570694f, 5.70054674149f,
								-5.13696432114f, -5.61583280563f, 4.06224298477f,
								-6.422539711f, -4.08374404907f, 4.06224298477f,
								-5.54376888275f, -3.34636831284f, 5.70054721832f };
		int[] MESH2_TRIANGLES = {0, 1, 2, 0, 2, 3};

		drawMeshLight("meshlight1", new Color(255, 255, 255), 15f, 8, MESH1_POINTS, MESH1_TRIANGLES);
		drawMeshLight("meshlight2", new Color(255, 255, 255), 15f, 8, MESH2_POINTS, MESH2_TRIANGLES);
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
				double r = getAtomRadius(mMol, i, surfaceSurplus) / cameraDistance;
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
		c.set(temp[0], temp[1], -temp[2]);
		}

	private void flipFeaturesToFront(Conformer conformer) {
		float carbonMeanY = 0f;
		float heteroMeanY = 0f;
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

	private double getAtomRadius(StereoMolecule mol, int atom, double surfaceSurplus) {
		return (surfaceSurplus >= 0) ? VDWRadii.VDW_RADIUS[mol.getAtomicNo(atom)] + surfaceSurplus
			 : (mRenderMode == MoleculeArchitect.CONSTRUCTION_MODE_BALLS) ? VDWRadii.VDW_RADIUS[mol.getAtomicNo(atom)]
			 : (mRenderMode == MoleculeArchitect.CONSTRUCTION_MODE_BALL_AND_STICKS) ? VDWRadii.VDW_RADIUS[mol.getAtomicNo(atom)] / 4f
			 : (mRenderMode == MoleculeArchitect.CONSTRUCTION_MODE_STICKS) ? MoleculeArchitect.STICK_SBOND_RADIUS
			 : MoleculeArchitect.WIRE_SBOND_RADIUS;
		}

	@Override
	public void init() {
		mSphereNo = 0;
		mCylinderNo = 0;
		mLastRGB = 0;
		mLastMaterial = -1;
		}

	@Override
	public void addSphere(int role, Coordinates c, double radius, int argb) {
		if (mOverrideARGB != ARGB_NONE)
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
	public void addCylinder(int role, double radius, double length, Coordinates c, double rotationY, double rotationZ, int argb) {
		if (mOverrideARGB != ARGB_NONE)
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
	public void addCone(int role, double radius, double height, Coordinates c, double rotationY, double rotationZ, int argb) {
		if (mOverrideARGB != ARGB_NONE)
			argb = mOverrideARGB;

		if (mLastRGB != argb || mLastMaterial != mAtomMaterial) {
			mLastRGB = argb;
			mLastMaterial = mAtomMaterial;

			Color color = createColor(argb, mAtomMaterial);
			createShader("cs" + Integer.toHexString(argb), color, mAtomMaterial);
			}

		final int DIVISIONS = 36;
		float[] vertexes = new float[2+DIVISIONS];
		int[] triangles = new int[2*DIVISIONS];
		double segmentAngle = 2.0 * Math.PI / DIVISIONS;

		int vertex = 0;
		int triangle = 0;

		vertexes[vertex++] = 0;
		vertexes[vertex++] = 0;
		vertexes[vertex++] = -(float)(height/(2*radius));

		double angle = 0;
		for(int i=0; i<DIVISIONS; i++) {
			vertexes[vertex++] = (float)(radius * Math.cos(angle));
			vertexes[vertex++] = (float)(radius * Math.sin(angle));
			vertexes[vertex++] = -(float)(height/(2*radius));
			angle += segmentAngle;
			}

		vertexes[vertex++] = 0;
		vertexes[vertex++] = 0;
		vertexes[vertex++] = (float)(height/(2*radius));

		for(int i=0; i<DIVISIONS; i++) {
			int next = (i == DIVISIONS-1) ? 1 : i+2;
			triangles[triangle++] = i+1;
			triangles[triangle++] = 0;
			triangles[triangle++] = next;

			triangles[triangle++] = i+1;
			triangles[triangle++] = next;
			triangles[triangle++] = DIVISIONS+1;
			}

		drawMesh("m" + mMeshNo++, vertexes, triangles, (float) radius,
				(float) c.x, (float) c.y, (float) c.z, 0f, (float) rotationY, (float) rotationZ);
	}

	/**
	 * For matt and shiny materials all atom colors are reduced somewhat in brightness.
	 * @param argb
	 * @param material
	 * @return
	 */
	private Color createColor(int argb, int material) {
		if (material != MATERIAL_GLASS && material != MATERIAL_MIRROR) {
			float f = (material == MATERIAL_SHINY) ? 0.6f : 0.8f;
			int r = 0x00FF0000 & (int)(f * (0x00FF0000 & argb));
			int g = 0x0000FF00 & (int)(f * (0x0000FF00 & argb));
			int b = 0x000000FF & (int)(f * (0x000000FF & argb));
			argb = 0xFF000000 + r + g + b;
			}

		return new Color(argb);
		}

	private void createShader(String name, Color color, int material) {
		switch (material) {
		case MATERIAL_MATT:
			setDiffuseShader(name, color);
			break;
		case MATERIAL_SHINY:
			setShinyDiffuseShader(name, color, 0.6f);
			break;
		case MATERIAL_GLASS:
			setGlassShader(name, color, 2.1f);
			break;
		case MATERIAL_MIRROR:
			setMirrorShader(name, color);
			break;
			}
		}

	@Override
	public void done() {
		}
	}

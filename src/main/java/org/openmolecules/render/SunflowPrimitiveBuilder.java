package org.openmolecules.render;

import com.actelion.research.chem.Coordinates;
import org.sunflow.SunflowAPI;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

import java.awt.*;

public class SunflowPrimitiveBuilder extends SunflowAPIAPI implements PrimitiveBuilder {
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
	private static final int DEFAULT_PRIMITIVE_MATERIAL = MATERIAL_SHINY;

	public static final String[] SURFACE_TEXT = { "wires", "transparent", "opaque", "glossy", "glass" };
	public static final int SURFACE_WIRES = 0;
	public static final int SURFACE_TRANSPARENT = 1;
	public static final int SURFACE_OPAQUE = 2;
	public static final int SURFACE_SHINY = 3;
	public static final int SURFACE_GLASS = 4;

	protected final float mCameraDistance;
	private final float mCameraX,mCameraZ,mFieldOfView;
	private float mBrightness;
	protected float mMaxY,mMinY,mFloorZ;
	private int mMaterial,mMeshNo;
	protected int mCylinderNo,mLastRGB,mLastMaterial,mSphereNo;
	private boolean mIsGlossyFloor;
	private Color   mBackgroundColor,mFloorColor;

	public SunflowPrimitiveBuilder() {
		this(DEFAULT_CAMERA_DISTANCE, DEFAULT_FIELD_OF_VIEW);
		}

	public SunflowPrimitiveBuilder(float cameraDistance, float fieldOfView) {
		this(cameraDistance, DEFAULT_CAMERA_X, DEFAULT_CAMERA_Z, fieldOfView);
		}

	public SunflowPrimitiveBuilder(float cameraDistance, float cameraX, float cameraZ, float fieldOfView) {
		mCameraDistance = cameraDistance;
		mCameraX = cameraX;
		mCameraZ = cameraZ;
		mFieldOfView = fieldOfView;
		mBrightness = 1f;

		mMaterial = DEFAULT_PRIMITIVE_MATERIAL;
		mBackgroundColor = DEFAULT_USE_BACKGROUND ? DEFAULT_BACKGROUND : null;
		mFloorColor = DEFAULT_USE_FLOOR ? DEFAULT_FLOOR_COLOR : null;
		mIsGlossyFloor = DEFAULT_GLOSSY_FLOOR;
		}

/*	public void setRandomSeed(long seed) {
	  	mRandomSeed = seed;
		}*/

	public void setPrimitiveMaterial(int material) {
		mMaterial = material;
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
		if (focus != -1f && mMinY != Float.MAX_VALUE)
			setThinlensCamera("thinLensCamera", mFieldOfView, aspect, 0f, 0f,
					mCameraDistance+mMinY+focus*(mMaxY-mMinY), 0.15f, 0, 0f);
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

	@Override
	public void init() {
		mSphereNo = 0;
		mCylinderNo = 0;
		mLastRGB = 0;
		mLastMaterial = -1;
		}

	@Override
	public void done() {}

	public Object addSphere(Coordinates c, double radius, int argb, int divisions) {
		if (mLastRGB != argb || mLastMaterial != mMaterial) {
			mLastRGB = argb;
			mLastMaterial = mMaterial;

			Color color = createColor(argb, mMaterial);
			createShader(("ps") + Integer.toHexString(argb), color, mMaterial);
		}

		drawSphere("s"+(mSphereNo++), (float)c.x, (float)c.y, (float)c.z, (float)radius);
		return null;
	}

	public Object addCylinder(double radius, double length, Coordinates c, double rotationY, double rotationZ, int argb, int divisions) {
		if (mLastRGB != argb || mLastMaterial != mMaterial) {
			mLastRGB = argb;
			mLastMaterial = mMaterial;
			Color color = createColor(argb, mMaterial);
			createShader("ps" + Integer.toHexString(argb), color, mMaterial);
		}

		drawCylinder("c" + mCylinderNo++, (float) radius, (float) radius, (float) length / 2f,
				(float) c.x, (float) c.y, (float) c.z, 0f, (float) rotationY, (float) rotationZ);
		return null;
	}

	public Object addCone(double radius, double height, Coordinates c, double rotationY, double rotationZ, int argb, int divisions) {
		if (mLastRGB != argb || mLastMaterial != mMaterial) {
			mLastRGB = argb;
			mLastMaterial = mMaterial;

			Color color = createColor(argb, mMaterial);
			createShader("ps" + Integer.toHexString(argb), color, mMaterial);
		}

		addConeMesh((float)radius, (float)height, c, (float)rotationY, (float)rotationZ);
		return null;
	}

	protected void addConeMesh(float radius, float height, Coordinates c, float rotationY, float rotationZ) {
		final int DIVISIONS = 36;
		float[] vertexes = new float[2+DIVISIONS];
		int[] triangles = new int[2*DIVISIONS];
		double segmentAngle = 2.0 * Math.PI / DIVISIONS;

		int vertex = 0;
		int triangle = 0;

		vertexes[vertex++] = 0;
		vertexes[vertex++] = 0;
		vertexes[vertex++] = -height/(2*radius);

		double angle = 0;
		for(int i=0; i<DIVISIONS; i++) {
			vertexes[vertex++] = radius * (float)Math.cos(angle);
			vertexes[vertex++] = radius * (float)Math.sin(angle);
			vertexes[vertex++] = -height/(2*radius);
			angle += segmentAngle;
		}

		vertexes[vertex++] = 0;
		vertexes[vertex++] = 0;
		vertexes[vertex++] = height/(2*radius);

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
				(float) c.x, (float) c.y, (float) c.z, 0f, rotationY, rotationZ);
	}

	/**
	 * For matt and shiny materials all atom colors are reduced somewhat in brightness.
	 * @param argb
	 * @param material
	 * @return
	 */
	protected Color createColor(int argb, int material) {
		if (material != MATERIAL_GLASS && material != MATERIAL_MIRROR) {
			float f = (material == MATERIAL_SHINY) ? 0.6f : 0.8f;
			int r = 0x00FF0000 & (int)(f * (0x00FF0000 & argb));
			int g = 0x0000FF00 & (int)(f * (0x0000FF00 & argb));
			int b = 0x000000FF & (int)(f * (0x000000FF & argb));
			argb = 0xFF000000 + r + g + b;
			}

		return new Color(argb);
		}

	protected void createShader(String name, Color color, int material) {
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
	}

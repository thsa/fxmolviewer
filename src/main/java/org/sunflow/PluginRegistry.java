package org.sunflow;

import org.sunflow.core.*;
import org.sunflow.core.accel.BoundingIntervalHierarchy;
import org.sunflow.core.accel.KDTree;
import org.sunflow.core.accel.NullAccelerator;
import org.sunflow.core.accel.UniformGrid;
import org.sunflow.core.bucket.*;
import org.sunflow.core.camera.FisheyeLens;
import org.sunflow.core.camera.PinholeLens;
import org.sunflow.core.camera.SphericalLens;
import org.sunflow.core.camera.ThinLens;
import org.sunflow.core.filter.*;
import org.sunflow.core.gi.*;
import org.sunflow.core.light.*;
import org.sunflow.core.modifiers.BumpMappingModifier;
import org.sunflow.core.modifiers.NormalMapModifier;
import org.sunflow.core.modifiers.PerlinModifier;
import org.sunflow.core.parser.*;
import org.sunflow.core.photonmap.CausticPhotonMap;
import org.sunflow.core.photonmap.GlobalPhotonMap;
import org.sunflow.core.photonmap.GridPhotonMap;
import org.sunflow.core.primitive.*;
import org.sunflow.core.renderer.BucketRenderer;
import org.sunflow.core.renderer.MultipassRenderer;
import org.sunflow.core.renderer.ProgressiveRenderer;
import org.sunflow.core.renderer.SimpleRenderer;
import org.sunflow.core.shader.*;
import org.sunflow.core.tesselatable.BezierMesh;
import org.sunflow.core.tesselatable.FileMesh;
import org.sunflow.core.tesselatable.Gumbo;
import org.sunflow.core.tesselatable.Teapot;
import org.sunflow.image.BitmapReader;
import org.sunflow.image.BitmapWriter;
import org.sunflow.image.readers.*;
import org.sunflow.image.writers.*;
import org.sunflow.system.Plugins;

/**
 * This class acts as the central repository for all user extensible types in
 * Sunflow, even built-in types are registered here. This class is static so
 * that new plugins may be reused by an application across several render
 * scenes.
 */
public final class PluginRegistry {
    // base types - needed by SunflowAPI
    public static final Plugins<PrimitiveList> primitivePlugins = new Plugins<PrimitiveList>(PrimitiveList.class);
    public static final Plugins<Tesselatable> tesselatablePlugins = new Plugins<Tesselatable>(Tesselatable.class);
    public static final Plugins<Shader> shaderPlugins = new Plugins<Shader>(Shader.class);
    public static final Plugins<Modifier> modifierPlugins = new Plugins<Modifier>(Modifier.class);
    public static final Plugins<LightSource> lightSourcePlugins = new Plugins<LightSource>(LightSource.class);
    public static final Plugins<CameraLens> cameraLensPlugins = new Plugins<CameraLens>(CameraLens.class);

    // advanced types - used inside the Sunflow core
    public static final Plugins<AccelerationStructure> accelPlugins = new Plugins<AccelerationStructure>(AccelerationStructure.class);
    public static final Plugins<BucketOrder> bucketOrderPlugins = new Plugins<BucketOrder>(BucketOrder.class);
    public static final Plugins<Filter> filterPlugins = new Plugins<Filter>(Filter.class);
    public static final Plugins<GIEngine> giEnginePlugins = new Plugins<GIEngine>(GIEngine.class);
    public static final Plugins<CausticPhotonMapInterface> causticPhotonMapPlugins = new Plugins<CausticPhotonMapInterface>(CausticPhotonMapInterface.class);
    public static final Plugins<GlobalPhotonMapInterface> globalPhotonMapPlugins = new Plugins<GlobalPhotonMapInterface>(GlobalPhotonMapInterface.class);
    public static final Plugins<ImageSampler> imageSamplerPlugins = new Plugins<ImageSampler>(ImageSampler.class);
    public static final Plugins<SceneParser> parserPlugins = new Plugins<SceneParser>(SceneParser.class);
    public static final Plugins<BitmapReader> bitmapReaderPlugins = new Plugins<BitmapReader>(BitmapReader.class);
    public static final Plugins<BitmapWriter> bitmapWriterPlugins = new Plugins<BitmapWriter>(BitmapWriter.class);

    // Register all plugins on startup:
    static {
        // primitives
        primitivePlugins.registerPlugin("triangle_mesh", TriangleMesh.class);
        primitivePlugins.registerPlugin("sphere", Sphere.class);
        primitivePlugins.registerPlugin("cylinder", Cylinder.class);
        primitivePlugins.registerPlugin("box", Box.class);
        primitivePlugins.registerPlugin("banchoff", BanchoffSurface.class);
        primitivePlugins.registerPlugin("hair", Hair.class);
        primitivePlugins.registerPlugin("julia", JuliaFractal.class);
        primitivePlugins.registerPlugin("particles", ParticleSurface.class);
        primitivePlugins.registerPlugin("plane", Plane.class);
        primitivePlugins.registerPlugin("quad_mesh", QuadMesh.class);
        primitivePlugins.registerPlugin("torus", Torus.class);
        primitivePlugins.registerPlugin("background", Background.class);
        primitivePlugins.registerPlugin("sphereflake", SphereFlake.class);
    }

    static {
        // tesslatable
        tesselatablePlugins.registerPlugin("bezier_mesh", BezierMesh.class);
        tesselatablePlugins.registerPlugin("file_mesh", FileMesh.class);
        tesselatablePlugins.registerPlugin("gumbo", Gumbo.class);
        tesselatablePlugins.registerPlugin("teapot", Teapot.class);
    }

    static {
        // shaders
        shaderPlugins.registerPlugin("ambient_occlusion", AmbientOcclusionShader.class);
        shaderPlugins.registerPlugin("constant", ConstantShader.class);
        shaderPlugins.registerPlugin("diffuse", DiffuseShader.class);
        shaderPlugins.registerPlugin("glass", GlassShader.class);
        shaderPlugins.registerPlugin("mirror", MirrorShader.class);
        shaderPlugins.registerPlugin("phong", PhongShader.class);
        shaderPlugins.registerPlugin("shiny_diffuse", ShinyDiffuseShader.class);
        shaderPlugins.registerPlugin("uber", UberShader.class);
        shaderPlugins.registerPlugin("ward", AnisotropicWardShader.class);
        shaderPlugins.registerPlugin("wireframe", WireframeShader.class);
        shaderPlugins.registerPlugin("myglass", MyGlassShader.class);
        shaderPlugins.registerPlugin("mydiffuse", MyDiffuseShader.class);
        shaderPlugins.registerPlugin("mywireframe", MyWireframeShader.class);
        shaderPlugins.registerPlugin("mytransparency", MyTransparencyShader.class);
        shaderPlugins.registerPlugin("myshiny_diffuse", MyShinyDiffuseShader.class);

        // textured shaders
        shaderPlugins.registerPlugin("textured_ambient_occlusion", TexturedAmbientOcclusionShader.class);
        shaderPlugins.registerPlugin("textured_diffuse", TexturedDiffuseShader.class);
        shaderPlugins.registerPlugin("textured_phong", TexturedPhongShader.class);
        shaderPlugins.registerPlugin("textured_shiny_diffuse", TexturedShinyDiffuseShader.class);
        shaderPlugins.registerPlugin("textured_ward", TexturedWardShader.class);

        // preview shaders
        shaderPlugins.registerPlugin("quick_gray", QuickGrayShader.class);
        shaderPlugins.registerPlugin("simple", SimpleShader.class);
        shaderPlugins.registerPlugin("show_normals", NormalShader.class);
        shaderPlugins.registerPlugin("show_uvs", UVShader.class);
        shaderPlugins.registerPlugin("show_instance_id", IDShader.class);
        shaderPlugins.registerPlugin("show_primitive_id", PrimIDShader.class);
        shaderPlugins.registerPlugin("view_caustics", ViewCausticsShader.class);
        shaderPlugins.registerPlugin("view_global", ViewGlobalPhotonsShader.class);
        shaderPlugins.registerPlugin("view_irradiance", ViewIrradianceShader.class);
    }

    static {
        // modifiers
        modifierPlugins.registerPlugin("bump_map", BumpMappingModifier.class);
        modifierPlugins.registerPlugin("normal_map", NormalMapModifier.class);
        modifierPlugins.registerPlugin("perlin", PerlinModifier.class);
    }

    static {
        // light sources
        lightSourcePlugins.registerPlugin("directional", DirectionalSpotlight.class);
        lightSourcePlugins.registerPlugin("ibl", ImageBasedLight.class);
        lightSourcePlugins.registerPlugin("point", PointLight.class);
        lightSourcePlugins.registerPlugin("sphere", SphereLight.class);
        lightSourcePlugins.registerPlugin("sunsky", SunSkyLight.class);
        lightSourcePlugins.registerPlugin("triangle_mesh", TriangleMeshLight.class);
        lightSourcePlugins.registerPlugin("cornell_box", CornellBox.class);
    }

    static {
        // camera lenses
        cameraLensPlugins.registerPlugin("pinhole", PinholeLens.class);
        cameraLensPlugins.registerPlugin("thinlens", ThinLens.class);
        cameraLensPlugins.registerPlugin("fisheye", FisheyeLens.class);
        cameraLensPlugins.registerPlugin("spherical", SphericalLens.class);
    }

    static {
        // accels
        accelPlugins.registerPlugin("bih", BoundingIntervalHierarchy.class);
        accelPlugins.registerPlugin("kdtree", KDTree.class);
        accelPlugins.registerPlugin("null", NullAccelerator.class);
        accelPlugins.registerPlugin("uniformgrid", UniformGrid.class);
    }

    static {
        // bucket orders
        bucketOrderPlugins.registerPlugin("column", ColumnBucketOrder.class);
        bucketOrderPlugins.registerPlugin("diagonal", DiagonalBucketOrder.class);
        bucketOrderPlugins.registerPlugin("hilbert", HilbertBucketOrder.class);
        bucketOrderPlugins.registerPlugin("random", RandomBucketOrder.class);
        bucketOrderPlugins.registerPlugin("row", RowBucketOrder.class);
        bucketOrderPlugins.registerPlugin("spiral", SpiralBucketOrder.class);
    }

    static {
        // filters
        filterPlugins.registerPlugin("blackman-harris", BlackmanHarrisFilter.class);
        filterPlugins.registerPlugin("box", BoxFilter.class);
        filterPlugins.registerPlugin("catmull-rom", CatmullRomFilter.class);
        filterPlugins.registerPlugin("gaussian", GaussianFilter.class);
        filterPlugins.registerPlugin("lanczos", LanczosFilter.class);
        filterPlugins.registerPlugin("mitchell", MitchellFilter.class);
        filterPlugins.registerPlugin("sinc", SincFilter.class);
        filterPlugins.registerPlugin("triangle", TriangleFilter.class);
        filterPlugins.registerPlugin("bspline", CubicBSpline.class);
    }

    static {
        // gi engines
        giEnginePlugins.registerPlugin("ambocc", AmbientOcclusionGIEngine.class);
        giEnginePlugins.registerPlugin("fake", FakeGIEngine.class);
        giEnginePlugins.registerPlugin("igi", InstantGI.class);
        giEnginePlugins.registerPlugin("irr-cache", IrradianceCacheGIEngine.class);
        giEnginePlugins.registerPlugin("path", PathTracingGIEngine.class);
    }

    static {
        // caustic photon maps
        causticPhotonMapPlugins.registerPlugin("kd", CausticPhotonMap.class);
    }

    static {
        // global photon maps
        globalPhotonMapPlugins.registerPlugin("grid", GridPhotonMap.class);
        globalPhotonMapPlugins.registerPlugin("kd", GlobalPhotonMap.class);
    }

    static {
        // image samplers
        imageSamplerPlugins.registerPlugin("bucket", BucketRenderer.class);
        imageSamplerPlugins.registerPlugin("ipr", ProgressiveRenderer.class);
        imageSamplerPlugins.registerPlugin("fast", SimpleRenderer.class);
        imageSamplerPlugins.registerPlugin("multipass", MultipassRenderer.class);
    }

    static {
        // parsers
        parserPlugins.registerPlugin("sc", SCParser.class);
        parserPlugins.registerPlugin("sca", SCAsciiParser.class);
        parserPlugins.registerPlugin("scb", SCBinaryParser.class);
        parserPlugins.registerPlugin("rib", ShaveRibParser.class);
        parserPlugins.registerPlugin("ra2", RA2Parser.class);
        parserPlugins.registerPlugin("ra3", RA3Parser.class);
    }

    static {
        // bitmap readers
        bitmapReaderPlugins.registerPlugin("hdr", HDRBitmapReader.class);
        bitmapReaderPlugins.registerPlugin("tga", TGABitmapReader.class);
        bitmapReaderPlugins.registerPlugin("png", PNGBitmapReader.class);
        bitmapReaderPlugins.registerPlugin("jpg", JPGBitmapReader.class);
        bitmapReaderPlugins.registerPlugin("bmp", BMPBitmapReader.class);
        bitmapReaderPlugins.registerPlugin("igi", IGIBitmapReader.class);
    }

    static {
        // bitmap writers
        bitmapWriterPlugins.registerPlugin("png", PNGBitmapWriter.class);
        bitmapWriterPlugins.registerPlugin("hdr", HDRBitmapWriter.class);
        bitmapWriterPlugins.registerPlugin("tga", TGABitmapWriter.class);
        bitmapWriterPlugins.registerPlugin("exr", EXRBitmapWriter.class);
        bitmapWriterPlugins.registerPlugin("igi", IGIBitmapWriter.class);
    }
}
package org.openmolecules.fx.viewer3d.nodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javafx.geometry.Point3D;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;


public class IcosahedronMesh extends MeshView {
    private List<Point3D> listVertices = new ArrayList<>();
    private List<Face3D> listFaces = new ArrayList<>();

    private final static int DEFAULT_LEVEL = 1;
    private final static float SPHERE_DIAMETER =  1f;
    private float diameter;
    private int level;
    private Map<Long,Integer> middlePointCache;


    
    public IcosahedronMesh(float diameter, int level) {
    	this.level = level;
    	this.diameter = diameter;
    	this.middlePointCache = new HashMap<Long,Integer>();
    	TriangleMesh mesh = generateMesh();
    	this.setMesh(mesh);
    	
    }

        /*
        ICOSAHEDRON 
    */
		private float[] baseVertices = new float[]{
		    -0.525731f,  0.850651f, 0.f,
		     0.525731f,  0.850651f, 0.f, 
		    -0.525731f, -0.850651f, 0.f,
		     0.525731f, -0.850651f, 0.f, 
		    0.f, -0.525731f,  0.850651f, 
		    0.f,  0.525731f,  0.850651f, 
		    0.f, -0.525731f, -0.850651f, 
		    0.f,  0.525731f, -0.850651f, 
		     0.850651f, 0.f, -0.525731f, 
		     0.850651f, 0.f,  0.525731f, 
		    -0.850651f, 0.f, -0.525731f, 
		    -0.850651f, 0.f,  0.525731f
		};
		/*
		private final float[] baseTexCoords = new float[]{
		        0.181818f, 0f,             0.363636f, 0f, 
		        0.545455f, 0f,             0.727273f, 0f, 
		        0.909091f, 0f,             0.0909091f, 0.333333f,
		        0.272727f, 0.333333f,      0.454545f, 0.333333f, 
		        0.636364f, 0.333333f,      0.818182f, 0.333333f, 
		        1f, 0.333333f,             0f, 0.666667f, 
		        0.181818f, 0.666667f,      0.363636f, 0.666667f, 
		        0.545455f, 0.666667f,      0.727273f, 0.666667f, 
		        0.909091f, 0.666667f,      0.0909091f, 1f, 
		        0.272727f, 1f,             0.454545f, 1f, 
		        0.636364f, 1f,             0.818182f, 1f
		};
		*/
		
		private final float[] baseTexCoords = new float[]{
		        0,0};

		private final List<Integer> baseFaces = Arrays.asList(
		        0,11,5,             0,5,1,             0,1,7,             0,7,10,
		        0,10,11,            1,5,9,             5,11,4,            11,10,2,
		        10,7,6,             7,1,8,             3,9,4,             3,4,2,
		        3,2,6,              3,6,8,             3,8,9,             4,9,5,
		        2,4,11,             6,2,10,            8,6,7,             9,8,1
		);
		
		private class Face3D{
			int v1;
			int v2;
			int v3;
			int t1;
			int t2;
			int t3;
			
			public Face3D(int v1, int v2,int v3) {
				this.v1 = v1;
				this.v2 = v2;
				this.v3 = v3;
				this.t1 = 0;
				this.t2 = 0;
				this.t3 = 0;

			}
		}


		    
    
    

    public final TriangleMesh generateMesh(){
        Collector<Float, ?, float[]> toFloatArray =
                Collectors.collectingAndThen(Collectors.toList(), floatList -> {
                    float[] array = new float[floatList.size()];
                    for (ListIterator<Float> iterator = floatList.listIterator(); iterator.hasNext();) {
                        array[iterator.nextIndex()] = iterator.next();
                    }
                    return array ;
                });
    	TriangleMesh triangleMesh = new TriangleMesh();
    	constructIcosphere(this.diameter,this.level);
    	this.middlePointCache.clear();
    	triangleMesh.getPoints().setAll(listVertices.stream().flatMap(v -> Stream.of((float)v.getX(), (float)v.getY(),(float)v.getZ())).collect(toFloatArray));
    	triangleMesh.getFaces().setAll(listFaces.stream().flatMapToInt(f -> IntStream.of(f.v1,f.t1,f.v2,f.t2,f.v3,f.t3)).toArray());
    	triangleMesh.getTexCoords().setAll(baseTexCoords);
    	return triangleMesh;

    }
   

    
    private void constructIcosphere(float diameter, int level) {
    	if(level==0) {
    		int nrVertices = baseVertices.length/3;
    		listVertices = IntStream.range(0,nrVertices).mapToObj(i -> new Point3D(baseVertices[3*i]*diameter,baseVertices[3*i+1]*diameter,baseVertices[3*i+2]*diameter))
    				.collect(Collectors.toList());
    		int nrFaces = baseFaces.size()/3;
    		listFaces = IntStream.range(0,nrFaces).mapToObj(i -> new Face3D(baseFaces.get(3*i),baseFaces.get(3*i+1),baseFaces.get(3*i+2)))
    				.collect(Collectors.toList());
    	}
    	else if(level>0) {
    		constructIcosphere(diameter,level-1);
    		ArrayList<Face3D> toDelete = new ArrayList<Face3D>();
    		IntStream.range(0, listFaces.size()).forEach( i-> {toDelete.add(listFaces.get(i));refineTriangle(listFaces.get(i));});
    		listFaces.removeAll(toDelete);

    	}

    }
    
    private void refineTriangle(Face3D face) {;
    	int p1 = face.v1;
    	int p2 = face.v2;
    	int p3 = face.v3;
    	int p4 = getMiddlePoint(p1,p2);
    	int p5 = getMiddlePoint(p2,p3);
    	int p6 = getMiddlePoint(p1,p3);
    	
    	listFaces.add(new Face3D(p1,p6,p4));
    	listFaces.add(new Face3D(p4,p6,p5));
    	listFaces.add(new Face3D(p4,p5,p2));
    	listFaces.add(new Face3D(p3,p6,p5));
    	
    	
    	
    }
    
    private int getMiddlePoint(int p1, int p2) {
    	long key = p1>p2 ? (((long) p1<<32) | p2 ) : (((long) p2<<32) | p1 );
    	int index;
    	if (middlePointCache.get(key)!=null) {
    		index =  middlePointCache.get(key);
    	}
    	else {
    		Point3D middlePoint = (listVertices.get(p1).add(listVertices.get(p2))).normalize().multiply(diameter);
    		listVertices.add(middlePoint);
    		index = listVertices.size()-1;
    		middlePointCache.put(key,index);
    	}
    	return index;
    }
    
    


}




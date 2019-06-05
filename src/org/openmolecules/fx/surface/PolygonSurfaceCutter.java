/*
 * FXMolViewer, showing and manipulating molecules and protein structures in 3D.
 * Copyright (C) 2019 Thomas Sander

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * @author Thomas Sander
 */

package org.openmolecules.fx.surface;

import javafx.collections.ObservableFloatArray;
import javafx.collections.ObservableIntegerArray;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.shape.Polygon;
import org.openmolecules.fx.viewer3d.V3DMolecule;

import java.util.Arrays;

import static org.openmolecules.fx.surface.SurfaceMesh.USE_NORMALS;

public class PolygonSurfaceCutter extends SurfaceCutter {
	public static final int SURFACE_CUT_NONE = 0;
	public static final int SURFACE_CUT_INSIDE = 1;
	public static final int SURFACE_CUT_OUTSIDE = 2;
	public static final int MAX_NEIGHBOR_VERTEXES = 10;

	private Polygon mPolygon;
	private V3DMolecule mFXMol;
	private Point2D mPaneOnScreen;
	private int mMode;
//	private boolean mIsClockwise;

	public PolygonSurfaceCutter(Polygon polygon, V3DMolecule fxmol, int mode, Point2D paneOnScreen) {
		mPolygon = polygon;
		mFXMol = fxmol;
		mMode = mode;
		mPaneOnScreen = paneOnScreen;
//		mIsClockwise = (getSignedPolygonArea(polygon) > 0);
	}

	@Override
	protected int markVertexesToRemoved(boolean[] isToBeRemoved) {
		ObservableFloatArray points = mSurfaceMesh.getPoints();
		int pointCount = points.size() / 3;
		int removalCount = 0;

		if (mMode == SURFACE_CUT_OUTSIDE) {
			for (int i=0; i<pointCount; i++) {
				isToBeRemoved[i] = !isInsidePolygon(points.get(3 * i), points.get(3 * i + 1), points.get(3 * i + 2));
				if (isToBeRemoved[i])
					removalCount++;
				}
			}

		// In case of SURFACE_CUT_INSIDE we only cut off the front part of the surface within the polygon. Thus, we need to
		// - find the front most surface vertex in the polygon
		// - find all connected recursively connected vertexes to front vertex within polygon
		else if (mMode == SURFACE_CUT_INSIDE) {
			int frontVertex = -1;
			float frontZ = Float.MAX_VALUE;
			int[] inPolygonIndex = new int[pointCount];
			int inPolygonCount = 0;
			for (int i=0; i<pointCount; i++) {
				if (isInsidePolygon(points.get(3 * i), points.get(3 * i + 1), points.get(3 * i + 2))) {
					inPolygonIndex[i] = inPolygonCount++;
//					if (frontZ > points.get(3 * i + 2)) {
//						frontZ = points.get(3 * i + 2);
//						frontVertex = i;
//						}
					Point3D p = mFXMol.localToScene(points.get(3 * i), points.get(3 * i + 1), points.get(3 * i + 2));
					if (frontZ > p.getZ()) {
						frontZ = (float)p.getZ();
						frontVertex = i;
						}
					}
				else {
					inPolygonIndex[i] = -1;
					}
				}

System.out.println("pointCount:"+pointCount+" inPolygonCount:"+inPolygonCount);

			if (frontVertex != -1) {
				int[] neighborVertex = new int[inPolygonCount * MAX_NEIGHBOR_VERTEXES];
				Arrays.fill(neighborVertex, -1);

				ObservableIntegerArray faces = mSurfaceMesh.getFaces();
				int facePointSize = USE_NORMALS ? 3 : 2;
				int faceSize = 3 * facePointSize;
				for (int i1 = 0; i1 < faces.size(); i1 += faceSize) {
					int v1 = faces.get(i1);
					int v2 = faces.get(i1 + facePointSize);
					int v3 = faces.get(i1 + 2*facePointSize);
					if (inPolygonIndex[v1] != -1 && inPolygonIndex[v2] != -1) {
						addNeighbours(inPolygonIndex[v1], v2, neighborVertex);
						addNeighbours(inPolygonIndex[v2], v1, neighborVertex);
						}
					if (inPolygonIndex[v1] != -1 && inPolygonIndex[v3] != -1) {
						addNeighbours(inPolygonIndex[v1], v3, neighborVertex);
						addNeighbours(inPolygonIndex[v3], v1, neighborVertex);
						}
					if (inPolygonIndex[v2] != -1 && inPolygonIndex[v3] != -1) {
						addNeighbours(inPolygonIndex[v2], v3, neighborVertex);
						addNeighbours(inPolygonIndex[v3], v2, neighborVertex);
						}
					}

				int current = 0;
				int highest = 0;
				int[] graphVertex = new int[inPolygonCount];
				graphVertex[0] = frontVertex;
				isToBeRemoved[frontVertex] = true;
				removalCount++;
				while (current <= highest) {
					int i1 = inPolygonIndex[graphVertex[current]] * MAX_NEIGHBOR_VERTEXES;
					int i2 = i1 + MAX_NEIGHBOR_VERTEXES;
					for (int i=i1; i<i2 && neighborVertex[i] != -1; i++) {
						int neighbor = neighborVertex[i];
						if (!isToBeRemoved[neighbor]) {
							graphVertex[++highest] = neighbor;
							isToBeRemoved[neighbor] = true;
							removalCount++;
							}
						}
					current++;
					}
				}
			}

System.out.println("removalCount:"+removalCount);


		// don't cut anything, if we have an unknown mode
		return removalCount;
		}

	private void addNeighbours(int inPolygonIndex, int vertex, int[] neighborVertex) {
		int i1 = inPolygonIndex * MAX_NEIGHBOR_VERTEXES;
		int i2 = i1 + MAX_NEIGHBOR_VERTEXES;
		for (int i=i1; i<i2; i++) {
			if (neighborVertex[i] == vertex)    // vertex already known as neighbor
				return;
			if (neighborVertex[i] == -1) {
				neighborVertex[i] = vertex;
				return;
				}
			}
		System.out.println("WARNING: MAX_NEIGHBOR_VERTEXES exceeded.");
		}

	/**
	 * Determines the point of the cut between an inside and an outside point
	 * and adds(!) the new location to the three dimensions of location
	 * In this implementation the method first finds that point Ps of the polygon that is closest to the
	 * screen coordinates of the point Po(xi,yi,zi), which is the edge end point that is outside of the
	 * polygon, i.e. inside if the remaining surface after the cut. The method determines, whether the
	 * screen position of Ps is left or right of the line from Po to Pi(xo,yo,zo). Then in a loop we
	 * look at polygon points left and right of Ps in order to find a point Pn that is on the other side
	 * (left or right) as Ps itself in regard to the line from Po to Pi. Once that is found, the intersection
	 * Px of lines (Pn,Pn-1) and line(Po,Pi) is calculated and the local 3D-coordinates of Px added to location.
	 * @param xi
	 * @param yi
	 * @param zi
	 * @param xo
	 * @param yo
	 * @param zo
	 * @param location buffer to which the three coordinates are added
	 */
	protected void addCutPosition(float xi, float yi, float zi, float xo, float yo, float zo, float[] location) {
		Point2D spo = mFXMol.localToScreen(xi, yi, zi).subtract(mPaneOnScreen); // point outside polygon
		Point2D spi = mFXMol.localToScreen(xo, yo, zo).subtract(mPaneOnScreen); // point inside polygon (is outside of remaining surface)

		// find closest polygon point to spi, which is outside of polygon
		ObservableList<Double> pl = mPolygon.getPoints();
		double minDistance = Double.MAX_VALUE;
		int minIndex = -1;
		for (int i=0; i<pl.size(); i+=2) {
			double dx = pl.get(i) - spo.getX();
			double dx2 = dx*dx;
			if (minDistance > dx2) {
				double dy = pl.get(i+1) - spo.getY();
				double dy2 = dy*dy;
				if (minDistance > dx2 + dy2) {
					minDistance = dx2 + dy2;
					minIndex = i;
					}
				}
			}

		double f = -1f;
		int isLeft = isLeft(pl.get(minIndex), pl.get(minIndex+1), spo, spi);
		if (isLeft == 0) {
			if (spo.getX() != spi.getX())
				f = (pl.get(minIndex) - spi.getX()) / (spo.getX() - spi.getX());
			else
				f = (pl.get(minIndex + 1) - spi.getY()) / (spo.getY() - spi.getY());
			}
		else {
			int[] index = new int[2];
			index[0] = minIndex;
			index[1] = minIndex;
			boolean found = false;
			for (int i=0; !found && i<pl.size()/4; i++) {
				for (int j=0; !found && j<2; j++) {
					double px = pl.get(index[j]);
					double py = pl.get(index[j]+1);
					if (j == 0) {    // polygon point is left of line from spo to spi
						index[0] -= 2;  // provided that the polygon was created clockwise, reducing index gets next point to the right seen from outside
						if (index[0] < 0)
							index[0] += pl.size();
					}
					else {
						index[1] += 2;
						if (index[1] >= pl.size())
							index[1] -= pl.size();
					}

					int newIsLeft = isLeft(pl.get(index[j]), pl.get(index[j]+1), spo, spi);
					if (newIsLeft != isLeft) {
						double npx = pl.get(index[j]);
						double npy = pl.get(index[j]+1);
						if (newIsLeft == 0) {
							if (spo.getX() != spi.getX())
								f = (npx - spi.getX()) / (spo.getX() - spi.getX());
							else
								f = (npy - spi.getY()) / (spo.getY() - spi.getY());
						}
						else {
							// https://en.wikipedia.org/wiki/Line%E2%80%93line_intersection#Given_two_points_on_each_line
							f = -((npx - px) * (npy - spi.getY()) - (npy - py) * (npx - spi.getX())) / ((npx - px) * (spi.getY() - spo.getY()) - (npy - py) * (spi.getX() - spo.getX()));
//System.out.println("  loops:" + (i + 1) + " f1:" + f);
						}
						found = true;
					}
				}
			}
		}


//if (i == pl.size()-4) {
//	f = 0;
//	System.out.println("Took one round!!!");
//	break;
//
// System.out.println("Type\tindex\tx\ty");
// System.out.println("spo\t-1\t"+spo.getX()+"\t"+spo.getY());
// System.out.println("spi\t-1\t"+spi.getX()+"\t"+spi.getY());
// for (int j=0; j<pl.size(); j+=2)
//  System.out.println((j==minIndex?"polygon min":"polygon")+"\t"+j+"\t"+Math.round(pl.get(j))+"\t"+Math.round(pl.get(j+1)));
// exit(0);
//
//}


		if (f < 0) {
			System.out.println("WARNING: f = "+f);
			f = 0;
		}
		else if (f > 1) {
			System.out.println("WARNING: f = "+f);
			f = 1;
		}

		location[0] += xo + (float)f * (xi - xo);
		location[1] += yo + (float)f * (yi - yo);
		location[2] += zo + (float)f * (zi - zo);
		}

	/**
	 * Tests if a point(x,y) is Left|On|Right of an infinite line.
	 *     Input: three points x, y, P0, P1
	 *     Return: >0 for P(x,y) left of the line through P0 and P1
	 *             =0 for P2  on the line
	 *             <0 for P2  right of the line
	 *     See: Algorithm 1 "Area of Triangles and Polygons"
	 *
	 * @param x
	 * @param y
	 * @param p0
	 * @param p1
	 * @return
	 */
	private int isLeft(double x, double y, Point2D p0, Point2D p1) {
		double extent = (p1.getX() - p0.getX()) * (y - p0.getY()) - (x - p0.getX()) * (p1.getY() - p0.getY());
		return extent < 0 ? -1 : extent == 0 ? 0 : 1;
	}

	private boolean isInsidePolygon(float x, float y, float z) {
		Point2D p = mFXMol.localToScreen(x, y, z).subtract(mPaneOnScreen);
		ObservableList<Double> pl = mPolygon.getPoints();
		boolean isInside = false;
		for (int i=0, j=pl.size()-2; i<pl.size(); i+=2) {
			if ( ((pl.get(i+1) > p.getY()) != (pl.get(j+1) > p.getY()))
					&& (p.getX() < (pl.get(j)-pl.get(i)) * (p.getY()-pl.get(i+1)) / (pl.get(j+1)-pl.get(i+1)) + pl.get(i)))
				isInside = !isInside;
			j = i;
		}
		return isInside;
	}

/*	private double getSignedPolygonArea(Polygon polygon) {
		ObservableList<Double> pl = polygon.getPoints();
		int size = pl.size();
		double x1 = pl.get(size-2);
		double y1 = pl.get(size-1);
		double area = 0.0;
		for (int i=0; i<size; i+=2) {
			double x2 = pl.get(i);
			double y2 = pl.get(i+1);
			area += (x2 - x1) * (y2 + y1);
			}
		return area / 2;
		}*/
	}

/*
 * Copyright 2017 Thomas Sander, Therwilerstrasse 41, CH-4153 Reinach, Switzerland
 *
 * This file is part of openmolecules.org's 3D-Molecule-Viewer.
 *
 * 3D-Molecule-Viewer is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * 3D-Molecule-Viewer is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with 3D-Molecule-Viewer.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Thomas Sander
 */

package org.openmolecules.mesh;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Random;
import java.util.TreeMap;

/**
 * This mesh uses a modified marching cubes algorithm avoiding the creation
 * of small and skinny triangles, which cause visible edges when applying surface
 * normals for surface smoothing. This algorithm differs from the original
 * by merging all vertexes that are closer to a voxel corner than a given
 * threshold (MAX_JOINT_POSITION). 0.5 would merge all edge cut positions,
 * which are closer to a given vertex than to any of its adjacent vertexes.
 * 0.5 reduces the number of triangles to about 60% (comparing to uncorrected
 * marching cubes), produces sound triangle geometries without any narrow
 * triangles and still seems to represent the iso-layer rather well.
 * Compared to the original marching cubes algorithm this one needs about
 * 15% to 20% more time for the triangulations, because vertexes of any layer
 * are merged, cached and reassigned before triangulating the layer.
 */
public class SmoothMarchingCubesMesh {
	private static final float MAX_JOINT_POSITION = 0.48f;   // max: 0.5, min: 0.0 (this is uncorrected maching cubes
	// TODO make statistics of triangle area and corner angle distribution with different MAX_JOINT_POSITION values

	// Convention of edge and vertex indexes:
	//	       4 ____________________ 5
	//         /|         4         /|
	//        / |                  / |
	//       /7 |                 /5 |
	//      /   |8               /   |9
	//    7/____|_______________/6   |
	//     |    |     6         |    |
	//     |   0|_______________|____|1
	//     |    /         0     |    /
	//   11|   /              10|   /
	//     |  /3                |  /1
	//     | /                  | /
	//     |/___________________|/
	//    3           2          2

	// Coordinate system
	//
	//       y |
	//         |
	//         |
	//         |
	//         |_____________
	//        /             x
	//       /
	//    z /

	private static final boolean VERBOSE_POINTS_AND_TRIANGLES = false;

	public static final int[] EDGE_TABLE = {
			0x000, 0x109, 0x203, 0x30a, 0x406, 0x50f, 0x605, 0x70c,
			0x80c, 0x905, 0xa0f, 0xb06, 0xc0a, 0xd03, 0xe09, 0xf00,
			0x190, 0x099, 0x393, 0x29a, 0x596, 0x49f, 0x795, 0x69c,
			0x99c, 0x895, 0xb9f, 0xa96, 0xd9a, 0xc93, 0xf99, 0xe90,
			0x230, 0x339, 0x033, 0x13a, 0x636, 0x73f, 0x435, 0x53c,
			0xa3c, 0xb35, 0x83f, 0x936, 0xe3a, 0xf33, 0xc39, 0xd30,
			0x3a0, 0x2a9, 0x1a3, 0x0aa, 0x7a6, 0x6af, 0x5a5, 0x4ac,
			0xbac, 0xaa5, 0x9af, 0x8a6, 0xfaa, 0xea3, 0xda9, 0xca0,
			0x460, 0x569, 0x663, 0x76a, 0x066, 0x16f, 0x265, 0x36c,
			0xc6c, 0xd65, 0xe6f, 0xf66, 0x86a, 0x963, 0xa69, 0xb60,
			0x5f0, 0x4f9, 0x7f3, 0x6fa, 0x1f6, 0x0ff, 0x3f5, 0x2fc,
			0xdfc, 0xcf5, 0xfff, 0xef6, 0x9fa, 0x8f3, 0xbf9, 0xaf0,
			0x650, 0x759, 0x453, 0x55a, 0x256, 0x35f, 0x055, 0x15c,
			0xe5c, 0xf55, 0xc5f, 0xd56, 0xa5a, 0xb53, 0x859, 0x950,
			0x7c0, 0x6c9, 0x5c3, 0x4ca, 0x3c6, 0x2cf, 0x1c5, 0x0cc,
			0xfcc, 0xec5, 0xdcf, 0xcc6, 0xbca, 0xac3, 0x9c9, 0x8c0,
			0x8c0, 0x9c9, 0xac3, 0xbca, 0xcc6, 0xdcf, 0xec5, 0xfcc,
			0x0cc, 0x1c5, 0x2cf, 0x3c6, 0x4ca, 0x5c3, 0x6c9, 0x7c0,
			0x950, 0x859, 0xb53, 0xa5a, 0xd56, 0xc5f, 0xf55, 0xe5c,
			0x15c, 0x055, 0x35f, 0x256, 0x55a, 0x453, 0x759, 0x650,
			0xaf0, 0xbf9, 0x8f3, 0x9fa, 0xef6, 0xfff, 0xcf5, 0xdfc,
			0x2fc, 0x3f5, 0x0ff, 0x1f6, 0x6fa, 0x7f3, 0x4f9, 0x5f0,
			0xb60, 0xa69, 0x963, 0x86a, 0xf66, 0xe6f, 0xd65, 0xc6c,
			0x36c, 0x265, 0x16f, 0x066, 0x76a, 0x663, 0x569, 0x460,
			0xca0, 0xda9, 0xea3, 0xfaa, 0x8a6, 0x9af, 0xaa5, 0xbac,
			0x4ac, 0x5a5, 0x6af, 0x7a6, 0x0aa, 0x1a3, 0x2a9, 0x3a0,
			0xd30, 0xc39, 0xf33, 0xe3a, 0x936, 0x83f, 0xb35, 0xa3c,
			0x53c, 0x435, 0x73f, 0x636, 0x13a, 0x033, 0x339, 0x230,
			0xe90, 0xf99, 0xc93, 0xd9a, 0xa96, 0xb9f, 0x895, 0x99c,
			0x69c, 0x795, 0x49f, 0x596, 0x29a, 0x393, 0x099, 0x190,
			0xf00, 0xe09, 0xd03, 0xc0a, 0xb06, 0xa0f, 0x905, 0x80c,
			0x70c, 0x605, 0x50f, 0x406, 0x30a, 0x203, 0x109, 0x000 };

//	public static final int[][] FACE_TABLE = MarchingCubesAlgorithm.FACE_TABLE;
	public static final int[][] FACE_TABLE = {
		{},
		{0, 8, 3},
		{1, 9, 0},
		{1, 8, 3, 9, 8, 1},
		{2, 10, 1},
		{0, 8, 3, 1, 2, 10},
		{2, 9, 0, 10, 9, 2},
		{3, 9, 8, 2, 9, 3, 2, 10, 9},
		{3, 11, 2},
		{0, 11, 2, 8, 11, 0},
		{1, 9, 0, 2, 3, 11},
		{2, 8, 11, 1, 8, 2, 1, 9, 8},
		{3, 10, 1, 11, 10, 3},
		{1, 11, 10, 0, 11, 1, 0, 8, 11},
		{0, 10, 9, 3, 10, 0, 3, 11, 10},
		{9, 8, 10, 10, 8, 11},
		{7, 8, 4},
		{4, 3, 0, 7, 3, 4},
		{0, 1, 9, 8, 4, 7},
		{9, 3, 1, 4, 3, 9, 4, 7, 3},
		{2, 10, 1, 7, 8, 4},
		{3, 4, 7, 3, 0, 4, 1, 2, 10},
		{9, 2, 10, 9, 0, 2, 8, 4, 7},
		{2, 12, 3, 3, 12, 7, 7, 12, 4, 4, 12, 9, 9, 12, 10, 10, 12, 2},
		{11, 2, 3, 7, 8, 4},
		{7, 0, 4, 11, 0, 7, 11, 2, 0},
		{2, 3, 11, 0, 1, 9, 7, 8, 4},
		{1, 12, 2, 2, 12, 11, 11, 12, 7, 7, 12, 4, 4, 12, 9, 9, 12, 1},
		{3, 10, 1, 3, 11, 10, 7, 8, 4},
		{0, 12, 1, 1, 12, 10, 10, 12, 11, 11, 12, 7, 7, 12, 4, 4, 12, 0},
		{7, 8, 4, 0, 10, 9, 3, 10, 0, 3, 11, 10},
		{11, 10, 7, 7, 10, 4, 10, 9, 4},
		{4, 9, 5},
		{8, 3, 0, 4, 9, 5},
		{5, 0, 1, 4, 0, 5},
		{4, 1, 5, 8, 1, 4, 8, 3, 1},
		{1, 2, 10, 9, 5, 4},
		{3, 0, 8, 1, 2, 10, 4, 9, 5},
		{10, 0, 2, 5, 0, 10, 5, 4, 0},
		{2, 12, 3, 3, 12, 8, 8, 12, 4, 4, 12, 5, 5, 12, 10, 10, 12, 2},
		{3, 11, 2, 4, 9, 5},
		{0, 11, 2, 0, 8, 11, 4, 9, 5},
		{0, 5, 4, 0, 1, 5, 2, 3, 11},
		{1, 12, 2, 2, 12, 11, 11, 12, 8, 8, 12, 4, 4, 12, 5, 5, 12, 1},
		{10, 3, 11, 10, 1, 3, 9, 5, 4},
		{4, 9, 5, 1, 11, 10, 0, 11, 1, 0, 8, 11},
		{3, 12, 0, 0, 12, 4, 4, 12, 5, 5, 12, 10, 10, 12, 11, 11, 12, 3},
		{8, 11, 4, 4, 11, 5, 11, 10, 5},
		{7, 9, 5, 8, 9, 7},
		{0, 7, 3, 9, 7, 0, 9, 5, 7},
		{8, 5, 7, 0, 5, 8, 0, 1, 5},
		{3, 1, 7, 7, 1, 5},
		{9, 7, 8, 9, 5, 7, 10, 1, 2},
		{10, 1, 2, 0, 7, 3, 9, 7, 0, 9, 5, 7},
		{5, 12, 10, 10, 12, 2, 2, 12, 0, 0, 12, 8, 8, 12, 7, 7, 12, 5},
		{5, 7, 10, 10, 7, 2, 7, 3, 2},
		{7, 9, 5, 7, 8, 9, 3, 11, 2},
		{11, 12, 7, 7, 12, 5, 5, 12, 9, 9, 12, 0, 0, 12, 2, 2, 12, 11},
		{2, 3, 11, 8, 5, 7, 0, 5, 8, 0, 1, 5},
		{1, 5, 2, 2, 5, 11, 5, 7, 11},
		{8, 5, 7, 8, 9, 5, 11, 10, 3, 10, 1, 3},
		{7, 11, 5, 11, 10, 5, 0, 9, 1},
		{10, 5, 11, 5, 7, 11, 0, 3, 8},
		{7, 11, 5, 5, 11, 10},
		{5, 10, 6},
		{0, 8, 3, 5, 10, 6},
		{9, 0, 1, 5, 10, 6},
		{1, 8, 3, 1, 9, 8, 5, 10, 6},
		{6, 1, 2, 5, 1, 6},
		{1, 6, 5, 1, 2, 6, 3, 0, 8},
		{5, 2, 6, 9, 2, 5, 9, 0, 2},
		{2, 12, 3, 3, 12, 8, 8, 12, 9, 9, 12, 5, 5, 12, 6, 6, 12, 2},
		{2, 3, 11, 10, 6, 5},
		{11, 0, 8, 11, 2, 0, 10, 6, 5},
		{0, 1, 9, 2, 3, 11, 5, 10, 6},
		{5, 10, 6, 2, 8, 11, 1, 8, 2, 1, 9, 8},
		{11, 1, 3, 6, 1, 11, 6, 5, 1},
		{0, 12, 1, 1, 12, 5, 5, 12, 6, 6, 12, 11, 11, 12, 8, 8, 12, 0},
		{3, 12, 0, 0, 12, 9, 9, 12, 5, 5, 12, 6, 6, 12, 11, 11, 12, 3},
		{9, 8, 5, 5, 8, 6, 8, 11, 6},
		{7, 8, 4, 6, 5, 10},
		{4, 3, 0, 4, 7, 3, 6, 5, 10},
		{6, 5, 10, 4, 7, 8, 1, 9, 0},
		{6, 5, 10, 9, 3, 1, 4, 3, 9, 4, 7, 3},
		{6, 1, 2, 6, 5, 1, 4, 7, 8},
		{7, 0, 4, 7, 3, 0, 5, 1, 6, 1, 2, 6},
		{8, 4, 7, 5, 2, 6, 9, 2, 5, 9, 0, 2},
		{3, 2, 7, 2, 6, 7, 9, 4, 5},
		{4, 7, 8, 6, 5, 10, 3, 11, 2},
		{10, 6, 5, 7, 0, 4, 11, 0, 7, 11, 2, 0},
		{7, 8, 4, 2, 3, 11, 1, 9, 0, 5, 10, 6},
		{11, 6, 7, 9, 4, 5, 1, 10, 2},
		{4, 7, 8, 11, 1, 3, 6, 1, 11, 6, 5, 1},
		{1, 0, 5, 0, 4, 5, 11, 6, 7},
		{9, 4, 5, 11, 6, 7, 3, 8, 0},
		{7, 11, 6, 9, 4, 5},
		{4, 10, 6, 9, 10, 4},
		{4, 10, 6, 4, 9, 10, 0, 8, 3},
		{1, 4, 0, 10, 4, 1, 10, 6, 4},
		{8, 12, 4, 4, 12, 6, 6, 12, 10, 10, 12, 1, 1, 12, 3, 3, 12, 8},
		{9, 6, 4, 1, 6, 9, 1, 2, 6},
		{3, 0, 8, 9, 6, 4, 1, 6, 9, 1, 2, 6},
		{0, 2, 4, 4, 2, 6},
		{2, 6, 3, 3, 6, 8, 6, 4, 8},
		{10, 4, 9, 10, 6, 4, 11, 2, 3},
		{9, 6, 4, 9, 10, 6, 8, 11, 0, 11, 2, 0},
		{11, 2, 3, 1, 4, 0, 10, 4, 1, 10, 6, 4},
		{4, 8, 6, 8, 11, 6, 1, 10, 2},
		{6, 12, 11, 11, 12, 3, 3, 12, 1, 1, 12, 9, 9, 12, 4, 4, 12, 6},
		{11, 6, 8, 6, 4, 8, 1, 0, 9},
		{6, 4, 11, 11, 4, 3, 4, 0, 3},
		{4, 8, 6, 6, 8, 11},
		{6, 9, 10, 7, 9, 6, 7, 8, 9},
		{7, 12, 6, 6, 12, 10, 10, 12, 9, 9, 12, 0, 0, 12, 3, 3, 12, 7},
		{7, 12, 6, 6, 12, 10, 10, 12, 1, 1, 12, 0, 0, 12, 8, 8, 12, 7},
		{7, 3, 6, 6, 3, 10, 3, 1, 10},
		{7, 12, 6, 6, 12, 2, 2, 12, 1, 1, 12, 9, 9, 12, 8, 8, 12, 7},
		{6, 7, 2, 7, 3, 2, 9, 1, 0},
		{0, 2, 8, 8, 2, 7, 2, 6, 7},
		{3, 2, 7, 7, 2, 6},
		{3, 11, 2, 6, 9, 10, 7, 9, 6, 7, 8, 9},
		{0, 9, 2, 9, 10, 2, 7, 11, 6},
		{10, 2, 1, 8, 0, 3, 7, 11, 6},
		{2, 1, 10, 7, 11, 6},
		{9, 1, 8, 1, 3, 8, 6, 7, 11},
		{0, 9, 1, 7, 11, 6},
		{8, 0, 3, 6, 7, 11},
		{7, 11, 6},
		{6, 11, 7},
		{3, 0, 8, 11, 7, 6},
		{1, 9, 0, 6, 11, 7},
		{8, 1, 9, 8, 3, 1, 11, 7, 6},
		{10, 1, 2, 6, 11, 7},
		{1, 2, 10, 3, 0, 8, 6, 11, 7},
		{2, 9, 0, 2, 10, 9, 6, 11, 7},
		{6, 11, 7, 3, 9, 8, 2, 9, 3, 2, 10, 9},
		{7, 2, 3, 6, 2, 7},
		{8, 2, 0, 7, 2, 8, 7, 6, 2},
		{2, 7, 6, 2, 3, 7, 0, 1, 9},
		{1, 12, 2, 2, 12, 6, 6, 12, 7, 7, 12, 8, 8, 12, 9, 9, 12, 1},
		{6, 3, 7, 10, 3, 6, 10, 1, 3},
		{0, 12, 1, 1, 12, 10, 10, 12, 6, 6, 12, 7, 7, 12, 8, 8, 12, 0},
		{3, 12, 0, 0, 12, 9, 9, 12, 10, 10, 12, 6, 6, 12, 7, 7, 12, 3},
		{10, 9, 6, 6, 9, 7, 9, 8, 7},
		{6, 8, 4, 11, 8, 6},
		{11, 4, 6, 3, 4, 11, 3, 0, 4},
		{8, 6, 11, 8, 4, 6, 9, 0, 1},
		{4, 12, 9, 9, 12, 1, 1, 12, 3, 3, 12, 11, 11, 12, 6, 6, 12, 4},
		{6, 8, 4, 6, 11, 8, 2, 10, 1},
		{1, 2, 10, 11, 4, 6, 3, 4, 11, 3, 0, 4},
		{11, 4, 6, 11, 8, 4, 10, 9, 2, 9, 0, 2},
		{9, 4, 10, 4, 6, 10, 3, 2, 11},
		{3, 6, 2, 8, 6, 3, 8, 4, 6},
		{2, 0, 6, 6, 0, 4},
		{9, 0, 1, 3, 6, 2, 8, 6, 3, 8, 4, 6},
		{4, 6, 9, 9, 6, 1, 6, 2, 1},
		{10, 12, 6, 6, 12, 4, 4, 12, 8, 8, 12, 3, 3, 12, 1, 1, 12, 10},
		{0, 4, 1, 1, 4, 10, 4, 6, 10},
		{6, 10, 4, 10, 9, 4, 3, 8, 0},
		{6, 10, 4, 4, 10, 9},
		{6, 11, 7, 5, 4, 9},
		{5, 4, 9, 7, 6, 11, 0, 8, 3},
		{5, 0, 1, 5, 4, 0, 7, 6, 11},
		{11, 7, 6, 4, 1, 5, 8, 1, 4, 8, 3, 1},
		{7, 6, 11, 5, 4, 9, 2, 10, 1},
		{6, 11, 7, 1, 2, 10, 0, 8, 3, 4, 9, 5},
		{7, 6, 11, 10, 0, 2, 5, 0, 10, 5, 4, 0},
		{8, 7, 4, 10, 5, 6, 2, 11, 3},
		{7, 2, 3, 7, 6, 2, 5, 4, 9},
		{5, 4, 9, 8, 2, 0, 7, 2, 8, 7, 6, 2},
		{6, 3, 7, 6, 2, 3, 4, 0, 5, 0, 1, 5},
		{2, 1, 6, 1, 5, 6, 8, 7, 4},
		{9, 5, 4, 6, 3, 7, 10, 3, 6, 10, 1, 3},
		{10, 5, 6, 8, 7, 4, 0, 9, 1},
		{0, 3, 4, 3, 7, 4, 10, 5, 6},
		{4, 8, 7, 10, 5, 6},
		{5, 8, 9, 6, 8, 5, 6, 11, 8},
		{6, 12, 5, 5, 12, 9, 9, 12, 0, 0, 12, 3, 3, 12, 11, 11, 12, 6},
		{6, 12, 5, 5, 12, 1, 1, 12, 0, 0, 12, 8, 8, 12, 11, 11, 12, 6},
		{3, 1, 11, 11, 1, 6, 1, 5, 6},
		{2, 10, 1, 5, 8, 9, 6, 8, 5, 6, 11, 8},
		{9, 1, 0, 11, 3, 2, 6, 10, 5},
		{8, 0, 11, 0, 2, 11, 5, 6, 10},
		{11, 3, 2, 5, 6, 10},
		{6, 12, 5, 5, 12, 9, 9, 12, 8, 8, 12, 3, 3, 12, 2, 2, 12, 6},
		{6, 2, 5, 5, 2, 9, 2, 0, 9},
		{5, 6, 1, 6, 2, 1, 8, 0, 3},
		{2, 1, 6, 6, 1, 5},
		{3, 8, 1, 8, 9, 1, 6, 10, 5},
		{1, 0, 9, 6, 10, 5},
		{3, 8, 0, 6, 10, 5},
		{6, 10, 5},
		{5, 11, 7, 10, 11, 5},
		{11, 5, 10, 11, 7, 5, 8, 3, 0},
		{5, 11, 7, 5, 10, 11, 1, 9, 0},
		{10, 7, 5, 10, 11, 7, 9, 8, 1, 8, 3, 1},
		{2, 5, 1, 11, 5, 2, 11, 7, 5},
		{8, 3, 0, 2, 5, 1, 11, 5, 2, 11, 7, 5},
		{9, 12, 5, 5, 12, 7, 7, 12, 11, 11, 12, 2, 2, 12, 0, 0, 12, 9},
		{5, 9, 7, 9, 8, 7, 2, 11, 3},
		{10, 7, 5, 2, 7, 10, 2, 3, 7},
		{7, 12, 8, 8, 12, 0, 0, 12, 2, 2, 12, 10, 10, 12, 5, 5, 12, 7},
		{0, 1, 9, 10, 7, 5, 2, 7, 10, 2, 3, 7},
		{8, 7, 9, 7, 5, 9, 2, 1, 10},
		{1, 3, 5, 5, 3, 7},
		{7, 5, 8, 8, 5, 0, 5, 1, 0},
		{3, 7, 0, 0, 7, 9, 7, 5, 9},
		{5, 9, 7, 7, 9, 8},
		{4, 11, 8, 5, 11, 4, 5, 10, 11},
		{5, 12, 4, 4, 12, 0, 0, 12, 3, 3, 12, 11, 11, 12, 10, 10, 12, 5},
		{1, 9, 0, 4, 11, 8, 5, 11, 4, 5, 10, 11},
		{11, 3, 10, 3, 1, 10, 4, 5, 9},
		{5, 12, 4, 4, 12, 8, 8, 12, 11, 11, 12, 2, 2, 12, 1, 1, 12, 5},
		{4, 5, 0, 5, 1, 0, 11, 3, 2},
		{2, 11, 0, 11, 8, 0, 5, 9, 4},
		{2, 11, 3, 5, 9, 4},
		{5, 12, 4, 4, 12, 8, 8, 12, 3, 3, 12, 2, 2, 12, 10, 10, 12, 5},
		{2, 0, 10, 10, 0, 5, 0, 4, 5},
		{8, 0, 3, 10, 2, 1, 5, 9, 4},
		{10, 2, 1, 4, 5, 9},
		{5, 1, 4, 4, 1, 8, 1, 3, 8},
		{1, 0, 5, 5, 0, 4},
		{0, 3, 8, 5, 9, 4},
		{5, 9, 4},
		{7, 10, 11, 4, 10, 7, 4, 9, 10},
		{0, 8, 3, 7, 10, 11, 4, 10, 7, 4, 9, 10},
		{4, 12, 7, 7, 12, 11, 11, 12, 10, 10, 12, 1, 1, 12, 0, 0, 12, 4},
		{1, 10, 3, 10, 11, 3, 4, 8, 7},
		{4, 12, 7, 7, 12, 11, 11, 12, 2, 2, 12, 1, 1, 12, 9, 9, 12, 4},
		{11, 3, 2, 9, 1, 0, 4, 8, 7},
		{4, 0, 7, 7, 0, 11, 0, 2, 11},
		{3, 2, 11, 4, 8, 7},
		{4, 12, 7, 7, 12, 3, 3, 12, 2, 2, 12, 10, 10, 12, 9, 9, 12, 4},
		{10, 2, 9, 2, 0, 9, 7, 4, 8},
		{7, 4, 3, 4, 0, 3, 10, 2, 1},
		{1, 10, 2, 4, 8, 7},
		{1, 3, 9, 9, 3, 4, 3, 7, 4},
		{9, 1, 0, 7, 4, 8},
		{0, 3, 4, 4, 3, 7},
		{4, 8, 7},
		{10, 11, 9, 9, 11, 8},
		{9, 10, 0, 0, 10, 3, 10, 11, 3},
		{10, 11, 1, 1, 11, 0, 11, 8, 0},
		{1, 10, 3, 3, 10, 11},
		{11, 8, 2, 2, 8, 1, 8, 9, 1},
		{0, 9, 1, 11, 3, 2},
		{2, 11, 0, 0, 11, 8},
		{2, 11, 3},
		{8, 9, 3, 3, 9, 2, 9, 10, 2},
		{0, 9, 2, 2, 9, 10},
		{3, 8, 0, 10, 2, 1},
		{1, 10, 2},
		{3, 8, 1, 1, 8, 9},
		{0, 9, 1},
		{3, 8, 0},
		{},
	};

	public static final int[][] CLOSURE_TABLE = {
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{9, 5, 10, 1},
			{3, 11, 7, 8},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{4, 7, 6, 5},
			{},
			{},
			{},
			{7, 6, 5, 4, 2, 10, 6, 11, 1, 9, 5, 10},
			{},
			{6, 5, 4, 7},
			{5, 4, 7, 6, 0, 8, 4, 9, 3, 11, 7, 8},
			{4, 7, 6, 5},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{10, 6, 11, 2},
			{},
			{0, 8, 4, 9},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{1, 2, 3, 0},
			{},
			{},
			{},
			{11, 2, 10, 6},
			{1, 2, 3, 0, 6, 11, 2, 10, 7, 8, 3, 11},
			{11, 2, 10, 6},
			{7, 8, 3, 11},
			{},
			{3, 11, 7, 8},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{2, 10, 6, 11},
			{},
			{},
			{},
			{},
			{},
			{},
			{8, 4, 9, 0},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{4, 7, 6, 5, 3, 11, 7, 8, 2, 10, 6, 11},
			{},
			{},
			{},
			{7, 6, 5, 4},
			{},
			{6, 5, 4, 7, 1, 9, 5, 10, 0, 8, 4, 9},
			{5, 4, 7, 6},
			{5, 4, 7, 6},
			{},
			{},
			{},
			{},
			{},
			{0, 1, 2, 3, 5, 10, 1, 9, 6, 11, 2, 10},
			{6, 11, 2, 10},
			{2, 10, 6, 11},
			{},
			{},
			{0, 1, 2, 3},
			{},
			{10, 1, 9, 5},
			{10, 1, 9, 5},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{11, 7, 8, 3},
			{},
			{},
			{},
			{1, 9, 5, 10},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{5, 10, 1, 9},
			{},
			{3, 0, 1, 2},
			{9, 0, 8, 4},
			{},
			{},
			{},
			{3, 0, 1, 2, 4, 9, 0, 8, 5, 10, 1, 9},
			{1, 9, 5, 10},
			{},
			{},
			{9, 0, 8, 4},
			{},
			{},
			{},
			{},
			{8, 3, 11, 7},
			{},
			{2, 3, 0, 1, 7, 8, 3, 11, 4, 9, 0, 8},
			{},
			{8, 3, 11, 7},
			{},
			{4, 9, 0, 8},
			{2, 3, 0, 1},
			{},
			{},
			{0, 8, 4, 9},
			{},
			{},
			{},
			{},
			{},
			{},
			{},
			{3, 0, 1, 2},
			{},
			{},
			{},
			{},
			{2, 3, 0, 1},
			{},
			{},
			{},
			{},
			{},
	};

	private TreeMap<Integer,Integer> mJoinedVertexMap1,mJoinedVertexMap2;
	private float mOffsetX,mOffsetY,mOffsetZ,mVoxelSize;	// all in angstrom
	private MeshBuilder mMeshBuilder;
	private float[][] mSquareBuffer;
	private static boolean[] sIsCubeWithMiddlePoint;

	private synchronized void ensureMiddlePoints() {
		if (sIsCubeWithMiddlePoint == null) {
			sIsCubeWithMiddlePoint = new boolean[FACE_TABLE.length];
			for (int i=0; i<FACE_TABLE.length; i++) {
				int[] edges = FACE_TABLE[i];
				for (int edge:edges) {
					if (edge == 12) {
						sIsCubeWithMiddlePoint[i] = true;
						break;
					}
				}
			}
		}
	}

	/**
	 * Creates and empty mesh with a voxel size of 1f.
	 * Use one of the create() methods to produce the vertexes and triangles of the mesh.
	 * @param meshBuilder
	 */
	public SmoothMarchingCubesMesh(MeshBuilder meshBuilder) {
		mMeshBuilder = meshBuilder;
		mVoxelSize = 1f;
	}

	/**
	 * Creates and empty mesh with a given voxel size.
	 * Use one of the create() methods to produce a centered mesh
	 * or call setOffset() and polygonize() to create a mesh from
	 * your own provided voxel grid.
	 * @param meshBuilder
	 */
	public SmoothMarchingCubesMesh(MeshBuilder meshBuilder, float voxelSize) {
		mMeshBuilder = meshBuilder;
		mVoxelSize = voxelSize;
	}

	public void createRandom(int size) {
		final int WIDTH = 5;
		Random r = new Random();
		float[] grid = new float[size * size * size];
		for (int x=1; x<size-1; x++) { for (int y=1; y<size-1; y++) { for (int z=1; z<size-1; z++) {
			if (x <= WIDTH || x >= size-WIDTH-1
			 || y <= WIDTH || y >= size-WIDTH-1
			 || z <= WIDTH || z >= size-WIDTH-1)
				grid[x+size*y+size*size*z] = r.nextFloat();
		}}}

		mOffsetX = -mVoxelSize*size/2f;
		mOffsetY = -mVoxelSize*size/2f;
		mOffsetZ = -mVoxelSize*size/2f;

		polygonise(grid, size, size, size, 0.5f);
	}

	public void create(String filename, int sizeX, int sizeY, int sizeZ, float isoLayer) {
		float[] grid = new float[sizeX * sizeY * sizeZ];

		try {
			FileInputStream reader = new FileInputStream(filename);

			if (filename.endsWith(".vol")) {
				System.out.println("Reading "+filename+" ...");
				DataInputStream dataReader = new DataInputStream(reader);
				for (int i = 0; i < 272; i++)   // get rid of 1088 header bytes
					dataReader.readInt();

				int i = 0;
				for (int x=0; x<sizeX; x++) { for (int y=0; y<sizeY; y++) { for (int z=0; z<sizeZ; z++) {
//					if (z>100 && z<412 && y>100 && y<412 && x>30 && x<62) {
						grid[i] = dataReader.readUnsignedByte() + 256 * dataReader.readByte();
/*						if (grid[i]<0x4000)
							grid[i] = 0x4000;
						if (grid[i]>0x5FFF)
							grid[i] = 0x5FFF;
						if ((i & 0xFFFFF) == 0)
							System.out.println("grid[" + i + "]:" + grid[i]);
						}
					else {
						dataReader.readShort();
						grid[i] = 0x4000;
					}*/
					i++;
				}}}
				System.out.println("Read "+grid.length+" floats.");
			}
			else {
				byte[] buf = new byte[sizeX*sizeY];
				for (int i = 0; i < sizeZ; i++) {
					int bytes = reader.read(buf);
					if (bytes != buf.length)
						break;
					for (int j=0; j<buf.length; j++)
						grid[i*buf.length+j] = (float)buf[j];
				}
			}

			reader.close();
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
			return;
		}

		grid = smoothGrid(grid, sizeX, sizeY, sizeZ, 3);

		mOffsetX = -mVoxelSize*sizeX/2f;
		mOffsetY = -mVoxelSize*sizeY/2f;
		mOffsetZ = -mVoxelSize*sizeZ/2f;

		if (Float.isNaN(isoLayer))
			isoLayer = findBestIsoLayer(grid);

		polygonise(grid, sizeX, sizeY, sizeZ, isoLayer);
	}

	private float[] smoothGrid(float[] grid, int sizeX, int sizeY, int sizeZ, int centerMultiplier) {
		int factor = centerMultiplier - 1;
		float[] buf = new float[sizeX * sizeY * sizeZ];
		for (int x=0; x<sizeX; x++) {
			int x1 = Math.max(0, x-1)*sizeY*sizeZ;
			int x2 = Math.min(sizeX-1, x+1)*sizeY*sizeZ;
			for (int y=0; y<sizeY; y++) {
				int y1 = Math.max(0, y-1)*sizeZ;
				int y2 = Math.min(sizeY-1, y+1)*sizeZ;
				for (int z = 0; z<sizeZ; z++) {
					int z1 = Math.max(0, z-1);
					int z2 = Math.min(sizeZ-1, z+1);
					float sum = 0;
					int count = 0;
					for (int xi=x1; xi<=x2; xi+=sizeY*sizeZ) {
						for (int yi=y1; yi<=y2; yi+=sizeZ) {
							for (int zi=z1; zi<=z2; zi++) {
								sum += grid[xi+yi+zi];
								count++;
							}
						}
					}
					int i = x * sizeY * sizeZ + y * sizeZ + z;
					sum += factor * grid[i];
					count += factor;
					buf[i] = sum / count;
				}
			}
		}
	return buf;
	}

	private float findBestIsoLayer(float[] grid) {
		final int histogramSize = 256;
		float min = grid[0];
		float max = grid[0];
		for (int i=1; i<grid.length; i++) {
			if (min > grid[i])
				min = grid[i];
			if (max < grid[i])
				max = grid[i];
		}
		int[] count = new int[histogramSize+1];
		for (int i=1; i<grid.length; i++)
			count[Math.round((grid[i]-min)/(max-min)*histogramSize)]++;

		int maxChangeIndex = 0;
		int maxChange = count[1] - count[0];
		for (int i=1; i<histogramSize-1; i++) {
			if (maxChange < count[i+1] - count[i]) {
				maxChange = count[i+1] - count[i];
				maxChangeIndex = i;
			}
		}
		for (int i=0; i<count.length; i++)
			System.out.println("count["+i+"]:"+count[i]);
		System.out.println("Isolayer finding: min:"+min+" max:"+max+" iso:"+(min + (max - min) * maxChangeIndex / histogramSize));
		return min + (max - min) * maxChangeIndex / histogramSize;
	}

	public void create(String filename, int sizeX, int sizeY, int sizeZ, int x1, int x2, int y1, int y2, int z1, int z2, float isoLayer) {
		int dx = 2+x2-x1;
		int dy = 2+y2-y1;
		int dz = 2+z2-z1;
		float[] grid = new float[dx*dy*dz];

		try {
			FileInputStream reader = new FileInputStream(filename);

			byte[] buf = new byte[sizeY*sizeZ];
			for (int i=0; i<sizeX; i++) {
				int bytes = reader.read(buf);
				if (bytes != buf.length)
					break;
				int x = 1+i-x1;
				if (x >= 1 && x < dx-1) {
					for (int j = y1; j < y2; j++) {
						int y = 1+j-y1;
						for (int k = z1; k < z2; k++) {
							int z = 1+k - z1;
	//						System.out.println("x:"+x+" y:"+y+" z:"+z+" i:"+i+" j:"+j);
							grid[x * dy * dz + y * dz + z] = (float)buf[j*sizeZ+k];
						}
					}
				}
			}

			reader.close();
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
			return;
		}

		mOffsetX = -dx/2;
		mOffsetY = -dy/2f;
		mOffsetZ = -dz/2f;

		polygonise(grid, dx, dy, dz, isoLayer);
	}

	/*
	public void createTestSphere() {
		final int RADIUS = 30; // radius of test sphere in voxel
		final int BORDER = 2;
		final int GRID_SIZE = 2*RADIUS+2*BORDER;

		mIsoLayer = 5f;
		float[] grid = new float[GRID_SIZE*GRID_SIZE*GRID_SIZE];
		for (int i=0; i<GRID_SIZE; i++) {
			for (int j=0; j<GRID_SIZE; j++) {
				for (int k=0; k<GRID_SIZE; k++) {
					float dx = i-GRID_SIZE/2 + (i<GRID_SIZE/2 ? RADIUS/2 : -RADIUS/2);
//					float dx = i-GRID_SIZE/2;
					float dy = j-GRID_SIZE/2;
					float dz = k-GRID_SIZE/2;
					grid[i*GRID_SIZE*GRID_SIZE+j*GRID_SIZE+k] = mIsoLayer+(RADIUS-(float)Math.sqrt(dx*dx+dy*dy+dz*dz));
				}
			}
		}
		mOffsetX = -mVoxelSize*GRID_SIZE/2f;
		mOffsetY = -mVoxelSize*GRID_SIZE/2f;
		mOffsetZ = -mVoxelSize*GRID_SIZE/2f;

//		polygonise(grid, GRID_SIZE-2, GRID_SIZE-2, GRID_SIZE-2);    // slightly wrong array sizes give interesting shapes
		polygonise(grid, GRID_SIZE, GRID_SIZE, GRID_SIZE);
	}*/

	/**
	 * Create centered test mesh with all 256 cubes and random edge cut positions.
	 * @param cmin first cube count no to include
	 * @param cmax first cube count no to not include
	 */
	public void createAllCubes(int cmin, int cmax) {
		Random random = new Random(1);
		int count = 0;
		float[] rand = new float[8];
		float[] grid = new float[48*48*48];
		float isoLayer = 5f;
		for (int i=1; i<2; i++) {   // repetitions with different random cuts
			for (int j = 0; j < 16; j++) {
				for (int k = 0; k < 16; k++) {

					int cube = i;
//					int cube = j * 16 + k;

					int gridIndex = (i * 3 + 1) * 48 * 48 + j * 3 * 48 + k * 3 + 48 + 1;

					for (int l=0; l<rand.length; l++)
						rand[l] = random.nextFloat();

					if (count >= cmin && count < cmax) {
						grid[gridIndex + 48 * 48] = isoLayer + ((cube & 1) == 0 ? -rand[0] : rand[0]);
						grid[gridIndex + 48 * 48 + 1] = isoLayer + ((cube & 2) == 0 ? -rand[1] : rand[1]);
						grid[gridIndex + 48 * 48 + 48 + 1] = isoLayer + ((cube & 4) == 0 ? -rand[2] : rand[2]);
						grid[gridIndex + 48 * 48 + 48] = isoLayer + ((cube & 8) == 0 ? -rand[3] : rand[3]);
						grid[gridIndex] = isoLayer + ((cube & 16) == 0 ? -rand[4] : rand[4]);
						grid[gridIndex + 1] = isoLayer + ((cube & 32) == 0 ? -rand[5] : rand[5]);
						grid[gridIndex + 48 + 1] = isoLayer + ((cube & 64) == 0 ? -rand[6] : rand[6]);
						grid[gridIndex + 48] = isoLayer + ((cube & 128) == 0 ? -rand[7] : rand[7]);
						if (cmin+1==cmax) {
							System.out.println("Corner Values:");
							System.out.println("(0,0,1):" + ((cube & 1) == 0 ? -rand[0] : rand[0]));
							System.out.println("(1,0,1):" + ((cube & 2) == 0 ? -rand[1] : rand[1]));
							System.out.println("(1,1,1):" + ((cube & 4) == 0 ? -rand[2] : rand[2]));
							System.out.println("(0,1,1):" + ((cube & 8) == 0 ? -rand[3] : rand[3]));
							System.out.println("(0,0,0):" + ((cube & 16) == 0 ? -rand[4] : rand[4]));
							System.out.println("(1,0,0):" + ((cube & 32) == 0 ? -rand[5] : rand[5]));
							System.out.println("(1,1,0):" + ((cube & 64) == 0 ? -rand[6] : rand[6]));
							System.out.println("(0,1,0):" + ((cube & 128) == 0 ? -rand[7] : rand[7]));
						}
					}

/*
					grid[gridIndex + 48 * 48] = isoLayer + ((cube & 1) == 0 ? -0.5f : 0.5f);
					grid[gridIndex + 48 * 48 + 1] = isoLayer + ((cube & 2) == 0 ? -0.5f : 0.5f);
					grid[gridIndex + 48 * 48 + 48 + 1] = isoLayer + ((cube & 4) == 0 ? -0.5f : 0.5f);
					grid[gridIndex + 48 * 48 + 48] = isoLayer + ((cube & 8) == 0 ? -0.5f : 0.5f);
					grid[gridIndex] = isoLayer + ((cube & 16) == 0 ? -0.5f : 0.5f);
					grid[gridIndex + 1] = isoLayer + ((cube & 32) == 0 ? -0.5f : 0.5f);
					grid[gridIndex + 48 + 1] = isoLayer + ((cube & 64) == 0 ? -0.5f : 0.5f);
					grid[gridIndex + 48] = isoLayer + ((cube & 128) == 0 ? -0.5f : 0.5f);
*/

					count++;
				}
			}
		}

		mOffsetX = -mVoxelSize*4/2f;
		mOffsetY = -mVoxelSize*48/2f;
		mOffsetZ = -mVoxelSize*48/2f;

		polygonise(grid, 48, 48, 48, isoLayer);
	}

	public void createRandomMesh(int size) {
		Random random = new Random(3);
		float[] grid = new float[size*size*size];
		float isoLayer = 5f;
		for (int i=1; i<size-1; i++) {
			for (int j=1; j<size-1; j++) {
				for (int k=1; k<size-1; k++) {
					grid[i*size*size+j*size+k] = isoLayer+random.nextFloat()-0.5f;
				}
			}
		}

		mOffsetX = -mVoxelSize*size/2f;
		mOffsetY = -mVoxelSize*size/2f;
		mOffsetZ = -mVoxelSize*size/2f;

		polygonise(grid, size, size, size, isoLayer);
	}

	/**
	 * Defines first the voxel position in space (default is 0f,0f,0f).
	 * @param x
	 * @param y
	 * @param z
	 */
	public void setOffset(float x, float y, float z) {
		mOffsetX = x;
		mOffsetY = y;
		mOffsetZ = z;
	}

	/**
	 *
	 * @param grid values mapped to voxel corners (z has lowest significance)
	 * @param sx voxel count int x direction
	 * @param sy voxel count int y direction
	 * @param sz voxel count int z direction
	 */
	protected void polygonise(float[] grid, int sx, int sy, int sz, float isoLayer) {
		ensureMiddlePoints();
		TreeMap<Integer,Integer> edgeVertexMap = new TreeMap<Integer,Integer>();
		int[] vertexIndex = new int[13];
		mJoinedVertexMap1 = new TreeMap<Integer,Integer>();
		mJoinedVertexMap2 = new TreeMap<Integer,Integer>();
		mSquareBuffer = new float[4][3];
		int gridIndex = sy*sz;
		for (int ix=1; ix<sx; ix++) {
			// we keep track of joined vertexes left and right of currently handled voxel layer
			TreeMap<Integer,Integer> tempMap = mJoinedVertexMap1;
			tempMap.clear();
			mJoinedVertexMap1 = mJoinedVertexMap2;
			mJoinedVertexMap2 = tempMap;

			gridIndex += sz;
			for (int iy=1; iy<sy; iy++) {
				gridIndex++;
				for (int iz=1; iz<sz; iz++) {
					// Determine the index into the edge table which
					// tells us which vertices are inside of the surface
					int cubeIndex = 0;
					if (grid[gridIndex - sy * sz - sz - 1] < isoLayer) cubeIndex |= 1;
					if (grid[gridIndex - sz - 1] < isoLayer) cubeIndex |= 2;
					if (grid[gridIndex - sz] < isoLayer) cubeIndex |= 4;
					if (grid[gridIndex - sy * sz - sz] < isoLayer) cubeIndex |= 8;
					if (grid[gridIndex - sy * sz - 1] < isoLayer) cubeIndex |= 16;
					if (grid[gridIndex - 1] < isoLayer) cubeIndex |= 32;
					if (grid[gridIndex] < isoLayer) cubeIndex |= 64;
					if (grid[gridIndex - sy * sz] < isoLayer) cubeIndex |= 128;

					if ((ix < sx-1) && (iy < sy-1) && (iz < sz-1))
						tryJoinCornerVertexes(ix, iy, iz, sy, sz, grid, isoLayer);

						// Cube is entirely in/out of the surface
					if (EDGE_TABLE[cubeIndex] != 0) {

						// Find the vertices where the surface intersects the cube
						if ((EDGE_TABLE[cubeIndex] & 1) != 0)
							vertexIndex[0] = getEdgeVertexIndex(edgeVertexMap, ix - 1, iy - 1, iz - 1, sy, sz, 0, 1, grid, isoLayer);
						if ((EDGE_TABLE[cubeIndex] & 2) != 0)
							vertexIndex[1] = getEdgeVertexIndex(edgeVertexMap, ix, iy - 1, iz - 1, sy, sz, 2, 2, grid, isoLayer);
						if ((EDGE_TABLE[cubeIndex] & 4) != 0)
							vertexIndex[2] = getEdgeVertexIndex(edgeVertexMap, ix - 1, iy - 1, iz, sy, sz, 0, 1, grid, isoLayer);
						if ((EDGE_TABLE[cubeIndex] & 8) != 0)
							vertexIndex[3] = getEdgeVertexIndex(edgeVertexMap, ix - 1, iy - 1, iz - 1, sy, sz, 2, 0, grid, isoLayer);
						if ((EDGE_TABLE[cubeIndex] & 16) != 0)
							vertexIndex[4] = getEdgeVertexIndex(edgeVertexMap, ix - 1, iy, iz - 1, sy, sz, 0, 1, grid, isoLayer);
						if ((EDGE_TABLE[cubeIndex] & 32) != 0)
							vertexIndex[5] = getEdgeVertexIndex(edgeVertexMap, ix, iy, iz - 1, sy, sz, 2, 2, grid, isoLayer);
						if ((EDGE_TABLE[cubeIndex] & 64) != 0)
							vertexIndex[6] = getEdgeVertexIndex(edgeVertexMap, ix - 1, iy, iz, sy, sz, 0, 1, grid, isoLayer);
						if ((EDGE_TABLE[cubeIndex] & 128) != 0)
							vertexIndex[7] = getEdgeVertexIndex(edgeVertexMap, ix - 1, iy, iz - 1, sy, sz, 2, 0, grid, isoLayer);
						if ((EDGE_TABLE[cubeIndex] & 256) != 0)
							vertexIndex[8] = getEdgeVertexIndex(edgeVertexMap, ix - 1, iy - 1, iz - 1, sy, sz, 1, 0, grid, isoLayer);
						if ((EDGE_TABLE[cubeIndex] & 512) != 0)
							vertexIndex[9] = getEdgeVertexIndex(edgeVertexMap, ix, iy - 1, iz - 1, sy, sz, 1, 2, grid, isoLayer);
						if ((EDGE_TABLE[cubeIndex] & 1024) != 0)
							vertexIndex[10] = getEdgeVertexIndex(edgeVertexMap, ix, iy - 1, iz, sy, sz, 1, 2, grid, isoLayer);
						if ((EDGE_TABLE[cubeIndex] & 2048) != 0)
							vertexIndex[11] = getEdgeVertexIndex(edgeVertexMap, ix - 1, iy - 1, iz, sy, sz, 1, 0, grid, isoLayer);

						if (sIsCubeWithMiddlePoint[cubeIndex]) {
							vertexIndex[12] = getMiddlePointVertexIndex(cubeIndex, vertexIndex);
//							createIndicator(ix, iy, iz);
							}

						if (VERBOSE_POINTS_AND_TRIANGLES)
							System.out.println("cubeIndex:"+cubeIndex+" ix:"+ix+" iy:"+iy+" iz:"+iz);

						// Create the triangle
						int[] faces = FACE_TABLE[cubeIndex];
						for (int j = 0; j < faces.length; j += 3)
							addTriangle(vertexIndex[faces[j]], vertexIndex[faces[j+1]], vertexIndex[faces[j+2]]);

						// Some cubes require additional squares within the cube faces to close the surface.
						// This happens rarely in molecule surfaces, but frequently in more rugged scanned voxel data.
						int[] squares = CLOSURE_TABLE[cubeIndex];
						for (int j = 0; j < squares.length; j += 4)
							addSquare(vertexIndex[squares[j]], vertexIndex[squares[j+1]], vertexIndex[squares[j+2]], vertexIndex[squares[j+3]]);
					}

					gridIndex++;
				}
			}
		}
	}

	private void addTriangle(int v0, int v1, int v2) {
		if (v0 != v1 && v1 != v2 && v0 != v2)
			mMeshBuilder.addTriangle(v0, v1, v2);
	}

	private void addSquare(int v0, int v1, int v2, int v3) {
		if (v0 == v1) {
			addTriangle(v0, v2, v3);
			return;
		}
		if (v1 == v2) {
			addTriangle(v0, v1, v3);
			return;
		}
		if (v2 == v3 || v3 == v0) {
			addTriangle(v0, v1, v2);
			return;
		}

		// split the square into two triangle such that we avoid the smallest possible triangle
		mMeshBuilder.getPoint(v0, mSquareBuffer[0]);
		mMeshBuilder.getPoint(v1, mSquareBuffer[1]);
		mMeshBuilder.getPoint(v2, mSquareBuffer[2]);
		mMeshBuilder.getPoint(v3, mSquareBuffer[3]);
		float size1 = Math.min(triangleSize(0, 1, 2), triangleSize(2, 3, 0));
		float size2 = Math.min(triangleSize(1, 2, 3), triangleSize(3, 0, 1));
//		System.out.println("size1:"+size1+" size2:"+size2);
		if (size1 == 0 && size2 == 0) {
			System.out.println("size1==size2==0  v0:"+v0+" v1:"+v1+" v2:"+v2+" v3:"+v3);
			System.out.println("P0: "+mSquareBuffer[0][0]+" "+mSquareBuffer[0][1]+" "+mSquareBuffer[0][2]);
			System.out.println("P1: "+mSquareBuffer[1][0]+" "+mSquareBuffer[1][1]+" "+mSquareBuffer[1][2]);
			System.out.println("P2: "+mSquareBuffer[2][0]+" "+mSquareBuffer[2][1]+" "+mSquareBuffer[2][2]);
			System.out.println("P3: "+mSquareBuffer[3][0]+" "+mSquareBuffer[3][1]+" "+mSquareBuffer[3][2]);
		}
		else if (size1 < size2) {
			addTriangle(v1, v2, v3);
			addTriangle(v3, v0, v1);
		}
		else {
			addTriangle(v0, v1, v2);
			addTriangle(v2, v3, v0);
		}
	}

	private float triangleSize(int i0, int i1, int i2) {
		float x1 = mSquareBuffer[i0][0];
		float y1 = mSquareBuffer[i0][1];
		float z1 = mSquareBuffer[i0][2];
		float x2 = mSquareBuffer[i1][0];
		float y2 = mSquareBuffer[i1][1];
		float z2 = mSquareBuffer[i1][2];
		float x3 = mSquareBuffer[i2][0];
		float y3 = mSquareBuffer[i2][1];
		float z3 = mSquareBuffer[i2][2];
		float aSquare = (x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1);
		float bSquare = (x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1);
		float cSquare = (x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2);
		float t = aSquare+bSquare-cSquare;
//		float area = 0.25f*(float)Math.sqrt(4*aSquare*bSquare-t*t);
		return 4*aSquare*bSquare-t*t;   // since we only compare, we don't need the area
	}

	private int getMiddlePointVertexIndex(int cubeIndex, int[] vertexIndex) {
		float[] p = new float[3];
		float x = 0f;
		float y = 0f;
		float z = 0f;
		int count = 0;
		for (int i=0; i<12; i++) {
			if ((EDGE_TABLE[cubeIndex] & (1 << i)) != 0) {
				mMeshBuilder.getPoint(vertexIndex[i], p);
				x += p[0];
				y += p[1];
				z += p[2];
				count++;
			}
		}
		return mMeshBuilder.addPoint(x/count, y/count, z/count);
	}

	private void tryJoinCornerVertexes(int ix, int iy, int iz, int sy, int sz, float[] grid, float isoLayer) {
		int cx = 0;
		int cy = 0;
		int cz = 0;
		int nx = 0;
		int ny = 0;
		int nz = 0;
		float x = 0;
		float y = 0;
		float z = 0;

		// describe reference corner
		int refIndex = ix*sy*sz + iy*sz + iz;
		float refValue = grid[refIndex];
		boolean refIsOutside = (refValue < isoLayer);

		// for every edge at the reference corner check, whether the iso layer
		// crosses the edge closer than MAX_JOINT_POSITION.
		float value = grid[refIndex - sy*sz];
		if ((value < isoLayer) ^ refIsOutside) {
			nx++;
			float pos = (isoLayer - refValue) / (value - refValue);
			if (pos < MAX_JOINT_POSITION) {
				x -= pos * mVoxelSize;
				cx++;
			}
		}
		value = grid[refIndex + sy*sz];
		if ((value < isoLayer) ^ refIsOutside) {
			nx++;
			float pos = (isoLayer - refValue) / (value - refValue);
			if (pos < MAX_JOINT_POSITION) {
				x += pos * mVoxelSize;
				cx++;
			}
		}
		value = grid[refIndex - sz];
		if ((value < isoLayer) ^ refIsOutside) {
			ny++;
			float pos = (isoLayer - refValue) / (value - refValue);
			if (pos < MAX_JOINT_POSITION) {
				y -= pos * mVoxelSize;
				cy++;
			}
		}
		value = grid[refIndex + sz];
		if ((value < isoLayer) ^ refIsOutside) {
			ny++;
			float pos = (isoLayer - refValue) / (value - refValue);
			if (pos < MAX_JOINT_POSITION) {
				y += pos * mVoxelSize;
				cy++;
			}
		}
		value = grid[refIndex - 1];
		if ((value < isoLayer) ^ refIsOutside) {
			nz++;
			float pos = (isoLayer - refValue) / (value - refValue);
			if (pos < MAX_JOINT_POSITION) {
				z -= pos * mVoxelSize;
				cz++;
			}
		}
		value = grid[refIndex + 1];
		if ((value < isoLayer) ^ refIsOutside) {
			nz++;
			float pos = (isoLayer - refValue) / (value - refValue);
			if (pos < MAX_JOINT_POSITION) {
				z += pos * mVoxelSize;
				cz++;
			}
		}

		int c = cx+cy+cz;

		// if we have less than 2 close cuts, there is nothing to join
		if (c < 2)
			return;

		// If we have an (en)closed bubble with at least one non-close cut
		// then don't join vertexes and mesh bubble as it is
		if (nx+ny+nz == 6 && c != 6)
			return;

		// two separate surfaces may be close without being joined
		if ((nx==2 && ny==0 && nz==0)
		 || (nx==0 && ny==2 && nz==0)
		 || (nx==0 && ny==0 && nz==2))
			return;

		// TODO decide on this
		// if we have two opposite close cut positions, which would be joined,
		// then don't join to prevent flat triangles
//		if (cx==2 || cy==2 || cz==2)
//			return;

		if (c > 1) {
			int key = iy*(sz+1) + iz;
			int index = mMeshBuilder.addPoint(mOffsetX+ mVoxelSize *ix+x/c,
											  mOffsetY+ mVoxelSize *iy+y/c,
											  mOffsetZ+ mVoxelSize *iz+z/c);
			mJoinedVertexMap2.put(key, index);
		}
	}

	private int getEdgeVertexIndex(TreeMap<Integer,Integer> edgeVertexMap,
	                               int ix, int iy, int iz, int sy, int sz,
	                               int edgeDir, int xPos, float[] grid, float isoLayer) {
		int gridIndex = ix*sy*sz + iy*sz + iz;
		int key = 4*gridIndex+edgeDir;
		Integer indexHolder = edgeVertexMap.get(key);
		if (indexHolder != null)
			return indexHolder.intValue();

		float val1 = grid[gridIndex];
		float val2 = (edgeDir == 0) ? grid[gridIndex+sy*sz]
				: (edgeDir == 1) ? grid[gridIndex+sz] : grid[gridIndex+1];

		float pos = (val2 == val1) ? 0.5f : (isoLayer - val1) / (val2 - val1);
		float x = (edgeDir == 0) ? pos + ix : ix;
		float y = (edgeDir == 1) ? pos + iy : iy;
		float z = (edgeDir == 2) ? pos + iz : iz;

		// on pos and edgeDir find corner key and check, whether we have a joint vertex on that corner
		if (pos < MAX_JOINT_POSITION || pos > 1f-MAX_JOINT_POSITION) {
			int joinedVertexKey = Math.round(y) * (sz+1) + Math.round(z);
			TreeMap<Integer,Integer> map = (xPos == 0) ? mJoinedVertexMap1
										 : (xPos == 2) ? mJoinedVertexMap2
										 : (pos < 0.5f) ? mJoinedVertexMap1 : mJoinedVertexMap2;
			Integer joinedVertexIndex = (map.get(joinedVertexKey));
			if (joinedVertexIndex != null) {
				edgeVertexMap.put(key, joinedVertexIndex);
				return joinedVertexIndex;
				}
			}

		int index = mMeshBuilder.addPoint(mOffsetX+ mVoxelSize *x, mOffsetY+ mVoxelSize *y, mOffsetZ+ mVoxelSize *z);

		edgeVertexMap.put(key, index);
		return index;
		}

/*	private void createIndicator(int ix, int iy, int iz) {
		float d = 0.04f;
		float dx = (ix-0.5f)*mVoxelSize;
		float dy = (iy-0.5f)*mVoxelSize;
		float dz = (iz-0.5f)*mVoxelSize;
		int i1 = mMeshBuilder.addPoint(mOffsetX+dx-d, mOffsetY+dy-d, mOffsetZ+dz-d);
		int i2 = mMeshBuilder.addPoint(mOffsetX+dx+d, mOffsetY+dy-d, mOffsetZ+dz-d);
		int i3 = mMeshBuilder.addPoint(mOffsetX+dx+d, mOffsetY+dy-d, mOffsetZ+dz+d);
		int i4 = mMeshBuilder.addPoint(mOffsetX+dx-d, mOffsetY+dy-d, mOffsetZ+dz+d);
		int i5 = mMeshBuilder.addPoint(mOffsetX+dx-d, mOffsetY+dy+d, mOffsetZ+dz-d);
		int i6 = mMeshBuilder.addPoint(mOffsetX+dx+d, mOffsetY+dy+d, mOffsetZ+dz-d);
		int i7 = mMeshBuilder.addPoint(mOffsetX+dx+d, mOffsetY+dy+d, mOffsetZ+dz+d);
		int i8 = mMeshBuilder.addPoint(mOffsetX+dx-d, mOffsetY+dy+d, mOffsetZ+dz+d);
		mMeshBuilder.addTriangle(i1, i2, i3);
		mMeshBuilder.addTriangle(i3, i4, i1);
		mMeshBuilder.addTriangle(i5, i6, i7);
		mMeshBuilder.addTriangle(i7, i8, i5);
		mMeshBuilder.addTriangle(i1, i4, i8);
		mMeshBuilder.addTriangle(i8, i5, i1);
		mMeshBuilder.addTriangle(i2, i3, i7);
		mMeshBuilder.addTriangle(i7, i6, i2);
		mMeshBuilder.addTriangle(i1, i2, i6);
		mMeshBuilder.addTriangle(i6, i5, i1);
		mMeshBuilder.addTriangle(i3, i4, i8);
		mMeshBuilder.addTriangle(i8, i7, i3);
		}*/
	}

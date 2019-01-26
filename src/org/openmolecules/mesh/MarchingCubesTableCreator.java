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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;

public class MarchingCubesTableCreator {
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

	private static final int[] EDGE_ROTATION_X = {2, 10, 6, 11, 0, 9, 4, 8, 3, 1, 5, 7};
	private static final int[] CORNER_ROTATION_X = {3, 2, 6, 7, 0, 1, 5, 4};
	private static final int[] EDGE_ROTATION_Y = {1, 2, 3, 0, 5, 6, 7, 4, 9, 10, 11, 8};
	private static final int[] CORNER_ROTATION_Y = {1, 2, 3, 0, 5, 6, 7, 4};
	private static final int[] EDGE_ROTATION_Z = {9, 5, 10, 1, 8, 7, 11, 3, 0, 4, 6, 2};
	private static final int[] CORNER_ROTATION_Z = {1, 5, 6, 2, 0, 4, 7, 3};

	private static final int[][] SPECIAL_EDGES = {
			/* 01011111 */  {6, 7, 11, 4, 5, 9, 4, 7, 6, 4, 6, 5},  // square: 4, 7, 6, 5
			/* 10100111 */  {2, 11, 3, 4, 8, 7, 5, 6, 10, 4, 7, 6, 4, 6, 5, 3, 11, 7, 7, 8, 3, 2, 10, 6, 2, 6, 11},
							// squares: 4, 7, 6, 5; 3, 11, 7, 8; 2, 10, 6, 11
			/* 01010111 */  {2, 6, 7, 2, 7, 3, 4, 5, 9, 4, 7, 6, 4, 6, 5},  // square: 4, 7, 6, 5
			/* 10101011 */  {1, 5, 6, 1, 6, 2, 4, 7, 8, 4, 7, 6, 4, 6, 5},  // square: 4, 7, 6, 5
	};

	private static final int[][] SPECIAL_SQUARES = {
			/* 01011111 */  {4, 7, 6, 5},
			/* 10100111 */  {4, 7, 6, 5, 3, 11, 7, 8, 2, 10, 6, 11},
			/* 01011111 */  {4, 7, 6, 5},
			/* 01011111 */  {4, 7, 6, 5},
	};

	private static int[][] SPECIAL_CORNERS = {
			/* 01011111 */  {0, 1, 2, 3, 4, 6},
			/* 10100111 */  {0, 1, 2, 5, 7},
			/* 01010111 */  {0, 1, 2, 4, 6},
			/* 10101011 */  {0, 1, 3, 5, 7}, // enantiomer of 01246
	};

	// index 12 refers to the middle point between all other vertexes of the cube
	private static final int[][] BASE_EDGES = {
			/* 00000001 */  {0, 8, 3},
			/* 00000011 */  {1, 8, 3, 9, 8, 1},
			/* 01000001 */  {0, 8, 3, 5, 10, 6},
			/* 00000111 */  {3, 9, 8, 2, 9, 3, 2, 10, 9}, // original: {2, 8, 3, 2, 10, 8, 10, 9, 8},
			/* 00001111 */  {9, 8, 10, 10, 8, 11},
			/* 00100111 */  {2, 12, 3, 3, 12, 8, 8, 12, 4, 4, 12, 5, 5, 12, 10, 10, 12, 2},
			//* original: */ {2, 10, 5, 3, 2, 5, 3, 5, 4, 3, 4, 8},
			/* 01000111 */  // {3, 9, 8, 2, 9, 3, 2, 5, 9, 2, 6, 5},
			{2, 12, 3, 3, 12, 8, 8, 12, 9, 9, 12, 5, 5, 12, 6, 6, 12, 2}, // with middle point
			//* original: */ {5, 9, 8, 5, 8, 2, 5, 2, 6, 3, 2, 8},
			/* 10001011 */  // {1, 9, 8, 1, 8, 2, 2, 8, 7, 2, 7, 6},
			{1, 12, 2, 2, 12, 6, 6, 12, 7, 7, 12, 8, 8, 12, 9, 9, 12, 1}, // with middle point
			//* original: */ {7, 9, 8, 2, 9, 7, 2, 7, 6, 1, 9, 2},
			/* 10000111 */  {6, 11, 7, 3, 9, 8, 2, 9, 3, 2, 10, 9},
			//* original: */ {6, 11, 7, 2, 10, 3, 10, 8, 3, 10, 9, 8},
			/* 11000011 */  {10, 7, 5, 10, 11, 7, 9, 8, 1, 8, 3, 1},
			/* 10100101 */  {6, 11, 7, 1, 2, 10, 0, 8, 3, 4, 9, 5},

			// The following contain faces with corner order 1010. If we invert these cubes,
			// we need to add additional triangles within the plane of these faces to close
			// the surface. These inverted cubes are handled first (SPECIAL_EDGES).
			/* 00000101 */  {0, 8, 3, 1, 2, 10},
			/* 01000011 */  {1, 8, 3, 1, 9, 8, 5, 10, 6},
			/* 10000011 */  {1, 8, 3, 1, 9, 8, 6, 11, 7},
			/* 00100101 */  {3, 0, 8, 1, 2, 10, 4, 9, 5},
	};

	private static int[][] BASE_CORNERS = {
			/* 00000001 */ {0},
			/* 00000011 */ {0, 1},
			/* 01000001 */ {0, 6},
			/* 00000111 */ {0, 1, 2},
			/* 00001111 */ {0, 1, 2, 3},
			/* 00100111 */ {0, 1, 2, 5},
			/* 01000111 */ {0, 1, 2, 6},
			/* 10001011 */ {0, 1, 3, 7}, // enantiomer of 0126
			/* 10000111 */ {0, 1, 2, 7},
			/* 11000011 */ {0, 1, 6, 7},
			/* 10100101 */ {0, 2, 5, 7},

			/* 00000101 */ {0, 2},
			/* 01000011 */ {0, 1, 6},
			/* 10000011 */ {0, 1, 7},   // enantiomer of 016
			/* 00100101 */ {0, 2, 5},
	};

	private static final int SYMMETRIC_BASE_CUBES = 12;

	private static final int[] CORRECTION_MASKS = { 0x00F, 0x00F, 0x033, 0x033, 0x066, 0x066 };
	private static final int[] CORRECTION_VERTEXES = { 0x005, 0x00A, 0x012, 0x021, 0x024, 0x042 };
	private static final int[][] CORRECTION_FACES = { {0, 3, 2, 0, 2, 1}, {0, 3, 2, 0, 2, 1},
			{0, 4, 8, 0, 9, 4}, {0, 4, 8, 0, 9, 4}, {1, 5, 9, 1, 10, 5}, {1, 5, 9, 1, 10, 5} };

	private static int[][] triangles;
	private static int[] edges, corners;
	private static boolean isInverse;


	public static void main(String[] args) {
		triangles = new int[256][];

		for (int i = 0; i < SPECIAL_SQUARES.length; i++)
			process(SPECIAL_SQUARES[i], SPECIAL_CORNERS[i], false);
		write();

		triangles = new int[256][];
		for (int i = 0; i < BASE_EDGES.length; i++)
			process(BASE_EDGES[i], BASE_CORNERS[i], false);
		for (int i = 0; i < BASE_EDGES.length; i++)
			process(BASE_EDGES[i], BASE_CORNERS[i], true);
//		correct();
		write();
	}

	private static void process(int[] inEdges, int[] inCorners, boolean invert) {
		edges = inEdges.clone();
		corners = inCorners.clone();
		isInverse = invert;
		if (invert) {
			boolean[] isPresent = new boolean[8];
			for (int corner : corners)
				isPresent[corner] = true;
			corners = new int[8 - corners.length];
			int index = 0;
			for (int j = 0; j < isPresent.length; j++)
				if (!isPresent[j])
					corners[index++] = j;
		}

		for (int j = 0; j < 4; j++) {
			rotate4Y();
			rotateX();
		}

		rotateZ();
		rotate4Y();

		rotateZ();
		rotateZ();
		rotate4Y();
	}

	private static void rotate4Y() {
		for (int j = 0; j < 4; j++) {
			saveEdges();
			rotateY();
		}
	}

	private static void saveEdges() {
		int[] edge = edges.clone();

		int index = 0;
		for (int corner : corners)
			index += (1 << corner);

		if (isInverse) {
			for (int i = 0; i < edge.length; i += 3) {
				int temp = edge[i];
				edge[i] = edge[i + 2];
				edge[i + 2] = temp;
			}
		}

		if (triangles[index] == null) {
			triangles[index] = edge;
		} else {
//			System.out.println("already used: " + index);
//			for (int i=0; i<edge.length; i++)
//				if (triangles[index][i] != edge[i])
//					System.out.println("mismatch: " + index);
		}

	}

	private static void rotateY() {
		for (int i = 0; i < edges.length; i++)
			edges[i] = (edges[i] == 12) ? 12 : EDGE_ROTATION_Y[edges[i]];
		for (int i = 0; i < corners.length; i++)
			corners[i] = CORNER_ROTATION_Y[corners[i]];
	}

	private static void rotateX() {
		for (int i = 0; i < edges.length; i++)
			edges[i] = (edges[i] == 12) ? 12 : EDGE_ROTATION_X[edges[i]];
		for (int i = 0; i < corners.length; i++)
			corners[i] = CORNER_ROTATION_X[corners[i]];
	}

	private static void rotateZ() {
		for (int i = 0; i < edges.length; i++)
			edges[i] = (edges[i] == 12) ? 12 : EDGE_ROTATION_Z[edges[i]];
		for (int i = 0; i < corners.length; i++)
			corners[i] = CORNER_ROTATION_Z[corners[i]];
	}

	private static void correct() {
		for (int i=0; i<CORRECTION_MASKS.length; i++) {
			int mask = CORRECTION_MASKS[i];
			int vertexes = CORRECTION_VERTEXES[i];
			int[] faces = CORRECTION_FACES[i];
			for (int j = 0; j< triangles.length; j++) {
				if ((j & mask) == vertexes) {
					int[] triangle = new int[triangles[j].length + faces.length];
					for (int k = 0; k < triangles[j].length; k++)
						triangle[k] = triangles[j][k];
					for (int k = 0; k < faces.length; k++)
						triangle[triangles[j].length + k] = faces[k];
					triangles[j] = triangle;
				}
			}
		}
	}

	private static void write() {
		int emptyCount = 0;
		StringWriter sw = new StringWriter();
		BufferedWriter writer = new BufferedWriter(sw);
		try {
			for (int[] edge : triangles) {
				writer.write("\t\t\t{");
				if (edge != null) {
					for (int i = 0; i < edge.length; i++) {
						writer.write(Integer.toString(edge[i]));
						if (i < edge.length - 1)
							writer.write(", ");
					}
				} else {
					emptyCount++;
				}
				writer.write("},");
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
		}
		System.out.println(sw.getBuffer().toString());
		System.out.println("Empty: " + emptyCount);
	}
}
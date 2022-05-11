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

import javafx.scene.shape.TriangleMesh;

public class TestMesh extends TriangleMesh {
    private static final int DIVISIONS = 50;
    private static final int LAYERS = 10;

    public TestMesh(float radius, float height) {
        super();

        //Generate the segments of the bottom circle (Cone Base)
        double segmentAngle = 2.0 * Math.PI / DIVISIONS;
        float layerHeight = height / LAYERS;

        getPoints().addAll(0, 0, -height/2);

        for (int layer=0; layer<=LAYERS; layer++) {
            float f = 1.5f - (float)Math.cos(2*Math.PI/3*((double)layer/LAYERS-0.5));
            float z = layer * layerHeight - height/2;
            double angle = layer * segmentAngle / 2;
            for(int i=0; i<DIVISIONS; i++) {
                float x = f * (float)(radius * Math.cos(angle));
                float y = f * (float)(radius * Math.sin(angle));
                getPoints().addAll(x, y, z);
                angle += segmentAngle;
//                System.out.println("x:"+x+" y:"+y+" z:"+z);
                }
            }

        getPoints().addAll(0, 0, height/2);

        getTexCoords().addAll(0,0); 

        for (int layer=0; layer<LAYERS; layer++) {
            int offset = 1 + layer * DIVISIONS;

            for(int i=0; i<DIVISIONS; i++) {
                int next = (i == DIVISIONS-1) ? 0 : i+1;
                getFaces().addAll(
                    offset+next,0, offset+DIVISIONS+i,0, offset+i,0,
                    offset+next,0, offset+DIVISIONS+next,0, offset+DIVISIONS+i,0); 
                }
            }

        int offset = 1+LAYERS*DIVISIONS;
        for(int i=0; i<DIVISIONS; i++) {
            int next = (i == DIVISIONS-1) ? 0 : i+1;
            getFaces().addAll(
                0,0, 1+next,0, 1+i,0,
                offset+i,0, offset+next,0, offset+DIVISIONS,0);
            }
        }
    }

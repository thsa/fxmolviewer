package org.openmolecules.fx.viewer3d.nodes;

import com.actelion.research.chem.Coordinates;

import static org.openmolecules.fx.viewer3d.nodes.Ribbon.POINTS_PER_SECTION;
import static org.openmolecules.fx.viewer3d.nodes.Ribbon.SECTIONS_PER_RESIDUE;

public class RibbonCalculator {
private static final float SPLINE_FACTOR = 0.8f;
    private static final float[][] SPLINE = {
            { -SPLINE_FACTOR, -SPLINE_FACTOR + 2f, SPLINE_FACTOR - 2f, SPLINE_FACTOR },
            { 2 * SPLINE_FACTOR, SPLINE_FACTOR - 3.0f, -2 * SPLINE_FACTOR + 3.0f, -SPLINE_FACTOR },
            { -SPLINE_FACTOR, 0f, SPLINE_FACTOR, 0f },
            { 0f, 1f, 0f, 0f } };

    private static final float RIBBON_RADIUS = 0.25f;
    private static final float RIBBON_WIDTH = 0.7f;
    private static final float CARTOON_RADIUS = 0.1f;
    private static final float CARTOON_WIDTH = 1.0f;
    private static final float CARTOON_COIL_SIZE = 0.2f;

    public RibbonCalculator(Ribbon ribbons) {
        mRibbon = ribbons;
    }

    private final Ribbon mRibbon;

    /**
     * This method calculates smooth residue coordinates and ribbon offset vectors for each
     * residue that defines the ribbon rotation. This results in a flattened, but elliptic
     * ribbon with a uniform cross-section shape from start to end.
     */
    public void drawRibbons() {
        Coordinates[] coords = new Coordinates[mRibbon.getResidues()];
        Coordinates[] offset = new Coordinates[mRibbon.getResidues()];

        int typeCA = mRibbon.getAtomType("CA");
        int typeO = mRibbon.getAtomType("O");

        // calculate offsets
        int fragmentCount = mRibbon.getFragmentCount();
        for (int fragment=0; fragment<fragmentCount; fragment++) {
            boolean cyclic= mRibbon.isCyclicFragment(fragment);
            int residues = mRibbon.getResiduesInFragment(fragment);
            if (residues < 2)
                continue;

            int firstResidue = mRibbon.getFirstResidueInFragment(fragment);
            int atomCA = mRibbon.getAtomInResidue(firstResidue, typeCA);
            int atomO = mRibbon.getAtomInResidue(firstResidue, typeO);

            if (atomCA == -1 || atomO == -1)
                continue;    // skip if we cannot find first CA or O

            Coordinates coordCA = mRibbon.getAtomCoords(atomCA);
            Coordinates lastCoordCA = coordCA;
            Coordinates coordO = mRibbon.getAtomCoords(atomO);
            Coordinates lastCoordO = coordO;
            Coordinates backboneOffset = new Coordinates();

            if (cyclic) {
                int lastResidue = firstResidue + residues - 1;
                int lastAtomCA = mRibbon.getAtomInResidue(lastResidue, typeCA);
                lastCoordCA = mRibbon.getAtomCoords(lastAtomCA);

                int lastAtomO = mRibbon.getAtomInResidue(lastResidue, typeO);
                lastCoordO = mRibbon.getAtomCoords(lastAtomO);
                mixInResidueOffset(backboneOffset, coordCA, lastCoordCA, lastCoordO);
            }

            for (int loop=0; loop<residues; loop++) {
                int residue = firstResidue + loop;

                atomCA = mRibbon.getAtomInResidue(residue, typeCA);
                if (atomCA != -1)
                    coordCA = mRibbon.getAtomCoords(atomCA);

                atomO = mRibbon.getAtomInResidue(residue, typeO);
                coordO = (atomO != -1) ? mRibbon.getAtomCoords(atomO) : lastCoordO;

                // copy the CA coordinate into the control point array
                coords[loop] = new Coordinates(coordCA.x, coordCA.y, coordCA.z);

                // now I need to figure out where the ribbon goes
                mixInResidueOffset(backboneOffset, coordCA, lastCoordCA, lastCoordO);
                offset[loop] = new Coordinates(backboneOffset);
                lastCoordCA = coordCA;
                lastCoordO = coordO;
            }

            if (!cyclic)
                offset[0].set(offset[1]);

            float[] widths = new float[1];
            float[] heights = new float[1];
            widths[0] = RIBBON_WIDTH;
            heights[0] = RIBBON_RADIUS;
            drawFromAtomCACoords(fragment, coords, offset, widths, heights, cyclic);
        }
    }

    private void drawFromAtomCACoords(int fragment, Coordinates[] inCoords, Coordinates[] inOffset,
                                      float[] residueWidth, float[] residueHeight, boolean cyclic) {
        int residues = mRibbon.getResiduesInFragment(fragment);

        float invSectionsPerResidue = 1.0f / ((float) SECTIONS_PER_RESIDUE);
        int halfSectionsPerResidue = SECTIONS_PER_RESIDUE / 2;

        Coordinates[] point  = new Coordinates[SECTIONS_PER_RESIDUE * (residues + 1)];
        Coordinates[] offset = new Coordinates[SECTIONS_PER_RESIDUE * (residues + 1)];
        for (int i=0; i<point.length; i++)
            point[i] = new Coordinates();
        for (int i=0; i<offset.length; i++)
            offset[i] = new Coordinates();

        float[] widths;
        float[] heights;
        boolean scaleResidues = (residueWidth.length != 1);
        if (scaleResidues) {
            int scaleFactors = SECTIONS_PER_RESIDUE * (residues + 1);
            widths = new float[scaleFactors];
            heights = new float[scaleFactors];
        }
        else {
            widths  = residueWidth;
            heights = residueHeight;
        }

        float[][] q = new float[4][3];

        int sectionCount = 0;
        for (int cp3=0; cp3<residues; cp3++) {
            int cp2, cp1, cp4;
            if (!cyclic) {
                cp2 = Math.max((cp3 - 1), 0);
                cp1 = Math.max((cp3 - 2), 0);
                cp4  = (cp3 + 1 < residues) ? (cp3 + 1) : (residues-1);
            } else {
                cp2 = (residues + cp3 - 1) % residues;
                cp1 = (residues + cp3 - 2) % residues;
                cp4  = (residues + cp3 + 1) % residues;
            }

            // construct the interpolation points (into the "pts" array)
            // remember, these are offset by two to keep some information
            // about the previous residue in the first 2 elements
            calculateQMatrix(q,
                  inCoords[cp1],
                  inCoords[cp2],
                  inCoords[cp3],
                  inCoords[cp4]);

            // calculated interpolated spline points
            for (int i = 0; i< SECTIONS_PER_RESIDUE; i++)
                calculateInterpolation(point[sectionCount + i], i * invSectionsPerResidue, q);

            // interpolate offsets
            for (int i = 0; i< SECTIONS_PER_RESIDUE; i++) {
                float f = i * invSectionsPerResidue;
                offset[sectionCount + i].set((1f - f) * inOffset[cp2].x + f * inOffset[cp2+1].x,
                                             (1f - f) * inOffset[cp2].y + f * inOffset[cp2+1].y,
                                             (1f - f) * inOffset[cp2].z + f * inOffset[cp2+1].z);
                offset[sectionCount + i].unit();
            }

            // interpolate widths and heights
            if (scaleResidues) {
                float width2 = residueWidth[cp2];
                float width3 = residueWidth[cp3];
                float height2 = residueHeight[cp2];
                float height3 = residueHeight[cp3];

                if (width3 >= 0 && width2 >= 0) {
                    for (int i = 0; i< SECTIONS_PER_RESIDUE; i++) {
                        float f = i * invSectionsPerResidue;
                        widths[sectionCount + i]  = (1f - f) * width2 + f * width3;
                        heights[sectionCount + i] = (1f - f) * height2 + f * height3;
                    }
                } else {
                    float width4 = residueWidth[cp4];
                    if (width4 < 0)
                        width4 = -width4;

                    if (width3 < 0) {
                        width3 = -width3;

                        for (int i=0; i<halfSectionsPerResidue; i++) {
                            float f = i * invSectionsPerResidue;
                            widths[sectionCount + i]  = width2;
                            heights[sectionCount + i] = (1f - f) * height2 + f * height3;
                        }
                        for (int i = halfSectionsPerResidue; i< SECTIONS_PER_RESIDUE; i++) {
                            float f1 = i * invSectionsPerResidue;
                            float f2 = (i-halfSectionsPerResidue) * invSectionsPerResidue;
                            widths[sectionCount + i]  = (1f - f2) * width3 + f2 * width4;
                            heights[sectionCount + i] = (1f - f1) * height2 + f1 * height3;
                        }
                    } else {
                        width2 = -width2;

                        for (int i=0; i<halfSectionsPerResidue; i++) {
                            float f1 = i * invSectionsPerResidue;
                            float f2 = (i + (SECTIONS_PER_RESIDUE - halfSectionsPerResidue)) * invSectionsPerResidue;
                            widths[sectionCount + i]  = (1f - f2) * width2 + f2 * width3;
                            heights[sectionCount + i] = (1f - f1) * height2 + f1 * height3;
                        }
                        for (int i = halfSectionsPerResidue; i< SECTIONS_PER_RESIDUE; i++) {
                            float f = i * invSectionsPerResidue;
                            widths[sectionCount + i]  = width3;
                            heights[sectionCount + i] = (1f - f) * height2 + f * height3;
                        }
                    }
                }
            }

            sectionCount += SECTIONS_PER_RESIDUE;
        }

        // Last cross-section
        int cp2, cp1, cp4;
        if (!cyclic) {
            cp2 = Math.max(residues - 1, 0);
            cp1 = Math.max(residues - 2, 0);
            cp4  = residues-1;
        } else {
            cp2 = residues - 1;
            cp1 = residues - 2;
            cp4  = 1;
        }

        calculateInterpolation(point[sectionCount], 1.0f, q);
        offset[sectionCount].set(inOffset[cp1 + 1]);
        offset[sectionCount].unit();

        if (scaleResidues) {
            float width3 = residueWidth[cp2];  // cp3 is not available
            float width2 = residueWidth[cp2];
            float height2 = residueHeight[cp2];

            if (width3 >= 0 && width2 >= 0) {
                widths[sectionCount] = width2;
                heights[sectionCount] = height2;
            } else {
                float width4 = residueWidth[cp4];
                if (width4 < 0)
                    width4 = -width4;

                if (width3 < 0) {
                    width3 = -width3;

                    float f = (SECTIONS_PER_RESIDUE - halfSectionsPerResidue) * invSectionsPerResidue;
                    widths[sectionCount]  = (1f - f) * width3 + f * width4;
                    heights[sectionCount] = width2;
                } else {
                    widths[sectionCount]  = width3;
                    heights[sectionCount] = height2;
                }
            }
        }

        sectionCount++;

        drawFromInterpolatedBackbone(fragment, sectionCount, point, offset, widths, heights, scaleResidues);
    }


    /**
     * Draws a ribbon or cartoon from point coordinates and offsets
     * and optionally scale every section's width and height.
     */
    private void drawFromInterpolatedBackbone(int fragment, int sections, Coordinates[] point, Coordinates[] offset,
                                              float[] widths, float[] heights, boolean scaleResidues) {
        if (sections < 2)
            return;

        int pointCount = sections * POINTS_PER_SECTION;

        Coordinates[] points = new Coordinates[pointCount];
        Coordinates[] normals = new Coordinates[pointCount];

        // cross-section buffer
        float[] sectionPoint = new float[POINTS_PER_SECTION * 2];
        float[] sectionNormal = new float[POINTS_PER_SECTION * 2];
        float[] sectionTemplate = new float[POINTS_PER_SECTION * 2];

        float angle = 2f * (float)Math.PI / POINTS_PER_SECTION;
        for (int p = 0; p< POINTS_PER_SECTION; p++) {
            sectionTemplate[2*p] = (float)Math.sin(p * angle);
            sectionTemplate[2*p+1] = (float)Math.cos(p * angle);
        }

        Coordinates coordDirection = new Coordinates();
        Coordinates offsetDirection = new Coordinates();

        float previousWidth  = Float.NaN;
        float previousHeight = Float.NaN;

        float width  =  widths[0];
        float height = heights[0];

        // Calculate all cross-sections from backbone coordinates and offsets.
        for (int section=0; section<sections; section++) {
            if (scaleResidues) {
                width  = widths[section];
                height = heights[section];
            }

            // Calculate coordinate offsets and normals for cross-section
            // if not done before or if width or height have changed.
            if (width != previousWidth || height != previousHeight) {
                previousWidth = width;
                previousHeight = height;

                float invWidth = 1.0f / width;
                float invHeight = 1.0f / height;

                for (int p = 0; p<(POINTS_PER_SECTION /2); p++) {
                    sectionPoint[2*p] = width - height + height * sectionTemplate[2*p];
                    sectionPoint[2*p+1] = height * sectionTemplate[2*p+1];
                }
                for (int p = (POINTS_PER_SECTION /2); p< POINTS_PER_SECTION; p++) {
                    sectionPoint[2*p] = height - width + height * sectionTemplate[2*p];
                    sectionPoint[2*p+1] = height * sectionTemplate[2*p+1];
                }

                for (int p = 0; p< POINTS_PER_SECTION; p++) {
                    float x = invWidth  * sectionTemplate[2*p];
                    float y = invHeight * sectionTemplate[2*p+1];
                    float invLen = 1.0f / (float)Math.sqrt(x*x + y*y);
                    sectionNormal[2*p] = x * invLen;
                    sectionNormal[2*p+1] = y * invLen;
                }
            }

            if (section != (sections - 1)) {
                coordDirection.set(point[section]);
                coordDirection.sub(point[section+1]);
            } else {
                coordDirection.set(point[section-1]);
                coordDirection.sub(point[section]);
            }
            coordDirection.unit();

            offsetDirection.set(offset[section]);

            Coordinates upDirection = coordDirection.cross(offsetDirection).unit();

            for (int p = 0; p< POINTS_PER_SECTION; p++) {
                float x = sectionPoint[2*p];
                float y = sectionPoint[2*p+1];
                int index = (section * POINTS_PER_SECTION + p);
                points[index] = new Coordinates(point[section]);
                points[index].add(x * offsetDirection.x, x * offsetDirection.y, x * offsetDirection.z);
                points[index].add(y * upDirection.x, y * upDirection.y, y * upDirection.z);
            }

            for (int p = 0; p< POINTS_PER_SECTION; p++) {
                float x = sectionNormal[2*p];
                float y = sectionNormal[2*p+1];
                normals[section * POINTS_PER_SECTION + p] = new Coordinates(
                        x * offsetDirection.x + y * upDirection.x,
                        x * offsetDirection.y + y * upDirection.y,
                        x * offsetDirection.z + y * upDirection.z);
            }
        }

        mRibbon.createTriangeMesh(points, normals, fragment);
    }

    /**
     * Considering secondary structure assignments to the residues (helix, beta, other)
     * this method calculates smooth residue coordinates with a flattened backbone
     * in case of beta sheets. It creates the ribbon offset vector for each residue that
     * defines the ribbon rotation, and it assigns residue specific ribbon width and height
     * values depending on the secondary structure assignment. This results in a flat ribbon
     * with a flat arrow at the end of a beta sequence and a hose connecting helix and beta
     * sequences.
     */
    public void drawCartoons() {
        Coordinates[] coords = new Coordinates[mRibbon.getResidues()];
        Coordinates[] offset = new Coordinates[mRibbon.getResidues()];

        float[] widths = new float[mRibbon.getResidues()];
        float[] heights = new float[mRibbon.getResidues()];

        int typeCA = mRibbon.getAtomType("CA");
        int typeO = mRibbon.getAtomType("O");

        // calculate offsets
        int fragmentCount = mRibbon.getFragmentCount();
        for (int fragment=0; fragment<fragmentCount; fragment++) {
            boolean cyclic = mRibbon.isCyclicFragment(fragment);
            int residues = mRibbon.getResiduesInFragment(fragment);
            if (residues < 2)
                continue;

            int residue = mRibbon.getFirstResidueInFragment(fragment);
            int atomCA = mRibbon.getAtomInResidue(residue, typeCA);
            int atomO = mRibbon.getAtomInResidue(residue, typeO);

            if (atomCA == -1 || atomO == -1)
                continue;    // skip if we cannot find first CA or O

            int currentAtomCA = atomCA;

            Coordinates coordCA = mRibbon.getAtomCoords(atomCA);
            Coordinates lastCoordCA = coordCA;
            Coordinates coordO = mRibbon.getAtomCoords(atomO);
            Coordinates lastCoordO = coordO;
            Coordinates backboneOffset = new Coordinates();

            if (cyclic) {
                int lastResidue = residue + residues - 1;
                int lastAtomCA = mRibbon.getAtomInResidue(lastResidue, typeCA);
                lastCoordCA = mRibbon.getAtomCoords(lastAtomCA);
                int lastAtomO = mRibbon.getAtomInResidue(lastResidue, typeO);
                lastCoordO = mRibbon.getAtomCoords(lastAtomO);
                mixInResidueOffset(backboneOffset, coordCA, lastCoordCA, lastCoordO);
            }

            for (int r=0; r<residues; r++) {
                residue = mRibbon.getFirstResidueInFragment(fragment) + r;

                int newAtomCA = mRibbon.getAtomInResidue(residue, typeCA);
                if (newAtomCA >= 0) {
                    currentAtomCA = atomCA;
                    atomCA = newAtomCA;
                    coordCA = mRibbon.getAtomCoords(atomCA);
                }

                atomO = mRibbon.getAtomInResidue(residue, typeO);
                coordO = (atomO >= 0) ? mRibbon.getAtomCoords(atomO) : lastCoordO;

                if (mRibbon.isSSHelix(residue)) {
                    widths[r] = CARTOON_WIDTH;
                    heights[r] = CARTOON_RADIUS;
                    coords[r] = coordCA;
                }
                else if (mRibbon.isSSBeta(residue)) {
                    widths[r] = CARTOON_WIDTH;
                    heights[r] = CARTOON_RADIUS;

                    boolean drawArrow = false;

                    // add planarity to the beta sheet.
                    int nextAtomCA = -1;

                    if ((r + 1) < residues) {
                        int nextResidue= mRibbon.getFirstResidueInFragment(fragment) + r + 1;
                        nextAtomCA = mRibbon.getAtomInResidue(nextResidue, typeCA);

                        if (!mRibbon.isSSBeta(nextResidue))
                            drawArrow = true;
                    } else {
                        drawArrow = true;
                    }

                    if (drawArrow)
                        widths[r] = -CARTOON_WIDTH * 1.8f;

                    if (nextAtomCA < 0)
                        nextAtomCA = atomCA;

                    Coordinates coordsBeta = new Coordinates(mRibbon.getAtomCoords(atomCA));
                    coordsBeta.scale(2f);
                    coordsBeta.add(mRibbon.getAtomCoords(nextAtomCA));
                    coordsBeta.add(mRibbon.getAtomCoords(currentAtomCA));
                    coordsBeta.scale(0.25f);

                    coords[r] = coordsBeta;
                }
                else {
                    widths[r] = CARTOON_COIL_SIZE;
                    heights[r] = CARTOON_COIL_SIZE;
                    coords[r] = coordCA;
                }

                mixInResidueOffset(backboneOffset, coordCA, lastCoordCA, lastCoordO);
                offset[r] = new Coordinates(backboneOffset);
                lastCoordCA = coordCA;
                lastCoordO = coordO;
            }

            if (!cyclic)
                offset[0] = new Coordinates(offset[1]);

            drawFromAtomCACoords(fragment, coords, offset, widths, heights, cyclic);
        }
    }

    private void mixInResidueOffset(Coordinates meanOffset, Coordinates coordCA, Coordinates lastCoordCA, Coordinates lastCoordO) {
        Coordinates backboneDir = coordCA.subC(lastCoordCA);
        Coordinates lastCarbonylDir = lastCoordO.subC(lastCoordCA);
        Coordinates peptidePlane = backboneDir.cross(lastCarbonylDir);     // peptide plane of previous residue
        Coordinates orthogonal = peptidePlane.cross(backboneDir);          // orthogonal to plane and backbone
        if (orthogonal.dot(meanOffset) < 0)
            orthogonal.negate();
        meanOffset.add(orthogonal).unit();
    }

    private void calculateInterpolation(Coordinates out, float w, float[][] q) {
        out.x = w * (w * (w * q[0][0] + q[1][0]) + q[2][0]) + q[3][0];
        out.y = w * (w * (w * q[0][1] + q[1][1]) + q[2][1]) + q[3][1];
        out.z = w * (w * (w * q[0][2] + q[1][2]) + q[2][2]) + q[3][2];
    }

    private void calculateQMatrix(float[][] q, Coordinates pts1, Coordinates pts2,
                                               Coordinates pts3, Coordinates pts4) {
        for (int i=0; i<4; i++) {
            q[i][0] = SPLINE[i][0] * (float)pts1.x
                    + SPLINE[i][1] * (float)pts2.x
                    + SPLINE[i][2] * (float)pts3.x
                    + SPLINE[i][3] * (float)pts4.x;
            q[i][1] = SPLINE[i][0] * (float)pts1.y
                    + SPLINE[i][1] * (float)pts2.y
                    + SPLINE[i][2] * (float)pts3.y
                    + SPLINE[i][3] * (float)pts4.y;
            q[i][2] = SPLINE[i][0] * (float)pts1.z
                    + SPLINE[i][1] * (float)pts2.z
                    + SPLINE[i][2] * (float)pts3.z
                    + SPLINE[i][3] * (float)pts4.z;
        }
    }
}

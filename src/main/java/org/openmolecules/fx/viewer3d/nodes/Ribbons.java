/*
 * FXMolViewer, showing and manipulating molecules and protein structures in 3D.
 * Copyright (C) 2024 Thomas Sander

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

package org.openmolecules.fx.viewer3d.nodes;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.Molecule3D;
import com.actelion.research.chem.StereoMolecule;
import javafx.collections.ObservableFloatArray;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import org.openmolecules.fx.viewer3d.V3DMolecule;

import java.util.ArrayList;

public class Ribbons {
    public static final int MODE_NONE = 0;
    public static final int MODE_RIBBON = 1;
    public static final int MODE_CARTOON = 2;
    public static final String[] RIBBON_MODE_CODE = { "none", "ribbons", "cartoons" };

    private static final String[] PROTEIN_ATOM_TYPE = { "N", "CA", "C", "O" };
    public static final int ATOM_TYPE_N = 0;
    public static final int ATOM_TYPE_CA = 1;
    public static final int ATOM_TYPE_C = 2;
    public static final int ATOM_TYPE_O = 3;
    public static final int POINTS_PER_SECTION = 16;
    public static final int SECTIONS_PER_RESIDUE = 9;
    private static final int SS_OTHER = 0;
    private static final int SS_HELIX = 1;
    private static final int SS_BETA = 2;
    private static final int HUE_START = 30;
    private static final int HUE_SPAN = 300;
    private static final int COLOR_MODE_BY_SS_SECTION = 1;
    private static final int COLOR_MODE_BY_FRAGMENT = 2;
    private static final int COLOR_MODE_BY_RESIDUE = 3;
    private static final int COLOR_MODE = COLOR_MODE_BY_SS_SECTION;

    private int[][][] mResidueAtom;
    private int[][] mResidueColor;
    private int[][] mSSType;
    private V3DMolecule mMol3D;
    private final StereoMolecule mMol;
    private MeshView[] mRibbonMesh;
    private Color mColor;   // ribbon color; if null, then mesh colors are according to COLOR_MODE
    private boolean[] mIsBackboneAtom;

    /**
     * Calculates and adds a ribbon or cartoon visualization of the protein backbone
     * to the protein V3DMolecule. The ribbon or cartoon may consist of multiple sections,
     * i.e. fragments, if the protein's residues use multiple chain-IDs or if the
     * residues are not in increasing order, which happens, e.g., due to cropping.
     * @param mol
     * @param mol3D
     */
    public Ribbons(StereoMolecule mol, V3DMolecule mol3D) {
        mMol3D = mol3D;
        mMol = mol;
        mColor = mMol3D.getColor();

        mResidueAtom = readBackbone();  // works, if mMol is Molecule3D from PDB entry
        if (mResidueAtom == null)
            mResidueAtom = determineBackbone();

        mRibbonMesh = new MeshView[mResidueAtom.length];
    }

    private Ribbons(StereoMolecule mol) {
        mMol = mol;
    }

    /**
     * Helper method to determine all backbone atoms even if the molecule was not read from a PDB-file
     * @param mol
     * @return
     */
    public static boolean[] determineBackboneAtoms(StereoMolecule mol) {
        Ribbons ribbons = new Ribbons(mol);
        int[][][] residueAtom = ribbons.readBackbone();  // works, if mMol is Molecule3D from PDB entry
        if (residueAtom == null)
            residueAtom = ribbons.determineBackbone();
        return ribbons.mIsBackboneAtom;
    }

    private int[][][] readBackbone() {
        if (!(mMol instanceof Molecule3D)
         || mMol.getAllAtoms() == 0
         || ((Molecule3D)mMol).getResSequence(0) == -1)
            return null;

        Molecule3D mol = (Molecule3D)mMol;
        mol.ensureHelperArrays(Molecule.cHelperNeighbours);
        mIsBackboneAtom = new boolean[mol.getAtoms()];

        ArrayList<int[]> atomsList = new ArrayList<>();
        int[] atoms = null;
        int previousResidue = -1;
        int atomsFound = 0;
        for (int atom=0; atom<mMol.getAtoms(); atom++) {
            int residue = mol.getResSequence(atom);
            if (residue != previousResidue) {
                previousResidue = residue;
                atoms = new int[PROTEIN_ATOM_TYPE.length];
                atomsFound = 0;
            }
            String atomName = mol.getAtomName(atom);
            for (int i=0; i<PROTEIN_ATOM_TYPE.length; i++) {
                if (PROTEIN_ATOM_TYPE[i].equals(atomName)) {
                    atoms[i] = atom;
                    atomsFound++;
                    if (atomsFound == PROTEIN_ATOM_TYPE.length) {
                        atomsList.add(atoms);
                        for (int a : atoms)
                            mIsBackboneAtom[a] = true;
                    }
                    break;
                }
            }
        }

        ArrayList<int[][]> fragmentList = new ArrayList<>();
        ArrayList<int[]> residueList = new ArrayList<>();

        // We split into fragments when either the chainID changes or when the
        // residue-ID differs more than 1 from the previous residue. This happens,
        // for instance, after cropping and parts of the protein are cut away.
        int chainIDAtomType = 1;  // we use C-alpha for chain-ID lookup
        if (!atomsList.isEmpty()) {
            residueList.add(atomsList.get(0));
            int atomCA = atomsList.get(0)[chainIDAtomType];
            int lastResidue = mol.getResSequence(atomCA);
            String lastChainID = mol.getAtomChainId(atomCA);
            for (int i=1; i<atomsList.size(); i++) {
                atomCA = atomsList.get(i)[chainIDAtomType];
                int residue = mol.getResSequence(atomCA);
                String chainID = mol.getAtomChainId(atomCA);
                if (lastResidue != residue-1
                 || !lastChainID.equals(chainID)) {
                    if (residueList.size() >= 2)
                        fragmentList.add(residueList.toArray(new int[0][]));
                    residueList.clear();
                }

                residueList.add(atomsList.get(i));
                lastResidue = residue;
                lastChainID = chainID;
            }
        }

        if (residueList.size() >= 2)
            fragmentList.add(residueList.toArray(new int[0][]));

        return fragmentList.toArray(new int[0][][]);
    }

    /**
     * Our molecules are not necessarily read from PDB files and atoms are not
     * necessarily in residue order. Thus, we need to determine any protein
     * chains ourselves rather than using atom sequence IDs and PDB atom types.
     */
    private int[][][] determineBackbone() {
        mMol.ensureHelperArrays(Molecule.cHelperNeighbours);

        ArrayList<int[][]> fragmentList = new ArrayList<>();
        ArrayList<int[]> residueList = new ArrayList<>();
        mIsBackboneAtom = new boolean[mMol.getAtoms()];
        for (int atom=0; atom<mMol.getAtoms(); atom++) {
            int[] residueAtom = findResidue(atom);
            if (residueAtom != null) {
                residueList.add(residueAtom);
                while ((residueAtom = getNextResidue(residueAtom[ATOM_TYPE_C])) != null)
                    residueList.add(residueAtom);
                while ((residueAtom = getPreviousResidue(residueList.get(0)[ATOM_TYPE_N])) != null)
                    residueList.add(0, residueAtom);

                if (residueList.size() >= 2)
                    fragmentList.add(residueList.toArray(new int[0][]));

                residueList.clear();
            }
        }

        return fragmentList.toArray(new int[0][][]);
    }

    /**
     * Checks, whether atom is the nitrogen atom of an N-C-C(=O) substructure
     * consisting of hitherto unused atoms. If yes, then an array of the four
     * fragment atoms is returned and these atoms are flagged in atomUsed.
     * @param atom
     * @return null or array of backbone members
     */
    private int[] findResidue(int atom) {
        if (!mIsBackboneAtom[atom] && mMol.getAtomicNo(atom) == 7) {
            for (int i=0; i<mMol.getConnAtoms(atom); i++) {
                if (mMol.getConnBondOrder(atom, i) == 1) {
                    int atomCA = mMol.getConnAtom(atom, i);
                    if (!mIsBackboneAtom[atomCA] && mMol.getAtomicNo(atomCA) == 6) {
                        for (int j=0; j<mMol.getConnAtoms(atomCA); j++) {
                            if (mMol.getConnBondOrder(atomCA, j) == 1) {
                                int atomC = mMol.getConnAtom(atomCA, j);
                                if (!mIsBackboneAtom[atomC] && mMol.getAtomicNo(atomC) == 6) {
                                    for (int k=0; k<mMol.getConnAtoms(atomC); k++) {
                                        if (mMol.getConnBondOrder(atomC, k) == 2) {
                                            int atomO = mMol.getConnAtom(atomC, k);
                                            if (!mIsBackboneAtom[atomO] && mMol.getAtomicNo(atomO) == 8) {
                                                int[] residueAtom = new int[4];
                                                residueAtom[ATOM_TYPE_N] = atom;
                                                residueAtom[ATOM_TYPE_CA] = atomCA;
                                                residueAtom[ATOM_TYPE_C] = atomC;
                                                residueAtom[ATOM_TYPE_O] = atomO;
                                                for (int ra : residueAtom)
                                                    mIsBackboneAtom[ra] = true;
                                                return residueAtom;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private int[] getNextResidue(int atom) {
        for (int i=0; i<mMol.getConnAtoms(atom); i++) {
            if (mMol.getConnBondOrder(atom, i) == 1) {
                int[] residueAtom = findResidue(mMol.getConnAtom(atom, i));
                if (residueAtom != null)
                    return residueAtom;
            }
        }
        return null;
    }

    private int[] getPreviousResidue(int atom) {
        for (int i=0; i<mMol.getConnAtoms(atom); i++) {
            if (mMol.getConnBondOrder(atom, i) == 1) {
                int atomC = mMol.getConnAtom(atom, i);
                if (!mIsBackboneAtom[atomC] && mMol.getAtomicNo(atomC) == 6) {
                    int atomO = -1;
                    for (int j=0; j<mMol.getConnAtoms(atomC); j++) {
                        if (mMol.getConnBondOrder(atomC, j) == 2) {
                            int connAtom = mMol.getConnAtom(atomC, j);
                            if (mMol.getAtomicNo(connAtom) == 8
                             && !mIsBackboneAtom[connAtom]) {
                                atomO = connAtom;
                                break;
                            }
                        }
                    }
                    if (atomO != -1) {
                        for (int j=0; j<mMol.getConnAtoms(atomC); j++) {
                            if (mMol.getConnBondOrder(atomC, j) == 1) {
                                int atomCA = mMol.getConnAtom(atomC, j);
                                if (!mIsBackboneAtom[atomCA] && mMol.getAtomicNo(atomCA) == 6) {
                                    for (int k=0; k<mMol.getConnAtoms(atomCA); k++) {
                                        if (mMol.getConnBondOrder(atomCA, k) == 1) {
                                            int atomN = mMol.getConnAtom(atomCA, k);
                                            if (!mIsBackboneAtom[atomN] && mMol.getAtomicNo(atomN) == 7) {
                                                int[] residueAtom = new int[4];
                                                residueAtom[ATOM_TYPE_N] = atomN;
                                                residueAtom[ATOM_TYPE_CA] = atomCA;
                                                residueAtom[ATOM_TYPE_C] = atomC;
                                                residueAtom[ATOM_TYPE_O] = atomO;
                                                for (int ra : residueAtom)
                                                    mIsBackboneAtom[ra] = true;
                                                return residueAtom;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public int getAtomType(String type) {
        for (int i=0; i<PROTEIN_ATOM_TYPE.length; i++)
            if (PROTEIN_ATOM_TYPE[i].equals(type))
                return i;
        return -1;
    }

    public void removeRibbon() {
        for (int f=0; f<mRibbonMesh.length; f++) {
            if (mRibbonMesh[f] != null) {
                mMol3D.getChildren().remove(mRibbonMesh[f]);
                mRibbonMesh[f] = null;
            }
        }
    }

    public int getAtomInResidue(int fragment, int residue, int atomType) {
        return mResidueAtom[fragment][residue][atomType];
    }

    public Coordinates getAtomCoords(int atom) {
        return mMol.getAtomCoordinates(atom);
    }

    public int getFragmentCount() {
        return mResidueAtom.length;
    }

    public int getResidues(int f) {
        return mResidueAtom[f].length;
    }

    public void assignSSTypeToResidues() {
//System.out.println("res\tpsi\tphi\tss");
        mSSType = new int[mResidueAtom.length][];
        for (int f=0; f<getFragmentCount(); f++) {
            mSSType[f] = new int[mResidueAtom[f].length];
            for (int r=1; r<mResidueAtom[f].length-1; r++) {
                int atomC_0 = mResidueAtom[f][r-1][ATOM_TYPE_C];
                int atomN_1 = mResidueAtom[f][r][ATOM_TYPE_N];
                int atomCA_1 = mResidueAtom[f][r][ATOM_TYPE_CA];
                int atomC_1 = mResidueAtom[f][r][ATOM_TYPE_C];
                int atomN_2 = mResidueAtom[f][r+1][ATOM_TYPE_N];
                int[] atom = new int[4];
                atom[0] = atomC_0;
                atom[1] = atomN_1;
                atom[2] = atomCA_1;
                atom[3] = atomC_1;
                double phi = -180 / Math.PI * mMol.calculateTorsion(atom);
                atom[0] = atomN_1;
                atom[1] = atomCA_1;
                atom[2] = atomC_1;
                atom[3] = atomN_2;
                double psi = -180 / Math.PI * mMol.calculateTorsion(atom);
                // Cutoff value derived from statistics plot at:
                // https://proteopedia.org/wiki/index.php/Tutorial:Ramachandran_principle_and_phi_psi_angles
                if (phi >= 0) {
                    if (phi >= 35 && phi <= 80 && psi >= 0 && psi <= 80)
                        mSSType[f][r] = SS_HELIX;    // left handed alpha-helix
                }
                else {
                    if (psi <= -165 || psi >= 90) {
                        mSSType[f][r] = SS_BETA;
                    }
                    else if (psi >= 45) {
                        if (phi >= -90)
                            mSSType[f][r] = SS_BETA;
                    }
                    else if (psi >= -70) {
                        if (phi >= -125)
                            mSSType[f][r] = SS_HELIX;
                    }
                }
//System.out.println(r+"\t"+(int)psi+"\t"+(int)phi+"\t"+(mSimpleSecondaryStructure[r]==SS_HELIX?"helix":mSimpleSecondaryStructure[r]==SS_BETA?"beta":"other"));
            }

            // We don't allow a helix to directly follow after a beta sheet (to get a nice arrow)
            for (int r = 1; r< mSSType[f].length; r++)
                if (mSSType[f][r-1] == SS_BETA
                 && mSSType[f][r] == SS_HELIX)
                    mSSType[f][r] = SS_OTHER;

            // We consider separated single or double beta or helix not to be real beta or helix
            // and change them to other!
            int sameCount = 1;
            for (int r = 0; r< mSSType[f].length; r++) {
                if (mSSType[f][r] == SS_OTHER)
                    continue;
                while (r + 1 < mSSType[f].length && mSSType[f][r + 1] == mSSType[f][r]) {
                    sameCount++;
                    r++;
                }
                if (sameCount <= 2) {
                    for (int i=0; i<sameCount; i++)
                        mSSType[f][r-i] = SS_OTHER;
                }
                sameCount = 1;
            }
        }
    }

    public boolean isSSBeta(int f, int r) {
        // We distinguish between HELIX, BETA, and REST, while the relevant paper distinguishes 7 more fine-grained cases:
        // Kabsch W, Sander C. Biopolymers. 1983;22:2577. doi: 10.1002/bip.360221211.
        return mSSType[f][r] == SS_BETA;
    }

    public boolean isSSHelix(int f, int r) {
        return mSSType[f][r] == SS_HELIX;
    }

    public boolean isCyclicFragment(int f) {
        int atomN1 = mResidueAtom[f][0][ATOM_TYPE_N];
        int atomC2 = mResidueAtom[f][mResidueAtom[f].length-1][ATOM_TYPE_C];
        return atomN1 != -1 && atomC2 != -1
            && mMol.getAtomCoordinates(atomN1).distance(mMol.getAtomCoordinates(atomC2)) < 2;
    }

    public boolean[] getBackboneMask() {
        return mIsBackboneAtom;
    }

    public void draw(int mode) {
        if (mode == MODE_RIBBON) {
            if (mColor == null) {
                if (COLOR_MODE == COLOR_MODE_BY_SS_SECTION)
                    assignSSTypeToResidues();
                assignColorToResidues();
            }
            new RibbonCalculator(this).drawRibbons();
        }

        if (mode == MODE_CARTOON) {
            assignSSTypeToResidues();
            if (mColor == null)
                assignColorToResidues();
            new RibbonCalculator(this).drawCartoons();
        }
    }

    public void createTriangeMesh(Coordinates[] points, Coordinates[] normals, int fragment) {
        TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
        for (Coordinates c : points)
            mesh.getPoints().addAll((float)c.x, (float)c.y, (float)c.z);
        for (Coordinates c : normals)
            mesh.getNormals().addAll((float)c.x, (float)c.y, (float)c.z);

        if (mColor == null)
            updateTexCoordsForMultipleColors(mesh);
        else
            updateTexCoordsForSingleColor(mesh);

        int[] col = new int[2];

        // The ribbon is built by interconnected cycles of n points (n = POINTS_PER_SECTION)
        // |  /  |  /  |  /  |     |
        // p2 -- p4 -- p --- p ... pn -> (p2)   cycle of n points
        // |  /  |  /  |  /  |     |
        // p1 -- p3 -- p --- p ... pn -> (p1)   cycle of n points
        // |  /  |  /  |  /  |     |
        for (int i=POINTS_PER_SECTION; i<points.length; i+=POINTS_PER_SECTION) {
            if (mColor == null)
                setColorIndexes(col, fragment, i/POINTS_PER_SECTION-1);
            for (int j=0; j<POINTS_PER_SECTION; j++) {
                int d = (j == 0) ? POINTS_PER_SECTION -1 : -1;
                int p1 = i+j-POINTS_PER_SECTION+d;
                int p2 = i+j+d;
                int p3 = i+j-POINTS_PER_SECTION;
                int p4 = i+j;
                mesh.getFaces().addAll(p3, p3, col[0], p4, p4, col[1], p1, p1, col[0]);  // anti-clockwise and alternating between sections
                mesh.getFaces().addAll(p2, p2, col[1], p1, p1, col[0], p4, p4, col[1]);  // to easy color assignment
            }
        }

        mRibbonMesh[fragment] = new MeshView(mesh);
        mRibbonMesh[fragment].setCullFace(CullFace.NONE);
        //mRibbonMesh[fragment].setDrawMode(DrawMode.LINE);
        mRibbonMesh[fragment].setMaterial(createMaterial(mColor));

        mMol3D.getChildren().add(mRibbonMesh[fragment]);
    }

    private void setColorIndexes(int[] colorIndex, int fragment, int section) {
        int residue = section / SECTIONS_PER_RESIDUE;
        section = section % SECTIONS_PER_RESIDUE;
        if (residue >= mResidueColor[fragment].length-1) {
            colorIndex[0] = colorIndex[1] = mResidueColor[fragment][mResidueColor[fragment].length-1];
        }
        else {
            int colorDif = mResidueColor[fragment][residue+1] - mResidueColor[fragment][residue];
            colorIndex[0] = mResidueColor[fragment][residue] + colorDif * section / SECTIONS_PER_RESIDUE;
            colorIndex[1] = mResidueColor[fragment][residue] + colorDif * (section+1) / SECTIONS_PER_RESIDUE;
        }
    }

    public void updateColor() {
        Color molColor = mMol3D.getColor();
        if (molColor == null && mResidueColor == null)
            assignColorToResidues();

        int[] col = new int[2];

        for (int fragment=0; fragment<mResidueAtom.length; fragment++) {
            if (mRibbonMesh[fragment] != null) {
                if (molColor == null ^ mColor == null) {
                    TriangleMesh mesh = (TriangleMesh)mRibbonMesh[fragment].getMesh();
                    if (molColor == null)
                        updateTexCoordsForMultipleColors(mesh);

                    // update texCoord indexes in faces
                    int points = mesh.getPoints().size() / 3;
                    int index = 2;
                    for (int i=POINTS_PER_SECTION; i<points; i+=POINTS_PER_SECTION) {
                        if (molColor == null)
                            setColorIndexes(col, fragment, i/POINTS_PER_SECTION-1);
                        for (int j=0; j<POINTS_PER_SECTION; j++) {
                            for (int k=0; k<6; k++) { // two triangle, 6 corners altogether
                                mesh.getFaces().set(index, (k & 1) == 0 ? col[0] : col[1]);
                                index += 3;
                            }
                        }
                    }

                    if (molColor != null)
                        updateTexCoordsForSingleColor(mesh);
                }

                if ((molColor == null) ^ (mColor == null)
                 || (molColor != null && !molColor.equals(mColor)))
                    mRibbonMesh[fragment].setMaterial(createMaterial(molColor));
            }
        }

        mColor = molColor;
    }

    private void updateTexCoordsForSingleColor(TriangleMesh mesh) {
        ObservableFloatArray texCoords = mesh.getTexCoords();
        texCoords.clear();
        texCoords.addAll(0, 0);
    }

    private void updateTexCoordsForMultipleColors(TriangleMesh mesh) {
        ObservableFloatArray texCoords = mesh.getTexCoords();
        texCoords.clear();
        float dx = 1f / (float)(HUE_SPAN+1);
        for (float x=dx/2; x<=1f; x+=dx)
            texCoords.addAll(x, 0);
    }

    private PhongMaterial createMaterial(Color color) {
        PhongMaterial material = new PhongMaterial();
        if (color != null) {
            material.setDiffuseColor(color);
            material.setSpecularColor(color.darker());
        }
        else {
            material.setSpecularColor(Color.GRAY);
            material.setDiffuseMap(createTextureImage());
        }
        return material;
    }

    private void assignColorToResidues() {
        if (COLOR_MODE == COLOR_MODE_BY_SS_SECTION
         && mSSType == null)
            assignSSTypeToResidues();

        mResidueColor = new int[mResidueAtom.length][];
        for (int f=0; f<mResidueAtom.length; f++)
            mResidueColor[f] = new int[mResidueAtom[f].length];

        if (COLOR_MODE == COLOR_MODE_BY_SS_SECTION) {
            int count = 1;
            for (int f=0; f<mResidueAtom.length; f++)
                for (int r=1; r<mSSType[f].length; r++)
                    if (mSSType[f][r] != mSSType[f][r - 1])
                        count++;

            int index = 0;
            for (int f=0; f<mResidueAtom.length; f++) {
                for (int r=1; r<mSSType[f].length; r++) {
                    if (mSSType[f][r] != mSSType[f][r-1])
                        index++;
                    mResidueColor[f][r] = (index == 0) ? 0 : index * HUE_SPAN / (count - 1);
                }
            }
        }
        else if (COLOR_MODE == COLOR_MODE_BY_RESIDUE) {
            int residueCount = 0;
            for (int f=0; f<mResidueAtom.length; f++)
                residueCount += mResidueAtom[f].length;

            int index = 0;
            for (int f=0; f<mResidueAtom.length; f++) {
                for (int r=0; r<mResidueAtom[f].length; r++) {
                    mResidueColor[f][r] = (index == 0) ? 0 : index * HUE_SPAN / (residueCount - 1);
                    index++;
                }
            }
        }
        else if (COLOR_MODE == COLOR_MODE_BY_FRAGMENT) {
            for (int f=0; f<mResidueAtom.length; f++) {
                for (int r=0; r<mResidueAtom[f].length; r++) {
                    mResidueColor[f][r] = (f == 0) ? 0 : f * HUE_SPAN / (mResidueAtom.length - 1);
                }
            }
        }
    }

    private Image createTextureImage() {
        Image image = new WritableImage(HUE_SPAN+1, 1);
        PixelWriter pw = ((WritableImage)image).getPixelWriter();

        for (int i=0; i<=HUE_SPAN; i++) {
            int hue = (HUE_START + i > 360) ? HUE_START + i - 360 : HUE_START + i;
            pw.setColor(i, 0, Color.hsb(hue, 1.0,1.0));
        }

        return image;
    }
}

package org.openmolecules.fx.viewer3d.nodes;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.Molecule3D;
import javafx.collections.ObservableFloatArray;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import org.openmolecules.fx.viewer3d.V3DMolecule;

import java.util.ArrayList;

public class Ribbon {
    public static final int MODE_NONE = 0;
    public static final int MODE_RIBBON = 1;
    public static final int MODE_CARTOON = 2;
    // we might define atom types for sugar and nucleotide chains as well
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
    private static final int HUE_START = 60;
    private static final int HUE_SPAN = 240;
    private static final int COLOR_MODE_STATIC = 1;
    private static final int COLOR_MODE_BY_SS = 2;
    private static final int COLOR_MODE_BY_SS_TYPE = 2;
    private static final int COLOR_MODE_BY_SS_SECTION = 3;
    private static final int COLOR_MODE_BY_FRAGMENT = 4;
    private static final int COLOR_MODE_BY_RESIDUE = 5;
    private static final int COLOR_MODE = COLOR_MODE_BY_SS_SECTION;

    private final int[][] mResidueAtom;
    private final int[] mChainStartIndex;
    private int[] mResidueColor;
    private int[] mSimpleSecondaryStructure;
    private final V3DMolecule mMol3D;
    private final Molecule3D mMol;
    private MeshView mRibbonMesh;

    public Ribbon(Molecule3D mol, V3DMolecule mol3D) {
        mMol3D = mol3D;
        mMol = mol;

        mMol.ensureHelperArrays(Molecule.cHelperNeighbours);

        ArrayList<int[]> atomsList = new ArrayList<>();
        int[] atoms = null;
        int previousResidue = -1;
        int atomsFound = 0;
        for (int atom=0; atom<mMol.getAtoms(); atom++) {
            int residue = mMol.getResSequence(atom) - 1;    // 0-based
            if (residue != previousResidue) {
                previousResidue = residue;
                atoms = new int[PROTEIN_ATOM_TYPE.length];
                atomsFound = 0;
            }
            String atomName = mMol.getAtomName(atom);
            for (int i=0; i<PROTEIN_ATOM_TYPE.length; i++) {
                if (PROTEIN_ATOM_TYPE[i].equals(atomName)) {
                    atoms[i] = atom;
                    atomsFound++;
                    if (atomsFound == PROTEIN_ATOM_TYPE.length)
                        atomsList.add(atoms);
                    break;
                }
            }
        }

        mResidueAtom = atomsList.toArray(new int[0][]);

        int chainIDAtomType = 1;  // we use C-alpha for chain-ID lookup
        ArrayList<Integer> chainIDStartList = new ArrayList<>();
        if (mResidueAtom.length != 0) {
            chainIDStartList.add(0);
            String lastChainID = mMol.getAtomChainId(mResidueAtom[0][chainIDAtomType]);
            for (int i = 1; i < mResidueAtom.length; i++) {
                String chainID = mMol.getAtomChainId(mResidueAtom[i][chainIDAtomType]);
                if (!lastChainID.equals(chainID)) {
                    lastChainID = chainID;
                    chainIDStartList.add(i);
                }
            }
        }

        mChainStartIndex = new int[chainIDStartList.size()];
        for (int i=0; i<chainIDStartList.size(); i++)
            mChainStartIndex[i] = chainIDStartList.get(i);
    }

    public int getAtomType(String type) {
        for (int i=0; i<PROTEIN_ATOM_TYPE.length; i++)
            if (PROTEIN_ATOM_TYPE[i].equals(type))
                return i;
        return -1;
    }

    public void removeRibbon() {
        if (mRibbonMesh != null) {
            mMol3D.getChildren().remove(mRibbonMesh);
            mRibbonMesh = null;
        }
    }

    public int getResidues() {
        return mResidueAtom.length;
    }

    public int getAtomInResidue(int residue, int atomType) {
        return mResidueAtom[residue][atomType];
    }

    public Coordinates getAtomCoords(int atom) {
        return mMol.getAtomCoordinates(atom);
    }

    public int getFragmentCount() {
        return mChainStartIndex.length;
    }

    public int getFirstResidueInFragment(int f) {
        return mChainStartIndex[f];
    }

    public int getLastResidueInFragment(int f) {
        return f+1<mChainStartIndex.length ? mChainStartIndex[f+1]-1 : mResidueAtom.length-1;

    }

    public int getResiduesInFragment(int f) {
        return (f+1 < mChainStartIndex.length ? mChainStartIndex[f] : mResidueAtom.length) - mChainStartIndex[f];
    }

    public void assignSecondaryStructure() {
//System.out.println("res\tpsi\tphi\tss");
        mSimpleSecondaryStructure = new int[mResidueAtom.length];
        for (int f=0; f<getFragmentCount(); f++) {
            int lastResidue = getLastResidueInFragment(f);
            for (int r=getFirstResidueInFragment(f)+1; r<lastResidue; r++) {
                int atomC_0 = getAtomInResidue(r-1, ATOM_TYPE_C);
                int atomN_1 = getAtomInResidue(r, ATOM_TYPE_N);
                int atomCA_1 = getAtomInResidue(r, ATOM_TYPE_CA);
                int atomC_1 = getAtomInResidue(r, ATOM_TYPE_C);
                int atomN_2 = getAtomInResidue(r+1, ATOM_TYPE_N);
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
                        mSimpleSecondaryStructure[r] = SS_HELIX;    // left handed alpha-helix
                }
                else {
                    if (psi <= -165 || psi >= 90) {
                        mSimpleSecondaryStructure[r] = SS_BETA;
                    }
                    else if (psi >= 45) {
                        if (phi >= -90)
                            mSimpleSecondaryStructure[r] = SS_BETA;
                    }
                    else if (psi >= -70) {
                        if (phi >= -125)
                            mSimpleSecondaryStructure[r] = SS_HELIX;
                    }
                }
//System.out.println(r+"\t"+(int)psi+"\t"+(int)phi+"\t"+(mSimpleSecondaryStructure[r]==SS_HELIX?"helix":mSimpleSecondaryStructure[r]==SS_BETA?"beta":"other"));
            }
        }

        // We don't allow a helix to directly follow after a beta sheet (to get a nice arrow)
        for (int r=1; r<mSimpleSecondaryStructure.length; r++)
            if (mSimpleSecondaryStructure[r-1] == SS_BETA
             && mSimpleSecondaryStructure[r] == SS_HELIX)
                mSimpleSecondaryStructure[r] = SS_OTHER;

        // We consider separated single or double beta or helix not to be real beta or helix
        // and change them to other!
        int sameCount = 1;
        for (int r=0; r<mSimpleSecondaryStructure.length; r++) {
            if (mSimpleSecondaryStructure[r] == SS_OTHER)
                continue;
            while (r + 1 < mSimpleSecondaryStructure.length && mSimpleSecondaryStructure[r + 1] == mSimpleSecondaryStructure[r]) {
                sameCount++;
                r++;
            }
            if (sameCount <= 2) {
                for (int i=0; i<sameCount; i++)
                    mSimpleSecondaryStructure[r-i] = SS_OTHER;
            }
            sameCount = 1;
        }
    }

    public boolean isSSBeta(int r) {
        // Kabsch W, Sander C. Biopolymers. 1983;22:2577. doi: 10.1002/bip.360221211.
        // original cases are:
        // SS_HELIX_ALPHA = 0;
        // SS_HELIX_3_10 = 1;
        // SS_HELIX_PI = 2;
        // SS_BETA = 3;
        // SS_BRIDGE = 4;
        // SS_TURN = 5;
        // SS_COIL = 6
        // We need to distinguish between HELIX, BETA, and REST only.
        return mSimpleSecondaryStructure[r] == SS_BETA;
    }

    public boolean isSSHelix(int r) {
        return mSimpleSecondaryStructure[r] == SS_HELIX;
    }

    public boolean isCyclicFragment(int f) {
        int atomN1 = getAtomInResidue(getFirstResidueInFragment(f), ATOM_TYPE_N);
        int atomC2 = getAtomInResidue(getLastResidueInFragment(f), ATOM_TYPE_C);
        return atomN1 != -1 && atomC2 != -1
            && mMol.getAtomCoordinates(atomN1).distance(mMol.getAtomCoordinates(atomC2)) < 2;
    }

    /**
     * Changes current color to the given rgb
     * @param idx
     * @param rgb
     */
    public void setColor(int idx, int rgb) {
        System.out.println("set atom color; idx:"+idx);
    }

    public void draw(int mode) {
        if (mode == MODE_RIBBON) {
            if (COLOR_MODE == COLOR_MODE_BY_SS_TYPE || COLOR_MODE == COLOR_MODE_BY_SS_SECTION)
                assignSecondaryStructure();
            if (COLOR_MODE != COLOR_MODE_STATIC)
                assignColorToResidues();
            new RibbonCalculator(this).drawRibbons();
        }

        if (mode == MODE_CARTOON) {
            assignSecondaryStructure();
            if (COLOR_MODE != COLOR_MODE_STATIC)
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

        PhongMaterial material = new PhongMaterial();

        if (COLOR_MODE == COLOR_MODE_STATIC) {
            mesh.getTexCoords().addAll(0, 0);
            material.setDiffuseColor(Color.ORANGE);
            material.setSpecularColor(Color.ORANGE.darker());
        }
        else {
            material.setSpecularColor(Color.GRAY);
            material.setDiffuseMap(createTexture(mesh));
        }

        // The ribbon is built by interconnected cycles of n points
        // |  /  |  /  |  /  |     |
        // p2 -- p4 -- p --- p ... pn -> (p1)   cycle of n points
        // |  /  |  /  |  /  |     |
        // p1 -- p3 -- p --- p ... pn -> (p1)   cycle of n points
        // |  /  |  /  |  /  |     |
        int n = POINTS_PER_SECTION;
        for (int i=n; i<points.length; i+=n) {
            int maxRes = (fragment+1 < mChainStartIndex.length) ? mChainStartIndex[fragment+1]-1 : mResidueAtom.length-1;
            int res = Math.min(maxRes, mChainStartIndex[fragment] + i / (SECTIONS_PER_RESIDUE * POINTS_PER_SECTION));
            for (int j=0; j<n; j++) {
                int d = (j == 0) ? POINTS_PER_SECTION -1 : -1;
                int p1 = i+j-n+d;
                int p2 = i+j+d;
                int p3 = i+j-n;
                int p4 = i+j;
                int col = COLOR_MODE == COLOR_MODE_STATIC ? 0 : mResidueColor[res];
                mesh.getFaces().addAll(p1, p1, col, p4, p4, col, p2, p2, col);
                mesh.getFaces().addAll(p1, p1, col, p3, p3, col, p4, p4, col);
            }
        }

        mRibbonMesh = new MeshView(mesh);
        mRibbonMesh.setCullFace(CullFace.NONE);
        //ribbonMesh.setDrawMode(DrawMode.LINE);

        mRibbonMesh.setMaterial(material);

        mMol3D.getChildren().add(mRibbonMesh);
    }

    private void assignColorToResidues() {
        mResidueColor = new int[mResidueAtom.length];

        if (COLOR_MODE == COLOR_MODE_BY_SS_SECTION) {
            int count = 1;
            for (int r=1; r<mSimpleSecondaryStructure.length; r++)
                if (mSimpleSecondaryStructure[r] != mSimpleSecondaryStructure[r-1])
                    count++;
            int index = 0;
            for (int r=1; r<mSimpleSecondaryStructure.length; r++) {
                if (mSimpleSecondaryStructure[r] != mSimpleSecondaryStructure[r-1])
                    index++;
                mResidueColor[r] = index * HUE_SPAN / (count - 1);
            }
        }
        else {
            for (int r=0; r<mResidueAtom.length; r++) {
                if (COLOR_MODE == COLOR_MODE_BY_SS_TYPE) {
                    mResidueColor[r] = (isSSHelix(r) ? 0 : isSSBeta(r) ? 1 : 2) * HUE_SPAN / 2;
                }
                else if (COLOR_MODE == COLOR_MODE_BY_RESIDUE) {
                    mResidueColor[r] = r * HUE_SPAN / (getResidues() - 1);
                }
                else if (COLOR_MODE == COLOR_MODE_BY_FRAGMENT) {
                    for (int f = mChainStartIndex.length - 1; f >= 0; f--) {
                        if (r >= mChainStartIndex[f]) {
                            mResidueColor[r] = (f == 0) ? 0 : f * HUE_SPAN / (mChainStartIndex.length - 1);
                            break;
                        }
                    }
                }
            }
        }
    }

    private Image createTexture(TriangleMesh mesh) {
        Image image = new WritableImage(HUE_SPAN+1, 1);
        PixelWriter pw = ((WritableImage)image).getPixelWriter();

        for (int i=0; i<=HUE_SPAN; i++) {
            int hue = (HUE_START + i > 360) ? HUE_START + i - 360 : HUE_START + i;
            pw.setColor(i, 0, Color.hsb(hue, 1.0,1.0));
        }

        ObservableFloatArray texCoords = mesh.getTexCoords();
        texCoords.clear();
        float dx = 1f / (float)image.getWidth();
        for (float x=dx/2; x<=1f; x+=dx)
            texCoords.addAll(x, 0);

        return image;
    }
}

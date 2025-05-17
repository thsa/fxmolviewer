package org.openmolecules.render;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.RingCollection;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.chem.conf.VDWRadii;
import javafx.scene.paint.Color;

import java.util.ArrayList;

/**
 * A MoleculeArchitect is a class that constructs a 3D molecule model
 * from cylinders and spheres. It talks to a MoleculeBuilder class that
 * implements createCylinder() and createSphere() to build the molecule
 * from primitives for a particular environment, e.g. for 3D-JavaFX or
 * the SunFlow ray-tracer.
 */
public class MoleculeArchitect {
	public static final int CONSTRUCTION_MODE_BALL_AND_STICKS = 0;
	public static final int CONSTRUCTION_MODE_STICKS = 1;
	public static final int CONSTRUCTION_MODE_THINSTICKS = 2;
	public static final int CONSTRUCTION_MODE_WIRES = 3;
	public static final int CONSTRUCTION_MODE_BALLS = 4;
	public static final int CONSTRUCTION_MODE_NONE = 5;
	public final static int CONSTRUCTION_MODE_DEFAULT = CONSTRUCTION_MODE_BALL_AND_STICKS;
	public final static String[] CONSTRUCTION_MODE_CODE = { "ballAndSticks", "sticks", "thinsticks", "wires", "balls", "none" };
	public final static String[] CONSTRUCTION_MODE_TEXT = { "Ball & Sticks", "Sticks", "Thin Sticks", "Wires", "Balls", "None" };


	public static final int HYDROGEN_MODE_ALL = 0;
	public static final int HYDROGEN_MODE_POLAR = 1;
	public static final int HYDROGEN_MODE_NONE = 2;
	public static final int HYDROGEN_MODE_DEFAULT = HYDROGEN_MODE_ALL;
	public final static String[] HYDROGEN_MODE_CODE = { "all", "polar", "none" };

	public enum ColorMode {ATOMIC_NO, WIRES;}
	public static final ColorMode COLOR_MODE_DEFAULT = ColorMode.ATOMIC_NO;

	private static final double BALLS_ATOM_FACTOR = 0.95;
	private static final double BALL_AND_STICK_ATOM_FACTOR = 0.25;
	private static final double STICK_ATOM_FACTOR = 0.17;
	private static final double THINSTICK_ATOM_FACTOR = 0.15;
	private static final double WIRE_ATOM_FACTOR = 0.125;

	private static final double BALLS_CONE_FACTOR = 0.95;
	private static final double BALL_AND_STICK_CONE_FACTOR = 0.25;
	private static final double STICK_CONE_FACTOR = 0.22;
	private static final double THINSTICK_CONE_FACTOR = 0.20;
	private static final double WIRE_CONE_FACTOR = 0.18;

	private static final double BALL_AND_STICK_SBOND_RADIUS = 0.18;
	private static final double BALL_AND_STICK_DBOND_RADIUS = 0.10;
	private static final double BALL_AND_STICK_TBOND_RADIUS = 0.08;
	private static final double BALL_AND_STICK_DBOND_SHIFT = 0.15;
	private static final double BALL_AND_STICK_TBOND_SHIFT = 0.26;

	public static final double STICK_SBOND_RADIUS = 0.16;
	private static final double STICK_DBOND_RADIUS = 0.08;
	private static final double STICK_PI_BOND_SHIFT = 0.40;

	public static final double THINSTICK_SBOND_RADIUS = 0.08;
	private static final double THINSTICK_DBOND_RADIUS = 0.06;
	private static final double THINSTICK_PI_BOND_SHIFT = 0.28;

	public static final double WIRE_SBOND_RADIUS = 0.04;
	private static final double WIRE_DBOND_RADIUS = 0.04;
	private static final double WIRE_PI_BOND_SHIFT = 0.18;

	// Radii of metal ligand bond dots
	private static final double BALL_AND_STICK_DOT_RADIUS = 0.12;
	private static final double STICK_DOT_RADIUS = 0.10;
	private static final double THINSTICK_DOT_RADIUS = 0.08;
	private static final double WIRE_DOT_RADIUS = 0.05;

	public static final int BALL_AND_STICK_STICK_COLOR = 0xFFE0E0E0;

	private static final int CONNECTION_POINT_COLOR = 0xFFCCCA90;   // changed from 0xFFFF1493 to make it less look like oxygen

	private static final int COLOR_NONE = 0x00000000;   // don't draw objects with this color

	private final static int[] ATOM_ARGB = { COLOR_NONE,
						0xFFFFFFFF, 0xFFD9FFFF, 0xFFCC80FF, 0xFFC2FF00, 0xFFFFB5B5, //  ?, H,He,Li, Be,B
			0xFF909090, 0xFF3050F8, 0xFFFF0D0D, 0xFF90E050, 0xFFB3E3F5, 0xFFAB5CF2, //  C, N, O, F,Ne,Na
			0xFF8AFF00, 0xFFBFA6A6, 0xFFF0C8A0, 0xFFFF8000, 0xFFFFFF30, 0xFF1FF01F, // Mg,Al,Si, P, S,Cl
			0xFF80D1E3, 0xFF8F40D4, 0xFF3DFF00, 0xFFE6E6E6, 0xFFBFC2C7, 0xFFA6A6AB, // Ar, K,Ca,Sc,Ti, V
			0xFF8A99C7, 0xFF9C7AC7, 0xFFE06633, 0xFFF090A0, 0xFF50D050, 0xFFC88033, // Cr,Mn,Fe,Co,Ni,Cu
			0xFF7D80B0, 0xFFC28F8F, 0xFF668F8F, 0xFFBD80E3, 0xFFFFA100, 0xFFA62929, // Zn,Ga,Ge,As,Se,Br
			0xFF5CB8D1, 0xFF702EB0, 0xFF00FF00, 0xFF94FFFF, 0xFF94E0E0, 0xFF73C2C9, // Kr,Rb,Sr, Y,Zr,Nb
			0xFF54B5B5, 0xFF3B9E9E, 0xFF248F8F, 0xFF0A7D8C, 0xFF006985, 0xFFC0C0C0, // Mo,Tc,Ru,Rh,Pd,Ag
			0xFFFFD98F, 0xFFA67573, 0xFF668080, 0xFF9E63B5, 0xFFD47A00, 0xFF940094, // Cd,In,Sn,Sb,Te, I
			0xFF429EB0, 0xFF57178F, 0xFF00C900, 0xFF70D4FF, 0xFFFFFFC7, 0xFFD9FFC7, // Xe,Cs,Ba,La,Ce,Pr
			0xFFC7FFC7, 0xFFA3FFC7, 0xFF8FFFC7, 0xFF61FFC7, 0xFF45FFC7, 0xFF30FFC7, // Nd,Pm,Sm,Eu,Gd,Tb
			0xFF1FFFC7, 0xFF00FF9C, 0xFF00E675, 0xFF00D452, 0xFF00BF38, 0xFF00AB24, // Dy,Ho,Er,Tm,Yb,Lu
			0xFF4DC2FF, 0xFF4DA6FF, 0xFF2194D6, 0xFF267DAB, 0xFF266696, 0xFF175487, // Hf,Ta, W,Re,Os,Ir
			0xFFD0D0E0, 0xFFFFD123, 0xFFB8B8D0, 0xFFA6544D, 0xFF575961, 0xFF9E4FB5, // Pt,Au,Hg,Tl,Pb,Bi
			0xFFAB5C00, 0xFF754F45, 0xFF428296, 0xFF420066, 0xFF007D00, 0xFF70ABFA, // Po,At,Rn,Fr,Ra,Ac
			0xFF00BAFF, 0xFF00A1FF, 0xFF008FFF, 0xFF0080FF, 0xFF006BFF, 0xFF545CF2, // Th,Pa, U,Np,Pu,Am
			0xFF785CE3, 0xFF8A4FE3, 0xFFA136D4, 0xFFB31FD4, 0xFFB31FBA, 0xFFB30DA6, // Cm,Bk,Cf,Es,Fm,Md
			0xFFBD0D87, 0xFFC70066, 0xFFCC0059, 0xFFD1004F, 0xFFD90045, 0xFFE00038, // No,Lr,Rf,Db,Sg,Bh
			0xFFE6002E, 0xFFEB0026,													// Hs,Mt
	};
	public static final int ATOM_ARGB_LENGTH = ATOM_ARGB.length;

	public static int getAtomicNoARGB(int atomicNo) {
		return ATOM_ARGB[atomicNo < ATOM_ARGB.length ? atomicNo : 6];   // higher atomicNos are assumed to be some kind of carbon
	}

	public static Color getAtomColor(int atomicNo, double opacity) {
		int argb = getAtomicNoARGB(atomicNo);
		return Color.rgb((argb & 0xFF0000) >> 16, (argb & 0x00FF00) >> 8, argb & 0x0000FF, opacity);
	}

	private StereoMolecule mMol;
	private Conformer mConformer;
	private final MoleculeBuilder mBuilder;
	private final Coordinates center,delta,point1,point2;    // reused Coordinate objects
	private ColorMode mColorMode;
	private int mConstructionMode;
	private int mHydrogenMode;
	private int mBondNodeID;
	private boolean mSplitAllBonds;

	public MoleculeArchitect(MoleculeBuilder builder) {
		mBuilder = builder;
		center = new Coordinates();
		delta = new Coordinates();
		point1 = new Coordinates();
		point2 = new Coordinates();
		mConstructionMode = CONSTRUCTION_MODE_DEFAULT;
		mHydrogenMode = HYDROGEN_MODE_ALL;
		mColorMode = COLOR_MODE_DEFAULT;
		}

	public ColorMode getColorMode() {
		return mColorMode;
	}

	public int getConstructionMode() {
		return mConstructionMode;
	}

	public int getHydrogenMode() {
		return mHydrogenMode;
	}

	public void setColorMode(ColorMode mode) {
		mColorMode = mode;
	}

	public void setConstructionMode(int mode) {
		mConstructionMode = mode;
	}

	/**
	 * If set to true, then bonds are always created as two parts.
	 * This may be useful for better showing selections, because then bond halfs
	 * touching selected atoms are always drawn in the selection color.
	 * Otherwise, bonds between atoms ow the same kind are one piece and drawn
	 * in selection color only of both atoms are selected.
	 * @param b
	 */
	public void setSplitAllBonds(boolean b) {
		mSplitAllBonds = b;
	}

	public void setHydrogenMode(int mode) {
		mHydrogenMode = mode;
	}

	public MoleculeBuilder getRenderer() {
		return mBuilder;
		}

	public void buildMolecule(Conformer conformer) {
		StereoMolecule mol = conformer.getMolecule();
		buildMolecule(conformer, mol, new RangeConstructionFilter(0, mol.getAllAtoms(), 0, mol.getAllBonds()));
		}

	public void buildMolecule(StereoMolecule mol) {
		buildMolecule(null, mol, new RangeConstructionFilter(0, mol.getAllAtoms(), 0, mol.getAllBonds()));
		}

	public void buildMolecule(Conformer conformer, ConstructionFilter cf) {
		buildMolecule(conformer, conformer.getMolecule(), cf);
		}

	public void buildMolecule(StereoMolecule mol, ConstructionFilter cf) {
		buildMolecule(null, mol, cf);
		}

	public void buildMolecule(StereoMolecule mol, ArrayList<Integer> atoms, ArrayList<Integer> bonds) {
		mMol = mol;
		buildMolecule(new ListConstructionFilter(atoms, bonds));
		}

	private void buildMolecule(Conformer conformer, StereoMolecule mol, ConstructionFilter cf) {
		mConformer = conformer;
		mMol = mol;
		mol.ensureHelperArrays(Molecule.cHelperRings);
		mBuilder.init();
		// adding non-hydrogen atoms and bonds first allows us to later add hydrogens without disrupting order of already created primitives
		buildMolecule(cf);
		//buildMolecule(Math.max(fromAtom, mol.getAtoms()), mol.getAllAtoms(), Math.max(fromBond, mol.getBonds()), mol.getAllBonds());
		mBuilder.done();
		}

	private void buildMolecule(ConstructionFilter cf) {
		if (mConstructionMode == CONSTRUCTION_MODE_NONE)
			return;

		if (mConstructionMode == CONSTRUCTION_MODE_STICKS
		 || mConstructionMode == CONSTRUCTION_MODE_THINSTICKS
		 || mConstructionMode == CONSTRUCTION_MODE_BALL_AND_STICKS
		 || mConstructionMode == CONSTRUCTION_MODE_WIRES)
			for (int bond=cf.getNextBond(); bond != -1; bond=cf.getNextBond())
				if (includeAtom(mMol.getBondAtom(0, bond))
				 && includeAtom(mMol.getBondAtom(1, bond)))
					buildBond(bond);

		for (int atom=cf.getNextAtom(); atom != -1; atom=cf.getNextAtom()) {
			if (includeAtom(atom)) {
				int atomicNo = mMol.getAtomicNo(atom);
				boolean isAttachmentPoint = (atomicNo == 0 || "*".equals(mMol.getAtomCustomLabel(atom)));
				if (isAttachmentPoint) {
					double radius = // mMol.isMarkedAtom(atom) ? VDWRadii.getVDWRadius(atomicNo)/4 :
							  (mConstructionMode == CONSTRUCTION_MODE_WIRES) ? WIRE_CONE_FACTOR * VDWRadii.getVDWRadius(atomicNo)
						    : (mConstructionMode == CONSTRUCTION_MODE_THINSTICKS) ? THINSTICK_CONE_FACTOR * VDWRadii.getVDWRadius(atomicNo)
						    : (mConstructionMode == CONSTRUCTION_MODE_STICKS) ? STICK_CONE_FACTOR * VDWRadii.getVDWRadius(atomicNo)
							: (mConstructionMode == CONSTRUCTION_MODE_BALLS) ? BALLS_CONE_FACTOR * VDWRadii.getVDWRadius(atomicNo)
							:							BALL_AND_STICK_CONE_FACTOR * VDWRadii.getVDWRadius(atomicNo);
					buildAttachmentPoint(atom, radius);
					}
				else if (mConstructionMode != CONSTRUCTION_MODE_WIRES || hasNoVisibleNeighbours(atom)) {
					double radius = // mMol.isMarkedAtom(atom) ? VDWRadii.getVDWRadius(atomicNo)/4 :
							  (mConstructionMode == CONSTRUCTION_MODE_BALL_AND_STICKS) ? BALL_AND_STICK_ATOM_FACTOR * VDWRadii.getVDWRadius(atomicNo)
							: (mConstructionMode == CONSTRUCTION_MODE_STICKS) ?
									(hasNoVisibleNeighbours(atom) ? STICK_ATOM_FACTOR * VDWRadii.getVDWRadius(atomicNo) : STICK_SBOND_RADIUS)
							: (mConstructionMode == CONSTRUCTION_MODE_THINSTICKS) ?
									(hasNoVisibleNeighbours(atom) ? THINSTICK_ATOM_FACTOR * VDWRadii.getVDWRadius(atomicNo) : THINSTICK_SBOND_RADIUS)
							: (mConstructionMode == CONSTRUCTION_MODE_BALLS) ? BALLS_ATOM_FACTOR * VDWRadii.getVDWRadius(atomicNo)  // to avoid collision with vdw-radii based surface
							: WIRE_ATOM_FACTOR * VDWRadii.getVDWRadius(atomicNo);
					mBuilder.addAtomSphere(RoleHelper.createAtomRole(atom), getCoordinates(atom), radius, getAtomRGB(atom));
					}
				}
			}
		}

	private boolean hasNoVisibleNeighbours(int atom) {
		if (mHydrogenMode == HYDROGEN_MODE_ALL
		 || (mHydrogenMode == HYDROGEN_MODE_POLAR) && mMol.getAtomicNo(atom) != 6)
			return mMol.getAllConnAtoms(atom) == 0;

		return mMol.getConnAtoms(atom) == 0;
		}

	private int getAtomRGB(int atom) {
		return getAtomicNoARGB(mMol.getAtomicNo(atom));
		}

	private int getBondAtomRGB(int atom) {
		return "*".equals(mMol.getAtomCustomLabel(atom)) ? COLOR_NONE : getAtomRGB(atom);
		}

	private boolean includeAtom(int atom) {
		return mHydrogenMode == HYDROGEN_MODE_ALL
			|| !mMol.isSimpleHydrogen(atom)
			|| mMol.getConnAtoms(atom) != 1
			|| (mHydrogenMode == HYDROGEN_MODE_POLAR
			 && mMol.getAtomicNo(mMol.getConnAtom(atom, 0)) != 6);
		}

	/**
	 * @param atom wild card atom drawn as cone, which defines a connection to some other not included part of the molecule
	 */
	private void buildAttachmentPoint(int atom, double radius) {
		Coordinates c1 = getCoordinates(atom);

//		if (!hasVisibleNeighbours(atom)) {
//			mBuilder.addAtomSphere(atomRole(atom), c1, radius, getAtomColor(atom));
//			return;
//			}

		int coreAtom = mMol.getConnAtom(atom, 0);
		Coordinates c2 = getCoordinates(coreAtom);
		delta.set(c1).sub(c2);

		double dist = delta.getLength();
		double dxy = Math.sqrt(delta.x * delta.x + delta.y * delta.y);
		double b = Math.asin(dxy / dist);
		if (delta.z < 0.0)
			b = Math.PI - b;
		if (delta.x < 0.0)
			b = -b;
		double c = (delta.x < 0.0) ? Math.atan(delta.y / delta.x)
				: (delta.x > 0.0) ? Math.atan(delta.y / delta.x)
				: (delta.y > 0.0) ? Math.PI / 2 : -Math.PI / 2;

		double length = 2 * radius;
		Coordinates c3 = c1.subC(delta.scale((1.0-length/dist)/2.0));

		// attachment points may be defined in 2 ways: atomicNo=0 or atomCustomLabel="*". In the latter case they have an atomicNo!
		int rgb = getAtomRGB(mMol.getAtomicNo(atom) != 0 ? atom : coreAtom);

		mBuilder.addAtomCone(RoleHelper.createAtomRole(atom), radius, length, c3, b, c, rgb);
		}

	private void buildBond(int bond) {
		int atom1 = mMol.getBondAtom(0, bond);
		int atom2 = mMol.getBondAtom(1, bond);
		Coordinates c1 = getCoordinates(atom1);
		Coordinates c2 = getCoordinates(atom2);
		center.center(c1, c2);
		delta.set(c2).sub(c1);

		double d = delta.getLength();
		double dxy = Math.sqrt(delta.x * delta.x + delta.y * delta.y);
		double b = Math.asin(c2.z > c1.z ? dxy / d : -dxy / d);
		double c = (delta.x < 0.0) ? Math.atan(delta.y / delta.x) + Math.PI
				: (delta.x > 0.0) ? Math.atan(delta.y / delta.x)
				: (delta.y > 0.0) ? Math.PI / 2 : -Math.PI / 2;

		mBondNodeID = 0;	// incremented with every primitiv to give a unique id for the primitiv

		if (mConstructionMode == CONSTRUCTION_MODE_BALL_AND_STICKS)
			buildBallAndStickBond(bond, d, b, c);
		else
			buildStickBond(bond, d, b, c);
		}

	private void buildBallAndStickBond(int bond, double d, double b, double c) {
		int atom1 = mMol.getBondAtom(0, bond);
		int atom2 = mMol.getBondAtom(1, bond);
		int color1 = (getBondAtomRGB(atom1) == COLOR_NONE) ? COLOR_NONE : BALL_AND_STICK_STICK_COLOR;
		int color2 = (getBondAtomRGB(atom2) == COLOR_NONE) ? COLOR_NONE : BALL_AND_STICK_STICK_COLOR;

		if (color1 == COLOR_NONE && color2 == COLOR_NONE)
			return;

		// We want to cut bonds where they disappear inside the atom sphere, because in case of translucent atoms (e.g. glass) we don't want artefacts
		// Therefore, we calculate length reduction for every bond atom and bond cylinder individually before shifting bond center

		int bondRole = RoleHelper.createBondRole(bond, -1, mBondNodeID++);

		int order = mMol.getBondOrder(bond);
		if (order == 1) {
			d = calcNewReducedBondCenter(d, atom1, atom2, color1, color2, 0.2);
			if (d > 0.0) {
				mBuilder.addBondCylinder(bondRole, BALL_AND_STICK_SBOND_RADIUS, d, center, b, c, BALL_AND_STICK_STICK_COLOR);
				}
			return;
			}

		if (order == 2) {
			Coordinates ds = calculateDoubleBondShift(bond).scale(BALL_AND_STICK_DBOND_SHIFT);
			d = calcNewReducedBondCenter(d, atom1, atom2, color1, color2, 0.20+0.10);
			if (d > 0.0) {
				mBuilder.addBondCylinder(bondRole, BALL_AND_STICK_DBOND_RADIUS, d, point1.set(center).add(ds), b, c, BALL_AND_STICK_STICK_COLOR);
				mBuilder.addBondCylinder(bondRole, BALL_AND_STICK_DBOND_RADIUS, d, point1.set(center).sub(ds), b, c, BALL_AND_STICK_STICK_COLOR);
				}
			return;
			}

		if (order == 3) {
			Coordinates ds = calculateRandomOrthogonalShift(bond).scale(BALL_AND_STICK_TBOND_SHIFT);
			double d1 = calcNewReducedBondCenter(d, atom1, atom2, color1, color2, 0.26+0.10);
			if (d1 > 0.0) {
				mBuilder.addBondCylinder(bondRole, BALL_AND_STICK_TBOND_RADIUS, d1, point1.set(center).add(ds), b, c, BALL_AND_STICK_STICK_COLOR);
				mBuilder.addBondCylinder(bondRole, BALL_AND_STICK_TBOND_RADIUS, d1, point1.set(center).sub(ds), b, c, BALL_AND_STICK_STICK_COLOR);
				}
			d = calcNewReducedBondCenter(d, atom1, atom2, color1, color2, 0.1);
			if (d > 0.0)
				mBuilder.addBondCylinder(bondRole, BALL_AND_STICK_TBOND_RADIUS, d, center, b, c, BALL_AND_STICK_STICK_COLOR);
			}

		if (order == 0) {
			double d1 = (color1 == COLOR_NONE) ? 0.5 * d : calculateBondReduction(atom1, 0.2);
			double d2 = (color2 == COLOR_NONE) ? 0.5 * d : calculateBondReduction(atom2, 0.2);
			if (d1 + d2 >= d) {
				Coordinates p1 = getCoordinates(mMol.getBondAtom(0, bond));
				Coordinates p2 = getCoordinates(mMol.getBondAtom(1, bond));
				point1.between(p1, p2, d1/d);
				point2.between(p2, p1, d2/d);
				buildDottedBond(bond, color1, color2, point1, point2, BALL_AND_STICK_DOT_RADIUS, d);
				}
			return;
			}
		}

	/**
	 * Considering both end length reductions, this method calculates a new weighted center for the bond cylinder
	 * @return reduced length
	 */
	private double calcNewReducedBondCenter(double distance, int atom1, int atom2, int color1, int color2, double sideShift) {
		double d1 = (color1 == COLOR_NONE) ? 0.5 * distance : calculateBondReduction(atom1, sideShift);
		double d2 = (color2 == COLOR_NONE) ? 0.5 * distance : calculateBondReduction(atom2, sideShift);
		if (d1 + d2 >= distance)
			return 0;

		center.between(getCoordinates(atom1), getCoordinates(atom2), 0.5 + 0.5 * (d1 - d2));
		return distance - d1 - d2;
	}

	private void buildStickBond(int bond, double d, double b, double c) {
		int color1 = getBondAtomRGB(mMol.getBondAtom(0, bond));
		int color2 = getBondAtomRGB(mMol.getBondAtom(1, bond));

		int atom1 = mMol.getBondAtom(0, bond);
		int atom2 = mMol.getBondAtom(1, bond);

		Coordinates p1 = getCoordinates(atom1);
		Coordinates p2 = getCoordinates(atom2);

		int order = mMol.getBondOrder(bond);

		if (order == 0) {
			double r = (mConstructionMode == CONSTRUCTION_MODE_WIRES) ? WIRE_DOT_RADIUS
					 : (mConstructionMode == CONSTRUCTION_MODE_THINSTICKS) ? THINSTICK_DOT_RADIUS : STICK_DOT_RADIUS;
			buildDottedBond(bond, color1, color2, p1, p2, r, d);
			return;
			}

		double r1 = (mConstructionMode == CONSTRUCTION_MODE_WIRES) ? WIRE_SBOND_RADIUS
				  : (mConstructionMode == CONSTRUCTION_MODE_THINSTICKS) ? THINSTICK_SBOND_RADIUS : STICK_SBOND_RADIUS;

		if (order == 1) {
			buildStickBond(bond, color1, color2, p1, p2, r1, d, b, c);
			return;
			}

		double r2 = (mConstructionMode == CONSTRUCTION_MODE_WIRES) ? WIRE_DBOND_RADIUS
				  : (mConstructionMode == CONSTRUCTION_MODE_THINSTICKS) ? THINSTICK_DBOND_RADIUS : STICK_DBOND_RADIUS;
		double piShift = (mConstructionMode == CONSTRUCTION_MODE_WIRES) ? WIRE_PI_BOND_SHIFT
					   : (mConstructionMode == CONSTRUCTION_MODE_THINSTICKS) ? THINSTICK_PI_BOND_SHIFT : STICK_PI_BOND_SHIFT;

		if (order == 2) {
			Coordinates ds = calculateDoubleBondShift(bond).scale(piShift);
			buildStickBond(bond, color1, color2, p1, p2, r1, d, b, c);
			buildPiStickBond(bond, color1, color2, point1.set(p1).add(ds), point2.set(p2).add(ds), r2, piShift, d, b, c);
			return;
			}

		if (order == 3) {
			Coordinates ds = calculateRandomOrthogonalShift(bond).scale(piShift);
			buildStickBond(bond, color1, color2, p1, p2, r1, d, b, c);
			buildPiStickBond(bond, color1, color2, point1.set(p1).add(ds), point2.set(p2).add(ds), r2, piShift, d, b, c);
			buildPiStickBond(bond, color1, color2, point1.set(p1).sub(ds), point2.set(p2).sub(ds), r2, piShift, d, b, c);
			return;
			}

		if (order == 4) {
			Coordinates ds = calculateRandomOrthogonalShift(bond).scale(0.4*piShift);
			buildPiStickBond(bond, color1, color2, point1.set(p1).add(ds), point2.set(p2).add(ds), r2, 0.5*piShift, d, b, c);
			buildPiStickBond(bond, color1, color2, point1.set(p1).sub(ds), point2.set(p2).sub(ds), r2, 0.5*piShift, d, b, c);
			ds.scale(3.0);
			buildPiStickBond(bond, color1, color2, point1.set(p1).add(ds), point2.set(p2).add(ds), r2, 1.5*piShift, d, b, c);
			buildPiStickBond(bond, color1, color2, point1.set(p1).sub(ds), point2.set(p2).sub(ds), r2, 1.5*piShift, d, b, c);
			return;
			}

		if (order == 5) {
			buildPiStickBond(bond, color1, color2, p1, p2, r2, 0, d, b, c);
			Coordinates ds = calculateRandomOrthogonalShift(bond).scale(0.75*piShift);
			buildPiStickBond(bond, color1, color2, point1.set(p1).add(ds), point2.set(p2).add(ds), r2, piShift, d, b, c);
			buildPiStickBond(bond, color1, color2, point1.set(p1).sub(ds), point2.set(p2).sub(ds), r2, piShift, d, b, c);
			ds.scale(2.0);
			buildPiStickBond(bond, color1, color2, point1.set(p1).add(ds), point2.set(p2).add(ds), r2, 2*piShift, d, b, c);
			buildPiStickBond(bond, color1, color2, point1.set(p1).sub(ds), point2.set(p2).sub(ds), r2, 2*piShift, d, b, c);
			return;
			}
		}

	private void buildStickBond(int bond, int color1, int color2, Coordinates p1, Coordinates p2,
	                            double r, double d, double b, double c) {
		if (!mSplitAllBonds && color1 == color2) {
			if (color1 != COLOR_NONE) {
				center.center(p1, p2);
				mBuilder.addBondCylinder(RoleHelper.createBondRole(bond, -1, mBondNodeID++), r, d, center, b, c, color1);
				}
			}
		else {
			if (color1 != COLOR_NONE) {
				center.between(p1, p2, 0.25);
				mBuilder.addBondCylinder(RoleHelper.createBondRole(bond, 0, mBondNodeID++), r, d / 2, center, b, c, color1);
				}
			if (color2 != COLOR_NONE) {
				center.between(p1, p2, 0.75);
				mBuilder.addBondCylinder(RoleHelper.createBondRole(bond, 1, mBondNodeID++), r, d / 2, center, b, c, color2);
				}
			}
		}

	private void buildDottedBond(int bond, int color1, int color2, Coordinates p1, Coordinates p2,
								 double r, double d) {
		int dots = 2 * Math.max(2, (int)Math.round(d / (r * 5)));
//		double dd = r / (dots-1);
		for (int i=1; i<dots-1; i++) {
			center.between(p1, p2, (double)i/(dots-1));
			int color = i*2<dots ? color1 : color2;
			if (color != COLOR_NONE)
				mBuilder.addAtomSphere(RoleHelper.createBondRole(bond, i*2<dots ? 0 : 1, mBondNodeID++), center, r, color);
			}
		}

	/**
	 * Builds the second (and third in case of triple bond) shortened and thinner stick
	 * that indicates the unsaturation in STICK mode. Both stick ends are rounded by adding
	 * a sphere and the stick is created from two differently colored cylinders, if both
	 * and atoms have different atomic numbers.
	 * @param bond
	 * @param color1 color of bond atom 0
	 * @param color2 color of bond atom 1
	 * @param c1 bond atom 0 position parallel shifted
	 * @param c2 bond atom 1 position parallel shifted
	 * @param r stick radius
	 * @param piShift distance between sigma bond and thinner pi bond
	 * @param d distance between both bond atoms
	 * @param b rotation around y axis
	 * @param c rotation around z axis
	 */
	private void buildPiStickBond(int bond, int color1, int color2,
	                              Coordinates c1, Coordinates c2, double r, double piShift, double d, double b, double c) {
		center.center(c1, c2);

		double centerShift = piShift / (2 * d);

		double l1 = d / 2;
		Coordinates p1 = c1;
		if (mMol.getAllConnAtoms(mMol.getBondAtom(0, bond)) != 1) {
			p1 = point1.between(c1, c2, centerShift);
			l1 -= piShift / 2;
			}

		double l2 = d / 2;
		Coordinates p2 = c2;
		if (mMol.getAllConnAtoms(mMol.getBondAtom(1, bond)) != 1) {
			p2 = point2.between(c1, c2, 1.0 - centerShift);
			l2 -= piShift / 2;
			}

		if (mConstructionMode != CONSTRUCTION_MODE_WIRES) {
			if (color1 != COLOR_NONE)
				mBuilder.addAtomSphere(RoleHelper.createBondRole(bond, 0, mBondNodeID++), p1, r, color1); //modified by JW
			if (color2 != COLOR_NONE)
				mBuilder.addAtomSphere(RoleHelper.createBondRole(bond, 1, mBondNodeID++), p2, r, color2); // modified by JW
			}

		if (!mSplitAllBonds && color1 == color2) {
			if (color1 != COLOR_NONE) {
				center.center(p1, p2);
				mBuilder.addBondCylinder(RoleHelper.createBondRole(bond, -1, mBondNodeID++), r, l1 + l2, center, b, c, color1);
				}
			}
		else {
			if (color1 != COLOR_NONE) {
				p1.center(center);
				mBuilder.addBondCylinder(RoleHelper.createBondRole(bond, 0, mBondNodeID++), r, l1, p1, b, c, color1);
				}
			if (color2 != COLOR_NONE) {
				p2.center(center);
				mBuilder.addBondCylinder(RoleHelper.createBondRole(bond, 1, mBondNodeID++), r, l2, p2, b, c, color2);
				}
			}
		}

	private Coordinates calculateDoubleBondShift(int bond) {
		int maxScore = 0;
		int maxNeighbor = -1;
		int maxSide = -1;
		for (int i=0; i<2; i++) {
			int atom = mMol.getBondAtom(i, bond);
			int otherAtom = mMol.getBondAtom(1-i, bond);
			for (int j=0; j<mMol.getConnAtoms(atom); j++) {
				int connAtom = mMol.getConnAtom(atom, j);
				if (connAtom != otherAtom) {
					int score = getReferenceNeighborScore(mMol, atom, bond, connAtom, mMol.getConnBond(atom, j));
					if (maxScore < score) {
						maxScore = score;
						maxNeighbor = connAtom;
						maxSide = i;
						}
					}
				}
			}

		if (maxNeighbor != -1) {
			Coordinates ca = getCoordinates(mMol.getBondAtom(maxSide, bond));
			Coordinates vb = getCoordinates(mMol.getBondAtom(1-maxSide, bond)).subC(ca).unit();
			Coordinates vc = getCoordinates(maxNeighbor).subC(ca).unit();
			vb.scale(-vb.dot(vc));
			return vc.addC(vb).unit();
			}

		return calculateRandomOrthogonalShift(bond);
		}

	/**
	 * Returns the preferred double bond neighbor. Double bonds are constructed as one
	 * stick connecting the involved atoms and another parallel one moved to that side,
	 * where the reference bond neighbor points to.
	 * @param mol
	 * @param atom
	 * @param bond
	 * @param connAtom
	 * @param connBond
	 * @return
	 */
	private int getReferenceNeighborScore(StereoMolecule mol, int atom, int bond, int connAtom, int connBond) {
		if (mol.isRingBond(bond)) {
			RingCollection ringSet = mol.getRingSet();
			int score = 512;
			for (int i=0; i<ringSet.getSize(); i++) {
				if (ringSet.isBondMember(i, bond)) {
					if (ringSet.isBondMember(i, connBond)) {
						if (ringSet.isAromatic(i))
							score += 256;
						score += 256 - ringSet.getSize();
						}
					}
				}
			return score;
			}
		else {
			return (mol.getConnAtoms(atom) == 2 ? 256 : 0) + mol.getAtomicNo(connAtom);
			}
		}

	private Coordinates calculateRandomOrthogonalShift(int bond) {
		Coordinates c1 = getCoordinates(mMol.getBondAtom(0, bond));
		Coordinates c2 = getCoordinates(mMol.getBondAtom(1, bond));
		Coordinates v = c2.subC(c1).unit();
		double lxy = Math.sqrt(v.x*v.x+v.y*v.y);
		return (lxy == 0.0) ? new Coordinates(1.0, 0.0, 0.0)
							: new Coordinates(-v.x*v.z/lxy, -v.y*v.z/lxy, lxy);
	}

	private double calculateBondReduction(int atom, double sideShift) {
		double atomRadius = VDWRadii.getVDWRadius(mMol.getAtomicNo(atom)) / 4;
		return (sideShift >= atomRadius) ? 0.0 : Math.sqrt(atomRadius*atomRadius - sideShift*sideShift);
		}

	private Coordinates getCoordinates(int atom) {
		return (mConformer != null) ? mConformer.getCoordinates(atom) : mMol.getCoordinates(atom);
		}
	}

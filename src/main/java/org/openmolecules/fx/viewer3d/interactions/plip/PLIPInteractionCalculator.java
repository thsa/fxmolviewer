package org.openmolecules.fx.viewer3d.interactions.plip;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.interactions.V3DInteraction;
import org.openmolecules.fx.viewer3d.interactions.V3DInteractionCalculator;
import org.openmolecules.fx.viewer3d.interactions.V3DInteractionPoint;
import org.openmolecules.fx.viewer3d.interactions.V3DInteractionSites;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class PLIPInteractionCalculator implements V3DInteractionCalculator {
	// InteractionPoint types:
	public static final int IP_TYPE_HYDROPHOBIC = 0;
	public static final int IP_TYPE_AROMATIC_RING = 1;
	public static final int IP_TYPE_DONOR = 2;
	public static final int IP_TYPE_ACCEPTOR = 3;
	public static final int IP_TYPE_HALOGEN_DONOR = 4;
	public static final int IP_TYPE_HALOGEN_ACCEPTOR = 5;
	public static final int IP_TYPE_POS_CHARGE = 6;
	public static final int IP_TYPE_NEG_CHARGE = 7;
	public static final int IP_TYPE_WATER = 8;


	// Interaction types:
	public static final int I_TYPE_HYDROPHOBIC = 0;
	public static final int I_TYPE_HBOND = 1;
	public static final int I_TYPE_HALOGEN_BOND = 2;
	public static final int I_TYPE_SALT_BRIDGE = 3;
	public static final int I_TYPE_PI_CATION = 4;
	public static final int I_TYPE_PI_STACKING = 5;
	public static final int I_TYPE_WATER_BRIDGE = 6;
	public static final int I_TYPE_WATER_WATER = 7;

	private static final Color[] INTERACTION_COLOR = { Color.BEIGE, Color.CORNFLOWERBLUE, Color.MEDIUMPURPLE,
			Color.DARKOLIVEGREEN, Color.YELLOWGREEN, Color.ORANGE, Color.BLUE, Color.BLUE };


	//	private static final double BS_DIST_MAX = 8.5;
//	private static final double AROMATIC_PLANARITY = 7.5;  // degrees
	private static final double HYDROPH_DIST_MAX = 4.0;
	private static final double HBOND_DIST_MAX = 4.1;
	private static final double HBOND_DON_ANGLE_MIN = 100;  // degrees
	private static final double PISTACK_DIST_MAX = 7.5;
	private static final double PISTACK_ANG_DEV = 30;   // degrees
	private static final double PISTACK_OFFSET_MAX = 2.0;
//	private static final double PICATION_DIST_MAX = 6.0;     // original angle-independent value
	private static final double PICATION_DIST_MAX_0 = 6.5;     // we model a simple linear angle dependency to better reflect DOI 10.1021/jp906086x
	private static final double PICATION_DIST_PLUS_PER_DEGREE = 1.5 / 90;   // 5.0 in plane and 6.5 if perpendicular
	private static final double SALTBRIDGE_DIST_MAX = 5.5;
	private static final double HALOGEN_DIST_MAX = 4.0;
	private static final double HALOGEN_ACC_ANGLE = 120;    // degrees
	private static final double HALOGEN_DON_ANGLE = 165;    // degrees
	private static final double HALOGEN_ANGLE_DEV = 30;    // degrees
	private static final double WATER_BRIDGE_MINDIST = 2.5;
	private static final double WATER_BRIDGE_MAXDIST = 4.0;
	private static final double WATER_BRIDGE_OMEGA_MIN = 75;    // degrees
	private static final double WATER_BRIDGE_OMEGA_MAX = 140;    // degrees
	private static final double WATER_BRIDGE_THETA_MIN = 100;    // degrees

	private static final double WATER_WATER_MAXDIST = 3.5;


	/**
	 * This is one of potentially multiple interaction calculators, which each implement
	 * a specific approach (often based on a paper) to classify interactions between molecules.
	 * The classification is done in two steps. First, individual atoms or groups of atoms of
	 * a given molecule, which are capable of interacting with atoms of another molecule, are
	 * determined and classified as interaction points of that given molecule.
	 * Then, for any given two molecules it determines all interactions between the previously
	 * determined interaction points applying geometrical constraints (distance and angles)
	 * that are specific for the particular interaction point types.
	 * This calculator uses the logic described in Nucleic Acids Research, 2015 1 (doi: 10.1093/nar/gkv315),
	 * "PLIP: fully automated protein–ligand interaction profiler",
	 * Sebastian Salentin, Sven Schreiber, V. Joachim Haupt, Melissa F. Adasme, Michael Schroeder
	 * @param fxmol
	 * @return
	 */
	public List<V3DInteractionPoint> determineInteractionPoints(V3DMolecule fxmol) {
		ArrayList<V3DInteractionPoint> interactionPointList = new ArrayList<>();

		StereoMolecule mol = fxmol.getMolecule();
		mol.ensureHelperArrays(Molecule.cHelperRings);
		for (int ring=0; ring<mol.getRingSet().getSize(); ring++) {
			if (mol.getRingSet().isAromatic(ring)) {
				interactionPointList.add(new V3DInteractionPoint(fxmol, mol.getRingSet().getRingAtoms(ring), IP_TYPE_AROMATIC_RING));
			}
		}

		for (int atom=0; atom<mol.getAtoms(); atom++) {
			if (mol.getAtomicNo(atom) == 6 && mol.getAtomZValue(atom) == 0) {
				interactionPointList.add(new V3DInteractionPoint(fxmol, atom, IP_TYPE_HYDROPHOBIC));
			}

			if (mol.getAtomicNo(atom) == 7 || mol.getAtomicNo(atom) == 8) { // TODO S,P,Se?
				if (mol.getAllHydrogens(atom) != 0)
					interactionPointList.add(new V3DInteractionPoint(fxmol, atom, IP_TYPE_DONOR));
				else if (mol.getAtomCharge(atom) <= 0)
					interactionPointList.add(new V3DInteractionPoint(fxmol, atom, IP_TYPE_ACCEPTOR));
			}

			if ((mol.getAtomicNo(atom) == 7
			  || mol.getAtomicNo(atom) == 8
			  || mol.getAtomicNo(atom) == 16)
			 && mol.getConnAtoms(atom) == 1) {
				int connAtomicNo = mol.getAtomicNo(mol.getConnAtom(atom, 0));
				if (connAtomicNo == 6
				 || connAtomicNo == 7
				 || connAtomicNo == 15
				 || connAtomicNo == 16)
					interactionPointList.add(new V3DInteractionPoint(fxmol, atom, IP_TYPE_HALOGEN_ACCEPTOR));
			}

			if (mol.isHalogene(atom)
			 && mol.getConnAtoms(atom) == 1
			 && mol.getAtomicNo(mol.getConnAtom(atom, 0)) == 6) {
				interactionPointList.add(new V3DInteractionPoint(fxmol, atom, IP_TYPE_HALOGEN_DONOR));
			}

			if (isGuanidinCarbon(mol, atom)
			 || isImidazoleCarbon(mol, atom)  // imidazole side chains (as in histidine) are protonated only below pH=6.5; as in the underlying paper we protonate, but we are sceptical
			 || isAmineNitrogen(mol, atom)
			 || isSulfoniumSulfur(mol, atom)) {
				interactionPointList.add(new V3DInteractionPoint(fxmol, atom, IP_TYPE_POS_CHARGE));
			}

			if (isCarboxylCarbon(mol, atom)
			 || isPhosphatePhosphor(mol, atom)
			 || isSulfonateSulfur(mol, atom))
				interactionPointList.add(new V3DInteractionPoint(fxmol, atom, IP_TYPE_NEG_CHARGE));
		}

		return interactionPointList;
	}

	public void determineInteractions(V3DInteractionSites is1, V3DInteractionSites is2, TreeMap<Integer,ArrayList<V3DInteraction>> interactionMap) {
		interactionMap.clear();
		for (int i=0; i<INTERACTION_COLOR.length; i++)
			interactionMap.put(i, new ArrayList<>());

		for (V3DInteractionPoint p1: is1.getSites() ) {
			for (V3DInteractionPoint p2: is2.getSites() ) {
				V3DInteraction interaction = determineInteraction(p1,p2);
				if (interaction != null)
					interactionMap.get(interaction.getType()).add(interaction);
			}
		}

		removeRedundantHydrophobicInteractions(interactionMap);
		removeRedundantHBondInteractions(interactionMap);
	}

	private V3DInteraction determineInteraction(V3DInteractionPoint ip1, V3DInteractionPoint ip2) {
		Coordinates c1 = ip1.getCenter();
		Coordinates c2 = ip2.getCenter();
		Point3D p1 = ip1.getFXMol().localToParent(c1.x, c1.y, c1.z);
		Point3D p2 = ip2.getFXMol().localToParent(c2.x, c2.y, c2.z);
		double distance = p1.distance(p2);

		// HYDROPHOBIC
		if (ip1.getType() == IP_TYPE_HYDROPHOBIC
		 && ip2.getType() == IP_TYPE_HYDROPHOBIC) {
			return distance<HYDROPH_DIST_MAX ? new V3DInteraction(ip1, ip2, I_TYPE_HYDROPHOBIC, distance, 0.0, 1.0, INTERACTION_COLOR[I_TYPE_HYDROPHOBIC]) : null;
		}

		if ((ip1.getType() == IP_TYPE_DONOR && ip2.getType() == IP_TYPE_ACCEPTOR)
		 || (ip2.getType() == IP_TYPE_DONOR && ip1.getType() == IP_TYPE_ACCEPTOR)) {
			if (distance<HBOND_DIST_MAX) {
				StereoMolecule don = (ip1.getType() == IP_TYPE_DONOR) ? ip1.getMol() : ip2.getMol();
				V3DInteractionPoint donIP = (ip1.getType() == IP_TYPE_DONOR) ? ip1 : ip2;
				Point3D donP = (ip1.getType() == IP_TYPE_DONOR) ? p1 : p2;
				Point3D accP = (ip1.getType() == IP_TYPE_DONOR) ? p2 : p1;
				Point3D pHyd = null;
				int hydrogen = -1;
				double mindist = Double.MAX_VALUE;
				for (int i=0; i<don.getAllConnAtoms(donIP.getAtom()); i++) {
					int atom = don.getConnAtom(donIP.getAtom(), i);
					if (don.getAtomicNo(atom) == 1) {
						Point3D p = donIP.getFXMol().localToParent(don.getAtomX(atom), don.getAtomY(atom), don.getAtomZ(atom));
						double dist = accP.distance(p);
						if (mindist > dist) {
							mindist = dist;
							pHyd = p;
							hydrogen = atom;
						}
					}
				}
				double angle = pHyd.subtract(accP).angle(pHyd.subtract(donP));
				if (angle > HBOND_DON_ANGLE_MIN) {
					V3DInteraction ia = new V3DInteraction(ip1, ip2, I_TYPE_HBOND, distance, angle, 1.0, INTERACTION_COLOR[I_TYPE_HBOND]);
					ia.setVisAtom(ip1.getType() == IP_TYPE_DONOR ? 0 : 1, hydrogen);
					return ia;
				}
			}
			return null;
		}

		// HALOGEN-BOND
		if ((ip1.getType() == IP_TYPE_HALOGEN_DONOR && ip2.getType() == IP_TYPE_HALOGEN_ACCEPTOR)
		 || (ip2.getType() == IP_TYPE_HALOGEN_DONOR && ip1.getType() == IP_TYPE_HALOGEN_ACCEPTOR)) {
			if (distance<HALOGEN_DIST_MAX) {
				V3DInteractionPoint donIP = (ip1.getType() == IP_TYPE_HALOGEN_DONOR) ? ip1 : ip2;
				Point3D donP = (ip1.getType() == IP_TYPE_HALOGEN_DONOR) ? p1 : p2;
				Point3D accP = (ip1.getType() == IP_TYPE_HALOGEN_DONOR) ? p2 : p1;
				Coordinates crDon = donIP.getMol().getAtomCoordinates(donIP.getMol().getConnAtom(donIP.getAtom(), 0));
				Point3D prDon = donIP.getFXMol().localToParent(crDon.x, crDon.y, crDon.z);
				double angle1 = donP.subtract(prDon).angle(donP.subtract(accP));
				if (Math.abs(angle1 - HALOGEN_DON_ANGLE) < HALOGEN_ANGLE_DEV) {
					V3DInteractionPoint accIP = (ip1.getType() == IP_TYPE_HALOGEN_DONOR) ? ip2 : ip1;
					Coordinates accCR = accIP.getMol().getAtomCoordinates(accIP.getMol().getConnAtom(accIP.getAtom(), 0));
					Point3D accPR = accIP.getFXMol().localToParent(accCR.x, accCR.y, accCR.z);
					double angle2 = accP.subtract(accPR).angle(accP.subtract(donP));
					if (Math.abs(angle2 - HALOGEN_ACC_ANGLE) < HALOGEN_ANGLE_DEV)
						return new V3DInteraction(ip1, ip2, I_TYPE_HALOGEN_BOND, distance, 0.0, 1.0, INTERACTION_COLOR[I_TYPE_HALOGEN_BOND]);
				}
			}
		}

		// SALT-BRIDGE
		if ((ip1.getType() == IP_TYPE_POS_CHARGE && ip2.getType() == IP_TYPE_NEG_CHARGE)
		 || (ip2.getType() == IP_TYPE_POS_CHARGE && ip1.getType() == IP_TYPE_NEG_CHARGE)) {
			if (distance<SALTBRIDGE_DIST_MAX)
				return new V3DInteraction(ip1, ip2, I_TYPE_SALT_BRIDGE, distance, 0.0, 1.0, INTERACTION_COLOR[I_TYPE_SALT_BRIDGE]);
		}

		// PI-CATIONS
		if ((ip1.getType() == IP_TYPE_POS_CHARGE && ip2.getType() == IP_TYPE_AROMATIC_RING)
		 || (ip2.getType() == IP_TYPE_POS_CHARGE && ip1.getType() == IP_TYPE_AROMATIC_RING)) {
			double angle;
			if (ip1.getType() == IP_TYPE_AROMATIC_RING) {
				Coordinates n1 = ip1.calculatePlaneNormal();
				Point3D pn1 = ip1.getFXMol().localToParent(c1.x+n1.x, c1.y+n1.y, c1.z+n1.z);
				Point3D n1l = pn1.subtract(p1);     // plane normal in local space
				angle = p2.subtract(p1).angle(n1l);
			}
			else {
				Coordinates n2 = ip2.calculatePlaneNormal();
				Point3D pn2 = ip2.getFXMol().localToParent(c2.x+n2.x, c2.y+n2.y, c2.z+n2.z);
				Point3D n2l = pn2.subtract(p2);     // plane normal in local space
				angle = p1.subtract(p2).angle(n2l);
			}
			// We model a simple angle dependency to get close to DOI 10.1021/jp906086x
			if (angle > 90)
				angle = 180 - 90;
			double distMax = PICATION_DIST_MAX_0 - angle * PICATION_DIST_PLUS_PER_DEGREE;
			if (distance<distMax)
				return new V3DInteraction(ip1, ip2, I_TYPE_PI_CATION, distance, 0.0, 1.0, INTERACTION_COLOR[I_TYPE_PI_CATION]);
		}

		// PI-STACKING
		if (ip1.getType() == IP_TYPE_AROMATIC_RING
		 && ip2.getType() == IP_TYPE_AROMATIC_RING) {
			if (p1.distance(p2) < PISTACK_DIST_MAX) {
				// for a proper translation of the normal vectors in parent space we need to take a detour...
				Coordinates n1 = ip1.calculatePlaneNormal();
				Coordinates n2 = ip2.calculatePlaneNormal();
				Point3D pn1 = ip1.getFXMol().localToParent(c1.x+n1.x, c1.y+n1.y, c1.z+n1.z);
				Point3D pn2 = ip2.getFXMol().localToParent(c2.x+n2.x, c2.y+n2.y, c2.z+n2.z);
				Point3D n1l = pn1.subtract(p1);
				Point3D n2l = pn2.subtract(p2);

				double angle = n1l.angle(n2l);
				if (((angle < PISTACK_ANG_DEV)
				  || (Math.abs(90 - angle) < PISTACK_ANG_DEV)
				  || (180 - angle < PISTACK_ANG_DEV))
				 &&	((projectedDistanceOnPlane(p1, p2, n2l) < PISTACK_OFFSET_MAX)
				  || (projectedDistanceOnPlane(p2, p1, n1l) < PISTACK_OFFSET_MAX)))
					return new V3DInteraction(ip1, ip2, I_TYPE_PI_STACKING, distance, angle, 1.0, INTERACTION_COLOR[I_TYPE_PI_STACKING]);
			}
			return null;
		}

		// HBOND from WATER
		if (ip1.getType() == IP_TYPE_WATER
		 && (ip2.getType() == IP_TYPE_ACCEPTOR || ip2.getType() == IP_TYPE_DONOR)) {
			int[] hydrogenHolder = new int[1];
			if (isHBondToWater(ip2, ip1, p2, p1, distance, hydrogenHolder)) {
				V3DInteraction ia = new V3DInteraction(ip1, ip2, I_TYPE_WATER_BRIDGE, distance, 0.0, 1.0, INTERACTION_COLOR[I_TYPE_WATER_BRIDGE]);
				if (hydrogenHolder[0] != -1)
					ia.setVisAtom(1, hydrogenHolder[0]);
				return ia;
			}
		}

		// HBOND to WATER
		if (ip2.getType() == IP_TYPE_WATER
		 && (ip1.getType() == IP_TYPE_ACCEPTOR || ip1.getType() == IP_TYPE_DONOR)) {
			int[] hydrogenHolder = new int[1];
			if (isHBondToWater(ip1, ip2, p1, p2, distance, hydrogenHolder)) {
				V3DInteraction ia = new V3DInteraction(ip1, ip2, I_TYPE_WATER_BRIDGE, distance, 0.0, 1.0, INTERACTION_COLOR[I_TYPE_WATER_BRIDGE]);
				if (hydrogenHolder[0] != -1)
					ia.setVisAtom(0, hydrogenHolder[0]);
				return ia;
			}
		}

		// WATER-WATER
		if (ip1.getType() == IP_TYPE_WATER
				&& ip2.getType() == IP_TYPE_WATER) {
			if (distance < WATER_WATER_MAXDIST)
				return new V3DInteraction(ip1, ip2, I_TYPE_WATER_WATER, distance, 0.0, 1.0, INTERACTION_COLOR[I_TYPE_WATER_WATER]);
		}

		return null;
	}

	private boolean isHBondToWater(V3DInteractionPoint ip, V3DInteractionPoint water, Point3D p, Point3D pWater, double distance, int[] hydrogenHolder) {
		hydrogenHolder[0] = -1;

		if (distance < WATER_BRIDGE_MINDIST || distance > WATER_BRIDGE_MAXDIST)
			return false;

		if (ip.getType() == IP_TYPE_DONOR) {
			StereoMolecule don = ip.getMol();
			Point3D pHyd = null;
			double mindist = Double.MAX_VALUE;
			for (int i=0; i<don.getAllConnAtoms(ip.getAtom()); i++) {
				int atom = don.getConnAtom(ip.getAtom(), i);
				if (don.getAtomicNo(atom) == 1) {
					Point3D ph= ip.getFXMol().localToParent(don.getAtomX(atom), don.getAtomY(atom), don.getAtomZ(atom));
					double dist = pWater.distance(ph);
					if (mindist > dist) {
						mindist = dist;
						pHyd = ph;
						hydrogenHolder[0] = atom;
					}
				}
			}
			double angle = pHyd.subtract(pWater).angle(pHyd.subtract(p));
			if (angle < WATER_BRIDGE_THETA_MIN)
				return false;
		}

		return true;
	}

	private void removeRedundantHydrophobicInteractions(TreeMap<Integer,ArrayList<V3DInteraction>> interactionMap) {
		// remove all hydrophobic interactions between rings interacting via π-stacking
		ArrayList<V3DInteraction> redundant = new ArrayList<>();
		for (V3DInteraction hydrophobic : interactionMap.get(I_TYPE_HYDROPHOBIC)) {
			int atom1 = hydrophobic.getInteractionPoint(0).getAtom();
			int atom2 = hydrophobic.getInteractionPoint(1).getAtom();
			boolean found = false;
			for (V3DInteraction piStacking : interactionMap.get(I_TYPE_PI_STACKING)) {
				int[] piAtoms1 = piStacking.getInteractionPoint(0).getAtoms();
				int[] piAtoms2 = piStacking.getInteractionPoint(1).getAtoms();
				for (int piAtom1 : piAtoms1) {
					if (piAtom1 == atom1) {
						for (int piAtom2 : piAtoms2) {
							if (piAtom2 == atom2) {
								found = true;
								break;
							}
						}
					}
					if (found)
						break;
				}
				if (found)
					break;
			}
			if (found)
				redundant.add(hydrophobic);
		}
		interactionMap.get(I_TYPE_HYDROPHOBIC).removeAll(redundant);

		// For atoms that interact with several atoms remove all, but the one with the closest distance
		redundant.clear();
		for (int site=0; site<2; site++) {
			TreeMap<Integer,ArrayList<V3DInteraction>> atomInteractionMap = new TreeMap<>();
			for (V3DInteraction hydrophobic : interactionMap.get(I_TYPE_HYDROPHOBIC)) {
				int atom = hydrophobic.getInteractionPoint(site).getAtom();
				ArrayList<V3DInteraction> atomInteractions = atomInteractionMap.get(atom);
				if (atomInteractions == null) {
					atomInteractions = new ArrayList<>();
					atomInteractionMap.put(atom, atomInteractions);
				}
				atomInteractions.add(hydrophobic);
			}
			for (int atom : atomInteractionMap.keySet()) {
				ArrayList<V3DInteraction> atomInteractions = atomInteractionMap.get(atom);
				if (atomInteractions.size() > 1) {
					V3DInteraction shortest = atomInteractions.get(0);
					double distance = shortest.getDistance();
					for (int i=1; i<atomInteractions.size(); i++) {
						V3DInteraction interaction = atomInteractions.get(i);
						if (distance > interaction.getDistance()) {
							distance = interaction.getDistance();
							shortest = interaction;
						}
					}
					for (V3DInteraction interaction:atomInteractions)
						if (interaction != shortest)
							redundant.add(interaction);
				}
			}
		}
		interactionMap.get(I_TYPE_HYDROPHOBIC).removeAll(redundant);
	}

	private void removeRedundantHBondInteractions(TreeMap<Integer,ArrayList<V3DInteraction>> interactionMap) {
		ArrayList<V3DInteraction> redundant = new ArrayList<>();
		for (V3DInteraction hbond : interactionMap.get(I_TYPE_HBOND)) {
			int atom1 = hbond.getInteractionPoint(0).getAtom();
			int atom2 = hbond.getInteractionPoint(1).getAtom();

			boolean found = false;

			for (V3DInteraction saltBridge : interactionMap.get(I_TYPE_SALT_BRIDGE)) {
				int sbAtom1 = saltBridge.getInteractionPoint(0).getAtom();
				int sbAtom2 = saltBridge.getInteractionPoint(1).getAtom();
				if ((atom1 == sbAtom1 && atom2 == sbAtom2)
				 || (atom1 == sbAtom2 && atom2 == sbAtom1)) {
					found = true;
					break;
				}
			}

			if (found)
				redundant.add(hbond);
		}
		interactionMap.get(I_TYPE_HBOND).removeAll(redundant);

		ArrayList<V3DInteraction> allHBonds = interactionMap.get(I_TYPE_HBOND);
		ArrayList<V3DInteraction> bestHBonds = new ArrayList<>();
		for (int site=0; site<2; site++) {
			TreeMap<Integer,ArrayList<V3DInteraction>> atomInteractionMap = new TreeMap<>();
			for (V3DInteraction hbond : allHBonds) {
				V3DInteractionPoint ip = hbond.getInteractionPoint(site);
				if (ip.getType() == IP_TYPE_DONOR) {
					ArrayList<V3DInteraction> atomInteractions = atomInteractionMap.get(ip.getAtom());
					if (atomInteractions == null) {
						atomInteractions = new ArrayList<>();
						atomInteractionMap.put(ip.getAtom(), atomInteractions);
					}
					atomInteractions.add(hbond);
				}
			}

			for (int atom : atomInteractionMap.keySet()) {
				ArrayList<V3DInteraction> hbondsWithAtomAsDonor = atomInteractionMap.get(atom);
				V3DInteraction bestHBond = hbondsWithAtomAsDonor.get(0);
				double bestAngle = bestHBond.getAngle();
				for (int i=1; i<hbondsWithAtomAsDonor.size(); i++) {
					V3DInteraction hbond = hbondsWithAtomAsDonor.get(i);
					if (bestAngle < hbond.getAngle()) {
						bestAngle = hbond.getAngle();
						bestHBond = hbond;
					}
				}
				if (!bestHBonds.contains(bestHBond))
					bestHBonds.add(bestHBond);
			}
		}
		interactionMap.put(I_TYPE_HBOND, bestHBonds);
	}

	/**
	 * @param p
	 * @param q
	 * @param n
	 * @return distance of point p from point q projected on plane defined by point p and normal n
	 */
	private double projectedDistanceOnPlane(Point3D q, Point3D p, Point3D n) {
		Point3D pq = q.subtract(p);
		double d = pq.dotProduct(n);
		double pql = pq.magnitude();
		return Math.sqrt(pql*pql - d*d);
	}

	private boolean isGuanidinCarbon(StereoMolecule mol, int atom) {
		return mol.getAtomicNo(atom) == 6
			&& !mol.isAromaticAtom(atom)
			&& mol.getConnAtoms(atom) == 3
			&& mol.getAtomPi(atom) == 1
			&& mol.getAtomicNo(mol.getConnAtom(atom, 0)) == 7
			&& mol.getAtomicNo(mol.getConnAtom(atom, 1)) == 7
			&& mol.getAtomicNo(mol.getConnAtom(atom, 2)) == 7;
	}

	private boolean isImidazoleCarbon(StereoMolecule mol, int atom) {
		return mol.getAtomicNo(atom) == 6
			&& mol.isAromaticAtom(atom)
			&& mol.getAtomRingSize(atom) == 5
			&& mol.getAtomPi(atom) == 1
			&& mol.getConnAtoms(atom) == 2
			&& mol.getAtomicNo(mol.getConnAtom(atom, 0)) == 7
			&& mol.getAtomicNo(mol.getConnAtom(atom, 1)) == 7;
	}

	private boolean isAmineNitrogen(StereoMolecule mol, int atom) {
		return mol.getAtomicNo(atom) == 7
			&& mol.getAtomPi(atom) == 0
			&& mol.getAtomZValue(atom) == 0
			&& !mol.isAromaticAtom(atom)
			&& !mol.isStabilizedAtom(atom);
	}

	private boolean isSulfoniumSulfur(StereoMolecule mol, int atom) {
		return mol.getAtomicNo(atom) == 16
			&& mol.getAtomPi(atom) == 0
			&& mol.getAtomZValue(atom) == 0
			&& mol.getConnAtoms(atom) == 3;
	}

	private boolean isCarboxylCarbon(StereoMolecule mol, int atom) {
		if (mol.getAtomicNo(atom) == 6
		 && mol.getAtomPi(atom) == 1
		 && mol.getAtomZValue(atom) == 3
		 && mol.getConnAtoms(atom) == 3) {
			int count = 0;
			for (int i=0; i<3; i++) {
				int connAtom = mol.getConnAtom(atom, i);
				if (mol.getAtomicNo(connAtom) == 8
				 && mol.getConnAtoms(connAtom) == 1)
					count++;
			}
			return count == 2;
		}
		return false;
	}

	private boolean isPhosphatePhosphor(StereoMolecule mol, int atom) {
		if (mol.getAtomicNo(atom) == 15
		 && mol.getAtomPi(atom) == 1
		 && mol.getAtomZValue(atom) == 5
		 && mol.getConnAtoms(atom) == 4) {
			int count = 0;
			for (int i=0; i<3; i++) {
				int connAtom = mol.getConnAtom(atom, i);
				if (mol.getAtomicNo(connAtom) == 8
				 && mol.getConnAtoms(connAtom) == 1)
					count++;
			}
			return count >= 2;
		}
		return false;
	}

	private boolean isSulfonateSulfur(StereoMolecule mol, int atom) {
		if (mol.getAtomicNo(atom) == 16
		 && mol.getAtomPi(atom) == 2
		 && mol.getAtomZValue(atom) >= 5
		 && mol.getConnAtoms(atom) == 4) {
			int count = 0;
			for (int i=0; i<3; i++) {
				int connAtom = mol.getConnAtom(atom, i);
				if (mol.getAtomicNo(connAtom) == 8
						&& mol.getConnAtoms(connAtom) == 1)
					count++;
			}
			return count >= 3;
		}
		return false;
	}
}

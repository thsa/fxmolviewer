package org.openmolecules.render;

public class RoleHelper {
	private static final int INDEX_MASK = 0x00FFFFFF;
	private static final int TYPE_MASK = 0x07000000;
	private static final int BONDATOM_MASK = 0x18000000;
	private static final int BONDATOM_SHIFT = 27;
	private static final int NODE_ID_MASK = 0xE0000000;
	private static final int NODE_ID_SHIFT = 29;
	private static final int ROLE_TYPE_ATOM = 0x01000000;
	private static final int ROLE_TYPE_BOND = 0x02000000;
	private static final int ROLE_TYPE_PHARMACOPHORE = 0x03000000;
	private static final int ROLE_TYPE_EXCLUSION = 0x04000000;
	private static final int ROLE_TYPE_TORSION = 0x05000000;

	public static int createAtomRole(int atom) {
		return /*(mAtomDetail++ << MoleculeBuilder.ROLE_DETAIL_SHIFT) | */ ROLE_TYPE_ATOM | atom;
	}

	/**
	 * @param bond
	 * @param bondAtomIndex 0 or 1, if role can be associated to one of the bond atoms, otherwise -1
	 * @param nodeID id to distinguish different cylinders for the same bond (0-5)
	 * @return
	 */
	public static int createBondRole(int bond, int bondAtomIndex, int nodeID) {
		return (nodeID << NODE_ID_SHIFT) | ((bondAtomIndex + 1) << BONDATOM_SHIFT) | ROLE_TYPE_BOND | bond;
	}

	public static int createPharmacophoreRole() {
		return ROLE_TYPE_PHARMACOPHORE;
	}

	public static int createTorsionRole(int bond) {
		return ROLE_TYPE_TORSION | bond;
	}

	public static int createExclusionRole() {
		return ROLE_TYPE_EXCLUSION;
	}

	public static boolean isAtom(int role) {
		return (role & TYPE_MASK) == ROLE_TYPE_ATOM;
	}

	public static boolean isBond(int role) {
		return (role & TYPE_MASK) == ROLE_TYPE_BOND;
	}

	public static boolean isPharmacophore(int role) {
		return (role & TYPE_MASK) == ROLE_TYPE_PHARMACOPHORE;
	}

	public static boolean isTorsion(int role) {
		return (role & TYPE_MASK) == ROLE_TYPE_TORSION;
	}

	public static boolean isExclusion(int role) {
		return (role & TYPE_MASK) == ROLE_TYPE_EXCLUSION;
	}

	public static int getAtom(int role) {
		return isAtom(role) ? role & INDEX_MASK : -1;
	}

	public static int getBond(int role) {
		return isBond(role) ? role & INDEX_MASK : -1;
	}

	public static int getBondAtomIndex(int role) {
		return isBond(role) ? ((role & BONDATOM_MASK) >> BONDATOM_SHIFT) - 1 : -1;
	}

	public static int getTorsion(int role) {
		return isTorsion(role) ? role & INDEX_MASK : -1;
	}

	public static int getPharmacophoreAtom(int role) {
		return isPharmacophore(role) ? role & INDEX_MASK : -1;
	}

	public static int setIndex(int role, int index) {
		role &= ~RoleHelper.INDEX_MASK;
		return role | index;
	}
}

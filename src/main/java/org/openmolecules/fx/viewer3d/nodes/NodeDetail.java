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

package org.openmolecules.fx.viewer3d.nodes;

import com.actelion.research.chem.StereoMolecule;
import javafx.scene.paint.PhongMaterial;
import org.openmolecules.fx.viewer3d.V3DMoleculeBuilder;
import org.openmolecules.render.RoleHelper;

/**
 * Created by thomas on 15.11.15.
 */
public class NodeDetail {
    private final PhongMaterial mMaterial;
    private int mRole;
    private final boolean mMayOverride;
    private boolean mIsSelected;

	public NodeDetail(PhongMaterial material, int role, boolean mayOverride) {
        mMaterial = material;
        mRole = role;
	    mMayOverride = mayOverride;
        }

    public PhongMaterial getMaterial() {
        return mMaterial;
        }

    public boolean mayOverrideMaterial() {
        return mMayOverride;
        }

    public boolean isAtom() {
        return RoleHelper.isAtom(mRole);
        }

    public boolean isBond() {
		return RoleHelper.isBond(mRole);
	}
    
    public boolean isPharmacophore() {
        return RoleHelper.isPharmacophore(mRole);
        }

	public boolean isTorsion() {
		return RoleHelper.isTorsion(mRole);
	}

	public boolean isExclusion() {
		return RoleHelper.isExclusion(mRole);
	}

	public boolean isSelected() {
        return mIsSelected;
    }

    public void setSelected(boolean isSelected) {
        mIsSelected = isSelected;
    	}

	/**
	 * @return whether this node is transparent and used as helper for atom picking in wire or stick mode
	 */
	public boolean isTransparent() {
		return mMaterial == V3DMoleculeBuilder.getMaterial(0);
		}

	public int getRole() {
		return mRole;
		}

    public int getAtom() {
        return RoleHelper.getAtom(mRole);
        }

    public int getBond() {
		return RoleHelper.getBond(mRole);
    }

	public int getBondAtom(StereoMolecule mol) {
		int bond = RoleHelper.getBond(mRole);
		int no = RoleHelper.getBondAtomIndex(mRole);
		return no == -1 ? -1 : mol.getBondAtom(no, bond);
	}

	public int getTorsion() {
		return RoleHelper.getTorsion(mRole);
    }
    
    public int getPharmacophoreAtom() {
		return RoleHelper.getPharmacophoreAtom(mRole);
    }

    public void setIndex(int index) {
		mRole = RoleHelper.setIndex(mRole, index);
    }
}

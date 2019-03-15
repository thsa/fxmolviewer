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

package org.openmolecules.fx.viewer3d;

import com.actelion.research.chem.Molecule;
import javafx.scene.paint.PhongMaterial;
import org.openmolecules.render.MoleculeBuilder;

/**
 * Created by thomas on 15.11.15.
 */
public class NodeDetail {
    private PhongMaterial mMaterial;
    private int mRole;
    private boolean mMayOverride,mIsSelected;

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
        return (mRole & MoleculeBuilder.ROLE_IS_ATOM) != 0;
        }

    public boolean isBond() {
        return (mRole & MoleculeBuilder.ROLE_IS_BOND) != 0;
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
		return mMaterial == V3DMoleculeBuilder.sTransparentMaterial;
		}

	public int getRole() {
		return mRole;
		}

    public int getAtom() {
        return (mRole & MoleculeBuilder.ROLE_IS_ATOM) != 0 ? mRole & MoleculeBuilder.ROLE_INDEX_BITS : -1;
        }

    public int getBond() {
	    return (mRole & MoleculeBuilder.ROLE_IS_BOND) != 0 ? mRole & MoleculeBuilder.ROLE_INDEX_BITS : -1;
    }

    public void setIndex(int index) {
		mRole &= ~MoleculeBuilder.ROLE_INDEX_BITS;
		mRole |= index;
    }
}

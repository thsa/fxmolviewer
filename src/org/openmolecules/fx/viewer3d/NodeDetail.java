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

import javafx.scene.paint.PhongMaterial;

/**
 * Created by thomas on 15.11.15.
 */
public class NodeDetail {
    private PhongMaterial mMaterial;
    private int mAtom,mBond;
    private boolean mMayOverride,mIsSelected;

    public NodeDetail(PhongMaterial material, int atom, int bond, boolean mayOverride) {
        mMaterial = material;
        mAtom = atom;
        mBond = bond;
	    mMayOverride = mayOverride;
        }

    public PhongMaterial getMaterial() {
        return mMaterial;
        }

    public boolean mayOverrideMaterial() {
        return mMayOverride;
        }

    public boolean isAtom() {
        return mAtom != -1;
        }

    public boolean isBond() {
        return mBond != -1;
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

    public int getAtom() {
        return mAtom;
        }

    public int getBond() {
        return mBond;
    }

    public void setAtom(int atom) {
		mAtom = atom;
    }

	public void setBond(int bond) {
		mBond = bond;
	}
    }

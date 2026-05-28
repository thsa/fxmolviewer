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

import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.util.ColorHelper;
import javafx.scene.effect.BlendMode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.openmolecules.fx.viewer3d.interactions.V3DInteractionHandler;

public class V3DSceneWithSelection extends BorderPane implements V3DHighlightListener {
	private final V3DScene mScene3D;
	private Polygon mSelection;
	private Text mLeftBottomText;
	private int mSelectionMode;	// 1:adding, 2:subtracting

	public V3DSceneWithSelection(V3DScene scene3D) {
		mScene3D = scene3D;
		mScene3D.addHighlightListener(this);

		setOnMousePressed(me -> {
			mSelection = null;
			if (me.getButton() == MouseButton.PRIMARY && !me.isMetaDown() && !me.isAltDown()) {
				mSelectionMode = me.isShiftDown() ? 1 : me.isControlDown() ? 2 : 0;
				startSelection(me.getX(), me.getY());
			}
		} );
		setOnMouseDragged(me -> {
			mScene3D.setMouseDragged(true);
			if (mSelection != null) {
				appendToSelection(me.getX(), me.getY());
			}
		} );
		setOnMouseReleased(me -> {
			if (mSelection != null) {
				finishSelection();
			}
		} );
		getChildren().addAll(scene3D);
	}

	@Override
	public void highlightedAtomChanged(V3DMolecule fxmol, int atom) {
		String atomText = null;
		V3DInteractionHandler interactionHandler = mScene3D.getInteractionHandler();
		if (fxmol != null && atom != -1 && interactionHandler != null) {
			atomText = interactionHandler.getInteractionInfo(fxmol, atom);
		}
		if ((mLeftBottomText == null ^ atomText == null)
		 || (mLeftBottomText != null && !mLeftBottomText.getText().equals(atomText))) {
			if (mLeftBottomText != null) {
				getChildren().remove(mLeftBottomText);
				mLeftBottomText = null;
			}
			if (atomText != null) {
				int gap = HiDPIHelper.scale(8);
				int textSize = HiDPIHelper.scale(12);
				mLeftBottomText = new Text(atomText);
				mLeftBottomText.setFont(new Font(textSize));
//				mLeftBottomText.setWrappingWidth(getWidth()-2*gap);
//				mLeftBottomText.setTextAlignment(TextAlignment.LEFT);
				float[] rgb = new float[3];
				rgb[0] = (float)mScene3D.getBackground().getRed();
				rgb[1] = (float)mScene3D.getBackground().getGreen();
				rgb[2] = (float)mScene3D.getBackground().getBlue();
				mLeftBottomText.setFill(Color.gray(ColorHelper.perceivedBrightness(rgb) < 0.5 ? 0.9 : 0.1));
				mLeftBottomText.setX(gap);
				mLeftBottomText.setY(getHeight() - mLeftBottomText.getLayoutBounds().getHeight() + 2 * textSize);
				mLeftBottomText.setBlendMode(BlendMode.DIFFERENCE);
				getChildren().add(mLeftBottomText);
			}
		}
	}

	private void startSelection(double x, double y) {
		mSelection = new Polygon();
		mSelection.setStroke(V3DScene.SELECTION_COLOR);
		mSelection.setFill(Color.TRANSPARENT);
		mSelection.getPoints().add(x);
		mSelection.getPoints().add(y);
		getChildren().add(mSelection);
	}

	private void appendToSelection(double x, double y) {
		int index = mSelection.getPoints().size()-2;
		double xOld = mSelection.getPoints().get(index);
		double yOld = mSelection.getPoints().get(index+1);
		if (Math.abs(xOld - x) > 2 || Math.abs(yOld - y) > 2) {
			mSelection.getPoints().add(x);
			mSelection.getPoints().add(y);
		}
	}

	private void finishSelection() {
		getChildren().remove(mSelection);
		if (mSelection.getPoints().size() > 2)
			mScene3D.select(mSelection, mSelectionMode, localToScreen(0,0));
		mSelection = null;
	}

	public V3DScene getScene3D() {
		return mScene3D;
	}
}

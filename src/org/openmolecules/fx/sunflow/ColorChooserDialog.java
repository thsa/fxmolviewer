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

package org.openmolecules.fx.sunflow;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.util.NoSuchElementException;
import java.util.Optional;

public class ColorChooserDialog extends Dialog<Color> {
	public static Color showDialog(final Window parent, final String headerText, final Color currentColor) {
		Optional<Color> result = new ColorChooserDialog(parent, headerText).showAndWait();
		try { return result.get(); } catch (NoSuchElementException e) { return null; }
	}

	public ColorChooserDialog(Window parent, String headerText) {
		super();

		initOwner(parent);
//		initStyle(StageStyle.UNDECORATED);
		initModality(Modality.WINDOW_MODAL);
		setTitle("Color Chooser");
		setHeaderText(headerText);
//		setGraphic(new ImageView(this.getClass().getResource("something.png").toString()));

		ColorChooser colorChooser = new ColorChooser();
		getDialogPane().setContent(colorChooser);

		getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

		setResultConverter(button -> (button == ButtonType.OK) ? colorChooser.getChosenColor() : null);
	}
}

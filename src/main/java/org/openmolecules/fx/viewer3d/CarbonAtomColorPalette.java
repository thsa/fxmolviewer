package org.openmolecules.fx.viewer3d;

import javafx.scene.paint.Color;

import java.util.Arrays;
import java.util.List;

public class CarbonAtomColorPalette {
	
	private List<Color> mColors;
	private static CarbonAtomColorPalette sInstance;
	
	private CarbonAtomColorPalette() {
		init();
	}

	/**
	 * Assigns and returns reproducibly a color from the palette for a given id.
	 * If id < 0, null is returned.
	 * @param id
	 * @return
	 */
	public static Color getColor(int id) {
		if (id < 0)
			return null;

		if(sInstance==null) {
			synchronized(CarbonAtomColorPalette.class) {
				if(sInstance==null) {
					sInstance = new CarbonAtomColorPalette();
				}
			}
		}
		int nr = id % sInstance.mColors.size();
		return sInstance.mColors.get(nr);
	}
	
	private void init() {
		mColors = Arrays.asList(Color.AQUAMARINE, Color.BLUEVIOLET,
				Color.BROWN,Color.BURLYWOOD, Color.CADETBLUE, Color.CHARTREUSE, Color.CHOCOLATE,
				Color.CORAL, Color.CORNFLOWERBLUE, Color.CRIMSON, Color.CYAN,
				Color.DARKCYAN, Color.DARKGOLDENROD, Color.DARKGRAY, Color.DARKKHAKI, Color.DARKMAGENTA,
				Color.DARKORANGE, Color.DARKORCHID, Color.DARKRED, Color.DARKSALMON, Color.DARKSEAGREEN,
				Color.DARKSLATEBLUE, Color.DARKTURQUOISE, Color.DARKTURQUOISE,
				Color.DARKVIOLET, Color.DEEPPINK, Color.DEEPSKYBLUE, Color.DODGERBLUE,
				Color.FIREBRICK, Color.FUCHSIA, Color.GOLD, Color.GOLDENROD, Color.GREENYELLOW,
				Color.HOTPINK, Color.LEMONCHIFFON, Color.INDIANRED, Color.INDIGO, Color.KHAKI,
				Color.LAVENDER, Color.LAVENDERBLUSH, Color.LAWNGREEN, Color.LEMONCHIFFON, Color.LIGHTBLUE,
				Color.LIGHTCORAL, Color.LIGHTCYAN, Color.LIGHTGOLDENRODYELLOW, Color.LIGHTGREEN,
				Color.LIGHTPINK, Color.LIGHTSALMON, Color.LIGHTSEAGREEN, Color.LIGHTSKYBLUE,
				Color.LIGHTSTEELBLUE, Color.LIGHTYELLOW, Color.LIME, Color.LIMEGREEN, Color.MEDIUMAQUAMARINE,
				Color.MAGENTA, Color.MAROON, Color.MEDIUMORCHID, Color.MEDIUMPURPLE, Color.MEDIUMSEAGREEN,
				Color.MEDIUMSLATEBLUE, Color.MEDIUMSPRINGGREEN, Color.MEDIUMTURQUOISE, Color.MEDIUMVIOLETRED,
				Color.MIDNIGHTBLUE, Color.MINTCREAM, Color.MISTYROSE, Color.MOCCASIN, Color.OLIVE,
				Color.OLIVEDRAB, Color.ORANGE, Color.ORANGERED, Color.ORCHID, Color.PALEGOLDENROD, Color.PALEGREEN,
				Color.PALETURQUOISE, Color.PALEVIOLETRED, Color.PAPAYAWHIP, Color.PEACHPUFF, Color.PERU, Color.PINK,
				Color.PLUM, Color.POWDERBLUE, Color.PURPLE, Color.SPRINGGREEN, Color.STEELBLUE, Color.TAN, Color.TEAL,
				Color.THISTLE, Color.TOMATO, Color.TURQUOISE, Color.VIOLET, Color.WHEAT, Color.YELLOW, Color.YELLOWGREEN);
	}
}

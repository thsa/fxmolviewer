package org.openmolecules.fx.viewer3d;

import java.util.Arrays;
import java.util.List;
import javafx.scene.paint.Color;

public class CarbonAtomColorPalette {
	
	private List<Color> mColors;
	private static CarbonAtomColorPalette sInstance;
	
	private CarbonAtomColorPalette() {
		init();
	}
	
	public static Color getNextColor(int id) {
		if(sInstance==null) {
			synchronized(CarbonAtomColorPalette.class) {
				if(sInstance==null) {
					sInstance = new CarbonAtomColorPalette();
				}
			}
		}
		int nr = (id % sInstance.mColors.size());
		return sInstance.mColors.get(nr);
		
		
			
		
		
	}
	
	public void init() {
		
		mColors = Arrays.asList(new Color[] {Color.ALICEBLUE, Color.ANTIQUEWHITE, Color.AQUAMARINE,
			Color.BLUEVIOLET, 
			Color.BROWN,Color.BURLYWOOD, Color.CADETBLUE, Color.CHARTREUSE, Color.CHOCOLATE,
			Color.CORAL, Color.CORNFLOWERBLUE, Color.CORNSILK, Color.CRIMSON, Color.CYAN, 
			Color.DARKCYAN, Color.DARKGOLDENROD, Color.DARKGRAY, Color.DARKKHAKI, Color.DARKMAGENTA, 
			Color.DARKORANGE, Color.DARKORCHID, Color.DARKRED, Color.DARKSALMON, Color.DARKSEAGREEN, 
			Color.DARKSLATEBLUE, Color.DARKSLATEGRAY, Color.DARKTURQUOISE, Color.DARKTURQUOISE, 
			Color.DARKVIOLET, Color.DEEPPINK, Color.DEEPSKYBLUE, Color.DIMGRAY, Color.DODGERBLUE,
			Color.FIREBRICK, Color.FUCHSIA, Color.GOLD, Color.GOLDENROD, Color.GREENYELLOW, 
			Color.HOTPINK, Color.LEMONCHIFFON, Color.INDIANRED, Color.INDIGO, Color.KHAKI, 
			Color.LAVENDER, Color.LAVENDERBLUSH, Color.LAWNGREEN, Color.LEMONCHIFFON, Color.LIGHTBLUE,
			Color.LIGHTCORAL, Color.LIGHTCYAN, Color.LIGHTGOLDENRODYELLOW, Color.LIGHTGRAY, Color.LIGHTGREEN,
			Color.LIGHTPINK, Color.LIGHTSALMON, Color.LIGHTSEAGREEN, Color.LIGHTSKYBLUE, Color.LIGHTSLATEGREY,
			Color.LIGHTSTEELBLUE, Color.LIGHTYELLOW, Color.LIME, Color.LIMEGREEN, Color.MEDIUMAQUAMARINE, 
			Color.MAGENTA, Color.MAROON, Color.MEDIUMORCHID, Color.MEDIUMPURPLE, Color.MEDIUMSEAGREEN, 
			Color.MEDIUMSLATEBLUE, Color.MEDIUMSPRINGGREEN, Color.MEDIUMTURQUOISE, Color.MEDIUMVIOLETRED, 
			Color.MIDNIGHTBLUE, Color.MINTCREAM, Color.MISTYROSE, Color.MOCCASIN, Color.OLIVE, 
			Color.OLIVEDRAB, Color.ORANGE, Color.ORANGERED, Color.ORCHID, Color.PALEGOLDENROD, Color.PALEGREEN, 
			Color.PALETURQUOISE, Color.PALEVIOLETRED, Color.PAPAYAWHIP, Color.PEACHPUFF, Color.PERU, Color.PINK,
			Color.PLUM, Color.POWDERBLUE, Color.PURPLE, Color.SPRINGGREEN, Color.STEELBLUE, Color.TAN, Color.TEAL,
			Color.THISTLE, Color.TOMATO, Color.TURQUOISE, Color.VIOLET, Color.WHEAT, Color.YELLOW, Color.YELLOWGREEN});
	}
}

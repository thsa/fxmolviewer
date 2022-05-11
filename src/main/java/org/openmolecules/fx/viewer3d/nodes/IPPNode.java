package org.openmolecules.fx.viewer3d.nodes;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

/*
 * An interface that defines functionality for Nodes that represent Pharmacophoric features.
 * The locations of the Pharmacophores should be updated when the coordinates of the corresponding 3DGaussian are changed.
 * Furthermore, PPNodes should display a menu for removing them or adjusting the weight.
 */


public interface IPPNode {
	
	void showMenu(double x, double y);
	
	void construct();
	
	void cleanup();
	
	void update();
	
	double getScalingFactor();
	
	

}

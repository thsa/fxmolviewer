package org.openmolecules.fx.viewer3d.panel;

import javafx.scene.control.TreeTableView;
import javafx.scene.layout.Pane;

public class TreeTableViewNoHeader<T> extends TreeTableView<T>{
	
	    @Override
	    public void resize(double width, double height) {
	        super.resize(width, height);
	        Pane header = (Pane) lookup("TableHeaderRow");
	        header.setMinHeight(0);
	        header.setPrefHeight(0);
	        header.setMaxHeight(0);
	        header.setVisible(false);
	    }
	

}

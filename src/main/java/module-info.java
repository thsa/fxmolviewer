module fxmolviewer {
	requires java.desktop;
	requires javafx.controls;
	requires javafx.base;
	requires javafx.fxml;
	requires javafx.swing;
	requires openchemlib;
	requires janino;
	requires commons.compiler;
	requires mmtf.codec;
	requires mmtf.serialization;
	requires mmtf.api;

	exports org.openmolecules.fx.surface;
	exports org.openmolecules.fx.viewer3d;
	exports org.openmolecules.fx.viewer3d.io;
	exports org.openmolecules.mesh;
	exports org.openmolecules.render;
	exports org.openmolecules.pdb;

}

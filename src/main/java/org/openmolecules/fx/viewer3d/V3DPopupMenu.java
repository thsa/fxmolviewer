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

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Screen;
import org.openmolecules.fx.sunflow.RayTraceDialog;
import org.openmolecules.fx.sunflow.RayTraceOptions;
import org.openmolecules.fx.surface.ClipSurfaceCutter;
import org.openmolecules.fx.surface.PolygonSurfaceCutter;
import org.openmolecules.fx.surface.SurfaceMesh;
import org.openmolecules.fx.tasks.V3DMinimizer;
import org.openmolecules.fx.viewer3d.nodes.Ribbon;
import org.openmolecules.mesh.MoleculeSurfaceAlgorithm;
import org.openmolecules.render.MoleculeArchitect;
import org.openmolecules.render.TorsionStrainVisualization;

import java.util.EnumSet;
import java.util.Optional;

public class V3DPopupMenu extends ContextMenu {
	private static final boolean DEFAULT_USE_WHEEL_FOR_CLIPPING = false;
	private static final double CLIP_STEEPNESS = 2.5;
	private static final double MIN_CLIP = V3DScene.CAMERA_NEAR_CLIP;
	private static final double MAX_CLIP = Math.min(100, V3DScene.CAMERA_FAR_CLIP);

	private static RayTraceOptions sPreviousMoleculeRayTraceOptions,sPreviousSceneRayTraceOptions;
	private static V3DPopupMenu sPopupMenu;
	protected static boolean sUseMouseWheelForClipping = DEFAULT_USE_WHEEL_FOR_CLIPPING;

	private final V3DMolecule mMolecule;
	private final V3DScene mScene;

	public V3DPopupMenu(final V3DScene scene, final V3DMolecule fxmol) {
		if (sPopupMenu != null && sPopupMenu.isShowing())
			sPopupMenu.hide();

		sPopupMenu = this;
		mMolecule = fxmol;
		mScene = scene;
		V3DPopupMenuController controller = scene.getPopupMenuController();
		EnumSet<V3DScene.ViewerSettings> settings = scene.getSettings();

		if (controller != null)	// Add external File items
			controller.addExternalMenuItems(this, V3DPopupMenuController.TYPE_FILE);

		if (controller != null)	// Add external View items
			controller.addExternalMenuItems(this, V3DPopupMenuController.TYPE_EDIT);

		if (settings == null || settings.contains(V3DScene.ViewerSettings.EDITING)) {
			Menu menuEdit = new Menu("Edit");

			MenuItem itemCut = new MenuItem("Cut Molecule");
			itemCut.setDisable(fxmol == null);
			itemCut.setOnAction(e -> scene.cut(fxmol));

			MenuItem itemCopy3D = new MenuItem("Copy Molecule 3D");
			itemCopy3D.setDisable(fxmol == null);
			itemCopy3D.setOnAction(e -> scene.copy3D(fxmol));

			MenuItem itemCopy2D = new MenuItem("2D-Molecule");
			itemCopy2D.setDisable(fxmol == null);
			itemCopy2D.setOnAction(e -> scene.copy2D(fxmol));

			MenuItem itemCopyIDCode = new MenuItem("ID-Code");
			itemCopyIDCode.setDisable(fxmol == null);
			itemCopyIDCode.setOnAction(e -> scene.copyAsIDCode(fxmol));

			MenuItem itemCopyMolfileV2 = new MenuItem("Molfile V2");
			itemCopyMolfileV2.setDisable(fxmol == null);
			itemCopyMolfileV2.setOnAction(e -> scene.copyAsMolfileV2(fxmol));

			MenuItem itemCopyMolfileV3 = new MenuItem("Molfile V3");
			itemCopyMolfileV3.setDisable(fxmol == null);
			itemCopyMolfileV3.setOnAction(e -> scene.copyAsMolfileV3(fxmol));

			MenuItem itemCopySmiles = new MenuItem("SMILES");
			itemCopySmiles.setDisable(fxmol == null);
			itemCopySmiles.setOnAction(e -> scene.copyAsSmiles(fxmol));

			Menu menuCopy = new Menu("Copy Molecule As");
			menuCopy.getItems().addAll(itemCopy2D, itemCopyIDCode, itemCopyMolfileV2, itemCopyMolfileV3, itemCopySmiles);

			MenuItem itemPaste = new MenuItem("Paste Molecule");
			itemPaste.setOnAction(e -> scene.paste());

			MenuItem itemDelete = new MenuItem("Delete Molecule");
			itemDelete.setDisable(fxmol == null);
			itemDelete.setOnAction(e -> scene.delete(fxmol));

			MenuItem itemClear = new MenuItem("Clear All");
			itemClear.setOnAction(e -> scene.clearAll());

			menuEdit.getItems().addAll(itemCut, itemCopy3D, menuCopy, itemPaste, itemDelete, new SeparatorMenuItem(), itemClear);

			if (settings == null || !settings.contains(V3DScene.ViewerSettings.SMALL_MOLS)) {
				MenuItem itemCrop6 = new MenuItem("0.6 nm");
				itemCrop6.setOnAction(e -> scene.crop(fxmol, 6.0));

				MenuItem itemCrop8 = new MenuItem("0.8 nm");
				itemCrop8.setOnAction(e -> scene.crop(fxmol, 8.0));

				MenuItem itemCrop10 = new MenuItem("1.0 nm");
				itemCrop10.setOnAction(e -> scene.crop(fxmol, 10.0));

				MenuItem itemCrop12 = new MenuItem("1.2 nm");
				itemCrop12.setOnAction(e -> scene.crop(fxmol, 12.0));

				Menu menuCrop = new Menu("Crop all beyond");
				menuCrop.getItems().addAll(itemCrop6, itemCrop8, itemCrop10, itemCrop12);

				menuEdit.getItems().addAll(new SeparatorMenuItem(), menuCrop);
			}

			getItems().add(menuEdit);
			getItems().add(new SeparatorMenuItem());
		}
		else {
			MenuItem itemCopy3D = new MenuItem("3D-Molecule");
			itemCopy3D.setDisable(fxmol == null);
			itemCopy3D.setOnAction(e -> scene.copy3D(fxmol));

			MenuItem itemCopy2D = new MenuItem("2D-Molecule");
			itemCopy2D.setDisable(fxmol == null);
			itemCopy2D.setOnAction(e -> scene.copy2D(fxmol));

			MenuItem itemCopyIDCode = new MenuItem("ID-Code");
			itemCopyIDCode.setDisable(fxmol == null);
			itemCopyIDCode.setOnAction(e -> scene.copyAsIDCode(fxmol));

			MenuItem itemCopyMolfileV2 = new MenuItem("Molfile V2");
			itemCopyMolfileV2.setDisable(fxmol == null);
			itemCopyMolfileV2.setOnAction(e -> scene.copyAsMolfileV2(fxmol));

			MenuItem itemCopyMolfileV3 = new MenuItem("Molfile V3");
			itemCopyMolfileV3.setDisable(fxmol == null);
			itemCopyMolfileV3.setOnAction(e -> scene.copyAsMolfileV3(fxmol));

			MenuItem itemCopySmiles = new MenuItem("SMILES");
			itemCopySmiles.setDisable(fxmol == null);
			itemCopySmiles.setOnAction(e -> scene.copyAsSmiles(fxmol));

			Menu menuCopy = new Menu("Copy Molecule As");
			menuCopy.getItems().addAll(itemCopy3D, itemCopy2D, itemCopyIDCode, itemCopyMolfileV2, itemCopyMolfileV3, itemCopySmiles);

			getItems().add(menuCopy);
			getItems().add(new SeparatorMenuItem());
		}

		if (controller != null)	// Add external View items
			controller.addExternalMenuItems(this, V3DPopupMenuController.TYPE_VIEW);


		MenuItem itemCenter = new MenuItem("Center View");
		itemCenter.setOnAction(e -> scene.optimizeView());
		Menu menuReset = new Menu("Reset Location");
		MenuItem itemResetMolecule = new MenuItem("Of This Molecule");
		itemResetMolecule.setDisable(fxmol == null);
		itemResetMolecule.setOnAction(e -> {
			fxmol.clearTransform();
			fxmol.resetCoordinates();
		});
		MenuItem itemResetAll = new MenuItem("Of All Molecules");
		itemResetAll.setOnAction(e -> {
			for (Node n:scene.getWorld().getChildren()) {
				if (n instanceof V3DMolecule) {
					((V3DMolecule)n).clearTransform();
					((V3DMolecule)n).resetCoordinates();
				}
			}
		});
		menuReset.getItems().addAll(itemResetMolecule, itemResetAll);

		RadioMenuItem measurementsNone = new RadioMenuItem("None");
		measurementsNone.setSelected(scene.getMeasurementMode() == V3DScene.MEASUREMENT.NONE);
		measurementsNone.setOnAction(e -> scene.setMeasurementMode(V3DScene.MEASUREMENT.NONE));
		RadioMenuItem measurementsDistance = new RadioMenuItem("Distance");
		measurementsDistance.setSelected(scene.getMeasurementMode() == V3DScene.MEASUREMENT.DISTANCE);
		measurementsDistance.setOnAction(e -> scene.setMeasurementMode(V3DScene.MEASUREMENT.DISTANCE));
		RadioMenuItem measurementsAngle = new RadioMenuItem("Angle");
		measurementsAngle.setSelected(scene.getMeasurementMode() == V3DScene.MEASUREMENT.ANGLE);
		measurementsAngle.setOnAction(e -> scene.setMeasurementMode(V3DScene.MEASUREMENT.ANGLE));
		RadioMenuItem measurementsDihedral = new RadioMenuItem("Torsion");
		measurementsDihedral.setSelected(scene.getMeasurementMode() == V3DScene.MEASUREMENT.TORSION);
		measurementsDihedral.setOnAction(e -> scene.setMeasurementMode(V3DScene.MEASUREMENT.TORSION));
		MenuItem measurementsRemoveAll = new MenuItem("Remove All");
		measurementsRemoveAll.setOnAction(e -> scene.removeMeasurements());

		MenuItem itemStereoView = null;
		if (V3DStereoPane.getFullScreenView() == null) {
			itemStereoView = new Menu("Show Stereo View");
			ObservableList<Screen> screens = Screen.getScreens();
			for (int i=0; i<screens.size(); i++) {
				final Screen screen = screens.get(i);
				Rectangle2D bounds = screen.getBounds();
				String deviceString = "Screen "+i+" ["+Math.round(screen.getOutputScaleX()*bounds.getWidth())+"x"+Math.round(screen.getOutputScaleY()*bounds.getHeight())+"]";
				MenuItem itemSBS = new MenuItem("SBS - "+deviceString);
				itemSBS.setOnAction(e -> V3DStereoPane.createFullScreenView(scene, screen, V3DStereoPane.MODE_SBS));
				MenuItem itemHSBS = new MenuItem("HSBS - "+deviceString);
				itemHSBS.setOnAction(e -> V3DStereoPane.createFullScreenView(scene, screen, V3DStereoPane.MODE_HSBS));
				MenuItem itemOU = new MenuItem("OU - "+deviceString);
				itemOU.setOnAction(e -> V3DStereoPane.createFullScreenView(scene, screen, V3DStereoPane.MODE_OU));
				MenuItem itemHOU = new MenuItem("HOU - "+deviceString);
				itemHOU.setOnAction(e -> V3DStereoPane.createFullScreenView(scene, screen, V3DStereoPane.MODE_HOU));

				((Menu)itemStereoView).getItems().addAll(itemSBS, itemHSBS, itemOU, itemHOU);
			}
		}
		else {
			itemStereoView = new MenuItem("Close Stereo View");
			itemStereoView.setOnAction(e -> V3DStereoPane.closeFullScreenView() );
		}

		Menu menuView = new Menu("View");
		menuView.getItems().addAll(itemCenter, menuReset, new SeparatorMenuItem(), itemStereoView);

		getItems().add(menuView);
		getItems().add(new SeparatorMenuItem());

		Menu menuMeasurements = new Menu("Measurements");
		menuMeasurements.getItems().addAll(measurementsNone, measurementsDistance, measurementsAngle, measurementsDihedral, new SeparatorMenuItem(), measurementsRemoveAll);

		getItems().add(menuMeasurements);


		if (fxmol != null) {
			RadioMenuItem modePolarHydrogens = new RadioMenuItem("Display Polar Hydrogens Only");
			modePolarHydrogens.setSelected(fxmol.getHydrogenMode() == MoleculeArchitect.HydrogenMode.POLAR);
			modePolarHydrogens.setOnAction(e -> fxmol.setHydrogenMode(MoleculeArchitect.HydrogenMode.POLAR));
			
			RadioMenuItem modeAllHydrogens = new RadioMenuItem("Display All Hydrogens");
			modeAllHydrogens.setSelected(fxmol.getHydrogenMode() == MoleculeArchitect.HydrogenMode.ALL);
			modeAllHydrogens.setOnAction(e -> fxmol.setHydrogenMode(MoleculeArchitect.HydrogenMode.ALL));
			
			RadioMenuItem modeBallAndSticks = new RadioMenuItem("Ball And Sticks");
			modeBallAndSticks.setSelected(fxmol.getConstructionMode() == MoleculeArchitect.CONSTRUCTION_MODE_BALL_AND_STICKS);
			modeBallAndSticks.setOnAction(e -> fxmol.setConstructionMode(MoleculeArchitect.CONSTRUCTION_MODE_BALL_AND_STICKS));
			
			RadioMenuItem modeBalls = new RadioMenuItem("Balls");
			modeBalls.setSelected(fxmol.getConstructionMode() == MoleculeArchitect.CONSTRUCTION_MODE_BALLS);
			modeBalls.setOnAction(e -> fxmol.setConstructionMode(MoleculeArchitect.CONSTRUCTION_MODE_BALLS));
			
			RadioMenuItem modeSticks = new RadioMenuItem("Sticks");
			modeSticks.setSelected(fxmol.getConstructionMode() == MoleculeArchitect.CONSTRUCTION_MODE_STICKS);
			modeSticks.setOnAction(e -> fxmol.setConstructionMode(MoleculeArchitect.CONSTRUCTION_MODE_STICKS));
			
			RadioMenuItem modeWires = new RadioMenuItem("Wires");
			modeWires.setSelected(fxmol.getConstructionMode() == MoleculeArchitect.CONSTRUCTION_MODE_WIRES);
			modeWires.setOnAction(e -> fxmol.setConstructionMode(MoleculeArchitect.CONSTRUCTION_MODE_WIRES));
			
			Menu menuMode = new Menu("Molecule Style");
			menuMode.getItems().addAll(modePolarHydrogens, modeAllHydrogens, new SeparatorMenuItem(),
					 modeBallAndSticks, modeBalls, modeSticks, modeWires);

			getItems().add(menuMode);

			Menu menuColor = new Menu("Molecule Color");
			RadioMenuItem colorNone = new RadioMenuItem("By Atomic No");
			colorNone.setSelected(fxmol.getColor() == null);
			colorNone.setOnAction(e -> mMolecule.setColor(null));
			ColorPicker molColorPicker = new ColorPicker(fxmol.getColor());
			molColorPicker.setOnAction(t -> { fxmol.setColor(molColorPicker.getValue()); hide(); });
			CustomMenuItem colorExplicit = new CustomMenuItem(molColorPicker);
			colorExplicit.setHideOnClick(false);
			menuColor.getItems().addAll(colorNone, colorExplicit);
			getItems().add(menuColor);

			if (fxmol.getRole() == V3DMolecule.MoleculeRole.MACROMOLECULE) {
				RadioMenuItem modeNone = new RadioMenuItem("None");
				modeNone.setSelected(fxmol.getConstructionMode() == MoleculeArchitect.CONSTRUCTION_MODE_NONE);
				modeNone.setOnAction(e -> fxmol.setConstructionMode(MoleculeArchitect.CONSTRUCTION_MODE_NONE));
				menuMode.getItems().add(modeNone);

				RadioMenuItem ribbonModeCartoon = new RadioMenuItem("Cartoon");
				ribbonModeCartoon.setSelected(fxmol.getRibbonMode() == Ribbon.MODE_CARTOON);
				ribbonModeCartoon.setOnAction(e -> fxmol.setRibbonMode(Ribbon.MODE_CARTOON));

				RadioMenuItem ribbonModeRibbon = new RadioMenuItem("Ribbon");
				ribbonModeRibbon.setSelected(fxmol.getRibbonMode() == Ribbon.MODE_RIBBON);
				ribbonModeRibbon.setOnAction(e -> fxmol.setRibbonMode(Ribbon.MODE_RIBBON));

				RadioMenuItem ribbonModeNone = new RadioMenuItem("None");
				ribbonModeNone.setSelected(fxmol.getRibbonMode() == Ribbon.MODE_NONE);
				ribbonModeNone.setOnAction(e -> fxmol.setRibbonMode(Ribbon.MODE_NONE));

				Menu menuRibbonMode = new Menu("Show Backbone As");
				menuRibbonMode.getItems().addAll(ribbonModeCartoon, ribbonModeRibbon, ribbonModeNone);
				getItems().add(menuRibbonMode);
			}

			for (int i = 0; i<MoleculeSurfaceAlgorithm.SURFACE_TYPE.length; i++) {
				final int type = i;
				RadioMenuItem surfaceNone = new RadioMenuItem("None");
				surfaceNone.setSelected(fxmol.getSurfaceMode(type) == V3DMolecule.SurfaceMode.NONE);
				surfaceNone.setOnAction(e -> setSurfaceMode(fxmol, type, V3DMolecule.SurfaceMode.NONE));
				RadioMenuItem surfaceMesh = new RadioMenuItem("Triangles");
				surfaceMesh.setSelected(fxmol.getSurfaceMode(type) == V3DMolecule.SurfaceMode.WIRES);
				surfaceMesh.setOnAction(e -> setSurfaceMode(fxmol, type, V3DMolecule.SurfaceMode.WIRES));
				RadioMenuItem surfaceOpaque = new RadioMenuItem("Filled");
				surfaceOpaque.setSelected(fxmol.getSurfaceMode(type) == V3DMolecule.SurfaceMode.FILLED);
				surfaceOpaque.setOnAction(e -> setSurfaceMode(fxmol, type, V3DMolecule.SurfaceMode.FILLED));

				RadioMenuItem surfaceColorInherit = new RadioMenuItem("From Molecule");
				surfaceColorInherit.setSelected(fxmol.getSurfaceColorMode(type) == SurfaceMesh.SURFACE_COLOR_INHERIT);
				surfaceColorInherit.setOnAction(e -> fxmol.setSurfaceColorMode(type, SurfaceMesh.SURFACE_COLOR_INHERIT));
				RadioMenuItem surfaceColorPlain = new RadioMenuItem("Plain Color");
				surfaceColorPlain.setSelected(fxmol.getSurfaceColorMode(type) == SurfaceMesh.SURFACE_COLOR_PLAIN);
				surfaceColorPlain.setOnAction(e -> fxmol.setSurfaceColorMode(type, SurfaceMesh.SURFACE_COLOR_PLAIN));
				RadioMenuItem surfaceColorAtoms = new RadioMenuItem("By Polarity");
				surfaceColorAtoms.setSelected(fxmol.getSurfaceColorMode(type) == SurfaceMesh.SURFACE_COLOR_POLARITY);
				surfaceColorAtoms.setOnAction(e -> fxmol.setSurfaceColorMode(type, SurfaceMesh.SURFACE_COLOR_POLARITY));
				RadioMenuItem surfaceColorNegativity = new RadioMenuItem("By Donors & Acceptors");
				surfaceColorNegativity.setSelected(fxmol.getSurfaceColorMode(type) == SurfaceMesh.SURFACE_COLOR_DONORS_ACCEPTORS);
				surfaceColorNegativity.setOnAction(e -> fxmol.setSurfaceColorMode(type, SurfaceMesh.SURFACE_COLOR_DONORS_ACCEPTORS));
				RadioMenuItem surfaceColorByAtomicNo = new RadioMenuItem("By Atomic-No");
				surfaceColorByAtomicNo.setSelected(fxmol.getSurfaceColorMode(type) == SurfaceMesh.SURFACE_COLOR_ATOMIC_NOS);
				surfaceColorByAtomicNo.setOnAction(e -> fxmol.setSurfaceColorMode(type, SurfaceMesh.SURFACE_COLOR_ATOMIC_NOS));

				ColorPicker colorPicker = new ColorPicker(fxmol.getSurfaceColor(type));
				colorPicker.setOnAction(t -> { fxmol.setSurfaceColor(type, colorPicker.getValue()); hide(); });
				CustomMenuItem colorItem = new CustomMenuItem(colorPicker);
				colorItem.setHideOnClick(false);

				Menu menuSurfaceColor = new Menu("Surface Color");
				menuSurfaceColor.getItems().addAll(surfaceColorInherit, surfaceColorPlain, surfaceColorAtoms, surfaceColorNegativity, surfaceColorByAtomicNo, colorItem);

				Slider sliderTransparency = createSlider(0.0, 0.9, fxmol.getSurfaceTransparency(type));
				sliderTransparency.valueProperty().addListener(newValue -> fxmol.setSurfaceTransparency(type, ((DoubleProperty) newValue).doubleValue()));

				VBox transparencyPane = new VBox();
				transparencyPane.setPadding(new Insets(4, 4, 4, 4));
				transparencyPane.getChildren().addAll(sliderTransparency);
				CustomMenuItem transparencyItem = new CustomMenuItem(transparencyPane, false);
				Menu menuTransparency = new Menu("Transparency");
				menuTransparency.getItems().addAll(transparencyItem);

				Menu menuSurfaceCutter = new Menu("Active Surface Cutter");
				RadioMenuItem cutterNone = new RadioMenuItem("None");
				cutterNone.setSelected(scene.getSurfaceCutMode() == PolygonSurfaceCutter.SURFACE_CUT_NONE);
				cutterNone.setOnAction(e -> scene.activateSurfaceCutter(PolygonSurfaceCutter.SURFACE_CUT_NONE, null));
				RadioMenuItem cutterInside = new RadioMenuItem("Inside Selection");
				cutterInside.setSelected(scene.getSurfaceCutMode() == PolygonSurfaceCutter.SURFACE_CUT_INSIDE);
				cutterInside.setOnAction(e -> scene.activateSurfaceCutter(PolygonSurfaceCutter.SURFACE_CUT_INSIDE, fxmol));
				RadioMenuItem cutterOutside = new RadioMenuItem("Outside Selection");
				cutterOutside.setSelected(scene.getSurfaceCutMode() == PolygonSurfaceCutter.SURFACE_CUT_OUTSIDE);
				cutterOutside.setOnAction(e -> scene.activateSurfaceCutter(PolygonSurfaceCutter.SURFACE_CUT_OUTSIDE, fxmol));
				menuSurfaceCutter.getItems().addAll(cutterNone, cutterInside, cutterOutside);

				MenuItem itemRemoveInvisible = new MenuItem("Remove Invisible Part");
				itemRemoveInvisible.setOnAction(e -> {
					float nearClip = (float) (scene.getCamera().getTranslateZ() + scene.getCamera().getNearClip());
					float farClip = (float) (scene.getCamera().getTranslateZ() + scene.getCamera().getFarClip());
					fxmol.cutSurface(type, new ClipSurfaceCutter(nearClip, farClip, fxmol));
				} );

				Menu menuSurface = new Menu(MoleculeSurfaceAlgorithm.SURFACE_TYPE[type]+" Surface");
				menuSurface.getItems().addAll(surfaceNone, surfaceMesh, surfaceOpaque, new SeparatorMenuItem(),
						menuSurfaceColor, menuTransparency, new SeparatorMenuItem(), menuSurfaceCutter,
						new SeparatorMenuItem(), itemRemoveInvisible);
				getItems().add(menuSurface);
			}

			getItems().add(new SeparatorMenuItem());
			if (settings == null || !settings.contains(V3DScene.ViewerSettings.SIDEPANEL)) {
				MenuItem itemHide = new MenuItem("Hide Molecule");
				itemHide.setOnAction(e -> fxmol.setVisible(false));
				getItems().add(itemHide);
			}

			if (settings.contains(V3DScene.ViewerSettings.ATOM_INDEXES)) {
				if (fxmol.hasAtomIndexLabels()) {
					MenuItem itemAI = new MenuItem("Remove Atom Indexes");
					itemAI.setOnAction(e -> fxmol.removeAtomIndexLabels());
					getItems().add(itemAI);
				}
				else {
					MenuItem itemAI = new MenuItem("Add Atom Indexes");
					itemAI.setOnAction(e -> fxmol.addAtomIndexLabels(mScene.getWorld()));
					getItems().add(itemAI);
				}
			}

			if (!settings.contains(V3DScene.ViewerSettings.UPPERPANEL)
			 && (settings.contains(V3DScene.ViewerSettings.EDITING) || settings.contains(V3DScene.ViewerSettings.ALLOW_PHARMACOPHORES))) {
				MenuItem itemPP = new MenuItem("Add Pharmacophores");
				itemPP.setOnAction(e -> fxmol.addPharmacophore());
				getItems().add(itemPP);
			}
			if (!settings.contains(V3DScene.ViewerSettings.UPPERPANEL)) {
				RadioMenuItem itemTS = new RadioMenuItem("Visualize Torsion Strain");
				itemTS.setSelected(fxmol.getTorsionStrainVis()!=null && fxmol.getTorsionStrainVis().isVisible());
				itemTS.setOnAction(e -> {
					TorsionStrainVisualization torsionStrainVis = fxmol.getTorsionStrainVis();
					if(torsionStrainVis==null)
						fxmol.addTorsionStrainVisualization();
					else 
						torsionStrainVis.toggleVisibility();
				});
				getItems().add(itemTS);
			}

			/*
			MenuItem itemHidePP = new MenuItem("Hide Pharmacophore");
			itemHidePP.setDisable(fxmol.getPharmacophore()==null || !fxmol.getPharmacophore().isVisible());
			itemHidePP.setOnAction(e -> fxmol.getPharmacophore().setVisible(false));
			getItems().add(itemHidePP);
			MenuItem itemShowPP = new MenuItem("Show Pharmacophore");
			itemShowPP.setDisable(fxmol.getPharmacophore()==null || fxmol.getPharmacophore().isVisible());
			itemShowPP.setOnAction(e -> fxmol.getPharmacophore().setVisible(true));
			getItems().add(itemShowPP);
			MenuItem itemES = new MenuItem("Add ExclusionSphere");
			itemES.setDisable(fxmol.getPharmacophore()==null);
			itemES.setOnAction(e -> fxmol.getPharmacophore().placeExclusionSphere(VolumeGaussian.EXCLUSION));
			getItems().add(itemES);
			
			MenuItem itemIS = new MenuItem("Add InclusionSphere");
			itemIS.setDisable(fxmol.getPharmacophore()==null);
			itemIS.setOnAction(e -> fxmol.getPharmacophore().placeExclusionSphere(VolumeGaussian.INCLUSION));
			getItems().add(itemIS);
			getItems().add(new SeparatorMenuItem());
			*/
		}

		
		if (settings == null || !settings.contains(V3DScene.ViewerSettings.SIDEPANEL)) {
			MenuItem itemHideAll = new MenuItem("Hide All Molecules");
			itemHideAll.setOnAction(e -> scene.setAllVisible(false));
			getItems().add(itemHideAll);

			MenuItem itemShowAll = new MenuItem("Show All Molecules");
			itemShowAll.setOnAction(e -> scene.setAllVisible(true));
			getItems().add(itemShowAll);
			getItems().add(new SeparatorMenuItem());
		}

		final double[] zrange = scene.getVisibleZRange();
		zrange[0] -= scene.getCamera().getTranslateZ();
		zrange[1] -= scene.getCamera().getTranslateZ();

		final double slider1Min = clipValueToSlider(zrange[0]);
		final double slider1Max = clipValueToSlider(zrange[1]);
		final double slider2Min = clipValueToSlider(V3DScene.CAMERA_MIN_CLIP_THICKNESS);
		final double slider2Max = clipValueToSlider(zrange[1] - zrange[0]);

		Slider slider1 = createSlider(slider1Min, slider1Max, Math.min(slider1Max, Math.max(slider1Min, clipValueToSlider(scene.getCamera().nearClipProperty().get()))));
		slider1.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				double nearClip = scene.getCamera().nearClipProperty().getValue();
				double farClip = scene.getCamera().farClipProperty().getValue();

				if (newValue.doubleValue() == slider1Min && farClip == V3DScene.CAMERA_FAR_CLIP) {
					scene.getCamera().nearClipProperty().setValue(V3DScene.CAMERA_NEAR_CLIP);
				}
				else {
					double newClipStart = sliderToClipValue(newValue.doubleValue());
					scene.getCamera().nearClipProperty().setValue(newClipStart);

					if (farClip != V3DScene.CAMERA_FAR_CLIP) {
						double thickness = farClip - nearClip;
						double newFarClip = Math.min(V3DScene.CAMERA_FAR_CLIP, newClipStart + thickness);
						scene.getCamera().farClipProperty().setValue(newFarClip);
					}
				}
			}
		});

		double oldThickness = scene.getCamera().farClipProperty().get()
				- scene.getCamera().nearClipProperty().get();

		Slider slider2 = createSlider(slider2Min, slider2Max, Math.max(slider2Min, Math.min(slider2Max, clipValueToSlider(oldThickness))));
		slider2.valueProperty().addListener(newValue -> {
			double slider = ((DoubleProperty)newValue).doubleValue();
			if (slider == slider2Max) {
				scene.getCamera().farClipProperty().setValue(V3DScene.CAMERA_FAR_CLIP);
			}
			else {
				double newThickness = sliderToClipValue(slider);
				double clipStart = scene.getCamera().nearClipProperty().getValue();
				if (clipStart < zrange[0]) {
					clipStart = zrange[0];
					scene.getCamera().nearClipProperty().setValue(clipStart);
					}
				double newFarClip = Math.min(V3DScene.CAMERA_FAR_CLIP, clipStart + newThickness);
				scene.getCamera().farClipProperty().setValue(newFarClip);
			}
		});

		Slider slider3 = createSlider(0.0, 1.0, scene.getDepthCueingIntensity());
		slider3.valueProperty().addListener(newValue ->
			scene.setDepthCueingIntensity(((DoubleProperty)newValue).doubleValue()));

		CheckMenuItem useWheelForClipping = new CheckMenuItem("Use Mouse Wheel For Clipping");
		useWheelForClipping.setSelected(sUseMouseWheelForClipping);
		useWheelForClipping.setOnAction(e -> sUseMouseWheelForClipping = useWheelForClipping.isSelected());

		VBox sliderPane1 = new VBox();
		sliderPane1.setPadding(new Insets(4, 4, 4, 4));
		sliderPane1.setSpacing(8);
		sliderPane1.getChildren().addAll(new Label("Front of Visible Region"), slider1, new Label("Depth Of Visible Region"), slider2);
		CustomMenuItem sliderItem1 = new CustomMenuItem(sliderPane1, false);

		Menu menuClippingPlanes = new Menu("Clipping Planes");

		VBox sliderPane2 = new VBox();
		sliderPane2.setPadding(new Insets(16, 4, 4, 4));
		sliderPane2.setSpacing(8);
		sliderPane2.getChildren().addAll(new Label("Depth Cueing Intensity"), slider3);
		CustomMenuItem sliderItem2 = new CustomMenuItem(sliderPane2, false);
		menuClippingPlanes.getItems().addAll(sliderItem1, useWheelForClipping, sliderItem2);
		getItems().add(menuClippingPlanes);
		
		if ((settings == null || settings.contains(V3DScene.ViewerSettings.EDITING)) && !settings.contains(V3DScene.ViewerSettings.UPPERPANEL)) {
			getItems().add(new SeparatorMenuItem());
			MenuItem itemMinimizeMol = new MenuItem("Of This Molecule");
			itemMinimizeMol.setOnAction(e -> {
				showMinimizerDialog(scene,null,fxmol);
			});
			itemMinimizeMol.setDisable(fxmol == null);
			MenuItem itemMinimizeScene = new MenuItem("Of Visible Scene");
			itemMinimizeScene.setOnAction(e -> {
				showMinimizerDialog(scene, null, null);
			});
			Menu menuMinimize = new Menu("Minimize Energy");
			menuMinimize.getItems().addAll(itemMinimizeMol, itemMinimizeScene);
			getItems().add(menuMinimize);
		}


		getItems().add(new SeparatorMenuItem());
		MenuItem itemRayTraceMol = new MenuItem("Of This Molecule...");
		itemRayTraceMol.setOnAction(e -> showMoleculeRayTraceDialog(scene));
		itemRayTraceMol.setDisable(fxmol == null);
		MenuItem itemRaytraceScene = new MenuItem("Of Entire Scene...");
		itemRaytraceScene.setOnAction(e -> showSceneRayTraceDialog(scene));
		Menu menuRaytrace = new Menu("Photo-Realistic Image");
		menuRaytrace.getItems().addAll(itemRayTraceMol, itemRaytraceScene);
		getItems().add(menuRaytrace);
	}

	private void setSurfaceMode(V3DMolecule fxmol, int type, V3DMolecule.SurfaceMode mode) {
		V3DPopupMenuController controller = mScene.getPopupMenuController();
		if (controller != null && mode != V3DMolecule.SurfaceMode.NONE)
			controller.markCropDistanceForSurface(fxmol, type, mode);
		fxmol.setSurfaceMode(type, mode);
		fxmol.getMolecule().removeAtomMarkers();
	}

	private double clipValueToSlider(double clipValue) {
		double en = Math.exp(CLIP_STEEPNESS);
		return Math.log(1.0 + (en - 1.0) * clipValue / MAX_CLIP) / CLIP_STEEPNESS;
	}

	private double sliderToClipValue(double sliderValue) {
		double en = Math.exp(CLIP_STEEPNESS);
		return MAX_CLIP * (Math.exp(sliderValue*CLIP_STEEPNESS) - 1.0) / (en - 1);
	}

/*	private double clipValueToSlider(double clipValue, double minClip, double maxClip) {
		double en = Math.exp(CLIP_STEEPNESS);
		double b = (maxClip - en * minClip) / (1.0 - en);
		double a = minClip - b;
//System.out.println("clipValueToSlider("+clipValue+") : "+(Math.log((clipValue - b) / a)));
		return Math.log((clipValue - b) / a);
	}

	private double sliderToClipValue(double sliderValue, double minClip, double maxClip) {
		double en = Math.exp(CLIP_STEEPNESS);
		double b = (maxClip - en * minClip) / (1.0 - en);
		double a = minClip - b;
//System.out.println("sliderToClipValue("+sliderValue+") : "+(a * Math.exp(sliderValue) + b));
		return a * Math.exp(sliderValue) + b;
	}*/

	private double clipThicknessToSlider(double clipThickness) {
		double en = Math.exp(CLIP_STEEPNESS);
		double b = MAX_CLIP / (1.0 - en);
		double a = -b;
//System.out.println("clipThicknessToSlider("+clipThickness+") : "+(Math.log((clipThickness - b) / a)));
		return Math.log((clipThickness - b) / a);
	}

	private double sliderToClipThickness(double sliderValue) {
		double en = Math.exp(CLIP_STEEPNESS);
		double b = MAX_CLIP / (1.0 - en);
		double a = -b;
//System.out.println("sliderToClipThickness("+sliderValue+") : "+(a * Math.exp(sliderValue) + b));
		return a * Math.exp(sliderValue) + b;
	}

	/**
	 * Displays a raytrace dialog for rendering one molecule
	 */
	private void showMoleculeRayTraceDialog(V3DScene scene) {
		Platform.runLater(() -> {
			Optional<RayTraceOptions> result = new RayTraceDialog(scene.getScene().getWindow(), sPreviousMoleculeRayTraceOptions, mMolecule).showAndWait();
			result.ifPresent(options -> {
				sPreviousMoleculeRayTraceOptions = options;
				double cameraX = scene.getCamera().getTranslateX();
				double cameraY = scene.getCamera().getTranslateY();
				double cameraZ = scene.getCamera().getTranslateZ();
				double fieldOfView = scene.getFieldOfView();
				options.rayTraceInit(cameraX, cameraY, cameraZ, fieldOfView);
				options.addMolecule(mMolecule);
				options.rayTraceStart(scene.getScene().getWindow());
			} );
		} );
	}

	public void showSceneRayTraceDialog(V3DScene scene) {
		Platform.runLater(() -> {
			Optional<RayTraceOptions> result = new RayTraceDialog(scene.getScene().getWindow(), sPreviousSceneRayTraceOptions, null).showAndWait();
			result.ifPresent(options -> {
				sPreviousSceneRayTraceOptions = options;
				double cameraX = scene.getCamera().getTranslateX();
				double cameraY = scene.getCamera().getTranslateY();
				double cameraZ = scene.getCamera().getTranslateZ();
				double fieldOfView = scene.getFieldOfView();
				if (((PerspectiveCamera)scene.getCamera()).isVerticalFieldOfView())
					fieldOfView *= scene.getWidth() / scene.getHeight();
				options.rayTraceInit(cameraX, cameraY, cameraZ, fieldOfView);

				for (Node node:scene.getWorld().getChildren())
					addToRaytraceScene(node, options);

				options.rayTraceStart(scene.getScene().getWindow());
			} );
		} );
	}

	private void addToRaytraceScene(Node node, RayTraceOptions options) {
		if (node.isVisible()) {
			if (node instanceof V3DMolecule)
				options.addMolecule((V3DMolecule)node);
			else
				options.addOther(node);

			if (node instanceof V3DRotatableGroup)
				for (Node n : ((V3DRotatableGroup)node).getChildren())
					addToRaytraceScene(n, options);
		}
	}

	private Slider createSlider(double min, double max, double value) {
		Slider slider = new Slider(min, max, value);
		slider.setPrefWidth(200);
		slider.setShowTickMarks(true);
		slider.setValue(value);
		return slider;
	}
	
	public static FileChooser getMoleculeFileChooser() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Molecule File");
		fileChooser.getExtensionFilters().add(new ExtensionFilter("SD Files", "*.sdf"));
		fileChooser.getExtensionFilters().add(new ExtensionFilter("DWAR Files", "*.dwar"));
		fileChooser.getExtensionFilters().add(new ExtensionFilter("Mol2 Files", "*.mol2"));
		fileChooser.getExtensionFilters().add(new ExtensionFilter("Mol Files", "*.mol"));
		fileChooser.getExtensionFilters().add(new ExtensionFilter("PDB Files", "*.pdb"));
		//pane.setPinnedSide(Side.RIGHT);
		
		return fileChooser;
	}
	
	public static void showMinimizerDialog(V3DScene scene3D, V3DSceneEditor editor, V3DMolecule fxmol) {
		Dialog<ButtonType> dialog = new Dialog<ButtonType>();
		ToggleGroup group = new ToggleGroup();
		 
		RadioButton buttonAllAtoms = new RadioButton("All Atoms");
		buttonAllAtoms.setToggleGroup(group);
		buttonAllAtoms.setSelected(true);
		
		BooleanProperty hydrogensOnly = new SimpleBooleanProperty(false);
		RadioButton buttonHydrogens = new RadioButton("Hydrogens Only");
		buttonHydrogens.setToggleGroup(group);
		
		buttonHydrogens.setOnAction(e -> {
			hydrogensOnly.set(buttonHydrogens.isSelected());});

		buttonAllAtoms.setOnAction(e -> {
			hydrogensOnly.set(!buttonAllAtoms.isSelected());});
		
		VBox content = new VBox();
		
		content.getChildren().addAll(buttonHydrogens,buttonAllAtoms);
		dialog.getDialogPane().setContent(content);
		ButtonType buttonTypeOk = new ButtonType("Run", ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().add(buttonTypeOk);
		Optional<ButtonType> type = dialog.showAndWait();
		if(type.get().equals(buttonTypeOk))
			V3DMinimizer.minimize(scene3D, editor, fxmol,hydrogensOnly.get());
		
	}
	
	/*
	private Dialog<IAlignmentTask> getAlignmentDialog(V3DScene scene) {

		
		BooleanProperty strucFromInput = new SimpleBooleanProperty(true);
		ObjectProperty<File> file = new SimpleObjectProperty<File>();
		Dialog<IAlignmentTask> dialog = new Dialog<>();
		dialog.setTitle("Alignment Specifications");
		dialog.setResizable(false);
		
		HBox hbox1 = new HBox();
		
		Label label = new Label("Align Molecules from : ");
		
		ToggleGroup toggleGroupInput = new ToggleGroup();
		RadioButton fromScene = new RadioButton("Selected in Scene");
		fromScene.setSelected(true);
		RadioButton fromFile = new RadioButton("From File");
		fromScene.setToggleGroup(toggleGroupInput);
		fromFile.setToggleGroup(toggleGroupInput);
		hbox1.getChildren().addAll(label, fromScene, fromFile);
		
		fromFile.setOnAction(e -> {
			file.set(getMoleculeFileChooser().showOpenDialog(scene.getScene().getWindow()));
		});
		
		fromScene.setOnAction(e -> {
			file.set(null);
		});
		
		HBox hbox2 = new HBox();
		
		Label label2 = new Label("Take 3D Structures from : ");
		
		ToggleGroup toggleGroupConf = new ToggleGroup();
		RadioButton fromInput = new RadioButton("Input Structure (if available)");
		fromInput.setSelected(true);
		RadioButton fromConfGen = new RadioButton("Generate Conformers and Identify Best Match");
		fromInput.setToggleGroup(toggleGroupConf);
		fromConfGen.setToggleGroup(toggleGroupConf);
		
		hbox2.getChildren().addAll(label2, fromInput, fromConfGen);
		
		fromInput.setOnAction(e -> {
			strucFromInput.set(true);
		});
		
		fromConfGen.setOnAction(e -> {
			strucFromInput.set(false);
		});
		
		VBox content = new VBox();
		content.getChildren().addAll(hbox1,hbox2);
		Label label3 = new Label("PharmacophoreWeight (from 0 to 1) : ");
		TextField field = new TextField("0.5");
		HBox hbox3 = new HBox();
		hbox3.getChildren().addAll(label3, field);
		content.getChildren().add(hbox3);
		dialog.getDialogPane().setContent(content);
		ButtonType buttonTypeOk = new ButtonType("Run", ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().add(buttonTypeOk);
		dialog.setResultConverter(new Callback<ButtonType, IAlignmentTask>() {
		    @Override
		    public IAlignmentTask call(ButtonType b) {
		    	Double ppWeight = Double.parseDouble(field.getText());
		    	IAlignmentTask task = null;
		        if (b == buttonTypeOk) {
		        	if(file.get()!=null) {
		        		List<PheSAMolecule> shapes = V3DMoleculeParser.readPhesaScreeningLib(file.get(), strucFromInput.get());
		        		task = new V3DShapeAlignerFromFile(mScene,mMolecule,shapes,ppWeight);
		        	}
		        	else {
		        		task = new V3DShapeAlignerInPlace(mScene,mMolecule,!strucFromInput.get(),ppWeight);
		        	}
		           
		        }
		        return task;
		        
		    }
		});
		
		return dialog;
	}
	*/
	

}

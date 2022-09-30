package org.openmolecules.fx.viewer3d;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openmolecules.fx.surface.SurfaceMesh;
import org.openmolecules.fx.tasks.V3DMinimizer;
import org.openmolecules.fx.viewer3d.V3DMolecule.SurfaceMode;
import org.openmolecules.fx.viewer3d.nodes.NodeDetail;
import org.openmolecules.render.MoleculeArchitect.ConstructionMode;
import org.openmolecules.render.MoleculeArchitect.HydrogenMode;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.alignment3d.transformation.TransformationSequence;
import com.actelion.research.chem.docking.DockingEngine;
import com.actelion.research.chem.docking.receptorpharmacophore.NegativeReceptorImageCreator;
import com.actelion.research.chem.io.pdb.converter.MoleculeGrid;
import com.actelion.research.chem.phesa.ShapeVolume;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;

/**
 * the binding site is a region in a protein in the vicinity of a native ligand, it is the focus
 * of many analyses and custom visualization and requires special treatment
 * @author wahljo1
 *
 */

public class V3DBindingSite {
	
	public static final double GRID_DIMENSION = 6.0;
	public static final double GRID_RESOLUTION = 0.5;
	public enum DisplayMode {MODE1, MODE2, MODE3, MODE4};
	
	private V3DMolecule nativeLigand;
	private V3DMolecule receptor;
	private MoleculeGrid grid;
	private Set<Integer> bindingSiteAtoms;
	private List<V3DMolecule> solventMols;
	private SimpleObjectProperty<DisplayMode> displayModeProperty;
	
	public V3DBindingSite() {
		bindingSiteAtoms = new HashSet<Integer>();
	}
	
	public void initialize() {
		nativeLigand.assignLikelyProtonationStates();
		nativeLigand.setConstructionMode(ConstructionMode.BALL_AND_STICKS);
		receptor.assignLikelyProtonationStates();
		receptor.setHydrogenMode(HydrogenMode.POLAR);
		grid = new MoleculeGrid(nativeLigand.getMolecule(),GRID_RESOLUTION,
				new Coordinates(GRID_DIMENSION,GRID_DIMENSION,
						GRID_DIMENSION));
		DockingEngine.getBindingSiteAtoms(receptor.getMolecule(), bindingSiteAtoms, grid, true);
		displayModeProperty = new SimpleObjectProperty<DisplayMode>();
		displayModeProperty.addListener((v,ov,nv) -> {
			Platform.runLater(() -> {
			customizeView();});
		});
		displayModeProperty.set(DisplayMode.MODE1);

		
	}
	
	public void optimizeHBondNetwork() {
		 V3DMinimizer.minimize(null, this, true);
	}
	
	private void customizeView() {
		switch(displayModeProperty.get()) {
		case MODE1:
			receptor.removeAllSurfaces();
			nativeLigand.setConstructionMode(ConstructionMode.BALL_AND_STICKS);
			receptor.setConstructionMode(ConstructionMode.WIRES);
			break;
		case MODE2:
			receptor.removeAllSurfaces();
			nativeLigand.setConstructionMode(ConstructionMode.STICKS);
			receptor.setConstructionMode(ConstructionMode.WIRES);
			break;
		case MODE3:
			receptor.removeAllSurfaces();
			nativeLigand.setConstructionMode(ConstructionMode.BALL_AND_STICKS);
			receptor.setConstructionMode(ConstructionMode.STICKS);
			break;
		case MODE4:
			receptor.removeAllSurfaces();
			nativeLigand.setConstructionMode(ConstructionMode.BALL_AND_STICKS);
			receptor.setConstructionMode(ConstructionMode.STICKS);
			receptor.setSurface(0, SurfaceMode.FILLED, SurfaceMesh.SURFACE_COLOR_ATOMIC_NOS, 0.5);
			break;
		default:
			receptor.removeAllSurfaces();
			nativeLigand.setConstructionMode(ConstructionMode.STICKS);
			receptor.setConstructionMode(ConstructionMode.WIRES);
			break;
		}

		for(Node node : receptor.getChildren()) {
			NodeDetail detail = (NodeDetail)node.getUserData();
			if (detail != null && detail.isAtom()) {
				int atom = detail.getAtom();
				if(!bindingSiteAtoms.contains(atom)) {
					node.setOpacity(0.0);
				}

			}
			else if(detail != null && detail.isBond()) {
				int b = detail.getBond();
				int a1 = receptor.getMolecule().getBondAtom(0, b);
				int a2 = receptor.getMolecule().getBondAtom(1, b);
				if(!bindingSiteAtoms.contains(a1) && !bindingSiteAtoms.contains(a2)) {
					node.setOpacity(0.0);
				}
			
			}
		}
	}
	
	public void addNegRecImg() {
		TransformationSequence sequence = new TransformationSequence();
		ShapeVolume bsVolume = NegativeReceptorImageCreator.create(nativeLigand.getMolecule(),receptor.getMolecule(),sequence);
		bsVolume.transform(sequence);
		receptor.addNegativeReceptorImage(bsVolume);
	}

	public V3DMolecule getNativeLigand() {
		return nativeLigand;
	}

	public void setNativeLigand(V3DMolecule nativeLigand) {
		this.nativeLigand = nativeLigand;
	}

	public V3DMolecule getReceptor() {
		return receptor;
	}

	public void setReceptor(V3DMolecule receptor) {
		this.receptor = receptor;
	}

	public List<V3DMolecule> getSolventMols() {
		return solventMols;
	}

	public void setSolventMols(List<V3DMolecule> solventMols) {
		this.solventMols = solventMols;
	}

	public Set<Integer> getBindingSiteAtoms() {
		return bindingSiteAtoms;
	};
	
	public void setDisplayMode(DisplayMode displayMode) {
		displayModeProperty.set(displayMode);
	}
	

}

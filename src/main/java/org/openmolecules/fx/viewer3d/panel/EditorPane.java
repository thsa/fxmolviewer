/*
 * Copyright 2017 Thomas Sander, Therwilerstrasse 41, CH-4153 Reinach, Switzerland
 *
 * This file is part of openmolecules.org's 3D-Molecule-Viewer.
 *
 * 3D-Molecule-Viewer is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * 3D-Molecule-Viewer is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with 3D-Molecule-Viewer.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Thomas Sander
 */

package org.openmolecules.fx.viewer3d.panel;



import javafx.scene.Node;
import javafx.fxml.FXMLLoader;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Scale;
import javafx.scene.shape.Rectangle;

import org.openmolecules.fx.viewer3d.GUIColorPalette;
import org.openmolecules.fx.viewer3d.V3DScene;
import org.openmolecules.fx.viewer3d.editor.actions.V3DAddAtomAction;
import org.openmolecules.fx.viewer3d.editor.actions.V3DAddFragmentAction;
import org.openmolecules.fx.viewer3d.editor.actions.V3DDecreaseChargeAction;
import org.openmolecules.fx.viewer3d.editor.actions.V3DDeleteAction;
import org.openmolecules.fx.viewer3d.editor.actions.V3DDrawBondAction;
import org.openmolecules.fx.viewer3d.editor.actions.V3DEditorAction;
import org.openmolecules.fx.viewer3d.editor.actions.V3DIncreaseChargeAction;
import org.openmolecules.fx.viewer3d.editor.actions.V3DRotateBondAction;

import java.awt.Point;
import java.io.IOException;
//import org.openmolecules.fx.viewer3d.MoleculeMinimizer;


/**
 * Created by JW on 15.1.19
 */
public class EditorPane extends Pane  {
    static double IMAGE_HEIGHT = 285;
    static double IMAGE_WIDTH = 49;
    static double SCALE = 1.8;
    static int ROWS = 13;
    static int COLS = 2;
	private V3DScene mScene3D;
	private Rectangle mHighlightedAction;
	private V3DEditorAction[][] mActions;

	//private CheckBox mCheckBoxPin;


	public EditorPane(final V3DScene scene3D) {
		super();
		mScene3D = scene3D;
		try {
			Node canvas = FXMLLoader.load(EditorPane.class.getResource("/DrawButtonsV2.fxml"));
			canvas.setStyle("-fx-background-color:transparent; -fx-background-radius:0;");
			Scale scale = new Scale();
			scale.setX(SCALE);
            scale.setY(SCALE);
            canvas.getTransforms().addAll(scale); 
			getChildren().add(canvas);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		setStyle("-fx-background-color:" + GUIColorPalette.BLUE3 + "; " +  "-fx-opacity: 0.5;");
		setPrefSize(SCALE*IMAGE_WIDTH, SCALE*IMAGE_HEIGHT);
		setupHandlers();
		setOnMousePressed(me -> {
			if(me.getButton()==MouseButton.PRIMARY) {
				double x = me.getX();
		        double y = me.getY();
		        if (x >= 0 && x <= IMAGE_WIDTH * SCALE && y >= 0 && y < IMAGE_HEIGHT * SCALE) {
		            double dy = IMAGE_HEIGHT * SCALE / ROWS;
		            double dx = IMAGE_WIDTH * SCALE / COLS;
		            int col = (int) (x / dx);
		            int row = (int) (y / dy);
		            setAction(row, col);
				
			}
		}});

		
	}

	public V3DScene getV3DScene() {
		return mScene3D;
	}
	
	 private void setupHandlers()
	    {
	        mActions = new V3DEditorAction[][]
	            {
	                {
	                    null,
	                    new V3DRotateBondAction(),
	                },
	                {
	                    null,
	                    null
	                },

	                {
	                    new V3DDeleteAction(),
	                    new V3DDrawBondAction(),
	                },
	                {
	                	new V3DAddFragmentAction(V3DAddFragmentAction.CYCLOPROPYL,false),
	                	new V3DAddFragmentAction(V3DAddFragmentAction.CYCLOBUTYL,false)
	                },
	                {
	                    new V3DAddFragmentAction(V3DAddFragmentAction.CYCLOPENTYL,false),
	                    new V3DAddFragmentAction(V3DAddFragmentAction.CYCLOHEXYL,false)
	                },
	                {
	                    new V3DAddFragmentAction(V3DAddFragmentAction.CYCLOHEPTYL,false),
	                    new V3DAddFragmentAction(V3DAddFragmentAction.PHENYL,true)
	                },
	                {
	                    new V3DIncreaseChargeAction(),
	                    new V3DDecreaseChargeAction(),
	                },
	                {
	                    new V3DAddAtomAction(6),
	                    new V3DAddAtomAction(14)
	                },
	                {
	                	new V3DAddAtomAction(7),
	                	new V3DAddAtomAction(15)
	                },
	                {
	                	new V3DAddAtomAction(8),
	                	new V3DAddAtomAction(16),
	                },
	                {
	                	new V3DAddAtomAction(9),
	                	new V3DAddAtomAction(17),
	                },
	                {
	                	new V3DAddAtomAction(35),
	                	new V3DAddAtomAction(53),
	                },
	                {
	                	new V3DAddAtomAction(1),
	                	null
	                },
	            };

	    }
	  
	    public void setAction(int row, int col)
	    {
	        if (mActions[row][col] != null) {
	        	V3DEditorAction currentAction = mActions[row][col];
	        	getChildren().remove(mHighlightedAction);
	        	if(currentAction==mScene3D.getEditor().getAction()) {
	        		mScene3D.getEditor().setAction(null);
	        	}
	        	else {
	        		mScene3D.getEditor().setAction(currentAction);
	        		highlightSelection(currentAction);
	        	}
	            

	    }
	    }
	    
	    public Point getActionPosition(V3DEditorAction a)
	    {
	        for (int row = 0; row < ROWS; row++) {
	            for (int col = 0; col < COLS; col++) {
	                if (mActions[row][col] == a) {
	                    return new Point(col, row);
	                }
	            }
	        }
	        return null;
	    }
	    
	    private void highlightSelection(V3DEditorAction a)
	    {

	        if (a!= null) {
	            double dx = IMAGE_WIDTH / COLS * SCALE;
	            double dy = IMAGE_HEIGHT / ROWS * SCALE;
	            Point pt = getActionPosition(a);
	            int y = (int) (IMAGE_HEIGHT * SCALE / ROWS * pt.y);
	            int x = (int) (IMAGE_WIDTH * SCALE / COLS * pt.x);
	            mHighlightedAction = new Rectangle(x, y, (int) (dx + 0.5), (int) (dy + 0.5));
	            mHighlightedAction .setStyle("-fx-fill:rgba(129,129,129,.5);");
	            getChildren().add(mHighlightedAction);
	        }
	    }

	

	/*
	private ArrayList<StereoMolecule> parseSDFile(String sdfile) {
		ArrayList<StereoMolecule> mols = new ArrayList<StereoMolecule>();
		SDFileParser sdfp = new SDFileParser(sdfile);
		boolean notDone = sdfp.next();
		while(notDone) {
			try {
				mols.add(sdfp.getMolecule());
				notDone = sdfp.next();
			}
			catch(Exception e) {
				notDone = false;
			}
		}
		return mols;
	}


	private V3DMolecule[] readMolFile(String sdfile) {
		ArrayList<StereoMolecule> mols = parseSDFile(sdfile);
		V3DMolecule[] v3d_mols = new V3DMolecule[mols.size()];
		int i = 0;
		for(StereoMolecule mol: mols) {
			v3d_mols[i] = new V3DMolecule(mol);
			i++;

		}
		return v3d_mols;
	}
	*/
}

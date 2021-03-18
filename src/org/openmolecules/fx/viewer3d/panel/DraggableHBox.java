package org.openmolecules.fx.viewer3d.panel;

import org.openmolecules.fx.viewer3d.GUIColorPalette;
import org.openmolecules.fx.viewer3d.V3DSceneWithSelection;
import org.openmolecules.fx.viewer3d.V3DSceneWithSidePane;

import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;


public class DraggableHBox {
	
	private Node content;
	private Pane button = new Pane();
	private BorderPane box;
	private static final double BUTTON_WIDTH = 8.0;
	private double x = 0;
    // mouse position
    private double mousex = 0;
    private boolean dragging = false;


	
	public DraggableHBox(Node content) {
		this.content = content;
		box = new BorderPane();
		box.setCenter(content);
		box.setPrefWidth( BUTTON_WIDTH + 0.5*V3DSceneWithSidePane.SIDEPANEL_WIDTH);
		button.setPrefWidth( BUTTON_WIDTH);
		button.setMaxWidth( BUTTON_WIDTH);
		button.setStyle("-fx-background-color:" + GUIColorPalette.BLUE3 + ";" +  
				" -fx-text-fill:white;" + 
				" -fx-opacity: 0.5;" + 
				" -fx-background-radius:0;" + 
				" -fx-border-width:0;");
		box.setRight(button);
		button.prefHeightProperty().bind(box.heightProperty());
		init();

	}
	
	
	
	public BorderPane getBox() {
		return box;
	}
	
	 private void init() {

	        button.onMousePressedProperty().set(new EventHandler<MouseEvent>() {
	            @Override
	            public void handle(MouseEvent event) {
	                // record the current mouse X and Y position on Node
	                mousex = event.getSceneX();

	                x = box.getWidth();

	            }
	        });

	        //Event Listener for MouseDragged
	        button.onMouseDraggedProperty().set(new EventHandler<MouseEvent>() {
	            @Override
	            public void handle(MouseEvent event) {
	            	button.setCursor(Cursor.H_RESIZE);
	                // Get the exact moved X and Y
	                double offsetX = event.getSceneX() - mousex;
	                x += offsetX;
	                double scaledX = x;
	                box.setPrefWidth(Math.min(scaledX,BUTTON_WIDTH + V3DSceneWithSidePane.SIDEPANEL_WIDTH));
	                dragging = true;
	                // again set current Mouse x AND y position
	                mousex = event.getSceneX();


	                event.consume();
	            }
	        });

	        button.onMouseClickedProperty().set(new EventHandler<MouseEvent>() {
	            @Override
	            public void handle(MouseEvent event) {
	                dragging = false;
	            }
	        });

	    }

	    /**
	     * @return the dragging
	     */
	    protected boolean isDragging() {
	        return dragging;
	    }
	

}

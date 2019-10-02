package org.openmolecules.fx.viewer3d.panel;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class SlidingHBox {
	
	private Node mContent;
	private Button mButton = new Button(":");
	private BorderPane mBox;
	private static final int sButtonWidth = 10;
	private static final int sContentWidth = 300;
	private static final Rectangle mClip = new Rectangle();
	private Timeline mIn;
	private Timeline mOut;
	private BooleanProperty mIsExpanded = new SimpleBooleanProperty(true);
	
	public SlidingHBox(Node content) {
		mContent = content;
		mBox = new BorderPane();
		mBox.setCenter(mContent);
		mBox.setPrefWidth(sButtonWidth + sContentWidth);
		mButton.setPrefWidth(sButtonWidth);
		mBox.setRight(mButton);
		mButton.prefHeightProperty().bind(mBox.heightProperty());
		mButton.setOnAction(e -> togglePaneVisibility());
		setAnimation();
		
		mIsExpanded.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> paramObservableValue,Boolean paramT1, Boolean paramT2) {
				updateRectangle();
				if(paramT2){
					// To expand
					mOut.play();
				}else{
					// To close
					mIn.play();

				}
			}
		});
	}
	
	private void updateRectangle() {
		mClip.setHeight(mBox.getHeight());
	}
	
	private void setAnimation() {
        mOut = new Timeline();
        mIn = new Timeline();
        
        /* Animation for scroll down. */
        mOut.setCycleCount(1);
        mOut.setAutoReverse(true);
		final KeyValue kvOut1 = new KeyValue(mClip.widthProperty(), sButtonWidth + sContentWidth);
		final KeyValue kvOut2 = new KeyValue(mClip.translateXProperty(), 0);
		final KeyValue kvOut3 = new KeyValue(mBox.translateXProperty(), 0);
		final KeyFrame kfOut = new KeyFrame(Duration.millis(200),  kvOut1, kvOut2, kvOut3);
		mOut.getKeyFrames().add(kfOut);
		
		/* Animation for scroll up. */
		mIn.setCycleCount(1); 
		mIn.setAutoReverse(true);
		final KeyValue kvIn1 = new KeyValue(mClip.widthProperty(), sButtonWidth);
		final KeyValue kvIn2 = new KeyValue(mClip.translateXProperty(), sContentWidth);
		final KeyValue kvIn3 = new KeyValue(mBox.translateXProperty(), -sContentWidth+sButtonWidth);
		final KeyFrame kfIn = new KeyFrame(Duration.millis(200), kvIn1, kvIn2, kvIn3);
		mIn.getKeyFrames().add(kfIn);
		
	}
	
	private void togglePaneVisibility(){
		if(mIsExpanded.get()){
			mIsExpanded.set(false);
		}else{
			mIsExpanded.set(true);
		}
	}
	
	public BorderPane getBox() {
		return mBox;
	}
	

}

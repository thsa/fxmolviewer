package org.openmolecules.render;

import javafx.scene.Node;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.paint.Color;

/**
 * helper class to create histograms in JFX
 * uses a LineChart 
 * @author wahljo1
 *
 */

public class Histogram extends LineChart<Number,Number> {
	private Color color;

	public Histogram(Axis<Number> xAxis, Axis<Number> yAxis, Color color) {
		super(xAxis, yAxis);
		this.color = color;
	}
	
	public Histogram(Axis<Number> xAxis, Axis<Number> yAxis) {
		this(xAxis, yAxis, Color.ORANGE);
	}
	
	public void populate(Number[] xValues, Number[] yValues) {
		for(int i=0;i<yValues.length;i++) {
			 XYChart.Series<Number,Number> series = new XYChart.Series<Number,Number>();
			 series.getData().add(new XYChart.Data<Number,Number>(xValues[i], 0));
	    	 series.getData().add(new XYChart.Data<Number,Number>(xValues[i],yValues[i]));  
	    	 this.getData().addAll(series);
	    	 Node line = series.getNode().lookup(".chart-series-line");
	    	 String rgb = String.format("%d, %d, %d",
	    	         (int) (color.getRed() * 255),
	    	         (int) (color.getGreen() * 255),
	    	         (int) (color.getBlue() * 255));
	    	 line.setStyle("-fx-stroke: rgba(" + rgb + ", 1.0);");
		}
	}
	
	public void addMarker(double height, Number markerPosition, Color color) {
		 XYChart.Series series2 = new XYChart.Series();
		 series2.getData().add(new XYChart.Data(markerPosition, -height));
		 series2.getData().add(new XYChart.Data(markerPosition,height));  
		 this.getData().addAll(series2);
		 Color color2 = Color.RED; // or any other color
		 Node line2 = series2.getNode().lookup(".chart-series-line");
		 String rgb2 = String.format("%d, %d, %d",
   	         (int) (color2.getRed() * 255),
   	         (int) (color2.getGreen() * 255),
   	         (int) (color2.getBlue() * 255));
		 line2.setStyle("-fx-stroke: rgba(" + rgb2 + ", 1.0);"+ "-fx-stroke-width: 7px;");
		
	}
	
	

}

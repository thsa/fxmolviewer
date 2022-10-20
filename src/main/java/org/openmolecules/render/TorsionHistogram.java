package org.openmolecules.render;

import javafx.scene.Node;
import javafx.scene.chart.Axis.TickMark;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.paint.Color;

public class TorsionHistogram {
	
	private static final int STEPS = 5;
	private static final int MAX = 127;
	
	public static LineChart<Number,Number> create(byte[] distribution, double angle) {

		final NumberAxis xAxis = new NumberAxis();
		final NumberAxis yAxis = new NumberAxis();
		final Histogram histo = new Histogram(xAxis,yAxis);      
		histo.setCreateSymbols(false);
		histo.legendVisibleProperty().set(false);
		yAxis.setAutoRanging(false);
		xAxis.setAutoRanging(false);
		yAxis.setTickLabelsVisible(false);
		yAxis.setUpperBound(MAX);
		yAxis.setLowerBound(-5);
		xAxis.setUpperBound(365);
		xAxis.setLowerBound(-5);
		xAxis.setTickUnit(10);
		
		angle = angle*360.0/(2*Math.PI); //convert angle to degrees
	    if(angle<0.0)
	    	angle+=360.0 ;
	    Integer[] xValues = new Integer[distribution.length];
	    Integer[] yValues = new Integer[distribution.length];
		for(int i=0;i<distribution.length;i++) {
			 xValues[i] = i*STEPS;
			 yValues[i] = Integer.valueOf(distribution[i]);
		}
		histo.populate(xValues, yValues);
		int angleIndex = (int)angle/STEPS;
		histo.addMarker(5.0, angleIndex*STEPS, Color.RED);
		return histo;
           
	     

	}

}

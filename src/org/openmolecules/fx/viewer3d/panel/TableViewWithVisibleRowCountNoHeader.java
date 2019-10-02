package org.openmolecules.fx.viewer3d.panel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;

import com.sun.javafx.scene.control.skin.TableViewSkin;
import com.sun.javafx.scene.control.skin.VirtualFlow;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.Skin;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;

/**
 * TableView with visibleRowCountProperty.
 * 
 * @author Jeanette Winzenburg, Berlin
 */
public class TableViewWithVisibleRowCountNoHeader<T> extends TableView<T> {

	
	
	private IntegerProperty visibleRowCount = new SimpleIntegerProperty(this, "visibleRowCount", 0);


    public IntegerProperty visibleRowCountProperty() {
        return visibleRowCount;
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new TableViewSkinX<T>(this);
    }
    
    
    @Override
    public void resize(double width, double height) {
        super.resize(width, height);
        Pane header = (Pane) lookup("TableHeaderRow");
        header.setMinHeight(0);
        header.setPrefHeight(0);
        header.setMaxHeight(0);
        header.setVisible(false);
    }
    
    public IntegerProperty getVisibleRowCount() {
    	return visibleRowCount;
    }
    

    /**
     * Skin that respects table's visibleRowCount property.
     */
    public static class TableViewSkinX<T> extends TableViewSkin<T> {

        public TableViewSkinX(TableViewWithVisibleRowCountNoHeader<T> tableView) {
            super(tableView);
            registerChangeListener(tableView.visibleRowCountProperty(), "VISIBLE_ROW_COUNT");
            handleControlPropertyChanged("VISIBLE_ROW_COUNT");
        }

        @Override
        protected void handleControlPropertyChanged(String p) {
            super.handleControlPropertyChanged(p);
            if ("VISIBLE_ROW_COUNT".equals(p)) {
                needCellsReconfigured = true;
                getSkinnable().requestFocus();
            }
        }

        /**
         * Returns the visibleRowCount value of the table.
         */
        private int getVisibleRowCount() {
            return ((TableViewWithVisibleRowCountNoHeader<T>) getSkinnable()).visibleRowCountProperty().get();
        }

        /**
         * Calculates and returns the pref height of the 
         * for the given number of rows.
         * 
         * If flow is of type MyFlow, queries the flow directly
         * otherwise invokes the method.
         */
        protected double getFlowPrefHeight(int rows) {
            double height = 0;
            if (flow instanceof MyFlow) {
                height = ((MyFlow) flow).getPrefLength(rows);
            }
            else {
                for (int i = 0; i < rows && i < getItemCount(); i++) {
                    height += invokeFlowCellLength(i);
                }
            }    
            return height + snappedTopInset() + snappedBottomInset();

        }

        /**
         * Overridden to compute the sum of the flow height and header prefHeight.
         */
        @Override
        protected double computePrefHeight(double width, double topInset,
                double rightInset, double bottomInset, double leftInset) {
            // super hard-codes to 400 .. doooh
            double prefHeight = getFlowPrefHeight(getVisibleRowCount());
            return prefHeight; 
        }

        /**
         * Reflectively invokes protected getCellLength(i) of flow.
         * @param index the index of the cell.
         * @return the cell height of the cell at index.
         */
        protected double invokeFlowCellLength(int index) {
            double height = 1.0;
            Class<?> clazz = VirtualFlow.class;
            try {
                Method method = clazz.getDeclaredMethod("getCellLength", Integer.TYPE);
                method.setAccessible(true);
                return ((double) method.invoke(flow, index));
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                e.printStackTrace();
            }
            return height;
        }

        /**
         * Overridden to return custom flow.
         */
        @Override
        protected VirtualFlow createVirtualFlow() {
            return new MyFlow();
        }

        /**
         * Extended to expose length calculation per a given # of rows.
         */
        public static class MyFlow extends VirtualFlow {

            protected double getPrefLength(int rowsPerPage) {
                double sum = 0.0;
                int rows = rowsPerPage; //Math.min(rowsPerPage, getCellCount());
                for (int i = 0; i < rows; i++) {
                    sum += getCellLength(i);
                }
                return sum;
            }
 


        }
        
        

    }

    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(TableViewWithVisibleRowCountNoHeader.class
            .getName());
}
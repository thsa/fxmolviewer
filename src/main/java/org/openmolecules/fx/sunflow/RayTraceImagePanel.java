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

package org.openmolecules.fx.sunflow;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.sunflow.core.Display;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class RayTraceImagePanel extends Canvas implements ClipboardOwner,Display {
	private static final String FORMAT_JPEG = "jpeg";   // ImageIO format names
	private static final String FORMAT_PNG = "png";
	private static final int[] BORDERS = { 0xFF0000, 0x00FF00, 0x0000FF, 0xFFFF00, 0x00FFFF, 0xFF00FF };

	private static File cInitialDirectory;

	private WritableImage image;
	private double xo, yo;
	private double width, height;
	private volatile boolean doRepaint;
	private ContextMenu popup;
	private String imageName;

	private class ScrollZoomHandler {
		double mx;
		double my;

		public ScrollZoomHandler(Canvas canvas) {
			canvas.setOnMousePressed(me -> mousePressed(me) );
			canvas.setOnMouseDragged(me -> mouseDragged(me) );
			canvas.setOnMouseReleased(me -> mouseDragged(me) );
			canvas.setOnScroll(me -> zoom(-me.getDeltaY()) );
		}

		private void mousePressed(javafx.scene.input.MouseEvent me) {
			if (popup != null)
				popup.hide();

			mx = me.getX();
			my = me.getY();
			if (me.getButton() == MouseButton.MIDDLE) {
				reset();
			}
			else if (me.getButton() == MouseButton.SECONDARY) {
				createPopupMenu(me.getScreenX(), me.getScreenY());
			}
		}

		private void mouseDragged(javafx.scene.input.MouseEvent me) {
			double mx2 = me.getX();
			double my2 = me.getY();
			drag(mx2 - mx, my2 - my);
			mx = mx2;
			my = my2;
		}
	}

	public RayTraceImagePanel(int w, int h) {
		super(w, h);
		imageName = "Untitled";
		image = null;
		xo = yo = 0;
		widthProperty().addListener((observable, oldValue, newValue) -> repaint() );
		heightProperty().addListener((observable, oldValue, newValue) -> repaint() );
		new ScrollZoomHandler(this);
	}

	public void setImageName(String imageName) {
		if (imageName != null && imageName.length() != 0)
			this.imageName = imageName;
	}

	private void copyImage() {
		WritableImage bi = getImage();
		if (bi != null) {
			TransferableImage trans = new TransferableImage( SwingFXUtils.fromFXImage(bi, null) );
			Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
			c.setContents(trans,  this);
		}
	}

	private void saveImage(String format) {
		WritableImage image = getImage();
		if (image != null) {
			String extension = format.equals(FORMAT_PNG) ? ".png" : ".jpeg";

			FileChooser fc = new FileChooser();
			fc.setTitle("Save Image As");
			fc.setInitialFileName(imageName+extension);

			FileChooser.ExtensionFilter filter = format.equals(FORMAT_PNG)  ? new FileChooser.ExtensionFilter("PNG", "*.png")
																			: new FileChooser.ExtensionFilter("JPEG", "*.jpg", "*.jpeg");
			if (cInitialDirectory == null)
				fc.setInitialDirectory(new File(System.getProperty("user.home")));
			else
				fc.setInitialDirectory(cInitialDirectory);

			fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("All Images", "*.*"), filter);
//				fc.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("JPEG", "*.jpg", "*.jpeg"));
			File file = fc.showSaveDialog(getScene().getWindow());
			if (file != null) {
				cInitialDirectory = file.getParentFile();

				// there is a bug in FX with alpha and jpeg (JI-9021748). Instead of the following we need to rewrite the image
				// RenderedImage ri = SwingFXUtils.fromFXImage(image, null);

				BufferedImage ri = new BufferedImage((int) image.getWidth(), (int) image.getHeight(), BufferedImage.OPAQUE);
				ri.createGraphics().drawImage(SwingFXUtils.fromFXImage(image, null), 0, 0, null);

				try {
					if (format.equals(FORMAT_PNG)) {
						ImageIO.write(ri, "png", file);
					}
					else if (format.equals(FORMAT_JPEG)) {
//	    				ImageIO.write(ri, "jpeg", file);

						ImageOutputStream ios = ImageIO.createImageOutputStream(file);
						ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
						ImageWriteParam iwp = writer.getDefaultWriteParam();
						iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
						iwp.setCompressionQuality(1.0f);
						writer.setOutput(ios);
						writer.write(null, new IIOImage(ri, null, null), iwp);
						writer.dispose();
						}
					}
				catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		}
	}

	private void createPopupMenu(double x, double y) {
		popup = new ContextMenu();
		popup.setHideOnEscape(true);
		javafx.scene.control.MenuItem item1 = new javafx.scene.control.MenuItem("Copy Image");
		item1.setOnAction(e -> copyImage());
		popup.getItems().add(item1);
		popup.getItems().add(new SeparatorMenuItem());
		javafx.scene.control.MenuItem item2 = new javafx.scene.control.MenuItem("Save As JPEG...");
		item2.setOnAction(e -> saveImage(FORMAT_JPEG));
		popup.getItems().add(item2);
		javafx.scene.control.MenuItem item4 = new javafx.scene.control.MenuItem("Save As PNG...");
		item4.setOnAction(e -> saveImage(FORMAT_PNG));
		popup.getItems().add(item4);
		popup.show(this, x, y);
	}

	public void save(String filename, String format) {
		try {
			ImageIO.write(SwingFXUtils.fromFXImage(image, null), format, new File(filename));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private synchronized void drag(double dx, double dy) {
		xo += dx;
		yo += dy;
		repaint();
	}

	private synchronized void zoom(double d) {
		if (d == 0)
			return;
		// window center
		double cx = getWidth() * 0.5;
		double cy = getHeight() * 0.5;

		// origin of the image in window space
		double x = xo + (getWidth() - width) * 0.5;
		double y = yo + (getHeight() - height) * 0.5;

		// coordinates of the pixel we are over
		double sx = cx - x;
		double sy = cy - y;

		// scale
		if (width + d > 100) {
			height = (width + d) * height / width;
			sx = (width + d) * sx / width;
			sy = (width + d) * sy / width;
			width = (width + d);
		}

		// restore center pixel

		double x2 = cx - sx;
		double y2 = cy - sy;

		xo = (x2 - (getWidth() - width) * 0.5);
		yo = (y2 - (getHeight() - height) * 0.5);

		repaint();
	}

	public synchronized void reset() {
		xo = yo = 0;
		if (image != null) {
			width = image.getWidth();
			height = image.getHeight();
		}
		repaint();
	}

	public synchronized void imageBegin(int w, int h, int bucketSize) {
		if (image != null && w == image.getWidth() && h == image.getHeight()) {
			// dull image if it has same resolution (75%)
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					int rgba = image.getPixelReader().getArgb(x, y);
					image.getPixelWriter().setArgb(x, y, ((rgba & 0xFEFEFEFE) >>> 1) + ((rgba & 0xFCFCFCFC) >>> 2));
				}
			}
		} else {
			// allocate new framebuffer
			image = new WritableImage(w, h);
			// center
			this.width = w;
			this.height = h;
			xo = yo = 0;
		}
		repaint();
	}

	public synchronized void imagePrepare(int x, int y, int w, int h, int id) {
		int border = BORDERS[id % BORDERS.length] | 0xFF000000;
		for (int by = 0; by < h; by++) {
			for (int bx = 0; bx < w; bx++) {
				if (bx == 0 || bx == w - 1) {
					if (5 * by < h || 5 * (h - by - 1) < h)
						image.getPixelWriter().setArgb(x + bx, y + by, border);
				} else if (by == 0 || by == h - 1) {
					if (5 * bx < w || 5 * (w - bx - 1) < w)
						image.getPixelWriter().setArgb(x + bx, y + by, border);
				}
			}
		}
		repaint();
	}

	public synchronized void imageUpdate(int x, int y, int w, int h, org.sunflow.image.Color[] data, float[] alpha) {
		for (int j = 0, index = 0; j < h; j++)
			for (int i = 0; i < w; i++, index++)
				image.getPixelWriter().setArgb(x + i, y + j, data[index].copy().mul(1.0f / alpha[index]).toNonLinear().toRGBA(alpha[index]));
		repaint();
	}

	public synchronized void imageFill(int x, int y, int w, int h, org.sunflow.image.Color c, float alpha) {
		int rgba = c.copy().mul(1.0f / alpha).toNonLinear().toRGBA(alpha);
		for (int j = 0, index = 0; j < h; j++)
			for (int i = 0; i < w; i++, index++)
				image.getPixelWriter().setArgb(x + i, y + j, rgba);
		repaint();
	}

	public void imageEnd() {
		repaint();
	}

	public boolean imageCancelled() {
		return false;
	}

	public WritableImage getImage() {
		return image;
	}

	private void repaint() {
		if (!doRepaint) {
			doRepaint = true;
			Platform.runLater(() -> paint());
			}
		}

	private void paint() {
		doRepaint = false;

		GraphicsContext gc = getGraphicsContext2D();
		if (image == null)
			return;

		gc.setFill(Color.WHITE);
		gc.fillRect(0, 0, getWidth(), getHeight());

		int x = (int)Math.round(xo + (getWidth() - width) * 0.5);
		int y = (int)Math.round(yo + (getHeight() - height) * 0.5);
		int iw = (int)Math.round(width);
		int ih = (int)Math.round(height);
		gc.drawImage(image, x, y, iw, ih);
	}

	public void lostOwnership(Clipboard clip, Transferable trans) {
//	        System.out.println( "Lost Clipboard Ownership" );
	}

	private class TransferableImage implements Transferable {
		Image mImage;

		public TransferableImage(Image image) {
			this.mImage = image;
		}

		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			if ( flavor.equals(DataFlavor.imageFlavor ) && mImage != null) {
				return mImage;
			}
			else {
				throw new UnsupportedFlavorException(flavor);
			}
		}

		public DataFlavor[] getTransferDataFlavors() {
			DataFlavor[] flavors = new DataFlavor[1];
			flavors[0] = DataFlavor.imageFlavor;
			return flavors;
		}

		public boolean isDataFlavorSupported( DataFlavor flavor) {
			DataFlavor[] flavors = getTransferDataFlavors();
			for (int i=0; i<flavors.length; i++) {
				if (flavor.equals(flavors[i])) {
					return true;
				}
			}

			return false;
		}
	}
}
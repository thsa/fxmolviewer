package org.sunflow.core.display;

import com.actelion.research.gui.FileHelper;
import org.sunflow.SunflowAPI;
import org.sunflow.core.Display;
import org.sunflow.image.Color;
import org.sunflow.system.ImagePanel;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class FrameDisplay implements Display {
    private String filename;
    private RenderFrame frame;

    public FrameDisplay() {
        this(null);
    }

    public FrameDisplay(String filename) {
        this.filename = filename;
        frame = null;
    }

	/**
	 * Had to encapsulate imageBegin in invokeAndWait(), because the substance731 LAF
	 * threw Exceptions because of GUI manipulations not in EDT. TLS 12Dec2015
	 * @param w width of the rendered image in pixels
	 * @param h height of the rendered image in pixels
	 * @param bucketSize size of the buckets in pixels
	 */
	public void imageBegin(final int w, final int h, final int bucketSize) {
		if (SwingUtilities.isEventDispatchThread()) {
			_imageBegin(w, h, bucketSize);
		}
		else { try { SwingUtilities.invokeAndWait(new Runnable() {
				@Override public void run() { _imageBegin(w, h, bucketSize); }
			} ); } catch (Exception e) {}
		}
	}

	private void _imageBegin(int w, int h, int bucketSize) {
		if (frame == null) {
			frame = new RenderFrame();
			frame.imagePanel.imageBegin(w, h, bucketSize);
			Dimension screenRes = Toolkit.getDefaultToolkit().getScreenSize();
			boolean needFit = false;
			if (w >= (screenRes.getWidth() - 200) || h >= (screenRes.getHeight() - 200)) {
				frame.imagePanel.setPreferredSize(new Dimension((int) screenRes.getWidth() - 200, (int) screenRes.getHeight() - 200));
				needFit = true;
			} else
				frame.imagePanel.setPreferredSize(new Dimension(w, h));
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
			if (needFit)
				frame.imagePanel.fit();
		} else
			frame.imagePanel.imageBegin(w, h, bucketSize);
	}

	public void imagePrepare(int x, int y, int w, int h, int id) {
		frame.imagePanel.imagePrepare(x, y, w, h, id);
	}

	public void imageUpdate(int x, int y, int w, int h, Color[] data, float[] alpha) {
		frame.imagePanel.imageUpdate(x, y, w, h, data, alpha);
	}

	public void imageFill(int x, int y, int w, int h, Color c, float alpha) {
		frame.imagePanel.imageFill(x, y, w, h, c, alpha);
	}

	public void imageEnd() {
		frame.imagePanel.imageEnd();
		if (filename != null)
			frame.imagePanel.save(filename);
	}

	@Override
	public boolean imageCancelled() {
		return frame.isClosed();
	}

	@SuppressWarnings("serial")
    private static class RenderFrame extends JFrame implements ActionListener,ClipboardOwner {
        ImagePanel imagePanel;
		boolean frameIsClosed;

        RenderFrame() {
            super("Molecule Rendered By Sunflow v" + SunflowAPI.VERSION);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	        addWindowListener(new WindowAdapter() {
		        @Override
		        public void windowClosed(WindowEvent e) {
			        super.windowClosed(e);
			        frameIsClosed = true;
		        }
	        });
            imagePanel = new ImagePanel();
            imagePanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                	handlePopupTrigger(e);
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                	handlePopupTrigger(e);
                }
            });
            setContentPane(imagePanel);
            pack();
        }

		public boolean isClosed() {
			return frameIsClosed;
		}

		private boolean handlePopupTrigger(MouseEvent e) {
			if (e.isPopupTrigger()) {
				JPopupMenu popup = new JPopupMenu();
				JMenuItem item1 = new JMenuItem("Copy Image");
				item1.addActionListener(this);
				popup.add(item1);
				JMenuItem item2 = new JMenuItem("Save As JPEG...");
				item2.addActionListener(this);
				popup.add(item2);
				JMenuItem item3 = new JMenuItem("Save As PNG...");
				item3.addActionListener(this);
				popup.add(item3);
				popup.show(this, e.getX(), e.getY());
				return true;
		    	}
			return false;
			}
	
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("Copy Image")) {
				BufferedImage bi = imagePanel.getImage();
				if (bi != null) {
		            TransferableImage trans = new TransferableImage( bi );
		            Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
		            c.setContents( trans, this );
		            }
	    		return;
				}
			if (e.getActionCommand().equals("Save As JPEG...")) {
				BufferedImage bi = imagePanel.getImage();
				if (bi != null)
					saveImage(bi, FileHelper.cFileTypeJPG);
	    		return;
				}
			if (e.getActionCommand().equals("Save As PNG...")) {
				BufferedImage bi = imagePanel.getImage();
				if (bi != null)
					saveImage(bi, FileHelper.cFileTypePNG);
				return;
				}
			}

	    private void saveImage(BufferedImage bi, int format) {
		    try {
			    String filename = new FileHelper(this).selectFileToSave("Save Image As", FileHelper.cFileTypeJPG, "Untitled");
			    if (filename != null) {
				    if (format == FileHelper.cFileTypeJPG) {
					    ImageOutputStream ios = ImageIO.createImageOutputStream(new File(filename));
					    ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
					    ImageWriteParam iwp = writer.getDefaultWriteParam();
					    iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
					    iwp.setCompressionQuality(0.9f);
					    writer.setOutput(ios);
					    writer.write(null, new IIOImage(bi, null, null), iwp);
					    writer.dispose();
				        }
				    else if (format == FileHelper.cFileTypePNG) {
					    ImageIO.write(bi, "png", new File(filename));
				        }
			        }
		        }
			catch (IOException ioe) {}
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
	}
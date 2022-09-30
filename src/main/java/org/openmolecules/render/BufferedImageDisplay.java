package org.openmolecules.render;

import org.sunflow.SunflowAPI;
import org.sunflow.core.Display;
import org.sunflow.image.Color;
import org.sunflow.system.Timer;

import java.awt.image.BufferedImage;

public class BufferedImageDisplay implements Display {
    private BufferedImage image;
    private int[] pixels;
    private Timer t;
    private float seconds;
    private int frames;

    public BufferedImageDisplay() {
        image = null;
        t = new Timer();
        frames = 0;
        seconds = 0;
    }

    public synchronized void imageBegin(int w, int h, int bucketSize) {
        if (image != null && w == image.getWidth() && h == image.getHeight()) {
            // nothing to do
        } else {
            // allocate new framebuffer
            pixels = new int[w * h];
            image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        }
        // start counter
        t.start();
    }

    public void imagePrepare(int x, int y, int w, int h, int id) {
    }

    public void imageUpdate(int x, int y, int w, int h, Color[] data, float[] alpha) {
        int iw = image.getWidth();
        int off = x + iw * y;
        iw -= w;
        for (int j = 0, index = 0; j < h; j++, off += iw)
            for (int i = 0; i < w; i++, index++, off++)
                pixels[off] = 0xFF000000 | data[index].toRGB();
    }

    public void imageFill(int x, int y, int w, int h, Color c, float alpha) {
        int iw = image.getWidth();
        int off = x + iw * y;
        iw -= w;
        int rgb = 0xFF000000 | c.toRGB();
        for (int j=0; j < h; j++, off += iw)
            for (int i = 0; i < w; i++, off++)
                pixels[off] = rgb;
    }

    public synchronized void imageEnd() {
        // copy buffer
        image.setRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());

        // update stats
        t.end();
        seconds += t.seconds();
        frames++;
        if (seconds > 1) {
            // display average fps every second
        	System.out.println(String.format("Sunflow v%s - %.2f fps", SunflowAPI.VERSION, frames / seconds));
            frames = 0;
            seconds = 0;
        	}
    	}

    public synchronized BufferedImage getImage() {
        return image;
    	}

    @Override
    public boolean imageCancelled() {
        return false;   // return true to stop all rendering threads
        }
	}
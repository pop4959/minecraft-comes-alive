package net.mca.client.resources;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

import static java.awt.Color.RGBtoHSB;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

public class ByteImage {
    private final static int BANDS = 4;
    private final byte[] bytes;
    private final int width, height;

    public ByteImage(byte[] bytes, int width, int height) {
        this.bytes = bytes;
        this.width = width;
        this.height = height;
    }

    public ByteImage(int width, int height) {
        this.bytes = new byte[width * height * BANDS];
        this.width = width;
        this.height = height;
    }

    public static ByteImage read(InputStream stream) throws IOException {
        BufferedImage image = ImageIO.read(stream);

        if (image == null) {
            throw new IOException("Invalid file");
        }

        ByteImage byteImage = new ByteImage(image.getWidth(), image.getHeight());

        int[] data = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int d = data[y * image.getWidth() + x];
                byteImage.setPixel(x, y,
                        (d >> 16) & 0xFF,
                        (d >> 8) & 0xFF,
                        d & 0xFF,
                        (d >> 24) & 0xFF
                );
            }
        }

        return byteImage;
    }

    public static ByteImage read(byte[] bytes) throws IOException {
        return read(new ByteArrayInputStream(bytes));
    }

    public BufferedImage toBufferedImage() {
        BufferedImage bufferedImage = new BufferedImage(width, height, TYPE_INT_ARGB);

        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                bufferedImage.setRGB(x, y, getARGB(x, y));
            }
        }

        return bufferedImage;
    }

    public void write(File file) {
        try {
            ImageIO.write(toBufferedImage(), "png", file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] encode() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            ImageIO.write(toBufferedImage(), "png", stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return stream.toByteArray();
    }

    public void setPixel(int x, int y, int r, int g, int b, int a) {
        setPixel(x, y, (byte)r, (byte)g, (byte)b, (byte)a);
    }

    public void setPixel(int x, int y, byte r, byte g, byte b, byte a) {
        int i = getIndex(x, y);
        bytes[i] = r;
        bytes[i + 1] = g;
        bytes[i + 2] = b;
        bytes[i + 3] = a;
    }

    public int getIndex(int x, int y) {
        return (x * height + y) * BANDS;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getARGB(int x, int y) {
        int index = getIndex(x, y);
        return (bytes[index + 2] & 0xFF) | ((bytes[index + 1] & 0xFF) << 8) | ((bytes[index] & 0xFF) << 16) | ((bytes[index + 3] & 0xFF) << 24);
    }

    public int getABGR(int x, int y) {
        int index = getIndex(x, y);
        return (bytes[index] & 0xFF) | ((bytes[index + 1] & 0xFF) << 8) | ((bytes[index + 2] & 0xFF) << 16) | ((bytes[index + 3] & 0xFF) << 24);
    }

    public int getRed(int x, int y) {
        return bytes[getIndex(x, y)];
    }

    public int getGreen(int x, int y) {
        return bytes[getIndex(x, y) + 1];
    }

    public int getBlue(int x, int y) {
        return bytes[getIndex(x, y) + 2];
    }

    public int getAlpha(int x, int y) {
        return bytes[getIndex(x, y) + 3];
    }
}

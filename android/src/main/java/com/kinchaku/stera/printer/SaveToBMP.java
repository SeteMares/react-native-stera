package com.kinchaku.stera.printer;

import android.graphics.Bitmap;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Android Bitmap Object to .bmp image (Windows BMP v3 24bit) file util class
 * <p>
 * ref : http://en.wikipedia.org/wiki/BMP_file_format
 *
 * @author ultrakain ( ultrasonic@gmail.com )
 * @since 2012-09-27
 */
public class SaveToBMP {

    private final int BMP_WIDTH_OF_TIMES = 4;

    /**
     * Android Bitmap Object to Window's v3 24bit Bmp Format File
     *
     * @return file saved result
     */
    public boolean save(Bitmap orgBitmap, String filePath) {

        if (orgBitmap == null) {
            return false;
        }

        if (filePath == null) {
            return false;
        }

        boolean isSaveSuccess = true;

        //image size
        int width = orgBitmap.getWidth();
        int height = orgBitmap.getHeight();

        //image dummy data size
        //reason : bmp file's width equals 4's multiple
        int dummySize = 0;
        byte[] dummyBytesPerRow = null;
        boolean hasDummy = false;
        int BYTE_PER_PIXEL = 3;
        if (isBmpWidth4Times(width)) {
            hasDummy = true;
            dummySize = BMP_WIDTH_OF_TIMES - (width % BMP_WIDTH_OF_TIMES);
            dummyBytesPerRow = new byte[dummySize * BYTE_PER_PIXEL];
            Arrays.fill(dummyBytesPerRow, (byte) 0xFF);
        }

        int[] pixels = new int[width * height];
        int imageSize = pixels.length * BYTE_PER_PIXEL + (height * dummySize * BYTE_PER_PIXEL);
        int imageDataOffset = 0x36;
        int fileSize = imageSize + imageDataOffset;

        //Android Bitmap Image Data
        orgBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        //ByteArrayOutputStream baos = new ByteArrayOutputStream(fileSize);
        ByteBuffer buffer = ByteBuffer.allocate(fileSize);

        try {
			/*
			  BITMAP FILE HEADER Write Start
			 */
            buffer.put((byte) 0x42);
            buffer.put((byte) 0x4D);

            //size
            buffer.put(writeInt(fileSize));

            //reserved
            buffer.put(writeShort((short) 0));
            buffer.put(writeShort((short) 0));

            //image data start offset
            buffer.put(writeInt(imageDataOffset));

            /* BITMAP FILE HEADER Write End */

            //*******************************************

            /* BITMAP INFO HEADER Write Start */
            //size
            buffer.put(writeInt(0x28));
            buffer.put(writeInt(width));
            buffer.put(writeInt(height));
            buffer.put(writeShort((short) 1));
            buffer.put(writeShort((short) 24));
            buffer.put(writeInt(0));
            buffer.put(writeInt(imageSize));
            buffer.put(writeInt(0));
            buffer.put(writeInt(0));
            buffer.put(writeInt(0));
            buffer.put(writeInt(0));

            // BITMAP INFO HEADER Write End

            int row = height;
            int startPosition;
            int endPosition;

            while (row > 0) {

                startPosition = (row - 1) * width;
                endPosition = row * width;

                for (int i = startPosition; i < endPosition; i++) {
                    buffer.put(write24BitForPixcel(pixels[i]));

                    if (hasDummy) {
                        if (isBitmapWidthLastPixcel(width, i)) {
                            buffer.put(dummyBytesPerRow);
                        }
                    }
                }
                row--;
            }

            FileOutputStream fos = new FileOutputStream(filePath);
            fos.write(buffer.array());
            fos.close();

        } catch (IOException e1) {
            e1.printStackTrace();
            isSaveSuccess = false;
        }

        return isSaveSuccess;
    }

    /**
     * Is last pixel in Android Bitmap width
     */
    private boolean isBitmapWidthLastPixcel(int width, int i) {
        return i > 0 && (i % (width - 1)) == 0;
    }

    /**
     * BMP file is a multiples of 4?
     */
    private boolean isBmpWidth4Times(int width) {
        return width % BMP_WIDTH_OF_TIMES > 0;
    }

    /**
     * Write integer to little-endian
     */
    private byte[] writeInt(int value) throws IOException {
        byte[] b = new byte[4];

        b[0] = (byte) (value & 0x000000FF);
        b[1] = (byte) ((value & 0x0000FF00) >> 8);
        b[2] = (byte) ((value & 0x00FF0000) >> 16);
        b[3] = (byte) ((value & 0xFF000000) >> 24);

        return b;
    }

    /**
     * Write integer pixel to little-endian byte array
     *
	 */
    private byte[] write24BitForPixcel(int value) throws IOException {
        byte[] b = new byte[3];

        b[0] = (byte) (value & 0x000000FF);
        b[1] = (byte) ((value & 0x0000FF00) >> 8);
        b[2] = (byte) ((value & 0x00FF0000) >> 16);

        return b;
    }

    /**
     * Write short to little-endian byte array
     *
	 */
    private byte[] writeShort(short value) throws IOException {
        byte[] b = new byte[2];

        b[0] = (byte) (value & 0x00FF);
        b[1] = (byte) ((value & 0xFF00) >> 8);

        return b;
    }

    public static String bytesToHexadecimalString(byte[] bytes) {
        StringBuilder imageHexString = new StringBuilder();
        for (byte aByte : bytes) {
            String hexString = Integer.toHexString(aByte & 0xFF);
            if (hexString.length() == 1) {
                hexString = "0" + hexString;
            }
            imageHexString.append(hexString);
        }
        return imageHexString.toString();
    }
}

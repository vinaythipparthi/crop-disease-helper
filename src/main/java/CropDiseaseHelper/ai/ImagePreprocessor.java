package CropDiseaseHelper.ai;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Component
public class ImagePreprocessor {

    private static final int IMAGE_SIZE = 224;
    private static final int RESIZE_SIZE = 256;

    private static final float[] MEAN = {
            0.485f, 0.456f, 0.406f
    };

    private static final float[] STD = {
            0.229f, 0.224f, 0.225f
    };

    public float[] preprocess(MultipartFile file) throws IOException {

        // 1. Read uploaded image
        BufferedImage originalImage =
                ImageIO.read(file.getInputStream());

        if (originalImage == null) {
            throw new IllegalArgumentException(
                    "Invalid or unsupported image file"
            );
        }

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // 2. Convert image to RGB
        BufferedImage rgbImage = new BufferedImage(
                originalWidth,
                originalHeight,
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D rgbGraphics = rgbImage.createGraphics();

        try {
            rgbGraphics.setRenderingHint(
                    RenderingHints.KEY_COLOR_RENDERING,
                    RenderingHints.VALUE_COLOR_RENDER_QUALITY
            );

            rgbGraphics.drawImage(originalImage, 0, 0, null);

        } finally {
            rgbGraphics.dispose();
        }

        // 3. Resize the shorter side to 256 pixels
        double scale = (double) RESIZE_SIZE
                / Math.min(originalWidth, originalHeight);

        int resizedWidth = (int) Math.round(originalWidth * scale);
        int resizedHeight = (int) Math.round(originalHeight * scale);

        BufferedImage resizedImage = new BufferedImage(
                resizedWidth,
                resizedHeight,
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D resizeGraphics = resizedImage.createGraphics();

        try {
            resizeGraphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR
            );

            resizeGraphics.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );

            resizeGraphics.drawImage(
                    rgbImage,
                    0,
                    0,
                    resizedWidth,
                    resizedHeight,
                    null
            );

        } finally {
            resizeGraphics.dispose();
        }

        // 4. Center crop to 224 x 224
        int startX = (resizedWidth - IMAGE_SIZE) / 2;
        int startY = (resizedHeight - IMAGE_SIZE) / 2;

        BufferedImage croppedImage = resizedImage.getSubimage(
                startX,
                startY,
                IMAGE_SIZE,
                IMAGE_SIZE
        );

        // 5. Convert pixels to normalized NCHW tensor
        int channelSize = IMAGE_SIZE * IMAGE_SIZE;

        float[] input = new float[3 * channelSize];

        for (int y = 0; y < IMAGE_SIZE; y++) {

            for (int x = 0; x < IMAGE_SIZE; x++) {

                int pixel = croppedImage.getRGB(x, y);

                float red = ((pixel >> 16) & 0xFF) / 255.0f;
                float green = ((pixel >> 8) & 0xFF) / 255.0f;
                float blue = (pixel & 0xFF) / 255.0f;

                int index = y * IMAGE_SIZE + x;

                // Red channel
                input[index] =
                        (red - MEAN[0]) / STD[0];

                // Green channel
                input[channelSize + index] =
                        (green - MEAN[1]) / STD[1];

                // Blue channel
                input[2 * channelSize + index] =
                        (blue - MEAN[2]) / STD[2];
            }
        }

        // 6. Validate tensor
        if (input.length != 150528) {
            throw new IllegalStateException(
                    "Unexpected tensor size: " + input.length
            );
        }

        for (float value : input) {
            if (!Float.isFinite(value)) {
                throw new IllegalStateException(
                        "Image tensor contains an invalid value"
                );
            }
        }

        // 7. Debug information
        System.out.println("\n========== IMAGE PREPROCESSING DEBUG ==========");
        System.out.println("Original width: " + originalWidth);
        System.out.println("Original height: " + originalHeight);
        System.out.println("Resized width: " + resizedWidth);
        System.out.println("Resized height: " + resizedHeight);
        System.out.println("Crop start X: " + startX);
        System.out.println("Crop start Y: " + startY);
        System.out.println("Final image size: 224 x 224");
        System.out.println("Tensor length: " + input.length);

        for (int i = 0; i < 10; i++) {
            System.out.println(
                    "Tensor[" + i + "] = " + input[i]
            );
        }

        System.out.println("===============================================\n");

        return input;
    }
}

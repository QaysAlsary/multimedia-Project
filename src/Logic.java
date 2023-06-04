import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class Logic {
    BufferedImage applyKMeans(BufferedImage image) {
        int k = 10; // Number of clusters
        int maxIterations = 100; // Maximum number of iterations

        int width = image.getWidth();
        int height = image.getHeight();

        // Initialize the centroid colors randomly
        java.util.List<Color> centroids = getRandomCentroids(k);

        // Create a copy of the original image
        BufferedImage quantizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Perform K-Means clustering
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            // Assign each pixel to the nearest centroid
            int[][] clusterAssignments = assignPixelsToClusters(image, centroids);

            // Update the centroid colors based on the assigned pixels
            java.util.List<Color> newCentroids = calculateNewCentroids(image, clusterAssignments, centroids);

            // Check for convergence
            if (centroids.equals(newCentroids)) {
                break;
            }

            centroids = newCentroids;
        }

        // Assign each pixel to its final centroid color
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = image.getRGB(x, y);
                int clusterIndex = getNearestClusterIndex(new Color(rgb), centroids);
                Color clusterColor = centroids.get(clusterIndex);
                quantizedImage.setRGB(x, y, clusterColor.getRGB());
            }
        }


        return quantizedImage;
    }
    private java.util.List<Color> getRandomCentroids(int k) {
        java.util.List<Color> centroids = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < k; i++) {
            int red = random.nextInt(256);
            int green = random.nextInt(256);
            int blue = random.nextInt(256);
            centroids.add(new Color(red, green, blue));
        }

        return centroids;
    }
    private int[][] assignPixelsToClusters(BufferedImage image, java.util.List<Color> centroids) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[][] clusterAssignments = new int[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = image.getRGB(x, y);
                Color pixelColor = new Color(rgb);
                int nearestClusterIndex = getNearestClusterIndex(pixelColor, centroids);
                clusterAssignments[x][y] = nearestClusterIndex;
            }
        }

        return clusterAssignments;
    }
    private int getNearestClusterIndex(Color color, java.util.List<Color> centroids) {
        int nearestIndex = 0;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < centroids.size(); i++) {
            Color centroidColor = centroids.get(i);
            double distance = calculateColorDistance(color, centroidColor);

            if (distance < minDistance) {
                minDistance = distance;
                nearestIndex = i;
            }
        }

        return nearestIndex;
    }
    private double calculateColorDistance(Color color1, Color color2) {
        int redDiff = color1.getRed() - color2.getRed();
        int greenDiff = color1.getGreen() - color2.getGreen();
        int blueDiff = color1.getBlue() - color2.getBlue();

        return Math.sqrt(redDiff * redDiff + greenDiff * greenDiff + blueDiff * blueDiff);
    }
    private java.util.List<Color> calculateNewCentroids(BufferedImage image, int[][] clusterAssignments, java.util.List<Color> centroids) {
        int k = centroids.size();
        int[] sumRed = new int[k];
        int[] sumGreen = new int[k];
        int[] sumBlue = new int[k];
        int[] count = new int[k];

        int width = image.getWidth();
        int height = image.getHeight();

        // Calculate the sums of RGB values for each cluster
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = image.getRGB(x, y);
                int clusterIndex = clusterAssignments[x][y];

                sumRed[clusterIndex] += new Color(rgb).getRed();
                sumGreen[clusterIndex] += new Color(rgb).getGreen();
                sumBlue[clusterIndex] += new Color(rgb).getBlue();
                count[clusterIndex]++;
            }
        }

        // Calculate the new centroid colors based on the sums and counts
        java.util.List<Color> newCentroids = new ArrayList<>();

        for (int i = 0; i < k; i++) {
            int red = count[i] > 0 ? sumRed[i] / count[i] : 0;
            int green = count[i] > 0 ? sumGreen[i] / count[i] : 0;
            int blue = count[i] > 0 ? sumBlue[i] / count[i] : 0;

            newCentroids.add(new Color(red, green, blue));
        }

        return newCentroids;
    }



    BufferedImage applyMedianCut(BufferedImage image) {
        int maxColors = 8; // Maximum number of colors (change as needed)

        int width = image.getWidth();
        int height = image.getHeight();

        // Create a flattened array of RGB values for all pixels
        int[] rgbValues = image.getRGB(0, 0, width, height, null, 0, width);

        // Collect all pixels into a list
        java.util.List<Color> pixels = new ArrayList<>();
        for (int rgb : rgbValues) {
            Color color = new Color(rgb);
            pixels.add(color);
        }

        // Perform median cut algorithm
        java.util.List<Color> colorPalette = medianCut(pixels, maxColors);

        // Replace each pixel with the closest color in the palette
        for (int i = 0; i < rgbValues.length; i++) {
            int rgb = rgbValues[i];
            Color pixelColor = new Color(rgb);
            Color closestColor = getClosestColor(pixelColor, colorPalette);
            rgbValues[i] = closestColor.getRGB();
        }

        // Create the output image
        BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        outputImage.setRGB(0, 0, width, height, rgbValues, 0, width);

        return outputImage;
    }

    private java.util.List<Color> medianCut(java.util.List<Color> pixels, int maxColors) {
        // Calculate the initial color cube containing all pixels
        ColorCube cube = new ColorCube(pixels);

        // Queue to store the color cubes
        java.util.List<ColorCube> cubeQueue = new ArrayList<>();
        cubeQueue.add(cube);

        // Perform median cut until the desired number of colors is reached
        while (cubeQueue.size() < maxColors) {
            // Find the cube with the maximum side length
            int maxCubeIndex = 0;
            int maxCubeSideLength = 0;
            for (int i = 0; i < cubeQueue.size(); i++) {
                ColorCube currentCube = cubeQueue.get(i);
                int currentSideLength = Math.max(currentCube.getRedRange(),
                        Math.max(currentCube.getGreenRange(), currentCube.getBlueRange()));
                if (currentSideLength > maxCubeSideLength) {
                    maxCubeIndex = i;
                    maxCubeSideLength = currentSideLength;
                }
            }

            // Split the max cube into two new cubes along the axis with the largest range
            ColorCube maxCube = cubeQueue.get(maxCubeIndex);
            ColorCube[] splitCubes = maxCube.splitCube();

            // Remove the max cube from the queue and add the split cubes
            cubeQueue.remove(maxCubeIndex);
            cubeQueue.add(splitCubes[0]);
            cubeQueue.add(splitCubes[1]);
        }

        // Collect the representative colors from the color cubes
        java.util.List<Color> colorPalette = new ArrayList<>();
        for (ColorCube colorCube : cubeQueue) {
            Color representativeColor = colorCube.getRepresentativeColor();
            colorPalette.add(representativeColor);
        }

        return colorPalette;
    }

    BufferedImage applyUniformQuantization(BufferedImage image) {
        int numLevels = 8; // Number of quantization levels (change as needed)

        int width = image.getWidth();
        int height = image.getHeight();

        // Create a flattened array of RGB values for all pixels
        int[] rgbValues = image.getRGB(0, 0, width, height, null, 0, width);

        // Perform uniform quantization on each color component
        int levelSize = 256 / numLevels;
        for (int i = 0; i < rgbValues.length; i++) {
            int rgb = rgbValues[i];
            int red = (rgb >> 16) & 0xFF;
            int green = (rgb >> 8) & 0xFF;
            int blue = rgb & 0xFF;

            int quantizedRed = (red / levelSize) * levelSize;
            int quantizedGreen = (green / levelSize) * levelSize;
            int quantizedBlue = (blue / levelSize) * levelSize;

            rgbValues[i] = (quantizedRed << 16) | (quantizedGreen << 8) | quantizedBlue;
        }

        // Create the output image
        BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        outputImage.setRGB(0, 0, width, height, rgbValues, 0, width);

        return outputImage;
    }

    java.util.List<Color> generatePalette(BufferedImage image) {
        // Get the unique colors from the quantized image
        java.util.List<Color> uniqueColors = getUniqueColors(image);

        // Sort the colors based on their occurrence frequency
        uniqueColors.sort((color1, color2) -> {
            int count1 = getColorCount(color1, image);
            int count2 = getColorCount(color2, image);
            return Integer.compare(count2, count1); // Sort in descending order
        });

        // Return the top 10 colors or fewer if there are less than 10 unique colors
        return uniqueColors.subList(0, Math.min(10, uniqueColors.size()));
    }


    public static int[] calculateColorHistogram(BufferedImage image) {
        // Initialize histogram array with 256 bins for each color channel (RGB)
        int[] histogram = new int[256 * 3];

        // Loop through each pixel of the image and increment the corresponding bin in the histogram
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                int red = (pixel >> 16) & 0xff; // Extract red channel
                int green = (pixel >> 8) & 0xff; // Extract green channel
                int blue = pixel & 0xff; // Extract blue channel

                // Increment the corresponding bin in the histogram for each color channel
                histogram[red]++;
                histogram[green + 256]++;
                histogram[blue + 512]++;
            }
        }

        return histogram;
    }

    private java.util.List<Color> getUniqueColors(BufferedImage image) {
        java.util.List<Color> uniqueColors = new ArrayList<>();
        int width = image.getWidth();
        int height = image.getHeight();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = image.getRGB(x, y);
                Color color = new Color(rgb);

                if (!uniqueColors.contains(color)) {
                    uniqueColors.add(color);
                }
            }
        }

        return uniqueColors;
    }

    private int getColorCount(Color color, BufferedImage image) {
        int count = 0;
        int width = image.getWidth();
        int height = image.getHeight();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = image.getRGB(x, y);
                Color pixelColor = new Color(rgb);

                if (color.equals(pixelColor)) {
                    count++;
                }
            }
        }

        return count;
    }




    private int getDistance(Color color1, Color color2) {
        int redDiff = color1.getRed() - color2.getRed();
        int greenDiff = color1.getGreen() - color2.getGreen();
        int blueDiff = color1.getBlue() - color2.getBlue();
        return redDiff * redDiff + greenDiff * greenDiff + blueDiff * blueDiff;
    }

    private Color getClosestColor(Color targetColor, java.util.List<Color> colorPalette) {
        Color closestColor = colorPalette.get(0);
        int minDistance = getDistance(targetColor, closestColor);
        for (Color color : colorPalette) {
            int distance = getDistance(targetColor, color);
            if (distance < minDistance) {
                minDistance = distance;
                closestColor = color;
            }
        }
        return closestColor;
    }

    private static class ColorCube {
        private int minRed;
        private int maxRed;
        private int minGreen;
        private int maxGreen;
        private int minBlue;
        private int maxBlue;
        private java.util.List<Color> pixels;

        public ColorCube(java.util.List<Color> pixels) {
            this.pixels = pixels;
            initializeRanges();
        }

        private void initializeRanges() {
            minRed = Integer.MAX_VALUE;
            maxRed = Integer.MIN_VALUE;
            minGreen = Integer.MAX_VALUE;
            maxGreen = Integer.MIN_VALUE;
            minBlue = Integer.MAX_VALUE;
            maxBlue = Integer.MIN_VALUE;

            for (Color color : pixels) {
                int red = color.getRed();
                int green = color.getGreen();
                int blue = color.getBlue();

                if (red < minRed) minRed = red;
                if (red > maxRed) maxRed = red;
                if (green < minGreen) minGreen = green;
                if (green > maxGreen) maxGreen = green;
                if (blue < minBlue) minBlue = blue;
                if (blue > maxBlue) maxBlue = blue;
            }
        }

        public int getRedRange() {
            return maxRed - minRed;
        }

        public int getGreenRange() {
            return maxGreen - minGreen;
        }

        public int getBlueRange() {
            return maxBlue - minBlue;
        }

        public ColorCube[] splitCube() {
            int redRange = getRedRange();
            int greenRange = getGreenRange();
            int blueRange = getBlueRange();

            if (redRange >= greenRange && redRange >= blueRange) {
                // Split along the red axis
                int splitValue = (minRed + maxRed) / 2;
                ColorCube cube1 = new ColorCube(filterPixels(splitValue, "red", true));
                ColorCube cube2 = new ColorCube(filterPixels(splitValue, "red", false));
                return new ColorCube[]{cube1, cube2};
            } else if (greenRange >= redRange && greenRange >= blueRange) {
                // Split along the green axis
                int splitValue = (minGreen + maxGreen) / 2;
                ColorCube cube1 = new ColorCube(filterPixels(splitValue, "green", true));
                ColorCube cube2 = new ColorCube(filterPixels(splitValue, "green", false));
                return new ColorCube[]{cube1, cube2};
            } else {
                // Split along the blue axis
                int splitValue = (minBlue + maxBlue) / 2;
                ColorCube cube1 = new ColorCube(filterPixels(splitValue, "blue", true));
                ColorCube cube2 = new ColorCube(filterPixels(splitValue, "blue", false));
                return new ColorCube[]{cube1, cube2};
            }
        }

        private java.util.List<Color> filterPixels(int splitValue, String axis, boolean lowerHalf) {
            List<Color> filteredPixels = new ArrayList<>();
            for (Color color : pixels) {
                int red = color.getRed();
                int green = color.getGreen();
                int blue = color.getBlue();

                if (axis.equals("red")) {
                    if (lowerHalf && red <= splitValue) {
                        filteredPixels.add(color);
                    } else if (!lowerHalf && red > splitValue) {
                        filteredPixels.add(color);
                    }
                } else if (axis.equals("green")) {
                    if (lowerHalf && green <= splitValue) {
                        filteredPixels.add(color);
                    } else if (!lowerHalf && green > splitValue) {
                        filteredPixels.add(color);
                    }
                } else if (axis.equals("blue")) {
                    if (lowerHalf && blue <= splitValue) {
                        filteredPixels.add(color);
                    } else if (!lowerHalf && blue > splitValue) {
                        filteredPixels.add(color);
                    }
                }
            }
            return filteredPixels;
        }

        public Color getRepresentativeColor() {
            int sumRed = 0;
            int sumGreen = 0;
            int sumBlue = 0;
            int count = pixels.size();
            for (Color color : pixels) {
                sumRed += color.getRed();
                sumGreen += color.getGreen();
                sumBlue += color.getBlue();
            }
            int avgRed = sumRed / count;
            int avgGreen = sumGreen / count;
            int avgBlue = sumBlue / count;
            return new Color(avgRed, avgGreen, avgBlue);
        }
    }
}

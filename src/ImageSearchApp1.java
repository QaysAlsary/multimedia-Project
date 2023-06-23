import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import javax.imageio.ImageIO;

public class ImageSearchApp1 extends JFrame {
    private JFrame frame;
    private JButton chooseImageButton;
    private JButton searchButton;
    JButton cropButton = new JButton("Crop and Search");

    private JTextField dateTextField;
    private JTextArea colorNamesTextArea;
    private JPanel[] resultPanels;

    public BufferedImage currentImage;
    private File selectedImageFile;
    private List<Color> userColorPalette;
    private List<ImageData> imageList;
    int MAX_COLORS=8;
    BufferedImage originalImage;


    private static final int MAX_DISPLAYED_IMAGES = 3;

    private class ImageData {
        private File imageFile;
        private List<Color> colorPalette;
        private int rank;

        public ImageData(File imageFile, List<Color> colorPalette) {
            this.imageFile = imageFile;
            this.colorPalette = colorPalette;
        }

        public File getImageFile() {
            return imageFile;
        }

        public List<Color> getColorPalette() {
            return colorPalette;
        }

        public int getRank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }
    }

    public ImageSearchApp1() {
        frame = new JFrame("Image Search App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout());
        chooseImageButton = new JButton("Choose Image");
        searchButton = new JButton("Search");
        dateTextField = new JTextField(10);
        JLabel dateLabel = new JLabel("Date:");
        JLabel colorNamesLabel = new JLabel("Color Names:");
        colorNamesTextArea = new JTextArea(4, 20);

        topPanel.add(chooseImageButton);
        topPanel.add(dateLabel);
        topPanel.add(dateTextField);
        topPanel.add(colorNamesLabel);
        topPanel.add(new JScrollPane(colorNamesTextArea));
        topPanel.add(searchButton);
        topPanel.add(cropButton);


        JPanel centerPanel = new JPanel(new GridLayout(1, 3));
        resultPanels = new JPanel[MAX_DISPLAYED_IMAGES];
        for (int i = 0; i < MAX_DISPLAYED_IMAGES; i++) {
            resultPanels[i] = new JPanel();
            centerPanel.add(resultPanels[i]);
        }

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(centerPanel, BorderLayout.CENTER);

        List<File> searchFolders = new ArrayList<>();
        searchFolders.add(new File("D:\\imagesTest"));
        searchFolders.add(new File("D:\\imageTest2"));


        chooseImageButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showOpenDialog(ImageSearchApp1.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    selectedImageFile = fileChooser.getSelectedFile();
                    try {
                        originalImage = ImageIO.read(selectedImageFile);
                        int newWidth = 400;
                        int newHeight = 300;
                        BufferedImage resizedImage = resizeImage(originalImage, newWidth, newHeight);
                        System.out.println(resizedImage);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (originalImage == null) {
                        System.out.print("null++++++++++++++++++++++++++++++++");
                        return;
                    }

                    List<ImageData> outputImagesList = new ArrayList<>();

                    for (File searchFolder : searchFolders) {
                        List<ImageData> folderImageList = loadImageDataFromFolder(searchFolder);
                        outputImagesList.addAll(folderImageList);
                    }

                    userColorPalette = medianCut(getPixels(originalImage), MAX_COLORS);
                    outputImagesList = search(outputImagesList, userColorPalette);
                    filter(outputImagesList, dateTextField.getText());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        cropButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedImageFile != null) {
                    try {
                        List<ImageData> outputImagesList = new ArrayList<>();

                        for (File searchFolder : searchFolders) {
                            List<ImageData> folderImageList = loadImageDataFromFolder(searchFolder);
                            outputImagesList.addAll(folderImageList);
                        }
                        int x = 50; // The x-coordinate of the top-left corner of the cropped area
                        int y = 50; // The y-coordinate of the top-left corner of the cropped area
                        int width = 200; // The width of the cropped area
                        int height = 150; // The height of the cropped area

                        BufferedImage croppedImage = originalImage.getSubimage(x, y, width, height);
                        List<Color> croppedPixels = getPixels(croppedImage);
                        List<Color> croppedColorPalette = medianCut(croppedPixels, MAX_COLORS);
                        outputImagesList = search(outputImagesList, croppedColorPalette);
                        filter(outputImagesList, dateTextField.getText());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });



//        searchButton.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                try{
//                    if(originalImage == null){
//                        System.out.print("null++++++++++++++++++++++++++++++++");
//                        return;
//                    }
//                    File searchFolder = new File("D:\\imagesTest");
//                    List<ImageData> outputImagesList = loadImageDataFromFolder(searchFolder);
//                    userColorPalette = medianCut(getPixels(originalImage), MAX_COLORS);
//                    outputImagesList=search(outputImagesList,userColorPalette);
//                    filter(outputImagesList,dateTextField.getText());
//                }catch (Exception ex){
//                    ex.printStackTrace();
//                }
//            }
//        });

        frame.pack();
        frame.setVisible(true);
    }

    private BufferedImage resizeImage(BufferedImage originalImage, int width, int height) {
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resizedImage.createGraphics();
        graphics.drawImage(originalImage, 0, 0, width, height, null);
        graphics.dispose();
        return resizedImage;
    }
    private List<Color> getPixels(BufferedImage image) {
        List<Color> pixels = new ArrayList<>();
        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y));
                pixels.add(color);
            }
        }

        return pixels;
    }

    private java.util.List<Color> medianCut(java.util.List<Color> pixels, int maxColors) {
        // Calculate the initial color cube containing all pixels
        ImageSearchApp1.ColorCube cube = new ImageSearchApp1.ColorCube(pixels,currentImage);

        // Queue to store the color cubes
        java.util.List<ImageSearchApp1.ColorCube> cubeQueue = new ArrayList<>();
        cubeQueue.add(cube);

        // Perform median cut until the desired number of colors is reached
        while (cubeQueue.size() < maxColors) {
            // Find the cube with the maximum side length
            int maxCubeIndex = 0;
            int maxCubeSideLength = 0;
            for (int i = 0; i < cubeQueue.size(); i++) {
                ImageSearchApp1.ColorCube currentCube = cubeQueue.get(i);
                int currentSideLength = Math.max(currentCube.getRedRange(),
                        Math.max(currentCube.getGreenRange(), currentCube.getBlueRange()));
                if (currentSideLength > maxCubeSideLength) {
                    maxCubeIndex = i;
                    maxCubeSideLength = currentSideLength;
                }
            }

            // Split the max cube into two new cubes along the axis with the largest range
            ImageSearchApp1.ColorCube maxCube = cubeQueue.get(maxCubeIndex);
            ImageSearchApp1.ColorCube[] splitCubes = maxCube.splitCube();

            // Remove the max cube from the queue and add the split cubes
            cubeQueue.remove(maxCubeIndex);
            cubeQueue.add(splitCubes[0]);
            cubeQueue.add(splitCubes[1]);
        }

        // Collect the representative colors from the color cubes
        java.util.List<Color> colorPalette = new ArrayList<>();
        for (ImageSearchApp1.ColorCube colorCube : cubeQueue) {
            Color representativeColor = colorCube.getRepresentativeColor();
            colorPalette.add(representativeColor);
        }

        return colorPalette;
    }
    private List<ImageData> loadImageDataFromFolder(File folder) {
        List<ImageData> imageList = new ArrayList<>();

        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    BufferedImage image = loadImage(file);
                    currentImage = image;
                    List<Color> colorPalette = medianCut(getPixels(image), MAX_COLORS);
                    imageList.add(new ImageData(file, colorPalette));
                }
            }
        }

        return imageList;
    }

    private BufferedImage loadImage(File imageFile) {
        try {
            return ImageIO.read(imageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

//    private List<Color> getPixels(BufferedImage image) {
//        List<Color> pixels = new ArrayList<>();
//
//        int width = image.getWidth();
//        int height = image.getHeight();
//
//        for (int y = 0; y < height; y++) {
//            for (int x = 0; x < width; x++) {
//                Color color = new Color(image.getRGB(x, y));
//                pixels.add(color);
//            }
//        }
//
//        return pixels;
//    }

    private List<ImageData> search(List<ImageData> imageList, List<Color> userColorPalette) {
        for (ImageData imageData : imageList) {
            int similarity = calculatePaletteSimilarity(imageData.getColorPalette(), userColorPalette);
            imageData.setRank(similarity);
        }

        imageList.sort(Comparator.comparingInt(ImageData::getRank));
        return  imageList;
    }

    private int calculatePaletteSimilarity(List<Color> palette1, List<Color> palette2) {
        int similarity = 0;
        for (Color color1 : palette1) {
            int closestDistance = Integer.MAX_VALUE;
            for (Color color2 : palette2) {
                int distance = calculateColorDistance(color1, color2);
                if (distance < closestDistance) {
                    closestDistance = distance;
                }
            }
            similarity += closestDistance;
        }
        return similarity;
    }

    private int calculateColorDistance(Color color1, Color color2) {
        int redDiff = color1.getRed() - color2.getRed();
        int greenDiff = color1.getGreen() - color2.getGreen();
        int blueDiff = color1.getBlue() - color2.getBlue();
        return (redDiff * redDiff) + (greenDiff * greenDiff) + (blueDiff * blueDiff);
    }

    private void filter(List<ImageData> imageList, String date) {
        List<Color> filterColors = parseColorNames(colorNamesTextArea.getText());
        System.out.println(filterColors);
        List<ImageData> filteredImages = new ArrayList<>();

        for (ImageData imageData : imageList) {
            if (date.isEmpty() || date.equals(getImageDate(imageData.getImageFile()))) {
                if (containsSimilarColors(imageData.getColorPalette(), filterColors,15)) {
                    filteredImages.add(imageData);
                }else{
                    System.out.println("invalied++++++++++++++++++++++++++++++++");
                }
            }
        }

        if (!filteredImages.isEmpty()) {
            filteredImages.sort(Comparator.comparingInt(ImageData::getRank));

            int numDisplayedImages = Math.min(MAX_DISPLAYED_IMAGES, filteredImages.size());

            for (int i = 0; i < numDisplayedImages; i++) {
                displayImage(filteredImages.get(i).getImageFile(), resultPanels[i]);
            }
        }
    }

    private List<Color> parseColorNames(String colorNames) {
        List<Color> colors = new ArrayList<>();
        String[] colorArray = colorNames.split(",");
        for (String colorName : colorArray) {
            try {
                Color color = Color.decode(colorName.trim());
                colors.add(color);
            } catch (NumberFormatException ignored) {
            }
        }
        return colors;
    }

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//    private List<Color> parseColorNames(String colorNames) {
//        List<Color> colors = new ArrayList<>();
//        String[] colorArray = colorNames.split(",");
//        for (String colorName : colorArray) {
//            try {
//                Color color = Color.getColor(colorName.trim());
//                if (color != null) {
//                    colors.add(color);
//                }
//            } catch (NumberFormatException ignored) {
//            }
//        }
//        return colors;
//    }

    private boolean containsSimilarColors(List<Color> palette, List<Color> filterColors, int similarityThreshold) {
        for (Color filterColor : filterColors) {
            boolean foundSimilarColor = false;
            for (Color paletteColor : palette) {
                if (isSimilarColor(paletteColor, filterColor, similarityThreshold)) {
                    foundSimilarColor = true;
                    break;
                }
            }
            if (!foundSimilarColor) {
                return false;
            }
        }
        return true;
    }

    private boolean isSimilarColor(Color color1, Color color2, int similarityThreshold) {
        int redDiff = Math.abs(color1.getRed() - color2.getRed());
        int greenDiff = Math.abs(color1.getGreen() - color2.getGreen());
        int blueDiff = Math.abs(color1.getBlue() - color2.getBlue());
        int i = (redDiff ) + (greenDiff ) + (blueDiff );
        System.out.println(Math.sqrt(i));
        return Math.sqrt((redDiff) + (greenDiff) + (blueDiff)) <= similarityThreshold;
    }

    private boolean containsColors(List<Color> palette, List<Color> colors) {
        for (Color color : colors) {
            if (!palette.contains(color)) {
                return false;
            }
        }
        return true;
    }

    private String getImageDate(File imageFile) {
        try {
            BasicFileAttributes attributes = Files.readAttributes(imageFile.toPath(), BasicFileAttributes.class);
            FileTime creationTime = attributes.creationTime();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            return dateFormat.format(new Date(creationTime.toMillis()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private void displayImage(File imageFile, JPanel panel) {
        try {
            BufferedImage image = ImageIO.read(imageFile);
            ImageIcon icon = new ImageIcon(image.getScaledInstance(panel.getWidth(), panel.getHeight(), Image.SCALE_SMOOTH));
            JLabel label = new JLabel(icon);
            panel.removeAll();
            panel.add(label);
            panel.revalidate();
            panel.repaint();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new ImageSearchApp1();
            }
        });
    }
    private static class ColorCube {
        private int minRed;
        private int maxRed;
        private int minGreen;
        private int maxGreen;
        private int minBlue;
        private int maxBlue;
        private java.util.List<Color> pixels;
        BufferedImage currentImage;


        public ColorCube(java.util.List<Color> pixels,BufferedImage currentImage) {
            this.pixels = pixels;
            this.currentImage=currentImage;
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

        public ImageSearchApp1.ColorCube[] splitCube() {
            int redRange = getRedRange();
            int greenRange = getGreenRange();
            int blueRange = getBlueRange();

            if (redRange >= greenRange && redRange >= blueRange) {
                // Split along the red axis
                int splitValue = (minRed + maxRed) / 2;
                ImageSearchApp1.ColorCube cube1 = new ImageSearchApp1.ColorCube(filterPixels(splitValue, "red", true),currentImage);
                ImageSearchApp1.ColorCube cube2 = new ImageSearchApp1.ColorCube(filterPixels(splitValue, "red", false),currentImage);
                return new ImageSearchApp1.ColorCube[]{cube1, cube2};
            } else if (greenRange >= redRange && greenRange >= blueRange) {
                // Split along the green axis
                int splitValue = (minGreen + maxGreen) / 2;
                ImageSearchApp1.ColorCube cube1 = new ImageSearchApp1.ColorCube(filterPixels(splitValue, "green", true),currentImage);
                ImageSearchApp1.ColorCube cube2 = new ImageSearchApp1.ColorCube(filterPixels(splitValue, "green", false),currentImage);
                return new ImageSearchApp1.ColorCube[]{cube1, cube2};
            } else {
                // Split along the blue axis
                int splitValue = (minBlue + maxBlue) / 2;
                ImageSearchApp1.ColorCube cube1 = new ImageSearchApp1.ColorCube(filterPixels(splitValue, "blue", true),currentImage);
                ImageSearchApp1.ColorCube cube2 = new ImageSearchApp1.ColorCube(filterPixels(splitValue, "blue", false),currentImage);
                return new ImageSearchApp1.ColorCube[]{cube1, cube2};
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

//        public Color getRepresentativeColor() {
//            int sumRed = 0;
//            int sumGreen = 0;
//            int sumBlue = 0;
//            int count = pixels.size();
//            for (Color color : pixels) {
//                sumRed += color.getRed();
//                sumGreen += color.getGreen();
//                sumBlue += color.getBlue();
//            }
//            int avgRed = sumRed / count;
//            int avgGreen = sumGreen / count;
//            int avgBlue = sumBlue / count;
//            return new Color(avgRed, avgGreen, avgBlue);
//        }

        private Color getRepresentativeColor() {
            if (currentImage.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
                int representativePixel = currentImage.getRGB(currentImage.getWidth() / 2, currentImage.getHeight() / 2);
                return new Color(representativePixel);
            } else {
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
}

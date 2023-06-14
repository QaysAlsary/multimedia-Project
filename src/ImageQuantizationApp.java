import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;

public class ImageQuantizationApp extends JFrame {
    private JLabel originalImageLabel;
    private JLabel outputImageLabel;
    private JLabel paletteLabel;
    private JLabel histogramLabel;
    private JButton openImageButton;
    private JComboBox<String> algorithmComboBox;
    private JButton quantizeButton;
    private JButton saveImageButton;
    private File selectedImageFile;
    private BufferedImage originalImage;
    private BufferedImage outputImage;
    private JLabel elapsedTimeLabel;
    static Logic logic = new Logic();
    long startTime;
    long endTime;
    long elapsedTime;

    public ImageQuantizationApp() {
        setTitle("Image Quantization App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        originalImageLabel = new JLabel();
        outputImageLabel = new JLabel();
        paletteLabel = new JLabel();
        elapsedTimeLabel = new JLabel();
        histogramLabel = new JLabel();
        openImageButton = new JButton("Open Image");
        algorithmComboBox = new JComboBox<>(new String[]{"Uniform Quantization", "Median Cut", "K-Means"});
        quantizeButton = new JButton("Quantize");
        saveImageButton = new JButton("Save Indexed Image");

        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(openImageButton);
        topPanel.add(algorithmComboBox);
        topPanel.add(quantizeButton);
        topPanel.add(saveImageButton);

        JPanel centerPanel = new JPanel(new GridLayout(1, 2));
        centerPanel.add(originalImageLabel);
        centerPanel.add(outputImageLabel);

        JPanel bottomPanel = new JPanel(new GridLayout(1, 2));
        bottomPanel.add(paletteLabel);
        bottomPanel.add(histogramLabel);


        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        add(elapsedTimeLabel, BorderLayout.WEST);

        openImageButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showOpenDialog(ImageQuantizationApp.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    selectedImageFile = fileChooser.getSelectedFile();
                    try {
                        originalImage = ImageIO.read(selectedImageFile);
                        originalImageLabel.setIcon(new ImageIcon(originalImage));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        quantizeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (originalImage != null) {
                    startTime=System.currentTimeMillis();
                    String selectedAlgorithm = (String) algorithmComboBox.getSelectedItem();
                    if (selectedAlgorithm.equals("Uniform Quantization")) {
                        outputImage = logic.applyUniformQuantization(originalImage);
                    } else if (selectedAlgorithm.equals("Median Cut")) {
                        outputImage = logic.applyMedianCut(originalImage);
                    } else if (selectedAlgorithm.equals("K-Means")) {
                        outputImage = logic.applyKMeans(originalImage);

                    }

                    outputImageLabel.setIcon(new ImageIcon(outputImage));

                    List<Color> palette = logic.generatePalette(outputImage);
                    showPalette(palette);
                    JPanel colorHistogramPanel = drawColorHistogram(originalImage);
                    JFrame histogramFrame = new JFrame("Color Histogram");
                    histogramFrame.add(colorHistogramPanel);
                    histogramFrame.pack();
                    histogramFrame.setLocationRelativeTo(null);
                    histogramFrame.setVisible(true);
                    endTime=System.currentTimeMillis();
                    elapsedTime=endTime - startTime;
//                    System.out.println(elapsedTime/1000+"second");
                    long seconds = elapsedTime / 1000;
                    long milliseconds = elapsedTime % 1000;
                    elapsedTimeLabel.setText(String.format("Elapsed Time: %d.%03d seconds", seconds, milliseconds));


                } else {
                    JOptionPane.showMessageDialog(ImageQuantizationApp.this, "Please select an image first.");
                }
            }
        });

        saveImageButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (outputImage != null) {
                    JFileChooser fileChooser = new JFileChooser();
                    int result = fileChooser.showSaveDialog(ImageQuantizationApp.this);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        File outputFile = fileChooser.getSelectedFile();
                        try {
                            // Convert the output image to an indexed image
                            IndexColorModel indexedColorModel = generateIndexedColorModel(outputImage);
                            BufferedImage indexedImage = convertToIndexed(outputImage, indexedColorModel);

                            ImageIO.write(indexedImage, "png", outputFile);
                            JOptionPane.showMessageDialog(ImageQuantizationApp.this, "Indexed image saved successfully.");
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(ImageQuantizationApp.this, "No output image available.");
                }
            }
        });


        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }



    private void showPalette(List<Color> palette) {
        StringBuilder sb = new StringBuilder("<html><body>");
        sb.append("<div style='display:flex;'>"); // Add flex display style for horizontal layout
        for (Color color : palette) {
            sb.append("<div style='background-color:rgb(")
                    .append(color.getRed()).append(",")
                    .append(color.getGreen()).append(",")
                    .append(color.getBlue()).append(");width:20px;height:20px;margin-right:5px;'></div>"); // Add margin for spacing
        }
        sb.append("</div></body></html>");

        paletteLabel.setText(sb.toString());
    }
    private IndexColorModel generateIndexedColorModel(BufferedImage image) {
        List<Color> palette = logic.generatePalette(image);
        int paletteSize = palette.size();

        byte[] reds = new byte[paletteSize];
        byte[] greens = new byte[paletteSize];
        byte[] blues = new byte[paletteSize];

        for (int i = 0; i < paletteSize; i++) {
            Color color = palette.get(i);
            reds[i] = (byte) color.getRed();
            greens[i] = (byte) color.getGreen();
            blues[i] = (byte) color.getBlue();
        }

        return new IndexColorModel(8, paletteSize, reds, greens, blues);
    }

    private BufferedImage convertToIndexed(BufferedImage image, IndexColorModel indexedColorModel) {
        BufferedImage indexedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_INDEXED, indexedColorModel);
        Graphics2D g2d = indexedImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return indexedImage;
    }


    public static JPanel drawColorHistogram(BufferedImage image) {
        int[] histogram = logic.calculateColorHistogram(image);
        // Find the maximum count in the histogram
        int maxCount = Arrays.stream(histogram).max().orElse(1);
        // Create a new JPanel to display the histogram
        JPanel histogramPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Loop through each bin in the histogram
                for (int i = 0; i < 256; i++) {
                    int count = histogram[i];
                    // Calculate the height of the histogram bar based on the count
                    int barHeight = (int) (count * 200.0 / maxCount);
                    // Calculate the y-coordinate of the start of the histogram bar
                    int y = getHeight() - barHeight;
                    // Calculate the x-coordinate of the start of the histogram bar
                    int x = i * getWidth() / 256;
                    // Draw the histogram bar
                    g.setColor(new Color(i, i, i));
                    g.fillRect(x, y, getWidth() / 256, barHeight);
                }
            }
        };
        // Set the layout of the histogram panel to null
        histogramPanel.setLayout(null);
        // Set the preferred size of the histogram panel to 256x200 pixels
        histogramPanel.setPreferredSize(new Dimension(256, 200));
        return histogramPanel;
    }




}

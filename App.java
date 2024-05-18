import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

public class App extends JFrame {
    private JLabel imageLabel;
    private JLabel messageLabel;
    private BufferedImage originalImage;
    private BufferedImage compressedImage;
    private JSlider compressionSlider;

    private JLabel imageSizeLabel = new JLabel("Image size: N/A"); // Declare here
    private JPanel imagePanel = new JPanel(new BorderLayout());

    public App() {
        super("Image Uploader");

        // Set up the main panel
        JPanel mainPanel = new JPanel();
        JPanel leftPanel = new JPanel();
        JPanel rightPanel = new JPanel();

        mainPanel.setLayout(new BorderLayout());

        // Create the button and add action listener
        JButton uploadButton = new JButton("Upload Image");
        uploadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                uploadImage();
            }
        });

        // Create the download button and add action listener
        JButton downloadButton = new JButton("Download Image");
        downloadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                downloadImage();
            }
        });

        // Create a label to display the image
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setVerticalAlignment(JLabel.CENTER);

        // Create a label for drag-and-drop message
        messageLabel = new JLabel("Drag and Drop Here", JLabel.CENTER);
        messageLabel.setFont(new Font("Serif", Font.BOLD, 20));
        messageLabel.setForeground(Color.GRAY);

        // Create a panel to hold the image and message labels
        imagePanel.add(imageLabel, BorderLayout.CENTER);
        imagePanel.add(messageLabel, BorderLayout.NORTH);

        // Add drag and drop functionality
        new DropTarget(imageLabel, new DropTargetAdapter() {
            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                messageLabel.setText("Release to Drop");
            }

            @Override
            public void dragExit(DropTargetEvent dte) {
                messageLabel.setText("Drag and Drop Here");
            }

            @Override
            @SuppressWarnings("unchecked")
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable transferable = dtde.getTransferable();
                    List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                    if (files.size() > 0) {
                        File file = files.get(0);
                        displayImage(file);
                    }
                    dtde.dropComplete(true);
                } catch (UnsupportedFlavorException | IOException ex) {
                    ex.printStackTrace();
                    dtde.dropComplete(false);
                }
            }
        });

        // Create a slider for compression
        compressionSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 75);
        compressionSlider.setMajorTickSpacing(25);
        compressionSlider.setMinorTickSpacing(5);
        compressionSlider.setPaintTicks(true);
        compressionSlider.setPaintLabels(true);
        compressionSlider.addChangeListener(e -> compressImage());

        // Create a panel for the buttons and slider
        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new BorderLayout());
        controlsPanel.add(uploadButton, BorderLayout.WEST);
        controlsPanel.add(downloadButton, BorderLayout.EAST);
        controlsPanel.add(compressionSlider, BorderLayout.CENTER);

        // Add the controls panel and the image panel to the main panel
        mainPanel.add(imageSizeLabel, BorderLayout.NORTH);
        mainPanel.add(controlsPanel, BorderLayout.SOUTH);
        mainPanel.add(imagePanel, BorderLayout.CENTER);
        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(rightPanel, BorderLayout.EAST);

        // Add the main panel to the frame
        add(mainPanel);

        // Set up the frame
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void uploadImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(
                new javax.swing.filechooser.FileNameExtensionFilter("Image files", ImageIO.getReaderFileSuffixes()));
        int returnValue = fileChooser.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            displayImage(selectedFile);
        }
    }

    private void displayImage(File file) {
        try {
            originalImage = ImageIO.read(file);
            if (originalImage != null) {
                imageLabel.setIcon(new ImageIcon(originalImage));
                messageLabel.setText(""); // Clear the message
                compressImage(); // Compress the image initially based on the slider value
            } else {
                JOptionPane.showMessageDialog(this, "The selected file is not a valid image.", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "An error occurred while loading the image.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void compressImage() {
        if (originalImage == null) {
            return;
        }

        float quality = compressionSlider.getValue() / 100f;

        try {
            // Create a ByteArrayOutputStream instead of a temporary file
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageOutputStream ios = ImageIO.createImageOutputStream(baos);

            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            writer.write(null, new javax.imageio.IIOImage(originalImage, null, null), param);
            writer.dispose();
            ios.close(); // Close the stream explicitly

            // Get the compressed image data from the ByteArrayOutputStream
            byte[] compressedImageData = baos.toByteArray();

            // Update image and size
            compressedImage = ImageIO.read(new ByteArrayInputStream(compressedImageData));
            // imageLabel.setIcon(new ImageIcon(compressedImage));
            // resize the image to fit window
            // *********************************************** */
            // Get the dimensions of the panel
            int panelWidth = imagePanel.getWidth();
            int panelHeight = imagePanel.getHeight();

            // Calculate the maximum allowed dimensions for the image while maintaining
            // aspect ratio
            double imageAspectRatio = (double) imagePanel.getWidth() / (double) imagePanel.getHeight();
            int maxWidth = panelWidth;
            int maxHeight = panelHeight;

            if (imageAspectRatio > 1.0) { // Landscape image
                maxHeight = (int) (maxWidth / imageAspectRatio);
            } else { // Portrait image
                maxWidth = (int) (maxHeight * imageAspectRatio);
            }

            // Scale the image to fit within the calculated dimensions
            Image scaledImage = compressedImage.getScaledInstance(maxWidth, maxHeight, Image.SCALE_SMOOTH);
            // ************************************************ */
            // Update the image label with the scaled image
            imageLabel.setIcon(new ImageIcon(scaledImage));

            imageSizeLabel.setText("Image size: " + compressedImageData.length / 1000 + "KB (Kilobytes)");
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "An error occurred while compressing the image.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void downloadImage() {
        if (compressedImage == null) {
            JOptionPane.showMessageDialog(this, "No image to download.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JPEG file", "jpg", "jpeg"));
        int returnValue = fileChooser.showSaveDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".jpg") && !file.getName().toLowerCase().endsWith(".jpeg")) {
                file = new File(file.getAbsolutePath() + ".jpg");
            }

            try {
                ImageIO.write(compressedImage, "jpg", file);
                JOptionPane.showMessageDialog(this, "Image saved successfully.", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "An error occurred while saving the image.", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new App().setVisible(true);
            }
        });
    }
}

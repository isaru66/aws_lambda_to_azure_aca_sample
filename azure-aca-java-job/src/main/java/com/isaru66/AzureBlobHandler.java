package com.isaru66;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.ListBlobsOptions;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AzureBlobHandler {
  private static final float MAX_DIMENSION = 100;
  private final String REGEX = ".*\\.([^\\.]*)";
  private final String JPG_TYPE = "jpg";
  private final String JPG_MIME = "image/jpeg";
  private final String PNG_TYPE = "png";
  private final String PNG_MIME = "image/png";

  private static BlobServiceClient blobServiceClient = null;

  /**
   * Resizes (shrinks) an image into a small, thumbnail-sized image.
   * 
   * The new image is scaled down proportionally based on the source
   * image. The scaling factor is determined based on the value of
   * MAX_DIMENSION. The resulting new image has max(height, width)
   * = MAX_DIMENSION.
   * 
   * @param srcImage BufferedImage to resize.
   * @return New BufferedImage that is scaled down to thumbnail size.
   */
  private BufferedImage resizeImage(BufferedImage srcImage) {
    int srcHeight = srcImage.getHeight();
    int srcWidth = srcImage.getWidth();
    // Infer scaling factor to avoid stretching image unnaturally
    float scalingFactor = Math.min(
        MAX_DIMENSION / srcWidth, MAX_DIMENSION / srcHeight);
    int width = (int) (scalingFactor * srcWidth);
    int height = (int) (scalingFactor * srcHeight);

    BufferedImage resizedImage = new BufferedImage(width, height,
        BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = resizedImage.createGraphics();
    // Fill with white before applying semi-transparent (alpha) images
    graphics.setPaint(Color.white);
    graphics.fillRect(0, 0, width, height);
    // Simple bilinear resize
    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    graphics.drawImage(srcImage, 0, 0, width, height, null);
    graphics.dispose();
    return resizedImage;
  }

  public void listAndMoveBlobs() {
        final String containerName = System.getenv("AZURE_STORAGE_BLOB_CONTAINER_NAME");
        final String blobInputPrefix = System.getenv("AZURE_STORAGE_BLOB_INPUT_PREFIX") != null ? System.getenv("AZURE_STORAGE_BLOB_INPUT_PREFIX") : "input";
        final String blobOutputPrefix = System.getenv("AZURE_STORAGE_BLOB_OUTPUT_PREFIX") != null ? System.getenv("AZURE_STORAGE_BLOB_OUTPUT_PREFIX") : "resize";

        
        if (containerName == null || containerName.isEmpty()) {
            throw new IllegalArgumentException("AZURE_STORAGE_BLOB_CONTAINER_NAME environment variable is not set");
        }

        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        
        System.out.printf("%nListing blobs in container [%s] with prefix [%s]...%n", 
            containerName, blobInputPrefix);

        ListBlobsOptions options = new ListBlobsOptions().setPrefix(blobInputPrefix);
        
        containerClient.listBlobs(options, null).forEach(blobItem -> {
            try {
                BlobClient sourceBlob = containerClient.getBlobClient(blobItem.getName());
                
                // Display blob name and URL
                System.out.printf("%n  name: %s%n  URL: %s%n", 
                    blobItem.getName(), sourceBlob.getBlobUrl());

                // Move blob to output prefix location
                if (blobItem.getName().startsWith(blobInputPrefix + "/")) {
                    String newBlobName = blobItem.getName().replace(blobInputPrefix, blobOutputPrefix);
                    BlobClient destinationBlob = containerClient.getBlobClient(newBlobName);

                    String srcBlobName = blobItem.getName();
                    // Infer the image type
                    Matcher matcher = Pattern.compile(REGEX).matcher(srcBlobName);
                    if (!matcher.matches()) {
                        System.out.println("Unable to infer image type for blob " + srcBlobName);
                        throw new IllegalArgumentException("Unable to infer image type for blob " + srcBlobName);
                    }
                    String imageType = matcher.group(1);
                    if (!(JPG_TYPE.equals(imageType)) && !(PNG_TYPE.equals(imageType))) {
                        System.out.println("Skipping non-image " + srcBlobName);
                    }

                    // Read Image
                    InputStream blobContent = sourceBlob.openInputStream();

                    // Read the source image and resize it
                    BufferedImage srcImage = ImageIO.read(blobContent);
                    BufferedImage newImage = resizeImage(srcImage);

                    // Re-encode image to target format
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    ImageIO.write(newImage, imageType, outputStream);

                    BlobHttpHeaders blobHeaders = new BlobHttpHeaders()
                            .setContentType("application/octet-stream");
                    if (JPG_TYPE.equals(imageType)) {
                        blobHeaders.setContentType(JPG_MIME);
                    } else if (PNG_TYPE.equals(imageType)) {
                        blobHeaders.setContentType(PNG_MIME);
                    }


                    // Start copy operation
                    byte[] outputBytes = outputStream.toByteArray();
                    destinationBlob.upload(new ByteArrayInputStream(outputBytes), outputBytes.length, true);
                    destinationBlob.setHttpHeaders(blobHeaders);
                    System.out.printf("Resize image : %s%n", destinationBlob.getBlobName());
                    
                    // Delete the original blob
                    sourceBlob.delete();
                    System.out.printf("Delete original: %s%n", sourceBlob.getBlobName());
                }
            } catch (Exception e) {
                System.err.printf("Error processing blob %s: %s%n", 
                    blobItem.getName(), e.getMessage());
            }
        });
    }

    // Add a main method to test the functionality
    public static void main(String[] args) {
        System.out.println("Connection String: "+System.getenv("AZURE_STORAGE_CONNECTION_STRING"));
        blobServiceClient = new BlobServiceClientBuilder()
      .connectionString(System.getenv("AZURE_STORAGE_CONNECTION_STRING"))
      .buildClient();
        AzureBlobHandler handler = new AzureBlobHandler();
        handler.listAndMoveBlobs();
    }
}
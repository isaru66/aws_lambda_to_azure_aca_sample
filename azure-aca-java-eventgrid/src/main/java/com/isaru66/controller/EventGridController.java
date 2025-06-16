package com.isaru66.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;

import com.isaru66.model.ValidationResponse;

@RestController
@RequestMapping("/api")
public class EventGridController {
    private static final Logger logger = LoggerFactory.getLogger(EventGridController.class);
    private static final float MAX_DIMENSION = 100;
    private final String REGEX = ".*\\.([^\\.]*)";
    private final String JPG_TYPE = "jpg";
    private final String JPG_MIME = "image/jpeg";
    private final String PNG_TYPE = "png";
    private final String PNG_MIME = "image/png";

    @PostMapping("/webhook")
    public ResponseEntity<?> handleEventGridEvent(
            @RequestBody List<Map<String, Object>> events) {
        
        for (Map<String, Object> event : events) {
            String eventType = (String) event.get("eventType");
            logger.info("Received event type: {}", eventType);

            if ("Microsoft.EventGrid.SubscriptionValidationEvent".equals(eventType)) {
                // Handle validation event
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) event.get("data");
                String validationCode = (String) data.get("validationCode");
                
                // Return the validation response
                return ResponseEntity.ok(new ValidationResponse(validationCode));
            
            } else if ("Microsoft.Storage.BlobCreated".equals(eventType)) {
                try {
                    // Handle blob created event
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) event.get("data");
                    
                    String url = (String) data.get("url");
                    String contentType = (String) data.get("contentType");
                    Long contentLength = ((Number) data.get("contentLength")).longValue();
                    
                    logger.info("Blob created - URL: {}, Type: {}, Size: {} bytes", 
                        url, contentType, contentLength);

                    // Extract source container and blob name from URL
                    String[] urlParts = url.split("/");
                    String srcKey = urlParts[urlParts.length - 1];
                    String srcBucket = urlParts[urlParts.length - 2];
                    
                    // Process image only if it's jpg or png
                    Matcher matcher = Pattern.compile(REGEX).matcher(srcKey);
                    if (!matcher.matches()) {
                        logger.info("Unable to infer image type for key " + srcKey);
                        continue;
                    }
                    String imageType = matcher.group(1).toLowerCase();
                    if (!(JPG_TYPE.equals(imageType)) && !(PNG_TYPE.equals(imageType))) {
                        logger.info("Skipping non-image " + srcKey);
                        continue;
                    }

                    String dstBucket = System.getenv("DEST_BUCKET");
                    if (dstBucket == null || dstBucket.isEmpty()) {
                        throw new IllegalArgumentException("DEST_BUCKET environment variable is not set");
                    }
                    String dstKey = "resized-" + srcKey;

                    // Download the image from Azure Blob Storage
                    BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                        .endpoint(System.getenv("AZURE_BLOB_ENDPOINT"))
                        .credential(new DefaultAzureCredentialBuilder().build())
                        .buildClient();

                    BlobContainerClient srcContainerClient = blobServiceClient.getBlobContainerClient(srcBucket);
                    BlobClient srcBlobClient = srcContainerClient.getBlobClient(srcKey);
                    InputStream blobInputStream = getObject(srcBlobClient);

                    // Read the source image and resize it
                    BufferedImage srcImage = ImageIO.read(blobInputStream);
                    BufferedImage newImage = resizeImage(srcImage);

                    // Re-encode image to target format
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    ImageIO.write(newImage, imageType, outputStream);

                    // Upload new image to Azure Blob Storage
                    BlobContainerClient dstContainerClient = blobServiceClient.getBlobContainerClient(dstBucket);
                    BlobClient dstBlobClient = dstContainerClient.getBlobClient(dstKey);
                    putObject(dstBlobClient, outputStream, imageType);

                    logger.info("Successfully resized {}/{} and uploaded to {}/{}",
                        srcBucket, srcKey, dstBucket, dstKey);

                } catch (IOException e) {
                    logger.error("Error processing blob: {}", e.getMessage());
                    return ResponseEntity.internalServerError().build();
                }
            }
        }
        
        return ResponseEntity.ok().build();
    }

    private InputStream getObject(BlobClient blobClient) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blobClient.downloadStream(outputStream);
        return new java.io.ByteArrayInputStream(outputStream.toByteArray());
    }

    private void putObject(BlobClient blobClient, ByteArrayOutputStream outputStream, String imageType) {
        BlobHttpHeaders headers = new BlobHttpHeaders();
        if (JPG_TYPE.equals(imageType)) {
            headers.setContentType(JPG_MIME);
        } else if (PNG_TYPE.equals(imageType)) {
            headers.setContentType(PNG_MIME);
        }
        logger.info("Writing to: {}", blobClient.getBlobUrl());
        try {
            blobClient.upload(new java.io.ByteArrayInputStream(outputStream.toByteArray()), outputStream.size(), true);
            blobClient.setHttpHeaders(headers);
        } catch (Exception e) {
            logger.error("Error uploading blob: {}", e.getMessage());
            throw new RuntimeException("Failed to upload resized image", e);
        }
    }

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
}
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
                // Handle blob created event
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) event.get("data");
                
                String url = (String) data.get("url");
                String contentType = (String) data.get("contentType");
                Long contentLength = ((Number) data.get("contentLength")).longValue();
                
                logger.info("Blob created - URL: {}, Type: {}, Size: {} bytes", 
                    url, contentType, contentLength);
                
                // Process the blob as needed
                // TODO: Add your blob processing logic here
            }
        }
        
        return ResponseEntity.ok().build();
    }
}
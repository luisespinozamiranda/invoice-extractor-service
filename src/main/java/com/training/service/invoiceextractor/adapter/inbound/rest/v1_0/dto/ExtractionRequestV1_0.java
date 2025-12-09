package com.training.service.invoiceextractor.adapter.inbound.rest.v1_0.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST DTO for Extraction Request (API v1.0)
 * Used for file upload requests to extract invoice data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionRequestV1_0 {

    private MultipartFile file;
}

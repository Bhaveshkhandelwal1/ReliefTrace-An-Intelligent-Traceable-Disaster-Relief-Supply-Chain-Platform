package com.disasterreliefnet.verification.dto;

import com.disasterreliefnet.common.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UploadDocumentRequest(
    @NotNull(message = "Document type is required")
    DocumentType documentType,

    @NotBlank(message = "File URL cannot be blank")
    @Size(max = 500, message = "File URL cannot exceed 500 characters")
    String fileUrl
) {}

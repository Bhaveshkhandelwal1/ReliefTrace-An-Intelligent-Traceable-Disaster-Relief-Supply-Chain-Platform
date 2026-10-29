package com.disasterreliefnet.verification.mapper;

import com.disasterreliefnet.verification.domain.VerificationCase;
import com.disasterreliefnet.verification.domain.VerificationDocument;
import com.disasterreliefnet.verification.dto.DocumentDto;
import com.disasterreliefnet.verification.dto.VerificationCaseResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VerificationMapper {

    @Mapping(target = "documents", source = "documents")
    VerificationCaseResponse toCaseResponse(VerificationCase verificationCase);

    DocumentDto toDocumentDto(VerificationDocument document);

    List<DocumentDto> toDocumentDtoList(List<VerificationDocument> documents);
}

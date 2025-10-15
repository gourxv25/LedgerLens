package com.gourav.LedgerLens.Mapper;

import com.gourav.LedgerLens.Domain.Dtos.DocumentResponseDto;
import com.gourav.LedgerLens.Domain.Entity.Document;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DocumentMapper {

    DocumentResponseDto toDto(Document document);
}

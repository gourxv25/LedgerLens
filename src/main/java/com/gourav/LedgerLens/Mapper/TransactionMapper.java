package com.gourav.LedgerLens.Mapper;

import com.gourav.LedgerLens.Domain.Dtos.CreateTransactionDto;
import com.gourav.LedgerLens.Domain.Dtos.TransactionResponseDto;
import com.gourav.LedgerLens.Domain.Entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TransactionMapper {

    @Mapping(target ="user" , ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "document", ignore = true)
    Transaction toEntity(CreateTransactionDto createTransactionDto);

    TransactionResponseDto toDto(Transaction transaction);
    
}

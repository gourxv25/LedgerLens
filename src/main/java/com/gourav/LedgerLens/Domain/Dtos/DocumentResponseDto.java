package com.gourav.LedgerLens.Domain.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponseDto {

    private String publicId;
    private String s3Key;
    private String originalFileName;
    private String Status;
}

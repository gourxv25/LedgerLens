package com.gourav.LedgerLens.Service;

import java.io.IOException;

public interface TextExtractService {
    String extractTextFromS3File(String bucketName, String fileKey) throws IOException;
}

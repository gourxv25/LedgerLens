package com.gourav.LedgerLens.Service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface S3Service {

    String uploadFile(MultipartFile file) throws IOException;
    byte[] viewFile(String fileName) throws IOException;
}

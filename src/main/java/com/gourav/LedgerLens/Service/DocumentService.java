package com.gourav.LedgerLens.Service;

import com.gourav.LedgerLens.Domain.Entity.Document;
import com.gourav.LedgerLens.Domain.Entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.gourav.LedgerLens.Domain.Entity.User;

import java.io.IOException;
import java.util.List;


public interface DocumentService {

    void uploadFile(MultipartFile file, User loggedInUser) throws Exception;

    List<Document> getAllDocument();

    byte[] viewDocument(String publicId) throws IOException;
}

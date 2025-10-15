package com.gourav.LedgerLens.Service;

import com.gourav.LedgerLens.Domain.Entity.Document;
import com.gourav.LedgerLens.Domain.Entity.Transaction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.gourav.LedgerLens.Domain.Entity.User;

import java.io.IOException;
import java.util.List;


public interface DocumentService {

    Transaction uploadFile(MultipartFile file, User loggedInUser) throws Exception;
    Transaction test(User loggedInUser) throws IOException;

    List<Document> getAllDocument();

    byte[] viewDocument(String publicId);
}

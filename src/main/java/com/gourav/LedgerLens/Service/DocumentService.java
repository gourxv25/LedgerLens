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

    Page<Transaction> uploadFile(MultipartFile file, User loggedInUser, Pageable pageable) throws Exception;
    Transaction test(User loggedInUser) throws IOException;

    List<Document> getAllDocument();

    byte[] viewDocument(String publicId);
}

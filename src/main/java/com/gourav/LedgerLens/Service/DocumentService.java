package com.gourav.LedgerLens.Service;

import com.gourav.LedgerLens.Domain.Entity.Transaction;
import org.springframework.web.multipart.MultipartFile;

import com.gourav.LedgerLens.Domain.Entity.User;

import java.io.IOException;


public interface DocumentService {

    Transaction uploadFile(MultipartFile file, User loggedInUser) throws Exception;
    Transaction test(User loggedInUser) throws IOException;
}

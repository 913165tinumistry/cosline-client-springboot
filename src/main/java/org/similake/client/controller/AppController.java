package org.similake.client.controller;


import org.similake.client.service.AppService;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class AppController {

    @Autowired
    public AppService appService;

    @GetMapping("/loadDocuments")
    public String loadDocuments() {
        return appService.loadDocuments();
    }

    @GetMapping("/search")
    public List<String> search(@RequestParam(defaultValue = "spring") String query) {
        List<Document> docs = appService.search(query);
        return docs.stream()
                .map(Document::getContent)
                .collect(Collectors.toList());
    }
}

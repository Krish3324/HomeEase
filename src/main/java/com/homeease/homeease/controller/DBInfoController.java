package com.homeease.homeease.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.persistence.EntityManager;
import java.util.List;

@RestController
public class DBInfoController {

    @Autowired
    private EntityManager entityManager;

    @GetMapping("/api/debug/tables")
    @SuppressWarnings("unchecked")
    public List<String> listTables() {
        return (List<String>) entityManager.createNativeQuery("SHOW TABLES").getResultList();
    }

    @GetMapping("/api/debug/columns")
    @SuppressWarnings("unchecked")
    public List<Object[]> listColumns(@org.springframework.web.bind.annotation.RequestParam String table) {
        return (List<Object[]>) entityManager.createNativeQuery("DESCRIBE " + table).getResultList();
    }
}

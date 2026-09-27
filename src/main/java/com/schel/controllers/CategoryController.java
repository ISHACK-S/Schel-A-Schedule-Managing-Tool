package com.schel.controllers;

import com.schel.exceptions.AuthenticationException;
import com.schel.exceptions.DatabaseException;
import com.schel.models.Category;
import com.schel.services.CategoryService;

import java.util.UUID;

public final class CategoryController {

    private final CategoryService service;

    private static final CategoryController INSTANCE = new CategoryController();

    private CategoryController() {
        this.service = CategoryService.getInstance();
    }

    public static CategoryController getInstance() {
        return INSTANCE;
    }

    public void createCategory(String name, String color) throws DatabaseException, AuthenticationException {
        service.createCategory(name, color);
    }

    public Category[] getUserCategories() throws DatabaseException, AuthenticationException {
        return service.getUserCategories();
    }

    public boolean updateCategory(UUID id, String name, String color)
            throws DatabaseException, AuthenticationException {
        return service.updateCategory(id, name, color);
    }

    public boolean deleteCategory(UUID id) throws DatabaseException, AuthenticationException {
        return service.deleteCategory(id);
    }
}

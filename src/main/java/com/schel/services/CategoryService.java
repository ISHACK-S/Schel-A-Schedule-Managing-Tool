package com.schel.services;

import com.schel.controllers.AuthController;
import com.schel.exceptions.AuthenticationException;
import com.schel.exceptions.DatabaseException;
import com.schel.models.Category;
import com.schel.models.User;
import com.schel.repository.CategoryRepository;

import java.util.Objects;
import java.util.UUID;

public final class CategoryService {

    private static final int MAX_NAME_LENGTH = 100;

    private final CategoryRepository repository;
    private final AuthController authController;

    private static final CategoryService INSTANCE = new CategoryService();

    private CategoryService() {
        this.repository = CategoryRepository.getInstance();
        this.authController = AuthController.getInstance();
    }

    public static CategoryService getInstance() {
        return INSTANCE;
    }

    public void createCategory(String name, String color) throws DatabaseException, AuthenticationException {
        User user = currentUser();
        Category category = new Category();
        category.setUserId(user.getId());
        category.setName(validateName(name));
        category.setColor(normalizeColor(color));
        repository.create(category);
    }

    public Category[] getUserCategories() throws DatabaseException, AuthenticationException {
        return repository.findByUserId(currentUser().getId());
    }

    public boolean updateCategory(UUID id, String name, String color)
            throws DatabaseException, AuthenticationException {
        Objects.requireNonNull(id, "Category ID is required.");
        User user = currentUser();
        Category category = new Category();
        category.setName(validateName(name));
        category.setColor(normalizeColor(color));
        return repository.update(user.getId(), id, category);
    }

    public boolean deleteCategory(UUID id) throws DatabaseException, AuthenticationException {
        Objects.requireNonNull(id, "Category ID is required.");
        return repository.delete(currentUser().getId(), id);
    }

    private User currentUser() throws AuthenticationException {
        return authController.getCurrentUser()
                .orElseThrow(() -> new AuthenticationException("User must be logged in"));
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name cannot be blank.");
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Category name cannot exceed " + MAX_NAME_LENGTH + " characters.");
        }
        return trimmed;
    }

    private String normalizeColor(String color) {
        return color == null || color.isBlank() ? null : color.trim();
    }
}

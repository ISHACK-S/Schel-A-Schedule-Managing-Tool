package com.schel.menus;

import com.schel.controllers.CategoryController;
import com.schel.exceptions.AuthenticationException;
import com.schel.exceptions.DatabaseException;
import com.schel.models.Category;
import com.schel.utils.ConsoleInput;

import java.util.Scanner;
import java.util.UUID;

public class CategoryMenu {

    private final CategoryController controller = CategoryController.getInstance();

    public void show() {
        Scanner scanner = ConsoleInput.getScanner();
        while (true) {
            System.out.println("==============================");
            System.out.println("CATEGORY MANAGEMENT");
            System.out.println("==============================");
            System.out.println("1 Add Category");
            System.out.println("2 View Categories");
            System.out.println("3 Update Category");
            System.out.println("4 Delete Category");
            System.out.println("5 Back");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> addCategory(scanner);
                    case "2" -> viewCategories();
                    case "3" -> updateCategory(scanner);
                    case "4" -> deleteCategory(scanner);
                    case "5" -> {
                        return;
                    }
                    default -> System.out.println("Invalid selection. Please try again.");
                }
            } catch (AuthenticationException e) {
                System.out.println("Authentication required: " + e.getMessage());
                return;
            } catch (DatabaseException e) {
                System.out.println("Database error: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
            System.out.println();
        }
    }

    private void addCategory(Scanner scanner) throws DatabaseException, AuthenticationException {
        System.out.print("Category name: ");
        String name = scanner.nextLine();
        System.out.print("Category color (optional): ");
        String color = scanner.nextLine();
        controller.createCategory(name, color);
        System.out.println("Category created successfully.");
    }

    private void viewCategories() throws DatabaseException, AuthenticationException {
        Category[] categories = controller.getUserCategories();
        if (categories.length == 0) {
            System.out.println("No categories found.");
            return;
        }
        for (Category category : categories) {
            System.out.println("----------------------------");
            System.out.println("ID: " + category.getId());
            System.out.println("Name: " + safe(category.getName()));
            System.out.println("Color: " + safe(category.getColor()));
            System.out.println("Created At: " + (category.getCreatedAt() == null ? "" : category.getCreatedAt()));
        }
    }

    private void updateCategory(Scanner scanner) throws DatabaseException, AuthenticationException {
        UUID id = readId(scanner, "Category ID to update: ");
        System.out.print("New category name: ");
        String name = scanner.nextLine();
        System.out.print("New category color (optional; leave blank to clear): ");
        String color = scanner.nextLine();
        if (controller.updateCategory(id, name, color)) {
            System.out.println("Category updated successfully.");
        } else {
            System.out.println("Category not found.");
        }
    }

    private void deleteCategory(Scanner scanner) throws DatabaseException, AuthenticationException {
        UUID id = readId(scanner, "Category ID to delete: ");
        System.out.print("Are you sure you want to delete category " + id + "? (y/N): ");
        String confirmation = scanner.nextLine().trim();
        if (!confirmation.equalsIgnoreCase("y") && !confirmation.equalsIgnoreCase("yes")) {
            System.out.println("Aborted.");
            return;
        }
        if (controller.deleteCategory(id)) {
            System.out.println("Category deleted successfully.");
        } else {
            System.out.println("Category not found.");
        }
    }

    private UUID readId(Scanner scanner, String prompt) {
        System.out.print(prompt);
        try {
            return UUID.fromString(scanner.nextLine().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format.");
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

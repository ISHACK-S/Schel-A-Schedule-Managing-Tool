package com.schel.menus;

import com.schel.controllers.AuthController;
import com.schel.exceptions.AuthenticationException;
import com.schel.exceptions.DatabaseException;
import com.schel.models.User;

import java.util.Scanner;

public class LoginMenu {
    private final AuthController authController = AuthController.getInstance();

    public boolean show() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("=========================");
            System.out.println("SCHEL");
            System.out.println("=========================");
            System.out.println("1 Register");
            System.out.println("2 Login");
            System.out.println("3 Exit");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> handleRegister(scanner);
                case "2" -> {
                    boolean loggedIn = handleLogin(scanner);
                    if (loggedIn) {
                        return true;
                    }
                }
                case "3" -> {
                    return false;
                }
                default -> System.out.println("Invalid selection. Please try again.");
            }
            System.out.println();
        }
    }

    private void handleRegister(Scanner scanner) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Enter email: ");
        String email = scanner.nextLine().trim();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
        try {
            User user = authController.register(username, email, password);
            System.out.println("Registration successful. Logged in as " + user.displayName());
        } catch (AuthenticationException | DatabaseException e) {
            System.out.println("Registration failed: " + e.getMessage());
        }
    }

    private boolean handleLogin(Scanner scanner) {
        System.out.print("Username or email: ");
        String identifier = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        try {
            User user = authController.login(identifier, password);
            System.out.println("Login successful. Welcome, " + user.displayName());
            return true;
        } catch (AuthenticationException | DatabaseException e) {
            System.out.println("Login failed: " + e.getMessage());
            return false;
        }
    }
}
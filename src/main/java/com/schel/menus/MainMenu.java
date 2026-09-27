package com.schel.menus;

import com.schel.controllers.AuthController;
import com.schel.utils.ConsoleInput;

import java.util.Scanner;

public class MainMenu {

    private final AuthController authController = AuthController.getInstance();
    private final DashboardMenu dashboardMenu = new DashboardMenu();
    private final ScheduleMenu scheduleMenu = new ScheduleMenu();
    private final CategoryMenu categoryMenu = new CategoryMenu();
    private final ReminderMenu reminderMenu = new ReminderMenu();

    public boolean show() {
        Scanner scanner = ConsoleInput.getScanner();

        while (true) {
            if (!authController.isAuthenticated()) {
                return true;
            }
            String username = authController.getCurrentUser()
                    .map(user -> user.displayName())
                    .orElse("User");

            System.out.println("========================================");
            System.out.println("                  SCHEL");
            System.out.println("          Schedule Management");
            System.out.println("========================================");
            System.out.println("Welcome, " + username);
            System.out.println();
            System.out.println("1 Manage Schedules");
            System.out.println("2 Manage Categories");
            System.out.println("3 Manage Reminders");
            System.out.println("4 View Dashboard");
            System.out.println("5 Profile");
            System.out.println("6 Logout");
            System.out.print("Enter your choice: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> scheduleMenu.show();
                case "2" -> categoryMenu.show();
                case "3" -> reminderMenu.show();
                case "4" -> dashboardMenu.show();
                case "5" -> showProfile();
                case "6" -> {
                    authController.logout();
                    System.out.println("Logged out successfully.");
                    return true;
                }
                default -> System.out.println("Invalid selection. Please try again.");
            }

            System.out.println();
        }
    }

    private void showProfile() {
        authController.getCurrentUser().ifPresent(user -> {
            System.out.println("========================================");
            System.out.println("                 PROFILE");
            System.out.println("========================================");
            System.out.println("Username: " + user.getUsername());
            System.out.println("Email: " + user.getEmail());
            System.out.println("========================================");
        });
    }
}
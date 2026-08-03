package com.schel.menus;

import com.schel.controllers.AuthController;
import com.schel.models.User;

import java.util.Scanner;

public class DashboardMenu {
    private final AuthController authController = AuthController.getInstance();

    public void show() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            User user = authController.getCurrentUser().orElse(null);
            String name = user == null ? "User" : user.displayName();
            System.out.println("Welcome, " + name);
            System.out.println();

            System.out.println("1 Back");
            System.out.println("2 Schedule Manager");
            System.out.println("3 Logout");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> {
                    return; // back to previous menu
                }
                case "2" -> {
                    // Open Schedule Manager
                    ScheduleMenu scheduleMenu = new ScheduleMenu();
                    scheduleMenu.show();
                }
                case "3" -> {
                    authController.logout();
                    System.out.println("Logged out.");
                    return;
                }
                default -> System.out.println("Invalid selection. Please try again.");
            }

            System.out.println();
        }
    }
}
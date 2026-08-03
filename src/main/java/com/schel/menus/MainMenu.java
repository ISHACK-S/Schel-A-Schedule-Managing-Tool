package com.schel.menus;

import com.schel.controllers.AuthController;
import com.schel.models.User;

import java.util.Scanner;

public class MainMenu {

    private final AuthController authController = AuthController.getInstance();
    private final DashboardMenu dashboardMenu = new DashboardMenu();
    private final ScheduleMenu scheduleMenu = new ScheduleMenu();

    public boolean show() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            User user = authController.getCurrentUser().orElse(null);

            System.out.println("=========================");
            System.out.println("SCHEL");
            System.out.println("=========================");

            if (user != null) {
                System.out.println("Welcome, " + user.displayName());
                System.out.println();
            }

            System.out.println("1 Dashboard");
            System.out.println("2 Schedule Manager");
            System.out.println("3 Analytics (Coming Soon)");
            System.out.println("4 Settings (Coming Soon)");
            System.out.println("5 Logout");
            System.out.println("6 Exit");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {

                case "1" -> dashboardMenu.show();

                case "2" -> scheduleMenu.show();

                case "3" ->
                        System.out.println("Analytics feature is under development.");

                case "4" ->
                        System.out.println("Settings feature is under development.");

                case "5" -> {
                    authController.logout();
                    System.out.println("Logged out successfully.");
                    return true;
                }

                case "6" -> {
                    return false;
                }

                default ->
                        System.out.println("Invalid selection. Please try again.");
            }

            System.out.println();
        }
    }
}
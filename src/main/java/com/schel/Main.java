package com.schel;

import com.schel.api.AuthApiServer;
import com.schel.config.DatabaseConfig;
import com.schel.menus.LoginMenu;
import com.schel.menus.MainMenu;

public final class Main {
    public static void main(String[] args) {
        try {
            DatabaseConfig.getInstance();
            AuthApiServer.start(8080);
            System.out.println("SCHEL auth API running at http://localhost:8080/api");
        } catch (Exception e) {
            System.err.println("Failed to start the Schel auth API server: " + e.getMessage());
        }

        LoginMenu loginMenu = new LoginMenu();
        MainMenu mainMenu = new MainMenu();

        boolean running = true;
        while (running) {
            boolean loggedIn = loginMenu.show();
            if (!loggedIn) {
                System.out.println("Goodbye.");
                break;
            }
            boolean backToLogin = mainMenu.show();
            if (!backToLogin) {
                System.out.println("Goodbye.");
                break;
            }
        }
    }
}
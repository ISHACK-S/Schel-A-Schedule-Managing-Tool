package com.schel;

import com.schel.config.DatabaseConfig;
import com.schel.controllers.AuthController;
import com.schel.menus.LoginMenu;
import com.schel.menus.MainMenu;

public final class Main {
    public static void main(String[] args) {
        try {
            DatabaseConfig.getInstance();
        } catch (IllegalStateException e) {
            System.err.println("Missing SUPABASE_URL or SUPABASE_API_KEY environment variables. Set them and restart the application.");
            System.exit(1);
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
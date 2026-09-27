package com.schel.menus;

import com.schel.controllers.ReminderController;
import com.schel.controllers.ScheduleController;
import com.schel.exceptions.AuthenticationException;
import com.schel.exceptions.DatabaseException;
import com.schel.models.Reminder;
import com.schel.models.Schedule;
import com.schel.utils.ConsoleInput;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Scanner;
import java.util.UUID;

public class ReminderMenu {

    private static final DateTimeFormatter INPUT_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm");

    private final ReminderController reminderController = ReminderController.getInstance();
    private final ScheduleController scheduleController = ScheduleController.getInstance();

    public void show() {
        Scanner scanner = ConsoleInput.getScanner();
        while (true) {
            System.out.println("==============================");
            System.out.println("REMINDER MANAGEMENT");
            System.out.println("==============================");
            System.out.println("1 Add Reminder");
            System.out.println("2 View Reminders");
            System.out.println("3 Update Reminder");
            System.out.println("4 Delete Reminder");
            System.out.println("5 Back");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> addReminder(scanner);
                    case "2" -> viewReminders();
                    case "3" -> updateReminder(scanner);
                    case "4" -> deleteReminder(scanner);
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

    private void addReminder(Scanner scanner) throws DatabaseException, AuthenticationException {
        Schedule[] schedules = scheduleController.viewSchedules();
        if (schedules.length == 0) {
            System.out.println("No schedules found. Create a schedule before adding a reminder.");
            return;
        }
        for (int i = 0; i < schedules.length; i++) {
            System.out.println((i + 1) + ". " + schedules[i].getTitle() + " (ID: " + schedules[i].getId() + ")");
        }
        System.out.print("Select a schedule number: ");
        int selected;
        try {
            selected = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid schedule selection.");
        }
        if (selected < 1 || selected > schedules.length) {
            throw new IllegalArgumentException("Invalid schedule selection.");
        }
        System.out.print("Reminder date/time (YYYY-MM-DD HH:mm): ");
        OffsetDateTime reminderTime = parseDateTime(scanner.nextLine().trim());
        reminderController.createReminder(schedules[selected - 1].getId(), reminderTime);
        System.out.println("Reminder created successfully.");
    }

    private void viewReminders() throws DatabaseException, AuthenticationException {
        Reminder[] reminders = reminderController.getUserReminders();
        if (reminders.length == 0) {
            System.out.println("No reminders found.");
            return;
        }
        for (Reminder reminder : reminders) {
            System.out.println("----------------------------");
            System.out.println("Reminder ID: " + reminder.getId());
            System.out.println("Schedule ID: " + reminder.getScheduleId());
            System.out.println("Reminder Time: " + formatDateTime(reminder.getReminderTime()));
            System.out.println("Status: " + (reminder.isSent() ? "Sent" : "Pending"));
        }
    }

    private void updateReminder(Scanner scanner) throws DatabaseException, AuthenticationException {
        UUID id = readId(scanner, "Reminder ID to update: ");
        System.out.print("New reminder date/time (YYYY-MM-DD HH:mm): ");
        OffsetDateTime reminderTime = parseDateTime(scanner.nextLine().trim());
        if (reminderController.updateReminder(id, reminderTime)) {
            System.out.println("Reminder updated successfully.");
        } else {
            System.out.println("Reminder not found.");
        }
    }

    private void deleteReminder(Scanner scanner) throws DatabaseException, AuthenticationException {
        UUID id = readId(scanner, "Reminder ID to delete: ");
        System.out.print("Are you sure you want to delete reminder " + id + "? (y/N): ");
        String confirmation = scanner.nextLine().trim();
        if (!confirmation.equalsIgnoreCase("y") && !confirmation.equalsIgnoreCase("yes")) {
            System.out.println("Aborted.");
            return;
        }
        if (reminderController.deleteReminder(id)) {
            System.out.println("Reminder deleted successfully.");
        } else {
            System.out.println("Reminder not found.");
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

    private OffsetDateTime parseDateTime(String value) {
        try {
            return LocalDateTime.parse(value, INPUT_FORMAT)
                    .atZone(ZoneId.systemDefault())
                    .toOffsetDateTime();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid reminder date/time. Expected YYYY-MM-DD HH:mm.");
        }
    }

    private String formatDateTime(OffsetDateTime value) {
        return value == null ? "" : value.atZoneSameInstant(ZoneId.systemDefault()).format(DISPLAY_FORMAT);
    }
}

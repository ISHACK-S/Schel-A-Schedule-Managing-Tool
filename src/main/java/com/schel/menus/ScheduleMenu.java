package com.schel.menus;

import com.schel.controllers.ScheduleController;
import com.schel.exceptions.AuthenticationException;
import com.schel.exceptions.DatabaseException;
import com.schel.models.Schedule;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Scanner;
import java.util.UUID;

public class ScheduleMenu {

    private final ScheduleController controller = ScheduleController.getInstance();

    public void show() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("==============================");
            System.out.println("SCHEDULE MANAGER");
            System.out.println("==============================");
            System.out.println("1 Add Schedule");
            System.out.println("2 View All Schedules");
            System.out.println("3 Search Schedule");
            System.out.println("4 Update Schedule");
            System.out.println("5 Delete Schedule");
            System.out.println("6 Back");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> handleAdd(scanner);
                    case "2" -> handleView();
                    case "3" -> handleSearch(scanner);
                    case "4" -> handleUpdate(scanner);
                    case "5" -> handleDelete(scanner);
                    case "6" -> {
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
                System.out.println("Invalid input: " + e.getMessage());
            }

            System.out.println();
        }
    }

    private void handleAdd(Scanner scanner) throws DatabaseException, AuthenticationException {
        System.out.print("Title: ");
        String title = scanner.nextLine().trim();

        System.out.print("Description: ");
        String description = scanner.nextLine().trim();

        System.out.print("Task Date (YYYY-MM-DD): ");
        String dateStr = scanner.nextLine().trim();

        System.out.print("Start Time (HH:mm): ");
        String startStr = scanner.nextLine().trim();

        System.out.print("End Time (HH:mm): ");
        String endStr = scanner.nextLine().trim();

        String priority = selectPriority(scanner, null);

        String status = selectStatus(scanner, null);

        LocalDate taskDate;
        LocalTime startTime;
        LocalTime endTime;
        try {
            taskDate = LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid task date format. Expected YYYY-MM-DD");
        }
        try {
            startTime = LocalTime.parse(startStr);
            endTime = LocalTime.parse(endStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid time format. Expected HH:mm");
        }

        Schedule s = new Schedule();
        s.setTitle(title);
        s.setDescription(description);
        s.setTaskDate(taskDate);
        s.setStartTime(startTime);
        s.setEndTime(endTime);
        s.setPriority(priority);
        s.setStatus(status);

        Schedule created = controller.addSchedule(s);
        if (created != null) {
            System.out.println("Schedule added: " + created.getId());
        } else {
            System.out.println("Schedule created but could not be retrieved immediately.");
        }
    }

    private void handleView() throws DatabaseException, AuthenticationException {
        Schedule[] arr = controller.viewSchedules();
        if (arr == null || arr.length == 0) {
            System.out.println("No schedules found.");
            return;
        }
        for (Schedule s : arr) {
            printScheduleSummary(s);
        }
    }

    private void handleSearch(Scanner scanner) throws DatabaseException, AuthenticationException {
        System.out.print("Search keyword (title): ");
        String keyword = scanner.nextLine().trim();
        Schedule[] arr = controller.searchSchedules(keyword);
        if (arr == null || arr.length == 0) {
            System.out.println("No schedules found for '" + keyword + "'.");
            return;
        }
        for (Schedule s : arr) {
            printScheduleSummary(s);
        }
    }

    private void handleUpdate(Scanner scanner) throws DatabaseException, AuthenticationException {
        System.out.print("Schedule ID to update: ");
        String idStr = scanner.nextLine().trim();
        UUID id;
        try {
            id = UUID.fromString(idStr);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid UUID format.");
            return;
        }

        // find existing
        Schedule existing = null;
        Schedule[] all = controller.viewSchedules();
        for (Schedule s : all) {
            if (s.getId() != null && s.getId().equals(id)) {
                existing = s;
                break;
            }
        }
        if (existing == null) {
            System.out.println("Schedule not found.");
            return;
        }

        System.out.println("Leave empty to keep current value.");
        System.out.print("Title [" + safe(existing.getTitle()) + "]: ");
        String title = scanner.nextLine().trim();
        if (title.isEmpty()) title = existing.getTitle();

        System.out.print("Description [" + safe(existing.getDescription()) + "]: ");
        String description = scanner.nextLine().trim();
        if (description.isEmpty()) description = existing.getDescription();

        System.out.print("Task Date (YYYY-MM-DD) [" + (existing.getTaskDate() != null ? existing.getTaskDate().toString() : "") + "]: ");
        String dateStr = scanner.nextLine().trim();
        LocalDate taskDate = existing.getTaskDate();
        if (!dateStr.isEmpty()) {
            try {
                taskDate = LocalDate.parse(dateStr);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format.");
                return;
            }
        }

        System.out.print("Start Time (HH:mm) [" + (existing.getStartTime() != null ? existing.getStartTime().toString() : "") + "]: ");
        String startStr = scanner.nextLine().trim();
        LocalTime start = existing.getStartTime();
        if (!startStr.isEmpty()) {
            try {
                start = LocalTime.parse(startStr);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid time format.");
                return;
            }
        }

        System.out.print("End Time (HH:mm) [" + (existing.getEndTime() != null ? existing.getEndTime().toString() : "") + "]: ");
        String endStr = scanner.nextLine().trim();
        LocalTime end = existing.getEndTime();
        if (!endStr.isEmpty()) {
            try {
                end = LocalTime.parse(endStr);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid time format.");
                return;
            }
        }

        String priority = selectPriority(scanner, existing.getPriority());

        String status = selectStatus(scanner, existing.getStatus());

        Schedule updated = new Schedule();
        updated.setId(existing.getId());
        updated.setUserId(existing.getUserId());
        updated.setCategoryId(existing.getCategoryId());
        updated.setTitle(title);
        updated.setDescription(description);
        updated.setTaskDate(taskDate);
        updated.setStartTime(start);
        updated.setEndTime(end);
        updated.setPriority(priority);
        updated.setStatus(status);
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setUpdatedAt(LocalDateTime.now());

        Schedule result = controller.updateSchedule(updated);
        System.out.println("Schedule updated: " + result.getId());
    }

    private void handleDelete(Scanner scanner) throws DatabaseException, AuthenticationException {
        System.out.print("Schedule ID to delete: ");
        String idStr = scanner.nextLine().trim();
        UUID id;
        try {
            id = UUID.fromString(idStr);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid UUID format.");
            return;
        }
        System.out.print("Are you sure you want to delete schedule " + id + "? (y/N): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (!confirm.equals("y") && !confirm.equals("yes")) {
            System.out.println("Aborted.");
            return;
        }
        controller.deleteSchedule(id);
        System.out.println("Schedule deleted.");
    }

    private void printScheduleSummary(Schedule s) {
        System.out.println("----------------------------");
        System.out.println("ID: " + s.getId());
        System.out.println("Title: " + safe(s.getTitle()));
        System.out.println("Date: " + (s.getTaskDate() != null ? s.getTaskDate().toString() : ""));
        System.out.println("Time: " + (s.getStartTime() != null ? s.getStartTime().toString() : "") + " - " + (s.getEndTime() != null ? s.getEndTime().toString() : ""));
        System.out.println("Priority: " + safe(s.getPriority()));
        System.out.println("Status: " + safe(s.getStatus()));
        System.out.println("Description: " + safe(s.getDescription()));
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String selectPriority(Scanner scanner, String current) {
        while (true) {
            System.out.println("Priority:");
            System.out.println("1 LOW");
            System.out.println("2 MEDIUM");
            System.out.println("3 HIGH");
            if (current != null) {
                System.out.print("Choose an option (press Enter to keep '" + current + "'): ");
            } else {
                System.out.print("Choose an option: ");
            }
            String input = scanner.nextLine().trim();
            if (input.isEmpty() && current != null) {
                return current;
            }
            switch (input) {
                case "1" -> {
                    return "LOW";
                }
                case "2" -> {
                    return "MEDIUM";
                }
                case "3" -> {
                    return "HIGH";
                }
                default -> System.out.println("Invalid selection. Please enter 1, 2 or 3.");
            }
        }
    }

    private String selectStatus(Scanner scanner, String current) {
        while (true) {
            System.out.println("Status:");
            System.out.println("1 PENDING");
            System.out.println("2 COMPLETED");
            System.out.println("3 MISSED");
            if (current != null) {
                System.out.print("Choose an option (press Enter to keep '" + current + "'): ");
            } else {
                System.out.print("Choose an option: ");
            }
            String input = scanner.nextLine().trim();
            if (input.isEmpty() && current != null) {
                return current;
            }
            switch (input) {
                case "1" -> {
                    return "PENDING";
                }
                case "2" -> {
                    return "COMPLETED";
                }
                case "3" -> {
                    return "MISSED";
                }
                default -> System.out.println("Invalid selection. Please enter 1, 2 or 3.");
            }
        }
    }
}

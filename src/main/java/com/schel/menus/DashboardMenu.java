package com.schel.menus;

import com.schel.controllers.CategoryController;
import com.schel.controllers.AuthController;
import com.schel.controllers.ReminderController;
import com.schel.controllers.ScheduleController;
import com.schel.exceptions.AuthenticationException;
import com.schel.exceptions.DatabaseException;
import com.schel.models.Category;
import com.schel.models.Reminder;
import com.schel.models.Schedule;
import com.schel.models.User;

public class DashboardMenu {
    private final AuthController authController = AuthController.getInstance();
    private final ScheduleController scheduleController = ScheduleController.getInstance();
    private final CategoryController categoryController = CategoryController.getInstance();
    private final ReminderController reminderController = ReminderController.getInstance();

    public void show() {
        User user = authController.getCurrentUser().orElse(null);
        if (user == null) {
            System.out.println("Authentication required.");
            return;
        }
        try {
            Schedule[] schedules = scheduleController.viewSchedules();
            Category[] categories = categoryController.getUserCategories();
            Reminder[] reminders = reminderController.getUserReminders(schedules);

            int pending = countSchedulesWithStatus(schedules, "PENDING");
            int completed = countSchedulesWithStatus(schedules, "COMPLETED");
            int missed = countSchedulesWithStatus(schedules, "MISSED");

            System.out.println("========================================");
            System.out.println("             SCHEL DASHBOARD");
            System.out.println("========================================");
            System.out.println("User: " + user.displayName());
            System.out.println();
            System.out.println("Schedules");
            System.out.println("---------");
            System.out.println("Total: " + schedules.length);
            System.out.println("Pending: " + pending);
            System.out.println("Completed: " + completed);
            System.out.println("Missed: " + missed);
            System.out.println();
            System.out.println("Categories");
            System.out.println("----------");
            System.out.println("Total: " + categories.length);
            System.out.println();
            System.out.println("Reminders");
            System.out.println("---------");
            System.out.println("Total: " + reminders.length);
            System.out.println("========================================");
        } catch (AuthenticationException e) {
            System.out.println("Authentication required: " + e.getMessage());
        } catch (DatabaseException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private int countSchedulesWithStatus(Schedule[] schedules, String status) {
        int count = 0;
        for (Schedule schedule : schedules) {
            if (status.equalsIgnoreCase(schedule.getStatus())) {
                count++;
            }
        }
        return count;
    }
}
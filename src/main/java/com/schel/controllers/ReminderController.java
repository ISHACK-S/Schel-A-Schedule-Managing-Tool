package com.schel.controllers;

import com.schel.exceptions.AuthenticationException;
import com.schel.exceptions.DatabaseException;
import com.schel.models.Reminder;
import com.schel.models.Schedule;
import com.schel.services.ReminderService;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class ReminderController {

    private final ReminderService service;

    private static final ReminderController INSTANCE = new ReminderController();

    private ReminderController() {
        this.service = ReminderService.getInstance();
    }

    public static ReminderController getInstance() {
        return INSTANCE;
    }

    public void createReminder(UUID scheduleId, OffsetDateTime reminderTime)
            throws DatabaseException, AuthenticationException {
        service.createReminder(scheduleId, reminderTime);
    }

    public Reminder[] getUserReminders() throws DatabaseException, AuthenticationException {
        return service.getUserReminders();
    }

    public Reminder[] getUserReminders(Schedule[] userSchedules)
            throws DatabaseException, AuthenticationException {
        return service.getUserReminders(userSchedules);
    }

    public boolean updateReminder(UUID id, OffsetDateTime reminderTime)
            throws DatabaseException, AuthenticationException {
        return service.updateReminder(id, reminderTime);
    }

    public boolean deleteReminder(UUID id) throws DatabaseException, AuthenticationException {
        return service.deleteReminder(id);
    }
}

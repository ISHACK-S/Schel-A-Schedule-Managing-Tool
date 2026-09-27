package com.schel.services;

import com.schel.controllers.AuthController;
import com.schel.exceptions.AuthenticationException;
import com.schel.exceptions.DatabaseException;
import com.schel.models.Reminder;
import com.schel.models.Schedule;
import com.schel.models.User;
import com.schel.repository.ReminderRepository;
import com.schel.repository.ScheduleRepository;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ReminderService {

    private final ReminderRepository reminderRepository;
    private final ScheduleRepository scheduleRepository;
    private final AuthController authController;

    private static final ReminderService INSTANCE = new ReminderService();

    private ReminderService() {
        this.reminderRepository = ReminderRepository.getInstance();
        this.scheduleRepository = ScheduleRepository.getInstance();
        this.authController = AuthController.getInstance();
    }

    public static ReminderService getInstance() {
        return INSTANCE;
    }

    public void createReminder(UUID scheduleId, OffsetDateTime reminderTime)
            throws DatabaseException, AuthenticationException {
        User user = currentUser();
        validateReminderTime(reminderTime);
        Schedule schedule = scheduleRepository.getScheduleById(user.getId(), scheduleId);
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule not found.");
        }
        Reminder reminder = new Reminder();
        reminder.setScheduleId(schedule.getId());
        reminder.setReminderTime(reminderTime);
        reminder.setSent(false);
        reminderRepository.create(reminder);
    }

    public Reminder[] getUserReminders() throws DatabaseException, AuthenticationException {
        Schedule[] schedules = scheduleRepository.getAllSchedules(currentUser().getId());
        return getUserReminders(schedules);
    }

    public Reminder[] getUserReminders(Schedule[] userSchedules)
            throws DatabaseException, AuthenticationException {
        User user = currentUser();
        if (userSchedules == null || userSchedules.length == 0) {
            return new Reminder[0];
        }
        for (Schedule schedule : userSchedules) {
            if (schedule == null || !user.getId().equals(schedule.getUserId())) {
                throw new IllegalArgumentException("Schedules must belong to the current user.");
            }
        }
        List<UUID> scheduleIds = Arrays.stream(userSchedules)
                .map(Schedule::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return reminderRepository.findByScheduleIds(scheduleIds);
    }

    public boolean updateReminder(UUID id, OffsetDateTime reminderTime)
            throws DatabaseException, AuthenticationException {
        Objects.requireNonNull(id, "Reminder ID is required.");
        validateReminderTime(reminderTime);
        List<UUID> scheduleIds = getOwnedScheduleIds();
        Reminder updated = new Reminder();
        updated.setReminderTime(reminderTime);
        return reminderRepository.update(scheduleIds, id, updated);
    }

    public boolean deleteReminder(UUID id) throws DatabaseException, AuthenticationException {
        Objects.requireNonNull(id, "Reminder ID is required.");
        return reminderRepository.delete(getOwnedScheduleIds(), id);
    }

    private List<UUID> getOwnedScheduleIds() throws DatabaseException, AuthenticationException {
        Schedule[] schedules = scheduleRepository.getAllSchedules(currentUser().getId());
        return Arrays.stream(schedules)
                .map(Schedule::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private User currentUser() throws AuthenticationException {
        return authController.getCurrentUser()
                .orElseThrow(() -> new AuthenticationException("User must be logged in"));
    }

    private void validateReminderTime(OffsetDateTime reminderTime) {
        if (reminderTime == null) {
            throw new IllegalArgumentException("Invalid reminder date/time.");
        }
    }
}

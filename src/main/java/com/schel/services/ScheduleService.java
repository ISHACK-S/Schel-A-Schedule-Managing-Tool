package com.schel.services;

import com.schel.controllers.AuthController;
import com.schel.exceptions.AuthenticationException;
import com.schel.exceptions.DatabaseException;
import com.schel.models.Schedule;
import com.schel.models.User;
import com.schel.repository.ScheduleRepository;

import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

public final class ScheduleService {

    private final ScheduleRepository repository;
    private final AuthController authController;

    private static final ScheduleService INSTANCE = new ScheduleService();

    private ScheduleService() {
        this.repository = ScheduleRepository.getInstance();
        this.authController = AuthController.getInstance();
    }

    public static ScheduleService getInstance() {
        return INSTANCE;
    }

    public Schedule addSchedule(Schedule schedule) throws DatabaseException, AuthenticationException {
        Objects.requireNonNull(schedule, "Schedule cannot be null");
        User user = authController.getCurrentUser().orElseThrow(() -> new AuthenticationException("User must be logged in"));

        validateScheduleInput(schedule);

        // ensure schedule is assigned to current user
        schedule.setUserId(user.getId());

        return repository.createSchedule(schedule);
    }

    public Schedule[] viewSchedules() throws DatabaseException, AuthenticationException {
        User user = authController.getCurrentUser().orElseThrow(() -> new AuthenticationException("User must be logged in"));
        return repository.getAllSchedules(user.getId());
    }

    public Schedule[] searchSchedules(String keyword) throws DatabaseException, AuthenticationException {
        User user = authController.getCurrentUser().orElseThrow(() -> new AuthenticationException("User must be logged in"));
        return repository.searchSchedule(user.getId(), keyword);
    }

    public Schedule updateSchedule(Schedule schedule) throws DatabaseException, AuthenticationException {
        Objects.requireNonNull(schedule, "Schedule cannot be null");
        if (schedule.getId() == null) {
            throw new IllegalArgumentException("Schedule id is required for update");
        }
        User user = authController.getCurrentUser().orElseThrow(() -> new AuthenticationException("User must be logged in"));

        // Ensure schedule belongs to current user
        Schedule existing = repository.getScheduleById(schedule.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Schedule not found");
        }
        if (!user.getId().equals(existing.getUserId())) {
            throw new IllegalArgumentException("Cannot modify another user's schedule");
        }

        // Keep userId consistent
        schedule.setUserId(user.getId());

        validateScheduleInput(schedule);

        return repository.updateSchedule(schedule);
    }

    public void deleteSchedule(UUID scheduleId) throws DatabaseException, AuthenticationException {
        if (scheduleId == null) return;
        User user = authController.getCurrentUser().orElseThrow(() -> new AuthenticationException("User must be logged in"));
        Schedule existing = repository.getScheduleById(scheduleId);
        if (existing == null) {
            throw new IllegalArgumentException("Schedule not found");
        }
        if (!user.getId().equals(existing.getUserId())) {
            throw new IllegalArgumentException("Cannot delete another user's schedule");
        }
        repository.deleteSchedule(scheduleId);
    }

    private void validateScheduleInput(Schedule schedule) {
        if (schedule.getTitle() == null || schedule.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }
        if (schedule.getTaskDate() == null) {
            throw new IllegalArgumentException("Task date is required");
        }
        LocalTime s = schedule.getStartTime();
        LocalTime e = schedule.getEndTime();
        if (s == null || e == null) {
            throw new IllegalArgumentException("Start time and end time are required");
        }
        if (!s.isBefore(e)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
    }
}

package com.schel.services;

import com.schel.controllers.AuthController;
import com.schel.exceptions.AuthenticationException;
import com.schel.exceptions.DatabaseException;
import com.schel.models.Schedule;
import com.schel.models.User;
import com.schel.repository.ScheduleRepository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class ScheduleService {

    private static final Set<String> PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH");
    private static final Set<String> STATUSES = Set.of("PENDING", "COMPLETED", "MISSED");

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
        return repository.searchByTitle(user.getId(), keyword);
    }

    public Schedule[] searchSchedulesByDate(String date) throws DatabaseException, AuthenticationException {
        User user = authController.getCurrentUser().orElseThrow(() -> new AuthenticationException("User must be logged in"));
        return repository.searchByDate(user.getId(), date);
    }

    public Schedule[] searchSchedulesByStatus(String status) throws DatabaseException, AuthenticationException {
        User user = authController.getCurrentUser().orElseThrow(() -> new AuthenticationException("User must be logged in"));
        return repository.searchByStatus(user.getId(), status);
    }

    public Schedule updateSchedule(Schedule schedule) throws DatabaseException, AuthenticationException {
        Objects.requireNonNull(schedule, "Schedule cannot be null");
        if (schedule.getId() == null) {
            throw new IllegalArgumentException("Schedule id is required for update");
        }
        User user = authController.getCurrentUser().orElseThrow(() -> new AuthenticationException("User must be logged in"));

        Schedule existing = repository.getScheduleById(user.getId(), schedule.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Schedule not found.");
        }

        schedule.setUserId(user.getId());
        validateScheduleInput(schedule);
        schedule.setUpdatedAt(LocalDateTime.now());

        Schedule updated = repository.updateSchedule(user.getId(), schedule);
        if (updated == null) {
            throw new IllegalArgumentException("Schedule not found.");
        }
        return updated;
    }

    public void deleteSchedule(UUID scheduleId) throws DatabaseException, AuthenticationException {
        if (scheduleId == null) {
            throw new IllegalArgumentException("Schedule ID is required.");
        }
        User user = authController.getCurrentUser().orElseThrow(() -> new AuthenticationException("User must be logged in"));
        Schedule existing = repository.getScheduleById(user.getId(), scheduleId);
        if (existing == null) {
            throw new IllegalArgumentException("Schedule not found.");
        }
        repository.deleteSchedule(user.getId(), scheduleId);
    }

    private void validateScheduleInput(Schedule schedule) {
        if (schedule.getTitle() == null || schedule.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title is required.");
        }
        if (schedule.getTaskDate() == null) {
            throw new IllegalArgumentException("Invalid date format.");
        }
        LocalTime s = schedule.getStartTime();
        LocalTime e = schedule.getEndTime();
        if (s == null || e == null) {
            throw new IllegalArgumentException("Invalid time format.");
        }
        if (!s.isBefore(e)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
        if (schedule.getPriority() == null || !PRIORITIES.contains(schedule.getPriority())) {
            throw new IllegalArgumentException("Invalid priority.");
        }
        if (schedule.getStatus() == null || !STATUSES.contains(schedule.getStatus())) {
            throw new IllegalArgumentException("Invalid status.");
        }
    }
}

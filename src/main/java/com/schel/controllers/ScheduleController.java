package com.schel.controllers;

import com.schel.exceptions.AuthenticationException;
import com.schel.exceptions.DatabaseException;
import com.schel.models.Schedule;
import com.schel.services.ScheduleService;

public final class ScheduleController {

    private final ScheduleService service;

    private static final ScheduleController INSTANCE = new ScheduleController();

    private ScheduleController() {
        this.service = ScheduleService.getInstance();
    }

    public static ScheduleController getInstance() {
        return INSTANCE;
    }

    public Schedule addSchedule(Schedule schedule) throws DatabaseException, AuthenticationException {
        return service.addSchedule(schedule);
    }

    public Schedule[] viewSchedules() throws DatabaseException, AuthenticationException {
        return service.viewSchedules();
    }

    public Schedule[] searchSchedules(String keyword) throws DatabaseException, AuthenticationException {
        return service.searchSchedules(keyword);
    }

    public Schedule updateSchedule(Schedule schedule) throws DatabaseException, AuthenticationException {
        return service.updateSchedule(schedule);
    }

    public void deleteSchedule(java.util.UUID scheduleId) throws DatabaseException, AuthenticationException {
        service.deleteSchedule(scheduleId);
    }
}

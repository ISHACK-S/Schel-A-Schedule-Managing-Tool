package com.schel.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schel.database.SupabaseClient;
import com.schel.exceptions.DatabaseException;
import com.schel.models.Schedule;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ScheduleRepository {

    private static final String TABLE = "schedules";

    private final SupabaseClient client;
    private final ObjectMapper mapper;

    private static final ScheduleRepository INSTANCE = new ScheduleRepository();

    private ScheduleRepository() {
        this.client = SupabaseClient.getInstance();
        this.mapper = new ObjectMapper();
        this.mapper.findAndRegisterModules();
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static ScheduleRepository getInstance() {
        return INSTANCE;
    }

    public Schedule createSchedule(Schedule schedule) throws DatabaseException {
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule cannot be null");
        }
        String response = client.postReturning(TABLE, schedule);
        Schedule[] results = parseSchedules(response);
        return results.length == 0 ? null : results[0];
    }

    public Schedule[] getAllSchedules(UUID userId) throws DatabaseException {
        if (userId == null) return new Schedule[0];
        Map<String, String> params = new HashMap<>();
        params.put("user_id", "eq." + userId);
        params.put("order", "task_date.asc,start_time.asc");
        return getSchedules(params);
    }

    public Schedule getScheduleById(UUID userId, UUID scheduleId) throws DatabaseException {
        if (userId == null || scheduleId == null) return null;
        Map<String, String> params = new HashMap<>();
        params.put("id", "eq." + scheduleId);
        params.put("user_id", "eq." + userId);
        Schedule[] arr = getSchedules(params);
        return arr.length == 0 ? null : arr[0];
    }

    public Schedule[] searchByTitle(UUID userId, String title) throws DatabaseException {
        if (userId == null) return new Schedule[0];
        Map<String, String> params = new HashMap<>();
        params.put("user_id", "eq." + userId);
        params.put("title", "ilike.%" + title + "%");
        return getSchedules(params);
    }

    public Schedule[] searchByDate(UUID userId, String date) throws DatabaseException {
        if (userId == null) return new Schedule[0];
        Map<String, String> params = new HashMap<>();
        params.put("user_id", "eq." + userId);
        params.put("task_date", "eq." + date);
        return getSchedules(params);
    }

    public Schedule[] searchByStatus(UUID userId, String status) throws DatabaseException {
        if (userId == null) return new Schedule[0];
        Map<String, String> params = new HashMap<>();
        params.put("user_id", "eq." + userId);
        params.put("status", "eq." + status);
        return getSchedules(params);
    }

    public Schedule updateSchedule(UUID userId, Schedule schedule) throws DatabaseException {
        if (userId == null || schedule == null || schedule.getId() == null) {
            throw new IllegalArgumentException("User and schedule id are required for update");
        }
        String path = TABLE + "?id=eq." + schedule.getId() + "&user_id=eq." + userId;
        Map<String, Object> fields = new HashMap<>();
        fields.put("title", schedule.getTitle());
        fields.put("description", schedule.getDescription());
        fields.put("task_date", schedule.getTaskDate());
        fields.put("start_time", schedule.getStartTime());
        fields.put("end_time", schedule.getEndTime());
        fields.put("priority", schedule.getPriority());
        fields.put("status", schedule.getStatus());
        fields.put("updated_at", schedule.getUpdatedAt());
        client.patch(path, fields);
        return getScheduleById(userId, schedule.getId());
    }

    public void deleteSchedule(UUID userId, UUID scheduleId) throws DatabaseException {
        if (userId == null || scheduleId == null) return;
        String path = TABLE + "?id=eq." + scheduleId + "&user_id=eq." + userId;
        client.delete(path);
    }

    private Schedule[] getSchedules(Map<String, String> params) throws DatabaseException {
        String response = client.get(TABLE, params);
        return parseSchedules(response);
    }

    private Schedule[] parseSchedules(String response) throws DatabaseException {
        try {
            return mapper.readValue(response, Schedule[].class);
        } catch (JsonProcessingException e) {
            throw new DatabaseException("Failed to parse schedules response", e);
        }
    }
}

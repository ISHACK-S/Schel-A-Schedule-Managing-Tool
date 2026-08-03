package com.schel.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schel.config.Constants;
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
        client.post(TABLE, schedule);
        // Try to find created schedule by user and title (best-effort)
        Schedule[] results = searchSchedule(schedule.getUserId(), schedule.getTitle());
        return results.length == 0 ? null : results[0];
    }

    public Schedule[] getAllSchedules(UUID userId) throws DatabaseException {
        if (userId == null) return new Schedule[0];
        Map<String, String> params = new HashMap<>();
        params.put("user_id", "eq." + userId);
        String resp = client.get(TABLE, params);
        try {
            return mapper.readValue(resp, Schedule[].class);
        } catch (JsonProcessingException e) {
            System.out.println("Supabase Response:");
            System.out.println(resp);
            System.out.println("Jackson exception while parsing schedules response:");
            e.printStackTrace(System.out);
            throw new DatabaseException("Failed to parse schedules response: " + e.getMessage(), e);
        }
    }

    public Schedule getScheduleById(UUID scheduleId) throws DatabaseException {
        if (scheduleId == null) return null;
        Map<String, String> params = new HashMap<>();
        params.put("id", "eq." + scheduleId);
        String resp = client.get(TABLE, params);
        try {
            Schedule[] arr = mapper.readValue(resp, Schedule[].class);
            return arr.length == 0 ? null : arr[0];
        } catch (JsonProcessingException e) {
            System.out.println("Supabase Response:");
            System.out.println(resp);
            System.out.println("Jackson exception while parsing schedules response:");
            e.printStackTrace(System.out);
            throw new DatabaseException("Failed to parse schedules response: " + e.getMessage(), e);
        }
    }

    public Schedule[] searchSchedule(UUID userId, String keyword) throws DatabaseException {
        if (userId == null) return new Schedule[0];
        if (keyword == null) keyword = "";
        Map<String, String> params = new HashMap<>();
        params.put("user_id", "eq." + userId);
        // use ilike for case-insensitive partial match
        params.put("title", "ilike.%" + keyword + "%");
        String resp = client.get(TABLE, params);
        try {
            return mapper.readValue(resp, Schedule[].class);
        } catch (JsonProcessingException e) {
            System.out.println("Supabase Response:");
            System.out.println(resp);
            System.out.println("Jackson exception while parsing schedules response:");
            e.printStackTrace(System.out);
            throw new DatabaseException("Failed to parse schedules response: " + e.getMessage(), e);
        }
    }

    public Schedule updateSchedule(Schedule schedule) throws DatabaseException {
        if (schedule == null || schedule.getId() == null) {
            throw new IllegalArgumentException("Schedule or schedule id cannot be null");
        }
        String path = TABLE + "?id=eq." + schedule.getId();
        client.patch(path, schedule);
        return getScheduleById(schedule.getId());
    }

    public void deleteSchedule(UUID scheduleId) throws DatabaseException {
        if (scheduleId == null) return;
        String path = TABLE + "?id=eq." + scheduleId;
        client.delete(path);
    }
}

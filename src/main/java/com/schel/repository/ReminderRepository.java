package com.schel.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schel.database.SupabaseClient;
import com.schel.exceptions.DatabaseException;
import com.schel.models.Reminder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ReminderRepository {

    private static final String TABLE = "reminders";

    private final SupabaseClient client;
    private final ObjectMapper mapper;

    private static final ReminderRepository INSTANCE = new ReminderRepository();

    private ReminderRepository() {
        this.client = SupabaseClient.getInstance();
        this.mapper = new ObjectMapper();
        this.mapper.findAndRegisterModules();
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static ReminderRepository getInstance() {
        return INSTANCE;
    }

    public void create(Reminder reminder) throws DatabaseException {
        if (reminder == null) {
            throw new IllegalArgumentException("Reminder cannot be null.");
        }
        client.post(TABLE, reminder);
    }

    public Reminder findById(List<UUID> ownedScheduleIds, UUID id) throws DatabaseException {
        if (id == null || ownedScheduleIds == null || ownedScheduleIds.isEmpty()) {
            return null;
        }
        Map<String, String> params = new HashMap<>();
        params.put("id", "eq." + id);
        params.put("schedule_id", "in.(" + formatIds(ownedScheduleIds) + ")");
        Reminder[] reminders = getReminders(params);
        return reminders.length == 0 ? null : reminders[0];
    }

    public Reminder[] findByScheduleId(UUID scheduleId) throws DatabaseException {
        if (scheduleId == null) {
            return new Reminder[0];
        }
        Map<String, String> params = new HashMap<>();
        params.put("schedule_id", "eq." + scheduleId);
        return getReminders(params);
    }

    public Reminder[] findByScheduleIds(List<UUID> scheduleIds) throws DatabaseException {
        if (scheduleIds == null || scheduleIds.isEmpty()) {
            return new Reminder[0];
        }
        Map<String, String> params = new HashMap<>();
        params.put("schedule_id", "in.(" + formatIds(scheduleIds) + ")");
        params.put("order", "reminder_time.asc");
        return getReminders(params);
    }

    public boolean update(List<UUID> ownedScheduleIds, UUID id, Reminder reminder)
            throws DatabaseException {
        if (id == null || reminder == null || ownedScheduleIds == null || ownedScheduleIds.isEmpty()) {
            return false;
        }
        String path = TABLE + "?id=eq." + id + "&schedule_id=in.(" + formatIds(ownedScheduleIds) + ")";
        Map<String, Object> fields = new HashMap<>();
        fields.put("reminder_time", reminder.getReminderTime());
        return parseReminders(client.patchReturning(path, fields)).length > 0;
    }

    public boolean delete(List<UUID> ownedScheduleIds, UUID id) throws DatabaseException {
        if (id == null || ownedScheduleIds == null || ownedScheduleIds.isEmpty()) {
            return false;
        }
        String path = TABLE + "?id=eq." + id + "&schedule_id=in.(" + formatIds(ownedScheduleIds) + ")";
        return parseReminders(client.deleteReturning(path)).length > 0;
    }

    private Reminder[] getReminders(Map<String, String> params) throws DatabaseException {
        return parseReminders(client.get(TABLE, params));
    }

    private Reminder[] parseReminders(String response) throws DatabaseException {
        try {
            return mapper.readValue(response, Reminder[].class);
        } catch (JsonProcessingException e) {
            throw new DatabaseException("Failed to parse reminders response", e);
        }
    }

    private String formatIds(List<UUID> ids) {
        return ids.stream().map(UUID::toString).collect(Collectors.joining(","));
    }
}

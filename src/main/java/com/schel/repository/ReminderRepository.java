package com.schel.repository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.schel.database.SupabaseClient;
import com.schel.exceptions.DatabaseException;
import com.schel.models.Reminder;

public final class ReminderRepository {

    private static final String TABLE = "reminders";

    private final SupabaseClient client;
    private final ObjectMapper mapper;

    private static final ReminderRepository INSTANCE = new ReminderRepository();

    private ReminderRepository() {
        this.client = SupabaseClient.getInstance();
        this.mapper = new ObjectMapper();
        this.mapper.findAndRegisterModules();
        SimpleModule reminderTimestampModule = new SimpleModule();
        reminderTimestampModule.addDeserializer(OffsetDateTime.class, new JsonDeserializer<>() {
            @Override
            public OffsetDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
                String value = parser.getValueAsString();
                if (value == null) {
                    return (OffsetDateTime) context.handleUnexpectedToken(OffsetDateTime.class, parser);
                }
                try {
                    return OffsetDateTime.parse(value);
                } catch (java.time.format.DateTimeParseException noOffset) {
                    try {
                        return LocalDateTime.parse(value).atOffset(ZoneOffset.UTC);
                    } catch (java.time.format.DateTimeParseException invalidTimestamp) {
                        throw JsonMappingException.from(parser, "Invalid reminder_time timestamp", invalidTimestamp);
                    }
                }
            }
        });
        this.mapper.registerModule(reminderTimestampModule);
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
            logReminderParseFailure(response, e);
            throw new DatabaseException("Failed to parse reminders response", e);
        }
    }

    private void logReminderParseFailure(String response, JsonProcessingException exception) {
        System.err.println("Reminder response parse failure: " + exception.getClass().getSimpleName());
        if (exception instanceof JsonMappingException mappingException) {
            System.err.println("Reminder response field path: " + mappingException.getPathReference());
        }
        try {
            JsonNode root = mapper.readTree(response);
            System.err.println("Reminder response root type: " + (root == null ? "null" : root.getNodeType()));
            if (root != null && root.isArray()) {
                System.err.println("Reminder response row count: " + root.size());
                if (root.size() > 0 && root.get(0).isObject()) {
                    root.get(0).fields().forEachRemaining(entry -> System.err.println(
                            "Reminder response field type " + entry.getKey() + ": " + entry.getValue().getNodeType()));
                }
            }
        } catch (JsonProcessingException diagnosticException) {
            System.err.println("Reminder response structure could not be inspected: "
                    + diagnosticException.getClass().getSimpleName());
        }
    }

    private String formatIds(List<UUID> ids) {
        return ids.stream().map(UUID::toString).collect(Collectors.joining(","));
    }
}

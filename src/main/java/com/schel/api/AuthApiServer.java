package com.schel.api;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.schel.controllers.AuthController;
import com.schel.controllers.CategoryController;
import com.schel.controllers.ReminderController;
import com.schel.controllers.ScheduleController;
import com.schel.exceptions.AuthenticationException;
import com.schel.exceptions.DatabaseException;
import com.schel.models.Category;
import com.schel.models.Reminder;
import com.schel.models.Schedule;
import com.schel.models.User;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public final class AuthApiServer {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final HttpServer server;

    private AuthApiServer(int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/api", this::handleApiRequest);
        this.server.setExecutor(null);
    }

    public static AuthApiServer start(int port) throws IOException {
        AuthApiServer apiServer = new AuthApiServer(port);
        apiServer.server.start();
        return apiServer;
    }

    private void handleApiRequest(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if ("/api/health".equals(path)) {
                handleHealth(exchange);
                return;
            }

            if ("/api/auth/register".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleRegister(exchange);
                return;
            }

            if ("/api/auth/login".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleLogin(exchange);
                return;
            }

            if ("/api/categories".equals(path) && "GET".equalsIgnoreCase(method)) {
                handleListCategories(exchange);
                return;
            }

            if ("/api/categories".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleCreateCategory(exchange);
                return;
            }

            if ("/api/reminders".equals(path) && "GET".equalsIgnoreCase(method)) {
                handleListReminders(exchange);
                return;
            }

            if ("/api/reminders".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleCreateReminder(exchange);
                return;
            }

            if (path != null && path.startsWith("/api/reminders/")) {
                String idPart = path.substring("/api/reminders/".length());
                if (!idPart.isBlank()) {
                    String decodedId = URLDecoder.decode(idPart, StandardCharsets.UTF_8);
                    if ("PATCH".equalsIgnoreCase(method)) {
                        handleUpdateReminder(exchange, decodedId);
                        return;
                    }
                    if ("DELETE".equalsIgnoreCase(method)) {
                        handleDeleteReminder(exchange, decodedId);
                        return;
                    }
                }
            }

            if (path != null && path.startsWith("/api/categories/")) {
                String idPart = path.substring("/api/categories/".length());
                if (!idPart.isBlank()) {
                    String decodedId = URLDecoder.decode(idPart, StandardCharsets.UTF_8);
                    if ("PATCH".equalsIgnoreCase(method)) {
                        handleUpdateCategory(exchange, decodedId);
                        return;
                    }
                    if ("DELETE".equalsIgnoreCase(method)) {
                        handleDeleteCategory(exchange, decodedId);
                        return;
                    }
                }
            }

            if ("/api/schedules".equals(path) && "GET".equalsIgnoreCase(method)) {
                handleListSchedules(exchange);
                return;
            }

            if ("/api/schedules".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleCreateSchedule(exchange);
                return;
            }

            if (path != null && path.startsWith("/api/schedules/")) {
                String idPart = path.substring("/api/schedules/".length());
                if (!idPart.isBlank()) {
                    String decodedId = URLDecoder.decode(idPart, StandardCharsets.UTF_8);
                    if ("GET".equalsIgnoreCase(method)) {
                        handleGetScheduleById(exchange, decodedId);
                        return;
                    }
                    if ("PATCH".equalsIgnoreCase(method)) {
                        handleUpdateSchedule(exchange, decodedId);
                        return;
                    }
                    if ("DELETE".equalsIgnoreCase(method)) {
                        handleDeleteSchedule(exchange, decodedId);
                        return;
                    }
                }
            }

            sendJson(exchange, 404, Map.of("error", "Not found"));
        } catch (Exception e) {
            sendJson(exchange, 500, Map.of("error", "Internal server error"));
        }
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", "ok");
        payload.put("service", "schel-api");
        payload.put("timestamp", LocalDateTime.now().toString());
        sendJson(exchange, 200, payload);
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        try {
            Map<String, Object> input = readJsonBody(exchange);
            String username = String.valueOf(input.getOrDefault("username", "")).trim();
            String email = String.valueOf(input.getOrDefault("email", "")).trim();
            String password = String.valueOf(input.getOrDefault("password", ""));

            User user = AuthController.getInstance().register(username, email, password);
            sendJson(exchange, 201, Map.of("user", sanitizeUser(user)));
        } catch (AuthenticationException | DatabaseException e) {
            sendJson(exchange, resolveStatusCode(e), Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            sendJson(exchange, 500, Map.of("error", "Unable to register this account."));
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        try {
            Map<String, Object> input = readJsonBody(exchange);
            String identifier = String.valueOf(input.getOrDefault("identifier", "")).trim();
            String password = String.valueOf(input.getOrDefault("password", ""));

            User user = AuthController.getInstance().login(identifier, password);
            sendJson(exchange, 200, Map.of("user", sanitizeUser(user)));
        } catch (AuthenticationException | DatabaseException e) {
            sendJson(exchange, resolveStatusCode(e), Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logUnexpectedAuthFailure("login", e);
            sendJson(exchange, 500, Map.of("error", "Unable to sign in."));
        }
    }

    private void logUnexpectedAuthFailure(String operation, Exception exception) {
        System.err.println("Unexpected authentication API failure during " + operation + ":");
        for (Throwable current = exception; current != null; current = current.getCause()) {
            System.err.println(current.getClass().getName());
            for (StackTraceElement frame : current.getStackTrace()) {
                System.err.println("\tat " + frame);
            }
        }
    }

    private void handleListCategories(HttpExchange exchange) throws IOException {
        try {
            Category[] categories = CategoryController.getInstance().getUserCategories();
            sendJson(exchange, 200, Map.of("categories", toCategoryListPayload(categories)));
        } catch (AuthenticationException e) {
            sendJson(exchange, 401, Map.of("error", e.getMessage()));
        } catch (DatabaseException e) {
            sendJson(exchange, 500, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            sendJson(exchange, 500, Map.of("error", "Unable to load categories."));
        }
    }

    private void handleListReminders(HttpExchange exchange) throws IOException {
        try {
            Reminder[] reminders = ReminderController.getInstance().getUserReminders();
            sendJson(exchange, 200, Map.of("reminders", toReminderListPayload(reminders)));
        } catch (AuthenticationException e) {
            sendJson(exchange, 401, Map.of("error", e.getMessage()));
        } catch (DatabaseException e) {
            sendJson(exchange, 500, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            sendJson(exchange, 500, Map.of("error", "Unable to load reminders."));
        }
    }

    private void handleCreateReminder(HttpExchange exchange) throws IOException {
        try {
            Map<String, Object> input = readJsonBody(exchange);
            UUID scheduleId = parseOptionalUuid(input.get("schedule_id"));
            OffsetDateTime reminderTime = parseOffsetDateTime(input.get("reminder_time"));
            if (scheduleId == null) {
                throw new IllegalArgumentException("Schedule is required.");
            }
            ReminderController.getInstance().createReminder(scheduleId, reminderTime);
            Reminder[] reminders = ReminderController.getInstance().getUserReminders();
            Reminder created = findReminderByScheduleAndTime(reminders, scheduleId, reminderTime);
            sendJson(exchange, 201, Map.of("reminder", toReminderPayload(created)));
        } catch (AuthenticationException e) {
            sendJson(exchange, 401, Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, Map.of("error", e.getMessage()));
        } catch (DatabaseException e) {
            sendJson(exchange, 500, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            sendJson(exchange, 500, Map.of("error", "Could not create this reminder."));
        }
    }

    private void handleUpdateReminder(HttpExchange exchange, String reminderId) throws IOException {
        try {
            UUID id = UUID.fromString(reminderId);
            Map<String, Object> input = readJsonBody(exchange);
            OffsetDateTime reminderTime = parseOffsetDateTime(input.get("reminder_time"));
            boolean updated = ReminderController.getInstance().updateReminder(id, reminderTime);
            if (!updated) {
                sendJson(exchange, 404, Map.of("error", "Reminder not found."));
                return;
            }
            Reminder[] reminders = ReminderController.getInstance().getUserReminders();
            Reminder match = findReminderById(reminders, id);
            sendJson(exchange, 200, Map.of("reminder", toReminderPayload(match)));
        } catch (AuthenticationException e) {
            sendJson(exchange, 401, Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, Map.of("error", e.getMessage()));
        } catch (DatabaseException e) {
            sendJson(exchange, 500, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            sendJson(exchange, 500, Map.of("error", "Could not update this reminder."));
        }
    }

    private void handleDeleteReminder(HttpExchange exchange, String reminderId) throws IOException {
        try {
            UUID id = UUID.fromString(reminderId);
            boolean deleted = ReminderController.getInstance().deleteReminder(id);
            if (!deleted) {
                sendJson(exchange, 404, Map.of("error", "Reminder not found."));
                return;
            }
            sendEmpty(exchange, 204);
        } catch (AuthenticationException e) {
            sendJson(exchange, 401, Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, Map.of("error", e.getMessage()));
        } catch (DatabaseException e) {
            sendJson(exchange, 500, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            sendJson(exchange, 500, Map.of("error", "Could not delete this reminder."));
        }
    }

    private void handleCreateCategory(HttpExchange exchange) throws IOException {
        try {
            Map<String, Object> input = readJsonBody(exchange);
            String name = valueAsString(input.get("name"));
            String color = valueAsString(input.get("color"));

            CategoryController.getInstance().createCategory(name, color);
            Category[] categories = CategoryController.getInstance().getUserCategories();
            Category created = findCategoryByNameAndColor(categories, name, color);
            if (created == null) {
                sendJson(exchange, 201, Map.of("category", null));
                return;
            }
            sendJson(exchange, 201, Map.of("category", toCategoryPayload(created)));
        } catch (AuthenticationException e) {
            sendJson(exchange, 401, Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, Map.of("error", e.getMessage()));
        } catch (DatabaseException e) {
            sendJson(exchange, 500, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            sendJson(exchange, 500, Map.of("error", "Could not create this category."));
        }
    }

    private void handleUpdateCategory(HttpExchange exchange, String categoryId) throws IOException {
        try {
            UUID id = UUID.fromString(categoryId);
            Map<String, Object> input = readJsonBody(exchange);
            String name = valueAsString(input.get("name"));
            String color = valueAsString(input.get("color"));
            boolean updated = CategoryController.getInstance().updateCategory(id, name, color);
            if (!updated) {
                sendJson(exchange, 404, Map.of("error", "Category not found."));
                return;
            }
            Category[] categories = CategoryController.getInstance().getUserCategories();
            Category match = findCategoryById(categories, id);
            sendJson(exchange, 200, Map.of("category", toCategoryPayload(match)));
        } catch (AuthenticationException e) {
            sendJson(exchange, 401, Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, Map.of("error", e.getMessage()));
        } catch (DatabaseException e) {
            sendJson(exchange, 500, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            sendJson(exchange, 500, Map.of("error", "Could not update this category."));
        }
    }

    private void handleDeleteCategory(HttpExchange exchange, String categoryId) throws IOException {
        try {
            UUID id = UUID.fromString(categoryId);
            boolean deleted = CategoryController.getInstance().deleteCategory(id);
            if (!deleted) {
                sendJson(exchange, 404, Map.of("error", "Category not found."));
                return;
            }
            sendEmpty(exchange, 204);
        } catch (AuthenticationException e) {
            sendJson(exchange, 401, Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, Map.of("error", e.getMessage()));
        } catch (DatabaseException e) {
            sendJson(exchange, 500, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            sendJson(exchange, 500, Map.of("error", "Could not delete this category."));
        }
    }

    private void handleListSchedules(HttpExchange exchange) throws IOException {
        try {
            Schedule[] schedules = ScheduleController.getInstance().viewSchedules();
            sendJson(exchange, 200, Map.of("schedules", toScheduleListPayload(schedules)));
        } catch (AuthenticationException e) {
            sendJson(exchange, 401, Map.of("error", e.getMessage()));
        } catch (DatabaseException e) {
            sendJson(exchange, 500, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            sendJson(exchange, 500, Map.of("error", "Unable to load schedules."));
        }
    }

    private void handleGetScheduleById(HttpExchange exchange, String scheduleId) throws IOException {
        try {
            UUID id = UUID.fromString(scheduleId);
            Schedule[] schedules = ScheduleController.getInstance().viewSchedules();
            Schedule match = null;
            for (Schedule schedule : schedules) {
                if (schedule.getId().equals(id)) {
                    match = schedule;
                    break;
                }
            }
            if (match == null) {
                sendJson(exchange, 404, Map.of("error", "Schedule not found."));
                return;
            }
            sendJson(exchange, 200, Map.of("schedule", toSchedulePayload(match)));
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, Map.of("error", "Invalid schedule id."));
        } catch (AuthenticationException e) {
            sendJson(exchange, 401, Map.of("error", e.getMessage()));
        } catch (DatabaseException e) {
            sendJson(exchange, 500, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            sendJson(exchange, 500, Map.of("error", "Unable to load the schedule."));
        }
    }

    private void handleCreateSchedule(HttpExchange exchange) throws IOException {
        try {
            Map<String, Object> input = readJsonBody(exchange);
            Schedule schedule = buildScheduleFromInput(input, null);
            Schedule created = ScheduleController.getInstance().addSchedule(schedule);
            sendJson(exchange, 201, Map.of("schedule", toSchedulePayload(created)));
        } catch (AuthenticationException e) {
            sendJson(exchange, 401, Map.of("error", e.getMessage()));
        } catch (DatabaseException e) {
            sendJson(exchange, 500, Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            sendJson(exchange, 500, Map.of("error", "Could not create the schedule."));
        }
    }

    private void handleUpdateSchedule(HttpExchange exchange, String scheduleId) throws IOException {
        try {
            UUID id = UUID.fromString(scheduleId);
            Map<String, Object> input = readJsonBody(exchange);
            Schedule schedule = buildScheduleFromInput(input, id);
            Schedule updated = ScheduleController.getInstance().updateSchedule(schedule);
            sendJson(exchange, 200, Map.of("schedule", toSchedulePayload(updated)));
        } catch (AuthenticationException e) {
            sendJson(exchange, 401, Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, Map.of("error", e.getMessage()));
        } catch (DatabaseException e) {
            sendJson(exchange, 500, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            sendJson(exchange, 500, Map.of("error", "Could not update the schedule."));
        }
    }

    private void handleDeleteSchedule(HttpExchange exchange, String scheduleId) throws IOException {
        try {
            UUID id = UUID.fromString(scheduleId);
            ScheduleController.getInstance().deleteSchedule(id);
            sendEmpty(exchange, 204);
        } catch (AuthenticationException e) {
            sendJson(exchange, 401, Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, Map.of("error", e.getMessage()));
        } catch (DatabaseException e) {
            sendJson(exchange, 500, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            sendJson(exchange, 500, Map.of("error", "Could not delete the schedule."));
        }
    }

    private Map<String, Object> readJsonBody(HttpExchange exchange) throws IOException {
        byte[] raw = exchange.getRequestBody().readAllBytes();
        String body = new String(raw, StandardCharsets.UTF_8);
        if (body.isBlank()) {
            throw new IllegalArgumentException("Request body is required.");
        }
        try {
            return MAPPER.readValue(body, Map.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Request body must be valid JSON.");
        }
    }

    private Schedule buildScheduleFromInput(Map<String, Object> input, UUID id) {
        if (input == null) {
            throw new IllegalArgumentException("Request body is required.");
        }

        String title = valueAsString(input.get("title"));
        String description = valueAsString(input.get("description"));
        String priority = normalizeEnum(valueAsString(input.get("priority")), "LOW", "MEDIUM", "HIGH", "MEDIUM");
        String status = normalizeEnum(valueAsString(input.get("status")), "PENDING", "COMPLETED", "MISSED", "PENDING");

        LocalDate taskDate = parseLocalDate(input.get("task_date"));
        LocalTime startTime = parseLocalTime(input.get("start_time"));
        LocalTime endTime = parseLocalTime(input.get("end_time"));

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }
        if (taskDate == null) {
            throw new IllegalArgumentException("Task date is required");
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time are required");
        }
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        UUID categoryId = parseOptionalUuid(input.get("category_id"));
        return new Schedule(id, null, categoryId, title, description, taskDate, startTime, endTime, priority, status, null, null);
    }

    private Map<String, Object> sanitizeUser(User user) {
        Map<String, Object> result = new HashMap<>();
        if (user == null) {
            return result;
        }
        result.put("id", user.getId() == null ? null : user.getId().toString());
        result.put("username", user.getUsername());
        result.put("email", user.getEmail());
        if (user.getCreatedAt() != null) {
            result.put("created_at", user.getCreatedAt().toString());
        }
        return result;
    }

    private int resolveStatusCode(Exception e) {
        String message = e.getMessage() == null ? "" : e.getMessage();
        if (message != null && message.toLowerCase().contains("already")) {
            return 409;
        }
        if (message != null && message.toLowerCase().contains("invalid credentials")) {
            return 401;
        }
        if (message != null && (message.toLowerCase().contains("required") || message.toLowerCase().contains("valid email"))) {
            return 400;
        }
        return 500;
    }

    private String normalizeEnum(String value, String first, String second, String third, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String upper = value.trim().toUpperCase();
        if (first.equals(upper) || second.equals(upper) || third.equals(upper)) {
            return upper;
        }
        return fallback;
    }

    private String valueAsString(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private LocalDate parseLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        String raw = String.valueOf(value).trim();
        if (raw.isEmpty()) {
            return null;
        }
        return LocalDate.parse(raw);
    }

    private OffsetDateTime parseOffsetDateTime(Object value) {
        if (value == null) {
            return null;
        }
        String raw = String.valueOf(value).trim();
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(raw);
        } catch (Exception ignored) {
            LocalDateTime localDateTime = LocalDateTime.parse(raw);
            return localDateTime.atOffset(OffsetDateTime.now().getOffset());
        }
    }

    private LocalTime parseLocalTime(Object value) {
        if (value == null) {
            return null;
        }
        String raw = String.valueOf(value).trim();
        if (raw.isEmpty()) {
            return null;
        }
        return LocalTime.parse(raw);
    }

    private UUID parseOptionalUuid(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return UUID.fromString(String.valueOf(value));
    }

    private Reminder findReminderById(Reminder[] reminders, UUID id) {
        if (reminders == null || id == null) {
            return null;
        }
        for (Reminder reminder : reminders) {
            if (reminder != null && reminder.getId() != null && reminder.getId().equals(id)) {
                return reminder;
            }
        }
        return null;
    }

    private Reminder findReminderByScheduleAndTime(Reminder[] reminders, UUID scheduleId, OffsetDateTime reminderTime) {
        if (reminders == null || scheduleId == null || reminderTime == null) {
            return null;
        }
        for (Reminder reminder : reminders) {
            if (reminder != null && reminder.getScheduleId() != null && reminder.getScheduleId().equals(scheduleId)
                    && reminder.getReminderTime() != null && reminder.getReminderTime().equals(reminderTime)) {
                return reminder;
            }
        }
        return reminders.length > 0 ? reminders[reminders.length - 1] : null;
    }

    private Category findCategoryByNameAndColor(Category[] categories, String name, String color) {
        if (categories == null) {
            return null;
        }
        for (Category category : categories) {
            if (category == null) {
                continue;
            }
            String categoryName = category.getName() == null ? "" : category.getName();
            String categoryColor = category.getColor() == null ? "" : category.getColor();
            if (categoryName.equals(name == null ? "" : name) && categoryColor.equals(color == null ? "" : color)) {
                return category;
            }
        }
        return null;
    }

    private Category findCategoryById(Category[] categories, UUID id) {
        if (categories == null || id == null) {
            return null;
        }
        for (Category category : categories) {
            if (category != null && category.getId() != null && category.getId().equals(id)) {
                return category;
            }
        }
        return null;
    }

    private Map<String, Object> toReminderPayload(Reminder reminder) {
        Map<String, Object> payload = new HashMap<>();
        if (reminder == null) {
            return payload;
        }
        payload.put("id", reminder.getId() == null ? null : reminder.getId().toString());
        payload.put("schedule_id", reminder.getScheduleId() == null ? null : reminder.getScheduleId().toString());
        payload.put("reminder_time", reminder.getReminderTime() == null ? null : reminder.getReminderTime().toString());
        payload.put("is_sent", reminder.isSent());
        return payload;
    }

    private Object[] toReminderListPayload(Reminder[] reminders) {
        if (reminders == null) {
            return new Object[0];
        }
        Object[] payload = new Object[reminders.length];
        for (int i = 0; i < reminders.length; i++) {
            payload[i] = toReminderPayload(reminders[i]);
        }
        return payload;
    }

    private Map<String, Object> toCategoryPayload(Category category) {
        Map<String, Object> payload = new HashMap<>();
        if (category == null) {
            return payload;
        }
        payload.put("id", category.getId() == null ? null : category.getId().toString());
        payload.put("user_id", category.getUserId() == null ? null : category.getUserId().toString());
        payload.put("name", category.getName());
        payload.put("color", category.getColor());
        payload.put("created_at", category.getCreatedAt() == null ? null : category.getCreatedAt().toString());
        return payload;
    }

    private Object[] toCategoryListPayload(Category[] categories) {
        if (categories == null) {
            return new Object[0];
        }
        Object[] payload = new Object[categories.length];
        for (int i = 0; i < categories.length; i++) {
            payload[i] = toCategoryPayload(categories[i]);
        }
        return payload;
    }

    private Map<String, Object> toSchedulePayload(Schedule schedule) {
        Map<String, Object> payload = new HashMap<>();
        if (schedule == null) {
            return payload;
        }
        payload.put("id", schedule.getId() == null ? null : schedule.getId().toString());
        payload.put("user_id", schedule.getUserId() == null ? null : schedule.getUserId().toString());
        payload.put("category_id", schedule.getCategoryId() == null ? null : schedule.getCategoryId().toString());
        payload.put("title", schedule.getTitle());
        payload.put("description", schedule.getDescription());
        payload.put("task_date", schedule.getTaskDate() == null ? null : schedule.getTaskDate().toString());
        payload.put("start_time", schedule.getStartTime() == null ? null : schedule.getStartTime().toString());
        payload.put("end_time", schedule.getEndTime() == null ? null : schedule.getEndTime().toString());
        payload.put("priority", schedule.getPriority());
        payload.put("status", schedule.getStatus());
        payload.put("created_at", schedule.getCreatedAt() == null ? null : schedule.getCreatedAt().toString());
        payload.put("updated_at", schedule.getUpdatedAt() == null ? null : schedule.getUpdatedAt().toString());
        payload.put("category", "General");
        return payload;
    }

    private Object[] toScheduleListPayload(Schedule[] schedules) {
        if (schedules == null) {
            return new Object[0];
        }
        Object[] payload = new Object[schedules.length];
        for (int i = 0; i < schedules.length; i++) {
            payload[i] = toSchedulePayload(schedules[i]);
        }
        return payload;
    }

    private void setCorsHeaders(HttpExchange exchange) {
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET,POST,PATCH,DELETE,OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type,Authorization");
    }

    private void sendJson(HttpExchange exchange, int statusCode, Map<String, Object> payload) throws IOException {
        byte[] responseBytes = MAPPER.writeValueAsBytes(payload);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBytes);
        }
    }

    private void sendEmpty(HttpExchange exchange, int statusCode) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, -1);
    }
}

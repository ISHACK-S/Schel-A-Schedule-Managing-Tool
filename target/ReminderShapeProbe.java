import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schel.database.SupabaseClient;

public final class ReminderShapeProbe {
    public static void main(String[] args) {
        try {
            String json = SupabaseClient.getInstance().get("reminders", Map.of("select", "*", "limit", "5"));
            JsonNode root = new ObjectMapper().readTree(json);
            System.out.println("ROOT_TYPE=" + root.getNodeType());
            System.out.println("ROW_COUNT=" + (root.isArray() ? root.size() : -1));
            if (!root.isArray()) return;
            for (int rowIndex = 0; rowIndex < root.size(); rowIndex++) {
                JsonNode row = root.get(rowIndex);
                System.out.println("ROW_" + rowIndex + "_TYPE=" + row.getNodeType());
                row.fields().forEachRemaining(field -> {
                    JsonNode value = field.getValue();
                    System.out.println("FIELD=" + field.getKey() + ";TYPE=" + value.getNodeType());
                    if (value.isNull()) return;
                    if (("id".equals(field.getKey()) || "schedule_id".equals(field.getKey())) && value.isTextual()) {
                        boolean validUuid;
                        try { UUID.fromString(value.asText()); validUuid = true; }
                        catch (IllegalArgumentException e) { validUuid = false; }
                        System.out.println("FIELD_CHECK=" + field.getKey() + ";UUID=" + validUuid);
                    }
                    if (("reminder_time".equals(field.getKey()) || "created_at".equals(field.getKey()) || "updated_at".equals(field.getKey())) && value.isTextual()) {
                        String timestamp = value.asText();
                        boolean offsetTimestamp;
                        try { OffsetDateTime.parse(timestamp); offsetTimestamp = true; }
                        catch (Exception e) { offsetTimestamp = false; }
                        boolean localTimestamp;
                        try { LocalDateTime.parse(timestamp); localTimestamp = true; }
                        catch (Exception e) { localTimestamp = false; }
                        System.out.println("FIELD_CHECK=" + field.getKey() + ";OFFSET_DATETIME=" + offsetTimestamp + ";LOCAL_DATETIME=" + localTimestamp);
                    }
                });
            }
        } catch (Exception e) {
            System.out.println("PROBE_ERROR_TYPE=" + e.getClass().getSimpleName());
        }
    }
}

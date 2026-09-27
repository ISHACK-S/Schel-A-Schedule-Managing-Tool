package com.schel.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Reminder {

    private UUID id;

    @JsonProperty("schedule_id")
    private UUID scheduleId;

    @JsonProperty("reminder_time")
    private OffsetDateTime reminderTime;

    @JsonProperty("is_sent")
    private boolean isSent;

    public Reminder() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(UUID scheduleId) {
        this.scheduleId = scheduleId;
    }

    public OffsetDateTime getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(OffsetDateTime reminderTime) {
        this.reminderTime = reminderTime;
    }

    public boolean isSent() {
        return isSent;
    }

    public void setSent(boolean sent) {
        isSent = sent;
    }
}

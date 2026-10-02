package model;

import java.time.LocalDateTime;

public record AuditEntry(long id, LocalDateTime at, String username, String action, String details) { }

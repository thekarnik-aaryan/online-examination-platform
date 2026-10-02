package ui;

import model.AuditEntry;
import service.AuditService;

import java.util.List;

public class AuditPanel extends TablePanel<AuditEntry> {
    public AuditPanel() {
        super("Time", "User", "Action", "Details");
        refresh();
    }

    @Override protected List<AuditEntry> load() { return AuditService.latest(500); }

    @Override protected Object[] toRow(AuditEntry a) {
        return new Object[]{a.at() == null ? "" : a.at().format(Ui.FMT), a.username() == null ? "-" : a.username(), a.action(), a.details()};
    }
}

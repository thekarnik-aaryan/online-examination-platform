package security;

import exception.AccessDeniedException;
import model.Role;
import model.User;
import service.AuditService;

/** Role checks used at the top of every service method (not just in the GUI). */
public final class Authz {
    private Authz() {}

    public static User require(Role... allowed) {
        User u = Session.requireUser();
        for (Role r : allowed) if (u.role() == r) return u;
        AuditService.log("ACCESS_DENIED", "role=" + u.role() + " attempted a restricted action");
        throw new AccessDeniedException("You do not have permission to perform this action.");
    }
}

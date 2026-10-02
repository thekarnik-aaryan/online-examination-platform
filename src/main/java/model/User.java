package model;

public record User(long id, String username, String fullName, String email, Role role, boolean active) {
    @Override public String toString() { return fullName + " (" + username + ")"; }
}

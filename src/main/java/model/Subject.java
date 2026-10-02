package model;

public record Subject(long id, String code, String name) {
    @Override public String toString() { return code + " - " + name; }
}

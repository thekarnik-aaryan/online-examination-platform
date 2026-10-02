package model;

import java.util.List;

public record Question(long id, long examId, String text, int marks, List<Option> options) { }

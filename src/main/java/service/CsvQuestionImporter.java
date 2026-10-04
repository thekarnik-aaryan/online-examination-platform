package service;

import exception.ValidationException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Parses and validates a question CSV (comma or semicolon separated, UTF-8). Pure file logic, no database. */
public final class CsvQuestionImporter {
    public record Row(String text, int marks, List<String> options, int correct) { }

    private static final long MAX_BYTES = 1_000_000;
    private static final String TEMPLATE =
            "Question,Option 1,Option 2,Option 3,Option 4,Correct (1-4),Marks\r\n"
          + "Which keyword is used to inherit a class in Java?,implements,extends,inherits,super,2,2\r\n"
          + "\"Which of these is NOT a primitive type?\",int,boolean,String,char,3,2\r\n";
    private CsvQuestionImporter() {}

    public static void writeTemplate(Path target) {
        try {
            Files.writeString(target, "\uFEFF" + TEMPLATE, StandardCharsets.UTF_8); // BOM so Excel reads UTF-8
        } catch (IOException e) {
            throw new ValidationException("Could not save the template file there. Choose another folder.");
        }
    }

    public static List<Row> parse(Path file) {
        String content;
        try {
            if (Files.size(file) > MAX_BYTES) throw new ValidationException("The file is too large (limit 1 MB).");
            content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ValidationException("Could not read that file. Make sure it is closed in Excel and try again.");
        }
        return parseText(content);
    }

    static List<Row> parseText(String content) {
        if (content.startsWith("\uFEFF")) content = content.substring(1);
        int nl = content.indexOf('\n');
        String first = nl < 0 ? content : content.substring(0, nl);
        char delimiter = first.chars().filter(c -> c == ';').count() > first.chars().filter(c -> c == ',').count() ? ';' : ',';

        List<List<String>> table = readCsv(content, delimiter);
        List<Row> out = new ArrayList<>();
        for (int i = 0; i < table.size(); i++) {
            List<String> r = table.get(i);
            int rowNo = i + 1; // matches the row number shown in Excel
            if (r.stream().allMatch(c -> c.isBlank())) continue;
            if (i == 0 && r.get(0).trim().equalsIgnoreCase("question")) continue; // header
            out.add(toRow(rowNo, r));
        }
        if (out.isEmpty()) throw new ValidationException("The file contains no questions.");
        return out;
    }

    private static Row toRow(int rowNo, List<String> r) {
        if (r.size() < 7) throw new ValidationException("Row " + rowNo + ": expected 7 columns (Question, 4 options, Correct, Marks).");
        String text = r.get(0).trim();
        if (text.length() < 3 || text.length() > 1000) throw new ValidationException("Row " + rowNo + ": the question must be 3 to 1000 characters.");
        List<String> opts = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int k = 1; k <= 4; k++) {
            String o = r.get(k).trim();
            if (o.isEmpty() || o.length() > 300) throw new ValidationException("Row " + rowNo + ": Option " + k + " must be 1 to 300 characters.");
            if (!seen.add(o.toLowerCase(Locale.ROOT))) throw new ValidationException("Row " + rowNo + ": options must be different from each other.");
            opts.add(o);
        }
        int correct = num(rowNo, "Correct", r.get(5), 1, 4);
        int marks = num(rowNo, "Marks", r.get(6), 1, 100);
        return new Row(text, marks, opts, correct);
    }

    private static int num(int rowNo, String label, String v, int min, int max) {
        try {
            int n = Integer.parseInt(v.trim());
            if (n >= min && n <= max) return n;
        } catch (NumberFormatException ignored) { /* reported below */ }
        throw new ValidationException("Row " + rowNo + ": " + label + " must be a whole number from " + min + " to " + max + ".");
    }

    private static List<List<String>> readCsv(String s, char d) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder f = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < s.length() && s.charAt(i + 1) == '"') { f.append('"'); i++; } else quoted = false;
                } else f.append(c);
            } else if (c == '"') {
                quoted = true;
            } else if (c == d) {
                row.add(f.toString()); f.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < s.length() && s.charAt(i + 1) == '\n') i++;
                row.add(f.toString()); f.setLength(0);
                rows.add(row); row = new ArrayList<>();
            } else f.append(c);
        }
        if (quoted) throw new ValidationException("The file has an opening quotation mark that is never closed.");
        if (f.length() > 0 || !row.isEmpty()) { row.add(f.toString()); rows.add(row); }
        return rows;
    }
}

package com.educmind.reports.service;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Chuyển 1 báo cáo (record/DTO bất kỳ) thành CSV, không phụ thuộc Jackson.
 * Phần đầu: các dòng "field,value" cho giá trị đơn. Sau đó mỗi danh sách là 1 bảng riêng.
 */
public final class CsvReportWriter {

    private static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private CsvReportWriter() {
    }

    record Section(String title, List<String> header, List<List<String>> rows) {
    }

    record Flat(List<String[]> summary, List<Section> sections) {
    }

    static Flat toFlat(Object report) {
        List<String[]> rows = new ArrayList<>();
        Map<String, List<Object>> tables = new LinkedHashMap<>();
        collect("", report, rows, tables);
        List<Section> sections = new ArrayList<>();
        for (Map.Entry<String, List<Object>> t : tables.entrySet()) {
            List<Map<String, String>> flat = new ArrayList<>();
            Set<String> header = new LinkedHashSet<>();
            for (Object el : t.getValue()) {
                Map<String, String> m = new LinkedHashMap<>();
                flatten("", el, m);
                header.addAll(m.keySet());
                flat.add(m);
            }
            List<List<String>> body = new ArrayList<>();
            for (Map<String, String> m : flat) {
                List<String> cells = new ArrayList<>();
                for (String h : header) {
                    cells.add(m.getOrDefault(h, ""));
                }
                body.add(cells);
            }
            sections.add(new Section(t.getKey(), new ArrayList<>(header), body));
        }
        return new Flat(rows, sections);
    }

    public static byte[] write(Object report) {
        Flat flat = toFlat(report);
        StringBuilder sb = new StringBuilder();
        sb.append("field,value\n");
        for (String[] r : flat.summary()) {
            sb.append(escape(r[0])).append(',').append(escape(r[1])).append('\n');
        }
        for (Section sec : flat.sections()) {
            sb.append('\n').append("# ").append(sec.title()).append('\n');
            if (sec.header().isEmpty()) {
                sb.append("(trống)\n");
                continue;
            }
            sb.append(String.join(",", sec.header().stream().map(CsvReportWriter::escape).toList())).append('\n');
            for (List<String> cells : sec.rows()) {
                sb.append(String.join(",", cells.stream().map(CsvReportWriter::escape).toList())).append('\n');
            }
        }
        byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[BOM.length + body.length];
        System.arraycopy(BOM, 0, out, 0, BOM.length);
        System.arraycopy(body, 0, out, BOM.length, body.length);
        return out;
    }

    private static void collect(String prefix, Object obj, List<String[]> rows,
                                Map<String, List<Object>> tables) {
        for (Map.Entry<String, Object> p : properties(obj).entrySet()) {
            String name = prefix + p.getKey();
            Object v = p.getValue();
            if (isScalar(v)) {
                rows.add(new String[]{name, text(v)});
            } else if (v instanceof Collection<?> c) {
                tables.put(name, new ArrayList<>(c));
            } else {
                collect(name + ".", v, rows, tables);
            }
        }
    }

    private static void flatten(String prefix, Object obj, Map<String, String> out) {
        if (isScalar(obj)) {
            out.put(prefix.isEmpty() ? "value" : stripDot(prefix), text(obj));
            return;
        }
        for (Map.Entry<String, Object> p : properties(obj).entrySet()) {
            String name = prefix + p.getKey();
            Object v = p.getValue();
            if (isScalar(v)) {
                out.put(name, text(v));
            } else if (v instanceof Collection<?> c) {
                List<String> parts = new ArrayList<>();
                for (Object e : c) {
                    if (isScalar(e)) {
                        parts.add(text(e));
                    }
                }
                out.put(name, String.join(";", parts));
            } else {
                flatten(name + ".", v, out);
            }
        }
    }

    private static String stripDot(String s) {
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }

    private static boolean isScalar(Object v) {
        return v == null || v instanceof CharSequence || v instanceof Number
                || v instanceof Boolean || v instanceof Enum<?> || v instanceof UUID
                || v instanceof Temporal || v instanceof Character;
    }

    private static String text(Object v) {
        if (v == null) {
            return "";
        }
        String s = v.toString();
        if (v instanceof CharSequence && !s.isEmpty() && "=+@".indexOf(s.charAt(0)) >= 0) {
            return "'" + s;
        }
        return s;
    }

    private static Map<String, Object> properties(Object obj) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (obj == null) {
            return out;
        }
        try {
            if (obj instanceof Map<?, ?> m) {
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    out.put(String.valueOf(e.getKey()), e.getValue());
                }
            } else if (obj.getClass().isRecord()) {
                for (RecordComponent rc : obj.getClass().getRecordComponents()) {
                    out.put(rc.getName(), rc.getAccessor().invoke(obj));
                }
            } else {
                List<Method> getters = Arrays.stream(obj.getClass().getMethods())
                        .filter(m -> m.getParameterCount() == 0
                                && !Modifier.isStatic(m.getModifiers())
                                && !m.getName().equals("getClass")
                                && (m.getName().startsWith("get") || m.getName().startsWith("is")))
                        .sorted(Comparator.comparing(Method::getName))
                        .toList();
                for (Method m : getters) {
                    String n = m.getName().startsWith("get") ? m.getName().substring(3) : m.getName().substring(2);
                    if (!n.isEmpty()) {
                        n = Character.toLowerCase(n.charAt(0)) + n.substring(1);
                        out.put(n, m.invoke(obj));
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Không đọc được dữ liệu báo cáo: " + e.getMessage(), e);
        }
        return out;
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}

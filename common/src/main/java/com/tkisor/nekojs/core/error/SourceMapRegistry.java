package com.tkisor.nekojs.core.error;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SourceMapRegistry {

    private static final Pattern MAPPINGS_PATTERN = Pattern.compile("\"mappings\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern NAMES_PATTERN = Pattern.compile("\"names\"\\s*:\\s*\\[(.*?)\\]");

    private static final Map<String, ScriptMapping> MAPPINGS_MAP = new ConcurrentHashMap<>();

    public static void register(String scriptPath, String sourceMapJson) {
        if (sourceMapJson == null || sourceMapJson.isEmpty() || scriptPath == null) return;
        try {
            String mappings = "";
            Matcher mapMatcher = MAPPINGS_PATTERN.matcher(sourceMapJson);
            if (mapMatcher.find()) mappings = mapMatcher.group(1);

            String[] names = new String[0];
            Matcher nameMatcher = NAMES_PATTERN.matcher(sourceMapJson);
            if (nameMatcher.find()) {
                String namesStr = nameMatcher.group(1);
                if (!namesStr.trim().isEmpty()) {
                    String[] rawNames = namesStr.split(",");
                    names = new String[rawNames.length];
                    for (int i = 0; i < rawNames.length; i++) {
                        names[i] = rawNames[i].trim().replaceAll("^\"|\"$", "");
                    }
                }
            }

            if (!mappings.isEmpty()) {
                String normalizedPath = scriptPath.replace('\\', '/');
                ScriptMapping mapping = parseMappings(mappings, names);
                MAPPINGS_MAP.put(normalizedPath, mapping);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static ScriptMapping parseMappings(String mappings, String[] names) {
        String[] jsLines = mappings.split(";", -1);
        List<List<MappingEntry>> lineMappings = new ArrayList<>(jsLines.length);
        int sourceLine = 0;
        int sourceColumn = 0;
        int nameIndex = 0;

        for (String jsLineStr : jsLines) {
            List<MappingEntry> currentLineEntries = new ArrayList<>();
            lineMappings.add(currentLineEntries);
            if (jsLineStr.isEmpty()) continue;
            int jsColumn = 0;
            String[] segments = jsLineStr.split(",");
            for (String segment : segments) {
                if (segment.isEmpty()) continue;
                List<Integer> vals = decodeVlq(segment);
                jsColumn += vals.get(0);
                if (vals.size() >= 4) {
                    sourceLine += vals.get(2);
                    sourceColumn += vals.get(3);
                    String symbolName = null;
                    if (vals.size() >= 5) {
                        nameIndex += vals.get(4);
                        if (nameIndex >= 0 && nameIndex < names.length) {
                            symbolName = names[nameIndex];
                        }
                    }
                    currentLineEntries.add(new MappingEntry(jsColumn, sourceLine, sourceColumn, symbolName));
                }
            }
        }
        return new ScriptMapping(lineMappings);
    }

    public static OriginalPosition getMappedPosition(String scriptPath, int jsLine, int jsColumn) {
        OriginalPosition fallback = new OriginalPosition(jsLine, jsColumn, null);
        if (scriptPath == null || MAPPINGS_MAP.isEmpty()) return fallback;
        String query = scriptPath.replace('\\', '/');
        ScriptMapping mapping = MAPPINGS_MAP.get(query);
        if (mapping == null) {
            for (Map.Entry<String, ScriptMapping> entry : MAPPINGS_MAP.entrySet()) {
                if (entry.getKey().endsWith(query) || query.endsWith(entry.getKey())) {
                    mapping = entry.getValue();
                    break;
                }
            }
        }
        if (mapping != null) {
            int lineIndex = jsLine - 1;
            if (lineIndex >= 0 && lineIndex < mapping.lineMappings.size()) {
                List<MappingEntry> entries = mapping.lineMappings.get(lineIndex);
                if (entries == null || entries.isEmpty()) return fallback;
                int targetCol = jsColumn - 1;
                MappingEntry bestMatch = entries.get(0);
                for (MappingEntry entry : entries) {
                    if (entry.jsColumn <= targetCol) {
                        bestMatch = entry;
                    } else {
                        break;
                    }
                }
                return new OriginalPosition(bestMatch.tsLine + 1, bestMatch.tsColumn + 1, bestMatch.name);
            }
        }
        return fallback;
    }

    public static void clearByPrefix(String prefix) {
        if (prefix == null) return;
        MAPPINGS_MAP.keySet().removeIf(path -> path.toLowerCase().contains(prefix.toLowerCase()));
    }

    private static List<Integer> decodeVlq(String str) {
        List<Integer> result = new ArrayList<>();
        int value = 0, shift = 0;
        for (int i = 0; i < str.length(); i++) {
            int c = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".indexOf(str.charAt(i));
            if (c < 0) continue;
            value |= (c & 31) << shift;
            if ((c & 32) == 0) {
                int decoded = value >> 1;
                result.add((value & 1) != 0 ? -decoded : decoded);
                value = 0;
                shift = 0;
            } else {
                shift += 5;
            }
        }
        return result;
    }

    private static class ScriptMapping {
        final List<List<MappingEntry>> lineMappings;
        ScriptMapping(List<List<MappingEntry>> lineMappings) { this.lineMappings = lineMappings; }
    }

    private static class MappingEntry {
        final int jsColumn; final int tsLine; final int tsColumn; final String name;
        MappingEntry(int jsColumn, int tsLine, int tsColumn, String name) {
            this.jsColumn = jsColumn; this.tsLine = tsLine; this.tsColumn = tsColumn; this.name = name;
        }
    }

    public static class OriginalPosition {
        public final int line; public final int column; public final String name;
        public OriginalPosition(int line, int column, String name) {
            this.line = line; this.column = column; this.name = name;
        }
        @Override
        public String toString() {
            if (name != null) return name + " (" + line + ":" + column + ")";
            return line + ":" + column;
        }
    }
}

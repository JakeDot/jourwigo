package cgeo.geocaching.wherigo.openwig;

import cgeo.geocaching.wherigo.kahlua.stdlib.BaseLib;
import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction;
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame;
import cgeo.geocaching.wherigo.kahlua.vm.LuaState;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A resource class for loading translation maps from CSV-formatted text resources.
 * Each translation record is represented as key, language, value triplets.
 * The default fallback language is defined by the file's first triplet line.
 */
public class TranslationResource extends EventTable {

    private String defaultLanguage;
    private final Map<String, Map<String, String>> translations = new HashMap<>();

    public static final JavaFunction CONSTRUCTOR = (callFrame, nArguments) -> {
        Object arg0 = nArguments >= 1 ? callFrame.get(0) : null;
        Object arg1 = nArguments >= 2 ? callFrame.get(1) : null;

        Object sourceObj = arg0;
        if (arg0 instanceof Cartridge && arg1 != null) {
            sourceObj = arg1;
        }

        TranslationResource resource;
        if (sourceObj instanceof Media media) {
            resource = new TranslationResource(media);
        } else if (sourceObj instanceof String str) {
            resource = new TranslationResource(str);
        } else if (sourceObj != null) {
            resource = new TranslationResource(BaseLib.rawTostring(sourceObj));
        } else {
            resource = new TranslationResource();
        }

        if (Engine.instance != null && Engine.instance.cartridge != null) {
            Engine.instance.cartridge.addObject(resource);
        }
        return callFrame.push(resource);
    };

    public static void register(LuaState state) {
        LuaTable environment = state.getEnvironment();
        Object wherigoObj = environment.rawget("Wherigo");
        LuaTable wig;
        if (wherigoObj instanceof LuaTable table) {
            wig = table;
        } else {
            wig = environment;
        }

        if (Engine.instance != null && Engine.instance.savegame != null) {
            Engine.instance.savegame.addJavafunc(CONSTRUCTOR);
        }

        environment.rawset("TranslationResource", CONSTRUCTOR);
        wig.rawset("TranslationResource", CONSTRUCTOR);
    }

    public TranslationResource() {
        initTable();
    }

    public TranslationResource(String csvContent) {
        this();
        loadCsv(csvContent);
    }

    public TranslationResource(Media media) {
        this();
        if (media != null) {
            String text = media.readText();
            if (text != null) {
                loadCsv(text);
            }
        }
    }

    private void initTable() {
        JavaFunction translateFunc = (callFrame, nArguments) -> TranslationResource.this.luaTranslate(callFrame, nArguments);
        table.rawset("translate", translateFunc);
        table.rawset("Translate", translateFunc);

        if (Engine.instance != null && Engine.instance.savegame != null) {
            Engine.instance.savegame.addJavafunc(translateFunc);
        }
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public void loadCsv(String csvContent) {
        if (csvContent == null || csvContent.trim().isEmpty()) {
            return;
        }

        List<List<String>> records = parseCsv(csvContent);
        for (List<String> record : records) {
            if (record.size() >= 3) {
                String key = record.get(0) != null ? record.get(0).trim() : "";
                String lang = record.get(1) != null ? record.get(1).trim() : "";
                String val = record.get(2);

                if (key.isEmpty() || lang.isEmpty()) {
                    continue;
                }

                if (defaultLanguage == null) {
                    defaultLanguage = lang;
                }

                translations.computeIfAbsent(key, k -> new HashMap<>()).put(lang, val);
            }
        }
    }

    public String translate(String key) {
        return translate(key, null);
    }

    public String translate(String key, String lang) {
        if (key == null) {
            return null;
        }

        Map<String, String> langMap = translations.get(key);
        if (langMap == null) {
            return null;
        }

        if (lang != null && !lang.trim().isEmpty()) {
            String val = langMap.get(lang.trim());
            if (val != null) {
                return val;
            }
        }

        if (defaultLanguage != null) {
            String val = langMap.get(defaultLanguage);
            if (val != null) {
                return val;
            }
        }

        return null;
    }

    private int luaTranslate(LuaCallFrame callFrame, int nArguments) {
        if (nArguments < 1) {
            return callFrame.pushNil();
        }

        Object arg0 = callFrame.get(0);
        String key;
        String lang = null;

        if (arg0 instanceof TranslationResource) {
            key = nArguments >= 2 ? BaseLib.rawTostring(callFrame.get(1)) : null;
            lang = nArguments >= 3 ? BaseLib.rawTostring(callFrame.get(2)) : null;
        } else {
            key = BaseLib.rawTostring(arg0);
            lang = nArguments >= 2 ? BaseLib.rawTostring(callFrame.get(1)) : null;
        }

        String result = translate(key, lang);
        return callFrame.push(result);
    }

    @Override
    protected Object getItem(String key) {
        if ("DefaultLanguage".equalsIgnoreCase(key)) {
            return defaultLanguage;
        }
        return super.getItem(key);
    }

    @Override
    protected void setItem(String key, Object value) {
        if ("DefaultLanguage".equalsIgnoreCase(key)) {
            this.defaultLanguage = BaseLib.rawTostring(value);
        } else {
            super.setItem(key, value);
        }
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        out.writeUTF(defaultLanguage == null ? "" : defaultLanguage);
        out.writeInt(translations.size());
        for (Map.Entry<String, Map<String, String>> entry : translations.entrySet()) {
            out.writeUTF(entry.getKey());
            Map<String, String> langMap = entry.getValue();
            out.writeInt(langMap.size());
            for (Map.Entry<String, String> langEntry : langMap.entrySet()) {
                out.writeUTF(langEntry.getKey());
                out.writeUTF(langEntry.getValue() == null ? "" : langEntry.getValue());
            }
        }
        super.serialize(out);
    }

    @Override
    public void deserialize(DataInputStream in) throws IOException {
        String defLang = in.readUTF();
        this.defaultLanguage = defLang.isEmpty() ? null : defLang;
        int size = in.readInt();
        translations.clear();
        for (int i = 0; i < size; i++) {
            String key = in.readUTF();
            int langSize = in.readInt();
            Map<String, String> langMap = new HashMap<>();
            for (int j = 0; j < langSize; j++) {
                String lang = in.readUTF();
                String val = in.readUTF();
                langMap.put(lang, val);
            }
            translations.put(key, langMap);
        }
        super.deserialize(in);
    }

    public static List<List<String>> parseCsv(String input) {
        List<List<String>> records = new ArrayList<>();
        if (input == null || input.trim().isEmpty()) {
            return records;
        }

        char delimiter = ',';
        int commas = 0;
        int semicolons = 0;
        boolean inQuote = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
            } else if (!inQuote) {
                if (c == ',') commas++;
                else if (c == ';') semicolons++;
                else if (c == '\n') break;
            }
        }
        if (semicolons > commas) {
            delimiter = ';';
        }

        List<String> currentRecord = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        inQuote = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (inQuote) {
                if (c == '"') {
                    if (i + 1 < input.length() && input.charAt(i + 1) == '"') {
                        currentField.append('"');
                        i++;
                    } else {
                        inQuote = false;
                    }
                } else {
                    currentField.append(c);
                }
            } else {
                if (c == '"') {
                    inQuote = true;
                } else if (c == delimiter) {
                    currentRecord.add(currentField.toString());
                    currentField.setLength(0);
                } else if (c == '\r') {
                    if (i + 1 < input.length() && input.charAt(i + 1) == '\n') {
                        i++;
                    }
                    currentRecord.add(currentField.toString());
                    currentField.setLength(0);
                    records.add(currentRecord);
                    currentRecord = new ArrayList<>();
                } else if (c == '\n') {
                    currentRecord.add(currentField.toString());
                    currentField.setLength(0);
                    records.add(currentRecord);
                    currentRecord = new ArrayList<>();
                } else {
                    currentField.append(c);
                }
            }
        }
        if (inQuote || currentField.length() > 0 || !currentRecord.isEmpty()) {
            currentRecord.add(currentField.toString());
            records.add(currentRecord);
        }
        return records;
    }

    @Override
    protected String luaTostring() {
        return "a TranslationResource instance";
    }

    @Override
    public String toString() {
        return String.format("TranslationResource(%s)", defaultLanguage != null ? defaultLanguage : "no default");
    }
}

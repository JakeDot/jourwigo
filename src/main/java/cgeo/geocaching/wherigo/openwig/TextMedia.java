package cgeo.geocaching.wherigo.openwig;

import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction;
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class TextMedia extends Media {

    public static final JavaFunction READ = new JavaFunction() {
        @Override
        public int call(LuaCallFrame callFrame, int nArguments) {
            if (nArguments >= 1 && callFrame.get(0) instanceof TextMedia) {
                TextMedia media = (TextMedia) callFrame.get(0);
                return callFrame.push(media.readText());
            }
            return callFrame.push(null);
        }
    };

    public static final JavaFunction SHOW = new JavaFunction() {
        @Override
        public int call(LuaCallFrame callFrame, int nArguments) {
            if (nArguments >= 1 && callFrame.get(0) instanceof TextMedia) {
                TextMedia media = (TextMedia) callFrame.get(0);
                media.show();
            }
            return 0;
        }
    };

    public static void register() {
        if (Engine.instance != null && Engine.instance.savegame != null) {
            Engine.instance.savegame.addJavafunc(READ);
            Engine.instance.savegame.addJavafunc(SHOW);
        }
    }

    public TextMedia() {
        super();
        initTable();
    }

    public TextMedia(String filePath, String type) {
        super(filePath, type);
        initTable();
    }

    private void initTable() {
        table.rawset("Read", READ);
        table.rawset("read", READ);
        table.rawset("GetText", READ);
        table.rawset("getText", READ);
        table.rawset("Show", SHOW);
        table.rawset("show", SHOW);
    }

    public String readText() {
        if (filePath == null) return null;
        File f = new File(filePath);
        if (!f.exists() || !f.isFile()) return null;
        try {
            return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    public void show() {
        String content = readText();
        if (content == null) content = name != null ? name : "Text Media";
        Engine.dialog(new String[]{content}, new Media[]{this});
    }

    @Override
    protected Object getItem(String key) {
        if ("Text".equals(key) || "Content".equals(key)) {
            String content = readText();
            return content != null ? content : super.getItem(key);
        }
        return super.getItem(key);
    }

    @Override
    protected String luaTostring() {
        return "a TextMedia instance";
    }
}

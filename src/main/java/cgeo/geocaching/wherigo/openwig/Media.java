/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig
 */
package cgeo.geocaching.wherigo.openwig;

import cgeo.geocaching.wherigo.kahlua.stdlib.BaseLib;
import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction;
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

public class Media extends EventTable {

    private static int media_no;

    public static void reset () {
        media_no = 1;
    }

    public static final JavaFunction PLAY = new JavaFunction() {
        @Override
        public int call(LuaCallFrame callFrame, int nArguments) {
            if (nArguments >= 1 && callFrame.get(0) instanceof Media) {
                Media media = (Media) callFrame.get(0);
                media.play();
            }
            return 0;
        }
    };

    public static final JavaFunction STOP = new JavaFunction() {
        @Override
        public int call(LuaCallFrame callFrame, int nArguments) {
            if (nArguments >= 1 && callFrame.get(0) instanceof Media) {
                Media media = (Media) callFrame.get(0);
                media.stop();
            }
            return 0;
        }
    };

    public static final JavaFunction SHOW = new JavaFunction() {
        @Override
        public int call(LuaCallFrame callFrame, int nArguments) {
            if (nArguments >= 1 && callFrame.get(0) instanceof Media) {
                Media media = (Media) callFrame.get(0);
                media.show();
            }
            return 0;
        }
    };

    public static final JavaFunction READ = new JavaFunction() {
        @Override
        public int call(LuaCallFrame callFrame, int nArguments) {
            if (nArguments >= 1 && callFrame.get(0) instanceof Media) {
                Media media = (Media) callFrame.get(0);
                return callFrame.push(media.readText());
            }
            return callFrame.push(null);
        }
    };

    public static void register() {
        if (Engine.instance != null && Engine.instance.savegame != null) {
            Engine.instance.savegame.addJavafunc(PLAY);
            Engine.instance.savegame.addJavafunc(STOP);
            Engine.instance.savegame.addJavafunc(SHOW);
            Engine.instance.savegame.addJavafunc(READ);
        }
    }

    public int id;
    public String altText = null;
    public String type = null;
    protected String filePath = null;

    public Media() {
        id = media_no++;
        initTable();
    }

    public Media(String filePath, String type) {
        this();
        setFilePath(filePath);
        if (type != null) {
            this.type = type.toLowerCase(Locale.getDefault());
            table.rawset("Type", this.type);
            table.rawset("MediaType", this.type);
        }
    }

    private void initTable() {
        table.rawset("Play", PLAY);
        table.rawset("play", PLAY);
        table.rawset("Stop", STOP);
        table.rawset("stop", STOP);
        table.rawset("Show", SHOW);
        table.rawset("show", SHOW);
        table.rawset("Read", READ);
        table.rawset("read", READ);
        table.rawset("GetText", READ);
        table.rawset("getText", READ);
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
        if (filePath != null) {
            table.rawset("Filename", filePath);
            table.rawset("FilePath", filePath);
            table.rawset("Path", filePath);
        }
    }

    public void serialize (DataOutputStream out) throws IOException {
        out.writeInt(id);
        out.writeUTF(filePath == null ? "" : filePath);
        out.writeUTF(type == null ? "" : type);
        super.serialize(out);
    }

    public void deserialize (DataInputStream in) throws IOException {
        media_no--; // deserialize must be called directly after construction
        id = in.readInt();
        if (id >= media_no) media_no = id + 1;
        String fp = in.readUTF();
        setFilePath(fp.isEmpty() ? null : fp);
        String t = in.readUTF();
        this.type = t.isEmpty() ? null : t;
        super.deserialize(in);
    }

    @Override
    protected Object getItem (String key) {
        if ("Filename".equals(key) || "FilePath".equals(key) || "Path".equals(key)) {
            return filePath != null ? filePath : super.getItem(key);
        } else if ("Type".equals(key) || "MediaType".equals(key)) {
            return type != null ? type : super.getItem(key);
        } else if ("AltText".equals(key)) {
            return altText != null ? altText : super.getItem(key);
        } else if ("Text".equals(key) || "Content".equals(key)) {
            String text = readText();
            return text != null ? text : super.getItem(key);
        } else {
            return super.getItem(key);
        }
    }

    @Override
    protected void setItem (String key, Object value) {
        if ("Filename".equals(key) || "FilePath".equals(key) || "Path".equals(key)) {
            setFilePath(BaseLib.rawTostring(value));
        } else if ("Type".equals(key) || "MediaType".equals(key)) {
            this.type = BaseLib.rawTostring(value);
            if (this.type != null) {
                this.type = this.type.toLowerCase(Locale.getDefault());
            }
            table.rawset("Type", this.type);
            table.rawset("MediaType", this.type);
        } else if ("AltText".equals(key)) {
            altText = BaseLib.rawTostring(value);
        } else if ("Resources".equals(key)) {
            LuaTable lt = (LuaTable)value;
            int n = lt.len();
            for (int i = 1; i <= n; i++) {
                LuaTable res = (LuaTable)lt.rawget(Double.valueOf(i));
                String t = (String)res.rawget("Type");
                if ("fdl".equals(t)) continue;
                type = t.toLowerCase(Locale.getDefault());
            }
        } else super.setItem(key, value);
    }

    public String jarFilename () {
        return String.valueOf(id)+"."+(type==null ? "" : type);
    }

    public void play () {
        try {
            String mime = null;
            if ("wav".equalsIgnoreCase(type)) mime = "audio/x-wav";
            else if ("mp3".equalsIgnoreCase(type)) mime = "audio/mpeg";
            else if ("ogg".equalsIgnoreCase(type)) mime = "audio/ogg";
            else if ("m4a".equalsIgnoreCase(type) || "aac".equalsIgnoreCase(type)) mime = "audio/mp4";
            byte[] data = Engine.mediaFile(this);
            if (data != null) {
                Engine.ui.playSound(data, mime);
            }
        } catch (IOException e) {
            // meh
        }
    }

    public void stop() {
        Engine.ui.command("StopSound");
    }

    public void show() {
        String content = isTextType() ? readText() : null;
        if (content == null) {
            content = name != null ? name : (altText != null ? altText : "Media");
        }
        Engine.dialog(new String[]{content}, new Media[]{this});
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

    private boolean isTextType() {
        if (type == null) return false;
        String t = type.toLowerCase(Locale.getDefault());
        return t.equals("txt") || t.equals("text") || t.equals("csv") || t.equals("json") ||
               t.equals("xml") || t.equals("html") || t.equals("md") || t.equals("log");
    }

    @Override
    protected String luaTostring() {
        return "a ZMedia instance";
    }

    @Override
    public String toString() {
        return id + ":" + name ;
    }
}

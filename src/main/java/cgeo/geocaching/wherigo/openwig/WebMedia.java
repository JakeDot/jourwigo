/*
 File initially created for jourwigo.
 Package cgeo.geocaching.wherigo.openwig
 */
package cgeo.geocaching.wherigo.openwig;

import cgeo.geocaching.wherigo.kahlua.stdlib.BaseLib;
import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction;
import cgeo.geocaching.wherigo.kahlua.vm.LuaState;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class WebMedia extends Media {

    private String uri;
    private String mimeType = "text/plain";

    public static final JavaFunction CONSTRUCTOR = (callFrame, nArguments) -> {
        String uriParam = null;
        String mimeParam = null;
        if (nArguments >= 1) {
            Object arg0 = callFrame.get(0);
            if (arg0 instanceof Cartridge && nArguments >= 2) {
                uriParam = BaseLib.rawTostring(callFrame.get(1));
                if (nArguments >= 3) {
                    mimeParam = BaseLib.rawTostring(callFrame.get(2));
                }
            } else {
                uriParam = BaseLib.rawTostring(arg0);
                if (nArguments >= 2) {
                    mimeParam = BaseLib.rawTostring(callFrame.get(1));
                }
            }
        }
        WebMedia webMedia = new WebMedia(uriParam, mimeParam);
        if (Engine.instance != null && Engine.instance.cartridge != null) {
            Engine.instance.cartridge.addObject(webMedia);
        }
        return callFrame.push(webMedia);
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

        environment.rawset("WebMedia", CONSTRUCTOR);
        wig.rawset("WebMedia", CONSTRUCTOR);
    }

    public WebMedia() {
        this(null, "text/plain");
    }

    public WebMedia(String uri) {
        this(uri, "text/plain");
    }

    public WebMedia(String uri, String mimeType) {
        super();
        setUri(uri);
        setMimeType(mimeType);
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
        if (table != null) {
            table.rawset("Uri", uri);
            table.rawset("URI", uri);
            table.rawset("Url", uri);
            table.rawset("URL", uri);
        }
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = (mimeType != null && !mimeType.isBlank()) ? mimeType.trim() : "text/plain";
        if (table != null) {
            table.rawset("MimeType", this.mimeType);
        }
    }

    @Override
    public String readText() {
        if (uri == null || uri.isBlank()) {
            return super.readText();
        }
        try (InputStream in = URI.create(uri.trim()).toURL().openStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return super.readText();
        }
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        out.writeUTF(uri == null ? "" : uri);
        out.writeUTF(mimeType == null ? "text/plain" : mimeType);
        super.serialize(out);
    }

    @Override
    public void deserialize(DataInputStream in) throws IOException {
        String u = in.readUTF();
        setUri(u.isEmpty() ? null : u);
        String m = in.readUTF();
        setMimeType(m.isEmpty() ? "text/plain" : m);
        super.deserialize(in);
    }

    @Override
    protected Object getItem(String key) {
        if ("Uri".equalsIgnoreCase(key) || "URL".equalsIgnoreCase(key)) {
            return uri != null ? uri : super.getItem(key);
        } else if ("MimeType".equalsIgnoreCase(key)) {
            return mimeType != null ? mimeType : super.getItem(key);
        } else {
            return super.getItem(key);
        }
    }

    @Override
    protected void setItem(String key, Object value) {
        if ("Uri".equalsIgnoreCase(key) || "URL".equalsIgnoreCase(key)) {
            setUri(BaseLib.rawTostring(value));
        } else if ("MimeType".equalsIgnoreCase(key)) {
            setMimeType(BaseLib.rawTostring(value));
        } else {
            super.setItem(key, value);
        }
    }

    @Override
    protected String luaTostring() {
        return "a WebMedia instance";
    }

    @Override
    public String toString() {
        return "WebMedia(" + (uri != null ? uri : "no-uri") + ", " + mimeType + ")";
    }
}

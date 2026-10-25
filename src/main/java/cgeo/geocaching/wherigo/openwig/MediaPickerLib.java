/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig
 */
package cgeo.geocaching.wherigo.openwig;

import cgeo.geocaching.wherigo.kahlua.stdlib.BaseLib;
import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction;
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame;
import cgeo.geocaching.wherigo.kahlua.vm.LuaState;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;

public enum MediaPickerLib implements JavaFunction {

    SNAP_PHOTO("SnapPhoto", MediaPickerLib::snapPhoto),
    PICK_FILE("PickFile", MediaPickerLib::pickFile);

    private final String luaName;
    private final JavaFunction function;

    MediaPickerLib(String luaName, JavaFunction function) {
        this.luaName = luaName;
        this.function = function;
    }

    public static void register(LuaState state) {
        LuaTable environment = state.getEnvironment();
        Object wherigoObj = environment.rawget("Wherigo");
        LuaTable wig;
        if (wherigoObj instanceof LuaTable) {
            wig = (LuaTable) wherigoObj;
        } else {
            wig = environment;
        }

        for (MediaPickerLib func : values()) {
            if (Engine.instance != null && Engine.instance.savegame != null) {
                Engine.instance.savegame.addJavafunc(func);
            }
            wig.rawset(func.luaName, func);
        }

        ImageMedia.register();
        AudioMedia.register();
        VideoMedia.register();
        TextMedia.register();
    }

    @Override
    public String toString() {
        return luaName;
    }

    @Override
    public int call(LuaCallFrame callFrame, int nArguments) {
        return function.call(callFrame, nArguments);
    }

    private static int snapPhoto(LuaCallFrame callFrame, int nArguments) {
        String photoPath = Engine.ui.snapPhoto();
        Media media = createMedia(photoPath, "image/*");
        callFrame.push(media);
        return 1;
    }

    private static int pickFile(LuaCallFrame callFrame, int nArguments) {
        String mimeType = null;
        if (nArguments >= 1) {
            mimeType = BaseLib.rawTostring(callFrame.get(0));
        }
        String filePath = Engine.ui.pickFile(mimeType);
        Media media = createMedia(filePath, mimeType);
        callFrame.push(media);
        return 1;
    }

    public static Media createMedia(String filePath, String mimeType) {
        if (filePath == null) {
            return null;
        }

        String ext = "";
        int dotIdx = filePath.lastIndexOf('.');
        if (dotIdx >= 0 && dotIdx < filePath.length() - 1) {
            ext = filePath.substring(dotIdx + 1).toLowerCase(java.util.Locale.getDefault());
        }

        String mime = mimeType != null ? mimeType.toLowerCase(java.util.Locale.getDefault()) : "";

        if (mime.startsWith("image/") || isImageExtension(ext)) {
            return new ImageMedia(filePath, ext.isEmpty() ? "jpg" : ext);
        }

        if (mime.startsWith("audio/") || isAudioExtension(ext)) {
            return new AudioMedia(filePath, ext.isEmpty() ? "mp3" : ext);
        }

        if (mime.startsWith("video/") || isVideoExtension(ext)) {
            return new VideoMedia(filePath, ext.isEmpty() ? "mp4" : ext);
        }

        if (mime.startsWith("text/") || isTextExtension(ext)) {
            return new TextMedia(filePath, ext.isEmpty() ? "txt" : ext);
        }

        return new Media(filePath, ext.isEmpty() ? "dat" : ext);
    }

    private static boolean isImageExtension(String ext) {
        return "jpg".equals(ext) || "jpeg".equals(ext) || "png".equals(ext) ||
               "gif".equals(ext) || "webp".equals(ext) || "bmp".equals(ext);
    }

    private static boolean isAudioExtension(String ext) {
        return "mp3".equals(ext) || "wav".equals(ext) || "ogg".equals(ext) ||
               "flac".equals(ext) || "m4a".equals(ext) || "aac".equals(ext);
    }

    private static boolean isVideoExtension(String ext) {
        return "mp4".equals(ext) || "mkv".equals(ext) || "avi".equals(ext) ||
               "mov".equals(ext) || "webm".equals(ext) || "3gp".equals(ext);
    }

    private static boolean isTextExtension(String ext) {
        return "txt".equals(ext) || "csv".equals(ext) || "json".equals(ext) ||
               "xml".equals(ext) || "html".equals(ext) || "md".equals(ext) ||
               "log".equals(ext);
    }
}

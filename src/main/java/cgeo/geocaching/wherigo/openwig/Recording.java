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

public enum Recording implements JavaFunction {

    SNAP_PHOTO("SnapPhoto", Recording::snapPhoto),
    PICK_FILE("PickFile", Recording::pickFile),
    RECORD_AUDIO("RecordAudio", Recording::recordAudio),
    RECORD_VIDEO("RecordVideo", Recording::recordVideo),
    SCAN_BARCODE("ScanBarcode", Recording::scanBarcode);

    private final String luaName;
    private final JavaFunction function;

    Recording(String luaName, JavaFunction function) {
        this.luaName = luaName;
        this.function = function;
    }

    public String getLuaName() {
        return luaName;
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

        for (Recording func : values()) {
            if (Engine.instance != null && Engine.instance.savegame != null) {
                Engine.instance.savegame.addJavafunc(func);
            }
            wig.rawset(func.luaName, func);
        }

        Media.register();
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

    private static int scanBarcode(LuaCallFrame callFrame, int nArguments) {
        String barcodeText = Engine.ui != null ? Engine.ui.scanBarcode() : null;
        callFrame.push(barcodeText);
        return 1;
    }

    private static int pickFile(LuaCallFrame callFrame, int nArguments) {
        String mimeType = null;
        if (nArguments >= 1) {
            mimeType = BaseLib.rawTostring(callFrame.get(0));
        }
        String filePath = Engine.ui != null ? Engine.ui.pickFile(mimeType) : null;
        Media media = createMedia(filePath, mimeType);
        callFrame.push(media);
        return 1;
    }

    private static int recordAudio(LuaCallFrame callFrame, int nArguments) {
        String audioPath = Engine.ui != null ? Engine.ui.recordAudio() : null;
        Media media = createMedia(audioPath, "audio/wav");
        callFrame.push(media);
        return 1;
    }

    private static int recordVideo(LuaCallFrame callFrame, int nArguments) {
        String videoPath = Engine.ui != null ? Engine.ui.recordVideo() : null;
        Media media = createMedia(videoPath, "video/mp4");
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

        String type = ext;
        if (type.isEmpty()) {
            if (mime.startsWith("image/")) type = "jpg";
            else if (mime.startsWith("audio/")) type = "mp3";
            else if (mime.startsWith("video/")) type = "mp4";
            else if (mime.startsWith("text/")) type = "txt";
            else type = "dat";
        }

        Media media = new Media(filePath, type);
        if (Engine.instance != null && Engine.instance.cartridge != null) {
            Engine.instance.cartridge.addObject(media);
        }
        return media;
    }
}

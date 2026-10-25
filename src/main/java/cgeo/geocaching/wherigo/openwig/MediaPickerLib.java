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
            Engine.instance.savegame.addJavafunc(func);
            wig.rawset(func.luaName, func);
        }
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
        callFrame.push(photoPath);
        return 1;
    }

    private static int pickFile(LuaCallFrame callFrame, int nArguments) {
        String mimeType = null;
        if (nArguments >= 1) {
            mimeType = BaseLib.rawTostring(callFrame.get(0));
        }
        String filePath = Engine.ui.pickFile(mimeType);
        callFrame.push(filePath);
        return 1;
    }
}

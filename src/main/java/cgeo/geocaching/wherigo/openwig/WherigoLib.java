/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig
 */
package cgeo.geocaching.wherigo.openwig;

import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction;
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame;
import cgeo.geocaching.wherigo.kahlua.vm.LuaState;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl;
import cgeo.geocaching.wherigo.openwig.platform.UI;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import cgeo.geocaching.wherigo.kahlua.stdlib.BaseLib;

public enum WherigoLib implements JavaFunction {

    COMMAND("Command", WherigoLib::command, WherigoLib.class),
    ZONEPOINT("ZonePoint", WherigoLib::zonePoint, ZonePoint.class),
    DISTANCE("Distance", WherigoLib::distance, Double.class),
    CARTRIDGE("ZCartridge", (callFrame, nArgs) -> construct(Engine.instance.cartridge = new Cartridge(), callFrame, nArgs), Cartridge.class),
    MESSAGEBOX("MessageBox", WherigoLib::messageBox, WherigoLib.class),
    ZONE("Zone", (callFrame, nArgs) -> constructGeneric(Zone.class, callFrame, nArgs), Zone.class),
    DIALOG("Dialog", WherigoLib::dialog, WherigoLib.class),
    ZCHARACTER("ZCharacter", (callFrame, nArgs) -> construct(new Thing(true), callFrame, nArgs), Thing.class),
    ZITEM("ZItem", (callFrame, nArgs) -> construct(new Thing(false), callFrame, nArgs), Thing.class),
    ZCOMMAND("ZCommand", (callFrame, nArgs) -> constructGeneric(Action.class, callFrame, nArgs), Action.class),
    ZMEDIA("ZMedia", (callFrame, nArgs) -> constructGeneric(Media.class, callFrame, nArgs), Media.class),
    WEBMEDIA("WebMedia", WebMedia.CONSTRUCTOR, WebMedia.class),
    ZINPUT("ZInput", (callFrame, nArgs) -> constructGeneric(EventTable.class, callFrame, nArgs), EventTable.class),
    ZTIMER("ZTimer", (callFrame, nArgs) -> constructGeneric(Timer.class, callFrame, nArgs), Timer.class),
    ZTASK("ZTask", (callFrame, nArgs) -> constructGeneric(Task.class, callFrame, nArgs), Task.class),
    AUDIO("PlayAudio", WherigoLib::playAudio, WherigoLib.class),
    GETINPUT("GetInput", WherigoLib::getinput, WherigoLib.class),
    NOCASEEQUALS("NoCaseEquals", WherigoLib::nocaseequals, WherigoLib.class),
    SHOWSCREEN("ShowScreen", WherigoLib::showscreen, WherigoLib.class),
    TRANSLATEPOINT("TranslatePoint", WherigoLib::translatePoint, WherigoLib.class),
    SHOWSTATUSTEXT("ShowStatusText", WherigoLib::showStatusText, WherigoLib.class),
    VECTORTOPOINT("VectorToPoint", WherigoLib::vectorToPoint, WherigoLib.class),
    LOGMESSAGE("LogMessage", WherigoLib::logMessage, WherigoLib.class),
    MADE("made", WherigoLib::made, WherigoLib.class),
    GETVALUE("GetValue", WherigoLib::distanceGetValue, WherigoLib.class);

    private final String luaName;
    private final JavaFunction function;
    private final Class<?> klass;

    WherigoLib(String luaName, JavaFunction function, Class<?> klass) {
        this.luaName = luaName;
        this.function = function;
        this.klass = klass;
    }

    public static final Hashtable env = new Hashtable(); /* Wherigo's Env table */
    public static final String DEVICE_ID = "DeviceID";
    public static final String PLATFORM = "Platform";
    static {
        env.put("Device", "undefined");
        env.put("DeviceID", "undefined");
        env.put("Platform", "MIDP-2.0/CLDC-1.1");
        env.put("CartFolder", "c:/what/is/it/to/you");
        env.put("SyncFolder", "c:/what/is/it/to/you");
        env.put("LogFolder", "c:/what/is/it/to/you");
        env.put("CartFilename", "cartridge.gwc");
        env.put("PathSep", "/"); // no. you may NOT do file i/o on this device.
        env.put("Version", "2.11-compatible(r"+Engine.VERSION+")");
        env.put("Downloaded", Double.valueOf(0));
    }

    public static void register(LuaState state) {

        if (env.get(DEVICE_ID) == null) throw new IllegalStateException("set your DeviceID! WherigoLib.env.put(WherigoLib.DEVICE_ID, \"some value\")");

        LuaTable environment = state.getEnvironment();

        LuaTable wig = new LuaTableImpl();
        environment.rawset("Wherigo", wig);
        for (WherigoLib func : values()) {
            Engine.instance.savegame.addJavafunc(func);
            wig.rawset(func.luaName, func);
        }

        LuaTable distanceMetatable = new LuaTableImpl();
        distanceMetatable.rawset("__index", distanceMetatable);
        distanceMetatable.rawset("__call", GETVALUE);
        distanceMetatable.rawset(GETVALUE.luaName, GETVALUE);
        state.setClassMetatable(Double.class, distanceMetatable);

        state.setClassMetatable(WherigoLib.class, wig);
        wig.rawset("__index", wig);

        wig.rawset("Player", Engine.instance.player);
        wig.rawset("INVALID_ZONEPOINT", null);

        // screen constants
        wig.rawset("MAINSCREEN", Double.valueOf(UI.MAINSCREEN));
        wig.rawset("DETAILSCREEN", Double.valueOf(UI.DETAILSCREEN));
        wig.rawset("ITEMSCREEN", Double.valueOf(UI.ITEMSCREEN));
        wig.rawset("INVENTORYSCREEN", Double.valueOf(UI.INVENTORYSCREEN));
        wig.rawset("LOCATIONSCREEN", Double.valueOf(UI.LOCATIONSCREEN));
        wig.rawset("TASKSCREEN", Double.valueOf(UI.TASKSCREEN));

        LuaTable pack = (LuaTable)environment.rawget("package");
        LuaTable loaded = (LuaTable)pack.rawget("loaded");
        loaded.rawset("Wherigo", wig);

        LuaTable envtable = new LuaTableImpl(); /* Wherigo's Env table */
        Enumeration e = env.keys();
        while (e.hasMoreElements()) {
            String key = (String)e.nextElement();
            envtable.rawset(key, env.get(key));
        }
        envtable.rawset("Device", Engine.instance.gwcfile.device);
        environment.rawset("Env", envtable);

        Cartridge.register();
        Container.register();
        Player.register();
        Timer.register();
        Recording.register(state);
        TranslationResource.register(state);
        WebMedia.register(state);

        Media.reset();
    }

    @Override
    public String toString() {
        return luaName;
    }

    @Override
    public int call(LuaCallFrame callFrame, int nArguments) {
        return function.call(callFrame, nArguments);
    }

    private static int made (LuaCallFrame callFrame, int nArguments) {
        BaseLib.luaAssert(nArguments >= 2, "insufficient arguments for object:made");
        try {
            WherigoLib maker = (WherigoLib)callFrame.get(0);
            Object makee = callFrame.get(1);
            return callFrame.push(LuaState.toBoolean(maker.klass == makee.getClass()));
        } catch (ClassCastException e) { throw new IllegalStateException("bad arguments to object:made"); }
    }

    private static int constructGeneric(Class<? extends EventTable> cls, LuaCallFrame callFrame, int nArguments) {
        try {
            return construct(cls.getDeclaredConstructor().newInstance(), callFrame, nArguments);
        } catch (Exception e) {
            return 0;
        }
    }

    private static int construct(EventTable what, LuaCallFrame callFrame, int nArguments) {
        Object param = callFrame.get(0);
        Cartridge c = null;
        if (param instanceof Cartridge) {
            c = (Cartridge)param;
        } else if (param instanceof LuaTable) {
            LuaTable lt = (LuaTable)param;
            c = (Cartridge)lt.rawget("Cartridge");
            what.setTable((LuaTable)param);
            if (what instanceof Container) {
                Container cont = (Container)what;
                Container target = (Container)lt.rawget("Container");
                if (target != null)
                    cont.moveTo(target);
            }
        }
        if (c == null) c = Engine.instance.cartridge;
        c.addObject(what);
        return callFrame.push(what);
    }

    private static int zonePoint (LuaCallFrame callFrame, int nArguments) {
        if (nArguments == 0) {
            return callFrame.push(new ZonePoint());
        } else {
            BaseLib.luaAssert(nArguments >= 2, "insufficient arguments for ZonePoint");
            double a = LuaState.fromDouble(callFrame.get(0));
            double b = LuaState.fromDouble(callFrame.get(1));
            double c = 0;
            if (nArguments > 2) c = LuaState.fromDouble(callFrame.get(2));
            return callFrame.push(new ZonePoint(a,b,c));
        }
    }

    private static int distance (LuaCallFrame callFrame, int nArguments) {
        double a = LuaState.fromDouble(callFrame.get(0));
        String b = (String)callFrame.get(1);
        double dist = ZonePoint.convertDistanceFrom(a, b);
        return callFrame.push(LuaState.toDouble(dist));
    }

    private static int distanceGetValue (LuaCallFrame callFrame, int nArguments) {
        double a = LuaState.fromDouble(callFrame.get(0));
        String b = (String)callFrame.get(1);
        double dist = ZonePoint.convertDistanceTo(a, b);
        return callFrame.push(LuaState.toDouble(dist));
    }

    private static int messageBox (LuaCallFrame callFrame, int nArguments) {
        LuaTable lt = (LuaTable)callFrame.get(0);
        Engine.message(lt);
        return 0;
    }

    private static int dialog (LuaCallFrame callFrame, int nArguments) {
        LuaTable lt = (LuaTable)callFrame.get(0);
        int n = lt.len();
        String[] texts = new String[n];
        Media[] media = new Media[n];
        for (int i = 1; i <= n; i++) {
            LuaTable item = (LuaTable)lt.rawget(Double.valueOf(i));
            texts[i-1] = Engine.removeHtml((String)item.rawget("Text"));
            media[i-1] = (Media)item.rawget("Media");
        }
        Engine.dialog(texts, media);
        return 0;
    }

    private static int nocaseequals (LuaCallFrame callFrame, int nArguments) {
        Object a = callFrame.get(0); Object b = callFrame.get(1);
        String aa = a == null ? null : a.toString();
        String bb = b == null ? null : b.toString();
        boolean result = (aa == bb || (aa != null && aa.equalsIgnoreCase(bb)));
        return callFrame.push(LuaState.toBoolean(result));
    }

    private static int getinput (LuaCallFrame callFrame, int nArguments) {
        EventTable lt = (EventTable)callFrame.get(0);
        Engine.input(lt);
        return 1;
    }

    private static int showscreen (LuaCallFrame callFrame, int nArguments) {
        int screen = (int)LuaState.fromDouble(callFrame.get(0));
        UI.ScreenId screenId = UI.ScreenId.fromCode(screen);
        EventTable et = null;
        if (nArguments > 1) {
            Object o = callFrame.get(1);
            if (o instanceof EventTable) et = (EventTable)o;
        }
        Engine.log("CALL: ShowScreen("+screen+") " + (et == null ? "" : et.name), Engine.LOG_CALL);
        Engine.ui.showScreen(screenId, et);
        return 0;
    }

    private static int translatePoint (LuaCallFrame callFrame, int nArguments) {
        BaseLib.luaAssert(nArguments >= 3, "insufficient arguments for TranslatePoint");
        ZonePoint z = (ZonePoint)callFrame.get(0);
        double dist = LuaState.fromDouble(callFrame.get(1));
        double angle = LuaState.fromDouble(callFrame.get(2));
        return callFrame.push(z.translate(angle, dist));
    }

    private static int vectorToPoint (LuaCallFrame callFrame, int nArguments) {
        BaseLib.luaAssert(nArguments >= 2, "insufficient arguments for VectorToPoint");
        ZonePoint a = (ZonePoint)callFrame.get(0);
        ZonePoint b = (ZonePoint)callFrame.get(1);
        double bearing = ZonePoint.angle2azimuth(b.bearing(a));
        double distance = b.distance(a);
        return callFrame.push(LuaState.toDouble(distance), LuaState.toDouble(bearing));
    }

    private static int playAudio (LuaCallFrame callFrame, int nArguments) {
        Media m = (Media)callFrame.get(0);
        m.play();
        return 0;
    }

    private static int showStatusText (LuaCallFrame callFrame, int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "insufficient arguments for ShowStatusText");
        String text = (String)callFrame.get(0);
        if (text != null && text.length() == 0) text = null;
        Engine.ui.setStatusText(text);
        return 0;
    }

    private static int logMessage (LuaCallFrame callFrame, int nArguments) {
        if (nArguments < 1) return 0;
        Object arg = callFrame.get(0);
        String text;
        if (arg instanceof LuaTable) {
            LuaTable lt = (LuaTable)arg;
            text = (String)lt.rawget("Text");
        } else {
            text = arg.toString();
        }
        if (text != null && text.length() == 0) return 0;
        Engine.log("CUST: " + text, Engine.LOG_CALL);
        return 0;
    }

    private static int command(LuaCallFrame callFrame, int nArguments) {
      BaseLib.luaAssert(nArguments >= 1, "insufficient arguments for Command");
      String cmd = (String) callFrame.get(0);
      if (cmd != null && cmd.length() == 0)
        cmd = null;
      Engine.ui.command(cmd);
      return 0;
    }
}

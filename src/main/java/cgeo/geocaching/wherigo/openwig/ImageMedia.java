package cgeo.geocaching.wherigo.openwig;

import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction;
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame;

public class ImageMedia extends Media {

    public static final JavaFunction SHOW = new JavaFunction() {
        @Override
        public int call(LuaCallFrame callFrame, int nArguments) {
            if (nArguments >= 1 && callFrame.get(0) instanceof ImageMedia) {
                ImageMedia media = (ImageMedia) callFrame.get(0);
                media.show();
            }
            return 0;
        }
    };

    public static final JavaFunction PLAY = new JavaFunction() {
        @Override
        public int call(LuaCallFrame callFrame, int nArguments) {
            if (nArguments >= 1 && callFrame.get(0) instanceof ImageMedia) {
                ImageMedia media = (ImageMedia) callFrame.get(0);
                media.show();
            }
            return 0;
        }
    };

    public static void register() {
        if (Engine.instance != null && Engine.instance.savegame != null) {
            Engine.instance.savegame.addJavafunc(SHOW);
            Engine.instance.savegame.addJavafunc(PLAY);
        }
    }

    public ImageMedia() {
        super();
        initTable();
    }

    public ImageMedia(String filePath, String type) {
        super(filePath, type);
        initTable();
    }

    private void initTable() {
        table.rawset("Show", SHOW);
        table.rawset("show", SHOW);
        table.rawset("Play", PLAY);
        table.rawset("play", PLAY);
    }

    public void show() {
        Engine.dialog(new String[]{name != null ? name : (altText != null ? altText : "Image")}, new Media[]{this});
    }

    @Override
    protected String luaTostring() {
        return "an ImageMedia instance";
    }
}

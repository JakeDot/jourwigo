package net.jakedot.jourwigo;

import cgeo.geocaching.wherigo.openwig.EventTable;
import cgeo.geocaching.wherigo.openwig.Media;
import cgeo.geocaching.wherigo.openwig.platform.UI;
import cgeo.geocaching.wherigo.kahlua.vm.LuaClosure;

import java.util.Scanner;

/**
 * Console-based implementation of the Wherigo {@link UI} interface.
 * Renders game output as text to stdout and reads user input from stdin.
 */
public class ConsoleUI implements UI {

    private final Scanner scanner = new Scanner(System.in);
    private volatile boolean running = false;

    @Override
    public void refresh() {
        // Nothing to refresh for console output
    }

    @Override
    public void start() {
        running = true;
        System.out.println("[Wherigo] Game started.");
    }

    @Override
    public void end() {
        running = false;
        System.out.println("[Wherigo] Game ended.");
    }

    @Override
    public void showError(String msg) {
        System.err.println("[ERROR] " + msg);
    }

    @Override
    public void debugMsg(String msg) {
        System.out.print("[DEBUG] " + msg);
    }

    @Override
    public void setStatusText(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        System.out.println("[Status] " + text);
    }

    @Override
    public void pushDialog(String[] texts, Media[] media, String button1, String button2, LuaClosure callback) {
        System.out.println();
        System.out.println("--- Dialog ---");
        for (String text : texts) {
            if (text != null) System.out.println(text);
        }
        System.out.println("--------------");

        String btn1Label = (button1 != null) ? button1 : "OK";
        String btn2Label = button2;

        if (btn2Label == null) {
            System.out.println("[Press ENTER for: " + btn1Label + "]");
            scanner.nextLine();
            invokeCallbackIfPresent(callback, "Button1");
        } else {
            System.out.println("[1] " + btn1Label + "  [2] " + btn2Label);
            System.out.print("Choice: ");
            String choice = scanner.nextLine().trim();
            if ("2".equals(choice)) {
                invokeCallbackIfPresent(callback, "Button2");
            } else {
                invokeCallbackIfPresent(callback, "Button1");
            }
        }
    }

    @Override
    public void pushInput(EventTable input) {
        System.out.println();
        System.out.println("--- Input Request ---");
        String text = (String) input.table.rawget("Text");
        if (text != null) System.out.println(text);

        String type = (String) input.table.rawget("Type");
        String answer;

        if ("MultipleChoice".equalsIgnoreCase(type)) {
            Object choices = input.table.rawget("Choices");
            if (choices instanceof cgeo.geocaching.wherigo.kahlua.vm.LuaTable lt) {
                int n = lt.len();
                for (int i = 1; i <= n; i++) {
                    System.out.println("  " + i + ". " + lt.rawget(new Double(i)));
                }
                System.out.print("Enter number: ");
                String numStr = scanner.nextLine().trim();
                try {
                    int idx = Integer.parseInt(numStr);
                    answer = (String) lt.rawget(new Double(idx));
                } catch (NumberFormatException e) {
                    answer = numStr;
                }
            } else {
                System.out.print("Your answer: ");
                answer = scanner.nextLine();
            }
        } else {
            System.out.print("Your answer: ");
            answer = scanner.nextLine();
        }

        System.out.println("---------------------");
        cgeo.geocaching.wherigo.openwig.Engine.callEvent(input, "OnGetInput", answer);
    }

    @Override
    public void showScreen(ScreenId screenId, EventTable details) {
        System.out.println();
        System.out.println("=== Screen: " + screenName(screenId) + " ===");
        if (details != null) {
            System.out.println("  Item: " + details.name);
            if (details.description != null && !details.description.isEmpty()) {
                System.out.println("  " + details.description);
            }
        }
    }

    @Override
    public void playSound(byte[] data, String mime) {
        System.out.println("[Sound] Playing audio (" + mime + ", " + (data != null ? data.length : 0) + " bytes)");
    }

    @Override
    public void blockForSaving() {
        System.out.println("[Saving game...]");
    }

    @Override
    public void unblock() {
        System.out.println("[Game saved.]");
    }

    @Override
    public void command(String cmd) {
        if (cmd == null) {
            return;
        }
        System.out.println("[Command] " + cmd);
    }

    private static String screenName(ScreenId screenId) {
        return switch (screenId) {
            case MAINSCREEN -> "Main";
            case DETAILSCREEN -> "Detail";
            case INVENTORYSCREEN -> "Inventory";
            case ITEMSCREEN -> "Item";
            case LOCATIONSCREEN -> "Locations";
            case TASKSCREEN -> "Tasks";
            case UNKNOWN -> "Unknown";
        };
    }

    private static void invokeCallbackIfPresent(LuaClosure callback, String value) {
        if (callback != null) {
            cgeo.geocaching.wherigo.openwig.Engine.invokeCallback(callback, value);
        }
    }
}

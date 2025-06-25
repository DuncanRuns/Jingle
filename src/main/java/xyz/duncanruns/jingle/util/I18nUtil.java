package xyz.duncanruns.jingle.util;

import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.i18n.I18nManager;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

public class I18nUtil {
    public static String getString(String key) {
        return I18nManager.getString(key);
    }

    public static String format(String input) {
        if (" ".equals(input)){
            return "space";
        }
        return input.replaceAll("[^a-zA-Z0-9 ]", "").replaceAll(" ", "_").toLowerCase();
    }
}

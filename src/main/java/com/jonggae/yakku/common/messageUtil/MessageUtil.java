package com.jonggae.yakku.common.messageUtil;

import java.text.MessageFormat;

public class MessageUtil {

    public static String getMessage(String key) {
        return key;
    }

    public static String getFormattedMessage(String key, Object... arg) {
        return MessageFormat.format(key, arg);
    }
}

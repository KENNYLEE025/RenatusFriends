package com.nova.common.renatusFriends.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class MessageUtils
{
    private MessageUtils()
    {
    }

    public static Component prefix()
    {
        return Component.text("[Friends] ", NamedTextColor.GREEN);
    }

    public static Component success(String message)
    {
        return prefix().append(Component.text(message, NamedTextColor.GREEN));
    }

    public static Component error(String message)
    {
        return prefix().append(Component.text(message, NamedTextColor.RED));
    }

    public static Component info(String message)
    {
        return prefix().append(Component.text(message, NamedTextColor.GRAY));
    }

    public static Component warning(String message)
    {
        return prefix().append(Component.text(message, NamedTextColor.YELLOW));
    }
}
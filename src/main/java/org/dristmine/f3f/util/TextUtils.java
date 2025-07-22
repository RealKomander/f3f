package org.dristmine.f3f.util;

import net.minecraft.text.*;
import net.minecraft.util.Formatting;

public class TextUtils {

    /**
     * Converts Bukkit-style color codes (&c, &l, etc.) to Minecraft Text components
     */
    public static Text parseColorCodes(String text) {
        MutableText result = Text.empty();
        MutableText current = Text.literal("");

        for (int i = 0; i < text.length(); i++) {
            if (i < text.length() - 1 && text.charAt(i) == '&') {
                char colorCode = text.charAt(i + 1);

                // Apply current text segment before changing formatting
                if (!current.getString().isEmpty()) {
                    result.append(current);
                    current = Text.literal("");
                }

                // Apply formatting based on color code
                switch (colorCode) {
                    case '0': current = Text.literal("").formatted(Formatting.BLACK); break;
                    case '1': current = Text.literal("").formatted(Formatting.DARK_BLUE); break;
                    case '2': current = Text.literal("").formatted(Formatting.DARK_GREEN); break;
                    case '3': current = Text.literal("").formatted(Formatting.DARK_AQUA); break;
                    case '4': current = Text.literal("").formatted(Formatting.DARK_RED); break;
                    case '5': current = Text.literal("").formatted(Formatting.DARK_PURPLE); break;
                    case '6': current = Text.literal("").formatted(Formatting.GOLD); break;
                    case '7': current = Text.literal("").formatted(Formatting.GRAY); break;
                    case '8': current = Text.literal("").formatted(Formatting.DARK_GRAY); break;
                    case '9': current = Text.literal("").formatted(Formatting.BLUE); break;
                    case 'a': current = Text.literal("").formatted(Formatting.GREEN); break;
                    case 'b': current = Text.literal("").formatted(Formatting.AQUA); break;
                    case 'c': current = Text.literal("").formatted(Formatting.RED); break;
                    case 'd': current = Text.literal("").formatted(Formatting.LIGHT_PURPLE); break;
                    case 'e': current = Text.literal("").formatted(Formatting.YELLOW); break;
                    case 'f': current = Text.literal("").formatted(Formatting.WHITE); break;
                    case 'k': current = Text.literal("").formatted(Formatting.OBFUSCATED); break;
                    case 'l': current = Text.literal("").formatted(Formatting.BOLD); break;
                    case 'm': current = Text.literal("").formatted(Formatting.STRIKETHROUGH); break;
                    case 'n': current = Text.literal("").formatted(Formatting.UNDERLINE); break;
                    case 'o': current = Text.literal("").formatted(Formatting.ITALIC); break;
                    case 'r': current = Text.literal("").formatted(Formatting.RESET); break;
                    default:
                        // Not a valid color code, treat as regular characters
                        current.append("&" + colorCode);
                        break;
                }
                i++; // Skip the color code character
            } else {
                current.append(String.valueOf(text.charAt(i)));
            }
        }

        // Add remaining text
        if (!current.getString().isEmpty()) {
            result.append(current);
        }

        return result;
    }

    public static Text createDebugMessage(String message) {
        return parseColorCodes("&l&e[Debug]: &r" + message);
    }

    public static Text createDebugMessage(String messageKey, Object... args) {
        String rawMessage = Text.translatable(messageKey, args).getString();
        return parseColorCodes("&l&e[Debug]: &r" + rawMessage);
    }

    public static Text createPermissionDeniedMessage() {
        return parseColorCodes("&l&e[Debug]: &rUnable to change render distance; no permission");
    }

    public static Text createRenderDistanceMessage(int renderDistance) {
        return parseColorCodes("&l&e[Debug]: &rRender Distance: " + renderDistance);
    }
}

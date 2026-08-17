package su.deworld.dwchat.util;

import java.awt.Color;

public final class ColorUtil {

    private static final int[] MC_COLORS = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
            0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
            0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };
    private static final char[] MC_CODES = {
            '0','1','2','3','4','5','6','7',
            '8','9','a','b','c','d','e','f'
    };

    private ColorUtil() {}

    public static String discordColorToLegacy(Color color) {
        if (color == null) return "";
        int rgb = color.getRGB() & 0xFFFFFF;
        int nearest = 0;
        double minDist = Double.MAX_VALUE;
        for (int i = 0; i < MC_COLORS.length; i++) {
            double d = dist(rgb, MC_COLORS[i]);
            if (d < minDist) { minDist = d; nearest = i; }
        }
        return "&" + MC_CODES[nearest];
    }

    private static double dist(int c1, int c2) {
        int dr = ((c1>>16)&0xFF)-((c2>>16)&0xFF);
        int dg = ((c1>>8 )&0xFF)-((c2>>8 )&0xFF);
        int db = ( c1     &0xFF)-( c2     &0xFF);
        return Math.sqrt((double)dr*dr+(double)dg*dg+(double)db*db);
    }
}
package net.mca.client.resources;

import java.util.function.BiConsumer;

public class ClientUtils {
    public static void bethlehemLine(int x0, int y0, int x1, int y1, BiConsumer<Integer, Integer> callback) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0;
        int y = y0;

        while (true) {
            callback.accept(x, y);
            if (x == x1 && y == y1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }

}

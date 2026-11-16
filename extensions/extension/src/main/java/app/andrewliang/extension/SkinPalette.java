package app.andrewliang.extension;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;
import android.widget.ImageView;

/**
 * The average colors of a chat background image near the top and the bottom of the screen, for
 * "Auto color". The image is drawn once into a tiny bitmap, the way it is cropped on screen, and
 * averaged. This runs only when the image, its crop or the screen size changes, posted after the
 * frame, never in a draw. {@link #check} itself only compares a few cached values.
 */
final class SkinPalette {

    /** The width of the sample. The height follows the screen's aspect ratio. */
    private static final int SAMPLE_WIDTH = 32;
    /** How far from the top and from the bottom the image is averaged. */
    private static final float TOP_DP = ProgressiveBlurDrawable.TOP_FADE_DP;
    private static final float BOTTOM_DP = 110f;

    boolean valid;
    int top = Color.BLACK;
    int bottom = Color.BLACK;

    private Drawable drawable;
    private Bitmap bitmap;
    private int generation;
    private final Rect bounds = new Rect();
    private int width;
    private int height;
    private boolean pending;
    private boolean checked;

    /**
     * Samples again, after this frame, when the image or its crop changed since the last sample.
     * [onSampled] runs once the new colors are in.
     */
    void check(View skin, boolean artwork, Runnable onSampled) {
        if (skin == null) return;
        Drawable current = artwork ? drawableOf(skin) : null;
        if (current == null) {
            if (valid || !checked) {
                valid = false;
                checked = true;
                drawable = null;
                bitmap = null;
                if (onSampled != null) onSampled.run();
            }
            return;
        }
        Bitmap currentBitmap = current instanceof BitmapDrawable ? ((BitmapDrawable) current).getBitmap() : null;
        int currentGeneration = currentBitmap != null && !currentBitmap.isRecycled() ? currentBitmap.getGenerationId() : 0;
        boolean same = checked && current == drawable && currentBitmap == bitmap && currentGeneration == generation
                && current.getBounds().equals(bounds) && skin.getWidth() == width && skin.getHeight() == height;
        if (same || pending) return;
        pending = true;
        skin.post(() -> {
            pending = false;
            Drawable now = drawableOf(skin);
            drawable = now;
            bitmap = now instanceof BitmapDrawable ? ((BitmapDrawable) now).getBitmap() : null;
            generation = bitmap != null && !bitmap.isRecycled() ? bitmap.getGenerationId() : 0;
            if (now != null) bounds.set(now.getBounds());
            width = skin.getWidth();
            height = skin.getHeight();
            checked = true;
            valid = now != null && sample(skin, now);
            if (onSampled != null) onSampled.run();
        });
    }

    private static Drawable drawableOf(View skin) {
        Drawable d = skin instanceof ImageView ? ((ImageView) skin).getDrawable() : null;
        return d != null ? d : skin.getBackground();
    }

    private boolean sample(View skin, Drawable image) {
        int w = skin.getWidth();
        int h = skin.getHeight();
        if (w <= 0 || h <= 0) return false;
        int sw = SAMPLE_WIDTH;
        int sh = Math.max(8, Math.round(sw * h / (float) w));
        Bitmap small = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888);
        try {
            Canvas canvas = new Canvas(small);
            canvas.scale(sw / (float) w, sh / (float) h);
            try {
                image.draw(canvas);
            } catch (Throwable hardware) {
                // A hardware bitmap cannot be drawn on a software canvas: draw a software copy.
                small.eraseColor(0);
                if (!drawSoftware(canvas, image)) return false;
            }
            int[] pixels = new int[sw * sh];
            small.getPixels(pixels, 0, sw, 0, 0, sw, sh);
            float density = skin.getResources().getDisplayMetrics().density;
            int topRows = clampRows(Math.round(TOP_DP * density * sh / h), sh);
            int bottomRows = clampRows(Math.round(BOTTOM_DP * density * sh / h), sh);
            int t = average(pixels, sw, 0, topRows);
            int b = average(pixels, sw, sh - bottomRows, sh);
            if (t == 0 || b == 0) return false;
            top = t;
            bottom = b;
            return true;
        } catch (Throwable error) {
            return false;
        } finally {
            small.recycle();
        }
    }

    private static int clampRows(int rows, int total) {
        return Math.max(1, Math.min(rows, total));
    }

    /** The alpha-weighted average color of rows [from] to [to]; 0 when they are transparent. */
    private static int average(int[] pixels, int width, int from, int to) {
        long r = 0, g = 0, b = 0, weight = 0;
        for (int y = from; y < to; y++) {
            for (int x = 0; x < width; x++) {
                int c = pixels[y * width + x];
                int a = c >>> 24;
                if (a == 0) continue;
                r += (long) ((c >> 16) & 0xFF) * a;
                g += (long) ((c >> 8) & 0xFF) * a;
                b += (long) (c & 0xFF) * a;
                weight += a;
            }
        }
        if (weight < 255L * width) return 0;
        return Color.rgb((int) (r / weight), (int) (g / weight), (int) (b / weight));
    }

    /** Draws [drawable] with software copies of its hardware bitmaps. */
    private static boolean drawSoftware(Canvas canvas, Drawable drawable) {
        if (drawable instanceof LayerDrawable) {
            LayerDrawable layers = (LayerDrawable) drawable;
            boolean any = false;
            for (int i = 0; i < layers.getNumberOfLayers(); i++) {
                Drawable layer = layers.getDrawable(i);
                if (layer != null && drawSoftware(canvas, layer)) any = true;
            }
            return any;
        }
        if (drawable instanceof BitmapDrawable) {
            Bitmap source = ((BitmapDrawable) drawable).getBitmap();
            if (source == null || source.isRecycled()) return false;
            Bitmap copy = source.getConfig() == Bitmap.Config.HARDWARE
                    ? source.copy(Bitmap.Config.ARGB_8888, false) : source;
            if (copy == null) return false;
            canvas.drawBitmap(copy, null, drawable.getBounds(), new Paint(Paint.FILTER_BITMAP_FLAG));
            if (copy != source) copy.recycle();
            return true;
        }
        try {
            drawable.draw(canvas);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}

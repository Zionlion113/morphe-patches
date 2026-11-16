package app.andrewliang.extension;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * The background of a bar view next to a view with theme artwork, such as the status bar view
 * above a header or the gesture bar strip below the bottom tabs. It draws the artwork view again,
 * mirrored at the edge where the two views meet. Thus the artwork goes on into the bar without a
 * seam, and the artwork view itself does not change.
 */
final class MirroredArtworkDrawable extends Drawable {

    private final View artwork;
    private final View bar;
    private final boolean barAbove;
    private final int[] artworkLocation = new int[2];
    private final int[] barLocation = new int[2];
    private boolean drawing;

    /**
     * @param artwork the view with the artwork
     * @param bar the view that has this drawable as its background
     * @param barAbove true when [bar] is above [artwork], false when it is below
     */
    MirroredArtworkDrawable(View artwork, View bar, boolean barAbove) {
        this.artwork = artwork;
        this.bar = bar;
        this.barAbove = barAbove;
    }

    View artwork() {
        return artwork;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty() || drawing || !artwork.isAttachedToWindow() || artwork.getHeight() == 0) return;
        drawing = true;
        int save = canvas.save();
        try {
            artwork.getLocationInWindow(artworkLocation);
            bar.getLocationInWindow(barLocation);
            canvas.clipRect(bounds);
            // The mirror line is the artwork edge that faces the bar, in the coordinates of the bar.
            float edge = (barAbove ? artworkLocation[1] : artworkLocation[1] + artwork.getHeight()) - barLocation[1];
            canvas.translate(artworkLocation[0] - barLocation[0], 2 * edge);
            canvas.scale(1f, -1f);
            canvas.translate(0, artworkLocation[1] - barLocation[1]);
            artwork.draw(canvas);
        } finally {
            canvas.restoreToCount(save);
            drawing = false;
        }
    }

    @Override
    public void setAlpha(int alpha) {}

    @Override
    public void setColorFilter(ColorFilter colorFilter) {}

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}

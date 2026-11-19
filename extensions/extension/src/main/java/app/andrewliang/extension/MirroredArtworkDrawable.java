package app.andrewliang.extension;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import java.lang.ref.WeakReference;

final class MirroredArtworkDrawable extends Drawable {
    private final WeakReference<View> artwork;
    private final WeakReference<View> bar;
    private final boolean barAbove;
    private boolean drawing;
    private final int[] artworkLocation = new int[2];
    private final int[] barLocation = new int[2];

    @Override // android.graphics.drawable.Drawable
    public int getOpacity() {
        return -3;
    }

    @Override // android.graphics.drawable.Drawable
    public void setAlpha(int i) {
    }

    @Override // android.graphics.drawable.Drawable
    public void setColorFilter(ColorFilter colorFilter) {
    }

    MirroredArtworkDrawable(View view, View view2, boolean z) {
        this.artwork = new WeakReference<>(view);
        this.bar = new WeakReference<>(view2);
        this.barAbove = z;
    }

    View artwork() {
        return this.artwork.get();
    }

    @Override // android.graphics.drawable.Drawable
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        View artworkView = this.artwork.get();
        View barView = this.bar.get();
        if (bounds.isEmpty() || this.drawing || artworkView == null || barView == null || !artworkView.isAttachedToWindow() || artworkView.getHeight() == 0) {
            return;
        }
        this.drawing = true;
        int iSave = canvas.save();
        try {
            artworkView.getLocationInWindow(this.artworkLocation);
            barView.getLocationInWindow(this.barLocation);
            canvas.clipRect(bounds);
            boolean z = this.barAbove;
            int[] iArr = this.artworkLocation;
            int height = z ? iArr[1] : iArr[1] + artworkView.getHeight();
            int[] iArr2 = this.barLocation;
            canvas.translate(this.artworkLocation[0] - iArr2[0], (height - iArr2[1]) * 2.0f);
            canvas.scale(1.0f, -1.0f);
            canvas.translate(0.0f, this.artworkLocation[1] - this.barLocation[1]);
            artworkView.draw(canvas);
        } finally {
            canvas.restoreToCount(iSave);
            this.drawing = false;
        }
    }
}

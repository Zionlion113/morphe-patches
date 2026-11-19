package app.andrewliang.extension;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RecordingCanvas;
import android.graphics.Rect;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import java.util.Map;
import java.util.WeakHashMap;

final class ChatPanels {
    private static final float BLUR_RADIUS_DP = 20.0f;
    private static final String CONTAINER = "chat_ui_keyboard_container";
    private static final int DAY_PERF_FILL = -856295947;
    private static final int DAY_SAVER_FILL = -657931;
    private static final int NIGHT_PERF_FILL = -870704614;
    private static final int NIGHT_SAVER_FILL = -15066598;
    private static final float SAVER_FADE_DP = 20.0f;
    private static final String[] PLACEHOLDERS = {"chat_ui_keyboard_placeholder", "chat_ui_custom_input_placeholder"};
    private static final Map<View, State> STATES = new WeakHashMap();

    private ChatPanels() {
    }

    static void apply(View view, View view2, View view3) {
        FrostDrawable frostDrawable;
        boolean z;
        View viewFind = find(view, CONTAINER);
        if (viewFind == null) {
            return;
        }
        View viewVisiblePanel = visiblePanel(viewFind);
        Map<View, State> map = STATES;
        State state = map.get(viewFind);
        if (viewVisiblePanel == null) {
            if (state == null || !state.applied) {
                return;
            }
            restore(viewFind, state);
            return;
        }
        if (state == null) {
            state = new State();
            map.put(viewFind, state);
        }
        boolean z2 = true;
        if (!state.applied) {
            clearSolidBackgrounds(viewVisiblePanel, state);
            state.applied = true;
        } else {
            clearSolidBackgrounds(viewVisiblePanel, state);
        }
        if (viewFind.getBackground() instanceof FrostDrawable) {
            frostDrawable = (FrostDrawable) viewFind.getBackground();
            z = false;
        } else {
            frostDrawable = new FrostDrawable(viewFind);
            viewFind.setBackground(frostDrawable);
            z = true;
        }
        if (frostDrawable.sources == null || frostDrawable.sources.length != 2 || frostDrawable.sources[0] != view2 || frostDrawable.sources[1] != view3) {
            frostDrawable.sources = new View[]{view2, view3};
            z = true;
        }
        if ((view2 == null || !view2.isDirty()) && (view3 == null || !view3.isDirty())) {
            z2 = false;
        }
        if (z || z2) {
            viewFind.invalidate();
        }
    }

    static View visiblePanel(View view) {
        if ((view instanceof ViewGroup) && view.isShown() && view.getHeight() > 0) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View childAt = viewGroup.getChildAt(i);
                if (childAt != null && childAt.isShown() && childAt.getHeight() > 0 && !isPlaceholder(childAt)) {
                    return childAt;
                }
            }
        }
        return null;
    }

    static int chromeTop(View view, View view2) {
        int i;
        int[] iArr = new int[2];
        if (view == null || !view.isShown() || view.getHeight() <= 0) {
            i = Integer.MAX_VALUE;
        } else {
            view.getLocationInWindow(iArr);
            i = iArr[1];
        }
        if (view2 != null) {
            find(view2, CONTAINER);
        }
        if (i == Integer.MAX_VALUE) {
            return 0;
        }
        return i;
    }

    static int coverBottom(View view, View view2, View view3) {
        int[] iArr = new int[2];
        int iMax = 0;
        if (view != null && view.isShown() && view.getHeight() > 0) {
            view.getLocationInWindow(iArr);
            iMax = Math.max(0, iArr[1] + view.getHeight());
        }
        if (view2 != null && view2.isShown() && view2.getHeight() > 0) {
            view2.getLocationInWindow(iArr);
            iMax = Math.max(iMax, iArr[1] + view2.getHeight());
        }
        View viewFind = view3 == null ? null : find(view3, CONTAINER);
        if (viewFind == null || visiblePanel(viewFind) == null) {
            return iMax;
        }
        viewFind.getLocationInWindow(iArr);
        return Math.max(iMax, iArr[1] + viewFind.getHeight());
    }

    private static boolean isPlaceholder(View view) {
        int id = view.getId();
        if (id == 0) {
            return false;
        }
        for (String str : PLACEHOLDERS) {
            if (id == BlurBars.id(view.getContext(), str)) {
                return true;
            }
        }
        return false;
    }

    private static void clearSolidBackgrounds(View view, State state) {
        if (view == null) {
            return;
        }
        Drawable background = view.getBackground();
        if ((background instanceof ColorDrawable) && ((ColorDrawable) background).getColor() != 0) {
            if (!state.backgrounds.containsKey(view)) {
                state.backgrounds.put(view, background);
            }
            view.setBackground(new ColorDrawable(0));
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            int iMin = Math.min(viewGroup.getChildCount(), 16);
            for (int i = 0; i < iMin; i++) {
                clearSolidBackgrounds(viewGroup.getChildAt(i), state);
            }
        }
    }

    private static void restore(View view, State state) {
        if (view.getBackground() instanceof FrostDrawable) {
            view.setBackground(null);
        }
        for (Map.Entry<View, Drawable> entry : state.backgrounds.entrySet()) {
            View key = entry.getKey();
            if (key != null) {
                key.setBackground(entry.getValue());
            }
        }
        state.backgrounds.clear();
        state.applied = false;
    }

    private static View find(View view, String str) {
        int iId;
        if (view == null || (iId = BlurBars.id(view.getContext(), str)) == 0) {
            return null;
        }
        return view.findViewById(iId);
    }

    private static final class State {
        boolean applied;
        final Map<View, Drawable> backgrounds;

        private State() {
            this.backgrounds = new WeakHashMap();
        }
    }

    private static final class FrostDrawable extends Drawable {
        private Object gpu;
        private final View host;
        private int lastHeight;
        View[] sources;
        private final Paint fill = new Paint(1);
        private final int[] hostLocation = new int[2];
        private final int[] viewLocation = new int[2];
        private int lastKey = -1;

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

        FrostDrawable(View view) {
            this.host = view;
        }

        @Override // android.graphics.drawable.Drawable
        public void draw(Canvas canvas) {
            if (ProgressiveBlurDrawable.recording) {
                return;
            }
            Rect bounds = getBounds();
            if (bounds.isEmpty()) {
                return;
            }
            boolean zPowerSave = BlurBars.powerSave();
            boolean zChromeNight = BlurBars.chromeNight(this.host);
            if (!zPowerSave && Build.VERSION.SDK_INT >= 31 && canvas.isHardwareAccelerated()) {
                if (this.gpu == null) {
                    this.gpu = new Gpu();
                }
                ((Gpu) this.gpu).draw(this, canvas, bounds);
            }
            if (zPowerSave) {
                int i = zChromeNight ? ChatPanels.NIGHT_SAVER_FILL : ChatPanels.DAY_SAVER_FILL;
                int iDp = BlurBars.dp(this.host.getContext(), 20.0f);
                int i2 = (iDp << 4) | i;
                if (this.lastKey != i2 || this.lastHeight != bounds.height()) {
                    this.lastKey = i2;
                    this.lastHeight = bounds.height();
                    if (iDp >= bounds.height() || iDp <= 0) {
                        this.fill.setShader(null);
                        this.fill.setColor(i);
                    } else {
                        this.fill.setColor(-16777216);
                        bounds.height();
                        this.fill.setShader(new LinearGradient(0.0f, bounds.top, 0.0f, bounds.top + iDp, new int[]{16777215 & i, i}, new float[]{0.0f, 1.0f}, Shader.TileMode.CLAMP));
                    }
                }
            } else {
                int i3 = zChromeNight ? ChatPanels.NIGHT_PERF_FILL : ChatPanels.DAY_PERF_FILL;
                this.fill.setShader(null);
                this.fill.setColor(i3);
            }
            canvas.drawRect(bounds, this.fill);
        }
    }

    private static final class Gpu {
        private RenderEffect effect;
        private int effectHeight;
        private int effectWidth;
        private final RenderNode node;

        private Gpu() {
            this.node = new RenderNode("andrewChatPanel");
        }

        void draw(FrostDrawable frostDrawable, Canvas canvas, Rect rect) {
            int iWidth = rect.width();
            int iHeight = rect.height();
            if (iWidth <= 0 || iHeight <= 0 || frostDrawable.sources == null) {
                return;
            }
            this.node.setPosition(0, 0, iWidth, iHeight);
            RecordingCanvas recordingCanvasBeginRecording = this.node.beginRecording(iWidth, iHeight);
            ProgressiveBlurDrawable.recording = true;
            try {
                frostDrawable.host.getLocationInWindow(frostDrawable.hostLocation);
                int i = frostDrawable.hostLocation[0] + rect.left;
                int i2 = frostDrawable.hostLocation[1] + rect.top;
                for (View view : frostDrawable.sources) {
                    if (!BlurBars.isSelfOrAncestor(view, frostDrawable.host)) {
                        record(recordingCanvasBeginRecording, view, i, i2, frostDrawable.viewLocation);
                    }
                }
            } finally {
                ProgressiveBlurDrawable.recording = false;
                this.node.endRecording();
            }
            if (this.effect == null || iWidth != this.effectWidth || iHeight != this.effectHeight) {
                float fDp = BlurBars.dp(frostDrawable.host.getContext(), 20.0f);
                this.effect = RenderEffect.createBlurEffect(fDp, fDp, Shader.TileMode.CLAMP);
                this.effectWidth = iWidth;
                this.effectHeight = iHeight;
            }
            this.node.setRenderEffect(this.effect);
            int iSave = canvas.save();
            canvas.translate(rect.left, rect.top);
            canvas.drawRenderNode(this.node);
            canvas.restoreToCount(iSave);
        }

        private void record(Canvas canvas, View view, int i, int i2, int[] iArr) {
            if (view != null && view.getVisibility() == 0 && view.isAttachedToWindow()) {
                view.getLocationInWindow(iArr);
                int iSave = canvas.save();
                canvas.translate(iArr[0] - i, iArr[1] - i2);
                try {
                    view.draw(canvas);
                } catch (IllegalStateException unused) {
                } finally {
                    canvas.restoreToCount(iSave);
                }
            }
        }
    }
}

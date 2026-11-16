package app.andrewliang.extension;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RecordingCanvas;
import android.graphics.Rect;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Capsule chrome for the chat-room header: a circular back pill, a stadium around the avatar and
 * the name, and one long pill behind the action buttons. The pills have a semi-transparent dark
 * fill, a thin light border, and a native blur of the content behind them. A light gradient under
 * the pills fades the messages out; there is no full dark header bar.
 *
 * <p>Applied only while {@link LineSettings#chatHeaderCapsule()} is on, and only on the chat
 * history header that {@link BlurBars} already tracks. The style is checked each frame, so it
 * switches live from Andrew's Patch Setting.
 */
final class ChatHeaderPills {

    private static final float PILL_H = 40f;
    private static final float PILL_PAD = 10f;
    private static final float RIGHT_PAD = 6f;
    private static final float BLUR_RADIUS_DP = 18f;
    private static final int FILL = 0x8C000000;
    private static final int STROKE = 0x40FFFFFF;
    private static final int ICON = Color.WHITE;

    private static final String[] ACTION_BUTTONS = {
            "far_left_header_button",
            "left_header_button",
            "middle_header_button",
            "inner_right_header_button",
            "right_header_button",
            "right_header_image_button",
    };

    private static final ViewOutlineProvider CIRCLE = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            int w = view.getWidth();
            int h = view.getHeight();
            int pill = Math.min(Math.min(w, h), BlurBars.dp(view.getContext(), PILL_H));
            if (pill <= 0) {
                outline.setEmpty();
                return;
            }
            int l = (w - pill) / 2;
            int t = (h - pill) / 2;
            outline.setRoundRect(l, t, l + pill, t + pill, pill / 2f);
        }
    };

    private static final ViewOutlineProvider STADIUM = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            int w = view.getWidth();
            int h = view.getHeight();
            if (w <= 0 || h <= 0) {
                outline.setEmpty();
                return;
            }
            int pillH = Math.min(h, BlurBars.dp(view.getContext(), PILL_H));
            int t = (h - pillH) / 2;
            outline.setRoundRect(0, t, w, t + pillH, pillH / 2f);
        }
    };

    private static final ViewOutlineProvider OVERLAY = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            int w = view.getWidth();
            int h = view.getHeight();
            if (w <= 0 || h <= 0) {
                outline.setEmpty();
                return;
            }
            outline.setRoundRect(0, 0, w, h, h / 2f);
        }
    };

    private static final Map<View, State> STATES = new WeakHashMap<>();

    private ChatHeaderPills() {}

    static boolean enabled() {
        return LineSettings.blurSystemBars() && LineSettings.chatHeaderCapsule();
    }

    static Drawable newScrim() {
        return new LightScrim();
    }

    static void apply(View header, View[] sources) {
        if (header == null) return;
        State state = STATES.get(header);
        if (!enabled()) {
            if (state != null && state.applied) restore(header, state);
            return;
        }
        if (state == null) {
            state = new State();
            STATES.put(header, state);
        }
        style(header, state, sources);
        tint(header, state);
        invalidatePills(state);
    }

    private static void style(View header, State state, View[] sources) {
        if (!state.styled) {
            state.back = find(header, "header_up_button_bg");
            state.titleBox = titleInner(header);
            if (state.back != null) {
                copyPadding(state.back, state.backPad);
                int pad = BlurBars.dp(header.getContext(), PILL_PAD);
                int pill = BlurBars.dp(header.getContext(), PILL_H);
                state.back.setPadding(state.backPad[0] + pad, state.backPad[1],
                        state.backPad[2] + pad, state.backPad[3]);
                state.back.setMinimumWidth(pill);
                state.back.setMinimumHeight(pill);
            }
            if (state.titleBox != null) {
                copyPadding(state.titleBox, state.titlePad);
                int pad = BlurBars.dp(header.getContext(), PILL_PAD);
                state.titleBox.setPadding(state.titlePad[0] + pad, state.titlePad[1],
                        state.titlePad[2] + pad, state.titlePad[3]);
            }
            state.styled = true;
        }
        installPill(state.back, CIRCLE, sources, false);
        installPill(state.titleBox, STADIUM, sources, false);
        positionRightPill(header, state, sources);
        state.applied = true;
    }

    private static void installPill(View view, ViewOutlineProvider outline, View[] sources, boolean overlay) {
        if (view == null) return;
        view.setOutlineProvider(outline);
        if (!view.getClipToOutline()) view.setClipToOutline(true);
        Drawable background = view.getBackground();
        if (background instanceof CapsuleDrawable) {
            ((CapsuleDrawable) background).sources = sources;
            ((CapsuleDrawable) background).overlay = overlay;
        } else {
            view.setBackground(new CapsuleDrawable(view, sources, overlay));
        }
    }

    private static void positionRightPill(View header, State state, View[] sources) {
        ViewGroup root = rootOf(header);
        if (root == null) return;
        Rect union = actionUnion(header, root);
        if (union.isEmpty()) {
            if (state.rightPill != null) state.rightPill.setVisibility(View.GONE);
            return;
        }
        if (state.rightPill == null) {
            View pill = new View(header.getContext());
            int index = 0;
            View first = find(root, ACTION_BUTTONS[0]);
            if (first != null) {
                int at = root.indexOfChild(first);
                if (at >= 0) index = at;
            }
            root.addView(pill, index, new ViewGroup.LayoutParams(1, 1));
            state.rightPill = pill;
        }
        View pill = state.rightPill;
        if (pill.getParent() != root) return;
        pill.setVisibility(View.VISIBLE);
        ViewGroup.LayoutParams params = pill.getLayoutParams();
        if (params != null) {
            params.width = Math.max(1, union.width());
            params.height = Math.max(1, union.height());
            pill.setLayoutParams(params);
        }
        pill.setTranslationX(union.left);
        pill.setTranslationY(union.top);
        installPill(pill, OVERLAY, sources, true);
    }

    /** The visible action buttons, inset to a centered capsule, in [root] coordinates. */
    private static Rect actionUnion(View header, ViewGroup root) {
        Rect union = new Rect();
        int[] rootLoc = new int[2];
        int[] viewLoc = new int[2];
        root.getLocationInWindow(rootLoc);
        for (String name : ACTION_BUTTONS) {
            View button = find(header, name);
            if (!visible(button)) continue;
            button.getLocationInWindow(viewLoc);
            int left = viewLoc[0] - rootLoc[0];
            int top = viewLoc[1] - rootLoc[1];
            if (union.isEmpty()) {
                union.set(left, top, left + button.getWidth(), top + button.getHeight());
            } else {
                union.union(left, top, left + button.getWidth(), top + button.getHeight());
            }
        }
        if (union.isEmpty()) return union;
        int pillH = BlurBars.dp(header.getContext(), PILL_H);
        int inset = Math.max(0, (union.height() - pillH) / 2);
        union.top += inset;
        union.bottom = union.top + Math.min(pillH, union.height());
        int pad = BlurBars.dp(header.getContext(), RIGHT_PAD);
        union.left -= pad;
        union.right += pad;
        if (union.left < 0) union.left = 0;
        if (root.getWidth() > 0 && union.right > root.getWidth()) union.right = root.getWidth();
        if (union.right <= union.left) union.setEmpty();
        return union;
    }

    private static void tint(View header, State state) {
        ImageView up = findImage(header, "header_up_button");
        if (up != null) {
            if (state.upTint == null && !state.tinted) state.upTint = up.getImageTintList();
            up.setImageTintList(ColorStateList.valueOf(ICON));
            up.setColorFilter(ICON);
        }
        TextView title = findText(header, "header_title");
        if (title != null) {
            if (state.titleColors == null && !state.tinted) state.titleColors = title.getTextColors();
            title.setTextColor(ICON);
        }
        TextView count = findText(header, "header_title_count");
        if (count != null) {
            if (state.countColors == null && !state.tinted) state.countColors = count.getTextColors();
            count.setTextColor(ICON);
        }
        for (String name : ACTION_BUTTONS) {
            View button = find(header, name);
            if (button instanceof ViewGroup) tintImages((ViewGroup) button);
            else if (button instanceof ImageView) tintImage((ImageView) button);
        }
        state.tinted = true;
    }

    private static void tintImages(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof ImageView) tintImage((ImageView) child);
            else if (child instanceof ViewGroup) tintImages((ViewGroup) child);
        }
    }

    private static void tintImage(ImageView image) {
        image.setImageTintList(ColorStateList.valueOf(ICON));
        image.setColorFilter(ICON);
    }

    private static void restore(View header, State state) {
        if (state.back != null) {
            state.back.setPadding(state.backPad[0], state.backPad[1], state.backPad[2], state.backPad[3]);
            state.back.setMinimumWidth(0);
            state.back.setMinimumHeight(0);
            clearPill(state.back);
        }
        if (state.titleBox != null) {
            state.titleBox.setPadding(state.titlePad[0], state.titlePad[1],
                    state.titlePad[2], state.titlePad[3]);
            clearPill(state.titleBox);
        }
        if (state.rightPill != null) {
            ViewParent parent = state.rightPill.getParent();
            if (parent instanceof ViewGroup) ((ViewGroup) parent).removeView(state.rightPill);
            state.rightPill = null;
        }
        ImageView up = findImage(header, "header_up_button");
        if (up != null) {
            up.setColorFilter(null);
            up.setImageTintList(state.upTint);
        }
        TextView title = findText(header, "header_title");
        if (title != null && state.titleColors != null) title.setTextColor(state.titleColors);
        TextView count = findText(header, "header_title_count");
        if (count != null && state.countColors != null) count.setTextColor(state.countColors);
        for (String name : ACTION_BUTTONS) {
            View button = find(header, name);
            if (button instanceof ViewGroup) clearImages((ViewGroup) button);
            else if (button instanceof ImageView) clearImage((ImageView) button);
        }
        state.applied = false;
        state.styled = false;
        state.tinted = false;
        state.upTint = null;
        state.titleColors = null;
        state.countColors = null;
    }

    private static void clearPill(View view) {
        view.setClipToOutline(false);
        view.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        if (view.getBackground() instanceof CapsuleDrawable) view.setBackground(null);
    }

    private static void clearImages(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof ImageView) clearImage((ImageView) child);
            else if (child instanceof ViewGroup) clearImages((ViewGroup) child);
        }
    }

    private static void clearImage(ImageView image) {
        image.setColorFilter(null);
        image.setImageTintList(null);
    }

    private static void invalidatePills(State state) {
        if (state.back != null) state.back.invalidate();
        if (state.titleBox != null) state.titleBox.invalidate();
        if (state.rightPill != null) state.rightPill.invalidate();
    }

    private static View titleInner(View header) {
        View main = find(header, "main_view_group");
        if (main instanceof ViewGroup && ((ViewGroup) main).getChildCount() > 0) {
            return ((ViewGroup) main).getChildAt(0);
        }
        return main;
    }

    private static ViewGroup rootOf(View header) {
        View found = find(header, "header_root");
        if (found instanceof ViewGroup) return (ViewGroup) found;
        if (header instanceof ViewGroup && ((ViewGroup) header).getChildCount() > 0) {
            View child = ((ViewGroup) header).getChildAt(0);
            if (child instanceof ViewGroup) return (ViewGroup) child;
        }
        return header instanceof ViewGroup ? (ViewGroup) header : null;
    }

    private static boolean visible(View view) {
        return view != null && view.isShown() && view.getWidth() > 0 && view.getHeight() > 0;
    }

    private static View find(View root, String name) {
        int id = BlurBars.id(root.getContext(), name);
        return id == 0 ? null : root.findViewById(id);
    }

    private static ImageView findImage(View root, String name) {
        View view = find(root, name);
        return view instanceof ImageView ? (ImageView) view : null;
    }

    private static TextView findText(View root, String name) {
        View view = find(root, name);
        return view instanceof TextView ? (TextView) view : null;
    }

    private static void copyPadding(View view, int[] into) {
        into[0] = view.getPaddingLeft();
        into[1] = view.getPaddingTop();
        into[2] = view.getPaddingRight();
        into[3] = view.getPaddingBottom();
    }

    private static final class State {
        View back;
        View titleBox;
        View rightPill;
        final int[] backPad = new int[4];
        final int[] titlePad = new int[4];
        ColorStateList upTint;
        ColorStateList titleColors;
        ColorStateList countColors;
        boolean styled;
        boolean applied;
        boolean tinted;
    }

    /** A light fade from the status bar down to the bottom of the pills, not a solid header bar. */
    static final class LightScrim extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int lastHeight = -1;
        private int lastTop;
        private int lastBottom;

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            if (bounds.isEmpty()) return;
            if (lastHeight != bounds.height() || lastTop != bounds.top || lastBottom != bounds.bottom) {
                lastHeight = bounds.height();
                lastTop = bounds.top;
                lastBottom = bounds.bottom;
                paint.setShader(new LinearGradient(0, bounds.top, 0, bounds.bottom,
                        new int[] {0x73000000, 0x3D000000, 0x00000000},
                        new float[] {0f, 0.62f, 1f}, Shader.TileMode.CLAMP));
            }
            canvas.drawRect(bounds, paint);
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

    /**
     * The fill, border and optional blur of one pill. The blur records the same skin and content
     * views as the header region, not the header itself, so it does not recurse.
     */
    private static final class CapsuleDrawable extends Drawable {
        private final View host;
        View[] sources;
        boolean overlay;
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Rect capsule = new Rect();
        private final int[] hostLocation = new int[2];
        private Object gpu;

        CapsuleDrawable(View host, View[] sources, boolean overlay) {
            this.host = host;
            this.sources = sources;
            this.overlay = overlay;
            fill.setColor(FILL);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setColor(STROKE);
        }

        @Override
        public void draw(Canvas canvas) {
            if (ProgressiveBlurDrawable.recording) return;
            Rect bounds = getBounds();
            if (bounds.isEmpty()) return;
            capsuleOf(bounds, capsule);
            if (capsule.isEmpty()) return;
            float radius = capsule.height() / 2f;
            if (!BlurBars.powerSave() && Build.VERSION.SDK_INT >= 31 && canvas.isHardwareAccelerated()) {
                if (gpu == null) gpu = new Gpu();
                ((Gpu) gpu).draw(this, canvas, capsule);
            }
            canvas.drawRoundRect(capsule.left, capsule.top, capsule.right, capsule.bottom,
                    radius, radius, fill);
            float width = Math.max(1f, host.getResources().getDisplayMetrics().density);
            stroke.setStrokeWidth(width);
            float inset = width / 2f;
            canvas.drawRoundRect(capsule.left + inset, capsule.top + inset,
                    capsule.right - inset, capsule.bottom - inset, radius, radius, stroke);
        }

        private void capsuleOf(Rect bounds, Rect into) {
            if (overlay) {
                into.set(bounds);
                return;
            }
            int pillH = Math.min(bounds.height(), BlurBars.dp(host.getContext(), PILL_H));
            int top = bounds.top + (bounds.height() - pillH) / 2;
            if (bounds.width() <= bounds.height()) {
                int size = Math.min(bounds.width(), pillH);
                int left = bounds.left + (bounds.width() - size) / 2;
                into.set(left, top + (pillH - size) / 2, left + size, top + (pillH - size) / 2 + size);
            } else {
                into.set(bounds.left, top, bounds.right, top + pillH);
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

    /** Uniform backdrop blur of the content behind a pill. Loaded only on API 31 and later. */
    private static final class Gpu {
        private final RenderNode node = new RenderNode("andrewChatPills");
        private final int[] viewLocation = new int[2];
        private RenderEffect effect;
        private int effectWidth;
        private int effectHeight;

        void draw(CapsuleDrawable drawable, Canvas canvas, Rect capsule) {
            int width = capsule.width();
            int height = capsule.height();
            if (width <= 0 || height <= 0 || drawable.sources == null) return;
            node.setPosition(0, 0, width, height);
            RecordingCanvas recorded = node.beginRecording(width, height);
            ProgressiveBlurDrawable.recording = true;
            try {
                drawable.host.getLocationInWindow(drawable.hostLocation);
                int left = drawable.hostLocation[0] + capsule.left;
                int top = drawable.hostLocation[1] + capsule.top;
                for (View source : drawable.sources) record(recorded, source, left, top);
            } finally {
                ProgressiveBlurDrawable.recording = false;
                node.endRecording();
            }
            if (effect == null || width != effectWidth || height != effectHeight) {
                float radius = BlurBars.dp(drawable.host.getContext(), BLUR_RADIUS_DP);
                effect = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP);
                effectWidth = width;
                effectHeight = height;
            }
            node.setRenderEffect(effect);
            int save = canvas.save();
            canvas.translate(capsule.left, capsule.top);
            canvas.drawRenderNode(node);
            canvas.restoreToCount(save);
        }

        private void record(Canvas canvas, View view, int left, int top) {
            if (view == null || view.getVisibility() != View.VISIBLE || !view.isAttachedToWindow()) {
                return;
            }
            view.getLocationInWindow(viewLocation);
            int save = canvas.save();
            canvas.translate(viewLocation[0] - left, viewLocation[1] - top);
            view.draw(canvas);
            canvas.restoreToCount(save);
        }
    }
}

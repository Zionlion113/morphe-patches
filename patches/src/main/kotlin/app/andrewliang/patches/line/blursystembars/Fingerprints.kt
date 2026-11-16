package app.andrewliang.patches.line.blursystembars

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

/**
 * LINE's edge-to-edge helper (`ea2/g.f` on 26.16.0). Each screen passes its window and an
 * `EdgeToEdgeOverlapType`. The helper reads the theme colors for the bars, sets them on the
 * window, then sets the display cutout mode. No other method makes these four calls.
 */
internal object EdgeToEdgeWindowFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        methodCall(definingClass = "Landroid/view/Window;", name = "setNavigationBarColor"),
        methodCall(definingClass = "Landroid/view/Window;", name = "setNavigationBarDividerColor"),
        methodCall(definingClass = "Landroid/view/Window;", name = "setStatusBarColor"),
        fieldAccess(
            definingClass = "Landroid/view/WindowManager\$LayoutParams;",
            name = "layoutInDisplayCutoutMode",
            opcode = Opcode.IPUT,
        ),
    ),
)

/**
 * The Compose top bar of LINE's design system for themes without a seamless header
 * (`y82/u.b` on 26.16.0). It is a column of a status bar strip in the theme's status bar color and
 * the bar, which paints the theme artwork stretched to its bounds. The first parameter is the
 * default color, and the column modifier comes from `Modifier.then`.
 */
internal object ComposeTopBarFingerprint : Fingerprint(
    returnType = "V",
    custom = { method, _ ->
        method.parameterTypes.size == 7 && method.parameterTypes[0] == "J" &&
            method.parameterTypes[6] == "I"
    },
    filters = listOf(
        methodCall(definingClass = "Lcom/linecorp/line/compose/theme/i;", name = "b"),
        methodCall(definingClass = "Lu1/n;", name = "b"),
        methodCall(definingClass = "Lcom/linecorp/line/compose/theme/i;", name = "e"),
        methodCall(definingClass = "Landroidx/compose/ui/draw/a;", name = "a"),
        methodCall(definingClass = "Lcom/linecorp/line/compose/theme/a;", name = "c"),
    ),
)

private const val CHAT_SKIN_CLASS =
    "Lcom/linecorp/line/chat/ui/resources/message/message/list/skin/ChatHistorySkinImageView;"

/**
 * {@code ChatHistorySkinImageView.onDraw}. LINE sets the background's bounds to a shorter area
 * that leaves out the input bar and the gesture bar, then calls {@code super.onDraw}.
 */
internal object ChatHistorySkinOnDrawFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/graphics/Canvas;"),
    custom = { method, classDef -> classDef.type == CHAT_SKIN_CLASS && method.name == "onDraw" },
)

/**
 * {@code ChatHistorySkinImageView.getContentSkinImageArea}. LINE uses this size when it captures
 * the background as a bitmap.
 */
internal object ChatHistorySkinAreaFingerprint : Fingerprint(
    returnType = "Landroid/util/Size;",
    parameters = listOf(),
    custom = { method, classDef ->
        classDef.type == CHAT_SKIN_CLASS && method.name == "getContentSkinImageArea"
    },
)

/**
 * The method that sets the skin view's top margin to the status bar height for official themes
 * that have a header and a footer ({@code L()} on 26.14.0–26.16.0).
 */
internal object ChatHistorySkinLayoutFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(),
    custom = { method, classDef ->
        classDef.type == CHAT_SKIN_CLASS && method.implementation?.instructions?.any { instruction ->
            instruction.opcode == Opcode.IPUT &&
                (instruction as? ReferenceInstruction)?.reference is FieldReference &&
                (instruction.reference as FieldReference).name == "topMargin"
        } == true
    },
)


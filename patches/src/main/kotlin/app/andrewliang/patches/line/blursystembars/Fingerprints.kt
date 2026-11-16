package app.andrewliang.patches.line.blursystembars

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

/**
 * `LineApplication.onCreate()`. LINE does not obfuscate the class, and the method passes its own
 * name as a trace label.
 */
internal object LineApplicationOnCreateFingerprint : Fingerprint(
    definingClass = "Ljp/naver/line/android/LineApplication;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(string("LineApplication.onCreate")),
)

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

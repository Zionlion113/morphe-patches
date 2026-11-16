package app.andrewliang.patches.line.blursystembars

import app.andrewliang.patches.line.shared.lineSettingsExtensionPatch
import app.andrewliang.patches.line.shared.markLineSettingIncluded
import app.andrewliang.patches.shared.Constants.COMPATIBILITY_LINE
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION = "Lapp/andrewliang/extension/BlurBars;"

@Suppress("unused")
val blurSystemBarsPatch = bytecodePatch(
    name = "[General] Transparent system bars with blur",
    description = "Removes the theme color blocks behind the status bar and the gesture bar on every " +
        "screen. The bottom tabs keep their theme. In chats, the header stays on top and messages " +
        "blur out behind it.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_LINE)

    dependsOn(lineSettingsExtensionPatch)

    execute {
        // The extension finds the bar views of each screen by resource name, from activity
        // lifecycle callbacks. It registers them first thing in onCreate. p0 is the application.
        LineApplicationOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static { p0 }, $EXTENSION->init(Landroid/app/Application;)V",
        )

        // The edge-to-edge helper gives the window the theme colors of the bars. Older Android
        // versions draw them, so each call goes through the extension, which passes a clear color
        // while the switch is on. Both calls are `invoke-virtual { window, color }`, and the
        // static call takes the same two registers, so no branch moves.
        EdgeToEdgeWindowFingerprint.method.apply {
            val instructions = implementation!!.instructions.toList()
            var replaced = 0
            instructions.forEachIndexed { index, instruction ->
                if (instruction.opcode != Opcode.INVOKE_VIRTUAL) return@forEachIndexed
                val reference = (instruction as ReferenceInstruction).reference as MethodReference
                if (reference.definingClass != "Landroid/view/Window;") return@forEachIndexed
                val name = when (reference.name) {
                    "setStatusBarColor", "setNavigationBarColor" -> reference.name
                    else -> return@forEachIndexed
                }
                val call = instruction as FiveRegisterInstruction
                replaceInstruction(
                    index,
                    "invoke-static { v${call.registerC}, v${call.registerD} }, " +
                        "$EXTENSION->$name(Landroid/view/Window;I)V",
                )
                replaced++
            }
            if (replaced != 2) throw PatchException("Expected 2 bar color calls in the edge-to-edge helper, found $replaced")
        }

        markLineSettingIncluded("blurSystemBars")
    }
}

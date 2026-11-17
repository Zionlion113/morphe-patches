package app.andrewliang.patches.line.smoothkeyboard

import app.andrewliang.patches.line.shared.LineApplicationOnCreateFingerprint
import app.andrewliang.patches.line.shared.lineSettingsExtensionPatch
import app.andrewliang.patches.line.shared.markLineSettingIncluded
import app.andrewliang.patches.shared.Constants.COMPATIBILITY_LINE
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch

private const val EXTENSION = "Lapp/andrewliang/extension/KeyboardMotion;"

@Suppress("unused")
val smoothKeyboardPatch = bytecodePatch(
    name = "[Chat] Smooth keyboard animation",
    description = "Lets the blurred chat input bar follow the keyboard frame by frame, with less work on each " +
        "frame. LINE already moves the input bar with the keyboard; this keeps the blur of " +
        "\"Transparent system bars with blur\" in step with it. Android 11 and later.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_LINE)

    dependsOn(lineSettingsExtensionPatch)

    execute {
        LineApplicationOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static { p0 }, $EXTENSION->init(Landroid/app/Application;)V",
        )
        markLineSettingIncluded("smoothKeyboard")
    }
}

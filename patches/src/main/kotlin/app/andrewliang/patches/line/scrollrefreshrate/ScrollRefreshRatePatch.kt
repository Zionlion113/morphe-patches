package app.andrewliang.patches.line.scrollrefreshrate

import app.andrewliang.patches.line.shared.LineApplicationOnCreateFingerprint
import app.andrewliang.patches.line.shared.lineSettingsExtensionPatch
import app.andrewliang.patches.line.shared.markLineSettingIncluded
import app.andrewliang.patches.shared.Constants.COMPATIBILITY_LINE
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch

private const val EXTENSION = "Lapp/andrewliang/extension/ScrollRefreshRate;"

@Suppress("unused")
val scrollRefreshRatePatch = bytecodePatch(
    name = "[Chat] High refresh rate while scrolling",
    description = "Uses the highest screen refresh rate only while chat messages scroll, and lets the system " +
        "choose again when they stop. Uses Adaptive Refresh Rate on Android 15 and later. Never in Battery Saver.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_LINE)

    dependsOn(lineSettingsExtensionPatch)

    execute {
        LineApplicationOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static { p0 }, $EXTENSION->init(Landroid/app/Application;)V",
        )
        markLineSettingIncluded("scrollRefreshRate")
    }
}

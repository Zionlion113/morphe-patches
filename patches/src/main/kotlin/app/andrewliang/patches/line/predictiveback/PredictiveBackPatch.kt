package app.andrewliang.patches.line.predictiveback

import app.andrewliang.patches.line.shared.LineApplicationOnCreateFingerprint
import app.andrewliang.patches.line.shared.lineSettingsExtensionPatch
import app.andrewliang.patches.line.shared.markLineSettingIncluded
import app.andrewliang.patches.shared.Constants.COMPATIBILITY_LINE
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element

private const val EXTENSION = "Lapp/andrewliang/extension/PredictiveBack;"
private const val MARK = "app.andrewliang.predictive_back"
private const val FLAG = "android:enableOnBackInvokedCallback"

private val predictiveBackManifestPatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            val activities = document.getElementsByTagName("activity")
            for (i in 0 until activities.length) {
                val activity = activities.item(i) as? Element ?: continue
                if (!activity.hasAttribute(FLAG)) {
                    activity.setAttribute(FLAG, "true")
                    val meta = document.createElement("meta-data").apply {
                        setAttribute("android:name", MARK)
                        setAttribute("android:value", "true")
                    }
                    activity.appendChild(meta)
                }
            }
        }
    }
}

@Suppress("unused")
val predictiveBackPatch = bytecodePatch(
    name = "[General] Predictive back",
    description = "Shows Android's predictive back animation when you swipe back, on Android 14 and later. " +
        "Screens with their own back handling keep the old behavior.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_LINE)

    dependsOn(lineSettingsExtensionPatch, predictiveBackManifestPatch)

    execute {
        LineApplicationOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static { p0 }, $EXTENSION->init(Landroid/app/Application;)V",
        )
        markLineSettingIncluded("predictiveBack")
    }
}

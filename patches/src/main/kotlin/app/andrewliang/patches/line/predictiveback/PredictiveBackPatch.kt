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

/** The same name as `PredictiveBack.MARK` in the extension. */
private const val MARK = "app.andrewliang.predictive_back"

private const val FLAG = "android:enableOnBackInvokedCallback"

/**
 * Manifest half. Turns on the back flag for each activity that LINE has not set it on, and marks
 * each such activity with a meta-data entry, so the extension knows which ones it may need to
 * bridge. Activities where LINE set the flag itself, to true or to false, are left as they are.
 */
private val predictiveBackManifestPatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            val activities = document.getElementsByTagName("activity")
            for (i in 0 until activities.length) {
                val activity = activities.item(i) as? Element ?: continue
                if (activity.hasAttribute(FLAG)) continue
                activity.setAttribute(FLAG, "true")
                activity.appendChild(
                    document.createElement("meta-data").apply {
                        setAttribute("android:name", MARK)
                        setAttribute("android:value", "true")
                    },
                )
            }
        }
    }
}

@Suppress("unused")
val predictiveBackPatch = bytecodePatch(
    name = "[General] Predictive back",
    description = "Shows Android's predictive back animation when you swipe back, on Android 14 and " +
        "later. Screens with their own back handling keep the old behavior.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_LINE)

    dependsOn(lineSettingsExtensionPatch, predictiveBackManifestPatch)

    execute {
        // The extension adds a bridge for the activities that still handle back the old way.
        // It must see each activity as it is created, so it registers first thing. p0 is the
        // application.
        LineApplicationOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static { p0 }, $EXTENSION->init(Landroid/app/Application;)V",
        )

        markLineSettingIncluded("predictiveBack")
    }
}

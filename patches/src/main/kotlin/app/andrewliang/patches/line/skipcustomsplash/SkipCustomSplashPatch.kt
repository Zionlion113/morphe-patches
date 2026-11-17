package app.andrewliang.patches.line.skipcustomsplash

import app.andrewliang.patches.line.shared.lineSettingsExtensionPatch
import app.andrewliang.patches.line.shared.markLineSettingIncluded
import app.andrewliang.patches.line.shared.readLineSetting
import app.andrewliang.patches.shared.Constants.COMPATIBILITY_LINE
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.Opcode

internal object CustomSplashCheckFingerprint : Fingerprint(
    definingClass = "Ljp/naver/line/android/customsplash/CustomSplashViewModel;",
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        fieldAccess(
            definingClass = "Ljava/lang/Boolean;",
            name = "FALSE",
            opcode = Opcode.SGET_OBJECT,
        ),
    ),
    custom = { method, _ -> method.parameterTypes.size == 1 },
)

@Suppress("unused")
val skipCustomSplashPatch = bytecodePatch(
    name = "[General] Skip custom splash",
    description = "Skips LINE's extra promotional splash screen after Android's launch screen.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_LINE)

    dependsOn(lineSettingsExtensionPatch)

    execute {
        val method = CustomSplashCheckFingerprint.method
        method.addInstructionsWithLabels(
            0,
            readLineSetting("skipCustomSplash", "v0") +
                """
                    if-eqz v0, :stock
                    sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                    return-object v0
                """,
            ExternalLabel("stock", method.getInstruction(0)),
        )
        markLineSettingIncluded("skipCustomSplash")
    }
}

package app.andrewliang.patches.line.chathaptics

import app.andrewliang.patches.line.shared.LineApplicationOnCreateFingerprint
import app.andrewliang.patches.line.shared.lineSettingsExtensionPatch
import app.andrewliang.patches.line.shared.markLineSettingIncluded
import app.andrewliang.patches.shared.Constants.COMPATIBILITY_LINE
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val EXTENSION = "Lapp/andrewliang/extension/Haptics;"

/** LINE does not obfuscate this dialog. */
private const val UNSEND_DIALOG = "Lcom/linecorp/line/chat/ui/impl/message/contextmenu/UnsendMessageLdsDialog;"

/** The first constructor argument of the two unsend listeners. The cancel listener has another. */
private const val UNSEND_ARGUMENTS = "Lcom/linecorp/line/chat/ui/impl/message/contextmenu/UnsendMessageLdsDialog\$a;"

@Suppress("unused")
val chatHapticsPatch = bytecodePatch(
    name = "[Chat] Haptic feedback",
    description = "Adds a short vibration when you long-press a message, send a message, or unsend one. " +
        "It follows the system touch feedback setting.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_LINE)

    dependsOn(lineSettingsExtensionPatch)

    execute {
        // Long press and send are found at runtime, by view id. p0 is the application.
        LineApplicationOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static { p0 }, $EXTENSION->init(Landroid/app/Application;)V",
        )

        // Unsend: the unsend dialog sets three click listeners in onViewCreated: "Unsend
        // silently", "Unsend" and "Cancel". The two unsend listeners take the dialog arguments
        // first. Their `setOnClickListener { button, listener }` calls become a static call with
        // the same two registers, which sets a listener that buzzes first. No branch moves.
        val onViewCreated = mutableClassDefBy(UNSEND_DIALOG).methods.single { it.name == "onViewCreated" }
        onViewCreated.apply {
            val instructions = implementation!!.instructions.toList()
            // The listener class built into each register, from `new-instance`.
            val built = HashMap<Int, String>()
            var replaced = 0
            instructions.forEachIndexed { index, instruction ->
                if (instruction.opcode == Opcode.NEW_INSTANCE) {
                    val register = (instruction as OneRegisterInstruction).registerA
                    built[register] = ((instruction as ReferenceInstruction).reference as TypeReference).type
                    return@forEachIndexed
                }
                if (instruction.opcode != Opcode.INVOKE_VIRTUAL) return@forEachIndexed
                val reference = (instruction as ReferenceInstruction).reference as MethodReference
                if (reference.name != "setOnClickListener" || reference.definingClass != "Landroid/view/View;") {
                    return@forEachIndexed
                }
                val call = instruction as FiveRegisterInstruction
                val listener = built[call.registerD] ?: return@forEachIndexed
                val constructor = classDefBy(listener).methods.firstOrNull { it.name == "<init>" }
                    ?: return@forEachIndexed
                if (constructor.parameterTypes.firstOrNull()?.toString() != UNSEND_ARGUMENTS) return@forEachIndexed
                replaceInstruction(
                    index,
                    "invoke-static { v${call.registerC}, v${call.registerD} }, $EXTENSION->" +
                        "setUnsendClickListener(Landroid/view/View;Landroid/view/View\$OnClickListener;)V",
                )
                replaced++
            }
            if (replaced != 2) throw PatchException("Expected 2 unsend buttons, found $replaced")
        }

        markLineSettingIncluded("chatHaptics")
    }
}

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
private const val UNSEND_DIALOG = "Lcom/linecorp/line/chat/ui/impl/message/contextmenu/UnsendMessageLdsDialog;"
private const val UNSEND_ARGUMENTS = "Lcom/linecorp/line/chat/ui/impl/message/contextmenu/UnsendMessageLdsDialog\$a;"

@Suppress("unused")
val chatHapticsPatch = bytecodePatch(
    name = "[Chat] Haptic feedback",
    description = "Adds tactile feedback when you long-press a message, swipe to reply, send, unsend, " +
        "flip a switch, or switch bottom tabs. It follows the system touch feedback setting.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_LINE)

    dependsOn(lineSettingsExtensionPatch)

    execute {
        LineApplicationOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static { p0 }, $EXTENSION->init(Landroid/app/Application;)V",
        )

        val onViewCreated = mutableClassDefBy(UNSEND_DIALOG).methods.single { it.name == "onViewCreated" }
        val instructions = onViewCreated.implementation!!.instructions.toList()
        val built = HashMap<Int, String>()
        var replaced = 0

        instructions.forEachIndexed { index, instruction ->
            when (instruction.opcode) {
                Opcode.NEW_INSTANCE -> {
                    val register = (instruction as OneRegisterInstruction).registerA
                    val type = ((instruction as ReferenceInstruction).reference as TypeReference).type
                    built[register] = type
                }

                Opcode.INVOKE_VIRTUAL -> {
                    val reference = (instruction as ReferenceInstruction).reference as MethodReference
                    if (reference.name == "setOnClickListener" && reference.definingClass == "Landroid/view/View;") {
                        val call = instruction as FiveRegisterInstruction
                        val listener = built[call.registerD] ?: return@forEachIndexed
                        val constructor = classDefBy(listener).methods.firstOrNull { it.name == "<init>" }
                            ?: return@forEachIndexed
                        if (constructor.parameterTypes.firstOrNull()?.toString() == UNSEND_ARGUMENTS) {
                            onViewCreated.replaceInstruction(
                                index,
                                "invoke-static { v${call.registerC}, v${call.registerD} }, " +
                                    "$EXTENSION->setUnsendClickListener(Landroid/view/View;Landroid/view/View\$OnClickListener;)V",
                            )
                            replaced++
                        }
                    }
                }

                else -> {}
            }
        }

        if (replaced != 2) {
            throw PatchException("Expected 2 unsend buttons, found $replaced")
        }

        markLineSettingIncluded("chatHaptics")
    }
}

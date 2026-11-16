package app.andrewliang.patches.line.blursystembars

import app.andrewliang.patches.line.shared.LineApplicationOnCreateFingerprint
import app.andrewliang.patches.line.shared.lineSettingsExtensionPatch
import app.andrewliang.patches.line.shared.markLineSettingIncluded
import app.andrewliang.patches.shared.Constants.COMPATIBILITY_LINE
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION = "Lapp/andrewliang/extension/BlurBars;"

@Suppress("unused")
val blurSystemBarsPatch = bytecodePatch(
    name = "[General] Transparent system bars with blur",
    description = "Removes the theme color blocks behind the status bar and the gesture bar on every " +
        "screen: the theme artwork fills them, and the bottom tabs keep their theme. In chats, the " +
        "background fills the window, the header and the input bar stay on top, and messages blur " +
        "out behind them. The input box has smooth rounded corners. Battery Saver shows a dark " +
        "fade instead of the blur.",
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

        // The Compose top bar: the status bar strip gets a clear color, and the artwork moves
        // from the bar to the whole column, cropped instead of stretched. So the artwork fills
        // the status bar. Each change goes through the extension, which keeps LINE's value while
        // the switch is off. No new instruction is a branch target, so no branch moves.
        ComposeTopBarFingerprint.method.apply {
            val instructions = implementation!!.instructions.toList()
            fun Instruction.methodRef() = (this as? ReferenceInstruction)?.reference as? MethodReference
            fun Instruction.fieldRef() = (this as? ReferenceInstruction)?.reference as? FieldReference
            fun low(register: Int): Int {
                if (register > 15) throw PatchException("Register v$register is too high for invoke")
                return register
            }

            // The artwork modifier: Modifier.paint(painter, contentScale = FillBounds, ...).
            val drawIndex = instructions.indexOfFirst {
                it.opcode == Opcode.INVOKE_STATIC_RANGE &&
                    it.methodRef()?.definingClass == "Landroidx/compose/ui/draw/a;"
            }
            if (drawIndex < 0) throw PatchException("Compose paint call not found")
            val draw = instructions[drawIndex] as RegisterRangeInstruction
            val modifierType = draw.methodRef()!!.returnType
            val contentScaleType = draw.methodRef()!!.parameterTypes[3].toString()
            val scaleRegister = draw.startRegister + 3
            val artworkRegister = (instructions[drawIndex + 1] as OneRegisterInstruction).registerA

            val scaleIndex = (drawIndex - 1 downTo 0).first {
                val instruction = instructions[it]
                instruction.opcode == Opcode.SGET_OBJECT &&
                    (instruction as OneRegisterInstruction).registerA == scaleRegister
            }
            val scaleField = instructions[scaleIndex].fieldRef()!!
            // The next instruction sets the alpha register, so it is free until then.
            val afterScale = instructions[scaleIndex + 1]
            if (afterScale !is OneRegisterInstruction || afterScale.registerA != scaleRegister + 1 ||
                !afterScale.opcode.name.startsWith("const")
            ) {
                throw PatchException("Unexpected instruction after the content scale")
            }
            // ContentScale.Crop is the first field of the ContentScale companion.
            val crop = classDefBy(scaleField.definingClass).staticFields.first()
            if (crop.type == scaleField.type) throw PatchException("ContentScale.Crop not found")

            // The status bar strip: Modifier.background(color, RectangleShape).
            val colorIndex = instructions.indexOfFirst {
                it.opcode == Opcode.INVOKE_STATIC && it.methodRef()?.definingClass == "Lu1/n;" &&
                    it.methodRef()?.name == "b"
            }
            val colorCall = instructions[colorIndex] as FiveRegisterInstruction

            // The column modifier: modifier.then(Modifier).
            val thenIndex = (drawIndex + 1 until instructions.size).first {
                it.let { i -> instructions[i] }.let { instruction ->
                    instruction.opcode == Opcode.INVOKE_INTERFACE &&
                        instruction.methodRef()?.definingClass == modifierType &&
                        instruction.methodRef()?.parameterTypes?.size == 1
                }
            }
            val then = instructions[thenIndex] as FiveRegisterInstruction
            val thenResult = (instructions[thenIndex + 1] as OneRegisterInstruction).registerA

            // The bar modifier: the artwork modifier with the window insets padding.
            val padIndex = (thenIndex + 1 until instructions.size).first {
                val instruction = instructions[it]
                val reference = instruction.methodRef()
                instruction.opcode == Opcode.INVOKE_STATIC && reference != null &&
                    reference.returnType == modifierType && reference.parameterTypes.size == 2 &&
                    reference.parameterTypes[0] == modifierType &&
                    (instruction as FiveRegisterInstruction).registerC == artworkRegister
            }
            // The insets object is built just before it. Its int argument register is free after.
            val insets = instructions[padIndex - 1] as FiveRegisterInstruction
            if (insets.opcode != Opcode.INVOKE_DIRECT || insets.registerCount != 3) {
                throw PatchException("Unexpected instruction before the bar padding")
            }
            val freeRegister = insets.registerE
            val empty = instructions.first {
                it.opcode == Opcode.SGET_OBJECT && it.fieldRef()?.definingClass == modifierType.removeSuffix(";") + "\$a;"
            }.fieldRef()!!

            // From the last change to the first, so the found indexes stay right.
            addInstructions(
                padIndex,
                """
                    sget-object v$freeRegister, $empty
                    invoke-static { v${low(artworkRegister)}, v${low(freeRegister)} }, $EXTENSION->composeInnerModifier(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                    move-result-object v$artworkRegister
                    check-cast v$artworkRegister, $modifierType
                """,
            )
            replaceInstruction(
                thenIndex,
                "invoke-interface { v${low(then.registerC)}, v${low(thenResult)} }, ${then.methodRef()}",
            )
            addInstructions(
                thenIndex,
                """
                    invoke-static { v${low(artworkRegister)}, v${low(then.registerD)} }, $EXTENSION->composeOuterModifier(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                    move-result-object v$thenResult
                    check-cast v$thenResult, $modifierType
                """,
            )
            addInstructions(
                scaleIndex + 1,
                """
                    sget-object v${scaleRegister + 1}, ${crop.definingClass}->${crop.name}:${crop.type}
                    invoke-static/range { v$scaleRegister .. v${scaleRegister + 1} }, $EXTENSION->composeArtworkScale(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                    move-result-object v$scaleRegister
                    check-cast v$scaleRegister, $contentScaleType
                """,
            )
            addInstructions(
                colorIndex,
                """
                    invoke-static { v${low(colorCall.registerD)}, v${low(colorCall.registerE)} }, $EXTENSION->composeStatusBarColor(J)J
                    move-result-wide v${colorCall.registerD}
                """,
            )
        }

        // The chat background is drawn shorter than the window: it stops above the input bar,
        // and official themes also leave out the status bar and the gesture bar. After those
        // bounds are set, the extension crops the image to the full view. The layout method
        // that would push official themes down by the status bar height is skipped, and captures
        // of the background use the full view size.
        //
        // super.onDraw is the target of a goto from the setImageDrawable path. Replacing that
        // instruction keeps the label on the hook, so both paths run it. Inserting in front
        // of it would leave the label on super.onDraw, and the image path would skip the fix.
        val superDraw = ChatHistorySkinOnDrawFingerprint.method.implementation!!.instructions.indexOfLast {
            it.opcode == Opcode.INVOKE_SUPER &&
                (it as? ReferenceInstruction)?.reference is MethodReference &&
                (it.reference as MethodReference).name == "onDraw"
        }
        if (superDraw < 0) throw PatchException("super.onDraw not found in ChatHistorySkinImageView")
        val superRef = (ChatHistorySkinOnDrawFingerprint.method.implementation!!.instructions[superDraw]
            as ReferenceInstruction).reference as MethodReference
        ChatHistorySkinOnDrawFingerprint.method.replaceInstruction(
            superDraw,
            "invoke-static { p0 }, $EXTENSION->layoutChatSkin(Landroid/widget/ImageView;)V",
        )
        ChatHistorySkinOnDrawFingerprint.method.addInstruction(
            superDraw + 1,
            "invoke-super { p0, p1 }, ${superRef.definingClass}->${superRef.name}(" +
                superRef.parameterTypes.joinToString("") + ")${superRef.returnType}",
        )
        ChatHistorySkinAreaFingerprint.method.addInstructions(
            0,
            """
                invoke-static { p0 }, $EXTENSION->chatSkinArea(Landroid/widget/ImageView;)Landroid/util/Size;
                move-result-object v0
                if-eqz v0, :morphe_skin_area
                return-object v0
                :morphe_skin_area
                nop
            """.trimIndent(),
        )
        ChatHistorySkinLayoutFingerprint.method.addInstructions(
            0,
            """
                invoke-static { p0 }, $EXTENSION->prepareChatSkin(Landroid/view/View;)Z
                move-result v0
                if-eqz v0, :morphe_skin_layout
                return-void
                :morphe_skin_layout
                nop
            """.trimIndent(),
        )

        markLineSettingIncluded("blurSystemBars")
    }
}

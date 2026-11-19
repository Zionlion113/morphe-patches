package app.andrewliang.patches.line.blursystembars

import app.andrewliang.patches.line.shared.LineApplicationOnCreateFingerprint
import app.andrewliang.patches.line.shared.lineSettingsExtensionPatch
import app.andrewliang.patches.line.shared.markLineSettingIncluded
import app.andrewliang.patches.shared.Constants.COMPATIBILITY_LINE
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val EXTENSION = "Lapp/andrewliang/extension/BlurBars;"
private const val PIXEL_EXT = "Lapp/andrewliang/extension/PixelNativeEnhancements;"
private const val EXTENSION_PREFIX = "Lapp/andrewliang/extension/"

@Suppress("unused")
val blurSystemBarsPatch = bytecodePatch(
    name = "[General] Transparent system bars with blur",
    description = "Removes the theme color blocks behind the status bar and the gesture bar on every screen: " +
        "the theme artwork fills them, and the bottom tabs keep their theme. In chats, the background fills " +
        "the window, the header and the input bar stay on top, and messages blur out behind them. The input " +
        "box has smooth rounded corners. Battery Saver shows a dark fade instead of the blur.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_LINE)

    dependsOn(lineSettingsExtensionPatch)

    execute {
        LineApplicationOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static { p0 }, $EXTENSION->init(Landroid/app/Application;)V",
        )

        val edgeToEdge = EdgeToEdgeWindowFingerprint.method
        var replaced = 0
        edgeToEdge.implementation!!.instructions.toList().forEachIndexed { index, instruction ->
            if (instruction.opcode != Opcode.INVOKE_VIRTUAL) return@forEachIndexed
            val reference = (instruction as ReferenceInstruction).reference as MethodReference
            if (reference.definingClass != "Landroid/view/Window;") return@forEachIndexed
            if (reference.name != "setNavigationBarColor" && reference.name != "setStatusBarColor") {
                return@forEachIndexed
            }
            val call = instruction as FiveRegisterInstruction
            edgeToEdge.replaceInstruction(
                index,
                "invoke-static { v${call.registerC}, v${call.registerD} }, " +
                    "$EXTENSION->${reference.name}(Landroid/view/Window;I)V",
            )
            replaced++
        }
        if (replaced != 2) {
            throw PatchException("Expected 2 bar color calls in the edge-to-edge helper, found $replaced")
        }

        val topBar = ComposeTopBarFingerprint.method
        val instructions = topBar.implementation!!.instructions.toList()
        val drawIndex = instructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.INVOKE_STATIC_RANGE &&
                instruction.methodRef()?.definingClass == "Landroidx/compose/ui/draw/a;"
        }
        if (drawIndex < 0) throw PatchException("Compose paint call not found")

        val drawCall = instructions[drawIndex] as RegisterRangeInstruction
        val drawRef = (drawCall as Instruction).methodRef()!!
        val modifierType = drawRef.returnType
        val contentScaleType = drawRef.parameterTypes[3].toString()
        val scaleRegister = drawCall.startRegister + 3
        val artworkRegister = (instructions[drawIndex + 1] as OneRegisterInstruction).registerA

        val scaleIndex = (drawIndex - 1 downTo 0).first { i ->
            val inst = instructions[i]
            inst.opcode == Opcode.SGET_OBJECT && (inst as OneRegisterInstruction).registerA == scaleRegister
        }
        val scaleField = instructions[scaleIndex].fieldRef()!!
        val nextInst = instructions[scaleIndex + 1]
        if (nextInst !is OneRegisterInstruction ||
            nextInst.registerA != scaleRegister + 1 ||
            !nextInst.opcode.name.startsWith("const")
        ) {
            throw PatchException("Unexpected instruction after the content scale")
        }

        val crop = classDefBy(scaleField.definingClass).staticFields.first()
        if (crop.type == scaleField.type) {
            throw PatchException("ContentScale.Crop not found")
        }

        val colorIndex = instructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.INVOKE_STATIC &&
                instruction.methodRef()?.let { it.definingClass == "Lu1/n;" && it.name == "b" } == true
        }
        val colorCall = instructions[colorIndex] as FiveRegisterInstruction

        val thenIndex = (drawIndex + 1 until instructions.size).first { i ->
            val inst = instructions[i]
            inst.opcode == Opcode.INVOKE_INTERFACE &&
                inst.methodRef()?.let { it.definingClass == modifierType && it.parameterTypes.size == 1 } == true
        }
        val thenCall = instructions[thenIndex] as FiveRegisterInstruction
        val thenResult = (instructions[thenIndex + 1] as OneRegisterInstruction).registerA

        val outerIndex = (thenIndex + 1 until instructions.size).first { i ->
            val inst = instructions[i]
            val ref = inst.methodRef()
            inst.opcode == Opcode.INVOKE_STATIC &&
                ref != null &&
                ref.returnType == modifierType &&
                ref.parameterTypes.size == 2 &&
                ref.parameterTypes[0] == modifierType &&
                (inst as FiveRegisterInstruction).registerC == artworkRegister
        }
        val insets = instructions[outerIndex - 1] as FiveRegisterInstruction
        if (insets.opcode != Opcode.INVOKE_DIRECT || insets.registerCount != 3) {
            throw PatchException("Unexpected instruction before the bar padding")
        }
        val freeRegister = insets.registerE
        val emptyCompanion = "${modifierType.removeSuffix(";")}\$a;"
        val empty = instructions.first { inst ->
            inst.opcode == Opcode.SGET_OBJECT && inst.fieldRef()?.definingClass == emptyCompanion
        }.fieldRef()!!

        topBar.addInstructions(
            outerIndex,
            """
                sget-object v$freeRegister, $empty
                invoke-static { v${low(artworkRegister)}, v${low(freeRegister)} }, $EXTENSION->composeInnerModifier(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object v$artworkRegister
                check-cast v$artworkRegister, $modifierType
            """,
        )
        topBar.replaceInstruction(
            thenIndex,
            "invoke-interface { v${low(thenCall.registerC)}, v${low(thenResult)} }, ${(thenCall as Instruction).methodRef()}",
        )
        topBar.addInstructions(
            thenIndex,
            """
                invoke-static { v${low(artworkRegister)}, v${low(thenCall.registerD)} }, $EXTENSION->composeOuterModifier(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object v$thenResult
                check-cast v$thenResult, $modifierType
            """,
        )
        topBar.addInstructions(
            scaleIndex + 1,
            """
                sget-object v${scaleRegister + 1}, ${crop.definingClass}->${crop.name}:${crop.type}
                invoke-static/range { v$scaleRegister .. v${scaleRegister + 1} }, $EXTENSION->composeArtworkScale(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object v$scaleRegister
                check-cast v$scaleRegister, $contentScaleType
            """,
        )
        topBar.addInstructions(
            colorIndex,
            """
                invoke-static { v${low(colorCall.registerD)}, v${low(colorCall.registerE)} }, $EXTENSION->composeStatusBarColor(J)J
                move-result-wide v${colorCall.registerD}
            """,
        )

        val skinOnDraw = ChatHistorySkinOnDrawFingerprint.method
        val skinOnDrawInstructions = skinOnDraw.implementation!!.instructions
        val superDraw = skinOnDrawInstructions.indexOfLast { instruction ->
            instruction.opcode == Opcode.INVOKE_SUPER &&
                ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.name == "onDraw"
        }
        if (superDraw < 0) throw PatchException("super.onDraw not found in ChatHistorySkinImageView")
        val superRef = (skinOnDrawInstructions[superDraw] as ReferenceInstruction).reference as MethodReference
        skinOnDraw.replaceInstruction(
            superDraw,
            "invoke-static { p0 }, $EXTENSION->layoutChatSkin(Landroid/widget/ImageView;)V",
        )
        skinOnDraw.addInstruction(
            superDraw + 1,
            "invoke-super { p0, p1 }, ${superRef.definingClass}->${superRef.name}(${superRef.parameterTypes.joinToString("")})${superRef.returnType}",
        )

        val skinArea = ChatHistorySkinAreaFingerprint.method
        skinArea.addInstructionsWithLabels(
            0,
            """
                invoke-static { p0 }, $EXTENSION->chatSkinArea(Landroid/widget/ImageView;)Landroid/util/Size;
                move-result-object v0
                if-eqz v0, :morphe_skin_area
                return-object v0
            """,
            ExternalLabel("morphe_skin_area", skinArea.getInstruction(0)),
        )

        val skinLayout = ChatHistorySkinLayoutFingerprint.method
        skinLayout.addInstructionsWithLabels(
            0,
            """
                invoke-static { p0 }, $EXTENSION->prepareChatSkin(Landroid/view/View;)Z
                move-result v0
                if-eqz v0, :morphe_skin_layout
                return-void
            """,
            ExternalLabel("morphe_skin_layout", skinLayout.getInstruction(0)),
        )

        val scrollWithOffset = ChatHistoryRecyclerScrollOffsetFingerprint.method
        val scrollRegs = scrollWithOffset.implementation!!.registerCount
        low(scrollRegs - 1)
        scrollWithOffset.addInstructions(
            0,
            """
                invoke-static { p0, p1, p2 }, $EXTENSION->adjustChatScrollOffset(Landroid/view/View;II)I
                move-result p2
            """,
        )

        ComposeColorResolverFingerprint.methodOrNull?.let { colorResolver ->
            val colorInsts = colorResolver.implementation?.instructions?.toList().orEmpty()
            val getColorIdx = colorInsts.indexOfFirst { inst ->
                inst.opcode == Opcode.INVOKE_VIRTUAL &&
                    inst.methodRef()?.let { ref ->
                        ref.definingClass == "Landroid/content/res/Resources;" &&
                            ref.name == "getColor" &&
                            ref.parameterTypes.size == 2 &&
                            ref.returnType == "I"
                    } == true
            }
            if (getColorIdx >= 0) {
                val call = colorInsts[getColorIdx] as FiveRegisterInstruction
                if (call.registerCount == 3 &&
                    call.registerC <= 15 &&
                    call.registerD <= 15 &&
                    call.registerE <= 15
                ) {
                    colorResolver.replaceInstruction(
                        getColorIdx,
                        "invoke-static { v${call.registerC}, v${call.registerD}, v${call.registerE} }, " +
                            "$EXTENSION->getComposeColor(Landroid/content/res/Resources;ILandroid/content/res/Resources\$Theme;)I",
                    )
                }
            }
        }

        mutableClassDefByOrNull("Lcom/linecorp/line/compose/theme/i;")?.let { composeThemeClass ->
            for (method in composeThemeClass.methods.toList()) {
                // Slot mapping matches gh6.j fields:
                // 1 = icon/dot (h, i), 2 = background (b, f), 3 = stroke (c, d), 4 = text (j, k)
                val slot = when (method.name) {
                    "h", "i" -> 1
                    "b", "f" -> 2
                    "c", "d" -> 3
                    "j", "k" -> 4
                    else -> 0
                }
                val paramTypes = method.parameterTypes.map { it.toString() }
                if (slot == 0 ||
                    !AccessFlags.STATIC.isSet(method.accessFlags) ||
                    method.returnType != "J" ||
                    paramTypes.isEmpty() ||
                    paramTypes[0] != "Ljava/util/Set;"
                ) {
                    continue
                }
                val impl = method.implementation ?: continue
                val ins = paramTypes.sumOf { if (it == "J" || it == "D") 2 else 1 }
                val labelName = "morphe_compose_theme_${method.name}"

                if (impl.registerCount - ins >= 2 && impl.registerCount - ins <= 15) {
                    method.addInstructionsWithLabels(
                        0,
                        """
                            const/4 v0, $slot
                            invoke-static { p0, v0 }, $EXTENSION->overrideComposeThemeColor(Ljava/util/Set;I)I
                            move-result v0
                            if-eqz v0, :$labelName
                            invoke-static { v0 }, $EXTENSION->argbToComposeColor(I)J
                            move-result-wide v0
                            return-wide v0
                        """,
                        ExternalLabel(labelName, method.getInstruction(0)),
                    )
                } else {
                    // Zero-local overloads (d, f, i, k): preserve original implementation untouched in a
                    // private static backup method and replace entry point with an (ins + 2)-register wrapper.
                    val backupName = "${method.name}\$morphe"
                    val immutableParams = paramTypes.map { ImmutableMethodParameter(it, null, null) }
                    val backupMethod = MutableMethod(
                        ImmutableMethod(
                            composeThemeClass.type,
                            backupName,
                            immutableParams,
                            method.returnType,
                            AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
                            null,
                            null,
                            impl,
                        ),
                    )
                    val wrapper = MutableMethod(
                        ImmutableMethod(
                            composeThemeClass.type,
                            method.name,
                            immutableParams,
                            method.returnType,
                            method.accessFlags,
                            null,
                            null,
                            MutableMethodImplementation(ins + 2),
                        ),
                    )
                    val descriptor = paramTypes.joinToString("")
                    wrapper.addInstructions(
                        0,
                        """
                            invoke-static/range { v2 .. v${ins + 1} }, ${composeThemeClass.type}->$backupName($descriptor)J
                            move-result-wide v0
                            return-wide v0
                        """,
                    )
                    wrapper.addInstructionsWithLabels(
                        0,
                        """
                            const/4 v0, $slot
                            invoke-static { v2, v0 }, $EXTENSION->overrideComposeThemeColor(Ljava/util/Set;I)I
                            move-result v0
                            if-eqz v0, :$labelName
                            invoke-static { v0 }, $EXTENSION->argbToComposeColor(I)J
                            move-result-wide v0
                            return-wide v0
                        """,
                        ExternalLabel(labelName, wrapper.getInstruction(0)),
                    )
                    composeThemeClass.methods.remove(method)
                    composeThemeClass.methods.add(backupMethod)
                    composeThemeClass.methods.add(wrapper)
                }
            }
        }

        val windowOwners = mutableSetOf<String>()
        classDefForEach { classDef ->
            if (classDef.type.startsWith(EXTENSION_PREFIX)) return@classDefForEach
            if (classDef.methods.any { it.callsModalShow() }) {
                windowOwners += classDef.type
            }
        }
        windowOwners.forEach { type ->
            mutableClassDefByOrNull(type)?.methods?.forEach { method ->
                method.rerouteModalShow { candidate -> isKnownDialogType(candidate) }
            }
        }

        markLineSettingIncluded("blurSystemBars")
    }
}

private val KNOWN_DIALOG_SUPERCLASSES = setOf(
    "Landroid/app/Dialog;",
    "Landroid/app/AlertDialog;",
    "Landroidx/appcompat/app/AppCompatDialog;",
    "Landroidx/appcompat/app/AlertDialog;",
    "Landroidx/activity/ComponentDialog;",
    "Lcom/google/android/material/bottomsheet/BottomSheetDialog;",
)

private fun app.morphe.patcher.patch.BytecodePatchContext.isKnownDialogType(type: String): Boolean {
    if (type in KNOWN_DIALOG_SUPERCLASSES) return true
    var current: String? = type
    var depth = 0
    while (current != null && depth < 8) {
        if (current in KNOWN_DIALOG_SUPERCLASSES) return true
        if (current == "Ljava/lang/Object;" || current.endsWith("DialogFragment;")) return false
        current = classDefByOrNull(current)?.superclass
        depth++
    }
    return false
}

private fun Instruction.modalShowTarget(): String? {
    if (opcode != Opcode.INVOKE_VIRTUAL && opcode != Opcode.INVOKE_VIRTUAL_RANGE) return null
    val ref = methodRef() ?: return null
    if (ref.returnType != "V") return null
    val params = ref.parameterTypes.joinToString("")
    return when {
        ref.name == "show" && params.isEmpty() && !ref.definingClass.endsWith("DialogFragment;") && (
            ref.definingClass in KNOWN_DIALOG_SUPERCLASSES || ref.definingClass.endsWith("Dialog;")
            ) ->
            "$PIXEL_EXT->showBlurredDialog(Landroid/app/Dialog;)V"

        ref.definingClass == "Landroid/widget/PopupWindow;" &&
            ref.name == "showAtLocation" &&
            params == "Landroid/view/View;III" ->
            "$PIXEL_EXT->showPopupAtLocation(Landroid/widget/PopupWindow;Landroid/view/View;III)V"

        ref.definingClass == "Landroid/widget/PopupWindow;" &&
            ref.name == "showAsDropDown" &&
            params == "Landroid/view/View;" ->
            "$PIXEL_EXT->showPopupAsDropDown(Landroid/widget/PopupWindow;Landroid/view/View;)V"

        ref.definingClass == "Landroid/widget/PopupWindow;" &&
            ref.name == "showAsDropDown" &&
            params == "Landroid/view/View;II" ->
            "$PIXEL_EXT->showPopupAsDropDown(Landroid/widget/PopupWindow;Landroid/view/View;II)V"

        ref.definingClass == "Landroid/widget/PopupWindow;" &&
            ref.name == "showAsDropDown" &&
            params == "Landroid/view/View;III" ->
            "$PIXEL_EXT->showPopupAsDropDown(Landroid/widget/PopupWindow;Landroid/view/View;III)V"

        else -> null
    }
}

private fun Method.callsModalShow(): Boolean =
    implementation?.instructions?.any { it.modalShowTarget() != null } == true

private fun MutableMethod.rerouteModalShow(isDialogType: (String) -> Boolean) {
    val sites = (implementation ?: return).instructions.withIndex()
        .mapNotNull { (index, instruction) ->
            val target = instruction.modalShowTarget() ?: return@mapNotNull null
            Triple(index, instruction, target)
        }
    sites.asReversed().forEach { (index, instruction, target) ->
        if (target.endsWith("showBlurredDialog(Landroid/app/Dialog;)V")) {
            val defining = instruction.methodRef()!!.definingClass
            if (!isDialogType(defining)) {
                return@forEach
            }
        }
        val replacement = when (instruction) {
            is RegisterRangeInstruction -> {
                val endReg = instruction.startRegister + instruction.registerCount - 1
                "invoke-static/range { v${instruction.startRegister} .. v$endReg }, $target"
            }
            is FiveRegisterInstruction -> {
                val regs = buildList {
                    if (instruction.registerCount >= 1) add("v${instruction.registerC}")
                    if (instruction.registerCount >= 2) add("v${instruction.registerD}")
                    if (instruction.registerCount >= 3) add("v${instruction.registerE}")
                    if (instruction.registerCount >= 4) add("v${instruction.registerF}")
                    if (instruction.registerCount >= 5) add("v${instruction.registerG}")
                }.joinToString(", ")
                "invoke-static { $regs }, $target"
            }
            else -> return@forEach
        }
        replaceInstruction(index, replacement)
    }
}

private fun Instruction.methodRef(): MethodReference? =
    (this as? ReferenceInstruction)?.reference as? MethodReference

private fun Instruction.fieldRef(): FieldReference? =
    (this as? ReferenceInstruction)?.reference as? FieldReference

private fun low(register: Int): Int {
    if (register > 15) throw PatchException("Register v$register is too high for invoke")
    return register
}

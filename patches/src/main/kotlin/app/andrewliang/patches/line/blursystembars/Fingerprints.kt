package app.andrewliang.patches.line.blursystembars

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val CHAT_SKIN_CLASS =
    "Lcom/linecorp/line/chat/ui/resources/message/message/list/skin/ChatHistorySkinImageView;"

internal object EdgeToEdgeWindowFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        methodCall(definingClass = "Landroid/view/Window;", name = "setNavigationBarColor"),
        methodCall(definingClass = "Landroid/view/Window;", name = "setNavigationBarDividerColor"),
        methodCall(definingClass = "Landroid/view/Window;", name = "setStatusBarColor"),
        fieldAccess(
            definingClass = "Landroid/view/WindowManager\$LayoutParams;",
            name = "layoutInDisplayCutoutMode",
            opcode = Opcode.IPUT,
        ),
    ),
)

internal object ComposeTopBarFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        methodCall(definingClass = "Lcom/linecorp/line/compose/theme/i;", name = "b"),
        methodCall(definingClass = "Lu1/n;", name = "b"),
        methodCall(definingClass = "Lcom/linecorp/line/compose/theme/i;", name = "e"),
        methodCall(definingClass = "Landroidx/compose/ui/draw/a;", name = "a"),
        methodCall(definingClass = "Lcom/linecorp/line/compose/theme/a;", name = "c"),
    ),
    custom = { method, _ ->
        method.parameterTypes.size == 7 &&
            method.parameterTypes[0] == "J" &&
            method.parameterTypes[6] == "I"
    },
)

internal object ChatHistorySkinAreaFingerprint : Fingerprint(
    returnType = "Landroid/util/Size;",
    parameters = emptyList(),
    custom = { method, classDef ->
        classDef.type == CHAT_SKIN_CLASS && method.name == "getContentSkinImageArea"
    },
)

internal object ChatHistorySkinLayoutFingerprint : Fingerprint(
    returnType = "V",
    parameters = emptyList(),
    custom = { method, classDef ->
        classDef.type == CHAT_SKIN_CLASS &&
            method.implementation?.instructions?.any { instruction ->
                instruction.opcode == Opcode.IPUT &&
                    ((instruction as? ReferenceInstruction)?.reference as? FieldReference)?.name == "topMargin"
            } == true
    },
)

internal object ChatHistorySkinOnDrawFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/graphics/Canvas;"),
    custom = { method, classDef ->
        classDef.type == CHAT_SKIN_CLASS && method.name == "onDraw"
    },
)

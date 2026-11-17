package app.andrewliang.patches.line.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string

internal object LineApplicationOnCreateFingerprint : Fingerprint(
    definingClass = "Ljp/naver/line/android/LineApplication;",
    name = "onCreate",
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(string("LineApplication.onCreate")),
)

package app.andrewliang.patches.line.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string

/**
 * `LineApplication.onCreate()`. LINE does not obfuscate the class, and the method passes its own
 * name as a trace label.
 */
internal object LineApplicationOnCreateFingerprint : Fingerprint(
    definingClass = "Ljp/naver/line/android/LineApplication;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(string("LineApplication.onCreate")),
)

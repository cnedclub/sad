package cnedclub.sad.app

import cnedclub.sad.canvas.CanvasOp
import cnedclub.sad.script.ConfigScript
import cnedclub.sad.script.ConfigScriptEvalConfig
import cnedclub.sad.script.ConfigScriptRoot
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.jvm.util.isError
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

class Config(
    val defaultWidth: UInt,
    val defaultHeight: UInt,
    val canvasOperations: Map<String, CanvasOp>
)

fun evaluateConfig(src: SourceCode, reportHandle: (String, err: Boolean) -> Unit): Config {
    val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<ConfigScript>()

    val canvasOperations = mutableMapOf<String, CanvasOp>()
    val root = ConfigScriptRoot(canvasOperations, reportHandle)
    val result = BasicJvmScriptingHost().eval(src, compilationConfiguration, ConfigScriptEvalConfig(root))

    val isError = result.isError()

    val reports = result.reports
        .filter { it.severity != ScriptDiagnostic.Severity.DEBUG }
        .toTypedArray()

    if (reports.isNotEmpty()) {
        reportHandle("${reports.size} reports", isError)
        for (report in reports) {
            reportHandle(report.render(), isError)
        }
    }

    if (isError) throw RuntimeException("Failed to load config file due to script error")

    return Config(
        root.defaultWidth?.toUInt() ?: 16u,
        root.defaultHeight?.toUInt() ?: 16u,
        canvasOperations
    )
}

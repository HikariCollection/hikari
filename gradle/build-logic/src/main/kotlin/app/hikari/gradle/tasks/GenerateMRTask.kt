package app.hikari.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class GenerateMRTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val stringsFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val pluralsFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val stringRegex = Regex("""<string\s+name="([^"]+)"""")
        val pluralRegex = Regex("""<plurals\s+name="([^"]+)"""")

        val stringNames = stringsFile.asFile.get().readLines()
            .mapNotNull { line -> stringRegex.find(line)?.groupValues?.get(1) }
            .map { it.replace('.', '_').replace('-', '_') }
            .distinct()

        val pluralNames = pluralsFile.asFile.get().readLines()
            .mapNotNull { line -> pluralRegex.find(line)?.groupValues?.get(1) }
            .map { it.replace('.', '_').replace('-', '_') }
            .distinct()

        val builder = StringBuilder()
        builder.appendLine("package app.hikari.i18n")
        builder.appendLine()
        builder.appendLine(
            "@Suppress(\"ClassName\", \"PropertyName\", \"RedundantVisibilityModifier\", \"SpellCheckingInspection\")",
        )
        builder.appendLine("public object MR {")
        builder.appendLine("    public object strings {")
        for (name in stringNames) {
            builder.appendLine("        public val $name: StringResource get() = StringResource(R.string.$name)")
        }
        builder.appendLine("    }")
        builder.appendLine()
        builder.appendLine("    public object plurals {")
        for (name in pluralNames) {
            builder.appendLine("        public val $name: PluralsResource get() = PluralsResource(R.plurals.$name)")
        }
        builder.appendLine("    }")
        builder.appendLine("}")

        val outputFile = File(outputDir.asFile.get(), "app/hikari/i18n/MR.kt")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(builder.toString())
    }
}

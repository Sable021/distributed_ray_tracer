package com.raytracer.ci

import java.util.Locale

object ReportWriter {

    private fun n(value: Double): String = String.format(Locale.ROOT, "%.1f", value)

    private fun escape(s: String): String = s.replace("\\", "\\\\").replace("\"", "\\\"")

    fun toJson(report: ChangeabilityReport): String = buildString {
        appendLine("{")
        appendLine("  \"computedAt\": \"${escape(report.computedAt)}\",")
        appendLine("  \"index\": ${n(report.index)},")
        appendLine("  \"attributes\": {")
        report.attributes.forEachIndexed { ai, attr ->
            appendLine("    \"${attr.id}\": {")
            appendLine("      \"displayName\": \"${escape(attr.displayName)}\",")
            appendLine("      \"weight\": ${attr.weight},")
            appendLine("      \"score\": ${n(attr.score)},")
            appendLine("      \"indicators\": [")
            attr.indicators.forEachIndexed { ii, ind ->
                val comma = if (ii == attr.indicators.lastIndex) "" else ","
                appendLine(
                    "        { \"id\": \"${ind.id}\", \"name\": \"${escape(ind.name)}\", " +
                        "\"raw\": ${n(ind.raw)}, \"score\": ${n(ind.score)}, \"weight\": ${ind.weight} }$comma"
                )
            }
            appendLine("      ]")
            appendLine(if (ai == report.attributes.lastIndex) "    }" else "    },")
        }
        appendLine("  }")
        append("}")
    }

    fun toMarkdown(report: ChangeabilityReport): String = buildString {
        appendLine("# Changeability Index")
        appendLine()
        appendLine("**Score: ${n(report.index)} / 100**  _(computed ${report.computedAt})_")
        appendLine()
        appendLine("| Attribute | Weight | Score |")
        appendLine("|---|---:|---:|")
        report.attributes.forEach { attr ->
            appendLine("| ${attr.displayName} | ${attr.weight} | ${n(attr.score)} |")
        }
        appendLine("| **Changeability Index** | ${AttributeRegistry.sumWeights()} | **${n(report.index)}** |")
        appendLine()
        report.attributes.forEach { attr ->
            appendLine("### ${attr.displayName} — ${n(attr.score)}")
            appendLine()
            appendLine("| Indicator | Raw | Score | Weight |")
            appendLine("|---|---:|---:|---:|")
            attr.indicators.forEach { ind ->
                appendLine("| ${ind.id} ${ind.name} | ${n(ind.raw)} | ${n(ind.score)} | ${ind.weight} |")
            }
            appendLine()
        }
    }

    fun toConsole(report: ChangeabilityReport): String = buildString {
        val label = "Attribute"
        appendLine("%-26s %6s %8s".format(label, "Weight", "Score"))
        appendLine("-".repeat(42))
        report.attributes.forEach { attr ->
            appendLine("%-26s %6d %8s".format(attr.displayName, attr.weight, n(attr.score)))
        }
        appendLine("-".repeat(42))
        appendLine("%-26s %6d %8s".format("Changeability Index", AttributeRegistry.sumWeights(), n(report.index)))
    }
}

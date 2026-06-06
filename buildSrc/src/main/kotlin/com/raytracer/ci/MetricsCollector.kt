package com.raytracer.ci

import java.nio.file.Path

enum class TypeKind { CLASS, INTERFACE, RECORD, ENUM }

data class TypeDecl(
    val name: String,
    val kind: TypeKind,
    val isSealed: Boolean,
    val permits: List<String>,
    val implementsList: List<String>,
) {
    val isInterface: Boolean get() = kind == TypeKind.INTERFACE
}

data class JavaSource(
    val path: Path,
    val packageName: String,
    val loc: Int,
    val types: List<TypeDecl>,
    val branchingKeywordCount: Int,
    val methodCount: Int,
    val testMethodCount: Int,
    val imports: List<String>,
    val rawContent: String,
)

data class PackageMetric(val abstractness: Double, val instability: Double) {
    val distanceFromMainSequence: Double = kotlin.math.abs(abstractness + instability - 1.0)
}

object MetricsCollector {

    private val PACKAGE = Regex("""(?m)^\s*package\s+([\w.]+)\s*;""")
    private val IMPORT = Regex("""(?m)^\s*import\s+(?:static\s+)?([\w.]+)\s*;""")
    private val TYPE_DECL = Regex("""\b(class|interface|record|enum)\s+(\w+)([^{]*)\{""")
    private val TEST_ANNOTATION = Regex("""@Test\b""")
    private val BRANCH_KEYWORDS = Regex("""\b(if|for|while|switch|case|catch)\b""")
    private val PARENTHESISED_KEYWORDS = Regex("""\b(if|for|while|switch|catch)\b""")
    private val BLOCK_OPENERS = Regex("""\)\s*\{""")

    fun parse(path: Path, content: String): JavaSource {
        val pkg = PACKAGE.find(content)?.groupValues?.get(1) ?: ""
        val loc = content.lines().count { it.isNotBlank() }
        val imports = IMPORT.findAll(content).map { it.groupValues[1] }.toList()
        val branching = countMatches(content, BRANCH_KEYWORDS)
        val methods = (countMatches(content, BLOCK_OPENERS) - countMatches(content, PARENTHESISED_KEYWORDS))
            .coerceAtLeast(0)
        val testMethods = countMatches(content, TEST_ANNOTATION)
        return JavaSource(path, pkg, loc, parseTypes(content), branching, methods, testMethods, imports, content)
    }

    private fun parseTypes(content: String): List<TypeDecl> =
        TYPE_DECL.findAll(content)
            .filter { braceDepthBefore(content, it.range.first) == 0 }
            .map { match ->
                val kind = when (match.groupValues[1]) {
                    "interface" -> TypeKind.INTERFACE
                    "record" -> TypeKind.RECORD
                    "enum" -> TypeKind.ENUM
                    else -> TypeKind.CLASS
                }
                val name = match.groupValues[2]
                val tail = match.groupValues[3]
                TypeDecl(
                    name = name,
                    kind = kind,
                    isSealed = isSealedDeclaration(content, match.range.first),
                    permits = clauseTokens(tail, "permits"),
                    implementsList = clauseTokens(tail, "implements"),
                )
            }
            .toList()

    private fun braceDepthBefore(content: String, index: Int): Int {
        var depth = 0
        for (i in 0 until index) {
            when (content[i]) {
                '{' -> depth++
                '}' -> depth--
            }
        }
        return depth
    }

    private fun isSealedDeclaration(content: String, keywordStart: Int): Boolean {
        val boundary = content.lastIndexOfAny(charArrayOf(';', '{', '}'), keywordStart - 1)
        val modifiers = content.substring(boundary + 1, keywordStart)
        return Regex("""\bsealed\b""").containsMatchIn(modifiers)
    }

    private fun clauseTokens(tail: String, keyword: String): List<String> {
        val start = Regex("""\b$keyword\b""").find(tail) ?: return emptyList()
        var rest = tail.substring(start.range.last + 1)
        for (terminator in listOf("permits", "extends", "implements")) {
            if (terminator == keyword) continue
            val t = Regex("""\b$terminator\b""").find(rest)
            if (t != null) rest = rest.substring(0, t.range.first)
        }
        // Drop record components / generic params that precede the clause body.
        rest = rest.substringBefore('(')
        return rest.split(',')
            .map { it.trim().substringBefore('<') }
            .filter { it.isNotEmpty() }
    }

    fun implementationCounts(files: List<JavaSource>): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        files.forEach { file ->
            file.types.forEach { type ->
                type.implementsList.forEach { impl ->
                    counts[impl] = (counts[impl] ?: 0) + 1
                }
            }
        }
        return counts
    }

    fun interfaceToTypeRatio(files: List<JavaSource>): Double {
        val types = files.flatMap { it.types }
        if (types.isEmpty()) return 0.0
        val interfaces = types.count { it.kind == TypeKind.INTERFACE }
        return interfaces.toDouble() / types.size
    }

    fun totalSealedPermitsCount(files: List<JavaSource>): Int =
        files.flatMap { it.types }.sumOf { it.permits.size }

    fun packageMetrics(files: List<JavaSource>): Map<String, PackageMetric> {
        val internalPackages = files.map { it.packageName }.toSet()
        val efferent = internalPackages.associateWith { mutableSetOf<String>() }
        val afferent = internalPackages.associateWith { mutableSetOf<String>() }

        files.forEach { file ->
            file.imports.forEach { imp ->
                val target = imp.substringBeforeLast('.')
                if (target in internalPackages && target != file.packageName) {
                    efferent.getValue(file.packageName).add(target)
                    afferent.getValue(target).add(file.packageName)
                }
            }
        }

        return internalPackages.associateWith { pkg ->
            val types = files.filter { it.packageName == pkg }.flatMap { it.types }
            val abstractness =
                if (types.isEmpty()) 0.0
                else types.count { it.kind == TypeKind.INTERFACE }.toDouble() / types.size
            val ce = efferent.getValue(pkg).size
            val ca = afferent.getValue(pkg).size
            val instability = if (ce + ca == 0) 0.0 else ce.toDouble() / (ce + ca)
            PackageMetric(abstractness, instability)
        }
    }

    fun countMatches(text: String, regex: Regex): Int = regex.findAll(text).count()
}

package com.raytracer.ci

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.nio.file.Path

class MetricsCollectorTest {

    // ---- Per-file parser ----------------------------------------------------

    @Nested
    inner class Parser {

        @Test
        fun extractsPackageDeclaration() {
            val src = """
                package com.raytracer.geom;
                public class Foo {}
            """.trimIndent()
            assertEquals("com.raytracer.geom", MetricsCollector.parse(Path.of("Foo.java"), src).packageName)
        }

        @Test
        fun emptyPackageDefaultsToRoot() {
            val src = "public class Foo {}"
            assertEquals("", MetricsCollector.parse(Path.of("Foo.java"), src).packageName)
        }

        @Test
        fun countsTopLevelClassDeclaration() {
            val src = """
                package x;
                public class Foo {}
            """.trimIndent()
            val parsed = MetricsCollector.parse(Path.of("Foo.java"), src)
            assertEquals(1, parsed.types.size)
            val type = parsed.types.single()
            assertEquals("Foo", type.name)
            assertEquals(TypeKind.CLASS, type.kind)
            assertFalse(type.isSealed)
            assertFalse(type.isInterface)
        }

        @Test
        fun recognisesInterfaceDeclaration() {
            val src = """
                package x;
                public interface BRDF { void shade(); }
            """.trimIndent()
            val type = MetricsCollector.parse(Path.of("BRDF.java"), src).types.single()
            assertEquals("BRDF", type.name)
            assertEquals(TypeKind.INTERFACE, type.kind)
            assertTrue(type.isInterface)
        }

        @Test
        fun recognisesSealedInterfaceWithPermits() {
            val src = """
                package x;
                public sealed interface Light permits PointLight, AreaLight {
                    double[] diffuse();
                }
            """.trimIndent()
            val type = MetricsCollector.parse(Path.of("Light.java"), src).types.single()
            assertEquals(TypeKind.INTERFACE, type.kind)
            assertTrue(type.isSealed)
            assertEquals(listOf("PointLight", "AreaLight"), type.permits)
        }

        @Test
        fun recognisesRecord() {
            val src = """
                package x;
                public record RayCounts(long primary, long shadow) {}
            """.trimIndent()
            val type = MetricsCollector.parse(Path.of("RayCounts.java"), src).types.single()
            assertEquals(TypeKind.RECORD, type.kind)
        }

        @Test
        fun parsesImplementsList() {
            val src = """
                package x;
                public final class PhongBRDF implements BRDF {
                    public void shade() {}
                }
            """.trimIndent()
            val type = MetricsCollector.parse(Path.of("PhongBRDF.java"), src).types.single()
            assertEquals(listOf("BRDF"), type.implementsList)
        }

        @Test
        fun parsesMultipleImplementsTokens() {
            val src = """
                package x;
                public final class Composite implements Foo, Bar, Baz {}
            """.trimIndent()
            val type = MetricsCollector.parse(Path.of("Composite.java"), src).types.single()
            assertEquals(listOf("Foo", "Bar", "Baz"), type.implementsList)
        }

        @Test
        fun countsLocSkipsBlankLines() {
            val src = "package x;\n\n\npublic class Foo {}\n\n"
            assertEquals(2, MetricsCollector.parse(Path.of("Foo.java"), src).loc)
        }

        @Test
        fun branchingKeywordCountCountsControlFlow() {
            val src = """
                package x;
                class Foo {
                    void m() {
                        if (true) {}
                        for (int i = 0; i < 10; i++) {}
                        while (false) {}
                        switch (1) { case 1: break; }
                        try {} catch (Exception e) {}
                    }
                }
            """.trimIndent()
            val parsed = MetricsCollector.parse(Path.of("Foo.java"), src)
            // 1 if + 1 for + 1 while + 1 switch + 1 case + 1 catch = 6
            assertEquals(6, parsed.branchingKeywordCount)
        }

        @Test
        fun methodCountFindsTopLevelAndNestedMethods() {
            val src = """
                package x;
                class Foo {
                    public void a() {}
                    private int b(int x) { return x; }
                    static String c() { return ""; }
                }
            """.trimIndent()
            assertEquals(3, MetricsCollector.parse(Path.of("Foo.java"), src).methodCount)
        }

        @Test
        fun testMethodCountFindsJunitAnnotations() {
            val src = """
                package x;
                import org.junit.jupiter.api.Test;
                class FooTest {
                    @Test void a() {}
                    @Test void b() {}
                    void helper() {}
                }
            """.trimIndent()
            assertEquals(2, MetricsCollector.parse(Path.of("FooTest.java"), src).testMethodCount)
        }
    }

    // ---- Aggregate analyses -------------------------------------------------

    @Nested
    inner class Aggregates {

        private fun src(pkg: String, name: String, kind: String = "class",
                        implementsList: List<String> = emptyList(),
                        sealed: Boolean = false, permits: List<String> = emptyList(),
                        body: String = ""): JavaSource {
            val sealedKw = if (sealed) "sealed " else ""
            val implKw = if (implementsList.isNotEmpty()) " implements ${implementsList.joinToString(", ")}" else ""
            val permitsKw = if (permits.isNotEmpty()) " permits ${permits.joinToString(", ")}" else ""
            val text = "package $pkg;\npublic $sealedKw$kind $name$implKw$permitsKw { $body }\n"
            return MetricsCollector.parse(Path.of("$name.java"), text)
        }

        @Test
        fun countsInterfaceImplementations() {
            val files = listOf(
                src("x", "BRDF", kind = "interface"),
                src("x", "PhongBRDF", implementsList = listOf("BRDF")),
                src("x", "LambertBRDF", implementsList = listOf("BRDF")),
                src("x", "Unrelated"),
            )
            val counts = MetricsCollector.implementationCounts(files)
            assertEquals(2, counts["BRDF"])
            assertNull(counts["Unrelated"])
        }

        @Test
        fun computesInterfaceToTypeRatio() {
            val files = listOf(
                src("x", "BRDF", kind = "interface"),
                src("x", "PhongBRDF"),
                src("x", "Sphere"),
                src("x", "Plane"),
            )
            // 1 interface / 4 types = 0.25
            assertEquals(0.25, MetricsCollector.interfaceToTypeRatio(files), 1e-9)
        }

        @Test
        fun countsSealedPermitsAcrossFiles() {
            val files = listOf(
                src("x", "Light", kind = "interface", sealed = true, permits = listOf("PointLight", "AreaLight")),
                src("x", "Primitive", kind = "interface", sealed = true,
                    permits = listOf("Sphere", "Plane", "Triangle", "Cylinder", "BoundedQuad")),
                src("x", "Sphere"),
            )
            assertEquals(7, MetricsCollector.totalSealedPermitsCount(files))
        }

        @Test
        fun computesPackageInstabilityFromImports() {
            // Two packages: a (depends on nothing); b (depends on a).
            // I(a) = 0/(1+0) = 0, I(b) = 1/(0+1) = 1.
            val files = listOf(
                JavaSource(Path.of("A.java"), "a", loc = 10, types = listOf(
                    TypeDecl("A", TypeKind.CLASS, isSealed = false, permits = emptyList(),
                        implementsList = emptyList())), branchingKeywordCount = 0,
                    methodCount = 0, testMethodCount = 0, imports = emptyList(), rawContent = ""),
                JavaSource(Path.of("B.java"), "b", loc = 10, types = listOf(
                    TypeDecl("B", TypeKind.CLASS, isSealed = false, permits = emptyList(),
                        implementsList = emptyList())), branchingKeywordCount = 0,
                    methodCount = 0, testMethodCount = 0, imports = listOf("a.A"), rawContent = ""),
            )
            val metrics = MetricsCollector.packageMetrics(files)
            assertEquals(0.0, metrics.getValue("a").instability, 1e-9)
            assertEquals(1.0, metrics.getValue("b").instability, 1e-9)
        }

        @Test
        fun computesPackageAbstractnessFromInterfaceRatio() {
            val files = listOf(
                JavaSource(Path.of("I.java"), "p", loc = 1, types = listOf(
                    TypeDecl("I", TypeKind.INTERFACE, isSealed = false, permits = emptyList(),
                        implementsList = emptyList())),
                    branchingKeywordCount = 0, methodCount = 0, testMethodCount = 0,
                    imports = emptyList(), rawContent = ""),
                JavaSource(Path.of("C.java"), "p", loc = 1, types = listOf(
                    TypeDecl("C", TypeKind.CLASS, isSealed = false, permits = emptyList(),
                        implementsList = emptyList())),
                    branchingKeywordCount = 0, methodCount = 0, testMethodCount = 0,
                    imports = emptyList(), rawContent = ""),
            )
            val metrics = MetricsCollector.packageMetrics(files)
            assertEquals(0.5, metrics.getValue("p").abstractness, 1e-9)
        }
    }

    // ---- Pattern counting on raw content -----------------------------------

    @Nested
    inner class PatternCounting {

        @Test
        fun countsMatchesOfRegex() {
            val text = "Math.random(); Math.random(); foo();"
            assertEquals(2, MetricsCollector.countMatches(text, Regex("Math\\.random\\(\\)")))
        }

        @Test
        fun countsZeroWhenNoMatches() {
            assertEquals(0, MetricsCollector.countMatches("clean code", Regex("TODO|FIXME")))
        }
    }
}

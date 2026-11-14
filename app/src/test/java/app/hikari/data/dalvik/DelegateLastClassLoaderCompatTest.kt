package app.hikari.data.dalvik

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DelegateLastClassLoaderCompatTest {

    @Test
    fun `isParentFirstClass matches kotlin, kotlinx, and source api packages only`() {
        assertTrue(isParentFirstClass("kotlin.collections.CollectionsKt"))
        assertTrue(isParentFirstClass("kotlinx.coroutines.Job"))
        assertTrue(isParentFirstClass("app.hikari.source.HttpSource"))

        assertFalse(isParentFirstClass(null))
        assertFalse(isParentFirstClass("app.hikari.extension.source.CustomSource"))
        assertFalse(isParentFirstClass("okhttp3.OkHttpClient"))
    }

    @Test
    fun `resolveDelegateClass returns already loaded class before consulting any loader`() {
        val cached = String::class.java
        var bootCalled = false
        var selfCalled = false
        var parentCalled = false

        val resolved = resolveDelegateClass(
            name = "java.lang.String",
            findLoaded = { cached },
            loadFromBoot = {
                bootCalled = true
                error("unexpected")
            },
            findSelf = {
                selfCalled = true
                error("unexpected")
            },
            loadFromParent = {
                parentCalled = true
                error("unexpected")
            },
        )

        assertSame(cached, resolved)
        assertFalse(bootCalled)
        assertFalse(selfCalled)
        assertFalse(parentCalled)
    }

    @Test
    fun `resolveDelegateClass delegates kotlin and source api classes to parent before self`() {
        val parentClass = List::class.java
        val callOrder = mutableListOf<String>()

        val resolved = resolveDelegateClass(
            name = "app.hikari.source.Source",
            findLoaded = { null },
            loadFromBoot = {
                callOrder += "boot"
                throw ClassNotFoundException(it)
            },
            findSelf = {
                callOrder += "self"
                throw ClassNotFoundException(it)
            },
            loadFromParent = {
                callOrder += "parent"
                parentClass
            },
        )

        assertSame(parentClass, resolved)
        assertEquals(listOf("boot", "parent"), callOrder)
    }

    @Test
    fun `resolveDelegateClass prefers self over parent for extension classes and falls back to parent`() {
        val selfClass = Int::class.java
        val parentClass = Long::class.java
        val selfOrder = mutableListOf<String>()

        val resolvedFromSelf = resolveDelegateClass(
            name = "com.example.ExtensionSource",
            findLoaded = { null },
            loadFromBoot = {
                selfOrder += "boot"
                throw ClassNotFoundException(it)
            },
            findSelf = {
                selfOrder += "self"
                selfClass
            },
            loadFromParent = {
                selfOrder += "parent"
                parentClass
            },
        )
        assertSame(selfClass, resolvedFromSelf)
        assertEquals(listOf("boot", "self"), selfOrder)

        val fallbackOrder = mutableListOf<String>()
        val resolvedFromFallback = resolveDelegateClass(
            name = "com.example.SharedHelper",
            findLoaded = { null },
            loadFromBoot = {
                fallbackOrder += "boot"
                throw ClassNotFoundException(it)
            },
            findSelf = {
                fallbackOrder += "self"
                throw ClassNotFoundException(it)
            },
            loadFromParent = {
                fallbackOrder += "parent"
                parentClass
            },
        )
        assertSame(parentClass, resolvedFromFallback)
        assertEquals(listOf("boot", "self", "parent"), fallbackOrder)
    }

    @Test
    fun `resolveDelegateClass rethrows self ClassNotFoundException when parent fallback also fails`() {
        val selfException = ClassNotFoundException("from-self")

        val thrown = assertThrows(ClassNotFoundException::class.java) {
            resolveDelegateClass(
                name = "com.example.Missing",
                findLoaded = { null },
                loadFromBoot = null,
                findSelf = { throw selfException },
                loadFromParent = { throw ClassNotFoundException("from-parent") },
            )
        }

        assertSame(selfException, thrown)
    }
}

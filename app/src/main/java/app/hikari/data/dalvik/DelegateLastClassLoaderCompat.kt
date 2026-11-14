package app.hikari.data.dalvik

import android.os.Build
import dalvik.system.DelegateLastClassLoader
import dalvik.system.PathClassLoader
import java.net.URL
import java.util.Collections
import java.util.Enumeration

@Suppress("FunctionName")
fun DelegateLastClassLoaderCompat(
    dexPath: String,
    librarySearchPath: String?,
    parent: ClassLoader,
): ClassLoader {
    // ART recognizes the built-in loader and maps app images compiled for this app's class loader chain, and before
    // Android 17 a damaged one crashes every launch until the app is updated. ART doesn't recognize the backport.
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
        DelegateLastClassLoader(dexPath, librarySearchPath, parent)
    } else {
        DelegateLastPathClassLoader(dexPath, librarySearchPath, parent)
    }
}

/**
 * A [PathClassLoader] that looks up classes like [DelegateLastClassLoader]: the boot class loader first, then its own
 * dex path, then the parent.
 */
private class DelegateLastPathClassLoader(
    dexPath: String,
    librarySearchPath: String?,
    parent: ClassLoader,
) : PathClassLoader(dexPath, librarySearchPath, parent) {

    private val bootClassLoader: ClassLoader? = Any::class.java.classLoader

    override fun loadClass(name: String?, resolve: Boolean): Class<*> {
        return resolveDelegateClass(
            name = name,
            findLoaded = ::findLoadedClass,
            loadFromBoot = bootClassLoader?.let { loader -> { loader.loadClass(it) } },
            findSelf = ::findClass,
            loadFromParent = { parent.loadClass(it) },
        )
    }

    override fun getResource(name: String?): URL? {
        return bootClassLoader?.getResource(name)
            ?: findResource(name)
            ?: parent?.getResource(name)
    }

    override fun getResources(name: String?): Enumeration<URL> {
        val resources = buildList {
            bootClassLoader?.getResources(name)?.let { addAll(it.toList()) }
            findResources(name)?.let { addAll(it.toList()) }
            parent?.getResources(name)?.let { addAll(it.toList()) }
        }
        return Collections.enumeration(resources)
    }
}

internal fun isParentFirstClass(name: String?): Boolean {
    return name != null &&
        (name.startsWith("kotlin.") || name.startsWith("kotlinx.") || name.startsWith("app.hikari.source."))
}

internal inline fun resolveDelegateClass(
    name: String?,
    findLoaded: (String?) -> Class<*>?,
    noinline loadFromBoot: ((String?) -> Class<*>)?,
    findSelf: (String?) -> Class<*>,
    loadFromParent: (String?) -> Class<*>,
): Class<*> {
    findLoaded(name)?.let { return it }

    if (loadFromBoot != null) {
        try {
            return loadFromBoot(name)
        } catch (_: ClassNotFoundException) {}
    }

    if (isParentFirstClass(name)) {
        try {
            return loadFromParent(name)
        } catch (_: ClassNotFoundException) {}
    }

    val fromSuper = try {
        return findSelf(name)
    } catch (e: ClassNotFoundException) {
        e
    }

    return try {
        loadFromParent(name)
    } catch (_: ClassNotFoundException) {
        throw fromSuper
    }
}

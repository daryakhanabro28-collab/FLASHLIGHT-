package com.example

import com.example.data.model.VaultCategory
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun category_extension_resolution() {
        assertEquals(VaultCategory.PHOTO, VaultCategory.fromExtension("jpg"))
        assertEquals(VaultCategory.PHOTO, VaultCategory.fromExtension("PNG"))
        assertEquals(VaultCategory.PHOTO, VaultCategory.fromExtension("webp"))
        assertEquals(VaultCategory.VIDEO, VaultCategory.fromExtension("mp4"))
        assertEquals(VaultCategory.VIDEO, VaultCategory.fromExtension("mkv"))
        assertEquals(VaultCategory.DOCUMENT, VaultCategory.fromExtension("pdf"))
        assertEquals(VaultCategory.DOCUMENT, VaultCategory.fromExtension("txt"))
        assertEquals(VaultCategory.OTHER, VaultCategory.fromExtension("zip"))
    }
}

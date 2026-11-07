package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.VaultAccessMethod
import com.example.data.model.VaultCategory

class Converters {
    @TypeConverter
    fun fromCategory(category: VaultCategory): String = category.name

    @TypeConverter
    fun toCategory(value: String): VaultCategory = try {
        VaultCategory.valueOf(value)
    } catch (e: Exception) {
        VaultCategory.OTHER
    }

    @TypeConverter
    fun fromAccessMethod(method: VaultAccessMethod): String = method.name

    @TypeConverter
    fun toAccessMethod(value: String): VaultAccessMethod = try {
        VaultAccessMethod.valueOf(value)
    } catch (e: Exception) {
        VaultAccessMethod.CLOCK_LONG_PRESS
    }
}

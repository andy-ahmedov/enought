package com.andyahmedov.enought.data.repository

import android.content.SharedPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SharedPreferencesDailyLimitRepositoryTest {
    @Test
    fun `defaults to null when preference is missing`() = runTest {
        val repository = SharedPreferencesDailyLimitRepository(
            sharedPreferences = FakeSharedPreferences(),
        )

        assertNull(repository.getLimitAmountMinor())
        assertNull(repository.observeLimitAmountMinor().first())
    }

    @Test
    fun `returns updated value after saving preference`() = runTest {
        val repository = SharedPreferencesDailyLimitRepository(
            sharedPreferences = FakeSharedPreferences(),
        )

        repository.setLimitAmountMinor(150000L)

        assertEquals(150000L, repository.getLimitAmountMinor())
        assertEquals(150000L, repository.observeLimitAmountMinor().first())
    }

    @Test
    fun `clears saved value`() = runTest {
        val repository = SharedPreferencesDailyLimitRepository(
            sharedPreferences = FakeSharedPreferences(),
        )

        repository.setLimitAmountMinor(150000L)
        repository.clearLimit()

        assertNull(repository.getLimitAmountMinor())
        assertNull(repository.observeLimitAmountMinor().first())
    }

    private class FakeSharedPreferences : SharedPreferences {
        private val values = mutableMapOf<String, Any>()

        override fun getAll(): MutableMap<String, *> = values

        override fun getString(key: String?, defValue: String?): String? {
            return values[key] as? String ?: defValue
        }

        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
            @Suppress("UNCHECKED_CAST")
            return values[key] as? MutableSet<String> ?: defValues
        }

        override fun getInt(key: String?, defValue: Int): Int {
            return values[key] as? Int ?: defValue
        }

        override fun getLong(key: String?, defValue: Long): Long {
            return values[key] as? Long ?: defValue
        }

        override fun getFloat(key: String?, defValue: Float): Float {
            return values[key] as? Float ?: defValue
        }

        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            return values[key] as? Boolean ?: defValue
        }

        override fun contains(key: String?): Boolean {
            return values.containsKey(key)
        }

        override fun edit(): SharedPreferences.Editor = FakeEditor(values)

        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) = Unit

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) = Unit
    }

    private class FakeEditor(
        private val values: MutableMap<String, Any>,
    ) : SharedPreferences.Editor {
        private val pendingValues = mutableMapOf<String, Any?>()
        private var shouldClear = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply {
            pendingValues[key.orEmpty()] = value
        }

        override fun putStringSet(
            key: String?,
            values: MutableSet<String>?,
        ): SharedPreferences.Editor = apply {
            pendingValues[key.orEmpty()] = values
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply {
            pendingValues[key.orEmpty()] = value
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply {
            pendingValues[key.orEmpty()] = value
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply {
            pendingValues[key.orEmpty()] = value
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply {
            pendingValues[key.orEmpty()] = value
        }

        override fun remove(key: String?): SharedPreferences.Editor = apply {
            pendingValues[key.orEmpty()] = null
        }

        override fun clear(): SharedPreferences.Editor = apply {
            shouldClear = true
        }

        override fun commit(): Boolean {
            if (shouldClear) {
                values.clear()
            }
            pendingValues.forEach { (key, value) ->
                if (value == null) {
                    values.remove(key)
                } else {
                    values[key] = value
                }
            }
            pendingValues.clear()
            shouldClear = false
            return true
        }

        override fun apply() {
            commit()
        }
    }
}

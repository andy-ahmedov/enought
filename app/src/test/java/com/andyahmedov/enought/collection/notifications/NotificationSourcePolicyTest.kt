package com.andyahmedov.enought.collection.notifications

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationSourcePolicyTest {
    @Test
    fun `allows package from allowlist`() {
        val policy = NotificationSourcePolicy.fromPackages(
            setOf("ru.mirpay", "ru.example.bank"),
        )

        assertTrue(policy.isSupported("ru.mirpay"))
    }

    @Test
    fun `rejects package outside allowlist`() {
        val policy = NotificationSourcePolicy.fromPackages(
            setOf("ru.mirpay"),
        )

        assertFalse(policy.isSupported("ru.other.app"))
    }

    @Test
    fun `default policy allows Mir Pay, Alfa-Bank and Sber`() {
        val policy = NotificationSourcePolicy.default()

        assertTrue(policy.isSupported("ru.nspk.mirpay"))
        assertTrue(policy.isSupported("ru.alfabank.mobile.android"))
        assertTrue(policy.isSupported("ru.sberbankmobile"))
        assertFalse(policy.isSupported("com.idamob.tinkoff.android"))
        assertFalse(policy.isSupported("ru.other.app"))
    }
}

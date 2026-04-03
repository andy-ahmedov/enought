package com.andyahmedov.enought.collection.notifications

class NotificationSourcePolicy private constructor(
    private val supportedPackages: Set<String>,
) {
    fun isSupported(packageName: String): Boolean {
        return packageName.isNotBlank() && packageName in supportedPackages
    }

    companion object {
        private const val MIR_PAY_PACKAGE = "ru.nspk.mirpay"
        private const val ALFA_BANK_PACKAGE = "ru.alfabank.mobile.android"
        private const val SBER_BANK_PACKAGE = "ru.sberbankmobile"

        fun default(): NotificationSourcePolicy {
            return NotificationSourcePolicy(
                supportedPackages = setOf(
                    MIR_PAY_PACKAGE,
                    ALFA_BANK_PACKAGE,
                    SBER_BANK_PACKAGE,
                ),
            )
        }

        fun fromPackages(packageNames: Set<String>): NotificationSourcePolicy {
            return NotificationSourcePolicy(
                supportedPackages = packageNames
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toSet(),
            )
        }
    }
}

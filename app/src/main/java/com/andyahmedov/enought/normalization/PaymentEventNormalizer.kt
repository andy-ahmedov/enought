package com.andyahmedov.enought.normalization

import com.andyahmedov.enought.domain.model.PaymentCandidate
import com.andyahmedov.enought.domain.model.PaymentEvent

interface PaymentEventNormalizer {
    fun normalize(candidate: PaymentCandidate): PaymentEvent?
}

package com.example.studyreward.domain.reward

sealed interface ExchangeResult {
    data class Success(val minutesAdded: Int) : ExchangeResult
    data object InsufficientPoints : ExchangeResult
    data object AppNotFound : ExchangeResult
    data object InvalidAmount : ExchangeResult
}
package com.aykutcincik.mimir.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * App-wide singleton — herhangi bir API client 426 görünce trigger() çağrılır.
 * MimirApp Composable bu state'i dinler ve ForceUpdateScreen'a yönlendirir.
 *
 * Sadece Bootstrap'ta yakalama yetersiz: kullanıcı zaten login durumdayken
 * (refresh muaf endpoint, geçer) authenticated API'ler 426 döner — bu durumda
 * "Liste alınamadı" yerine güncelle ekranı görmek istiyoruz.
 */
object VersionGate {
    private val _triggered = MutableStateFlow(false)
    val triggered: StateFlow<Boolean> = _triggered

    fun trigger() { _triggered.value = true }
    fun reset() { _triggered.value = false }
}

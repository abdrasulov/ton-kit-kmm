package io.horizontalsystems.tonkit

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

val TonTransaction.transfers: List<Transfer>
    get() = Json.decodeFromString(transfersJson)

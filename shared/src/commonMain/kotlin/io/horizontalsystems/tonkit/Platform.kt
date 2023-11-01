package io.horizontalsystems.tonkit

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
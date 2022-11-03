package com.example.mmp3

import com.example.mmp3.rsocket.LiveSessionClient

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
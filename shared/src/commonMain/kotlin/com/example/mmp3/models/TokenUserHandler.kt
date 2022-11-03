package com.example.mmp3.models

import com.russhwolf.settings.Settings


interface TokenUserHandler {
    var sessionId : String?
}

class TokenUserSession() : TokenUserHandler{
    private val settings = Settings()
    override var sessionId : String? get() = settings.getStringOrNull("sessionId")
        set(value) {settings.putString("sessionId", value!!)}
}
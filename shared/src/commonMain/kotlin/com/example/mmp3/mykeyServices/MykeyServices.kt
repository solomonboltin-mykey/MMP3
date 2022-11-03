package com.example.mmp3.mykeyServices

import com.example.mmp3.models.TokenUserHandler

class MykeyServices(
    private val userId: String,
    private val tokenUserHandler: TokenUserHandler,
) {
    private val services = emptyList<MykeyService>()


}
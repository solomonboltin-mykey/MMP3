package com.example.mmp3.models

interface UserIdProvider {
    var userId : String?
}

expect fun getUserId() : String

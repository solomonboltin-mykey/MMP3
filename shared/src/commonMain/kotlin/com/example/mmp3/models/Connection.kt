package com.example.mmp3.models
import kotlinx.serialization.*


@Serializable
data class Connection(val userId: String, val liveId:String)
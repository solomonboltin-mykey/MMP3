package com.example.mmp3.mykeyServices

import com.example.mmp3.models.AuthenticationWrapper


interface MykeyServiceInterface{
    val serviceName : String
    val authentication: AuthenticationWrapper
    val mykeyServicesProperties: MykeyServicesProperties
    val sessions: MutableMap<String, MykeySession>

    fun baseurl() : String
    fun deleteSession(sessionId: String)
    fun


}
class MykeyService {
}
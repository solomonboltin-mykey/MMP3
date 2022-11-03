package com.example.mmp3.mykeyServices

class MykeyServicesProperties {
    val livesService = "lives"
    val livesServiceUrl = "https://lives.mykey.org"


    private val servicesUrlMap = mapOf(
        livesService to livesServiceUrl
    )

    fun getServiceUrl(serviceName: String): String {
        return servicesUrlMap[serviceName] ?: ""
    }
}


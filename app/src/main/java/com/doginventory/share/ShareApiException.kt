package com.doginventory.share

sealed class ShareApiException(message: String) : Exception(message) {
    class Network(message: String) : ShareApiException(message)
    class Http(val code: Int, message: String) : ShareApiException(message)
    class Parse(message: String) : ShareApiException(message)
}

package com.example.applora

data class Message(
    val message: String,
    val isSentByUser: Boolean,
    val senderName: String? = null
)
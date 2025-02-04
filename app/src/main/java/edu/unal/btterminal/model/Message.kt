package edu.unal.btterminal.model

data class Message(
    val content: String,
    val isSent: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) 
package edu.unal.btterminal.utils

enum class LineEnding(val value: String, val displayName: String) {
    NONE("", "None"),
    CR("\r", "CR"),
    LF("\n", "LF"),
    CRLF("\r\n", "CR+LF");
} 
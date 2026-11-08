package tv.polya.integration.torrserve

const val DEFAULT_TORRSERVE_SERVER_URL = "http://127.0.0.1:8090"

fun effectiveTorrServeServerUrl(savedValue: String?): String =
    savedValue?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() } ?: DEFAULT_TORRSERVE_SERVER_URL

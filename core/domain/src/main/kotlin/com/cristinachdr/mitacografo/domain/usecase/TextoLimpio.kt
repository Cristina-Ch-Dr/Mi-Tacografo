package com.cristinachdr.mitacografo.domain.usecase

/** Quita espacios sobrantes; un texto vacío se guarda como `null`. */
internal fun String?.limpio(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

/** País en mayúsculas ("es" → "ES"), o `null` si está vacío. */
internal fun String?.comoPais(): String? = limpio()?.uppercase()

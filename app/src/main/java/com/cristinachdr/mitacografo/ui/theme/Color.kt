package com.cristinachdr.mitacografo.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta "carretera": azul profundo (confianza, legible de día) + ámbar (avisos).
// Alto contraste en ambos temas para usarla en cabina.

// --- Tema claro ---
val AzulCarretera = Color(0xFF1E4D8C)
val SobreAzul = Color.White
val AzulContenedorClaro = Color(0xFFD6E3FF)
val SobreAzulContenedorClaro = Color(0xFF001B3E)
val Ambar = Color(0xFFB26A00)
val FondoClaro = Color(0xFFF8F9FC)
val SuperficieClara = Color.White
val SuperficieContenedorClara = Color(0xFFECEFF5) // barra inferior
val TextoClaro = Color(0xFF1A1C20)

// --- Tema oscuro (de noche en cabina) ---
val AzulNoche = Color(0xFFA8C8FF)
val SobreAzulNoche = Color(0xFF003062)
val AzulContenedorOscuro = Color(0xFF1E4D8C)
val SobreAzulContenedorOscuro = Color(0xFFD6E3FF)
val AmbarNoche = Color(0xFFFFB95C)
val FondoOscuro = Color(0xFF111318)
val SuperficieOscura = Color(0xFF1A1C20)
val SuperficieContenedorOscura = Color(0xFF22252B) // barra inferior
val TextoOscuro = Color(0xFFE2E2E9)

// --- Estados de las reglas (✅ ⚠️ ❔), comunes a ambos temas ---
val EstadoCumple = Color(0xFF2E7D32)
val EstadoExcedido = Color(0xFFC62828)
val EstadoNoVerificado = Color(0xFF757575)

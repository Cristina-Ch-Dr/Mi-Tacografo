package com.cristinachdr.mitacografo.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta sacada del icono (MT-44): el azul del fondo, el naranja de la onda y un azul clarito.
// El naranja se reserva para los avisos. Contrastes comprobados (WCAG AA, 4,5:1 en texto)
// para que se lea en cabina, de día y de noche.

// --- Colores del logo ---

/** Fondo del icono. Barra superior en los dos temas y color principal del tema claro. */
val AzulLogo = Color(0xFF234886)

/** Onda del icono. Como fondo, con texto oscuro encima (5:1); sobre blanco no llega a 4,5:1. */
val NaranjaLogo = Color(0xFFD27204)

/** Azul clarito elegido por Cristina: pestaña seleccionada y contenedores. */
val AzulClaro = Color(0xFFBCD2F5)

// --- Tema claro ---
val SobreAzul = Color.White // 9:1 sobre AzulLogo
val SobreAzulClaro = Color(0xFF0F2A55) // 9,2:1 sobre AzulClaro
val NaranjaTexto = Color(0xFF9A5200) // el naranja del logo oscurecido para texto sobre blanco (5,9:1)
val NaranjaContenedorClaro = Color(0xFFFFDDB8)
val SobreNaranjaContenedorClaro = Color(0xFF5C2E00) // 8,8:1
val FondoClaro = Color(0xFFF7F9FD)
val SuperficieClara = Color.White
val SuperficieContenedorClara = Color(0xFFD7E6FE) // barra inferior (elegido por Cristina; texto 7,4:1)
val PestanaActivaClara = Color(0xFFA6C5F7) // óvalo de la pestaña seleccionada
val TextoPestanaActivaClara = Color(0xFF1C3F7D) // 8,1:1 sobre la barra y 5,8:1 sobre el óvalo
val TextoClaro = Color(0xFF1A1C20)

// --- Tema oscuro (de noche en cabina) ---
val AzulNoche = Color(0xFF95B1DF) // borde de la tarjeta, horas y botón (elegido por Cristina; 7,8:1)
val SobreAzulNoche = Color(0xFF052048) // texto del botón, 7,4:1 sobre AzulNoche
val SobreAzulLogoNoche = Color(0xFFE6F0FD) // el claro de la esfera del icono, 7,8:1 sobre AzulLogo
val NaranjaNoche = Color(0xFFFFB95C) // el del nombre en la pantalla de carga
val SobreNaranjaNoche = Color(0xFF2E1500)
val NaranjaContenedorOscuro = Color(0xFF5A3300)
val SobreNaranjaContenedorOscuro = Color(0xFFFFDDB8) // 8,5:1
val FondoOscuro = Color(0xFF111318)
val SuperficieOscura = Color(0xFF1A1C20)
val SuperficieContenedorOscura = Color(0xFF22252B) // barra inferior
val PestanaActivaOscura = AzulLogo // óvalo en el azul del logo, como la barra de arriba
val IconoPestanaActivaOscura = SobreAzulLogoNoche // dentro del óvalo, 7,8:1
val TextoPestanaActivaOscura = Color(0xFFE5F1FF) // debajo, sobre la barra, 13,4:1
val TextoOscuro = Color(0xFFE2E2E9)

// --- Estados de las reglas (✅ ⚠️ ❔), comunes a ambos temas ---
val EstadoCumple = Color(0xFF2E7D32)
val EstadoExcedido = Color(0xFFC62828)
val EstadoNoVerificado = Color(0xFF757575)

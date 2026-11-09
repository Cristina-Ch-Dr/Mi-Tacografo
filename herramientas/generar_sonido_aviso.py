"""Genera `app/src/main/res/raw/aviso_jornada.wav`, el sonido de los avisos (MT-45).

Es una síntesis propia (sin grabaciones de nadie), así que no hay licencias de por medio.
Uso: python herramientas/generar_sonido_aviso.py app/src/main/res/raw/aviso_jornada.wav

Tres notas que suben (mi – la – mi aguda, como un «ding-ding-dong» de aviso), con armónicos
y caída exponencial, para que suene a campana y no a un mensaje de texto.
"""
import math
import struct
import sys
import wave

TASA = 22_050
NOTAS = [(659.25, 0.00), (880.00, 0.22), (1318.51, 0.44)]  # Hz, segundo en que empieza
DURACION = 1.35
ARMONICOS = [(1.0, 1.0), (2.0, 0.35), (3.0, 0.12), (4.2, 0.05)]  # múltiplo, volumen

muestras = []
for i in range(int(TASA * DURACION)):
    t = i / TASA
    valor = 0.0
    for frecuencia, empieza in NOTAS:
        dt = t - empieza
        if dt < 0:
            continue
        ataque = min(1.0, dt / 0.005)  # 5 ms para que no haga «clic»
        caida = math.exp(-dt * 4.0)
        for multiplo, volumen in ARMONICOS:
            valor += volumen * ataque * caida * math.sin(2 * math.pi * frecuencia * multiplo * dt)
    muestras.append(valor)

# Normaliza al 85 % para que suene fuerte sin saturar, y desvanece los últimos 50 ms.
pico = max(abs(v) for v in muestras)
fundido = int(TASA * 0.05)
for i in range(len(muestras)):
    factor = 0.85 / pico
    if i > len(muestras) - fundido:
        factor *= (len(muestras) - i) / fundido
    muestras[i] *= factor

with wave.open(sys.argv[1], 'wb') as wav:
    wav.setnchannels(1)
    wav.setsampwidth(2)
    wav.setframerate(TASA)
    wav.writeframes(b''.join(struct.pack('<h', int(v * 32767)) for v in muestras))
print('ok', len(muestras) / TASA, 's')

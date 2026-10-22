# DEDsafio Eclipse (Forge 1.20.1)

Mod para eventos tipo DEDsafio: eclipse con luna oscura y corona, oscuridad total, linterna con durabilidad y Herobrine.

## Comandos (necesitan nivel de operador)
| Comando | Qué hace |
|---|---|
| `/eclipse start [segundos]` | Aparece la luna y se acerca al sol (por defecto 120 s). El cielo se va poniendo gris y al llegar al eclipse queda oscurísimo. Pone la hora en mediodía y congela el ciclo día/noche mientras dura. |
| `/eclipse stop [segundos]` | Vuelve la luz poco a poco (por defecto 60 s) y devuelve la regla del ciclo de día. |
| `/eclipse estado` | Muestra el progreso. |
| `/eclipse bunker add <nombre> <radio>` | Marca un búnker (cubo) centrado en ti. |
| `/eclipse bunker addbox <nombre> <desde> <hasta>` | Marca un búnker con dos esquinas. |
| `/eclipse bunker remove <nombre>` / `list` | Quita / lista búnkeres. |
| `/eclipse herobrine spawn` / `clear` | Prueba: invoca / elimina Herobrines. |

## Reglas
- **Mirar el eclipse** (sin nada que tape el cielo) te prende fuego y te daña.
- **Linterna** (`dedsafio_eclipse:linterna`): clic derecho para encender/apagar. Gasta 1 de durabilidad por segundo encendida (1200 = 20 min). Tras apagarla hay 2 s de cooldown para volver a encenderla. Se repara con redstone en el yunque. Receta: vidrio, 3 lingotes de hierro y antorcha de redstone.
- **Herobrine** aparece con la oscuridad total, te mira quieto. Si lo alumbras se congela; si pasan **5 s sin alumbrarlo** te persigue. No aparece dentro de un búnker y desaparece si tú o él están en uno, o cuando termina el eclipse.

## Compilar
- Con GitHub: sube este repo y mira la pestaña **Actions**; el `.jar` sale en *Artifacts*.
- Local: Java 17 + Gradle 8.1.1 (`gradle build`), jar en `build/libs/`.

## Instalar
El `.jar` va en `mods/` **del servidor y de cada jugador** (Forge 1.20.1).

## Notas técnicas
- El cielo se dibuja con geometría (sin texturas), por eso se ve nítido a cualquier resolución.
- El mod incluye `data/minecraft/dimension_type/overworld.json` para enganchar el cielo del eclipse al Overworld.
- La oscuridad y el haz de la linterna son una capa en pantalla (no iluminación real de bloques).

# Auto Prestige (Fabric 1.21.11 + Mod Menu)

Que hace:
- Envia `/prestige`, espera a que se abra el cofre "Prestige" y hace clic en el boton verde (slot 27 por defecto). Cierra el menu y repite.
- Modo SCOREBOARD: lee `LEVEL` y `PRESTIGE` de la barra lateral y prestigia solo cuando nivel >= objetivo.
- Modo MANUAL: solo actua con la tecla "Hacer prestige ahora" o el boton del menu.
- Menu de configuracion en Mod Menu (boton de configurar del mod) y teclas en Opciones > Controles > Misc.
- Config guardada en `.minecraft/config/autoprestige.json`.

## Compilar (genera el .jar)
Requisitos: JDK 21 e internet (descarga Minecraft/Fabric).

1. Abre esta carpeta en IntelliJ IDEA (importa Gradle solo) o usa terminal.
2. Si no tienes gradlew: `gradle wrapper --gradle-version 9.2.1`
3. `./gradlew build`  (Windows: `gradlew.bat build`)
4. El jar queda en `build/libs/autoprestige-1.0.0.jar` (NO uses el `-sources.jar`).

Pon el jar en `.minecraft/mods` junto con Fabric API y Mod Menu.

Si Gradle dice que una version no existe (por ejemplo modmenu), cambia `gradle.properties` a la ultima para 1.21.11.

## Sin instalar nada (GitHub Actions)
1. Crea un repo en github.com y sube TODO el contenido de esta carpeta (incluida la carpeta oculta `.github`).
2. Ve a la pestana Actions > "Build jar" > espera ~3-5 min.
3. Entra a la ejecucion terminada y descarga el artefacto `autoprestige-jar` (zip con el .jar dentro).

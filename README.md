# NanoFiles

Sistema para compartir archivos entre clientes. Utiliza un directorio UDP para localizar archivos y servidores, y conexiones TCP para transferir los datos por bloques. Desarrollado con Java, sockets e hilos.

## Requisitos

- JDK 21.
- Bash para el script de compilación: disponible en macOS, Linux y Git Bash en Windows.

## Compilar

```bash
bash build.sh
```

## Probar en tu ordenador

Abre tres terminales en la carpeta del proyecto.

**1. Inicia el directorio:**

```bash
java -jar build/Directory.jar
```

**2. Prepara un archivo e inicia el primer cliente:**

```bash
mkdir -p shared-a
cp examples/sample.txt shared-a/
java -jar build/Nanofiles.jar shared-a
```

Responde `y` para usar `localhost` y escribe:

```text
ping
serve
```

Deja este cliente abierto.

**3. Inicia el segundo cliente:**

```bash
java -jar build/Nanofiles.jar shared-b
```

Responde `y` y escribe:

```text
ping
filelist
download sample.txt copia.txt
```

El archivo recibido estará en `shared-b/copia.txt`. Puedes usar `help` para ver los comandos y `quit` para cerrar cada cliente. Detén el directorio con `Ctrl+C`.

También puedes subir archivos con `upload <archivo> <IP:puerto>`; el comando `servers` muestra los servidores disponibles.

Los archivos vacíos no se incluyen en el catálogo. La versión publicada utiliza el identificador de protocolo `NANOFILES_V1`; usa el directorio y los clientes compilados de esta misma versión.

## Autor

Arsenii Gladkykh — [arsenii-g](https://github.com/arsenii-g).

La atribución del código base se conserva en [NOTICE.md](NOTICE.md).

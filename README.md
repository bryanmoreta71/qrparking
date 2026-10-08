# QR Parking

App Android (Kotlin) para validar tickets de parqueo escaneando el QR en lugar de digitar el código a mano.

## Cómo funciona

1. La app abre la página del centro comercial dentro de un navegador integrado (WebView).
   Inicias sesión una vez; las cookies se guardan, así que normalmente la sesión se mantiene.
2. Vas a la pantalla donde se ingresa el código del ticket (si quieres, toca el campo de texto).
3. Pulsas **Escanear QR**, apuntas al ticket y el código se escribe automáticamente en el campo.
4. Opcionalmente la app puede enviar el formulario sola (como si pulsaras "Enter").

## Configuración (menú ⋮ → Configuración)

| Opción | Para qué sirve |
| --- | --- |
| URL de la página | La página del centro comercial que se abre al iniciar. |
| Selector CSS del campo | Opcional. Ej. `#codigo` o `input[name=ticket]`. Si está vacío se usa el campo que tocaste por última vez o el primer campo de texto visible. |
| Usar solo los números del QR | Por si el QR trae texto extra (por ejemplo una URL) y la página solo acepta los dígitos. |
| Enviar automáticamente | Después de escribir el código, envía el formulario. |

## Compilar

- **Android Studio:** abre la carpeta del proyecto y ejecuta en tu teléfono.
- **Línea de comandos:** `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.
- **GitHub Actions:** cada push genera el APK; descárgalo desde la pestaña *Actions* → último run → *Artifacts*.

Requisitos: Android 7.0+ con Google Play Services (el escáner usa el lector de códigos de Google,
que no necesita pedir permiso de cámara).

## Limitaciones

- Si el campo está dentro de un iframe de otro dominio, la app no puede escribir en él.
- Si la página tiene un CAPTCHA o un teclado propio en pantalla, puede que haya que ajustar el script.

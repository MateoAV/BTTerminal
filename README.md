# BTTerminal - Aplicación Terminal Bluetooth

BTTerminal es una aplicación Android que permite comunicarse con dispositivos Bluetooth a través de una interfaz de terminal simple. Soporta dispositivos Bluetooth reales e incluye un dispositivo simulado para pruebas.

## Características

- Escaneo y descubrimiento de dispositivos Bluetooth
- Conexión a dispositivos Bluetooth usando Serial Port Profile (SPP)
- Envío y recepción de mensajes con finales de línea configurables (None, CR, LF, CR+LF)
- Soporte de dispositivo simulado para pruebas sin hardware real
- Visualización de mensajes en tiempo real con marcas de tiempo
- Soporte para el modelo de permisos Bluetooth de Android

## Configuración de Finales de Línea (CRLF)

La configuración de finales de línea es crucial para la comunicación con dispositivos Bluetooth, especialmente con microcontroladores como el ESP32:

- **None**: Sin caracteres adicionales
- **CR** (\r): Retorno de carro
- **LF** (\n): Nueva línea
- **CR+LF** (\r\n): Retorno de carro + Nueva línea

Para ESP32:
- Se recomienda usar **CR+LF** cuando se trabaja con la función `Serial.println()` del ESP32
- Usar **None** cuando se envían comandos específicos o datos binarios
- Si los mensajes aparecen en líneas incorrectas en el monitor, pruebe cambiando entre LF y CR+LF

## Conexión con ESP32

### Configuración del ESP32

1. Asegúrese que su ESP32 tenga el siguiente código base:

```cpp
#include "BluetoothSerial.h"

BluetoothSerial SerialBT;

void setup() {
    Serial.begin(115200);
    SerialBT.begin("ESP32-BT"); // Nombre que aparecerá en el escaneo
    
    // Ejemplo de comando LED
    pinMode(2, OUTPUT);
}

void loop() {
    if (SerialBT.available()) {
        String command = SerialBT.readStringUntil('\n');
        command.trim();
        
        if (command == "1") {
            digitalWrite(2, HIGH);
            SerialBT.println("Led1_On");
        }
        else if (command == "0") {
            digitalWrite(2, LOW);
            SerialBT.println("Led1_Off");
        }
    }
}
```

2. Verifique que el ESP32 aparezca en el escaneo de dispositivos
3. El nombre por defecto será "ESP32-BT"

## Requisitos

- Android Studio Hedgehog (2023.1.1) o más reciente
- Android SDK con nivel de API mínimo 24 (Android 7.0)
- Un dispositivo Android físico con soporte Bluetooth
- (Opcional) Un dispositivo Bluetooth que soporte el perfil SPP

## Construcción y Ejecución de la Aplicación

### Usando Android Studio

1. Clone el repositorio
2. Abra Android Studio
3. Seleccione "Abrir un proyecto existente"
4. Navegue al directorio del proyecto y haga clic en "OK"
5. Espere a que se complete la sincronización de Gradle
6. Conecte su dispositivo Android o inicie un emulador
7. Haga clic en el botón "Run" (ícono verde de play) o presione Shift + F10

### Ejecución en un Dispositivo Físico

#### Habilitar Opciones de Desarrollador

1. Vaya a Configuración de su dispositivo Android
2. Desplácese hasta "Acerca del teléfono"
3. Encuentre "Número de compilación" (generalmente al final)
4. Toque "Número de compilación" siete veces
5. Verá un mensaje diciendo "¡Ya eres un desarrollador!"
6. Regrese a Configuración
7. Ahora verá "Opciones de desarrollador" en la configuración del sistema

#### Habilitar Depuración USB

1. Vaya a Configuración > Opciones de desarrollador
2. Active "Depuración USB"
3. Conecte su dispositivo a la computadora vía USB
4. Acepte el mensaje de autorización de depuración USB en su dispositivo

#### Usar Depuración Inalámbrica (Android 11+)

1. Habilite Opciones de desarrollador (ver arriba)
2. Vaya a Opciones de desarrollador
3. Active "Depuración inalámbrica"
4. En su dispositivo Android:
   - Vaya a Configuración > Opciones de desarrollador > Depuración inalámbrica
   - Toque "Emparejar dispositivo con código QR" o "Emparejar dispositivo con código"
5. En Android Studio:
   - Vaya a "Device Manager"
   - Haga clic en "Pair using Wi-Fi"
   - Siga las instrucciones de emparejamiento
6. Una vez emparejado, su dispositivo aparecerá en la lista de selección de dispositivos

## Uso de la Aplicación

1. Inicie la aplicación
2. Conceda los permisos requeridos cuando se soliciten:
   - Permisos de ubicación (requeridos para escaneo Bluetooth)
   - Permisos de Bluetooth
3. Haga clic en "Scan Devices" para descubrir dispositivos Bluetooth cercanos
4. Seleccione un dispositivo de la lista para conectar
   - Los dispositivos reales aparecerán con sus nombres/direcciones
   - Un dispositivo simulado (TEST DEVICE) está disponible para pruebas

### Pruebas con Dispositivo Simulado

La aplicación incluye un dispositivo simulado para pruebas sin hardware real:
- Nombre: "TEST DEVICE"
- Envía respuestas predefinidas:
  - Envíe "1" para recibir "Led1_On"
  - Envíe "0" para recibir "Led1_Off"
  - Otros mensajes serán devueltos como eco

### Características del Terminal

- Campo de entrada de mensaje en la parte inferior
- Botón de envío para transmitir mensajes
- Selector de final de línea (None, CR, LF, CR+LF)
- Visualización de mensajes en tiempo real con marcas de tiempo
- Mensajes enviados tienen prefijo "→"
- Mensajes recibidos tienen prefijo "←"

## Solución de Problemas

1. Bluetooth No Disponible
   - Asegúrese que el Bluetooth esté activado en su dispositivo
   - Verifique si su dispositivo soporta Bluetooth

2. Problemas de Permisos
   - Conceda todos los permisos solicitados
   - Para Android 12+, asegúrese que los permisos de escaneo y conexión Bluetooth estén concedidos

3. Problemas de Conexión
   - Asegúrese que el dispositivo Bluetooth esté encendido y en rango
   - Verifique si el dispositivo soporta el perfil SPP
   - Intente reiniciar tanto la aplicación como el dispositivo Bluetooth
   - Para ESP32, verifique que el nombre del dispositivo sea correcto y esté en modo visible

4. Depuración Inalámbrica No Funciona
   - Asegúrese que ambos dispositivos estén en la misma red
   - Intente reiniciar Android Studio
   - Verifique la configuración del firewall

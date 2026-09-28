# DOCUMENTACIÓN TÉCNICA Y MANUAL DIGITAL DEL SISTEMA POS (POLLO POS)
**Proyecto:** Pollo POS - Sistema de Punto de Venta Móvil  
**Versión:** 2.0.0 (Android SDK 31 a 37)  
**Arquitectura:** MVVM (Model-View-ViewModel) + Offline-First con Room SQLite  

---

## 1. INTRODUCCIÓN Y ARQUITECTURA GENERAL

El sistema **Pollo POS** es una solución integral de punto de venta diseñada para pollerías y restaurantes de comida rápida. Su propósito es optimizar la atención en mesa y para llevar, la emisión de comandas a cocina, la cobranza multimétodo (efectivo, tarjeta, QR y mixto), la impresión de tickets térmicos y el control de turnos de caja con arqueo ciego.

### Componentes Arquitectónicos Clave:
* **Persistencia:** Base de datos relacional local con SQLite gestionada a través de **Android Jetpack Room** (10 entidades relacionales).
* **Hardware & Periféricos:** Integración con **Cámara de fotos** (mediante `FileProvider` y pantalla de recorte personalizada) e **Impresoras Térmicas ESC/POS** (mediante Bluetooth Serial Port Profile - SPP).
* **Consumo de APIs:** Arquitectura *Offline-First* con APIs Nativas de Android (Bluetooth SPP API, Camera Intent API, Android Print Framework API y Room Persistence API).

---

## 2. DOCUMENTACIÓN DE CONSUMO DE APIS Y HARDWARE

### 2.1. API de Cámara y FileProvider (Android Camera API)
* **Objetivo:** Permitir la toma fotográfica de nuevos platos para el catálogo de productos y fotos de perfil de cajeros.
* **Componentes involucrados:** `GestionProductosActivity.java`, `CropImageActivity.java`, `AndroidManifest.xml`, `res/xml/file_paths.xml`.
* **Consumo del API:**
  Se utiliza el contrato de Android `ActivityResultContracts.TakePicture()` junto con un `FileProvider` seguro para evitar excepciones `FileUriExposedException` en Android 12+.

```java
// Inicialización del FileProvider seguro y lanzamiento del API de Cámara
File dir = new File(getCacheDir(), "camera");
if (!dir.exists()) dir.mkdirs();
File tempFile = new File(dir, "cam_" + System.currentTimeMillis() + ".jpg");

// Obtención de URI segura mediante FileProvider
cameraTempUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", tempFile);

// Lanzamiento del contrato del sistema
takePictureLauncher.launch(cameraTempUri);
```

* **Flujo posterior a la captura:**
  Una vez tomada la foto, la aplicación navega hacia `CropImageActivity` enviando la URI temporal para que el usuario recorte el plato en formato 1:1 antes de guardarlo en el almacenamiento interno de la app.

---

### 2.2. API de Impresión Térmica Bluetooth (ESC/POS vía Bluetooth SPP)
* **Objetivo:** Emisión física e inalámbrica de comandas para cocina y tickets fiscales/recibos de venta al cliente.
* **Clase controladora:** `com.example.pollogithub.util.ThermalPrinterManager.java` y `EscPosTicketBuilder.java`.
* **UUID del Servicio SPP:** `00001101-0000-1000-8000-00805F9B34FB` (Serial Port Profile estándar).
* **Lógica de Conexión y Consumo de la API:**

```java
// Obtención del adaptador Bluetooth y dispositivo emparejado por MAC
BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
BluetoothDevice device = adapter.getRemoteDevice(printerMacAddress);

// Apertura del socket RFCOMM para comunicación serie con la impresora
BluetoothSocket socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
socket.connect(); // Establece enlace con la impresora térmica

// Envío del flujo de bytes con comandos ESC/POS (corte de papel, texto en negrita, etc.)
OutputStream outputStream = socket.getOutputStream();
outputStream.write(escPosBytes);
outputStream.flush();
socket.close();
```

---

### 2.3. API del Android Print Framework (Impresión del Sistema y PDF)
* **Objetivo:** Brindar contingencia digital cuando no se cuente con una impresora Bluetooth física, permitiendo guardar el recibo como PDF o imprimirlo en red.
* **Consumo del API:** En `ReciboActivity.java`:
```java
PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
PrintDocumentAdapter printAdapter = new PdfDocumentAdapter(this, pdfFile);
printManager.print("Recibo_" + pedidoId, printAdapter, new PrintAttributes.Builder().build());
```

---

### 2.4. Aclaración de APIs REST / HTTP
El proyecto utiliza una estrategia **Offline-First**. No depende de llamadas HTTP a un servidor externo para funcionar día a día; todas las transacciones de ventas, turnos e inventario se realizan de manera transaccional e inmediata en la base de datos SQLite local mediante **Room**, garantizando que el restaurante nunca se detenga por fallas en la conexión a Internet.

---

## 3. BASE DE DATOS LOCAL (ROOM / SQLITE)

El sistema cuenta con una base de datos relacional compuesta por **10 tablas**:
1. **`usuarios` (`UsuarioEntity`):** Administradores, cajeros y personal de cocina con contraseña encriptada y PIN de acceso rápido.
2. **`roles` (`RolEntity`):** Niveles de permisos dentro de la aplicación.
3. **`sucursales` (`SucursalEntity`):** Datos del local, dirección y teléfono para el encabezado del ticket.
4. **`turnos` (`TurnoEntity`):** Control de apertura y cierre de caja, fondo inicial, total esperado, contado real y diferencias.
5. **`categorias` (`CategoriaEntity`):** Clasificación del menú (Pollo Frito, A la Brasa, Combos, Bebidas, Acompañamientos).
6. **`productos` (`ProductoEntity`):** Platos disponibles, precios, emojis, recursos visuales y rutas de imágenes locales de cámara.
7. **`pedidos` (`PedidoEntity`):** Ventas registradas, número de orden consecutivo, tipo de entrega (mesa/llevar) y estados de cocina.
8. **`pedido_detalles` (`PedidoDetalleEntity`):** Desglose de cada producto dentro de un pedido con cantidades, precios unitarios y notas personalizadas.
9. **`pagos` (`PagoEntity`):** Registro de pagos en efectivo, tarjeta, QR o pagos mixtos con cálculo automático de vuelto.
10. **`movimientos_caja` (`MovimientoCajaEntity`):** Control de entradas y salidas menores de dinero durante el turno.

---

## 4. DOCUMENTACIÓN DETALLADA PANTALLA POR PANTALLA

A continuación se detalla cada pantalla del sistema, su propósito, textos visuales, layout XML, bloque para captura de pantalla y **exclusivamente las líneas de código Java que ejecutan la navegación a la siguiente pantalla con su explicación línea por línea**.

---

### PANTALLA 1: Login / Inicio de Sesión (`MainActivity`)

#### A. Propósito y Funcionamiento
Pantalla de bienvenida y control de acceso. Permite al usuario iniciar sesión mediante nombre de usuario y contraseña, o mediante un PIN rápido de 4 dígitos. Además, valida si el usuario ya tiene un turno abierto previamente para dirigirlo directo a ventas o a la apertura de caja.

#### B. Textos Visibles en Pantalla
* **Encabezado:** "POLLO POS", "Sistema de Punto de Venta"
* **Título:** "Ingreso al Sistema"
* **Subtítulo:** "Ingresa tus credenciales o PIN para iniciar turno"
* **Campos:** "Usuario o PIN", "Contraseña"
* **Enlace:** "¿Olvidaste tu contraseña o PIN?"
* **Botón Principal:** "INICIAR TURNO"

#### C. Captura de Pantalla
> *[PEGAR AQUÍ CAPTURA DE PANTALLA DE: MainActivity - activity_main.xml]*

#### D. Código del Layout XML (Extracto clave: `activity_main.xml`)
```xml
<!-- Botón para alternar visibilidad de contraseña -->
<ImageButton
    android:id="@+id/btnToggleEye"
    android:layout_width="48dp"
    android:layout_height="48dp"
    android:background="?attr/selectableItemBackgroundBorderless"
    android:src="@drawable/ic_eye" />

<!-- Enlace a pantalla de recuperación de contraseña -->
<TextView
    android:id="@+id/tvForgotPin"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="¿Olvidaste tu contraseña o PIN?"
    android:textColor="@color/ember_600" />

<!-- Botón de autenticación y navegación principal -->
<com.google.android.material.button.MaterialButton
    android:id="@+id/btnStartShift"
    android:layout_width="match_parent"
    android:layout_height="56dp"
    android:text="INICIAR TURNO"
    app:cornerRadius="14dp" />
```

#### E. Lógica Java de Navegación (`MainActivity.java`)
```java
// Caso 1: Navegación hacia recuperación de credenciales
findViewById(R.id.tvForgotPin).setOnClickListener(v -> {
    Intent intent = new Intent(MainActivity.this, RecuperarPasswordActivity.class);
    startActivity(intent);
});

// Caso 2: Login exitoso con turno previamente abierto -> Navega al Home
loginViewModel.getLoginSuccess().observe(this, usuario -> {
    Intent intent = new Intent(MainActivity.this, HomeActivity.class);
    intent.putExtra("USER_NAME", usuario.getNombreCompleto());
    startActivity(intent);
    finish();
});

// Caso 3: Login exitoso sin turno activo -> Navega a Apertura de Caja obligatoria
loginViewModel.getNeedsTurnoApertura().observe(this, needs -> {
    if (Boolean.TRUE.equals(needs)) {
        Intent intent = new Intent(MainActivity.this, AperturaCajaActivity.class);
        intent.putExtra("USER_NAME", loginViewModel.getLastUsuario().getNombreCompleto());
        intent.putExtra("USER_ID", loginViewModel.getLastUsuario().getId());
        intent.putExtra("SUCURSAL_ID", loginViewModel.getLastUsuario().getSucursalId());
        startActivity(intent);
        finish();
    }
});
```

#### F. Explicación Línea por Línea del Código de Navegación:
1. `findViewById(R.id.tvForgotPin).setOnClickListener(...)`: Asigna un evento de clic sobre el texto de recuperación de contraseña.
2. `Intent intent = new Intent(MainActivity.this, RecuperarPasswordActivity.class);`: Crea la intención explícita para pasar de `MainActivity` hacia `RecuperarPasswordActivity`.
3. `startActivity(intent);`: Ejecuta la transición para abrir la ventana de recuperación.
4. `loginViewModel.getLoginSuccess().observe(...)`: Escucha de manera reactiva el evento de autenticación exitosa emitido por el ViewModel.
5. `Intent intent = new Intent(MainActivity.this, HomeActivity.class);`: Instancia la navegación al panel principal (`HomeActivity`).
6. `intent.putExtra("USER_NAME", usuario.getNombreCompleto());`: Envía como parámetro extra el nombre completo del cajero autenticado.
7. `startActivity(intent);`: Inicia la actividad `HomeActivity`.
8. `finish();`: Cierra y destruye `MainActivity` del historial para que el usuario no regrese al login si presiona atrás.
9. `loginViewModel.getNeedsTurnoApertura().observe(...)`: Observa si el usuario requiere realizar la apertura de turno con fondo inicial.
10. `Intent intent = new Intent(MainActivity.this, AperturaCajaActivity.class);`: Configura el salto hacia `AperturaCajaActivity`.
11. `intent.putExtra(...)`: Adjunta los identificadores de usuario y sucursal necesarios para registrar el nuevo turno.
12. `startActivity(intent); finish();`: Ejecuta la navegación y finaliza el login.

---

### PANTALLA 2: Recuperación de Contraseña (`RecuperarPasswordActivity`)

#### A. Propósito y Funcionamiento
Permite a los usuarios solicitar un código de restablecimiento de contraseña o acceder a los créditos secretos del sistema mediante la clave especial.

#### B. Textos Visibles en Pantalla
* **Botón Regresar:** "←"
* **Título:** "Recuperar Acceso"
* **Descripción:** "Ingresa tu usuario para recibir instrucciones de recuperación"
* **Campo:** "Usuario registrado"
* **Botones:** "ENVIAR CÓDIGO", "Volver al Login"

#### C. Captura de Pantalla
> *[PEGAR AQUÍ CAPTURA DE PANTALLA DE: RecuperarPasswordActivity - activity_recuperar_password.xml]*

#### D. Código del Layout XML (Extracto clave: `activity_recuperar_password.xml`)
```xml
<ImageButton
    android:id="@+id/btnBack"
    android:layout_width="44dp"
    android:layout_height="44dp"
    android:src="@drawable/ic_back" />

<com.google.android.material.button.MaterialButton
    android:id="@+id/btnSendCode"
    android:layout_width="match_parent"
    android:layout_height="56dp"
    android:text="ENVIAR CÓDIGO" />

<TextView
    android:id="@+id/tvLogin"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Volver al Login" />
```

#### E. Lógica Java de Navegación (`RecuperarPasswordActivity.java`)
```java
// Navegación de retorno al Login
findViewById(R.id.btnBack).setOnClickListener(v -> finish());
findViewById(R.id.tvLogin).setOnClickListener(v -> finish());

// Navegación condicional hacia la pantalla de Autores / Créditos
findViewById(R.id.btnSendCode).setOnClickListener(v -> {
    String user = etUser.getText() != null ? etUser.getText().toString().trim() : "";
    if ("autores.69".equalsIgnoreCase(user)) {
        Intent intent = new Intent(RecuperarPasswordActivity.this, AutoresActivity.class);
        startActivity(intent);
        return;
    }
    Toast.makeText(this, "Código enviado al usuario", Toast.LENGTH_SHORT).show();
    finish();
});
```

#### F. Explicación Línea por Línea del Código de Navegación:
1. `findViewById(R.id.btnBack).setOnClickListener(v -> finish());`: Al tocar el botón de retroceso, invoca `finish()` destruyendo la actividad y devolviendo al usuario a `MainActivity`.
2. `findViewById(R.id.tvLogin).setOnClickListener(v -> finish());`: El texto "Volver al Login" también llama a `finish()` para regresar.
3. `if ("autores.69".equalsIgnoreCase(user))`: Valida si el texto ingresado coincide con el código secreto del equipo de desarrollo.
4. `Intent intent = new Intent(RecuperarPasswordActivity.this, AutoresActivity.class);`: Crea el intent hacia `AutoresActivity`.
5. `startActivity(intent); return;`: Abre la pantalla de autores y detiene el flujo.
6. `finish();`: En caso de ser un usuario estándar, envía mensaje informativo y regresa a la pantalla de login.

---

### PANTALLA 3: Apertura de Caja / Turno (`AperturaCajaActivity`)

#### A. Propósito y Funcionamiento
Obliga al cajero a ingresar el fondo inicial (caja chica) antes de comenzar a registrar ventas. Guarda el nuevo registro en la tabla `turnos` de Room y almacena el `turnoId` en `SessionManager`.

#### B. Textos Visibles en Pantalla
* **Título:** "Apertura de Turno"
* **Subtítulo:** "Ingresa el fondo inicial de caja chica para comenzar"
* **Campo de texto:** "Fondo Inicial (Bs.)"
* **Botones de monto rápido:** "Bs. 50", "Bs. 100", "Bs. 200"
* **Botón Principal:** "ABRIR TURNO Y COMENZAR"

#### C. Captura de Pantalla
> *[PEGAR AQUÍ CAPTURA DE PANTALLA DE: AperturaCajaActivity - activity_apertura_caja.xml]*

#### D. Código del Layout XML (Extracto clave: `activity_apertura_caja.xml`)
```xml
<EditText
    android:id="@+id/etFondoInicial"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="0.00"
    android:inputType="numberDecimal" />

<com.google.android.material.button.MaterialButton
    android:id="@+id/btnAbrirTurno"
    android:layout_width="match_parent"
    android:layout_height="56dp"
    android:text="ABRIR TURNO Y COMENZAR"
    app:cornerRadius="14dp" />
```

#### E. Lógica Java de Navegación (`AperturaCajaActivity.java`)
```java
repository.abrirTurno(finalFondo, userId, sucursalId, new PosRepository.Callback<TurnoEntity>() {
    @Override
    public void onSuccess(TurnoEntity turno) {
        // Guarda el ID del turno en la sesión
        repository.getSessionManager().setTurnoId(turno.getId());

        // Navegación hacia el dashboard principal
        Intent intent = new Intent(AperturaCajaActivity.this, HomeActivity.class);
        intent.putExtra("USER_NAME", userName);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onError(String error) {
        Toast.makeText(AperturaCajaActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
    }
});
```

#### F. Explicación Línea por Línea del Código de Navegación:
1. `repository.abrirTurno(...)`: Registra asíncronamente en SQLite la apertura del turno con el fondo monetario.
2. `repository.getSessionManager().setTurnoId(turno.getId());`: Actualiza la sesión local con el ID generado por Room.
3. `Intent intent = new Intent(AperturaCajaActivity.this, HomeActivity.class);`: Prepara el salto hacia `HomeActivity`.
4. `intent.putExtra("USER_NAME", userName);`: Pasa el nombre del operador para mostrarlo en el encabezado.
5. `intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);`: Limpia las pantallas anteriores de la pila para prevenir inconsistencias de caja.
6. `startActivity(intent); finish();`: Despacha la navegación y cierra la pantalla de apertura.

---

### PANTALLA 4: Terminal de Ventas y POS (`HomeActivity` -> `VentaFragment`)

#### A. Propósito y Funcionamiento
Pantalla neurálgica de ventas. Permite seleccionar modo "En Mesa" o "Para Llevar", filtrar productos por chips de categorías, agregar o restar ítems al carrito, abrir el diálogo de notas especiales (ej. "Sin mayonesa") y abrir la barra flotante de checkout.

#### B. Textos Visibles en Pantalla
* **Encabezado:** Saludo al cajero, modos: "🍽️ En Mesa", "🛍️ Para Llevar"
* **Chips de Categorías:** "Todos", "🍗 Pollo Frito", "🔥 A la Brasa", "🍔 Combos", "🍟 Acompañamientos", "🥤 Bebidas"
* **Tarjetas de Producto:** Nombre, descripción breve, precio ("Bs. XX.XX"), botón "+" y controles de cantidad "-" y "+"
* **Barra Flotante del Carrito:** "X ítems", "Total: Bs. XX.XX", botón "CONFIRMAR PEDIDO"

#### C. Captura de Pantalla
> *[PEGAR AQUÍ CAPTURA DE PANTALLA DE: VentaFragment - fragment_venta.xml]*

#### D. Código del Layout XML (Extracto clave: `fragment_venta.xml`)
```xml
<!-- Barra inferior flotante de pedido y confirmación -->
<androidx.cardview.widget.CardView
    android:id="@+id/cartBar"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:visibility="gone">

    <TextView
        android:id="@+id/tvCartCount"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="0 ítems" />

    <TextView
        android:id="@+id/tvCartTotal"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Bs. 0.00" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/btnCheckout"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Cobrar" />
</androidx.cardview.widget.CardView>
```

#### E. Lógica Java de Navegación (`VentaFragment.java`)
```java
private void procederAlPago(String tipoEntrega, Integer mesaId) {
    ventaViewModel.confirmarPedido(tipoEntrega, null, new PosRepository.Callback<PedidoEntity>() {
        @Override
        public void onSuccess(PedidoEntity pedido) {
            Intent intent = new Intent(requireContext(), PagoActivity.class);
            intent.putExtra("PEDIDO_ID", pedido.getId());
            intent.putExtra("ORDER_NUMBER", pedido.getNumeroOrden());
            intent.putExtra("TOTAL_AMOUNT", pedido.getTotal());
            intent.putExtra("TIPO_ENTREGA", tipoEntrega);
            startActivity(intent);
        }

        @Override
        public void onError(String error) {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
        }
    });
}
```

#### F. Explicación Línea por Línea del Código de Navegación:
1. `ventaViewModel.confirmarPedido(...)`: Guarda el pedido en estado inicial dentro de la base de datos Room.
2. `Intent intent = new Intent(requireContext(), PagoActivity.class);`: Prepara la transición hacia la pasarela de cobro `PagoActivity`.
3. `intent.putExtra("PEDIDO_ID", pedido.getId());`: Adjunta el ID primario del pedido generado en SQLite.
4. `intent.putExtra("ORDER_NUMBER", pedido.getNumeroOrden());`: Adjunta el número consecutivo de orden (ej. #0012).
5. `intent.putExtra("TOTAL_AMOUNT", pedido.getTotal());`: Pasa el monto monetario total a cobrar.
6. `intent.putExtra("TIPO_ENTREGA", tipoEntrega);`: Pasa el tipo de servicio ("mesa" o "para_llevar").
7. `startActivity(intent);`: Inicia la actividad `PagoActivity`.

---

### PANTALLA 5: Pasarela de Cobro y Métodos de Pago (`PagoActivity`)

#### A. Propósito y Funcionamiento
Gestiona la liquidación monetaria de la orden. Permite seleccionar método de pago (Efectivo, Tarjeta de crédito/débito, Código QR o Pago Mixto), ingresar el efectivo recibido con cálculo automático de vuelto en tiempo real, o aplicar cupones de descuento. Al confirmar el pago, asienta el cobro en Room y pasa a la pantalla de recibo.

#### B. Textos Visibles en Pantalla
* **Encabezado:** "Cobro de Pedido", "Orden #XXX"
* **Total a Cobrar:** "Bs. XX.XX"
* **Métodos de Pago:** "💵 Efectivo", "💳 Tarjeta", "📱 Pago QR", "🔀 Mixto"
* **Sección Efectivo:** "Monto recibido", botones rápidos ("Exacto", "+Bs. 10", "+Bs. 20", "+Bs. 50")
* **Resultado:** "Vuelto a entregar: Bs. XX.XX"
* **Botón Principal:** "CONFIRMAR PAGO (Bs. XX.XX)"

#### C. Captura de Pantalla
> *[PEGAR AQUÍ CAPTURA DE PANTALLA DE: PagoActivity - activity_pago.xml]*

#### D. Código del Layout XML (Extracto clave: `activity_pago.xml`)
```xml
<!-- Botón para regresar al pedido anterior -->
<ImageButton
    android:id="@+id/btnBack"
    android:layout_width="44dp"
    android:layout_height="44dp"
    android:src="@drawable/ic_back" />

<!-- Botón de acción principal para confirmar cobro -->
<com.google.android.material.button.MaterialButton
    android:id="@+id/btnConfirmarPago"
    android:layout_width="match_parent"
    android:layout_height="56dp"
    android:text="CONFIRMAR PAGO"
    app:cornerRadius="14dp" />
```

#### E. Lógica Java de Navegación (`PagoActivity.java`)
```java
// Retorno al terminal de ventas sin cobrar
findViewById(R.id.btnBack).setOnClickListener(v -> finish());

// Transición a la emisión de recibo tras procesar el cobro
pagoViewModel.registrarCobro(pago, () -> {
    Intent intent = new Intent(PagoActivity.this, ReciboActivity.class);
    intent.putExtra("PEDIDO_ID", pedidoId);
    intent.putExtra("ORDER_NUMBER", orderNumber);
    intent.putExtra("TOTAL_AMOUNT", totalAmount);
    intent.putExtra("METODO_PAGO", metodoSeleccionado);
    intent.putExtra("MONTO_RECIBIDO", montoRecibido);
    intent.putExtra("VUELTO", vueltoCalculado);
    startActivity(intent);
    finish();
});
```

#### F. Explicación Línea por Línea del Código de Navegación:
1. `findViewById(R.id.btnBack).setOnClickListener(v -> finish());`: Permite cancelar el cobro y regresar al carrito con `finish()`.
2. `pagoViewModel.registrarCobro(...)`: Registra el pago en SQLite e inserta la tupla en la tabla `pagos`.
3. `Intent intent = new Intent(PagoActivity.this, ReciboActivity.class);`: Construye la intención hacia `ReciboActivity`.
4. `intent.putExtra("PEDIDO_ID", pedidoId);` ...: Empaqueta los datos fiscales y financieros del cobro (monto, método, recibido y vuelto).
5. `startActivity(intent);`: Lanza la pantalla de recibo y comprobante digital.
6. `finish();`: Finaliza `PagoActivity` para impedir pagos duplicados de la misma orden.

---

### PANTALLA 6: Comprobante y Recibo Fiscal (`ReciboActivity`)

#### A. Propósito y Funcionamiento
Muestra el ticket digital detallado con el logo, dirección, lista de ítems, precios, impuestos, método de pago y vuelto. Ofrece las opciones de **Imprimir Ticket Térmico por Bluetooth ESC/POS**, **Compartir por WhatsApp/PDF** y **Comenzar Nueva Venta**.

#### B. Textos Visibles en Pantalla
* **Encabezado del Ticket:** "POLLO SABROSO S.A.", "Sucursal Central", "Orden #XXX"
* **Cuerpo:** Lista de platos, cantidades y subtotales
* **Desglose:** "Subtotal", "Descuento", "TOTAL", "Método", "Vuelto"
* **Botones:** "🖨️ IMPRIMIR TICKET", "📤 COMPARTIR", "NUEVA VENTA"

#### C. Captura de Pantalla
> *[PEGAR AQUÍ CAPTURA DE PANTALLA DE: ReciboActivity - activity_recibo.xml]*

#### D. Código del Layout XML (Extracto clave: `activity_recibo.xml`)
```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btnPrintTicket"
    android:layout_width="0dp"
    android:layout_height="52dp"
    android:text="IMPRIMIR"
    app:icon="@drawable/ic_printer" />

<com.google.android.material.button.MaterialButton
    android:id="@+id/btnNuevaVenta"
    android:layout_width="match_parent"
    android:layout_height="56dp"
    android:text="NUEVA VENTA"
    app:cornerRadius="14dp" />
```

#### E. Lógica Java de Navegación (`ReciboActivity.java`)
```java
// Botón para concluir el ciclo y volver al panel principal
findViewById(R.id.btnNuevaVenta).setOnClickListener(v -> {
    Intent intent = new Intent(ReciboActivity.this, HomeActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    startActivity(intent);
    finish();
});
```

#### F. Explicación Línea por Línea del Código de Navegación:
1. `findViewById(R.id.btnNuevaVenta).setOnClickListener(...)`: Asigna la acción al botón de nueva venta.
2. `Intent intent = new Intent(ReciboActivity.this, HomeActivity.class);`: Define el regreso a `HomeActivity`.
3. `intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);`: Limpia cualquier pantalla intermedia (como `PagoActivity`) y reutiliza la instancia existente de `HomeActivity`.
4. `startActivity(intent);`: Navega de retorno al inicio del POS.
5. `finish();`: Cierra el recibo.

---

### PANTALLA 7: Monitor de Cocina y Comandas KDS (`HomeActivity` -> `PedidosFragment`)

#### A. Propósito y Funcionamiento
Sistema KDS (Kitchen Display System) para el área de preparación. Muestra los pedidos en curso organizados por tarjetas con tiempo transcurrido y botón para avanzar el estado: **Pendiente → En Preparación → Listo → Entregado**.

#### B. Textos Visibles en Pantalla
* **Título:** "Monitor de Cocina & Pedidos"
* **Filtros por Estado:** "Todos", "⏳ Pendientes", "🍳 En Cocina", "✅ Listos", "📦 Entregados"
* **Tarjetas:** Número de orden, tiempo, productos y notas (ej. "Sin picante"), botón de cambio de estado.

#### C. Captura de Pantalla
> *[PEGAR AQUÍ CAPTURA DE PANTALLA DE: PedidosFragment - fragment_pedidos.xml]*

#### D. Código del Layout XML (Extracto clave: `fragment_pedidos.xml`)
```xml
<androidx.recyclerview.widget.RecyclerView
    android:id="@+id/rvOrders"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:clipToPadding="false"
    android:padding="16dp" />
```

#### E. Lógica Java de Navegación (`PedidosFragment.java`)
Esta pantalla opera como componente central del dashboard interactuando mediante diálogos modales (`showDetallePedidoDialog`) y actualizando en tiempo real la base de datos sin destruir la vista:
```java
// Apertura de detalles del pedido y actualización de estado
private void showDetallePedidoDialog(Order order) {
    DetallePedidoDialog dialog = new DetallePedidoDialog(requireContext(), order, nuevoEstado -> {
        pedidosViewModel.actualizarEstadoPedido(order.getId(), nuevoEstado);
    });
    dialog.show();
}
```

---

### PANTALLA 8: Panel de Administración y Perfil (`HomeActivity` -> `PerfilFragment`)

#### A. Propósito y Funcionamiento
Panel de control para el operador. Brinda acceso a la administración del catálogo de platos, registro de gastos/ingresos de caja chica, visualización de historial de turnos, vinculación de impresora térmica y cierre de sesión o turno.

#### B. Textos Visibles en Pantalla
* **Usuario:** Nombre del cajero, rol y sucursal
* **Opciones del Menú:**
  * "🍲 Gestión de Menú y Catálogo"
  * "💵 Movimientos de Caja Chica"
  * "📅 Historial de Turnos y Cierres"
  * "🖨️ Configurar Impresora Térmica"
  * "🔒 Cerrar Turno (Arqueo Final)"
  * "🚪 Cerrar Sesión"

#### C. Captura de Pantalla
> *[PEGAR AQUÍ CAPTURA DE PANTALLA DE: PerfilFragment - fragment_perfil.xml]*

#### D. Código del Layout XML (Extracto clave: `fragment_perfil.xml`)
```xml
<!-- Botón para ir al catálogo de productos -->
<LinearLayout
    android:id="@+id/btnGestionMenu"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:clickable="true" />

<!-- Botón para ir al historial de turnos -->
<LinearLayout
    android:id="@+id/btnHistorialTurnos"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:clickable="true" />

<!-- Botón para cerrar turno y arquear caja -->
<LinearLayout
    android:id="@+id/btnCloseShift"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:clickable="true" />
```

#### E. Lógica Java de Navegación (`PerfilFragment.java`)
```java
// 1. Navegación a Gestión del Catálogo de Productos
btnGestionMenu.setOnClickListener(v -> {
    Intent intent = new Intent(requireContext(), GestionProductosActivity.class);
    startActivity(intent);
});

// 2. Navegación al Historial de Turnos
btnHistorialTurnos.setOnClickListener(v -> {
    Intent intent = new Intent(requireContext(), HistorialTurnosActivity.class);
    startActivity(intent);
});

// 3. Navegación al Cierre de Turno y Arqueo
btnCloseShift.setOnClickListener(v -> {
    Intent intent = new Intent(requireContext(), CierreCajaActivity.class);
    startActivity(intent);
});

// 4. Cierre definitivo de sesión (Logout hacia MainActivity)
btnLogout.setOnClickListener(v -> {
    repository.getSessionManager().clear();
    Intent intent = new Intent(requireContext(), MainActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    if (getActivity() != null) getActivity().finish();
});
```

#### F. Explicación Línea por Línea del Código de Navegación:
1. `btnGestionMenu.setOnClickListener(...)`: Captura el clic en la opción de catálogo.
2. `Intent intent = new Intent(requireContext(), GestionProductosActivity.class);`: Prepara la apertura de la ventana CRUD de productos.
3. `startActivity(intent);`: Inicia `GestionProductosActivity`.
4. `btnHistorialTurnos.setOnClickListener(...)`: Captura la selección de historial.
5. `Intent intent = new Intent(requireContext(), HistorialTurnosActivity.class);`: Instancia la navegación al historial de cierres.
6. `btnCloseShift.setOnClickListener(...)`: Desencadena el flujo de arqueo final en `CierreCajaActivity`.
7. `repository.getSessionManager().clear();`: Borra las credenciales en caché al pulsar salir.
8. `intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);`: Limpia todo el stack de navegación para que la app quede en estado inicial en `MainActivity`.

---

### PANTALLA 9: Gestión del Catálogo de Productos y Uso de Cámara (`GestionProductosActivity`)

#### A. Propósito y Funcionamiento
Permite crear, editar, eliminar o marcar platos como agotados. Incluye la captura fotográfica del plato utilizando la cámara del dispositivo o seleccionando una imagen desde la galería, con posterior recorte 1:1.

#### B. Textos Visibles en Pantalla
* **Título:** "Catálogo de Productos"
* **Botón Nuevo:** "+ NUEVO PLATO"
* **Formulario Modal:** Nombre, Categoría, Precio, Switch "Disponible", botón "Tomar Foto con Cámara".

#### C. Captura de Pantalla
> *[PEGAR AQUÍ CAPTURA DE PANTALLA DE: GestionProductosActivity - activity_gestion_productos.xml]*

#### D. Código del Layout XML (Extracto clave: `activity_gestion_productos.xml`)
```xml
<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
    android:id="@+id/fabAddProduct"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Nuevo Plato"
    app:icon="@drawable/ic_add" />
```

#### E. Lógica Java de Navegación hacia Cámara y Recorte (`GestionProductosActivity.java`)
```java
// 1. Lanzamiento de la Cámara nativa mediante FileProvider
private void startCameraCapture() {
    File dir = new File(getCacheDir(), "camera");
    if (!dir.exists()) dir.mkdirs();
    File tempFile = new File(dir, "cam_" + System.currentTimeMillis() + ".jpg");
    cameraTempUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", tempFile);
    takePictureLauncher.launch(cameraTempUri);
}

// 2. Navegación hacia la pantalla de recorte de imagen (CropImageActivity)
private void launchCropActivity(Uri uri) {
    Intent intent = new Intent(this, CropImageActivity.class);
    intent.putExtra(CropImageActivity.EXTRA_IMAGE_URI, uri);
    cropLauncher.launch(intent);
}
```

#### F. Explicación Línea por Línea del Código de Navegación:
1. `File tempFile = new File(dir, "cam_" + ... + ".jpg");`: Crea el archivo de destino temporal para la foto.
2. `cameraTempUri = FileProvider.getUriForFile(...)`: Genera la URI segura bajo el estándar de `FileProvider`.
3. `takePictureLauncher.launch(cameraTempUri);`: Abre la aplicación de cámara de Android.
4. `Intent intent = new Intent(this, CropImageActivity.class);`: Tras capturar la foto, prepara el salto a la pantalla de recorte.
5. `intent.putExtra(CropImageActivity.EXTRA_IMAGE_URI, uri);`: Adjunta la URI de la fotografía recién capturada.
6. `cropLauncher.launch(intent);`: Lanza `CropImageActivity` esperando el resultado de la imagen procesada.

---

### PANTALLA 10: Recorte y Ajuste de Foto (`CropImageActivity`)

#### A. Propósito y Funcionamiento
Permite centrar, mover y recortar la fotografía del plato en proporción fija 1:1 antes de asociarla al producto.

#### B. Textos Visibles en Pantalla
* **Título:** "Ajustar Foto del Plato"
* **Subtítulo:** "Arrastra y encuadra la imagen en el recuadro"
* **Botones:** "Cancelar", "APLICAR RECORTE"

#### C. Captura de Pantalla
> *[PEGAR AQUÍ CAPTURA DE PANTALLA DE: CropImageActivity - activity_crop_image.xml]*

#### D. Código del Layout XML (Extracto clave: `activity_crop_image.xml`)
```xml
<ImageView
    android:id="@+id/ivCropPreview"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:scaleType="matrix" />

<com.google.android.material.button.MaterialButton
    android:id="@+id/btnApplyCrop"
    android:layout_width="0dp"
    android:layout_height="56dp"
    android:text="APLICAR RECORTE" />
```

#### E. Lógica Java de Navegación (`CropImageActivity.java`)
```java
// Cancelar recorte y regresar sin cambios
findViewById(R.id.btnCancel).setOnClickListener(v -> finish());

// Guardar recorte y retornar resultado a GestionProductosActivity
findViewById(R.id.btnApplyCrop).setOnClickListener(v -> {
    String croppedPath = saveCroppedBitmap(croppedBitmap);
    Intent resultIntent = new Intent();
    resultIntent.putExtra(EXTRA_CROPPED_PATH, croppedPath);
    setResult(RESULT_OK, resultIntent);
    finish();
});
```

#### F. Explicación Línea por Línea del Código de Navegación:
1. `findViewById(R.id.btnCancel).setOnClickListener(v -> finish());`: Descarta la foto y regresa.
2. `String croppedPath = saveCroppedBitmap(...);`: Guarda el bitmap recortado en la memoria privada de la app.
3. `Intent resultIntent = new Intent();`: Crea el intent contenedor del resultado.
4. `resultIntent.putExtra(EXTRA_CROPPED_PATH, croppedPath);`: Almacena la ruta del archivo generado.
5. `setResult(RESULT_OK, resultIntent);`: Establece el código de éxito `RESULT_OK` con los datos adjuntos.
6. `finish();`: Cierra la actividad devolviendo el control a `GestionProductosActivity`.

---

### PANTALLA 11: Cierre de Turno y Arqueo de Caja (`CierreCajaActivity`)

#### A. Propósito y Funcionamiento
Ejecuta el arqueo final ciego del turno. Muestra el total de ventas acumuladas, calcula el efectivo esperado, solicita al cajero que cuente el dinero físico real e informa en tiempo real si la caja está cuadrada o si existe sobrante o faltante. Al confirmar, cierra el turno en la base de datos y desautentica la sesión.

#### B. Textos Visibles en Pantalla
* **Título:** "Cierre de Turno & Arqueo"
* **Monto Esperado:** "Efectivo esperado en caja: Bs. XX.XX"
* **Campo de Conteo:** "Efectivo contado físicamente (Bs.)"
* **Tarjeta de Estado:** "Caja Cuadrada" (Verde), "Sobrante en caja" o "Faltante en caja" (Rojo)
* **Botón Principal:** "CONFIRMAR CIERRE DE TURNO"

#### C. Captura de Pantalla
> *[PEGAR AQUÍ CAPTURA DE PANTALLA DE: CierreCajaActivity - activity_cierre_caja.xml]*

#### D. Código del Layout XML (Extracto clave: `activity_cierre_caja.xml`)
```xml
<EditText
    android:id="@+id/etConteo"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="0.00"
    android:inputType="numberDecimal" />

<com.google.android.material.button.MaterialButton
    android:id="@+id/btnConfirmarCierre"
    android:layout_width="match_parent"
    android:layout_height="56dp"
    android:text="CONFIRMAR CIERRE DE TURNO"
    app:cornerRadius="14dp" />
```

#### E. Lógica Java de Navegación (`CierreCajaActivity.java`)
```java
repo.cerrarTurno(turnoActivo.getId(), esperado, contado, diff, new PosRepository.Callback<Void>() {
    @Override
    public void onSuccess(Void result) {
        repo.getSessionManager().setTurnoId(0);
        Toast.makeText(CierreCajaActivity.this, "Turno cerrado exitosamente", Toast.LENGTH_SHORT).show();

        // Reenrutamiento a la pantalla de Login limpiando el historial de navegación
        Intent intent = new Intent(CierreCajaActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onError(String error) {
        Toast.makeText(CierreCajaActivity.this, "Error al cerrar turno: " + error, Toast.LENGTH_SHORT).show();
    }
});
```

#### F. Explicación Línea por Línea del Código de Navegación:
1. `repo.cerrarTurno(...)`: Actualiza en la tabla `turnos` la fecha de cierre, monto final y diferencia.
2. `repo.getSessionManager().setTurnoId(0);`: Resetea el ID de turno a 0 en las preferencias de sesión.
3. `Intent intent = new Intent(CierreCajaActivity.this, MainActivity.class);`: Prepara el regreso a `MainActivity`.
4. `intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);`: Destruye todas las actividades activas del turno para garantizar seguridad física y lógica en el terminal.
5. `startActivity(intent); finish();`: Muestra la pantalla de login para el siguiente turno.

---

### PANTALLA 12: Historial de Turnos (`HistorialTurnosActivity`)

#### A. Propósito y Funcionamiento
Permite consultar el listado histórico de todos los turnos cerrados, visualizando fecha, hora, monto inicial, ventas totales y discrepancia de caja (sobrantes o faltantes).

#### B. Textos Visibles en Pantalla
* **Encabezado:** "Historial de Turnos"
* **Lista de Tarjetas:** Fecha, Cajero, Fondo inicial, Total vendido, Diferencia de arqueo.
* **Botón:** "←" (Regresar)

#### C. Captura de Pantalla
> *[PEGAR AQUÍ CAPTURA DE PANTALLA DE: HistorialTurnosActivity - activity_historial_turnos.xml]*

#### D. Lógica Java de Navegación (`HistorialTurnosActivity.java`)
```java
// Retorno al menú de perfil
findViewById(R.id.btnBack).setOnClickListener(v -> finish());
```

---

### PANTALLA 13: Reportes y Métricas (`HomeActivity` -> `ReportesFragment`)

#### A. Propósito y Funcionamiento
Módulo analítico que procesa la información de ventas del día, el plato más vendido con su respectivo emoji, las horas pico de mayor afluencia de clientes y el desglose de ingresos por categoría.

#### B. Textos Visibles en Pantalla
* **Título:** "Reportes y Analítica"
* **Tarjetas de Métricas:**
  * "Total Ventas del Día: Bs. XX.XX"
  * "Plato Estrella: Pollo a la Brasa 1/4"
  * "Horas Pico: 12:00 PM - 2:00 PM"
* **Gráficas/Barras:** Distribución porcentual por categorías.

#### C. Captura de Pantalla
> *[PEGAR AQUÍ CAPTURA DE PANTALLA DE: ReportesFragment - fragment_reportes.xml]*

---

### PANTALLA 14: Créditos y Autores del Sistema (`AutoresActivity`)

#### A. Propósito y Funcionamiento
Pantalla de créditos que expone la información técnica del software, integrantes del equipo de desarrollo, tecnologías empleadas y arquitectura del sistema.

#### B. Textos Visibles en Pantalla
* **Título:** "Equipo de Desarrollo"
* **Subtítulo:** "Pollo POS v2.0 - Arquitectura Nativa Android"
* **Tarjetas de Integrantes:** Nombres, roles y aportes al proyecto.
* **Botón Regresar:** "← Volver"

#### C. Captura de Pantalla
> *[PEGAR AQUÍ CAPTURA DE PANTALLA DE: AutoresActivity - activity_autores.xml]*

#### D. Lógica Java de Navegación (`AutoresActivity.java`)
```java
findViewById(R.id.btnBack).setOnClickListener(v -> finish());
```

---

## 5. MANUAL DE USUARIO RÁPIDO: CÓMO USAR CADA FUNCIÓN DIGITAL

1. **Apertura de Sistema:**
   * Abre la app e ingresa con tu usuario y contraseña, o tu PIN de 4 dígitos.
   * Si es tu primer acceso del día, ingresa el monto de caja chica (ej. Bs. 100) y presiona **ABRIR TURNO Y COMENZAR**.
2. **Tomar una Venta:**
   * Selecciona el modo de consumo: **En Mesa** o **Para Llevar**.
   * Filtra por categorías (Pollo Frito, Brasa, Combos, etc.) y presiona el botón **+** sobre el producto deseado.
   * Si el cliente desea personalizaciones (ej. "Sin ají"), mantén presionado el producto o toca la opción de notas.
   * En la barra flotante inferior, verifica el total y presiona **Cobrar**.
3. **Cobro y Métodos de Pago:**
   * Selecciona **Efectivo**, **Tarjeta**, **QR** o **Mixto**.
   * Si es efectivo, escribe el monto recibido y el sistema calculará el cambio automáticamente.
   * Presiona **CONFIRMAR PAGO**.
4. **Emisión de Recibo y Comanda:**
   * Visualiza el ticket digital. Toca **IMPRIMIR** para emitir el ticket físico por la impresora Bluetooth vinculada, o **COMPARTIR** para enviarlo por WhatsApp.
   * Toca **NUEVA VENTA** para regresar a la pantalla de pedidos.
5. **Atención en Cocina (KDS):**
   * Toca la pestaña **Pedidos** en la barra inferior para ver las comandas en tiempo real.
   * Cambia el estado a **En Cocina** y luego a **Listo** para despachar.
6. **Administrar Menú y Tomar Fotos:**
   * Ve a la pestaña **Perfil** → **Gestión de Menú y Catálogo**.
   * Para añadir un plato nuevo, toca **+ Nuevo Plato**, escribe el nombre, precio y presiona **Tomar Foto con Cámara**. Encuadra la foto en el recortador 1:1 y presiona **Aplicar Recorte**.
7. **Cierre de Turno y Arqueo Final:**
   * Ve a **Perfil** → **Cerrar Turno**.
   * Cuenta físicamente los billetes y monedas de la gaveta e introduce el total.
   * Revisa la alerta: si está verde la caja está cuadrada; si está roja verifica si hubo faltante.
   * Presiona **CONFIRMAR CIERRE DE TURNO** para finalizar la jornada y dejar la caja lista para el siguiente cajero.

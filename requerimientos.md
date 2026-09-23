Requerimientos Funcionales (RF)

Gestión de usuarios y acceso

RF01: El sistema debe permitir el inicio de sesión mediante usuario y contraseña, o mediante PIN de acceso rápido.
RF02: El sistema debe permitir recuperar la contraseña mediante un código enviado al correo o teléfono registrado, o mediante la intervención de un administrador.
RF03: El sistema debe permitir la gestión de roles de usuario (administrador, cajero, cocina), aunque en la primera versión todas las funciones sean operadas por un mismo usuario.
RF04: El sistema debe registrar la apertura de turno de un usuario, incluyendo el fondo inicial de caja.

Gestión de productos

RF05: El sistema debe permitir visualizar los productos organizados por categoría (pollo frito, a la brasa, combos, bebidas, acompañamientos).
RF06: El sistema debe permitir marcar un producto como agotado, ocultándolo o deshabilitándolo para nuevos pedidos.
RF07: El sistema debe permitir buscar productos por nombre.

Gestión de pedidos

RF08: El sistema debe permitir crear un nuevo pedido, seleccionando productos, cantidades y notas adicionales por producto (ej. "sin ají").
RF09: El sistema debe permitir especificar si el pedido es para consumir en mesa o para llevar.
RF10: El sistema debe permitir aplicar un código de descuento a un pedido antes de confirmar el cobro.
RF11: El sistema debe asignar un número de orden identificable al cliente para cada pedido.
RF12: El sistema debe permitir visualizar el estado de los pedidos en curso (en cocina, listo, entregado).
RF13: El sistema debe permitir actualizar el estado de un pedido conforme avanza su preparación y entrega.
RF14: El sistema debe permitir cancelar un pedido, registrando el motivo de la cancelación.

Pagos

RF15: El sistema debe permitir registrar el cobro de un pedido mediante efectivo, tarjeta, QR, o pago mixto.
RF16: El sistema debe calcular automáticamente el vuelto cuando el pago es en efectivo.
RF17: El sistema debe generar un comprobante/recibo de venta al confirmar el pago, con opción de imprimir o compartir digitalmente.

Caja

RF18: El sistema debe permitir el cierre de caja al final de un turno, calculando el efectivo esperado según las ventas registradas.
RF19: El sistema debe permitir el ingreso manual del efectivo contado físicamente y calcular la diferencia (sobrante o faltante) respecto al esperado.

Reportes

RF20: El sistema debe permitir visualizar el total de ventas del día.
RF21: El sistema debe permitir visualizar el producto más vendido en un período determinado.
RF22: El sistema debe permitir visualizar las horas de mayor demanda (horas pico).
RF23: El sistema debe permitir consultar el historial de cierres de caja por turno.
Requerimientos No Funcionales (RNF)

Rendimiento

RNF01: El sistema debe registrar un nuevo pedido en menos de 3 segundos desde que se confirma, en condiciones normales de red.
RNF02: La pantalla de selección de productos debe cargar en menos de 2 segundos.

Usabilidad

RNF03: La interfaz debe estar optimizada para uso táctil en dispositivos móviles, con elementos de al menos 44x44 px de área de toque.
RNF04: El sistema debe permitir completar un pedido básico (selección + cobro) en un máximo de 5 pasos, dado el uso frecuente y repetitivo del flujo.

Disponibilidad y confiabilidad

RNF05: El sistema debe estar disponible al menos el 99% del tiempo durante el horario de atención del local.
RNF06: El sistema debe evitar la pérdida de información de un pedido en caso de fallo de conexión momentáneo, mediante reintento automático de sincronización.

Seguridad

RNF07: Las contraseñas de los usuarios deben almacenarse cifradas (hash), nunca en texto plano.
RNF08: El sistema debe cerrar la sesión automáticamente tras un periodo de inactividad configurable.
RNF09: Solo los usuarios con rol de administrador deben poder acceder a los reportes de ventas y al historial de cierres de caja.

Escalabilidad

RNF10: El modelo de datos debe soportar múltiples sucursales sin requerir cambios estructurales, aunque en la primera versión opere una sola.
RNF11: El sistema debe soportar el crecimiento del número de usuarios por turno (de un operador único a varios roles) sin rediseño del backend.

Compatibilidad

RNF12: La aplicación debe funcionar correctamente en las versiones recientes de Android e iOS (dado el enfoque híbrido).
RNF13: El backend debe exponer una API que pueda ser consumida tanto por la app móvil como por futuras integraciones (ej. impresora térmica, panel web administrativo).

Mantenibilidad

RNF14: El código del backend debe estar organizado en capas (controladores, lógica de negocio, acceso a datos) para facilitar el mantenimiento y las pruebas.
RNF15: La base de datos debe mantener integridad referencial mediante llaves foráneas y restricciones, evitando datos inconsistentes.
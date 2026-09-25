-- ============================================================
-- BRASA POS / PolloPOS · Esquema de base de datos (PostgreSQL)
-- Sistema de Punto de Venta para Restaurante de Pollo
-- Arquitectura Multi-Sucursal, RBAC y Arqueo Contable Fiel
-- ============================================================

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================
-- 1. SUCURSALES (Multi-sucursal: RNF10)
-- ============================================================
CREATE TABLE IF NOT EXISTS sucursales (
    sucursal_id     SERIAL PRIMARY KEY,
    nombre          VARCHAR(100) NOT NULL,
    direccion       VARCHAR(255),
    telefono        VARCHAR(20),
    activa          BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE sucursales IS 'Sedes y locales comerciales del negocio.';

-- ============================================================
-- 2. ROLES Y USUARIOS (RF01, RF02, RF03, RNF07, RNF09, RNF11)
-- ============================================================
CREATE TABLE IF NOT EXISTS roles (
    rol_id          SERIAL PRIMARY KEY,
    nombre          VARCHAR(30) NOT NULL UNIQUE  -- 'admin', 'cajero', 'cocina'
);

CREATE TABLE IF NOT EXISTS usuarios (
    usuario_id      SERIAL PRIMARY KEY,
    sucursal_id     INTEGER NOT NULL REFERENCES sucursales(sucursal_id),
    rol_id          INTEGER NOT NULL REFERENCES roles(rol_id),
    nombre_completo VARCHAR(100) NOT NULL,
    nombre_usuario  VARCHAR(50) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    pin_rapido      VARCHAR(255),         -- Hash de PIN para acceso rápido en caja (RF01)
    correo          VARCHAR(150),
    telefono        VARCHAR(20),
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_usuarios_sucursal ON usuarios(sucursal_id);
CREATE INDEX IF NOT EXISTS idx_usuarios_nombre_usuario ON usuarios(nombre_usuario);

-- Códigos temporales para recuperación de contraseña (RF02)
CREATE TABLE IF NOT EXISTS recuperacion_password (
    recuperacion_id SERIAL PRIMARY KEY,
    usuario_id      INTEGER NOT NULL REFERENCES usuarios(usuario_id) ON DELETE CASCADE,
    codigo          VARCHAR(10) NOT NULL,
    expira_en       TIMESTAMPTZ NOT NULL,
    usado           BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- 3. CATEGORÍAS Y PRODUCTOS (RF05, RF06, RF07, RNF02)
-- ============================================================
CREATE TABLE IF NOT EXISTS categorias (
    categoria_id    SERIAL PRIMARY KEY,
    nombre          VARCHAR(60) NOT NULL,   -- Pollo frito, A la brasa, Combos, Bebidas...
    orden           INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS productos (
    producto_id     SERIAL PRIMARY KEY,
    sucursal_id     INTEGER NOT NULL REFERENCES sucursales(sucursal_id),
    categoria_id    INTEGER NOT NULL REFERENCES categorias(categoria_id),
    nombre          VARCHAR(100) NOT NULL,
    descripcion     VARCHAR(255),
    precio          NUMERIC(10,2) NOT NULL CHECK (precio >= 0),
    disponible      BOOLEAN NOT NULL DEFAULT TRUE,  -- botón 'marcar agotado' = FALSE (RF06)
    imagen_url      VARCHAR(500),                   -- URL de imagen en WebP / Bucket
    imagen_emoji    VARCHAR(10),                    -- Emoji salvavidas opcional
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_productos_sucursal ON productos(sucursal_id);
CREATE INDEX IF NOT EXISTS idx_productos_categoria ON productos(categoria_id);
CREATE INDEX IF NOT EXISTS idx_productos_disponible ON productos(disponible);
CREATE INDEX IF NOT EXISTS idx_productos_nombre ON productos(nombre);

-- ============================================================
-- 4. TURNOS DE CAJA (RF04, RF18, RF19, RF23)
-- ============================================================
CREATE TABLE IF NOT EXISTS turnos (
    turno_id            SERIAL PRIMARY KEY,
    sucursal_id         INTEGER NOT NULL REFERENCES sucursales(sucursal_id),
    usuario_id          INTEGER NOT NULL REFERENCES usuarios(usuario_id),
    fondo_inicial       NUMERIC(10,2) NOT NULL DEFAULT 0,
    abierto_en          TIMESTAMPTZ NOT NULL DEFAULT now(),
    cerrado_en          TIMESTAMPTZ,
    efectivo_esperado   NUMERIC(10,2),      -- Calculado al cerrar (Fondo + Efectivo Ventas + Ingresos Extra - Gastos)
    efectivo_contado    NUMERIC(10,2),      -- Ingresado físicamente por el cajero (RF19)
    diferencia          NUMERIC(10,2) GENERATED ALWAYS AS (efectivo_contado - efectivo_esperado) STORED,
    estado              VARCHAR(15) NOT NULL DEFAULT 'abierto'
                            CHECK (estado IN ('abierto', 'cerrado'))
);

CREATE INDEX IF NOT EXISTS idx_turnos_usuario ON turnos(usuario_id);
CREATE INDEX IF NOT EXISTS idx_turnos_estado ON turnos(estado);

-- ============================================================
-- 5. MOVIMIENTOS DE CAJA CHICA (Gastos e Ingresos Extra de Turno)
-- ============================================================
CREATE TABLE IF NOT EXISTS movimientos_caja (
    movimiento_id   SERIAL PRIMARY KEY,
    turno_id        INTEGER NOT NULL REFERENCES turnos(turno_id) ON DELETE CASCADE,
    usuario_id      INTEGER NOT NULL REFERENCES usuarios(usuario_id),
    tipo            VARCHAR(10) NOT NULL CHECK (tipo IN ('INGRESO', 'EGRESO')),
    monto           NUMERIC(10,2) NOT NULL CHECK (monto > 0),
    concepto        VARCHAR(255) NOT NULL,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_movimientos_turno ON movimientos_caja(turno_id);

-- ============================================================
-- 6. MESAS (RF09)
-- ============================================================
CREATE TABLE IF NOT EXISTS mesas (
    mesa_id         SERIAL PRIMARY KEY,
    sucursal_id     INTEGER NOT NULL REFERENCES sucursales(sucursal_id),
    numero          INTEGER NOT NULL,
    UNIQUE (sucursal_id, numero)
);

-- ============================================================
-- 7. DESCUENTOS (RF10)
-- ============================================================
CREATE TABLE IF NOT EXISTS descuentos (
    descuento_id        SERIAL PRIMARY KEY,
    codigo              VARCHAR(30) NOT NULL UNIQUE,
    tipo                VARCHAR(15) NOT NULL CHECK (tipo IN ('porcentaje', 'monto_fijo')),
    valor               NUMERIC(10,2) NOT NULL CHECK (valor > 0),
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    valido_desde        TIMESTAMPTZ,
    valido_hasta        TIMESTAMPTZ,
    creado_en           TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- 8. PEDIDOS (RF08 - RF14)
-- ============================================================
CREATE TABLE IF NOT EXISTS pedidos (
    pedido_id           SERIAL PRIMARY KEY,
    sucursal_id         INTEGER NOT NULL REFERENCES sucursales(sucursal_id),
    turno_id            INTEGER NOT NULL REFERENCES turnos(turno_id),
    usuario_id          INTEGER NOT NULL REFERENCES usuarios(usuario_id),
    mesa_id             INTEGER REFERENCES mesas(mesa_id),
    numero_orden        INTEGER NOT NULL,          -- Número corto visible (RF11)
    tipo_entrega        VARCHAR(15) NOT NULL
                            CHECK (tipo_entrega IN ('mesa', 'para_llevar')),
    estado              VARCHAR(20) NOT NULL DEFAULT 'cocina'
                            CHECK (estado IN ('en_cocina', 'cocina', 'listo', 'entregado', 'cancelado')),
    estado_pago         VARCHAR(15) NOT NULL DEFAULT 'pendiente'
                            CHECK (estado_pago IN ('pendiente', 'pagado', 'cancelado')),
    subtotal            NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (subtotal >= 0),
    descuento_id        INTEGER REFERENCES descuentos(descuento_id),
    monto_descuento     NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (monto_descuento >= 0),
    total               NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (total >= 0),
    motivo_cancelacion  VARCHAR(255),               -- RF14: motivo al cancelar
    creado_en           TIMESTAMPTZ NOT NULL DEFAULT now(),
    listo_en            TIMESTAMPTZ,
    entregado_en        TIMESTAMPTZ,
    cancelado_en        TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_pedidos_sucursal ON pedidos(sucursal_id);
CREATE INDEX IF NOT EXISTS idx_pedidos_turno ON pedidos(turno_id);
CREATE INDEX IF NOT EXISTS idx_pedidos_estado ON pedidos(estado);
CREATE INDEX IF NOT EXISTS idx_pedidos_creado_en ON pedidos(creado_en);

-- ============================================================
-- 9. DETALLES DE PEDIDO (RF08: productos, cantidades y notas)
-- ============================================================
CREATE TABLE IF NOT EXISTS pedido_detalles (
    detalle_id       SERIAL PRIMARY KEY,
    pedido_id        INTEGER NOT NULL REFERENCES pedidos(pedido_id) ON DELETE CASCADE,
    producto_id      INTEGER NOT NULL REFERENCES productos(producto_id),
    cantidad         INTEGER NOT NULL CHECK (cantidad > 0),
    precio_unitario  NUMERIC(10,2) NOT NULL CHECK (precio_unitario >= 0),
    subtotal         NUMERIC(10,2) NOT NULL CHECK (subtotal >= 0),
    notas            VARCHAR(255)  -- ej. 'sin ají', 'pechuga', 'bien dorado'
);

CREATE INDEX IF NOT EXISTS idx_pedido_detalles_pedido ON pedido_detalles(pedido_id);
CREATE INDEX IF NOT EXISTS idx_pedido_detalles_producto ON pedido_detalles(producto_id);

-- ============================================================
-- 10. PAGOS (RF15, RF16, RF17) - Soporte Robusto para Pagos Mixtos
-- ============================================================
CREATE TABLE IF NOT EXISTS pagos (
    pago_id          SERIAL PRIMARY KEY,
    pedido_id        INTEGER NOT NULL REFERENCES pedidos(pedido_id),
    turno_id         INTEGER NOT NULL REFERENCES turnos(turno_id),
    metodo_pago      VARCHAR(20) NOT NULL
                         CHECK (metodo_pago IN ('efectivo', 'tarjeta', 'qr', 'mixto')),
    monto            NUMERIC(10,2) NOT NULL CHECK (monto > 0),
    monto_recibido   NUMERIC(10,2) NOT NULL DEFAULT 0,
    monto_efectivo   NUMERIC(10,2) NOT NULL DEFAULT 0, -- Porción en billetes/monedas
    monto_digital    NUMERIC(10,2) NOT NULL DEFAULT 0, -- Porción en QR o tarjeta
    vuelto           NUMERIC(10,2) NOT NULL DEFAULT 0,
    referencia       VARCHAR(255),
    creado_en        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_pagos_pedido ON pagos(pedido_id);
CREATE INDEX IF NOT EXISTS idx_pagos_turno ON pagos(turno_id);

-- ============================================================
-- 11. VISTAS ANALÍTICAS (Reportes y Arqueo Exacto de Turnos)
-- ============================================================
CREATE OR REPLACE VIEW vw_resumen_turnos AS
SELECT 
    t.turno_id,
    t.sucursal_id,
    t.usuario_id,
    u.nombre_completo AS cajero_nombre,
    t.fondo_inicial,
    t.abierto_en,
    t.cerrado_en,
    t.estado,
    -- Total de ventas de órdenes facturadas y cobradas
    COALESCE((
        SELECT SUM(p.total) FROM pedidos p WHERE p.turno_id = t.turno_id AND p.estado_pago = 'pagado'
    ), 0) AS total_ventas,
    -- Recaudación de efectivo puro + cuota de efectivo de pagos mixtos
    COALESCE((
        SELECT SUM(pg.monto_efectivo) FROM pagos pg WHERE pg.turno_id = t.turno_id
    ), 0) AS total_efectivo,
    -- Recaudación con tarjeta
    COALESCE((
        SELECT SUM(pg.monto) FROM pagos pg WHERE pg.turno_id = t.turno_id AND pg.metodo_pago = 'tarjeta'
    ), 0) AS total_tarjeta,
    -- Recaudación digital QR + cuota digital de pagos mixtos
    COALESCE((
        SELECT SUM(
            CASE 
                WHEN pg.metodo_pago = 'qr' THEN pg.monto 
                WHEN pg.metodo_pago = 'mixto' THEN pg.monto_digital 
                ELSE 0 
            END
        ) FROM pagos pg WHERE pg.turno_id = t.turno_id
    ), 0) AS total_qr,
    -- Movimientos de caja chica
    COALESCE((
        SELECT SUM(mc.monto) FROM movimientos_caja mc WHERE mc.turno_id = t.turno_id AND mc.tipo = 'INGRESO'
    ), 0) AS total_ingresos_extra,
    COALESCE((
        SELECT SUM(mc.monto) FROM movimientos_caja mc WHERE mc.turno_id = t.turno_id AND mc.tipo = 'EGRESO'
    ), 0) AS total_egresos_gastos,
    -- Cantidad total de órdenes cobradas en el turno
    COALESCE((
        SELECT COUNT(p.pedido_id) FROM pedidos p WHERE p.turno_id = t.turno_id AND p.estado_pago = 'pagado'
    ), 0) AS total_pedidos,
    -- Efectivo que DEBE haber físicamente en caja: Fondo + Efectivo Cobrado + Ingresos Extra - Gastos
    (
        t.fondo_inicial 
        + COALESCE((SELECT SUM(pg.monto_efectivo) FROM pagos pg WHERE pg.turno_id = t.turno_id), 0)
        + COALESCE((SELECT SUM(mc.monto) FROM movimientos_caja mc WHERE mc.turno_id = t.turno_id AND mc.tipo = 'INGRESO'), 0)
        - COALESCE((SELECT SUM(mc.monto) FROM movimientos_caja mc WHERE mc.turno_id = t.turno_id AND mc.tipo = 'EGRESO'), 0)
    ) AS efectivo_esperado,
    t.efectivo_contado,
    t.diferencia
FROM turnos t
INNER JOIN usuarios u ON t.usuario_id = u.usuario_id;

CREATE OR REPLACE VIEW vw_estadisticas_diarias AS
SELECT
    p.sucursal_id,
    DATE(p.creado_en) AS fecha,
    COUNT(p.pedido_id) AS total_pedidos,
    COALESCE(SUM(p.total), 0) AS total_ventas,
    ROUND(COALESCE(AVG(p.total), 0), 2) AS ticket_promedio,
    COUNT(CASE WHEN p.tipo_entrega = 'mesa' THEN 1 END) AS pedidos_mesa,
    COUNT(CASE WHEN p.tipo_entrega = 'para_llevar' THEN 1 END) AS pedidos_para_llevar,
    COALESCE((
        SELECT SUM(pg.monto_efectivo) 
        FROM pagos pg 
        INNER JOIN pedidos ped ON pg.pedido_id = ped.pedido_id 
        WHERE ped.sucursal_id = p.sucursal_id 
          AND DATE(ped.creado_en) = DATE(p.creado_en)
          AND ped.estado_pago = 'pagado'
    ), 0) AS ventas_efectivo,
    COALESCE((
        SELECT SUM(pg.monto) 
        FROM pagos pg 
        INNER JOIN pedidos ped ON pg.pedido_id = ped.pedido_id 
        WHERE ped.sucursal_id = p.sucursal_id 
          AND DATE(ped.creado_en) = DATE(p.creado_en)
          AND pg.metodo_pago = 'tarjeta'
          AND ped.estado_pago = 'pagado'
    ), 0) AS ventas_tarjeta,
    COALESCE((
        SELECT SUM(
            CASE 
                WHEN pg.metodo_pago = 'qr' THEN pg.monto 
                WHEN pg.metodo_pago = 'mixto' THEN pg.monto_digital 
                ELSE 0 
            END
        ) 
        FROM pagos pg 
        INNER JOIN pedidos ped ON pg.pedido_id = ped.pedido_id 
        WHERE ped.sucursal_id = p.sucursal_id 
          AND DATE(ped.creado_en) = DATE(p.creado_en)
          AND ped.estado_pago = 'pagado'
    ), 0) AS ventas_qr
FROM pedidos p
WHERE p.estado_pago = 'pagado'
GROUP BY p.sucursal_id, DATE(p.creado_en);

COMMIT;

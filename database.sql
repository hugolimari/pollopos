-- ============================================================
-- BRASA POS · Esquema de base de datos (PostgreSQL)
-- Sistema de punto de venta para local de venta de pollo
-- ============================================================

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================
-- 1. SUCURSALES (Multi-sucursal desde el inicio: RNF10)
-- ============================================================
CREATE TABLE IF NOT EXISTS sucursales (
    sucursal_id     SERIAL PRIMARY KEY,
    nombre          VARCHAR(100) NOT NULL,
    direccion       VARCHAR(255),
    telefono        VARCHAR(20),
    activa          BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE sucursales IS 'Locales del negocio. Preparado para múltiples sucursales.';

-- ============================================================
-- 2. ROLES Y USUARIOS (RF01, RF02, RF03, RNF07, RNF11)
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
    pin_rapido      VARCHAR(255),         -- Hash de PIN para acceso rápido (RF01)
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
    disponible      BOOLEAN NOT NULL DEFAULT TRUE,  -- botón "marcar agotado" = FALSE (RF06)
    imagen_url      VARCHAR(500),                   -- URL en el bucket de almacenamiento en la nube
    imagen_emoji    VARCHAR(10),                    -- Emoji opcional como salvavidas
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
    efectivo_esperado   NUMERIC(10,2),      -- Calculado al cerrar
    efectivo_contado    NUMERIC(10,2),      -- Ingresado físicamente por el cajero (RF19)
    diferencia          NUMERIC(10,2) GENERATED ALWAYS AS (efectivo_contado - efectivo_esperado) STORED,
    estado              VARCHAR(15) NOT NULL DEFAULT 'abierto'
                            CHECK (estado IN ('abierto', 'cerrado'))
);

CREATE INDEX IF NOT EXISTS idx_turnos_usuario ON turnos(usuario_id);
CREATE INDEX IF NOT EXISTS idx_turnos_estado ON turnos(estado);

-- ============================================================
-- 5. MESAS (RF09)
-- ============================================================
CREATE TABLE IF NOT EXISTS mesas (
    mesa_id         SERIAL PRIMARY KEY,
    sucursal_id     INTEGER NOT NULL REFERENCES sucursales(sucursal_id),
    numero          INTEGER NOT NULL,
    UNIQUE (sucursal_id, numero)
);

-- ============================================================
-- 6. DESCUENTOS (RF10)
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
-- 7. PEDIDOS (RF08 - RF14)
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
-- 8. DETALLES DE PEDIDO (RF08: productos, cantidades y notas)
-- ============================================================
CREATE TABLE IF NOT EXISTS pedido_detalles (
    detalle_id       SERIAL PRIMARY KEY,
    pedido_id        INTEGER NOT NULL REFERENCES pedidos(pedido_id) ON DELETE CASCADE,
    producto_id      INTEGER NOT NULL REFERENCES productos(producto_id),
    cantidad         INTEGER NOT NULL CHECK (cantidad > 0),
    precio_unitario  NUMERIC(10,2) NOT NULL CHECK (precio_unitario >= 0),
    subtotal         NUMERIC(10,2) NOT NULL CHECK (subtotal >= 0),
    notas            VARCHAR(255)  -- ej. "sin ají", "pechuga", "bien dorado"
);

CREATE INDEX IF NOT EXISTS idx_pedido_detalles_pedido ON pedido_detalles(pedido_id);
CREATE INDEX IF NOT EXISTS idx_pedido_detalles_producto ON pedido_detalles(producto_id);

-- ============================================================
-- 9. PAGOS (RF15, RF16, RF17)
-- ============================================================
CREATE TABLE IF NOT EXISTS pagos (
    pago_id          SERIAL PRIMARY KEY,
    pedido_id        INTEGER NOT NULL REFERENCES pedidos(pedido_id),
    turno_id         INTEGER NOT NULL REFERENCES turnos(turno_id),
    metodo_pago      VARCHAR(20) NOT NULL
                         CHECK (metodo_pago IN ('efectivo', 'tarjeta', 'qr', 'mixto')),
    monto            NUMERIC(10,2) NOT NULL CHECK (monto > 0),
    monto_recibido   NUMERIC(10,2) NOT NULL DEFAULT 0,
    vuelto           NUMERIC(10,2) NOT NULL DEFAULT 0,
    referencia       VARCHAR(100),
    creado_en        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_pagos_pedido ON pagos(pedido_id);
CREATE INDEX IF NOT EXISTS idx_pagos_turno ON pagos(turno_id);

-- ============================================================
-- 10. VISTAS ANALÍTICAS (Reportes y Arqueo de Turnos)
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
    COALESCE(SUM(CASE WHEN p.estado_pago = 'pagado' THEN p.total ELSE 0 END), 0) AS total_ventas,
    COALESCE(SUM(CASE WHEN pg.metodo_pago = 'efectivo' THEN pg.monto ELSE 0 END), 0) AS total_efectivo,
    COALESCE(SUM(CASE WHEN pg.metodo_pago = 'tarjeta' THEN pg.monto ELSE 0 END), 0) AS total_tarjeta,
    COALESCE(SUM(CASE WHEN pg.metodo_pago = 'qr' THEN pg.monto ELSE 0 END), 0) AS total_qr,
    COUNT(DISTINCT CASE WHEN p.estado_pago = 'pagado' THEN p.pedido_id END) AS total_pedidos,
    (t.fondo_inicial + COALESCE(SUM(CASE WHEN pg.metodo_pago = 'efectivo' THEN pg.monto ELSE 0 END), 0)) AS efectivo_esperado,
    t.efectivo_contado,
    t.diferencia
FROM turnos t
INNER JOIN usuarios u ON t.usuario_id = u.usuario_id
LEFT JOIN pedidos p ON t.turno_id = p.turno_id
LEFT JOIN pagos pg ON p.pedido_id = pg.pedido_id
GROUP BY t.turno_id, t.sucursal_id, t.usuario_id, u.nombre_completo, t.fondo_inicial, t.abierto_en, t.cerrado_en, t.estado, t.efectivo_contado, t.diferencia;

CREATE OR REPLACE VIEW vw_estadisticas_diarias AS
SELECT
    p.sucursal_id,
    DATE(p.creado_en) AS fecha,
    COUNT(p.pedido_id) AS total_pedidos,
    COALESCE(SUM(p.total), 0) AS total_ventas,
    ROUND(COALESCE(AVG(p.total), 0), 2) AS ticket_promedio,
    COUNT(CASE WHEN p.tipo_entrega = 'mesa' THEN 1 END) AS pedidos_mesa,
    COUNT(CASE WHEN p.tipo_entrega = 'para_llevar' THEN 1 END) AS pedidos_para_llevar,
    COALESCE(SUM(CASE WHEN pg.metodo_pago = 'efectivo' THEN pg.monto ELSE 0 END), 0) AS ventas_efectivo,
    COALESCE(SUM(CASE WHEN pg.metodo_pago = 'tarjeta' THEN pg.monto ELSE 0 END), 0) AS ventas_tarjeta,
    COALESCE(SUM(CASE WHEN pg.metodo_pago = 'qr' THEN pg.monto ELSE 0 END), 0) AS ventas_qr
FROM pedidos p
LEFT JOIN pagos pg ON p.pedido_id = pg.pedido_id
WHERE p.estado_pago = 'pagado'
GROUP BY p.sucursal_id, DATE(p.creado_en);

COMMIT;

-- ============================================================
-- BASE DE DATOS: FarmaSoft Plus (Colombia)
-- Compatibilidad: MySQL 8.0+ / MariaDB / HeidiSQL
-- ============================================================

CREATE DATABASE IF NOT EXISTS farmasoft_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE farmasoft_db;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS domicilios;
DROP TABLE IF EXISTS devoluciones;
DROP TABLE IF EXISTS detalles_formulas;
DROP TABLE IF EXISTS formulas_medicas;
DROP TABLE IF EXISTS detalles_ventas;
DROP TABLE IF EXISTS ventas;
DROP TABLE IF EXISTS detalles_compras;
DROP TABLE IF EXISTS compras;
DROP TABLE IF EXISTS movimientos_inventario;
DROP TABLE IF EXISTS lotes;
DROP TABLE IF EXISTS productos;
DROP TABLE IF EXISTS categorias;
DROP TABLE IF EXISTS proveedores;
DROP TABLE IF EXISTS usuarios;
DROP TABLE IF EXISTS empleados;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS clientes;
DROP TABLE IF EXISTS configuracion_global;
SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------
-- 0. TABLA CONFIGURACION GLOBAL (Parámetros del sistema)
-- ------------------------------------------------------------
CREATE TABLE configuracion_global (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre_farmacia VARCHAR(150) NOT NULL,
    nit VARCHAR(50) NOT NULL,
    telefono VARCHAR(30),
    direccion VARCHAR(200),
    iva_general DECIMAL(5,2) DEFAULT 19.00,
    moneda VARCHAR(10) DEFAULT 'COP',
    tiempo_alerta_vencimiento_dias INT DEFAULT 60
) ENGINE=InnoDB;

INSERT INTO configuracion_global (id, nombre_farmacia, nit, telefono, direccion, iva_general, moneda, tiempo_alerta_vencimiento_dias) VALUES
(1, 'FarmaSoft Plus S.A.S.', '900123456-7', '6022345678', 'Calle Principal # 10-20', 19.00, 'COP', 60);

-- ------------------------------------------------------------
-- 1. TABLA ROLES
-- ------------------------------------------------------------
CREATE TABLE roles (
    id_rol INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB;

INSERT INTO roles (id_rol, nombre) VALUES
(1, 'ADMINISTRADOR'),
(2, 'FARMACEUTICO_REGENTE'),
(3, 'VENDEDOR'),
(4, 'AUXILIAR_FARMACIA');

-- ------------------------------------------------------------
-- 2. TABLA CLIENTES
-- ------------------------------------------------------------
CREATE TABLE clientes (
    id_cliente INT AUTO_INCREMENT PRIMARY KEY,
    nombre_completo VARCHAR(150) NOT NULL,
    tipo_documento ENUM('CC', 'CE', 'PASAPORTE', 'TI', 'NIT') DEFAULT 'CC',
    numero_documento VARCHAR(20) NOT NULL UNIQUE,
    telefono VARCHAR(15),
    direccion VARCHAR(200),
    correo VARCHAR(100),
    fecha_nacimiento DATE,
    eps VARCHAR(100),
    alergias TEXT,
    fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

INSERT INTO clientes (nombre_completo, tipo_documento, numero_documento, telefono, direccion, correo, fecha_nacimiento, eps, alergias) VALUES
('Carlos Alberto Gómez', 'CC', '1114829301', '3157894512', 'Calle 25 # 14-30', 'carlos.gomez@gmail.com', '1988-04-12', 'Sura', 'Penicilina'),
('María Fernanda Rojas', 'CC', '1085293841', '3124567890', 'Carrera 10 # 5-12', 'mafe.rojas@hotmail.com', '1995-08-23', 'Sanitas', 'Ninguna'),
('Jorge Enrique Linares', 'CC', '14239841', '3189012345', 'Av. Pasoancho # 56-11', 'jorge.linares@yahoo.com', '1975-11-02', 'Nueva EPS', 'Aspirina, Ibuprofeno'),
('Diana Marcela Ruiz', 'CC', '1113840291', '3001234567', 'Calle 5 # 38-20', 'diana.ruiz@gmail.com', '2001-02-15', 'Salud Total', 'Sulfa'),
('Andrés Felipe Valencia', 'CC', '1098231456', '3168901234', 'Cra 8 # 12-45', 'andres.valencia@outlook.com', '1992-06-30', 'Compensar', 'Ninguna');

-- ------------------------------------------------------------
-- 3. TABLA EMPLEADOS Y USUARIOS
-- ------------------------------------------------------------
CREATE TABLE empleados (
    id_empleado INT AUTO_INCREMENT PRIMARY KEY,
    nombre_completo VARCHAR(150) NOT NULL,
    tipo_documento ENUM('CC', 'CE', 'PASAPORTE') DEFAULT 'CC',
    numero_documento VARCHAR(20) NOT NULL UNIQUE,
    telefono VARCHAR(15),
    direccion VARCHAR(200),
    correo VARCHAR(100),
    cargo VARCHAR(50) NOT NULL,
    salario DECIMAL(12,2) NOT NULL,
    estado ENUM('ACTIVO', 'INACTIVO') DEFAULT 'ACTIVO'
) ENGINE=InnoDB;

INSERT INTO empleados (id_empleado, nombre_completo, tipo_documento, numero_documento, telefono, direccion, correo, cargo, salario) VALUES
(1, 'Luz Marina Bermúdez', 'CC', '31892014', '3115678901', 'Calle 12 # 4-15', 'luz.bermudez@farmasoft.com', 'Administrador General', 4500000.00),
(2, 'Jonathan Smith Pérez', 'CC', '1115938201', '3174561230', 'Cra 15 # 22-08', 'jonathan.perez@farmasoft.com', 'Regente de Farmacia', 3200000.00),
(3, 'Sandra Patricia Osorio', 'CC', '66982014', '3148901234', 'Calle 40 # 18-90', 'sandra.osorio@farmasoft.com', 'Auxiliar de Farmacia', 1800000.00),
(4, 'Kevin Alexis Quintero', 'CC', '1112938471', '3201237894', 'Cra 27 # 8-33', 'kevin.quintero@farmasoft.com', 'Vendedor / Cajero', 1600000.00);

CREATE TABLE usuarios (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    id_empleado INT UNIQUE NOT NULL,
    id_rol INT NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    estado ENUM('ACTIVO', 'BLOQUEADO', 'INACTIVO') DEFAULT 'ACTIVO',
    FOREIGN KEY (id_empleado) REFERENCES empleados(id_empleado) ON DELETE CASCADE,
    FOREIGN KEY (id_rol) REFERENCES roles(id_rol)
) ENGINE=InnoDB;

INSERT INTO usuarios (id_empleado, id_rol, username, password_hash) VALUES
(1, 1, 'admin', '$2a$10$wT3yKkXG1z/9W8Z3E9s2.e.yG9v5uL7Qf1u1u1u1u1u1u1u1u1u1u'),
(2, 2, 'regente01', '$2a$10$wT3yKkXG1z/9W8Z3E9s2.e.yG9v5uL7Qf1u1u1u1u1u1u1u1u1u1u'),
(3, 4, 'sandra.aux', '$2a$10$wT3yKkXG1z/9W8Z3E9s2.e.yG9v5uL7Qf1u1u1u1u1u1u1u1u1u1u'),
(4, 3, 'kevin.vendedor', '$2a$10$wT3yKkXG1z/9W8Z3E9s2.e.yG9v5uL7Qf1u1u1u1u1u1u1u1u1u1u');

-- ------------------------------------------------------------
-- 4. TABLA PROVEEDORES
-- ------------------------------------------------------------
CREATE TABLE proveedores (
    id_proveedor INT AUTO_INCREMENT PRIMARY KEY,
    razon_social VARCHAR(150) NOT NULL,
    nit VARCHAR(20) NOT NULL UNIQUE,
    direccion VARCHAR(200),
    telefono VARCHAR(15),
    correo VARCHAR(100),
    nombre_contacto VARCHAR(100),
    tipo_productos VARCHAR(150),
    estado ENUM('ACTIVO', 'INACTIVO') DEFAULT 'ACTIVO'
) ENGINE=InnoDB;

INSERT INTO proveedores (id_proveedor, razon_social, nit, direccion, telefono, correo, nombre_contacto, tipo_productos) VALUES
(1, 'Laboratorios Procaps S.A.', '890200123-1', 'Calle 80 # 78B-11, Barranquilla', '6053718000', 'ventas@procaps.com.co', 'Lina Marcela Hoyos', 'Medicamentos Genéricos y Marca'),
(2, 'Genfar S.A.', '860005432-8', 'Transversal 23 # 97-73, Bogotá', '6016345000', 'pedidos@genfar.com.co', 'Roberto Gómez', 'Medicamentos Genéricos y Venta Libre'),
(3, 'Tecnoquímicas S.A. (TQ)', '890300220-4', 'Calle 23 N° 7-39, Cali', '6028825000', 'contacto@tq.com.co', 'Beatriz Elena Restrepo', 'Cuidado Personal, Bebés y Fármacos'),
(4, 'Bayer S.A. Colombia', '860001200-5', 'Carrera 58 # 11-84, Bogotá', '6014234000', 'servicio.cliente@bayer.com', 'Carlos Mario Silva', 'Especialidades Farmacéuticas');

-- ------------------------------------------------------------
-- 5. TABLA CATEGORIAS
-- ------------------------------------------------------------
CREATE TABLE categorias (
    id_categoria INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT
) ENGINE=InnoDB;

INSERT INTO categorias (id_categoria, nombre, descripcion) VALUES
(1, 'Analgésicos y Antiinflamatorios', 'Medicamentos para aliviar el dolor y reducir la inflamación'),
(2, 'Antibióticos', 'Fármacos de prescripción para combatir infecciones bacterianas'),
(3, 'Antialérgicos y Antihistamínicos', 'Tratamiento de alergias y congestión'),
(4, 'Cuidado Personal e Higiene', 'Productos para el aseo diario, jabones y cuidado corporal'),
(5, 'Cuidado Infantil y Bebés', 'Leches de fórmula, pañales y cremas para bebés'),
(6, 'Dermocosmética', 'Cuidado avanzado de la piel y protección solar'),
(7, 'Dispositivos Médicos y Primeros Auxilios', 'Termómetros, jeringas, curas, gasas y tensiómetros');

-- ------------------------------------------------------------
-- 6. TABLA PRODUCTOS
-- ------------------------------------------------------------
CREATE TABLE productos (
    id_producto INT AUTO_INCREMENT PRIMARY KEY,
    codigo_interno VARCHAR(30) UNIQUE NOT NULL,
    codigo_barras VARCHAR(50) UNIQUE NOT NULL,
    nombre_comercial VARCHAR(150) NOT NULL,
    nombre_generico VARCHAR(150),
    descripcion TEXT,
    presentacion VARCHAR(100),
    concentracion VARCHAR(50),
    laboratorio VARCHAR(100),
    registro_invima VARCHAR(50) NOT NULL,
    id_categoria INT NOT NULL,
    id_proveedor INT NOT NULL,
    precio_compra DECIMAL(12,2) NOT NULL,
    precio_venta DECIMAL(12,2) NOT NULL,
    stock_total INT DEFAULT 0,
    stock_minimo INT DEFAULT 10,
    ubicacion_estante VARCHAR(50),
    requiere_formula BOOLEAN DEFAULT FALSE,
    es_venta_libre BOOLEAN DEFAULT TRUE,
    es_controlado BOOLEAN DEFAULT FALSE,
    requiere_refrigeracion BOOLEAN DEFAULT FALSE,
    restricciones_venta VARCHAR(200),
    estado ENUM('ACTIVO', 'INACTIVO', 'DESCONTINUADO') DEFAULT 'ACTIVO',
    FOREIGN KEY (id_categoria) REFERENCES categorias(id_categoria),
    FOREIGN KEY (id_proveedor) REFERENCES proveedores(id_proveedor)
) ENGINE=InnoDB;

INSERT INTO productos (id_producto, codigo_interno, codigo_barras, nombre_comercial, nombre_generico, descripcion, presentacion, concentracion, laboratorio, registro_invima, id_categoria, id_proveedor, precio_compra, precio_venta, stock_total, stock_minimo, ubicacion_estante, requiere_formula, es_venta_libre, es_controlado, requiere_refrigeracion) VALUES
(1, 'PROD-001', '770200100101', 'Dolex Forte', 'Acetaminofén + Cafeína', 'Analgésico y antipirético para dolores intensos', 'Caja x 12 Tabletas', '500mg / 65mg', 'GSK / TQ', 'INVIMA 2018M-0001234', 1, 3, 4500.00, 8500.00, 150, 20, 'Estante A1', FALSE, TRUE, FALSE, FALSE),
(2, 'PROD-002', '770200100102', 'Amoxicilina Genfar', 'Amoxicilina', 'Antibiótico bactericida de amplio espectro', 'Caja x 50 Cápsulas', '500 mg', 'Genfar', 'INVIMA 2020M-0015678', 2, 2, 12000.00, 22000.00, 80, 15, 'Estante B2 (Restringido)', TRUE, FALSE, FALSE, FALSE),
(3, 'PROD-003', '770200100103', 'Apronax', 'Naproxeno Sódico', 'Antiinflamatorio no esteroideo de alivio prolongado', 'Caja x 10 Tabletas', '550 mg', 'Bayer', 'INVIMA 2019M-0009876', 1, 4, 15000.00, 26000.00, 45, 10, 'Estante A2', FALSE, TRUE, FALSE, FALSE),
(4, 'PROD-004', '770200100104', 'Loratadina Procaps', 'Loratadina', 'Antihistamínico no sedante', 'Caja x 10 Tabletas', '10 mg', 'Procaps', 'INVIMA 2021M-0011223', 3, 1, 3000.00, 7000.00, 120, 15, 'Estante C1', FALSE, TRUE, FALSE, FALSE),
(5, 'PROD-005', '770200100105', 'Bloqueador Umbrela Urban', 'Protector Solar FPS 50+', 'Protección dermatológica contra rayos UV y luz azul', 'Frasco x 50 ml', 'FPS 50+', 'Medihealth / TQ', 'NSOC12345-22CO', 6, 3, 38000.00, 62000.00, 25, 5, 'Vitrina Dermocosmética', FALSE, TRUE, FALSE, FALSE),
(6, 'PROD-006', '770200100106', 'Insulina Lantus', 'Insulina Glargina', 'Insulina de acción prolongada', 'Caja x 5 Plumas soloSTAR 3ml', '100 UI/ml', 'Sanofi', 'INVIMA 2017M-0004512', 2, 1, 110000.00, 165000.00, 12, 5, 'Nevera Principal (2-8°C)', TRUE, FALSE, FALSE, TRUE);

-- ------------------------------------------------------------
-- 7. TABLA LOTES Y CONTROL DE VENCIMIENTOS
-- ------------------------------------------------------------
CREATE TABLE lotes (
    id_lote INT AUTO_INCREMENT PRIMARY KEY,
    id_producto INT NOT NULL,
    numero_lote VARCHAR(50) NOT NULL,
    fecha_fabricacion DATE,
    fecha_vencimiento DATE NOT NULL,
    cantidad_inicial INT NOT NULL,
    cantidad_actual INT NOT NULL,
    estado ENUM('DISPONIBLE', 'PROXIMO_A_VENCER', 'VENCIDO', 'RETIRADO', 'DEVUELTO') DEFAULT 'DISPONIBLE',
    FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
) ENGINE=InnoDB;

INSERT INTO lotes (id_producto, numero_lote, fecha_fabricacion, fecha_vencimiento, cantidad_inicial, cantidad_actual, estado) VALUES
(1, 'LOTE-DOL-2025-A', '2025-01-10', '2027-01-10', 100, 100, 'DISPONIBLE'),
(1, 'LOTE-DOL-2024-B', '2024-05-15', '2026-05-15', 50, 50, 'DISPONIBLE'),
(2, 'LOTE-AMX-9988', '2024-08-01', '2026-08-01', 80, 80, 'DISPONIBLE'),
(3, 'LOTE-APR-1022', '2024-11-20', '2026-11-20', 45, 45, 'DISPONIBLE'),
(4, 'LOTE-LOR-5541', '2025-02-01', '2027-02-01', 120, 120, 'DISPONIBLE'),
(5, 'LOTE-UMB-0034', '2024-10-10', '2026-10-10', 25, 25, 'DISPONIBLE'),
(6, 'LOTE-INS-8812', '2025-03-01', '2026-09-01', 12, 12, 'DISPONIBLE');

-- ------------------------------------------------------------
-- 8. TABLA COMPRAS (A PROVEEDORES)
-- ------------------------------------------------------------
CREATE TABLE compras (
    id_compra INT AUTO_INCREMENT PRIMARY KEY,
    numero_factura_proveedor VARCHAR(50) NOT NULL,
    id_proveedor INT NOT NULL,
    id_empleado INT NOT NULL,
    fecha_compra DATETIME DEFAULT CURRENT_TIMESTAMP,
    subtotal DECIMAL(12,2) NOT NULL,
    impuestos DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total DECIMAL(12,2) NOT NULL,
    forma_pago ENUM('EFECTIVO', 'TRANSFERENCIA', 'CREDITO_PROVEEDOR') DEFAULT 'CREDITO_PROVEEDOR',
    estado ENUM('PENDIENTE', 'RECIBIDA', 'PARCIALMENTE_RECIBIDA', 'CANCELADA', 'DEVUELTA') DEFAULT 'RECIBIDA',
    FOREIGN KEY (id_proveedor) REFERENCES proveedores(id_proveedor),
    FOREIGN KEY (id_empleado) REFERENCES empleados(id_empleado)
) ENGINE=InnoDB;

INSERT INTO compras (numero_factura_proveedor, id_proveedor, id_empleado, fecha_compra, subtotal, impuestos, total, forma_pago, estado) VALUES
('FAC-TQ-90812', 3, 1, '2026-07-01 10:30:00', 1625000.00, 308750.00, 1933750.00, 'CREDITO_PROVEEDOR', 'RECIBIDA'),
('FAC-GEN-11204', 2, 2, '2026-07-15 14:15:00', 960000.00, 182400.00, 1142400.00, 'TRANSFERENCIA', 'RECIBIDA');

CREATE TABLE detalles_compras (
    id_detalle_compra INT AUTO_INCREMENT PRIMARY KEY,
    id_compra INT NOT NULL,
    id_producto INT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(12,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    FOREIGN KEY (id_compra) REFERENCES compras(id_compra) ON DELETE CASCADE,
    FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
) ENGINE=InnoDB;

INSERT INTO detalles_compras (id_compra, id_producto, cantidad, precio_unitario, subtotal) VALUES
(1, 1, 150, 4500.00, 675000.00),
(1, 5, 25, 38000.00, 950000.00),
(2, 2, 80, 12000.00, 960000.00);

-- ------------------------------------------------------------
-- 9. TABLA VENTAS
-- ------------------------------------------------------------
CREATE TABLE ventas (
    id_venta INT AUTO_INCREMENT PRIMARY KEY,
    numero_factura VARCHAR(50) UNIQUE NOT NULL,
    id_cliente INT,
    id_empleado INT NOT NULL,
    fecha_venta DATETIME DEFAULT CURRENT_TIMESTAMP,
    subtotal DECIMAL(12,2) NOT NULL,
    descuento DECIMAL(12,2) DEFAULT 0.00,
    impuesto_iva DECIMAL(12,2) DEFAULT 0.00,
    total DECIMAL(12,2) NOT NULL,
    metodo_pago ENUM('EFECTIVO', 'TARJETA_DEBITO', 'TARJETA_CREDITO', 'TRANSFERENCIA', 'NEQUI_DAVIPLATA', 'PAGO_MIXTO') DEFAULT 'EFECTIVO',
    estado ENUM('PENDIENTE', 'PAGADA', 'ANULADA', 'DEVUELTA') DEFAULT 'PAGADA',
    FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente),
    FOREIGN KEY (id_empleado) REFERENCES empleados(id_empleado)
) ENGINE=InnoDB;

INSERT INTO ventas (numero_factura, id_cliente, id_empleado, fecha_venta, subtotal, descuento, impuesto_iva, total, metodo_pago, estado) VALUES
('FARM-00001', 1, 4, '2026-07-27 09:10:00', 34500.00, 0.00, 0.00, 34500.00, 'NEQUI_DAVIPLATA', 'PAGADA'),
('FARM-00002', 2, 4, '2026-07-27 10:25:00', 22000.00, 0.00, 0.00, 22000.00, 'EFECTIVO', 'PAGADA');

CREATE TABLE detalles_ventas (
    id_detalle_venta INT AUTO_INCREMENT PRIMARY KEY,
    id_venta INT NOT NULL,
    id_producto INT NOT NULL,
    id_lote INT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(12,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    FOREIGN KEY (id_venta) REFERENCES ventas(id_venta) ON DELETE CASCADE,
    FOREIGN KEY (id_producto) REFERENCES productos(id_producto),
    FOREIGN KEY (id_lote) REFERENCES lotes(id_lote)
) ENGINE=InnoDB;

INSERT INTO detalles_ventas (id_venta, id_producto, id_lote, cantidad, precio_unitario, subtotal) VALUES
(1, 1, 1, 1, 8500.00, 8500.00),
(1, 3, 4, 1, 26000.00, 26000.00),
(2, 2, 3, 1, 22000.00, 22000.00);

-- ------------------------------------------------------------
-- 10. TABLA FORMULAS MEDICAS (Para medicamentos que la exigen)
-- ------------------------------------------------------------
CREATE TABLE formulas_medicas (
    id_formula INT AUTO_INCREMENT PRIMARY KEY,
    id_cliente INT NOT NULL,
    nombre_medico VARCHAR(150) NOT NULL,
    tarjeta_profesional VARCHAR(50) NOT NULL,
    entidad_salud VARCHAR(100),
    fecha_expedicion DATE NOT NULL,
    vigencia_dias INT DEFAULT 30,
    archivo_adjunto_url VARCHAR(255),
    observaciones TEXT,
    FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente)
) ENGINE=InnoDB;

INSERT INTO formulas_medicas (id_formula, id_cliente, nombre_medico, tarjeta_profesional, entidad_salud, fecha_expedicion, vigencia_dias, observaciones) VALUES
(1, 2, 'Dr. Fernando Arango', 'RM-45892-VALLE', 'Clínica Imbanaco', '2026-07-26', 15, 'Tomar Amoxicilina cada 8 horas por 7 días');

CREATE TABLE detalles_formulas (
    id_detalle_formula INT AUTO_INCREMENT PRIMARY KEY,
    id_formula INT NOT NULL,
    id_producto INT NOT NULL,
    dosis VARCHAR(100) NOT NULL,
    frecuencia VARCHAR(100) NOT NULL,
    duracion_tratamiento VARCHAR(100) NOT NULL,
    FOREIGN KEY (id_formula) REFERENCES formulas_medicas(id_formula) ON DELETE CASCADE,
    FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
) ENGINE=InnoDB;

INSERT INTO detalles_formulas (id_formula, id_producto, dosis, frecuencia, duracion_tratamiento) VALUES
(1, 2, '1 Cápsula 500mg', 'Cada 8 horas', '7 días');

-- ------------------------------------------------------------
-- 11. MOVIMIENTOS DE INVENTARIO (Auditoría)
-- ------------------------------------------------------------
CREATE TABLE movimientos_inventario (
    id_movimiento INT AUTO_INCREMENT PRIMARY KEY,
    tipo_movimiento ENUM('ENTRADA_COMPRA', 'SALIDA_VENTA', 'AJUSTE_PERDIDA', 'AJUSTE_DANIO', 'DEVOLUCION_CLIENTE', 'DEVOLUCION_PROVEEDOR', 'TRASLADO_ENTRADA', 'TRASLADO_SALIDA') NOT NULL,
    id_producto INT NOT NULL,
    id_lote INT,
    cantidad INT NOT NULL,
    existencia_anterior INT NOT NULL,
    nueva_existencia INT NOT NULL,
    fecha_movimiento DATETIME DEFAULT CURRENT_TIMESTAMP,
    id_usuario INT NOT NULL,
    motivo VARCHAR(255),
    FOREIGN KEY (id_producto) REFERENCES productos(id_producto),
    FOREIGN KEY (id_lote) REFERENCES lotes(id_lote),
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
) ENGINE=InnoDB;

INSERT INTO movimientos_inventario (tipo_movimiento, id_producto, id_lote, cantidad, existence_anterior, nueva_existencia, id_usuario, motivo) VALUES
('ENTRADA_COMPRA', 1, 1, 100, 0, 100, 1, 'Carga inicial por compra FAC-TQ-90812'),
('SALIDA_VENTA', 1, 1, 1, 100, 99, 4, 'Venta según Factura FARM-00001');

-- ------------------------------------------------------------
-- 12. TABLA DOMICILIOS
-- ------------------------------------------------------------
CREATE TABLE domicilios (
    id_domicilio INT AUTO_INCREMENT PRIMARY KEY,
    id_venta INT NOT NULL UNIQUE,
    id_cliente INT NOT NULL,
    direccion_entrega VARCHAR(200) NOT NULL,
    telefono_contacto VARCHAR(15) NOT NULL,
    nombre_domiciliario VARCHAR(100),
    costo_domicilio DECIMAL(10,2) DEFAULT 0.00,
    estado ENUM('PENDIENTE', 'EN_PREPARACION', 'EN_CAMINO', 'ENTREGADO', 'CANCELADO') DEFAULT 'PENDIENTE',
    fecha_hora_salida DATETIME NULL,
    fecha_hora_entrega DATETIME NULL,
    FOREIGN KEY (id_venta) REFERENCES ventas(id_venta),
    FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente)
) ENGINE=InnoDB;

INSERT INTO domicilios (id_venta, id_cliente, direccion_entrega, telefono_contacto, nombre_domiciliario, costo_domicilio, estado) VALUES
(1, 1, 'Calle 25 # 14-30 Apt 302', '3157894512', 'Marmato Motos Express', 4000.00, 'ENTREGADO');

-- ------------------------------------------------------------
-- 13. TABLA DEVOLUCIONES
-- ------------------------------------------------------------
CREATE TABLE devoluciones (
    id_devolucion INT AUTO_INCREMENT PRIMARY KEY,
    tipo_devolucion ENUM('CLIENTE', 'PROVEEDOR') NOT NULL,
    id_venta INT NULL,
    id_compra INT NULL,
    id_producto INT NOT NULL,
    cantidad INT NOT NULL,
    motivo ENUM('PRODUCTO_DEFECTUOSO', 'ERROR_ENTREGA', 'PROXIMO_A_VENCER', 'VENCIDO', 'RETIRO_MERCADO', 'EMPAQUE_DANADO') NOT NULL,
    estado_producto ENUM('APTO_PARA_REINGRESO', 'DESECHADO', 'DEVUELTO_A_FABRICA') NOT NULL,
    fecha_devolucion DATETIME DEFAULT CURRENT_TIMESTAMP,
    id_usuario_regente INT NOT NULL,
    observaciones TEXT,
    FOREIGN KEY (id_venta) REFERENCES ventas(id_venta),
    FOREIGN KEY (id_compra) REFERENCES compras(id_compra),
    FOREIGN KEY (id_producto) REFERENCES productos(id_producto),
    FOREIGN KEY (id_usuario_regente) REFERENCES usuarios(id_usuario)
) ENGINE=InnoDB;
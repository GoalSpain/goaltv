<?php
header('Content-Type: application/json');

$host = 'localhost';
$db   = 'panel_app';
$user = 'admin_app';
$pass = 'q7Un8%Yu^KN4X!';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$db;charset=utf8mb4", $user, $pass);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch (PDOException $e) {
    die(json_encode(['error' => 'Error de servidor']));
}

$device_id  = trim($_POST['device_id'] ?? '');
$modelo     = trim($_POST['modelo'] ?? 'Desconocido');
$os         = trim($_POST['os'] ?? 'Desconocido');
$nombre_app = trim($_POST['nombre_app'] ?? 'poltivi');
$ip         = $_SERVER['HTTP_CF_CONNECTING_IP'] ?? $_SERVER['HTTP_X_FORWARDED_FOR'] ?? $_SERVER['REMOTE_ADDR'];

if (empty($device_id) || strlen($device_id) > 128) {
    die(json_encode(['error' => 'ID inválido']));
}

if (!in_array($nombre_app, ['poltivi', 'goal_tv'])) {
    $nombre_app = 'poltivi';
}

// GoalTV es premium por defecto. PolTivi evaluará las 72h o el estatus manual.
$es_premium_default = ($nombre_app === 'goal_tv') ? 1 : 0;

$sql = "INSERT INTO dispositivos_app (device_id, modelo_dispositivo, version_os, ip_actual, nombre_app, es_premium, fecha_instalacion) 
        VALUES (:id, :modelo, :os, :ip, :app, :def_premium, NOW())
        ON DUPLICATE KEY UPDATE 
        modelo_dispositivo = :modelo, 
        version_os = :os, 
        ip_actual = :ip, 
        nombre_app = :app,
        ultima_conexion = NOW()";

$stmt = $pdo->prepare($sql);
$stmt->execute([
    ':id'          => $device_id,
    ':modelo'      => htmlspecialchars($modelo),
    ':os'          => htmlspecialchars($os),
    ':ip'          => $ip,
    ':app'         => $nombre_app,
    ':def_premium' => $es_premium_default
]);

// Consultar estado actual y evaluar si está dentro de las primeras 72 horas
$stmt = $pdo->prepare("SELECT es_premium, bloqueado, fecha_instalacion FROM dispositivos_app WHERE device_id = :id");
$stmt->execute([':id' => $device_id]);
$estado = $stmt->fetch(PDO::FETCH_ASSOC);

$es_premium = (bool)$estado['es_premium'];
$bloqueado  = (bool)$estado['bloqueado'];

// Si es PolTivi y no está marcado como premium manual, comprobamos si pasaron las 72 horas desde la instalación
if ($nombre_app === 'poltivi' && !$es_premium) {
    $fecha_instalacion = strtotime($estado['fecha_instalacion']);
    $horas_transcurridas = (time() - $fecha_instalacion) / 3600;
    if ($horas_transcurridas <= 72) {
        $es_premium = true; // Activo temporalmente por periodo de prueba de 72h
    }
}

echo json_encode([
    'status'     => 'ok',
    'device_id'  => $device_id,
    'es_premium' => $es_premium,
    'bloqueado'  => $bloqueado
]);
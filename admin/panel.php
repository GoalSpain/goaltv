<?php
ini_set('display_errors', 0);
error_reporting(0);

ini_set('session.cookie_httponly', 1);
ini_set('session.use_strict_mode', 1);
session_start();

$ADMIN_USER = 'Antonioni';
$ADMIN_PASS = '5B*V4a8zZX4#!9';

if (!isset($_SESSION['intentos'])) { $_SESSION['intentos'] = 0; }
if ($_SESSION['intentos'] >= 5) { die("Acceso bloqueado temporalmente."); }

if (isset($_POST['login'])) {
    if (hash_equals($ADMIN_USER, $_POST['usuario'] ?? '') && hash_equals($ADMIN_PASS, $_POST['password'] ?? '')) {
        session_regenerate_id(true);
        $_SESSION['admin_logged'] = true;
        $_SESSION['intentos'] = 0;
        header("Location: panel.php");
        exit;
    } else {
        $_SESSION['intentos']++;
        $error = "Credenciales incorrectas";
    }
}

if (isset($_GET['logout'])) { session_destroy(); header("Location: panel.php"); exit; }

if (empty($_SESSION['admin_logged'])): ?>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Acceso Admin PolTivi</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-dark text-light d-flex align-items-center justify-content-center" style="height: 100vh;">
    <div class="card bg-secondary text-white p-4 shadow" style="width: 350px;">
        <h4 class="mb-3 text-center">Panel Admin</h4>
        <?php if (!empty($error)) echo "<div class='alert alert-danger p-2'>$error</div>"; ?>
        <form method="POST">
            <div class="mb-3"><label>Usuario</label><input type="text" name="usuario" class="form-control" required></div>
            <div class="mb-3"><label>Contraseña</label><input type="password" name="password" class="form-control" required></div>
            <button type="submit" name="login" class="btn btn-primary w-100">Entrar</button>
        </form>
    </div>
</body>
</html>
<?php exit; endif;

try {
    $pdo = new PDO("mysql:host=localhost;dbname=panel_app;charset=utf8mb4", "admin_app", "q7Un8%Yu^KN4X!");
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch (PDOException $e) { die("Error DB"); }

if (isset($_POST['accion'])) {
    $id_disp = (int)$_POST['id_dispositivo'];
    $app_activa = $_POST['app_filtro'] ?? 'poltivi';

    if ($_POST['accion'] === 'toggle_premium' && $app_activa !== 'goal_tv') {
        $pdo->prepare("UPDATE dispositivos_app SET es_premium = NOT es_premium WHERE id = ?")->execute([$id_disp]);
    } elseif ($_POST['accion'] === 'eliminar') {
        $pdo->prepare("DELETE FROM dispositivos_app WHERE id = ?")->execute([$id_disp]);
    } elseif ($_POST['accion'] === 'guardar_nota') {
        $nota = trim($_POST['notas'] ?? '');
        $pdo->prepare("UPDATE dispositivos_app SET notas = ? WHERE id = ?")->execute([$nota, $id_disp]);
    }
    header("Location: panel.php?app=" . $app_activa);
    exit;
}

$app_filtro = $_GET['app'] ?? 'poltivi';
if (!in_array($app_filtro, ['poltivi', 'goal_tv'])) { $app_filtro = 'poltivi'; }

$stmt = $pdo->prepare("SELECT * FROM dispositivos_app WHERE nombre_app = ? ORDER BY ultima_conexion DESC");
$stmt->execute([$app_filtro]);
$dispositivos = $stmt->fetchAll(PDO::FETCH_ASSOC);

$count_poltivi = $pdo->query("SELECT COUNT(*) FROM dispositivos_app WHERE nombre_app = 'poltivi'")->fetchColumn();
$count_goal = $pdo->query("SELECT COUNT(*) FROM dispositivos_app WHERE nombre_app = 'goal_tv'")->fetchColumn();
?>
<!DOCTYPE html>
<html lang="es" class="dark">
<head>
    <meta charset="UTF-8">
    <title>Panel de Control - PolTivi & GoalTV</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body { background-color: #030816; color: #f1f5f9; }
        .card-custom { background: #0a122c; border: 1px solid rgba(255,255,255,0.08); }
        .os-text { color: #cbd5e1 !important; } 
    </style>
</head>
<body class="p-4">
<div class="container card-custom p-4 rounded shadow-lg">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h2>Panel de Gestión Central</h2>
        <a href="?logout=1" class="btn btn-outline-danger btn-sm">Cerrar Sesión</a>
    </div>

    <ul class="nav nav-tabs mb-4 border-bottom border-secondary">
        <li class="nav-item">
            <a class="nav-link text-light <?= $app_filtro === 'poltivi' ? 'active bg-primary text-white fw-bold' : '' ?>" href="?app=poltivi">
                PolTivi <span class="badge bg-dark ms-1"><?= $count_poltivi ?></span>
            </a>
        </li>
        <li class="nav-item">
            <a class="nav-link text-light <?= $app_filtro === 'goal_tv' ? 'active bg-success text-white fw-bold' : '' ?>" href="?app=goal_tv">
                GoalTV <span class="badge bg-dark ms-1"><?= $count_goal ?></span>
            </a>
        </li>
    </ul>

    <div class="table-responsive">
        <table class="table table-dark table-hover align-middle">
            <thead>
                <tr>
                    <th>ID Dispositivo</th>
                    <th>Modelo / OS</th>
                    <th>1ª Instalación</th>
                    <th>IP Actual</th>
                    <th>Notas / Cliente</th>
                    <?php if ($app_filtro === 'poltivi'): ?>
                        <th>Estado</th>
                    <?php endif; ?>
                    <th>Acciones</th>
                </tr>
            </thead>
            <tbody>
                <?php if (empty($dispositivos)): ?>
                <tr><td colspan="<?= $app_filtro === 'poltivi' ? '7' : '6' ?>" class="text-center text-muted py-4">No hay dispositivos registrados.</td></tr>
                <?php endif; ?>
                <?php foreach ($dispositivos as $d): 
                    $fecha_formateada = !empty($d['fecha_instalacion']) ? date('d/m/Y H:i', strtotime($d['fecha_instalacion'])) : '-';
                ?>
                <tr>
                    <td><code><?= htmlspecialchars($d['device_id']) ?></code></td>
                    <td>
                        <strong class="text-white"><?= htmlspecialchars($d['modelo_dispositivo']) ?></strong><br>
                        <small class="os-text">Android <?= htmlspecialchars($d['version_os']) ?></small>
                    </td>
                    <td><small class="os-text"><?= $fecha_formateada ?></small></td>
                    <td><span class="text-white"><?= htmlspecialchars($d['ip_actual']) ?></span></td>
                    <td>
                        <form method="POST" class="d-flex gap-1">
                            <input type="hidden" name="id_dispositivo" value="<?= $d['id'] ?>">
                            <input type="hidden" name="app_filtro" value="<?= $app_filtro ?>">
                            <input type="text" name="notas" value="<?= htmlspecialchars($d['notas'] ?? '') ?>" placeholder="Nota..." class="form-control form-control-sm bg-dark text-light border-secondary" style="width: 130px;">
                            <button type="submit" name="accion" value="guardar_nota" class="btn btn-sm btn-outline-light" title="Guardar">💾</button>
                        </form>
                    </td>
                    <?php if ($app_filtro === 'poltivi'): ?>
                    <td>
                        <?php if ($d['es_premium']): ?>
                            <span class="badge bg-success">PREMIUM</span>
                        <?php else: ?>
                            <span class="badge bg-secondary">GRATIS</span>
                        <?php endif; ?>
                    </td>
                    <?php endif; ?>
                    <td>
                        <form method="POST" class="d-inline">
                            <input type="hidden" name="id_dispositivo" value="<?= $d['id'] ?>">
                            <input type="hidden" name="app_filtro" value="<?= $app_filtro ?>">
                            <?php if ($app_filtro === 'poltivi'): ?>
                                <button type="submit" name="accion" value="toggle_premium" class="btn btn-sm <?= $d['es_premium'] ? 'btn-warning' : 'btn-success' ?>">
                                    <?= $d['es_premium'] ? 'Quitar Premium' : 'Hacer Premium' ?>
                                </button>
                            <?php else: ?>
                                <span class="badge bg-success px-2 py-1">PREMIUM (DEF)</span>
                            <?php endif; ?>
                            <button type="submit" name="accion" value="eliminar" class="btn btn-sm btn-danger ms-1" onclick="return confirm('¿Eliminar registro?');">✕</button>
                        </form>
                    </td>
                </tr>
                <?php endforeach; ?>
            </tbody>
        </table>
    </div>
</div>
</body>
</html>
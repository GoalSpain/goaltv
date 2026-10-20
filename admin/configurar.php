<?php
ini_set('display_errors', 0);
error_reporting(0);

$mensaje = "";
$usuario_val = "";
$password_val = "";
$dispositivo_encontrado = false;
$guardado_exitoso = false;

try {
    $pdo = new PDO("mysql:host=localhost;dbname=panel_app;charset=utf8mb4", "admin_app", "q7Un8%Yu^KN4X!");
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch (PDOException $e) {
    die("Error de conexión con la base de datos: " . $e->getMessage());
}

$device_id = trim($_GET['device_id'] ?? $_GET['id'] ?? $_POST['device_id'] ?? '');

if (!empty($device_id)) {
    $stmt = $pdo->prepare("SELECT usuario_remoto, password_remoto FROM dispositivos_app WHERE device_id = ? AND nombre_app = 'goal_tv'");
    $stmt->execute([$device_id]);
    $res = $stmt->fetch(PDO::FETCH_ASSOC);

    if ($res) {
        $dispositivo_encontrado = true;
        $usuario_val = (string)($res['usuario_remoto'] ?? '');
        $password_val = (string)($res['password_remoto'] ?? '');
    } else {
        $mensaje = "El ID de dispositivo no está registrado en GoalTV.";
    }
}

if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['guardar']) && !empty($device_id)) {
    $usuario_val = trim($_POST['usuario_remoto'] ?? '');
    $password_val = trim($_POST['password_remoto'] ?? '');
    
    $stmt = $pdo->prepare("UPDATE dispositivos_app SET usuario_remoto = ?, password_remoto = ? WHERE device_id = ? AND nombre_app = 'goal_tv'");
    $stmt->execute([$usuario_val, $password_val, $device_id]);
    $guardado_exitoso = true;
    $mensaje = "¡Datos enviados con éxito!";
}
?>
<!DOCTYPE html>
<html lang="es" class="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Configuración GoalTV - Acceso Remoto</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800;900&display=swap" rel="stylesheet">
    <script>
        tailwind.config = {
            darkMode: 'class',
            theme: {
                extend: {
                    colors: {
                        goaltv: {
                            bg: '#0b0f19',
                            card: '#161d31',
                            border: '#2a3655',
                            orange: '#ff6600',
                            orangeHover: '#e65c00',
                            green: '#00e676'
                        }
                    },
                    fontFamily: { sans: ['Inter', 'sans-serif'] }
                }
            }
        }
    </script>
    <style>
        body { background-color: #0b0f19; color: #f1f5f9; font-family: 'Inter', sans-serif; }
        .glass-card { background: #161d31; border: 1px solid #2a3655; }
        .btn-orange { background: linear-gradient(135deg, #ff6600 0%, #ff8533 100%); transition: all 0.2s ease; }
        .btn-orange:hover { background: linear-gradient(135deg, #e65c00 0%, #ff6600 100%); transform: scale(1.01); }
    </style>
</head>
<body class="min-h-screen flex items-center justify-center p-4">
    <div class="w-full max-w-md">
        <div class="glass-card p-8 rounded-2xl shadow-2xl space-y-6">
            
            <div class="text-center space-y-3">
                <div class="flex justify-center mb-2">
                    <img src="logo.png" alt="GoalTV" class="h-10 object-contain drop-shadow-md">
                </div>
                <h2 class="text-2xl font-black text-white tracking-tight">Acceso <span class="text-orange-500">Remoto</span></h2>
                <p class="text-slate-400 text-xs">Introduce tus datos de acceso para la aplicación.</p>
            </div>

            <?php if (!empty($mensaje)): ?>
                <div class="bg-goaltv-green/10 border border-goaltv-green/30 text-goaltv-green text-xs py-2.5 px-4 rounded-xl text-center font-medium">
                    <?= htmlspecialchars($mensaje) ?>
                </div>
            <?php endif; ?>

            <?php if ($guardado_exitoso): ?>
                <script>
                    setTimeout(function() {
                        window.location.href = "goaltv://configuracion-guardada?status=ok";
                    }, 1500);
                </script>
                <div class="text-center space-y-3 py-4">
                    <i class="fa-solid fa-circle-check text-orange-500 text-4xl"></i>
                    <p class="text-sm text-slate-200">¡Datos enviados correctamente!<br>Regresando a la aplicación...</p>
                </div>
            <?php elseif ($dispositivo_encontrado): ?>
                <form method="POST" class="space-y-4">
                    <input type="hidden" name="device_id" value="<?= htmlspecialchars($device_id) ?>">
                    
                    <div>
                        <label class="block text-xs font-semibold text-slate-300 mb-1">ID de Dispositivo (GoalTV)</label>
                        <input type="text" value="<?= htmlspecialchars($device_id) ?>" class="w-full bg-[#0b0f19] text-slate-400 border border-[#2a3655] rounded-xl px-4 py-2.5 text-xs font-mono" disabled>
                    </div>

                    <div>
                        <label class="block text-xs font-semibold text-slate-300 mb-1">Usuario</label>
                        <input type="text" name="usuario_remoto" value="<?= htmlspecialchars($usuario_val) ?>" class="w-full bg-[#0b0f19] text-slate-100 border border-[#2a3655] focus:border-orange-500 rounded-xl px-4 py-2.5 text-sm focus:outline-none" placeholder="Introduce tu usuario...">
                    </div>

                    <div>
                        <label class="block text-xs font-semibold text-slate-300 mb-1">Contraseña</label>
                        <input type="text" name="password_remoto" value="<?= htmlspecialchars($password_val) ?>" class="w-full bg-[#0b0f19] text-slate-100 border border-[#2a3655] focus:border-orange-500 rounded-xl px-4 py-2.5 text-sm focus:outline-none" placeholder="Introduce tu contraseña...">
                    </div>

                    <button type="submit" name="guardar" class="w-full btn-orange py-3.5 text-white font-bold text-sm rounded-xl shadow-lg flex items-center justify-center gap-2">
                        <i class="fa-solid fa-paper-plane"></i> Enviar a GoalTV
                    </button>
                </form>
            <?php else: ?>
                <div class="text-center text-red-400 text-xs py-4">
                    No se ha podido identificar el dispositivo en GoalTV o no se ha proporcionado un ID válido.
                </div>
            <?php endif; ?>

        </div>
    </div>
</body>
</html>
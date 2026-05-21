<?php
// Autorise les requêtes JSON
header('Content-Type: application/json');

// Paramètres de connexion
$host   = "localhost";
$dbName = "track_project";
$user   = "root";
$pass   = "";

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbName;charset=utf8mb4", $user, $pass);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(["ok" => false, "msg" => "Connexion échouée"]);
    exit;
}

// Récupération et validation des paramètres POST
$lat    = $_POST['latitude']    ?? null;
$lon    = $_POST['longitude']   ?? null;
$dev    = $_POST['device_id']   ?? null;
$recAt  = $_POST['recorded_at'] ?? date('Y-m-d H:i:s');

if ($lat === null || $lon === null || $dev === null) {
    echo json_encode(["ok" => false, "msg" => "Paramètres manquants"]);
    exit;
}

try {
    // Requête préparée → protège contre les injections SQL
    $stmt = $pdo->prepare(
        "INSERT INTO gps_points (latitude, longitude, device_id, recorded_at)
         VALUES (:lat, :lon, :dev, :rec)"
    );
    $stmt->execute([
        ':lat' => $lat,
        ':lon' => $lon,
        ':dev' => $dev,
        ':rec' => $recAt,
    ]);

    echo json_encode(["ok" => true, "msg" => "Point enregistré"]);
} catch (PDOException $e) {
    echo json_encode(["ok" => false, "msg" => $e->getMessage()]);
}
?>
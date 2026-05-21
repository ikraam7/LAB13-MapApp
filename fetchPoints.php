<?php
header('Content-Type: application/json');

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

try {
    // Récupère tous les points, du plus récent au plus ancien
    $stmt = $pdo->prepare("SELECT * FROM gps_points ORDER BY recorded_at DESC");
    $stmt->execute();
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

    // Renvoie le tableau sous la clé "points" (attendue par Android)
    echo json_encode(["ok" => true, "points" => $rows]);
} catch (PDOException $e) {
    echo json_encode(["ok" => false, "msg" => $e->getMessage()]);
}
?>
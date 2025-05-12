# Script simplifié pour installer les JARs dans Maven

# Définir les chemins des fichiers JAR
$jarFiles = @(
    @{file = "C:\Users\gbujon\Desktop\P8-TourGuide\TourGuide\libs\gpsUtil.jar"; groupId = "local.lib"; artifactId = "gpsUtil"; version = "1.0.0"},
    @{file = "C:\Users\gbujon\Desktop\P8-TourGuide\TourGuide\libs\RewardCentral.jar"; groupId = "local.lib"; artifactId = "RewardCentral"; version = "1.0.0"},
    @{file = "C:\Users\gbujon\Desktop\P8-TourGuide\TourGuide\libs\TripPricer.jar"; groupId = "local.lib"; artifactId = "TripPricer"; version = "1.0.0"}
)

Write-Host "Début de l'installation des dépendances Maven"

foreach ($jar in $jarFiles) {
    Write-Host "Installation de $($jar.artifactId)-$($jar.version)..."
    
    if (Test-Path $jar.file) {
        $mvnCommand = "mvn install:install-file " +
                      "-Dfile=`"$($jar.file)`" " +
                      "-DgroupId=`"$($jar.groupId)`" " +
                      "-DartifactId=`"$($jar.artifactId)`" " +
                      "-Dversion=`"$($jar.version)`" " +
                      "-Dpackaging=jar"

        Write-Host "Commande: $mvnCommand"
        
        Invoke-Expression $mvnCommand

        if ($LASTEXITCODE -eq 0) {
            Write-Host "Installation réussie de $($jar.artifactId)"
        } else {
            Write-Host "Erreur lors de l'installation de $($jar.artifactId)"
        }
    } else {
        Write-Host "Fichier introuvable: $($jar.file)"
    }
    Write-Host ""
}

Write-Host "Opération terminée"
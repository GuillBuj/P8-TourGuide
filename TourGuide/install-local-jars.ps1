# Script simplifié pour installer les JARs dans Maven (avec chemins relatifs)

# Chemin de base relatif (ici : dossier "libs" dans le dossier courant)
$basePath = Join-Path -Path (Get-Location) -ChildPath "libs"

# Définir les fichiers JAR avec leurs métadonnées
$jarFiles = @(
    @{file = "gpsUtil.jar"; groupId = "local.lib"; artifactId = "gpsUtil"; version = "1.0.0"},
    @{file = "RewardCentral.jar"; groupId = "local.lib"; artifactId = "RewardCentral"; version = "1.0.0"},
    @{file = "TripPricer.jar"; groupId = "local.lib"; artifactId = "TripPricer"; version = "1.0.0"}
)

Write-Host "Début de l'installation des dépendances Maven"

foreach ($jar in $jarFiles) {
    $fullPath = Join-Path -Path $basePath -ChildPath $jar.file
    Write-Host "Installation de $($jar.artifactId)-$($jar.version)..."

    if (Test-Path $fullPath) {
        $mvnCommand = "mvn install:install-file " +
                      "-Dfile=`"$fullPath`" " +
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
        Write-Host "Fichier introuvable: $fullPath"
    }
    Write-host ""
}

Write-Host "Opération terminée"
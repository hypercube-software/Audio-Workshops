function Get-JavaBinary {
    if (-not $env:JAVA_HOME) {
        Write-Host "ERROR: JAVA_HOME is not defined." -ForegroundColor Red
        exit 1
    }

    # Join-Path creates backslashes, so we flip them to forward slashes
    $javaExe = (Join-Path $env:JAVA_HOME "bin\java.exe").Replace('\', '/')

    if (-not (Test-Path $javaExe)) {
        Write-Host "ERROR: java.exe not found at $javaExe" -ForegroundColor Red
        exit 1
    }

    return $javaExe
}

function Get-TargetJar {
    $searchPath = Join-Path $PSScriptRoot "..\target\midi-workshop-*-SNAPSHOT.jar"
    $jarItem = Get-ChildItem $searchPath | Select-Object -First 1

    if (-not $jarItem) {
        Write-Host "ERROR: JAR file not found in target directory." -ForegroundColor Red
        exit 1
    }

    # Convert the absolute path to forward slashes
    return $jarItem.FullName.Replace('\', '/')
}

function Run-Java {
    param(
        [string[]]$javaArgs,
        [string[]]$appArgs
    )

    $javaExe = Get-JavaBinary
    $jarFile = Get-TargetJar

    # Replace backslashes in appArgs too (in case paths are passed as arguments)
    $normalizedAppArgs = $appArgs | ForEach-Object { $_.Replace('\', '/') }

    # Build JVM arguments
    $allJavaArgs = @("-XX:+UseZGC", "-jar", $jarFile)
    if ($javaArgs) { $allJavaArgs += $javaArgs }

    # Display execution details
    Write-Host "`n--- Java Execution Details (Normalized Paths) ---" -ForegroundColor Yellow
    Write-Host "Java Binary : $javaExe"
    Write-Host "Target JAR  : $jarFile"
    Write-Host "JVM Args    : $($allJavaArgs -join ' ')"
    Write-Host "App Args    : $($normalizedAppArgs -join ' ')"
    Write-Host "------------------------------------------------`n" -ForegroundColor Yellow

    # Execute Java
    & $javaExe $allJavaArgs $normalizedAppArgs
}

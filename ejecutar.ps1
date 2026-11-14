<#
.SYNOPSIS
    Compila el prototipo, abre un emulador Android, instala la app y la lanza.

.PARAMETER Avd
    Nombre del emulador (AVD). Por defecto usa el primero que exista (p. ej. Medium_Phone).

.PARAMETER SinCompilar
    No recompila: instala el APK que ya está en app\build\outputs\apk\debug.

.PARAMETER Limpiar
    Borra los datos de la app antes de abrirla (la base SQLite vuelve a los datos de ejemplo).

.PARAMETER Gpu
    Modo de renderizado del emulador. Por defecto 'swiftshader_indirect' (software): con 'host'
    la GPU NVIDIA deja la app en negro al dibujar el mapa. Otros valores: host, angle_indirect, auto.

.EXAMPLE
    .\ejecutar.ps1
    .\ejecutar.ps1 -SinCompilar
    .\ejecutar.ps1 -Avd Medium_Phone -Limpiar
#>
param(
    [string]$Avd,
    [string]$Gpu = 'swiftshader_indirect',
    [switch]$SinCompilar,
    [switch]$Limpiar
)

$ErrorActionPreference = 'Stop'
$proyecto  = $PSScriptRoot
$paquete   = 'com.unsa.publicidad.prototipo'
$actividad = "$paquete/.MainActivity"
$apk       = Join-Path $proyecto 'app\build\outputs\apk\debug\app-debug.apk'

function Paso($texto) { Write-Host "==> $texto" -ForegroundColor Cyan }
function Falla($texto) { Write-Host "ERROR: $texto" -ForegroundColor Red; exit 1 }

# --- 1. Ubicar el SDK de Android (local.properties > ANDROID_HOME > ruta por defecto)
$sdk = $null
$localProps = Join-Path $proyecto 'local.properties'
if (Test-Path $localProps) {
    $linea = Get-Content $localProps | Where-Object { $_ -match '^\s*sdk\.dir\s*=' } | Select-Object -First 1
    if ($linea) { $sdk = ($linea -replace '^\s*sdk\.dir\s*=\s*', '') -replace '\\:', ':' -replace '\\\\', '\' }
}
if (-not $sdk -or -not (Test-Path $sdk)) { $sdk = $env:ANDROID_HOME }
if (-not $sdk -or -not (Test-Path $sdk)) { $sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
if (-not (Test-Path $sdk)) { Falla "No se encontró el SDK de Android. Instálalo desde Android Studio." }

$adb      = Join-Path $sdk 'platform-tools\adb.exe'
$emulador = Join-Path $sdk 'emulator\emulator.exe'
if (-not (Test-Path $adb))      { Falla "No existe $adb (instala 'Android SDK Platform-Tools')." }
if (-not (Test-Path $emulador)) { Falla "No existe $emulador (instala 'Android Emulator')." }

# --- 2. Compilar el APK con el JDK de Android Studio
if (-not $SinCompilar) {
    if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
        $jbr = 'C:\Program Files\Android\Android Studio\jbr'
        if (Test-Path (Join-Path $jbr 'bin\java.exe')) { $env:JAVA_HOME = $jbr }
        else { Falla "No se encontró un JDK. Define JAVA_HOME o instala Android Studio." }
    }
    Paso "Compilando (gradlew assembleDebug)..."
    Push-Location $proyecto
    try { & .\gradlew.bat assembleDebug } finally { Pop-Location }
    if ($LASTEXITCODE -ne 0) { Falla "La compilación falló." }
}
if (-not (Test-Path $apk)) { Falla "No se encontró el APK en $apk. Ejecuta sin -SinCompilar." }

# --- 3. Usar un dispositivo/emulador ya conectado o arrancar uno
& $adb start-server | Out-Null

function Dispositivos {
    & $adb devices | Select-Object -Skip 1 |
        Where-Object { $_ -match '^(\S+)\s+device$' } |
        ForEach-Object { ($_ -split '\s+')[0] }
}

$serial = Dispositivos | Select-Object -First 1
if ($serial) {
    Paso "Usando el dispositivo ya conectado: $serial"
    if ($serial -like 'emulator-*' -and $Gpu -eq 'swiftshader_indirect') {
        $gles = (& $adb -s $serial shell dumpsys SurfaceFlinger 2>$null | Select-String 'GLES:' | Select-Object -First 1 | Out-String)
        if ($gles -and $gles -notmatch 'SwiftShader') {
            Write-Host "AVISO: este emulador se abrió con la GPU del equipo y la app puede verse en negro." -ForegroundColor Yellow
            Write-Host "       Ciérralo y vuelve a ejecutar este script para que lo abra en modo software." -ForegroundColor Yellow
        }
    }
} else {
    $avds = @(& $emulador -list-avds | Where-Object { $_.Trim() -ne '' })
    if ($avds.Count -eq 0) { Falla "No hay emuladores creados. Crea uno en Android Studio > Device Manager." }
    if (-not $Avd) { $Avd = $avds[0] }
    elseif ($avds -notcontains $Avd) { Falla "El AVD '$Avd' no existe. Disponibles: $($avds -join ', ')" }

    Paso "Abriendo el emulador '$Avd' (GPU: $Gpu)..."
    Start-Process -FilePath $emulador -ArgumentList @('-avd', $Avd, '-gpu', $Gpu, '-netdelay', 'none', '-netspeed', 'full') -WindowStyle Hidden

    Paso "Esperando a que el emulador se conecte..."
    $limite = (Get-Date).AddMinutes(3)
    while (-not $serial) {
        if ((Get-Date) -gt $limite) { Falla "El emulador no apareció en adb tras 3 minutos." }
        Start-Sleep -Seconds 2
        $serial = Dispositivos | Where-Object { $_ -like 'emulator-*' } | Select-Object -First 1
    }

    Paso "Esperando a que Android termine de arrancar ($serial)..."
    $limite = (Get-Date).AddMinutes(5)
    do {
        if ((Get-Date) -gt $limite) { Falla "Android no terminó de arrancar tras 5 minutos." }
        Start-Sleep -Seconds 2
        $listo = (& $adb -s $serial shell getprop sys.boot_completed 2>$null | Out-String).Trim()
    } until ($listo -eq '1')
    & $adb -s $serial shell input keyevent 82 2>$null | Out-Null   # desbloquear pantalla
}

# --- 4. Instalar y lanzar la app
Paso "Instalando la app..."
& $adb -s $serial install -r -t $apk
if ($LASTEXITCODE -ne 0) { Falla "No se pudo instalar el APK." }

if ($Limpiar) {
    Paso "Borrando los datos de la app..."
    & $adb -s $serial shell pm clear $paquete | Out-Null
}

Paso "Abriendo la app..."
& $adb -s $serial shell am start -n $actividad | Out-Null
if ($LASTEXITCODE -ne 0) { Falla "No se pudo abrir la actividad $actividad." }

Write-Host "Listo: la app está abierta en $serial." -ForegroundColor Green

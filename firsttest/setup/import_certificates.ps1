# Import-Certificate.ps1
# This script helps import SSL certificates to the Java keystore

param (
    [string]$certUrl,
    [string]$alias,
    [string]$javaHome = $env:JAVA_HOME
)

if (-not $javaHome) {
    Write-Host "JAVA_HOME environment variable is not set. Please set it to your Java installation directory."
    exit 1
}

$keytool = "$javaHome\bin\keytool.exe"
$keystore = "$javaHome\lib\security\cacerts"
$password = "changeit"  # Default Java keystore password

# Create temp directory if it doesn't exist
$tempDir = "$env:TEMP\certs"
if (-not (Test-Path $tempDir)) {
    New-Item -ItemType Directory -Path $tempDir | Out-Null
}

# Download certificate
$certFile = "$tempDir\$alias.cer"
try {
    Invoke-WebRequest -Uri $certUrl -OutFile $certFile
    Write-Host "Certificate downloaded to $certFile"
} catch {
    Write-Host "Error downloading certificate: $_"
    exit 1
}

# Import certificate to Java keystore
try {
    & $keytool -import -noprompt -trustcacerts -alias $alias -file $certFile -keystore $keystore -storepass $password
    Write-Host "Certificate imported successfully with alias: $alias"
} catch {
    Write-Host "Error importing certificate: $_"
    exit 1
}

Write-Host "Certificate import completed successfully!"

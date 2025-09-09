# Setup-Certificates.ps1
# This script downloads and installs common development certificates

# Create temp directory for certificates
$tempDir = "$env:TEMP\dev_certs"
if (-not (Test-Path $tempDir)) {
    New-Item -ItemType Directory -Path $tempDir | Out-Null
}

# Function to download and import a certificate
function Install-Certificate {
    param (
        [string]$certUrl,
        [string]$alias,
        [string]$javaHome = $env:JAVA_HOME
    )

    if (-not $javaHome) {
        Write-Host "JAVA_HOME environment variable is not set. Please set it to your Java installation directory."
        return $false
    }

    $keytool = "$javaHome\bin\keytool.exe"
    $keystore = "$javaHome\lib\security\cacerts"
    $password = "changeit"  # Default Java keystore password

    $certFile = "$tempDir\$alias.cer"
    
    Write-Host "Downloading certificate from $certUrl..."
    try {
        Invoke-WebRequest -Uri $certUrl -OutFile $certFile -ErrorAction Stop
        Write-Host "Certificate downloaded to $certFile"
    } catch {
        Write-Host "Warning: Could not download certificate from $certUrl"
        Write-Host "Error: $_"
        return $false
    }

    # Import certificate to Java keystore
    try {
        & $keytool -import -noprompt -trustcacerts -alias $alias -file $certFile -keystore $keystore -storepass $password -ErrorAction Stop
        Write-Host "✅ Certificate imported successfully with alias: $alias"
        return $true
    } catch {
        Write-Host "Warning: Could not import certificate $alias"
        Write-Host "Error: $_"
        return $false
    }
}

# List of common development certificates to install
$certificates = @(
    @{
        Name = "DigiCert Global Root CA"
        Url = "https://cacerts.digicert.com/DigiCertGlobalRootCA.crt"
        Alias = "DigiCertGlobalRootCA"
    },
    @{
        Name = "DigiCert Global Root G2"
        Url = "https://cacerts.digicert.com/DigiCertGlobalRootG2.crt"
        Alias = "DigiCertGlobalRootG2"
    },
    @{
        Name = "Let's Encrypt Authority X3"
        Url = "https://letsencrypt.org/certs/lets-encrypt-x3-cross-signed.der"
        Alias = "LetsEncryptX3"
    },
    @{
        Name = "ISRG Root X1"
        Url = "https://letsencrypt.org/certs/isrgrootx1.der"
        Alias = "ISRGRootX1"
    },
    @{
        Name = "Microsoft RSA Root Certificate Authority 2017"
        Url = "https://www.microsoft.com/pkiops/certs/Microsoft%20RSA%20Root%20Certificate%20Authority%202017.crt"
        Alias = "MicrosoftRSARootCA2017"
    }
)

# Install all certificates
Write-Host "Starting certificate installation..."
$successCount = 0

foreach ($cert in $certificates) {
    Write-Host "`nInstalling $($cert.Name)..."
    if (Install-Certificate -certUrl $cert.Url -alias $cert.Alias) {
        $successCount++
    }
}

# Additional step: Install Windows Root Certificates (if running as admin)
if (([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Host "`nInstalling certificates to Windows Trusted Root Store..."
    Get-ChildItem -Path $tempDir -Filter *.cer | ForEach-Object {
        try {
            $cert = New-Object System.Security.Cryptography.X509Certificates.X509Certificate2($_.FullName)
            $store = New-Object System.Security.Cryptography.X509Certificates.X509Store("Root", "LocalMachine")
            $store.Open("ReadWrite")
            $store.Add($cert)
            $store.Close()
            Write-Host "✅ Added $($cert.Subject) to Windows Trusted Root Store"
        } catch {
            Write-Host "⚠️ Could not add certificate to Windows Trusted Root Store: $_"
        }
    }
} else {
    Write-Host "`n⚠️ Not running as administrator. To install certificates system-wide, please run this script as Administrator."
}

# Summary
Write-Host "`nCertificate installation complete!"
Write-Host "Successfully installed $successCount out of $($certificates.Count) certificates."
Write-Host "Certificates were saved to: $tempDir"
Write-Host "`nPlease restart your IDE for the changes to take effect."

# Keep the window open
Read-Host "`nPress Enter to exit..."

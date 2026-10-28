# Usage (from anywhere):  powershell -ExecutionPolicy Bypass -File infra\scripts\seal-secrets.ps1 dev
# Generates random passwords and writes ENCRYPTED SealedSecrets (safe to commit) next to each app:
# escala-api\, elegibilidade-worker\ (their databases) and shared\ (RabbitMQ) under infra\k8s\<env>\.
# Needs: kubectl pointed at the cluster, kubeseal.exe on PATH, sealed-secrets controller installed,
# and the namespace escala-<env> already created.
param(
  [Parameter(Mandatory = $true, Position = 0)]
  [ValidateSet('dev', 'qa', 'prod')]
  [string]$Environment
)
$ErrorActionPreference = 'Stop'
$ns  = "escala-$Environment"
$base = Join-Path $PSScriptRoot "..\k8s\$Environment"

function New-Pass {
  $bytes = New-Object byte[] 30
  [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
  ([Convert]::ToBase64String($bytes) -replace '[^a-zA-Z0-9]', '').Substring(0, 24)
}

function New-Sealed([string]$name, [string[]]$literals) {
  $kargs = @('create', 'secret', 'generic', $name, '-n', $ns) + $literals + @('--dry-run=client', '-o', 'yaml')
  $yaml = & kubectl @kargs | & kubeseal --controller-name sealed-secrets --controller-namespace kube-system --format yaml
  if ($LASTEXITCODE -ne 0) { throw "kubeseal failed for $name" }
  ($yaml -join "`n")
}

$utf8 = New-Object Text.UTF8Encoding $false
function Write-Sealed([string]$relPath, [string]$yaml) {
  [IO.File]::WriteAllText((Join-Path $base $relPath), $yaml + "`n", $utf8)
}

Write-Sealed 'escala-api\sealed-secret.yaml' (New-Sealed 'db-escala-api' @('--from-literal=POSTGRES_DB=escala', '--from-literal=POSTGRES_USER=escala', "--from-literal=POSTGRES_PASSWORD=$(New-Pass)"))
Write-Sealed 'elegibilidade-worker\sealed-secret.yaml' (New-Sealed 'db-elegibilidade' @('--from-literal=POSTGRES_DB=elegibilidade', '--from-literal=POSTGRES_USER=elegibilidade', "--from-literal=POSTGRES_PASSWORD=$(New-Pass)"))
Write-Sealed 'shared\sealed-secret.yaml' (New-Sealed 'rabbitmq-credentials' @('--from-literal=username=escala', "--from-literal=password=$(New-Pass)"))
Write-Host "Sealed secrets written under $base (commit them)."

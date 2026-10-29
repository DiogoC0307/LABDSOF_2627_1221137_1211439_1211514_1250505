# Usage:  powershell -ExecutionPolicy Bypass -File infra\scripts\seal-secrets.ps1 <dev|qa|prod|observability> [-Force]
# Generates random passwords and writes ENCRYPTED SealedSecrets (safe to commit) next to the app that uses them:
#   <env>\escala-api\sealed-secret.yaml                    its database
#   <env>\escala-api\sealed-secret-rabbitmq.yaml           its RabbitMQ login
#   <env>\elegibilidade-worker\sealed-secret.yaml          its database
#   <env>\elegibilidade-worker\sealed-secret-rabbitmq.yaml its RabbitMQ login
#   <env>\shared\sealed-secret.yaml                        RabbitMQ admin login (management UI)
#   observability\grafana\sealed-secret.yaml               Grafana admin login
# Files that already exist are KEPT, so running it again only creates what is missing and never changes the
# passwords of databases that are already running. Use -Force only to rotate (see the README before doing that).
# Needs: kubectl pointed at the cluster, kubeseal.exe on PATH, and the sealed-secrets controller installed.
param(
  [Parameter(Mandatory = $true, Position = 0)]
  [ValidateSet('dev', 'qa', 'prod', 'observability')]
  [string]$Environment,
  [switch]$Force
)
$ErrorActionPreference = 'Stop'
$ns   = if ($Environment -eq 'observability') { 'observability' } else { "escala-$Environment" }
$base = Join-Path $PSScriptRoot "..\k8s\$Environment"
$utf8 = New-Object Text.UTF8Encoding $false

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

$items = @()
if ($Environment -eq 'observability') {
  $items += @{ Path = 'grafana\sealed-secret.yaml'; Name = 'grafana-admin'; Lits = { @('--from-literal=admin-user=admin', "--from-literal=admin-password=$(New-Pass)") } }
} else {
  $items += @{ Path = 'escala-api\sealed-secret.yaml';                    Name = 'db-escala-api';                Lits = { @('--from-literal=POSTGRES_DB=escala', '--from-literal=POSTGRES_USER=escala', "--from-literal=POSTGRES_PASSWORD=$(New-Pass)") } }
  $items += @{ Path = 'elegibilidade-worker\sealed-secret.yaml';          Name = 'db-elegibilidade';             Lits = { @('--from-literal=POSTGRES_DB=elegibilidade', '--from-literal=POSTGRES_USER=elegibilidade', "--from-literal=POSTGRES_PASSWORD=$(New-Pass)") } }
  $items += @{ Path = 'shared\sealed-secret.yaml';                        Name = 'rabbitmq-credentials';         Lits = { @('--from-literal=username=escala', "--from-literal=password=$(New-Pass)") } }
  $items += @{ Path = 'escala-api\sealed-secret-rabbitmq.yaml';           Name = 'rabbitmq-escala-api';          Lits = { @('--from-literal=username=escala-api', "--from-literal=password=$(New-Pass)") } }
  $items += @{ Path = 'elegibilidade-worker\sealed-secret-rabbitmq.yaml'; Name = 'rabbitmq-elegibilidade-worker'; Lits = { @('--from-literal=username=elegibilidade-worker', "--from-literal=password=$(New-Pass)") } }
}

foreach ($i in $items) {
  $file = Join-Path $base $i.Path
  if ((Test-Path $file) -and -not $Force) { Write-Host "kept    $($i.Path)"; continue }
  $yaml = New-Sealed $i.Name (& $i.Lits)
  [IO.File]::WriteAllText($file, $yaml + "`n", $utf8)
  Write-Host "sealed  $($i.Path)"
}
Write-Host "Done. Commit the new sealed-secret*.yaml files."

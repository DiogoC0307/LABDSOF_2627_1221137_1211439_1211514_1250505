# Prints the credentials of one service's database and opens a tunnel to it.
# Usage:  powershell -ExecutionPolicy Bypass -File infra\scripts\db-access.ps1 dev escala-api
#         powershell -ExecutionPolicy Bypass -File infra\scripts\db-access.ps1 prod elegibilidade-worker
# Then connect (IntelliJ Database tool, DBeaver, psql) to localhost:<port> with the values printed.
# Needs: kubectl working against the cluster (SSH tunnel to the VM open). Ctrl+C closes the tunnel.
param(
  [Parameter(Mandatory = $true, Position = 0)][ValidateSet('dev', 'qa', 'prod')][string]$Environment,
  [Parameter(Mandatory = $true, Position = 1)][ValidateSet('escala-api', 'elegibilidade-worker')][string]$Service,
  [int]$LocalPort = 0
)
$ErrorActionPreference = 'Stop'
$ns     = "escala-$Environment"
$secret = if ($Service -eq 'escala-api') { 'db-escala-api' } else { 'db-elegibilidade' }
if ($LocalPort -eq 0) { $LocalPort = if ($Service -eq 'escala-api') { 5432 } else { 5433 } }

function Get-Value([string]$key) {
  $b64 = kubectl -n $ns get secret $secret -o "jsonpath={.data.$key}"
  if (-not $b64) { throw "Secret $secret / $key not found in namespace $ns" }
  [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($b64))
}
$db = Get-Value 'POSTGRES_DB'; $user = Get-Value 'POSTGRES_USER'; $pass = Get-Value 'POSTGRES_PASSWORD'

Write-Host ""
Write-Host "Environment : $Environment    Service: $Service"
Write-Host "Host        : localhost"
Write-Host "Port        : $LocalPort"
Write-Host "Database    : $db"
Write-Host "User        : $user"
Write-Host "Password    : $pass"
Write-Host "JDBC URL    : jdbc:postgresql://localhost:$LocalPort/$db"
Write-Host ""
Write-Host "Tunnel open. Leave this window running, press Ctrl+C to close it."
kubectl -n $ns port-forward "pod/pg-$Service-0" "${LocalPort}:5432"

# Prints the RabbitMQ login of one environment and opens a tunnel to its management UI.
# Usage:  powershell -ExecutionPolicy Bypass -File infra\scripts\rabbitmq-ui.ps1 dev
# Then open http://localhost:15672 (queues, retry queues, dead-letter queues, message rates).
param(
  [Parameter(Mandatory = $true, Position = 0)][ValidateSet('dev', 'qa', 'prod')][string]$Environment,
  [int]$LocalPort = 15672
)
$ErrorActionPreference = 'Stop'
$ns = "escala-$Environment"
function Get-Value([string]$key) {
  $b64 = kubectl -n $ns get secret rabbitmq-credentials -o "jsonpath={.data.$key}"
  [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($b64))
}
Write-Host ""
Write-Host "Environment : $Environment"
Write-Host "URL         : http://localhost:$LocalPort"
Write-Host "User        : $(Get-Value 'username')"
Write-Host "Password    : $(Get-Value 'password')"
Write-Host "AMQP (apps) : rabbitmq:5672 inside the cluster"
Write-Host ""
Write-Host "Tunnel open. Leave this window running, press Ctrl+C to close it."
kubectl -n $ns port-forward svc/rabbitmq "${LocalPort}:15672"

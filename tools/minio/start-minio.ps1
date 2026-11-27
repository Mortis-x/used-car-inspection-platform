$ErrorActionPreference = "Stop"

$env:MINIO_ROOT_USER = "minioadmin"
$env:MINIO_ROOT_PASSWORD = "minioadmin"

$exe = Join-Path $PSScriptRoot "minio.exe"
$data = Join-Path $PSScriptRoot "data"

if (-not (Test-Path $exe)) {
    Write-Host "未找到 minio.exe，请先准备 Windows 版 MinIO 并放到 $PSScriptRoot" -ForegroundColor Red
    exit 1
}

New-Item -ItemType Directory -Force -Path $data | Out-Null
Write-Host "MinIO 控制台: http://localhost:9001  账号/密码: minioadmin / minioadmin"
& $exe server $data --address ":9000" --console-address ":9001"

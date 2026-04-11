param(
    [switch]$BackendOnly,
    [switch]$FrontendOnly,
    [switch]$SkipBuild,
    [switch]$StartDepsIfMissing,
    [string]$RedisPassword = "123456"
)

$ErrorActionPreference = "Stop"

function Write-Step([string]$message) {
    Write-Host "==> $message" -ForegroundColor Cyan
}

function Write-WarnMsg([string]$message) {
    Write-Host "[WARN] $message" -ForegroundColor Yellow
}

function Write-Ok([string]$message) {
    Write-Host "[OK] $message" -ForegroundColor Green
}

function Test-PortOpen([int]$port) {
    return Test-NetConnection -ComputerName "127.0.0.1" -Port $port -InformationLevel Quiet
}

function Get-JavaMajorVersion([string]$javaCommand) {
    try {
        # Use cmd /c to avoid PowerShell treating java -version stderr output as terminating errors.
        $versionLine = (cmd /c ('"' + $javaCommand + '" -version 2>&1') | Select-Object -First 1)
        if ($versionLine -match '"(\d+)\.(\d+).*"') {
            if ($Matches[1] -eq "1") {
                return [int]$Matches[2]
            }
            return [int]$Matches[1]
        }
        if ($versionLine -match '"(\d+).*"') {
            return [int]$Matches[1]
        }
        return 0
    }
    catch {
        return 0
    }
}

function Resolve-Java17Path {
    $candidates = @()
    if ($env:JAVA17_HOME) {
        $candidates += (Join-Path $env:JAVA17_HOME "bin\java.exe")
    }
    if ($env:JAVA_HOME) {
        $candidates += (Join-Path $env:JAVA_HOME "bin\java.exe")
    }
    $candidates += "C:\Program Files\Java\jdk-17\bin\java.exe"
    $candidates += "C:\Program Files\Java\latest\bin\java.exe"
    $candidates += "java"

    foreach ($candidate in $candidates) {
        $resolved = $candidate
        if ($candidate -ne "java" -and -not (Test-Path $candidate)) {
            continue
        }
        $major = Get-JavaMajorVersion -javaCommand $resolved
        if ($major -ge 17) {
            return $resolved
        }
    }
    return $null
}

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendModuleDir = Join-Path $projectRoot "spring-ai-alibaba-admin-server-start"
$frontendDir = Join-Path $projectRoot "frontend\packages\main"
$composeFile = Join-Path $projectRoot "deploy\docker-compose\docker-compose-service.yaml"
$backendJar = Join-Path $backendModuleDir "target\spring-ai-alibaba-admin-server-start.jar"
$backendPort = 8200

$startBackend = -not $FrontendOnly
$startFrontend = -not $BackendOnly

Write-Step "Checking Docker containers required by local backend"
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "docker command not found"
}

$requiredContainers = @(
    "spring-ai-admin-elasticsearch",
    "spring-ai-admin-nacos",
    "spring-ai-admin-rmq-namesrv",
    "spring-ai-admin-rmq-broker",
    "spring-ai-admin-rmq-proxy"
)

$runningNames = @()
$dockerPsRaw = docker ps --format "{{.Names}}"
if ($dockerPsRaw) {
    $runningNames = $dockerPsRaw -split "`n" | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne "" }
}

$missingContainers = @()
foreach ($name in $requiredContainers) {
    if ($runningNames -notcontains $name) {
        $missingContainers += $name
    }
}

if ($missingContainers.Count -gt 0) {
    Write-WarnMsg ("Missing dependency containers: " + ($missingContainers -join ", "))
    if ($StartDepsIfMissing) {
        Write-Step "Starting missing dependencies from docker-compose"
        docker compose -f $composeFile up -d elasticsearch nacos rmq-namesrv rmq-broker rmq-proxy rmq-init-topic | Out-Host
    }
    else {
        Write-Host "Run this command first, then re-run script:" -ForegroundColor Yellow
        Write-Host "docker compose -f `"$composeFile`" up -d elasticsearch nacos rmq-namesrv rmq-broker rmq-proxy rmq-init-topic" -ForegroundColor White
        exit 1
    }
}
else {
    Write-Ok "Required dependency containers are running"
}

Write-Step "Checking local MySQL(3306) and Redis(6379)"
if (-not (Test-PortOpen -port 3306)) {
    throw "Local MySQL 127.0.0.1:3306 is not reachable"
}
if (-not (Test-PortOpen -port 6379)) {
    throw "Local Redis 127.0.0.1:6379 is not reachable"
}
Write-Ok "Local MySQL/Redis ports are reachable"

$javaCommand = Resolve-Java17Path
if (-not $javaCommand) {
    throw "No Java 17+ found. Please install JDK17 and set JAVA17_HOME or JAVA_HOME."
}
Write-Ok "Using Java command: $javaCommand"

if ($startBackend) {
    if (-not $SkipBuild) {
        Write-Step "Building backend jar (skip tests)"
        Push-Location $projectRoot
        try {
            mvn -pl spring-ai-alibaba-admin-server-start -am -DskipTests package | Out-Host
        }
        finally {
            Pop-Location
        }
    }

    if (-not (Test-Path $backendJar)) {
        throw "Backend jar not found: $backendJar"
    }

    Write-Step "Starting backend in new PowerShell window"
    $backendCmd = @"
`$env:SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/admin?useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&allowMultiQueries=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai'
`$env:SPRING_DATASOURCE_USERNAME='root'
`$env:SPRING_DATASOURCE_PASSWORD='123456'
`$env:SPRING_DATA_REDIS_HOST='127.0.0.1'
`$env:SPRING_DATA_REDIS_PORT='6379'
`$env:SPRING_DATA_REDIS_DATABASE='0'
`$env:SPRING_DATA_REDIS_PASSWORD='$RedisPassword'
`$env:SPRING_REDIS_HOST='127.0.0.1'
`$env:SPRING_REDIS_PORT='6379'
`$env:SPRING_REDIS_DATABASE='0'
`$env:SPRING_REDIS_PASSWORD='$RedisPassword'
`$env:SPRING_ELASTICSEARCH_URIS='http://127.0.0.1:9200'
`$env:NACOS_SERVER_ADDR='127.0.0.1:8848'
`$env:ROCKETMQ_ENDPOINTS='127.0.0.1:18080'
`$env:ROCKETMQ_DOCUMENT_INDEX_TOPIC='topic_saa_studio_document_index'
`$env:ROCKETMQ_DOCUMENT_INDEX_GROUP='group_saa_studio_document_index'
`$env:MANAGEMENT_OTLP_TRACING_EXPORT_ENDPOINT='http://127.0.0.1:4318/v1/traces'
Set-Location '$projectRoot'
& '$javaCommand' -jar '$backendJar' --spring.profiles.active=local --server.port=$backendPort
"@

    Start-Process powershell -ArgumentList @(
        "-NoExit",
        "-ExecutionPolicy", "Bypass",
        "-Command", $backendCmd
    ) | Out-Null
    Write-Ok "Backend launching: http://127.0.0.1:$backendPort"
}

if ($startFrontend) {
    Write-Step "Starting frontend in new PowerShell window"
    $frontendCmd = @"
Set-Location '$frontendDir'
if (Test-Path '.\src\.umi') { Remove-Item '.\src\.umi' -Recurse -Force }
`$env:WEB_SERVER='http://127.0.0.1:$backendPort'
npm run dev
"@

    Start-Process powershell -ArgumentList @(
        "-NoExit",
        "-ExecutionPolicy", "Bypass",
        "-Command", $frontendCmd
    ) | Out-Null
    Write-Ok "Frontend launching: http://127.0.0.1:8000"
}

Write-Host ""
Write-Host "Done. Use the new PowerShell windows for backend/frontend logs." -ForegroundColor Green

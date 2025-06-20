# Development script to run NextJS and Spring Boot concurrently
# Usage: .\dev.ps1

# Set environment variables

$env:SERVER_PORT = "8080"

$env:NEXTJS_DEV_PORT = "3000"
./mvnw clean compile -s settings.xml
Write-Host "Starting development servers..." -ForegroundColor Green
Write-Host "NextJS will be available at http://localhost:$env:NEXTJS_DEV_PORT" -ForegroundColor Cyan
Write-Host "Spring Boot will be available at http://localhost:$env:SERVER_PORT" -ForegroundColor Cyan
Write-Host "Proxy Gateway will be available at http://localhost:$env:PROXY_PORT" -ForegroundColor Cyan
Write-Host "Press Ctrl+C to stop all servers" -ForegroundColor Yellow

# Store original directory
$originalDir = Get-Location

# Function to generate TypeScript client code
function Generate-TypeScriptClient {
    param(
        [string]$apiUrl = "http://localhost:8080/api-docs",
        [string]$outputDir = "./frontend/client/",
        [switch]$Watch = $false
    )

    Write-Host "Generating TypeScript client code..." -ForegroundColor Green

    # Wait for Spring Boot to be ready
    $maxAttempts = 30
    $attempt = 0

    do {
        try {
            $response = Invoke-WebRequest -Uri $apiUrl -Method GET -TimeoutSec 5
            if ($response.StatusCode -eq 200) {
                Write-Host "Spring Boot API is ready!" -ForegroundColor Green
                break
            }
        }
        catch {
            $attempt++
            if ($attempt -lt $maxAttempts) {
                Write-Host "Waiting for Spring Boot API... (attempt $attempt/$maxAttempts)" -ForegroundColor Yellow
                Start-Sleep -Seconds 2
            }
            else {
                Write-Host "Failed to connect to Spring Boot API after $maxAttempts attempts" -ForegroundColor Red
                return
            }
        }
    } while ($attempt -lt $maxAttempts)

    # Create output directory if it doesn't exist
    if (-not (Test-Path $outputDir)) {
        New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
    }

    # Generate client using swagger-typescript-api for cleaner output
    try {
        Set-Location "$originalDir"

        # Check if swagger-typescript-api is installed
        $packageJson = Get-Content "package.json" | ConvertFrom-Json
        if (-not ($packageJson.devDependencies.'swagger-typescript-api' -or $packageJson.dependencies.'swagger-typescript-api')) {
            Write-Host "Installing swagger-typescript-api..." -ForegroundColor Yellow
            npm install -D swagger-typescript-api
        }

        # Generate with swagger-typescript-api for better TypeScript support
        npx swagger-typescript-api generate `
            -p $apiUrl `
            -o ./src/generated `
            -n api.ts `
            --axios `
            --modular `
            --extract-request-params `
            --extract-request-body `
            --extract-response-body `
            --extract-response-error `
            --unwrap-response-data `
            --single-http-client `
            --route-types

        Write-Host "TypeScript client generated successfully!" -ForegroundColor Green

        # Create an index file for easier imports
        $indexContent = @"
// Auto-generated index file
export * from './api';
export * from './data-contracts';
export * from './http-client';
"@
        Set-Content -Path "./src/generated/index.ts" -Value $indexContent

        # Create a custom axios instance configuration
        $axiosConfigContent = @"
// Custom axios configuration
import axios from 'axios';
import { Api } from './api';

const baseURL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:$($env:SERVER_PORT)';

export const apiClient = new Api({
  baseURL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add request interceptor for auth tokens if needed
apiClient.instance.interceptors.request.use(
  (config) => {
    // Add auth token if available
    const token = typeof window !== 'undefined' ? localStorage.getItem('authToken') : null;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Add response interceptor for error handling
apiClient.instance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Handle unauthorized access
      console.error('Unauthorized access');
    }
    return Promise.reject(error);
  }
);

export default apiClient;
"@
        Set-Content -Path "./src/generated/client.ts" -Value $axiosConfigContent

    }
    catch {
        Write-Host "Failed to generate TypeScript client: $_" -ForegroundColor Red
    }
    finally {
        Set-Location $originalDir
    }
}

# Function to watch for API changes and regenerate
function Start-ApiWatcher {
    param(
        [scriptblock]$GenerateFunction
    )

    $watcher = Start-Job -ScriptBlock {
        param($apiUrl, $outputDir, $originalDir)

        $lastHash = ""
        while ($true) {
            try {
                $response = Invoke-WebRequest -Uri $apiUrl -Method GET -TimeoutSec 5
                $currentHash = [System.Security.Cryptography.SHA256]::Create().ComputeHash([System.Text.Encoding]::UTF8.GetBytes($response.Content))
                $currentHashString = [System.BitConverter]::ToString($currentHash).Replace("-", "")

                if ($currentHashString -ne $lastHash -and $lastHash -ne "") {
                    Write-Host "`nAPI specification changed, regenerating TypeScript client..." -ForegroundColor Yellow

                    # Regenerate inline since we can't pass functions to jobs
                    Set-Location "$originalDir\src\main\nextjs"
                    npx swagger-typescript-api generate `
                        -p $apiUrl `
                        -o ./src/generated `
                        -n api.ts `
                        --axios `
                        --modular `
                        --unwrap-response-data `
                        --single-http-client

                    Set-Location $originalDir
                }

                $lastHash = $currentHashString
            }
            catch {
                # API might be temporarily unavailable, just continue
            }

            Start-Sleep -Seconds 5
        }
    } -ArgumentList "http://localhost:$($env:SERVER_PORT)/api-docs", "./src/main/nextjs/src/generated", $originalDir

    return $watcher
}

# Function to cleanup background processes
function Stop-Servers {
    Write-Host "`nStopping servers..." -ForegroundColor Yellow

    if ($nextjsJob) {
        Stop-Job -Job $nextjsJob -ErrorAction SilentlyContinue
        Remove-Job -Job $nextjsJob -Force -ErrorAction SilentlyContinue
    }

    if ($springbootJob) {
        Stop-Job -Job $springbootJob -ErrorAction SilentlyContinue
        Remove-Job -Job $springbootJob -Force -ErrorAction SilentlyContinue
    }

    if ($proxyJob) {
        Stop-Job -Job $proxyJob -ErrorAction SilentlyContinue
        Remove-Job -Job $proxyJob -Force -ErrorAction SilentlyContinue
    }

    if ($watcherJob) {
        Stop-Job -Job $watcherJob -ErrorAction SilentlyContinue
        Remove-Job -Job $watcherJob -Force -ErrorAction SilentlyContinue
    }

    # Return to original directory
    Set-Location $originalDir
}

# Register cleanup on Ctrl+C
[console]::TreatControlCAsInput = $false
$null = Register-EngineEvent -SourceIdentifier PowerShell.Exiting -Action { Stop-Servers }

try {
    # Start Spring Boot application FIRST as background job
    Write-Host "Starting Spring Boot application..." -ForegroundColor Green
    $springbootJob = Start-Job -ScriptBlock {
        param($profile, $port)
        $env:SPRING_PROFILES_ACTIVE = $profile
        $env:SERVER_PORT = $port
        Set-Location $using:originalDir
        .\mvnw spring-boot:run "-Dspring-boot.run.profiles=$profile" -s settings.xml
    } -ArgumentList $env:SPRING_PROFILES_ACTIVE, $env:SERVER_PORT

    # Wait a moment for Spring Boot to start
    Start-Sleep -Seconds 3

    # Start Proxy Gateway as background job
    Write-Host "Starting Proxy Gateway..." -ForegroundColor Green
    $proxyJob = Start-Job -ScriptBlock {
        param($port)
        $env:SERVER_PORT = $port
        Set-Location "$using:originalDir\proxy"
        .\mvnw spring-boot:run
    } -ArgumentList $env:PROXY_PORT

    # Start NextJS development server as background job
    Write-Host "Starting NextJS development server..." -ForegroundColor Green
    $nextjsJob = Start-Job -ScriptBlock {
        param($port)
        $env:NEXTJS_DEV_PORT = $port
        Set-Location "$using:originalDir\src\main\nextjs"
        pnpm run dev
    } -ArgumentList $env:NEXTJS_DEV_PORT

    Write-Host "`nServers starting in background..." -ForegroundColor Green

    # Wait a bit for servers to start, then generate client code
    Start-Sleep -Seconds 5
    Generate-TypeScriptClient

    # Start API watcher
    Write-Host "Starting API watcher for automatic regeneration..." -ForegroundColor Green
    $watcherJob = Start-ApiWatcher

    Write-Host "Monitoring output (press Ctrl+C to stop):`n" -ForegroundColor Gray

    # Monitor all jobs and display output
    while ($nextjsJob.State -eq 'Running' -or $springbootJob.State -eq 'Running' -or $proxyJob.State -eq 'Running') {
        # Display Proxy output
        $proxyJob | Receive-Job -ErrorAction SilentlyContinue

        # Display NextJS output
        $nextjsJob | Receive-Job -ErrorAction SilentlyContinue

        # Display Spring Boot output
        $springbootJob | Receive-Job -ErrorAction SilentlyContinue

        # Display Watcher output
        $watcherJob | Receive-Job -ErrorAction SilentlyContinue

        # Small delay to prevent CPU spinning
        Start-Sleep -Milliseconds 100
    }
}
catch {
    Write-Host "Error occurred: $_" -ForegroundColor Red
}
finally {
    Stop-Servers
    Unregister-Event -SourceIdentifier PowerShell.Exiting -ErrorAction SilentlyContinue
}
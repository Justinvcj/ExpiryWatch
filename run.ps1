# Read the .env file and set environment variables for this process
Get-Content .env | ForEach-Object {
    if ($_ -match '^(.*?)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2], "Process")
    }
}

# Start the Spring Boot application
Write-Host "Starting ExpiryWatch on http://localhost:8080..." -ForegroundColor Green
.\mvnw.cmd spring-boot:run

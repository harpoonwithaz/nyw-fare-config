if (Test-Path .env) {
    Get-Content .env | ForEach-Object {
        $line = $_.Trim()
        # Ignore comments and empty lines
        if ($line -and -not $line.StartsWith("#")) {
            $name, $value = $line -split '=', 2
            [System.Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim(), "Process")
        }
    }
} else {
    Write-Host "Warning: .env file not found. Running with system environment." -ForegroundColor Yellow
}

.\mvnw.cmd spring-boot:run
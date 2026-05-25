$impls = Get-ChildItem -Path "src\main\java\com\example\mpct\service\impl\*.java"
foreach ($file in $impls) {
    $content = Get-Content $file.FullName
    $content = $content -replace "package com.example.mpct.service;", "package com.example.mpct.service.impl;`n`nimport com.example.mpct.service.*;"
    
    $name = $file.BaseName
    if ($name -match "(.*)Impl") {
        $interfaceName = $matches[1]
        $content = $content -replace "public class $name", "public class $name implements $interfaceName"
    }
    
    Set-Content -Path $file.FullName -Value $content
}

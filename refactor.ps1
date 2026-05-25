$entities = Get-ChildItem -Path "src\main\java\com\example\mpct\model\entity\*.java"
foreach ($file in $entities) {
    $content = Get-Content $file.FullName
    $content = $content -replace "package com.example.mpct.model;", "package com.example.mpct.model.entity;"
    $content = $content -replace "import com.example.mpct.model.Role;", "import com.example.mpct.model.enums.Role;"
    $content = $content -replace "import com.example.mpct.model.TipoTramite;", "import com.example.mpct.model.enums.TipoTramite;"
    $content = $content -replace "import com.example.mpct.model.EstadoTramite;", "import com.example.mpct.model.enums.EstadoTramite;"
    $content = $content -replace "import com.example.mpct.model.EstadoInspeccion;", "import com.example.mpct.model.enums.EstadoInspeccion;"
    Set-Content -Path $file.FullName -Value $content
}

$enums = Get-ChildItem -Path "src\main\java\com\example\mpct\model\enums\*.java"
foreach ($file in $enums) {
    $content = Get-Content $file.FullName
    $content = $content -replace "package com.example.mpct.model;", "package com.example.mpct.model.enums;"
    Set-Content -Path $file.FullName -Value $content
}

# Update all other files
$allJavaFiles = Get-ChildItem -Path "src\main\java\com\example\mpct\*.java" -Recurse | Where-Object { $_.FullName -notmatch "\\model\\" }
foreach ($file in $allJavaFiles) {
    $content = Get-Content $file.FullName
    # Replace wildcard import
    $content = $content -replace "import com.example.mpct.model.\*;", "import com.example.mpct.model.entity.*;`nimport com.example.mpct.model.enums.*;"
    
    # Replace specific imports
    $content = $content -replace "import com.example.mpct.model.User;", "import com.example.mpct.model.entity.User;"
    $content = $content -replace "import com.example.mpct.model.UserProfile;", "import com.example.mpct.model.entity.UserProfile;"
    $content = $content -replace "import com.example.mpct.model.Tramite;", "import com.example.mpct.model.entity.Tramite;"
    $content = $content -replace "import com.example.mpct.model.Pago;", "import com.example.mpct.model.entity.Pago;"
    $content = $content -replace "import com.example.mpct.model.Inspeccion;", "import com.example.mpct.model.entity.Inspeccion;"
    $content = $content -replace "import com.example.mpct.model.Licencia;", "import com.example.mpct.model.entity.Licencia;"
    $content = $content -replace "import com.example.mpct.model.Configuracion;", "import com.example.mpct.model.entity.Configuracion;"

    $content = $content -replace "import com.example.mpct.model.Role;", "import com.example.mpct.model.enums.Role;"
    $content = $content -replace "import com.example.mpct.model.TipoTramite;", "import com.example.mpct.model.enums.TipoTramite;"
    $content = $content -replace "import com.example.mpct.model.EstadoTramite;", "import com.example.mpct.model.enums.EstadoTramite;"
    $content = $content -replace "import com.example.mpct.model.EstadoInspeccion;", "import com.example.mpct.model.enums.EstadoInspeccion;"
    
    Set-Content -Path $file.FullName -Value $content
}

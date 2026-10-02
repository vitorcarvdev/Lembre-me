# Me Lembre

Aplicativo Android pessoal para cadastrar lembretes simples (mensagem, data e hora) e receber uma notificação no horário escolhido.

## Requisitos

- Android Studio (Ladybug ou mais recente recomendado)
- JDK 17
- Android SDK com API 35

## Abrir o projeto

1. No Android Studio: **File → Open**
2. Selecione a pasta do projeto (`melembre`).

No Windows, o caminho esperado é:

`D:\LocalHost\www\projetos\sistemas\melembre`

## Compilar

No Windows (PowerShell ou CMD):

```bat
gradlew.bat assembleDebug
```

## APK Debug

`app/build/outputs/apk/debug/app-debug.apk`

## Permissões importantes

- **Notificações (Android 13+):** `POST_NOTIFICATIONS`
- **Alarmes exatos (Android 12+):** `SCHEDULE_EXACT_ALARM`
- **Reinício do aparelho:** `RECEIVE_BOOT_COMPLETED`

## Stack

- Kotlin, Jetpack Compose, Material 3
- Room (armazenamento local)
- AlarmManager (`setExactAndAllowWhileIdle`)

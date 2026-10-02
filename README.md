# Me Lembre

Aplicativo Android pessoal para cadastrar lembretes simples (mensagem, data e hora) e receber uma notificação no horário escolhido.

## Requisitos

- Android Studio (Ladybug ou mais recente recomendado)
- **JDK 17 ou 21** para o Gradle (não use JDK 25)
- Android SDK com API 35

## Abrir o projeto

1. No Android Studio: **File → Open**
2. Selecione a pasta do projeto (`melembre`).

No Windows, o caminho esperado é:

`D:\LocalHost\www\projetos\sistemas\melembre`

### Se o sync falhar com "Incompatible Gradle JVM version"

O Gradle 8.10.2 deste projeto **não roda em JDK 25**.

No Android Studio, faça um destes:

1. No painel **Build**, clique em **Apply compatible Gradle JDK configuration and sync**, **ou**
2. **File → Settings → Build, Execution, Deployment → Build Tools → Gradle**
   - Em **Gradle JDK**, escolha **17** ou **21** (ex.: `jbr-17` / Embedded JDK)
   - Clique **Apply** → **OK** → Sync again

Não altere o código do app por causa disso — é só a JVM usada para rodar o Gradle.

## Compilar

Pelo terminal, na raiz do projeto:

```bash
./gradlew assembleDebug
```

No Windows (PowerShell ou CMD), com JDK 17/21 no `JAVA_HOME`:

```bat
gradlew.bat assembleDebug
```

## APK Debug

Após `assembleDebug`, o APK fica em:

`app/build/outputs/apk/debug/app-debug.apk`

Instale em um dispositivo ou emulador com depuração USB habilitada.

## Permissões importantes

- **Notificações (Android 13+):** `POST_NOTIFICATIONS` — o app solicita na primeira abertura.
- **Alarmes exatos (Android 12+):** `SCHEDULE_EXACT_ALARM` — necessário para disparar no minuto certo. Se estiver desativado, o app avisa e abre a tela de configuração do Android. Lembretes **não** são salvos enquanto alarmes exatos não estiverem permitidos.
- **Reinício do aparelho:** `RECEIVE_BOOT_COMPLETED` — reagenda lembretes futuros após reboot.

## Stack

- Kotlin, Jetpack Compose, Material 3
- Room (armazenamento local)
- AlarmManager (`setExactAndAllowWhileIdle`)

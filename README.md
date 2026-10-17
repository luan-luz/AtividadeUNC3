# TRILHA — Aplicativo Android nativo

Aplicativo Android feito em **Java com componentes de interface nativos**, sem WebView, sem conexão com servidores e sem rastreadores. Compatível com Android 7.0 (API 24) ou posterior.

## Recursos
- Cadastrar, renomear e excluir atividades.
- Cadastrar, renomear e excluir tipos próprios de cada atividade.
- Cronômetro de concentração com pausar, retomar, concluir, descartar e retomada após fechar o app.
- Histórico de sessões, totais do dia e tempo acumulado.
- Armazenamento local (`SharedPreferences` + JSON), sem necessidade de conta ou internet.

## Gerar APK no Android Studio
1. Abra a pasta `TRILHA_Android_Nativo` como um projeto no Android Studio.
2. Aguarde a sincronização do Gradle (JDK 17, Android SDK 35).
3. Use **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. O arquivo gerado fica em `app/build/outputs/apk/debug/app-debug.apk`.

Instale o APK copiando-o para o dispositivo e abrindo o arquivo. Talvez seja necessário autorizar a instalação desta fonte no Android. APK de depuração é assinado automaticamente pelo Android Gradle Plugin com uma chave local, apropriado para uso pessoal e testes. Para publicar, crie uma assinatura de lançamento própria.

## Privacidade e limitações
Os dados permanecem armazenados no dispositivo e poderão ser perdidos se o usuário limpar os dados do app ou o desinstalar. Não há sincronização, backup em nuvem nem notificações. O cronômetro conta o tempo de parede decorrido e mantém a sessão mesmo com o app em segundo plano. O histórico é preservado ao excluir uma atividade ou tipo.

# 1o · v1.2.0 — Atividades, desafios e aprendizagem

App Android nativo em Java, interface preta/branca/vermelha e uso **100% offline**. Sem WebView, conta, servidor, telemetria ou permissão de internet.

## Funcionalidades
- Atividades e tipos personalizados.
- Único cronômetro com preparação ilimitada, foco planejado, pausa, retomada e excedente.
- Histórico local de sessões, mantido intacto.
- Desafios de 7, 14, 30, 60 e 90 dias, vinculados a atividade e tipo.
- Metas diárias de 1 a 720 minutos, com mudanças para dias específicos; 0 indica descanso.
- Percentual de dias cumpridos, sequência atual e melhor sequência, tempo total de foco, preparação e excedente.
- Desafios de aprendizagem: compreensão escrita, ideias principais, explicação livre e dúvidas.
- Questões de recuperação ativa com resposta sem consulta e avaliação manual de 0 a 2.
- Reagendamento de revisão por intervalos crescentes (1, 3, 7, 14 e 30 dias); falhas voltam ao intervalo curto.
- Cópias locais em JSON via seletor nativo de arquivos: exportação e restauração.

## Regras dos indicadores
A soma de foco dos blocos vinculados ao desafio e encerrados em um mesmo dia civil é comparada à meta daquele dia. Tempo extra já pertence ao foco e não é somado duas vezes. Dias de descanso são excluídos do denominador. Sessões avulsas não entram automaticamente em desafios. Uma sessão pertence a no máximo um desafio.

Autoavaliação de recordação não é uma prova objetiva de retenção; os indicadores refletem o desempenho que o usuário registrou. Tempos de preparação longos não significam necessariamente procrastinação.

## Segurança dos dados
O banco local atual permanece no `SharedPreferences` com JSON e migração aditiva. Alterações são versionadas por `dataVersion`, mantendo sessões anteriores. Ao migrar para SQLite/Room, exigir backup e testes de preservação. A restauração substitui todos os registros do dispositivo e exige confirmação.

**Atualizações automáticas via GitHub Releases ainda não estão implementadas.** O APK atual é de depuração; cada runner pode usar certificado diferente. Para manter dados e permitir atualização por cima do app instalado, configurar assinatura estável de distribuição antes de habilitar atualizações.

## Gerar APK
GitHub Actions: `Actions → Gerar APK Android`. Baixe o ZIP no artifact `1o-Android-APK` e extraia `app-debug.apk`.

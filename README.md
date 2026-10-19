# 1o — atividades e blocos de foco

Aplicativo Android **nativo em Java**, offline para execução das tarefas, com tela escura preta e detalhes vermelhos. Sem WebView, sem conta e sem servidor obrigatório. A identidade de pacote `br.com.trilha.foco` foi mantida por compatibilidade com os dados anteriores.

## Fluxo da sessão
1. Selecione a atividade e um tipo; defina a duração de foco planejada entre 1 e 720 minutos.
2. **Preparação:** contagem sem limite, incluindo pausas. Toque em "Iniciar bloco de foco" para trocar de fase.
3. **Foco:** o cronômetro progride normalmente mesmo após atingir a meta; o excedente aparece como **tempo extra**, sem interrupção.
4. Conclua para registrar preparação, foco real, meta e tempo extra.
5. Opcionalmente registre uma preparação encerrada antes do foco, mantendo a tentativa na análise.

## Dados e histórico
Os dados permanecem em `SharedPreferences` + JSON local. O histórico inclui tempos, IDs de atividade e tipo, momento de abertura/início do foco/encerramento e status. Sessões antigas continuam legíveis sem criar metas ou preparação inexistentes. O resumo apresenta tempo total de foco, preparação, extra, média de preparação, proporção de metas alcançadas e preparações encerradas antes do foco.

Uma preparação longa não prova procrastinação; a relação com resultados, contexto, tipo de tarefa e número de observações precisa ser analisada antes de extrair conclusões.

## Compilação
O GitHub Actions da `main` executa o workflow `.github/workflows/android-apk.yml` e publica o APK de depuração nos **Artifacts**. Em caso de HTTP 429 temporário dos repositórios Maven, tenta a compilação novamente.

## Atualizações e preservação
A atualização direta do aplicativo por GitHub Releases **ainda não foi implementada**. APKs de depuração de execuções diferentes podem ter assinaturas diferentes e não se instalar sobre o app atual. Antes de ativar atualização in-app é necessário configurar assinatura de produção estável. Por enquanto, evitar desinstalar a versão instalada sem ter cópia de segurança dos dados; o aplicativo ainda não possui exportação/backup.

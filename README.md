# 1o — Android nativo offline e atualizações pelo GitHub

Versão **1.3.0**. App em Java/Android, identidade preta/branca/vermelha, atividades, tipos, desafios, preparação, foco, tempo extra, histórico, recuperação ativa, revisões espaçadas e backup JSON.

## Atualização integrada
A tela inicial inclui **Verificar atualizações**. Na abertura, o aplicativo também faz uma consulta assíncrona opcional à última versão pública do GitHub Releases (máximo uma verificação bem-sucedida por 24h); se estiver sem internet, ignora o erro sem interromper as funções locais.

Quando houver uma versão nova, oferece download e instalação pelo instalador do Android. A instalação precisa de aprovação explícita do usuário e, conforme o aparelho, permissão para instalar apps desta fonte. **Não há instalações silenciosas.**

A validação exige HTTPS, arquivo de Release com nome exato `1o.apk`, SHA-256 informado pelo GitHub, limite de tamanho, mesmo pacote Android e `versionCode` maior. A assinatura digital será checada pelo próprio instalador Android, que rejeita assinaturas incompatíveis.

## Assinatura permanente — configuração inicial (obrigatória)
A assinatura precisa ser a mesma em todas as versões. **Nunca publique o keystore nem a senha no GitHub, em commit ou arquivo do repositório.**

1. Guarde em local seguro o pacote privado da chave entregue separadamente nesta conversa.
2. GitHub → repositório `luan-luz/AtividadeUNC3` → **Settings → Secrets and variables → Actions → New repository secret**.
3. Crie **ONEO_KEYSTORE_B64** com o texto integral do arquivo `ONEO_KEYSTORE_B64.txt` (apenas a linha Base64).
4. Crie **ONEO_STORE_PASSWORD** com o texto integral do arquivo `ONEO_STORE_PASSWORD.txt`.
5. Na aba **Actions**, abra **Publicar atualização 1o**, selecione a branch `main` e clique em **Run workflow**.
6. Confira a release publicada em `https://github.com/luan-luz/AtividadeUNC3/releases`, tag `v1.3.0`, com arquivo `1o.apk`. **Instale exclusivamente esse APK assinado.**
7. Para futuras versões, incremente `versionCode` e `versionName` no `app/build.gradle`, faça commit na `main` e o GitHub publicará automaticamente uma nova Release com a mesma assinatura.

A função de atualização do próprio aplicativo somente procura Releases públicas. O ZIP da aba *Actions → Artifacts* contém APK **de depuração**, com assinatura instável, e **não deve ser usado como instalação definitiva**.

### Migração de versões anteriores
Se estiver com o APK de depuração v1.2 instalado, a assinatura definitiva não corresponderá. Antes de remover a versão antiga, abra **Desafios → Exportar cópia dos dados**, guarde o JSON em local acessível e verifique que existe; depois, se o Android recusar instalar sobre a versão anterior, desinstale o 1o antigo, instale o `1o.apk` oficial de Releases e vá em **Desafios → Restaurar uma cópia**. A desinstalação apaga os dados internos e esse backup é essencial.

## Build
- `.github/workflows/android-apk.yml`: compila apenas um APK *DEBUG de teste* para verificar o código.
- `.github/workflows/release-1o.yml`: compila *RELEASE assinado* e publica no GitHub Releases, exigindo os segredos acima.

## Segurança e limites
O aplicativo é funcional offline, não exige conta e não sincroniza dados pessoais com o GitHub. A consulta online usa exclusivamente metadados públicos de versão e download de APK. A assinatura é privada; se a chave for perdida, futuras atualizações por cima do aplicativo instalado ficarão inviabilizadas. A automação não concede permissão para instalações silenciosas.

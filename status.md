# STATUS — FologneBlocker

> **FologneBlocker**. Aplicativo Android de filtragem DNS local com Flutter + Kotlin.
> Estado atual: **ponte Flutter/Kotlin e diagnóstico da VPN implementados, com validação Android pendente**.

## Como atualizar este arquivo

- `[ ]` = pendente; `[x]` = concluído e verificado.
- Não marque tarefas com `[x]` somente porque o código foi escrito: execute o teste ou verifique o artefato indicado.
- Respeite a ordem das etapas; correções emergenciais podem ocorrer antes.
- Após cada sessão de desenvolvimento, registre as alterações em **Histórico de progresso**.
- Documentos de referência: [`escopo.md`](./escopo.md) e [`arq.md`](./arq.md).

## Etapa 0 — Documentação e preparação do repositório

**Objetivo:** definir o produto antes de implementar. Esta etapa é **somente de documentação**, sem desenvolvimento de funcionalidades.

- [x] Criar e revisar `escopo.md` (funcionalidades, limites, critérios de aceite).
- [x] Criar e revisar `arq.md` (arquitetura, módulos, fluxos e escolhas técnicas).
- [x] Criar e revisar `status.md` (etapas e checklists).
- [x] Criar `README.md` com resumo, estado experimental, instalação futura e limitações.
- [ ] Definir nome definitivo, identificador do pacote e identidade visual do aplicativo. Nome FologneBlocker e pacote `com.fologne.fologneblocker` definidos. Falta a identidade visual.
- [ ] Definir licença do código original após avaliar compatibilidade das dependências.
- [x] Documentar fontes externas e obrigações de licença, especialmente listas HaGeZi GPL-3.0.
- [x] Criar `PRIVACY.md` com tratamento local de DNS, retenção, provedores externos e permissões.
- [ ] Criar `SECURITY.md` com canal para reporte de vulnerabilidades e limites de segurança. Documento criado. Falta confirmar o reporte privado no GitHub.
- [x] Descrever em `README.md` as restrições: DNS-only, DNS privado/DoH de terceiros, tráfego pelo mesmo domínio e incompatibilidade com VPNs simultâneas.
- [x] Elaborar plano de teste manual em Android real, com cenários de bloqueio e conectividade.

**Critério de conclusão:** documentação revisada e publicada no repositório.

## Etapa 1 — Base do projeto e ferramentas

**Objetivo:** gerar projeto Flutter Android reproduzível e núcleo Kotlin sem funcionalidades simuladas.

- [x] Criar projeto Flutter com suporte Android e organização modular.
- [ ] Configurar versões compatíveis de Flutter, Dart, Kotlin, Gradle e Android SDK. Versões configuradas. Falta validar a compilação Android.
- [x] Definir `minSdk`, `targetSdk` e registrar justificativa em `arq.md`.
- [ ] Estruturar pastas para apresentação Flutter, ponte nativa e motor DNS Kotlin.
- [x] Configurar tema, navegação, telas vazias e estado inicial real “Desativado”.
- [x] Adicionar lint, formatação e análise estática.
- [x] Criar `.gitignore` e verificar ausência de segredos e arquivos locais.
- [ ] Configurar CI no GitHub Actions para análise e testes executáveis. Workflow criado e comandos validados localmente. Falta a primeira execução no GitHub.
- [ ] Gerar e instalar APK de debug em aparelho/emulador. A instalação dos pacotes do Android SDK requer o aceite da licença do SDK.

**Critério de conclusão:** projeto compila e abre sem depender de mocks permanentes.

## Etapa 2 — Integração Flutter ↔ Kotlin

**Objetivo:** comunicação confiável com estado nativo como fonte da verdade.

- [ ] Implementar canal nativo para solicitar ativação e desativação.
- [ ] Implementar consulta do estado real do serviço.
- [ ] Implementar eventos de alteração de estado e erros para Flutter.
- [x] Definir contratos tipados de mensagens, códigos de erro e versões.
- [ ] Tratar recriação da Activity, reabertura e perda temporária do listener.
- [x] Exibir os estados `idle`, `permission_required`, `starting`, `active`, `stopping`, `error` e `revoked`.
- [ ] Criar testes dos contratos da ponte e transições de estado. Testes Flutter passaram com mensagens simuladas. Falta verificar as transições no Android.

Os comandos e eventos nativos foram implementados. Os itens de integração com o Android permanecem pendentes de compilação e execução em dispositivo ou emulador.

**Critério de conclusão:** telas refletem o estado real da camada Android.

## Etapa 3 — Serviço VPN local

**Objetivo:** estabelecer e encerrar corretamente um túnel de escopo DNS, sem root.

- [x] Declarar `VpnService` e permissões necessárias no `AndroidManifest.xml`.
- [x] Exibir explicação clara da VPN antes do pedido de autorização.
- [ ] Solicitar consentimento pelo `VpnService.prepare()` e tratar cancelamento.
- [ ] Implementar serviço Kotlin com ciclo de vida e recurso TUN.
- [ ] Configurar endereço DNS virtual e rotas restritas ao DNS virtual, sem rota padrão de internet.
- [ ] Avaliar e configurar IPv4 e IPv6; registrar limitações de versões/dispositivos.
- [ ] Ativar notificação de serviço em primeiro plano quando exigida.
- [ ] Encerrar descritores, sockets e rotinas corretamente ao desativar.
- [ ] Tratar `onRevoke()`, outra VPN já ativa e reinicialização do serviço.
- [ ] Testar o comportamento com Wi-Fi, dados móveis e perda de conectividade.

O diagnóstico usa rotas IPv4 e IPv6 restritas ao DNS virtual, apenas para o FologneBlocker, e encerra em até 30 segundos. A compilação Android e os testes do serviço, da notificação e da conectividade estão pendentes devido à falta dos pacotes do SDK Android.

**Critério de conclusão:** VPN é ativada/desativada pelo sistema, sem derrubar indevidamente o restante da conectividade. Isso **não** comprova bloqueio DNS.

## Etapa 4 — Leitura e resposta de pacotes DNS

**Objetivo:** processar DNS real recebido pela interface TUN.

- [ ] Implementar leitura/escrita robusta dos pacotes da interface TUN.
- [ ] Interpretar IPv4 e IPv6 e validar comprimentos e cabeçalhos.
- [ ] Implementar DNS sobre UDP/53 com remontagem da resposta IP/UDP e checksums corretos.
- [ ] Tratar respostas inválidas, pacotes malformados e buffers limitados.
- [ ] Suportar perguntas A, AAAA e tipos adicionais sem resposta artificial incorreta.
- [ ] Implementar tratamento adequado de EDNS(0), IDs, flags e códigos de erro relevantes.
- [ ] Implementar DNS sobre TCP/53 ou documentar explicitamente o limite antes de declarar compatibilidade ampla.
- [ ] Testar consultas reais de aplicativos, DNS IPv4/IPv6 e casos de falha.

**Critério de conclusão:** consultas DNS alcançam o serviço, que gera respostas válidas demonstráveis em aparelho real.

## Etapa 5 — Encaminhamento seguro de DNS

**Objetivo:** resolver domínios permitidos sem passar por servidor próprio.

- [ ] Implementar cliente DNS-over-HTTPS (DoH) com timeouts e validação TLS.
- [ ] Definir resolvedor padrão e permitir troca por endpoint compatível.
- [ ] Evitar loops com `VpnService.protect()` nos sockets apropriados.
- [ ] Tratar bootstrap do endereço do resolvedor e troca entre redes.
- [ ] Tratar códigos de resposta, timeouts, respostas truncadas e falhas TLS.
- [ ] Escolher e documentar política de falha: não usar DNS em texto puro silenciosamente.
- [ ] Implementar cache DNS de memória respeitando TTL, se necessário.
- [ ] Testar sites e aplicativos permitidos com VPN ativa em Wi-Fi e rede móvel.

**Critério de conclusão:** resolução permitida é funcional, sem vazamento ou redirecionamento não documentado pelo próprio motor.

## Etapa 6 — Motor de filtragem DNS

**Objetivo:** bloquear domínios reais e respeitar exceções.

- [ ] Implementar normalização de nomes (minúsculas, ponto final e validações).
- [ ] Implementar correspondência exata e de subdomínio por fronteira de rótulo.
- [ ] Definir precedência: permissão manual > bloqueio manual > listas ativadas.
- [ ] Utilizar uma estrutura de busca eficiente e imutável para regras ativas.
- [ ] Implementar resposta DNS de bloqueio consistente e configurada/documentada.
- [ ] Registrar contagem real de consultas permitidas, bloqueadas e falhas.
- [ ] Testar bloqueios sem afetar domínios semelhantes não incluídos nas regras.
- [ ] Validar uso de A e AAAA e consultas repetidas/cache.
- [ ] Verificar bloqueio de domínios controlados de teste em aparelho real.

**Critério de conclusão:** o domínio de teste bloqueado falha, o permitido resolve e os contadores refletem eventos reais.

## Etapa 7 — Listas, configurações e armazenamento

**Objetivo:** fornecer regras atualizáveis com persistência e funcionamento offline.

- [ ] Integrar HaGeZi Multi Normal como fonte inicial de anúncios/rastreadores.
- [ ] Integrar HaGeZi NSFW como lista separada e **desativada por padrão**.
- [ ] Validar URLs, formatos e licenças das listas na data de implementação.
- [ ] Implementar download HTTPS com limite de tamanho e timeout.
- [ ] Implementar parser de listas de domínios sem executar conteúdo remoto.
- [ ] Validar listas baixadas e realizar atualização atômica com rollback.
- [ ] Persistir metadados das fontes, configurações e regras próprias com Room/SQLite.
- [ ] Criar allowlist e blocklist manuais com adicionar/remover/editar.
- [ ] Implementar ativação/desativação de categorias sem reiniciar o app; regenerar snapshot do motor quando necessário.
- [ ] Programar atualização periódica via WorkManager respeitando restrições do Android.
- [ ] Permitir atualização manual e exibir última tentativa, sucesso e erro.
- [ ] Manter última lista válida utilizável quando offline.
- [ ] Testar lista malformada, vazia, indisponível, duplicada e muito grande.

**Critério de conclusão:** regras sobrevivem ao reinício e atualizações não quebram a última configuração válida.

## Etapa 8 — Interface Flutter e estatísticas

**Objetivo:** oferecer controles compreensíveis e dados verdadeiros.

- [ ] Implementar Home com ativar/desativar, estado real e mensagem de erro.
- [ ] Implementar tela de filtros (anúncios/rastreadores e NSFW opcional).
- [ ] Implementar tela de listas e exceções.
- [ ] Implementar tela de estatísticas com contadores reais.
- [ ] Implementar tela de configurações do DNS, privacidade e atualizações.
- [ ] Persistir estatísticas agregadas, sem histórico identificável por padrão.
- [ ] Adicionar opção para limpar estatísticas e configurações personalizadas.
- [ ] Criar estados de carregamento, vazio, erro e acessibilidade básica.
- [ ] Testar interface em tela pequena, orientação e modo claro/escuro.

**Critério de conclusão:** toda ação visual corresponde a uma operação do motor nativo, sem dados fictícios.

## Etapa 9 — Estabilidade, desempenho e compatibilidade

**Objetivo:** tornar o serviço confiável em uso prolongado.

- [ ] Medir CPU, RAM, aquecimento e consumo de bateria sob carga representativa.
- [ ] Evitar trabalho excessivo, polling contínuo e escrita em disco por consulta.
- [ ] Testar uso prolongado e transições de primeiro/segundo plano.
- [ ] Testar troca Wi-Fi ↔ dados móveis e retorno após perda de rede.
- [ ] Testar resolução DNS privado/DoT, DoH embutido e endpoints externos; documentar bypasses possíveis.
- [ ] Testar conflitos com outras VPNs e regras Android de VPN sempre ativa.
- [ ] Testar interrupção do processo, reinicialização e recuperação quando suportada.
- [ ] Investigar páginas e aplicativos quebrados por falso positivo.
- [ ] Criar testes unitários e instrumentados para regressões essenciais.

**Critério de conclusão:** testes documentados sem falhas críticas conhecidas e limitações explicitadas.

## Etapa 10 — Segurança, privacidade e conformidade

**Objetivo:** reduzir riscos técnicos e de publicação.

- [ ] Revisar logs e excluir domínios, IPs e identificadores desnecessários por padrão.
- [ ] Documentar transferência de consultas permitidas ao resolvedor DoH escolhido.
- [ ] Implementar consentimento claro e política de privacidade compatível com a operação real.
- [ ] Revisar `AndroidManifest`, permissões e tipos de foreground service segundo SDK de publicação.
- [ ] Revisar licenças de bibliotecas e condições de uso/redistribuição das listas.
- [ ] Revisar dependências e executar análise de segurança.
- [ ] Verificar que não existe inspeção HTTPS, certificado CA próprio ou captura indevida de conteúdo.
- [ ] Documentar limitações, falsos positivos e procedimento de recuperação.
- [ ] Se publicar na Play Store, avaliar política atual de `VpnService`, declarações e aprovação específica.

**Critério de conclusão:** documentação e comportamento do app são consistentes e revisados.

## Etapa 11 — Distribuição e manutenção

**Objetivo:** disponibilizar uma versão experimental rastreável.

- [ ] Definir nome de pacote definitivo, versão e assinatura de distribuição.
- [ ] Gerar APK de release assinado e testar instalação/atualização.
- [ ] Escrever instruções de instalação e desinstalação.
- [ ] Criar `CHANGELOG.md` e notas de versão com problemas conhecidos.
- [ ] Publicar GitHub Release com artefatos e checksums.
- [ ] Configurar fluxo de issues, reports de bugs e contribuições.
- [ ] Planejar correções de segurança e atualizações compatíveis com novos Androids.
- [ ] Considerar publicação em loja **somente** após auditoria técnica e das políticas vigentes.

**Critério de conclusão:** release experimental reproduzível, testada e com limitações visíveis.

---

## Marco do primeiro MVP funcional

O MVP **não** depende de todas as melhorias das etapas finais, mas exige:

- [ ] VPN consentida e ativada em aparelho Android real.
- [ ] Consultas DNS permitidas resolvidas por DoH com sucesso.
- [ ] Pelo menos um domínio de teste bloqueado com resposta DNS válida.
- [ ] Pelo menos um domínio permitido resolvido normalmente.
- [ ] Interface exibe o estado real do serviço.
- [ ] Contadores refletem o processamento real, não valores simulados.
- [ ] Desativar a VPN restaura a rede normal.
- [ ] Limitações importantes e comportamento de falha documentados.

## Histórico de progresso

| Data | Etapa | Alteração | Evidência/teste |
|---|---|---|---|
| 2026-11-13 | 0 | Escopo, arquitetura e checklist revisados. README, privacidade, segurança, fontes e plano de teste adicionados. | Documentos conferidos e licença das listas consultada no repositório HaGeZi. A disponibilidade do reporte privado no GitHub ainda precisa ser confirmada. |
| 2026-11-13 | 1 | Base Flutter Android/Kotlin, navegação, tema, lint, teste de widget, GitHub Actions e exclusões do Git. | Windows com Flutter 3.44.0 e Dart 3.12.0. `flutter pub get`, `dart format`, `flutter analyze` e `flutter test` passaram. `git check-ignore` conferiu exclusões e arquivos versionáveis. `flutter build apk --debug` falhou por falta do Android SDK instalado. |
| 2026-11-13 | 2–3 | Renomeação para FologneBlocker, contrato dos canais v1 e serviço de diagnóstico VPN com consentimento e estado nativo. | `flutter pub get`, `dart format`, `flutter analyze` e três testes Flutter passaram. `flutter build apk --debug` falhou porque os pacotes do SDK Android ainda precisam ser instalados. |

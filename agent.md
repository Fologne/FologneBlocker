# Instruções para o Codex — FologneBlocker

> **Arquivo canônico de instruções de desenvolvimento:** `agent.md`.
> **Integração com Codex:** mantenha também `AGENTS.md` na raiz: o Codex descobre automaticamente `AGENTS.md` e deve ler este documento antes de trabalhar.
> **Projeto:** FologneBlocker, Android, Flutter + Kotlin, open source.
> **Aplicação:** estas regras se aplicam a qualquer alteração de código, configuração, documentação, testes ou releases neste repositório.

## 1. Missão e limites do produto

Desenvolver um aplicativo Android para reduzir anúncios e rastreamento por **filtragem DNS local**, com categoria **opcional** para bloquear domínios adultos. O usuário controla a ativação da proteção. A motivação inclui reduzir a exibição de anúncios sexualizados ou de aplicativos adultos, **sem alegar que DNS é capaz de identificar o conteúdo visual de um anúncio**.

O aplicativo deve funcionar **sem root**, utilizando `VpnService` do Android para uma VPN local com escopo DNS. O objetivo inicial é disponibilizar um APK experimental pelo GitHub, não construir um serviço de VPN remota. Não existe backend próprio no MVP.

**Nunca prometer:** bloqueio de 100% dos anúncios; eliminação universal de publicidade do YouTube, Instagram ou outros apps; captura de todo DNS de terceiros; filtragem de anúncios que compartilham domínio com conteúdo legítimo; funcionamento simultâneo com outra VPN; ou identificação automática de imagens e vídeos adultos.

## 2. Documentos obrigatórios e precedência

Antes de alterar o repositório, **ler integralmente**, quando existirem:

1. `escopo.md` — funcionalidades, exclusões, requisitos e critérios de aceitação do produto.
2. `arq.md` — decisões de arquitetura, interfaces, protocolo DNS e responsabilidades das camadas.
3. `status.md` — etapas, checklist, marcos, critérios de conclusão e progresso efetivo.
4. `agent.md` — regras permanentes de trabalho, segurança, qualidade e entrega.
5. `README.md`, `PRIVACY.md`, `SECURITY.md` e outros arquivos existentes que afetem a tarefa.

A **solicitação explícita do usuário** determina a tarefa atual. Para requisitos do produto, consultar `escopo.md`; para detalhes técnicos, `arq.md`; para ordem e progresso, `status.md`; para modo de execução e precauções, este arquivo. Se houver conflito, **não decidir silenciosamente**: informar o conflito e propor a menor alteração coerente. Não reescrever escopo ou arquitetura apenas por preferência pessoal.

Se um arquivo referenciado ainda não existir, registrar a ausência e seguir somente o que puder ser feito corretamente. Não inventar conteúdo ou afirmar que leu arquivos inexistentes.

## 3. Stack definida — não substituir sem autorização

| Área | Escolha do projeto |
|---|---|
| Interface Android | Flutter + Dart |
| Núcleo de rede | Kotlin nativo |
| Ponte entre camadas | Platform Channels ou Pigeon, com contrato claro |
| VPN local | Android `VpnService` + interface TUN |
| Filtragem | DNS e regras de domínio/subdomínio |
| Resolução de consultas permitidas | DNS-over-HTTPS (DoH) sobre TLS |
| Persistência local | Room/SQLite; Kotlin como proprietário dos dados |
| Atualizações | HTTPS e WorkManager quando aplicável |
| Distribuição inicial | APK e GitHub Releases |

**Não adicionar** Django, Node.js, Firebase, Supabase, um servidor próprio, login, assinatura, pagamentos, analytics ou serviços de telemetria sem uma decisão explícita e registrada de expansão de escopo.

A UI Flutter exibe dados e solicita ações. **Toda captura de pacotes, análise DNS, aplicação de regras, resolução e persistência operacional são responsabilidades do Kotlin.** Não colocar o motor DNS no Dart nem consultar banco de dados diretamente do Flutter se isso duplicar a fonte da verdade.

## 4. Fluxo de trabalho obrigatório

### 4.1 Antes de programar

1. Ler a solicitação atual e os documentos aplicáveis.
2. Verificar `git status` e preservar arquivos e mudanças anteriores do usuário.
3. Localizar no `status.md` a **etapa e as tarefas relacionadas** ao pedido.
4. Identificar dependências, compatibilidade de SDK, requisitos Android e riscos de privacidade/licenciamento.
5. Para tarefas amplas, descrever brevemente o plano e os critérios de validação antes de implementar.
6. Se for necessário alterar o escopo ou a arquitetura, justificar a mudança e atualizar os documentos relacionados de forma consistente, quando autorizado.

### 4.2 Durante a implementação

- Executar **somente a etapa/tarefa solicitada**, avançando à seguinte apenas quando o usuário pedir ou autorizar a execução sequencial.
- Implementar em incrementos pequenos, compiláveis e verificáveis.
- Preferir alterações mínimas e soluções nativas consolidadas; evitar complexidade, dependências e abstrações desnecessárias.
- Não criar dezenas de classes vazias só para preencher a estrutura sugerida em `arq.md`.
- Não substituir funcionalidades reais por mocks duradouros ou resultados inventados.
- Mocks são permitidos **apenas nos testes** ou quando forem claramente temporários e identificados.
- Explicar decisões que afetam privacidade, comportamento da rede, desempenho, permissões ou compatibilidade.
- Manter comentários e documentação em **português do Brasil**, salvo APIs, identificadores, nomes técnicos e convenções já adotadas.
- Código legível, nomes consistentes, tratamento explícito de erro e ausência de segredos em texto puro.

### 4.3 Após modificar código

1. Executar formatação, análise estática, testes e build **disponíveis e pertinentes** à alteração.
2. Relatar comandos executados e resultados reais; distinguir sucesso, falha e teste não executado.
3. Atualizar `status.md` **somente para tarefas comprovadamente concluídas**. Use `[x]` quando houver evidência suficiente; mantenha `[ ]` em tarefas parciais ou sem comprovação.
4. Não marcar testes em dispositivo físico como concluídos se foi usado apenas emulador ou análise de código.
5. Se a alteração muda comportamento/limites, atualizar `escopo.md`, `arq.md` e documentação de usuário, conforme necessário.
6. Resumir ao usuário: arquivos alterados, funcionalidade efetiva, evidências de teste, pendências e próximo item do `status.md`.

**Proibido:** tratar interface pronta como VPN funcional; tratar VPN estabelecida como prova de filtragem DNS; afirmar que anúncios foram bloqueados sem testar consultas; fabricar estatísticas, logs, medições ou testes.

## 5. Regras de Git e repositório público

O repositório foi concebido para ser **público**. Agir como se tudo que fosse commitado pudesse ser visto, copiado e auditado por terceiros.

- Não publicar tokens, senhas, cookies, chaves de API, credenciais de CI, arquivos `.env`, arquivos de assinatura Android (`.jks`, `.keystore`), senhas de keystore, `key.properties`, dados pessoais ou relatórios de tráfego identificáveis.
- Configurar e preservar `.gitignore` para arquivos locais, saídas de build, artefatos de desenvolvimento e credenciais. Conferir também arquivos **já rastreados** antes de commits.
- Não registrar consultas DNS reais do proprietário do dispositivo em commits, issues, screenshots ou fixtures de teste. Usar domínios de exemplo ou dados sintéticos.
- Não executar `git push --force`, reescrever histórico, apagar branches, limpar o repositório com comandos destrutivos, publicar releases ou realizar mudanças remotas **sem instrução explícita**.
- Não sobrescrever alterações alheias ou remover arquivos não relacionados à tarefa.
- Não declarar conformidade, segurança, desempenho ou funcionamento sem evidências.
- Manter `README.md` com descrição correta, instalação, requisitos, limitações, fontes externas, aviso de privacidade e licença adotada.
- Preferir CI com permissões mínimas, dependências confiáveis e sem exposição de segredos em builds de pull requests não confiáveis.
- Antes de distribuir APK, verificar assinatura, dependências, licenças, dados incluídos e se há materiais internos ou privados no pacote.

**Aspecto jurídico:** publicar um bloqueador de anúncios não é, por si só, uma garantia de legalidade absoluta nem de ausência de reclamações. Codex não deve emitir parecer jurídico ou prometer imunidade a processos. Questões de licenças, marcas, direitos autorais, privacidade e políticas de plataformas devem ser verificadas antes da distribuição.

## 6. Licenças, propriedade intelectual e crédito

- **Não selecionar automaticamente** MIT, Apache-2.0, GPL ou qualquer licença para o aplicativo sem revisar as licenças efetivas das dependências e a decisão do mantenedor.
- Distinguir sempre **código autoral**, **bibliotecas de terceiros**, **listas de domínios** e **artefatos redistribuídos**.
- As listas **HaGeZi DNS Blocklists** são publicadas sob **GPL-3.0**; verificar licença, atribuição, eventuais avisos e condições vigentes **antes de copiar, modificar, embutir em APK ou redistribuir**. Não presumir que baixar uma lista automaticamente concede qualquer uso sem obrigações nem que a licença dela automaticamente define a licença do código independente do app.
- Preferir baixar as listas de suas fontes originais por HTTPS e informar origem, autoria e licença ao usuário, observando as obrigações aplicáveis.
- Não copiar código proprietário de bloqueadores comerciais, interfaces inteiras, logotipos, nomes comerciais nem recursos protegidos sem autorização/licença.
- Evitar usar marcas de Google, AdGuard e outros de forma que sugira associação, apoio ou produto oficial.
- Criar/atualizar avisos de terceiros (por exemplo `THIRD_PARTY_NOTICES.md`) quando houver material sujeito a atribuição.
- Não remover avisos de copyright e licença de material de terceiros.
- Se a compatibilidade jurídica/licenciamento não estiver clara, **não empacotar o recurso duvidoso**: registrar a pendência e pedir decisão do mantenedor.

Fontes para conferir, não substituir por suposições:
- HaGeZi: <https://github.com/hagezi/dns-blocklists> e <https://github.com/hagezi/dns-blocklists/blob/main/FAQ.md>.
- Licenças de bibliotecas: repositórios oficiais, metadados dos pacotes e arquivos `LICENSE` correspondentes.

## 7. Privacidade e proteção de dados

Princípio: **mínima coleta, processamento local e transparência**.

- Não criar cadastro, perfis de navegação, analytics, publicidade dentro do app, telemetria própria ou envio de logs a servidor do projeto no MVP.
- Guardar apenas configurações, metadados de listas e **estatísticas agregadas reais**. Histórico de domínios/IPs consultados fica **desativado por padrão** e **não é requisito do MVP**.
- Nunca incluir domínios visitados, endereços IP, identificadores de apps ou conteúdo sensível em logs de produção por padrão.
- Se forem necessários logs de depuração, torná-los temporários, mínimos, desativados em release e sem dados pessoais sempre que possível.
- Informar claramente que **consultas DNS permitidas serão enviadas ao provedor DoH selecionado**. Não dizer que “nenhum dado sai do aparelho”.
- Explicar a finalidade da VPN antes de solicitar autorização do Android; não esconder a operação nem pedir permissões sem necessidade.
- Não instalar certificados raiz, executar MITM, descriptografar HTTPS ou fazer inspeção de mensagens e conteúdo de outros aplicativos.
- Tratar listas e respostas externas como **dados não confiáveis**: validar tamanho, formato, URL, resposta HTTP/TLS e limites de memória.
- Se um recurso futuro envolver dados pessoais, avaliar obrigações aplicáveis da **LGPD** antes de implementá-lo.
- Documentar coleta, armazenamento, compartilhamentos externos, retenção, exclusão e permissões efetivos em `PRIVACY.md` antes da distribuição pública.

## 8. Segurança e política de uso da VPN

- Usar somente APIs Android oficiais; o app funciona **sem root**.
- Solicitar permissão por `VpnService.prepare()` e respeitar recusa, cancelamento, `onRevoke()`, encerramento do processo e política de execução em segundo plano.
- Declarar apenas permissões justificáveis e observar requisitos de foreground service e manifest para as versões Android suportadas.
- Evitar configuração que intercepte indiscriminadamente tráfego normal: o desenho preferido é uma **VPN DNS-only**, com rota restrita ao DNS virtual sempre que a plataforma permitir.
- Tratar IPv4 e IPv6, TCP/UDP DNS, cabeçalhos, checksums, tamanhos, concorrência, tempo limite e falhas da rede corretamente dentro do escopo implementado.
- Evitar loops na resolução DoH (avaliando uso correto de `VpnService.protect()` e bootstrap do resolvedor).
- **Não usar fallback silencioso para DNS sem criptografia** quando o aplicativo promete resolver consultas permitidas via DoH. Exibir a falha de modo compreensível.
- Preservar rede e estabilidade: impedir deadlocks, vazamento de sockets, corrupção de pacotes, perda de conectividade não explicada e consumo excessivo de bateria.
- Não interpretar bloqueio de domínio como inspeção do tipo de anúncio. Listas NSFW **separadas e desligadas por padrão**.
- Não usar AccessibilityService, overlays abusivos ou recursos de acesso sensível para inspecionar outros apps.
- Não implementar manipulação de tráfego para monetização, injeção/substituição de anúncios ou desvio comercial de requisições de publicidade.
- Não contornar restrições de segurança do Android, do usuário ou das lojas.
- Nunca comunicar que uma VPN local DNS-only cria um túnel criptografado remoto completo ou um kill switch universal.

**Publicação na Google Play:** não presumir aprovação. Antes de publicar, revisar política atual de `VpnService`, declaração de uso, consentimentos, privacidade, classificação, requisitos Android e regras contra manipulação de tráfego publicitário. A distribuição inicial é por GitHub Releases; uma release pública também exige as revisões de segurança e licenças pertinentes.

Referências oficiais para verificação:
- Android VPN: <https://developer.android.com/develop/connectivity/vpn>.
- Política `VpnService` do Google Play: <https://support.google.com/googleplay/android-developer/answer/12564964?hl=pt-BR>.

## 9. Critérios técnicos para implementação

### 9.1 Motor DNS

- Filtrar por **domínio normalizado**, com comparação por fronteira de rótulos e suporte a subdomínios.
- Precedência: **allowlist manual > blocklist manual > listas remotas habilitadas > resolução normal**.
- Uma regra para `ads.example.com` não deve atingir `notads.example.com`.
- Responder a bloqueios com mensagem DNS válida e corretamente associada à requisição; não retornar bytes arbitrários.
- Tratar registros A e AAAA, IDs, flags, perguntas, respostas, compressão, EDNS(0) e erros conforme suporte declarado.
- Priorizar DNS UDP/53 no primeiro protótipo, mas documentar e tratar o que faltar para DNS TCP/53, IPv6 e compatibilidade ampla antes de alegar estabilidade.
- Reconhecer bypass por DNS privado/DoT/DoH de terceiros, conexões IP diretas e anúncios em domínios de conteúdo legítimo.

### 9.2 Listas remotas

Fontes previstas, sujeitas a verificação na implementação:

- Anúncios/rastreadores: `https://cdn.jsdelivr.net/gh/hagezi/dns-blocklists@latest/wildcard/multi-onlydomains.txt`
- Conteúdo adulto, **opcional**: `https://cdn.jsdelivr.net/gh/hagezi/dns-blocklists@latest/wildcard/nsfw-onlydomains.txt`

- Validar se esses endpoints e os formatos são compatíveis com o parser **antes de depender deles**.
- Download HTTPS com timeout e limites; sem execução de conteúdo remoto.
- Parser resistente a dados malformados, vazios, gigantes, duplicados e caracteres inválidos.
- Compilar regras em snapshot imutável para consultas rápidas.
- Atualizar atomicamente, preservando a última versão válida em falhas; nunca eliminar toda proteção porque uma atualização falhou.
- Registar fonte, data, hash/versão, tamanho e resultado da atualização **sem registrar navegação do usuário**.
- Não armazenar listas inteiras como um único campo de banco se isso degradar desempenho; escolher formato proporcional ao volume.

### 9.3 Integração Flutter/Kotlin

- Kotlin é a fonte da verdade de `idle`, `permission_required`, `starting`, `active`, `stopping`, `error` e `revoked`.
- `active` só depois de estabelecer o TUN; estado de bloqueio efetivo só depois de demonstrar processamento de consultas.
- Definir métodos/eventos tipados para iniciar/parar, consultar estado, listar fontes, mudar filtros, gerenciar regras, visualizar estatísticas e atualizar listas.
- Tratar abertura/fechamento da interface, recriação de Activity, retomada e perda de observador.
- Todas as telas devem mostrar dados reais ou estados vazios explícitos. Nenhum contador inventado.

### 9.4 Desempenho e robustez

- Operações de rede e TUN fora da thread principal; recursos e coroutines encerrados corretamente.
- Evitar consulta ao banco no caminho quente de cada resolução DNS; usar estruturas em memória e agregação periódica de métricas.
- Limitar concorrência, fila, tamanho de pacotes, memória, tempo de espera e tamanho de downloads.
- Tratar trocas Wi-Fi/dados móveis, falta de conexão, falha TLS, indisponibilidade DoH e conflito com outra VPN.
- Compatibilidade deve ser baseada em versões de Android realmente testadas, não em valores inventados.

## 10. Testes e definição de concluído

**Sem testes/evidências, a tarefa continua pendente ou é descrita como parcialmente concluída.** Quando disponíveis, utilizar:

```bash
flutter pub get
flutter analyze
flutter test
flutter build apk --debug
```

Para testes Kotlin/Gradle, usar tarefas existentes no projeto (por exemplo `./gradlew testDebugUnitTest` dentro de `android/`; no Windows, `./gradlew.bat` ou `.\\gradlew.bat` conforme o terminal). Não afirmar que comandos foram executados se não puderam rodar.

Exigir testes conforme a mudança:

- Regras: normalização, domínio exato, subdomínios, precedência e falsos positivos.
- DNS: pacotes válidos/inválidos, A/AAAA, IDs, checksums, UDP, IPv4/IPv6 e TCP quando suportado.
- DoH: resposta válida, timeout, indisponibilidade, TLS inválido, bootstrap e troca de rede.
- Listas: download, parsing, duplicatas, lista vazia/gigante, atualização atômica, rollback e modo offline.
- Bridge: contratos, erro, cancelamento de permissão e estados nativos.
- UI: ativação, falha, configurações, persistência, estatísticas e acessibilidade básica.
- Dispositivo Android real: ativar VPN, resolver domínio permitido, bloquear domínio de teste, manter conectividade e desligar a proteção.

**Marco mínimo de MVP:** (a) VPN autorizada e estabelecida; (b) DNS permitido resolvido via DoH; (c) domínio de teste bloqueado com resposta válida; (d) contadores nativos verdadeiros; (e) desativação restaura conectividade; (f) falha de atualização preserva listas válidas.

Não inventar teste físico quando só houver emulador ou CI. Registrar precisamente o ambiente de validação.

## 11. Checklist e documentação de progresso

- Atualizar `status.md` apenas em tarefas afetadas, preservando formato e etapas já existentes.
- `[ ]` = não concluído ou sem verificação suficiente; `[x]` = concluído com evidência disponível.
- Evitar percentuais e resumos que façam parecer concluído o que ainda depende de testes físicos.
- Quando descobrir impeditivos, descrevê-los no relatório e, se necessário, registrar nota de pendência junto à tarefa relevante.
- Manter a **Etapa 0 exclusivamente documental**, como definido em `status.md`.
- Não expandir o número de etapas, mover tarefas ou renomear arquivos sem justificativa ligada à solicitação do usuário.
- Não adicionar funcionalidade fora do escopo no meio de uma etapa; propor uma alteração para avaliação.

## 12. Padrão de resposta do Codex ao finalizar cada tarefa

Responder concisamente, em português, contendo:

1. **Implementado:** resultado real, sem exageros.
2. **Arquivos alterados:** caminhos e finalidade das principais alterações.
3. **Verificações:** testes/comandos executados e seus resultados; mencionar o que não foi testado.
4. **`status.md`:** quais itens foram marcados `[x]` e quais permaneceram `[ ]`.
5. **Limitações e riscos:** especialmente rede, privacidade, licenças e Android.
6. **Próximo passo:** próximo item pertinente; não iniciá-lo por conta própria se não foi solicitado.

## 13. Condutas expressamente proibidas

- Gerar somente uma interface e dizer que o aplicativo já bloqueia anúncios.
- Fazer com que o contador cresça por temporizador sem consultas reais.
- Simular aprovação de VPN, respostas DNS, testes físicos ou sucesso de build.
- Adicionar rastreamento, anúncios internos, acesso oculto a dados ou envio de navegação a servidores próprios.
- Introduzir MITM, interceptação HTTPS, certificados raiz ou root sem alterar explicitamente o escopo e realizar análise de segurança (todos fora do MVP).
- Copiar código, listas, imagens, ícones ou marcas de terceiros sem conferir autorização/licença.
- Declarar que o projeto não oferece nenhum risco jurídico, que a Google Play aceitará o APK ou que bloqueia todos os anúncios.
- Introduzir fallback DNS não criptografado sem ciência do usuário.
- Fazer commits que exponham dados sensíveis ou alterar histórico Git de modo destrutivo.

---

**Regra final:** priorize um aplicativo **funcional, auditável, seguro, testável e honesto sobre suas limitações**. Obedeça ao escopo, siga a arquitetura, mantenha o status verídico e proteja os usuários e os colaboradores de riscos desnecessários.

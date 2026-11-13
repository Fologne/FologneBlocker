# ESCOPO — FologneBlocker

> Este documento estabelece o objetivo e os limites do **FologneBlocker**. Mudanças devem ser refletidas em `arq.md` e `status.md`.

## 1. Visão geral

Desenvolver um aplicativo **Android**, sem root, com interface **Flutter** e motor nativo **Kotlin**, que use uma **VPN local limitada a DNS** para bloquear a resolução de domínios associados a anúncios, rastreamento e, opcionalmente, conteúdo adulto.

A motivação é reduzir publicidade intrusiva, especialmente anúncios sexualizados ou de aplicativos adultos exibidos no celular. O filtro age **sobre domínios DNS**, não sobre o significado de imagens, vídeos ou textos dos anúncios.

O projeto será inicialmente **open source e distribuível pelo GitHub**. O funcionamento básico não exigirá login, servidor próprio ou mensalidade.

## 2. Objetivos

1. Reduzir anúncios e rastreadores onde o bloqueio por domínio for tecnicamente possível.
2. Oferecer controle explícito sobre filtragem de domínios de conteúdo adulto, independentemente do bloqueio geral de anúncios.
3. Preservar a privacidade: configurações e estatísticas locais, sem telemetria própria por padrão.
4. Manter uso simples: ativação, estado da proteção, exceções e atualização de listas.
5. Funcionar em Wi-Fi e redes móveis, respeitando os limites reais do Android.
6. Disponibilizar código auditável e documentação clara.

## 3. Plataforma e tecnologia

| Camada | Tecnologia definida | Papel |
|---|---|---|
| Interface | Flutter / Dart | Telas, estado visual, ações do usuário |
| Integração | Platform Channels ou Pigeon | Contratos Flutter ↔ Android |
| Núcleo | Kotlin | VPN, DNS, regras, persistência |
| VPN | Android `VpnService` | Túnel TUN local para DNS |
| Resolução externa | DNS-over-HTTPS (DoH) | Consultas autorizadas a provedor escolhido |
| Persistência | Room / SQLite | Configuração, metadados e estatísticas |
| Atualização | HTTPS + WorkManager | Downloads e atualização periódica de listas |
| Distribuição inicial | APK / GitHub Releases | Instalação independente da Play Store |

As versões de SDK e dependências serão definidas após testes de compatibilidade, não inventadas antecipadamente.

## 4. Público-alvo

Usuários Android que desejam reduzir publicidade, rastreamento e acesso a domínios adultos conforme suas escolhas, sem root ou configuração manual de DNS em cada rede.

## 5. Funcionalidades incluídas

### RF-01 — Controle da proteção
- Iniciar e parar VPN mediante consentimento do Android.
- Exibir estado **real** do serviço e possíveis erros.
- Manter proteção independente de a interface Flutter estar aberta, quando o Android permitir.

### RF-02 — Interceptação e filtragem DNS
- Capturar consultas direcionadas ao DNS virtual configurado pelo próprio serviço.
- Aplicar regras de domínio exato e subdomínio.
- Gerar respostas DNS corretas para nomes bloqueados.
- Encaminhar consultas permitidas a resolvedor DoH.
- Tratar famílias IPv4/IPv6 e protocolos DNS UDP/TCP conforme compatibilidade implementada e testada.

### RF-03 — Categorias
- Bloqueio de publicidade/rastreamento por lista geral.
- Bloqueio **opcional** de domínios adultos com controle próprio, desligado inicialmente.
- Categorias adicionais somente após validação do MVP.

### RF-04 — Gerenciamento de listas
- Integrar fonte remota inicial de anúncios/rastreadores e fonte NSFW.
- Baixar e atualizar por HTTPS.
- Validar formatos e preservar a última lista funcional caso atualização falhe.
- Exibir origem, estado, data e falha da atualização.

### RF-05 — Regras próprias
- Adicionar/remover domínios em bloqueio manual.
- Adicionar/remover domínios em lista de permissão (allowlist).
- Definir precedência clara entre regras manuais e listas.

### RF-06 — Estatísticas
- Mostrar consultas DNS processadas, bloqueadas e permitidas.
- Mostrar período de referência e opção para limpar os números.
- Não afirmar métricas que não sejam medidas pelo motor nativo.

### RF-07 — Configurações
- Escolher ou alterar provedor DoH válido.
- Controlar atualização de listas e categorias.
- Exibir alertas sobre problemas de conectividade, DNS privado e outra VPN.

### RF-08 — Transparência e privacidade
- Explicar por que a VPN é necessária e pedir autorização do Android.
- Informar que consultas permitidas são entregues ao provedor DNS escolhido.
- Guardar configurações e estatísticas localmente.
- Deixar histórico detalhado de domínios **desligado por padrão** (pode não existir no MVP).

## 6. Requisitos não funcionais

- **RNF-01 Privacidade:** sem conta, analytics ou backend próprio no MVP.
- **RNF-02 Segurança:** TLS para DoH e HTTPS de atualização; nenhuma interceptação HTTPS/CA.
- **RNF-03 Resiliência:** atualizações atômicas; recuperar estados de serviço; comunicar erros.
- **RNF-04 Desempenho:** consultas processadas com baixa latência; listas indexadas em memória.
- **RNF-05 Bateria:** evitar polling e persistência de eventos a cada consulta.
- **RNF-06 Usabilidade:** linguagem simples, status inequívoco e ações reversíveis.
- **RNF-07 Compatibilidade:** testar nas versões Android suportadas; documentar dispositivos e restrições.
- **RNF-08 Auditabilidade:** código aberto, fontes de listas identificadas e licenças respeitadas.

## 7. Fontes externas inicialmente previstas

- **HaGeZi Multi Normal** — anúncios e rastreadores: `https://cdn.jsdelivr.net/gh/hagezi/dns-blocklists@latest/wildcard/multi-onlydomains.txt`
- **HaGeZi NSFW** — domínios adultos (opcional): `https://cdn.jsdelivr.net/gh/hagezi/dns-blocklists@latest/wildcard/nsfw-onlydomains.txt`
- Repositório: <https://github.com/hagezi/dns-blocklists>

**Importante:** conferir existência, formato, licenciamento **GPL-3.0** e condições de redistribuição antes de publicar binários com listas incorporadas. Não confundir bloqueio de domínios adultos com identificação de anúncios sexualizados. Preferir download de fontes externas e registrar claramente os créditos e as licenças.

## 8. Fora do escopo do MVP

- Versão iOS, desktop, navegador ou extensão de browser.
- Servidor remoto, Django, autenticação, assinatura ou sincronização em nuvem.
- Root, alteração da ROM, AccessibilityService para inspecionar outros apps.
- Inspeção de conteúdo HTTPS, instalação de certificados CA, MITM ou análise visual por IA.
- Bloqueio garantido de publicidade do YouTube, Instagram ou qualquer anúncio servido no domínio legítimo.
- Remoção de banners de layout vazios deixados por outros aplicativos.
- Bloqueio universal de DNS próprio, DNS-over-HTTPS, DNS-over-TLS ou conexões diretas por IP em apps terceiros.
- Uso simultâneo com outra VPN Android, se o sistema não oferecer suporte.
- Funcionalidades corporativas, controle parental remoto, inspeção de terceiros ou coleta centralizada.
- Publicação automática na Google Play.

## 9. Restrições e riscos conhecidos

1. **DNS não inspeciona conteúdo:** não distingue um anúncio adulto de um anúncio comum quando ambos vêm do mesmo domínio.
2. **Bypass de DNS:** aplicativos podem utilizar resolvers próprios, DNS privado, DoH/DoT ou IPs diretos, fora do alcance do túnel DNS-only.
3. **Conflito de VPN:** o Android normalmente permite uma VPN ativa por perfil de usuário; uso simultâneo pode falhar.
4. **Falsos positivos:** uma lista pode impedir o funcionamento de recursos legítimos; deve haver allowlist.
5. **Ambiente Android:** fabricantes e restrições de bateria podem interromper processos ou serviços.
6. **Terceiros:** disponibilidade de listas e resolvedores DoH depende de provedores externos.
7. **Licenças e lojas:** é necessário revisar licenças dos recursos e políticas de distribuição antes de lançar.
8. **Privacidade:** ainda que o app não envie logs para servidor próprio, o resolvedor DoH recebe as consultas permitidas.

## 10. Telas previstas

1. **Home:** ativar/desativar, estado da VPN, resumo de consultas.
2. **Filtros:** categorias de anúncios/rastreadores e adulto opcional.
3. **Listas:** fontes externas, inclusão de domínios permitidos/bloqueados.
4. **Estatísticas:** contagens reais e limpeza.
5. **Configurações:** endpoint DoH, atualizações, privacidade e informações técnicas.
6. **Consentimento:** explicação da VPN antes da janela do Android.

## 11. Critérios de aceitação do MVP

O MVP será considerado funcional quando, **em um Android real**:

- A VPN local for autorizada, iniciada e desligada sem travar a conectividade.
- Uma consulta a domínio permitido retornar resposta DNS funcional pelo resolvedor DoH.
- Uma consulta a domínio presente na blocklist retornar resposta de bloqueio válida.
- A interface informar corretamente o estado do serviço e contagens reais.
- A VPN continuar operando com a interface fechada enquanto o sistema mantiver o serviço.
- Uma falha de atualização não eliminar a última lista válida.
- O processo de desligamento liberar o túnel e restaurar a resolução normal.
- Limitações conhecidas (DoH de apps, DNS privado, outras VPNs) estiverem documentadas.

## 12. Entregas

- Repositório GitHub com `README.md`, `escopo.md`, `arq.md` e `status.md`.
- Projeto Flutter com código nativo Kotlin e testes.
- Documentação de privacidade, segurança, fontes e licenças.
- APK de debug durante desenvolvimento e APK assinado apenas para a release.
- Evidências de testes manuais do bloqueio de DNS.

## 13. Evoluções possíveis, após o MVP

- Filtros por aplicativo, quando tecnicamente viáveis com `VpnService`.
- Mais fontes de regras compatíveis e perfis de bloqueio.
- Exportação/importação de configuração.
- Widget ou atalho de ativação.
- Diagnósticos locais para identificar falsos positivos, **somente com consentimento**.
- Outras plataformas, sujeitas a uma arquitetura de núcleo diferente.

## 14. Controle de mudanças

Qualquer funcionalidade nova deve primeiro alterar este escopo, depois a arquitetura (`arq.md`) e as tarefas (`status.md`).

# Plano de teste manual em Android real

Execute os cenários conforme cada recurso estiver disponível. Registre versão do APK, modelo e API do Android, tipo de rede, resultado observado e falhas. Use apenas domínios sintéticos ou controlados pelo teste e remova dados pessoais das evidências.

| Cenário | Procedimento e resultado esperado |
|---|---|
| Instalação | Instalar o APK de debug, abrir o app e verificar o estado Desativado. |
| Consentimento | Recusar a autorização de VPN e confirmar que a proteção segue desativada. Repetir e autorizar. Conferir o estado ativo somente após estabelecer o túnel. |
| Consulta permitida | Resolver um domínio controlado com registros A e AAAA e confirmar a resposta e o encaminhamento ao provedor DoH com evidência de teste. |
| Consulta bloqueada | Bloquear um domínio controlado, repetir a consulta com cache limpo e validar o identificador, a pergunta e o código DNS esperado. Verificar que um domínio semelhante fora da regra ainda resolve. |
| Contadores | Registrar os agregados iniciais, executar consultas controladas e verificar a variação por resultado. |
| Conectividade | Repetir consultas em Wi-Fi e rede móvel, alternar a rede e conferir a recuperação. Desativar a VPN e confirmar que a resolução normal volta. |
| Ciclo de vida | Fechar e reabrir a interface durante a proteção, revogar a VPN nas configurações do Android e conferir estado e conectividade. |
| Falhas externas | Indisponibilizar o DoH e verificar erro sem fallback em texto puro. Induzir falha na atualização e confirmar que a última lista válida segue em uso. |

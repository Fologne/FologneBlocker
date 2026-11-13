# Privacidade

O diagnóstico do FologneBlocker solicita autorização de VPN pelo Android e abre um túnel restrito ao próprio app por até 30 segundos. Nesta fase, o app não lê o TUN, não encaminha consultas DNS e não coleta histórico, estatísticas ou telemetria.

## Tratamento previsto para o MVP

- A VPN local exigirá autorização do Android após uma explicação na interface. O aplicativo usará a autorização para encaminhar ao filtro as consultas destinadas ao DNS virtual.
- O motor Kotlin processará consultas localmente. O provedor DoH selecionado receberá as consultas permitidas por HTTPS e aplicará sua própria política de privacidade. A fonte de listas receberá solicitações HTTPS de atualização.
- Configurações, regras, metadados e contadores agregados ficarão no armazenamento local do aplicativo. O MVP não prevê histórico por domínio nem envio de logs a um servidor do projeto.
- Os dados locais permanecerão até a limpeza pelo usuário ou remoção do aplicativo. A limpeza de estatísticas será disponibilizada na interface. Os prazos e controles de retenção serão revisados conforme a implementação antes da distribuição.

Esta página deve ser atualizada com os provedores, permissões e controles efetivamente usados por cada versão distribuída.

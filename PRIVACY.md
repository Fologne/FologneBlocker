# Privacidade

Ao ativar a VPN, o FologneBlocker solicita autorização do Android quando necessário. O motor Kotlin lê as consultas encaminhadas ao DNS virtual e as envia por HTTPS ao provedor escolhido, Cloudflare por padrão ou Google Public DNS. O provedor recebe o conteúdo dessas consultas e o IP público da conexão. Os pacotes são processados em memória, sem gravação de histórico ou envio de telemetria própria. [Privacidade do Cloudflare 1.1.1.1](https://developers.cloudflare.com/1.1.1.1/privacy/public-dns-resolver/) e [privacidade do Google Public DNS](https://developers.google.com/speed/public-dns/privacy).

## Tratamento previsto para o MVP

- A VPN local exigirá autorização do Android após uma explicação na interface. O aplicativo usará a autorização para encaminhar ao filtro as consultas destinadas ao DNS virtual.
- O motor Kotlin processará consultas localmente. O provedor DoH selecionado receberá as consultas permitidas por HTTPS e aplicará sua própria política de privacidade. A fonte de listas receberá solicitações HTTPS de atualização.
- Configurações, regras, metadados e contadores agregados ficarão no armazenamento local do aplicativo. O MVP não prevê histórico por domínio nem envio de logs a um servidor do projeto.
- Os dados locais permanecerão até a limpeza pelo usuário ou remoção do aplicativo. A limpeza de estatísticas será disponibilizada na interface. Os prazos e controles de retenção serão revisados conforme a implementação antes da distribuição.

Esta página deve ser atualizada com os provedores, permissões e controles efetivamente usados por cada versão distribuída.

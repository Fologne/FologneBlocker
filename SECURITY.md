# Segurança

Reporte vulnerabilidades de forma privada pelo recurso de [reporte de vulnerabilidades do GitHub](https://github.com/Fologne/FologneBlocker/security/advisories/new). Se o recurso estiver indisponível, solicite um canal privado ao mantenedor em uma issue sem detalhes da falha. A disponibilidade do reporte privado precisa ser confirmada no repositório antes da primeira distribuição.

Inclua versão, versão do Android, impacto e passos de reprodução com dados sintéticos. Remova consultas DNS reais, IPs, credenciais e outros dados pessoais de logs e anexos.

O projeto prevê uma VPN local limitada a DNS, sem root ou inspeção HTTPS. DNS privado, resolvedores próprios e conexões diretas podem contornar a filtragem. A proteção depende das consultas que o Android encaminhar ao DNS virtual.

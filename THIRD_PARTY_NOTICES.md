# Fontes de terceiros

O cliente DoH usa [OkHttp 4.12.0](https://github.com/square/okhttp/tree/parent-4.12.0), licenciado sob Apache-2.0. JUnit 4.13.2 (EPL-1.0) é usado nos testes de JVM. O agendamento usa [AndroidX WorkManager 2.11.2](https://developer.android.com/jetpack/androidx/releases/work#2.11.2), sob Apache-2.0. A revisão de avisos do binário e dependências transitivas continua pendente antes da distribuição pública.

O aplicativo usa as listas **Multi Normal** (anúncios e rastreamento) e **NSFW** (categoria opcional) de HaGeZi. O autor publica as listas sob GPL-3.0 e exige que uma cópia da licença acompanhe qualquer redistribuição. Consulte o [repositório original de HaGeZi](https://github.com/hagezi/dns-blocklists) e a [FAQ do projeto](https://github.com/hagezi/dns-blocklists/blob/main/FAQ.md).

O aplicativo baixa as listas via jsDelivr, em seus arquivos originais com cabeçalhos e avisos do autor. O APK é distribuído sem cópias das listas. Antes de copiar, modificar ou redistribuir qualquer snapshot, revisar os termos da GPL-3.0 aplicáveis, preservar avisos e créditos e verificar obrigações relativas ao material redistribuído. A licença do código do FologneBlocker continua pendente.

# FologneBlocker

FologneBlocker é um aplicativo experimental para Android. O projeto prevê reduzir anúncios e rastreamento por filtragem local de DNS, com uma categoria opcional de domínios adultos, desativada por padrão.

A distribuição inicial está planejada pelo GitHub Releases. A interface permite testar o estabelecimento da VPN por até 30 segundos, apenas para o FologneBlocker. O processamento de pacotes, a resolução DoH e o bloqueio de anúncios estão previstos para etapas posteriores.

## Desenvolvimento Android

Use Flutter 3.44.0, JDK 17 e Android SDK Platform 36. Execute `flutter doctor -v` para verificar o ambiente e as ferramentas do SDK. O identificador Android é `com.fologne.fologneblocker`. O Android instala este APK separadamente de builds com o identificador anterior.

```sh
flutter pub get
dart format --output=none --set-exit-if-changed lib test
flutter analyze
flutter test
flutter build apk --debug
```

Com um aparelho ou emulador conectado, instale com `adb install -r build/app/outputs/flutter-apk/app-debug.apk`. Veja o [plano de teste manual](TESTE_ANDROID.md).

## Limites da proteção prevista

- A VPN local terá rotas restritas ao DNS virtual. A filtragem atua sobre domínios e não inspeciona imagens, vídeos ou conteúdo HTTPS.
- DNS privado, DoT ou DoH próprio de aplicativos e conexões diretas por IP podem contornar a filtragem.
- Anúncios servidos pelo mesmo domínio do conteúdo legítimo podem passar pelo filtro.
- O Android permite uma VPN ativa por perfil. Usar o FologneBlocker pode interromper outra VPN desse perfil.

O provedor DoH escolhido receberá as consultas permitidas quando o encaminhamento estiver disponível. Veja [Privacidade](PRIVACY.md), [Segurança](SECURITY.md) e [Fontes de terceiros](THIRD_PARTY_NOTICES.md).

A licença do código original está pendente de revisão das dependências e decisão do mantenedor.

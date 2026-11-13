# FologneBlocker

FologneBlocker é um aplicativo experimental para Android. O projeto prevê reduzir anúncios e rastreamento por filtragem local de DNS, com uma categoria opcional de domínios adultos, desativada por padrão.

A VPN local encaminha por HTTPS as consultas DNS UDP/53 que chegam ao DNS virtual. Cloudflare é o padrão e Google Public DNS pode ser escolhido ao ativar a VPN. O bloqueio por regras está previsto para a Etapa 6.

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

O workflow **Verificações Flutter** do GitHub Actions disponibiliza o artefato `fologneblocker-debug` após o build e os testes. Baixe o ZIP na execução do workflow e extraia `app-debug.apk`. Para testar via USB em um computador sem SDK de compilação, use o ADB do pacote Android Platform-Tools. [Como baixar artefatos do GitHub Actions](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/download-workflow-artifacts).

## Limites da proteção prevista

- A VPN local usa rotas restritas ao DNS virtual. O protótipo processa DNS sobre UDP em IPv4 e IPv6. Consultas que precisam repetir a operação por TCP/53 podem falhar.
- DNS privado, DoT ou DoH próprio de aplicativos e conexões diretas por IP podem contornar a filtragem.
- Anúncios servidos pelo mesmo domínio do conteúdo legítimo podem passar pelo filtro.
- O Android permite uma VPN ativa por perfil. Usar o FologneBlocker pode interromper outra VPN desse perfil.

O provedor DoH escolhido recebe as consultas encaminhadas. Falhas de transporte ou respostas inválidas geram erro DNS, sem fallback em texto puro. Veja [Privacidade](PRIVACY.md), [Segurança](SECURITY.md) e [Fontes de terceiros](THIRD_PARTY_NOTICES.md).

A licença do código original está pendente de revisão das dependências e decisão do mantenedor.

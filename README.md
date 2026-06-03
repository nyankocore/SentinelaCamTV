# Sentinela Cam TV

Sentinela Cam TV é um aplicativo comercial para visualizar câmeras de segurança em Android TV e Google TV.

O foco do app é transformar a TV em uma central simples de monitoramento local: mosaico de câmeras, tela cheia, cadastro por RTSP direto, descoberta ONVIF e navegação pensada para controle remoto.

![Mosaico do Sentinela Cam TV](docs/images/mosaico-readme.png)

## Estado comercial

A partir da versão `2.0.0`, o projeto passa a ser proprietário e exclusivo da Google Play Store.

A fase `1.x` foi a última fase pública/GPL já publicada. Os direitos concedidos nas versões GPL anteriores continuam válidos para essas versões, mas novas versões comerciais não serão distribuídas como software livre.

## Principais recursos

- Mosaico adaptativo para visualizar câmeras na TV.
- Tela cheia para acompanhar uma câmera individual.
- Cadastro por RTSP direto.
- Descoberta e cadastro por ONVIF.
- Suporte a DVRs/NVRs com múltiplos canais quando expostos por ONVIF.
- Alternância entre vídeo HD e SD. Aviso: várias câmeras em HD no mosaico podem travar o app em aparelhos modestos.
- Overlay de diagnóstico para investigar reprodução, codec, decoder, buffer e reconexões.
- Interface pensada para controle remoto e TV Boxes modestas.
- Teste grátis de 7 dias para novos assinantes.
- Plano mensal e plano anual pela Google Play.
- Modo grátis limitado a 1 câmera ativa.

## Compatibilidade atual

No momento, o Sentinela Cam TV é focado em câmeras fixas via RTSP ou ONVIF.

O ONVIF é usado para descoberta, autenticação e obtenção dos streams RTSP. O app ainda não controla PTZ, movimentação, zoom óptico, presets ou outros comandos de câmera.

O foco atual é visualização, não administração completa do dispositivo.

## Privacidade e diagnóstico

O app não envia imagens das câmeras, senhas, credenciais ou URLs RTSP completas para servidores externos.

Na versão Play, o diagnóstico automático pode usar Firebase Crashlytics e Android Vitals para registrar falhas técnicas. Esses registros devem conter apenas dados sanitizados, como versão do app, modelo do aparelho, Android, ABI, tipo de stream, codec, decoder e erro curto de reprodução.

O diagnóstico automático pode ser ativado ou desativado dentro do app.

## Distribuição

A distribuição comercial será feita pela Google Play Store em formato Android App Bundle (`.aab`).

O repositório de desenvolvimento deve permanecer privado. Não há publicação nova planejada por GitHub Releases ou F-Droid.

## Desenvolvimento

Este repositório é de desenvolvimento individual no momento. Pull requests e contribuições externas não estão abertas.

## Licença

Novas versões a partir da `2.0.0` são proprietárias. Consulte [LICENSE](LICENSE).

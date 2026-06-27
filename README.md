# Sentinela Cam TV

Sentinela Cam TV é um aplicativo open-source para visualizar câmeras de segurança em Android TV e Google TV.

Ele foi criado para monitoramento local, com foco em privacidade, simplicidade e bom funcionamento em TVs e TV Boxes com hardware modesto.

![Mosaico do Sentinela Cam TV](docs/images/mosaico-readme.png)

## Principais recursos

- Mosaico adaptativo para câmeras RTSP/ONVIF.
- Até 3 mosaicos organizáveis, com slots independentes.
- Tela cheia para cada câmera.
- Troca direcional de câmeras na tela cheia usando o mapa do mosaico.
- Cadastro por RTSP direto.
- Descoberta e cadastro por ONVIF.
- Suporte a DVR/NVR com múltiplos canais quando expostos por ONVIF.
- Alternância HD/SD e modos de menor latência/estabilidade.
- Captura de foto da câmera em tela cheia.
- Atualização manual pelo GitHub Releases.
- Logs locais para suporte.
- Interface pensada para controle remoto.

## Compatibilidade atual

- Suporta câmeras fixas via RTSP/ONVIF.
- ONVIF é usado para descoberta, autenticação e obtenção dos streams.
- No momento, o app não controla PTZ, zoom óptico, presets ou movimentação da câmera.
- O foco atual é visualização local, não administração completa do dispositivo.

## Privacidade

O Sentinela Cam TV não usa anúncios, rastreamento, analytics, telemetria, Firebase, Google Play Services ou nuvem obrigatória.

As câmeras são acessadas localmente. O aplicativo não envia imagens, credenciais ou dados das câmeras para servidores externos.

## Download

Baixe a versão mais recente na página de releases:

https://github.com/nyankocore/SentinelaCamTV/releases/latest

Use o APK da ABI do seu aparelho quando souber qual é. Se não souber, use o APK `universal`.

## Suporte

Relatos de bugs podem ser enviados pelas Issues:

https://github.com/nyankocore/SentinelaCamTV/issues

Os logs são exportados manualmente pelo usuário dentro do app. Nenhum log é enviado automaticamente.

## Estado do projeto

O app está em desenvolvimento ativo. Algumas funções ainda podem mudar, e o seu feedback é muito importante.

No momento, o projeto está em desenvolvimento individual. Relatos de bugs são bem-vindos pelas Issues, mas contribuições de código ainda não estão abertas.

## Licença

Sentinela Cam TV é software livre licenciado sob `GPL-3.0-or-later`.

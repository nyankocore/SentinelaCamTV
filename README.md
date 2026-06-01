# Sentinela Cam TV

Sentinela Cam TV é um aplicativo open-source para visualizar câmeras de segurança em Android TV e Google TV.

Ele foi criado para monitoramento local, com foco em privacidade, controle remoto, simplicidade e bom funcionamento em TVs e TV Boxes com hardware modesto.

![Mosaico do Sentinela Cam TV](docs/images/mosaico-readme.png)

## Principais recursos

- Mosaico adaptativo para visualizar várias câmeras na TV.
- Tela cheia para acompanhar uma câmera individual.
- Cadastro por RTSP direto.
- Descoberta e cadastro por ONVIF.
- Suporte a DVRs/NVRs com múltiplos canais quando expostos por ONVIF.
- Alternância entre vídeo HD e SD. AVISO: Várias câmeras em HD no mosaico pode travar o app em aparelhos modestos.
- Interface pensada para controle remoto.
- Logs locais para suporte, exportados manualmente pelo usuário.
- Atualização manual pelo GitHub Releases.

## Compatibilidade atual

No momento, o Sentinela Cam TV é focado em câmeras fixas via RTSP ou ONVIF.

O ONVIF é usado para descoberta, autenticação e obtenção dos streams RTSP. O app ainda não controla PTZ, movimentação, zoom óptico, presets ou outros comandos de câmera.

O foco atual é visualizar câmeras na TV, não administrar todas as funções do dispositivo.

## Privacidade

O Sentinela Cam TV não usa anúncios, rastreamento, analytics, telemetria, Firebase, Google Play Services ou nuvem obrigatória.

As câmeras são acessadas localmente. O aplicativo não envia imagens, credenciais, logs ou dados das câmeras para servidores externos.

## Download

Baixe a versão mais recente na página de releases:

https://github.com/nyankocore/SentinelaCamTV/releases/latest

Escolha o APK da arquitetura do seu aparelho. Se não souber qual usar, baixe o APK `universal`.

## Suporte

Encontrou algum problema? relate na página Issues:
https://github.com/nyankocore/SentinelaCamTV/issues
Para facilitar o diagnóstico envie o log do app, exporte o arquivo pelo app, envie para um serviço de nuvem e cole o link de compartilhamento do arquivo no relato do problema.

## Sugestões, feedback, perguntas

https://github.com/nyankocore/SentinelaCamTV/discussions

## Estado do projeto

O app está em desenvolvimento ativo. Algumas funções ainda podem mudar.

No momento, o projeto está em desenvolvimento individual. Relatos de bugs são bem-vindos, mas contribuições de código ainda não estão abertas.

## Licença

Sentinela Cam TV é software livre licenciado sob `GPL-3.0-or-later`.

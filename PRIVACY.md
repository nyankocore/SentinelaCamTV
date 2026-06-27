# Privacidade

Sentinela Cam TV foi pensado para uso local e privado.

## O que o app não faz

- Não exibe anúncios.
- Não usa analytics.
- Não possui rastreadores.
- Não cria conta de usuário.
- Não envia telemetria para servidores do projeto.
- Não depende de nuvem para visualizar câmeras locais.
- Não envia dados das câmeras para terceiros.

## Dados usados pelo app

O app armazena localmente dados necessários para conectar DVRs, NVRs e câmeras, como nome, endereço, URL RTSP e credenciais.

Credenciais salvas pelo app ficam no próprio aparelho. O projeto não recebe, coleta ou sincroniza esses dados.

## Rede

O app precisa de permissão de internet/rede para conectar ao DVR ou às câmeras RTSP na rede local. Essa permissão também é usada quando o usuário aciona manualmente a busca por atualizações no GitHub.

A descoberta ONVIF/WS-Discovery só deve ocorrer após ação do usuário.

## Atualizações

O botão `Buscar atualização` consulta o GitHub Releases apenas quando acionado pelo usuário. O app não faz checagem em segundo plano e não instala atualizações silenciosamente.

## Logs locais

O app pode gerar logs locais para suporte e relatórios locais de crashes. Esses arquivos ficam no aparelho até o usuário decidir exportá-los.

Logs não devem incluir senhas, tokens ou URLs RTSP com credenciais.

## Capturas

Fotos capturadas na tela cheia são salvas localmente no aparelho ou na pasta escolhida pelo usuário, quando o Android TV oferecer seletor de pasta.

O app não envia fotos, vídeos ou imagens das câmeras para servidores do projeto.

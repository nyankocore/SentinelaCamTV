# Privacidade

Sentinela Cam TV foi pensado para visualizar câmeras locais em Android TV e Google TV.

## Dados das câmeras

O app armazena localmente dados necessários para conectar DVRs, NVRs e câmeras, como nome, endereço, URL RTSP e credenciais.

Credenciais salvas pelo app ficam no próprio aparelho. O app não envia imagens, senhas, credenciais ou URLs RTSP completas para servidores externos.

## Rede

O app usa a rede para conectar aos dispositivos RTSP/ONVIF configurados pelo usuário.

A descoberta ONVIF/WS-Discovery ocorre apenas após ação explícita do usuário.

## Assinaturas

Na versão Play Store, assinaturas e teste grátis são processados pela Google Play Billing.

O app consulta a Google Play para verificar se existe assinatura ativa, teste grátis ativo ou acesso limitado gratuito. A cobrança, cancelamento, renovação e gerenciamento da assinatura são feitos pela conta Google do usuário.

## Diagnóstico automático

Na versão Play Store, o app pode usar Android Vitals e Firebase Crashlytics para ajudar a identificar falhas, travamentos e erros de reprodução.

O diagnóstico automático deve enviar somente dados técnicos sanitizados, como:

- versão do app;
- modelo do aparelho;
- versão do Android;
- ABI;
- quantidade de câmeras cadastradas;
- qualidade HD/SD;
- codec e decoder;
- erro curto do Media3/ExoPlayer;
- tipo de origem ONVIF/RTSP;
- eventos de watchdog e reconexão.

O app não deve enviar imagens das câmeras, áudio, senhas, credenciais, URLs RTSP completas, tokens ou dados pessoais intencionais.

O diagnóstico automático pode ser ativado ou desativado dentro do app.

## Anúncios

A versão `2.0.0` não inclui anúncios.

## Atualizações

A versão comercial é distribuída pela Google Play Store. O app não usa atualizador próprio pelo GitHub Releases.

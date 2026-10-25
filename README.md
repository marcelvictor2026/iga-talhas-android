# GIGA TALHAS — protótipo Android 0.3

Projeto Android Studio (Kotlin + WebView local), cliente fixo Energizer Brasil.

## Alterações 0.3
- O.S. recebe número e data/hora de abertura automáticos.
- Apenas uma O.S. ativa por instalação, mantida no armazenamento local.
- Fechamento permite assinatura do técnico da Energizer (nome + desenho) ou sem assinatura; justificativa opcional.
- Registra horário do encerramento, mantém histórico por talha e libera nova O.S. após finalizar.
- O PDF de impressão inclui dados do cliente, checklist, identificação da talha, horários e status da assinatura.

## Como abrir
Abra a pasta `GigaTalhas` no Android Studio, sincronize Gradle e use Build > Build APK(s).

## Limitações — não usar como sistema antifraude em produção
- O armazenamento local usa localStorage, sem banco transacional ou backup. A trava de O.S. funciona apenas neste aparelho e pode ser perdida ao limpar dados.
- O relógio local não fornece comprovação antifraude de horários.
- Fotos ainda não são persistidas na O.S. finalizada nem anexadas ao PDF; captura nativa de câmera e trilha de auditoria continuam pendentes.
- PDF é gerado pelo diálogo de impressão do Android; requer teste em dispositivo.
- Não houve compilação de APK nem teste em celular nesta entrega.
- O checklist técnico deve ser validado contra o manual e procedimentos de segurança aplicáveis.

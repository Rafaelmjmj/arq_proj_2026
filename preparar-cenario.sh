#!/usr/bin/env bash
# Preparação do cenário didático da Aula 10 — Concorrência no Trokr.
#
# Adaptado do script da aula para as rotas deste projeto:
#   - as rotas seguem o roteiro de testes do professor: ações por POST
#     (/enviar-homologacao, /aprovar, /enviar, /aceitar, /aceitar-contraproposta)
#     e finalização por PATCH;
#   - a contraproposta é criada em POST /propostas/{id}/contrapropostas;
#   - o aceite tem duas etapas: a contraproposta (/aceitar) e a raiz
#     (/aceitar-contraproposta).
# Para o roteiro oficial do professor, use o cenario-final.sh.
#
# Monta duas negociações em NEGOCIADO com o usuário A em comum:
#   Troca 1: raiz de A (PRODUTO, 10 créditos) x contraproposta de B (SERVICO, 15)
#   Troca 2: raiz de C (EXPERIENCIA, 20)      x contraproposta de A (PRODUTO, 10)
#
# Rode de novo antes de CADA corrida: depois de uma corrida as propostas já
# estão finalizadas. No Windows, rode no Git Bash.
set -euo pipefail

BASE_URL=${BASE_URL:-"http://localhost:8090"}
SUFIXO=$(date +%s)$RANDOM

# Extrai o "id" de uma resposta JSON. Usa o jq se existir; senão, o sed
# (o id é sempre o primeiro campo das respostas do Trokr).
extrair_id() {
    if command -v jq >/dev/null 2>&1; then
        jq -r '.id'
    else
        sed -E 's/^[^0-9]*"id":([0-9]+).*/\1/'
    fi
}

# Chama a API e para o script com uma mensagem clara se a resposta não for 2xx.
chamar() {
    local metodo=$1 caminho=$2 corpo=${3:-}
    local saida status
    if [ -n "$corpo" ]; then
        saida=$(curl -s -w $'\n%{http_code}' -X "$metodo" "$BASE_URL$caminho" \
            -H "Content-Type: application/json" -d "$corpo")
    else
        saida=$(curl -s -w $'\n%{http_code}' -X "$metodo" "$BASE_URL$caminho")
    fi
    status=${saida##*$'\n'}
    if [ "${status:0:1}" != "2" ]; then
        echo "Erro: $metodo $caminho respondeu $status" >&2
        echo "${saida%$'\n'*}" >&2
        exit 1
    fi
    echo "${saida%$'\n'*}"
}

echo "==> 1. Criando usuários A, B e C..."
ID_A=$(chamar POST /usuarios "{\"nome\": \"Usuario A\", \"email\": \"usuarioA_${SUFIXO}@trokr.com\", \"cidade\": \"Formosa\"}" | extrair_id)
ID_B=$(chamar POST /usuarios "{\"nome\": \"Usuario B\", \"email\": \"usuarioB_${SUFIXO}@trokr.com\", \"cidade\": \"Formosa\"}" | extrair_id)
ID_C=$(chamar POST /usuarios "{\"nome\": \"Usuario C\", \"email\": \"usuarioC_${SUFIXO}@trokr.com\", \"cidade\": \"Formosa\"}" | extrair_id)
echo "    Usuário A: id=$ID_A | Usuário B: id=$ID_B | Usuário C: id=$ID_C"

echo "==> 2. Criando itens com categorias diferentes..."
ITEM_A=$(chamar POST /itens "{\"titulo\": \"Item A (Produto)\", \"descricao\": \"Oferecido por A\", \"categoria\": \"PRODUTO\", \"pesoGramas\": 1200, \"usuarioId\": $ID_A}" | extrair_id)
ITEM_B=$(chamar POST /itens "{\"titulo\": \"Item B (Serviço)\", \"descricao\": \"Oferecido por B\", \"categoria\": \"SERVICO\", \"duracaoMinutos\": 60, \"usuarioId\": $ID_B}" | extrair_id)
ITEM_C=$(chamar POST /itens "{\"titulo\": \"Item C (Experiência)\", \"descricao\": \"Oferecido por C\", \"categoria\": \"EXPERIENCIA\", \"nivel\": \"INICIANTE\", \"usuarioId\": $ID_C}" | extrair_id)
echo "    Item A (PRODUTO - 10 créditos): id=$ITEM_A"
echo "    Item B (SERVICO - 15 créditos): id=$ITEM_B"
echo "    Item C (EXPERIENCIA - 20 créditos): id=$ITEM_C"

# Monta uma negociação: raiz do dono até Ativa, contraproposta do outro
# enviada e aceita pelo dono da raiz. Imprime "<raiz> <contra>".
negociar() {
    local dono=$1 item_dono=$2 autor=$3 item_autor=$4
    local raiz contra
    raiz=$(chamar POST /propostas "{\"titulo\": \"Troca de teste\", \"descricao\": \"Cenário da Aula 10\", \"usuarioId\": $dono, \"itemId\": $item_dono}" | extrair_id)
    chamar POST "/propostas/$raiz/enviar-homologacao" >/dev/null
    chamar POST "/propostas/$raiz/aprovar" >/dev/null
    contra=$(chamar POST "/propostas/$raiz/contrapropostas" "{\"descricao\": \"Contraproposta de teste\", \"usuarioId\": $autor, \"itemId\": $item_autor}" | extrair_id)
    chamar POST "/propostas/$contra/enviar" >/dev/null
    chamar POST "/propostas/$contra/aceitar?usuarioId=$dono" >/dev/null
    chamar POST "/propostas/$raiz/aceitar-contraproposta?usuarioId=$dono" >/dev/null
    echo "$raiz $contra"
}

echo "==> 3. Montando Negociação 1: Proposta Raiz de A aceita contraproposta de B..."
read -r RAIZ_1 CONTRA_1 <<< "$(negociar "$ID_A" "$ITEM_A" "$ID_B" "$ITEM_B")"
echo "    Negociação 1 pronta em NEGOCIADO: Raiz=$RAIZ_1 (Usuário A), Contraproposta=$CONTRA_1 (Usuário B)"

echo "==> 4. Montando Negociação 2: Proposta Raiz de C aceita contraproposta de A..."
read -r RAIZ_2 CONTRA_2 <<< "$(negociar "$ID_C" "$ITEM_C" "$ID_A" "$ITEM_A")"
echo "    Negociação 2 pronta em NEGOCIADO: Raiz=$RAIZ_2 (Usuário C), Contraproposta=$CONTRA_2 (Usuário A)"

echo ""
echo "================================================================="
echo "   CENÁRIO PRONTO PARA A DEMONSTRAÇÃO DE CONCORRÊNCIA"
echo "================================================================="
echo "Proposta Raiz 1 (Troca 1): ID = $RAIZ_1 (A oferece PRODUTO -> 10 créditos)"
echo "Proposta Raiz 2 (Troca 2): ID = $RAIZ_2 (A oferece PRODUTO -> 10 créditos)"
echo "Usuário A:                 ID = $ID_A (Saldo atual = 0 créditos)"
echo "Saldo esperado de A após a Corrida 2 (sem lost update): 20 créditos (10 + 10)"
echo ""
echo "Comandos para executar (com a aplicação no ar com TROKR_ATRASO_MS=2000):"
echo "  Corrida 1 (finalizar mesma proposta 2x) — esperado: 200 e 409:"
echo "    ./corrida.sh $RAIZ_1"
echo ""
echo "  Corrida 2 (finalizar 2 propostas concorrentes com Usuário A) — esperado: 200 e 200:"
echo "    ./corrida.sh $RAIZ_1 $RAIZ_2"
echo ""
echo "  Conferir o saldo de A:  curl $BASE_URL/usuarios/$ID_A"
echo ""
echo "Atenção: cada corrida finaliza as propostas. Rode ./preparar-cenario.sh"
echo "de novo antes da outra corrida."
echo "================================================================="

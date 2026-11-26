#!/usr/bin/env bash
# Cria 3 usuários, 3 itens e 7 negociações em NEGOCIADO_RAIZ,
# prontas para finalizar (Blocos 7 e 9 do roteiro de testes).
# Script do professor (Apêndice A do Roteiro de Testes da Entrega Final).
set -euo pipefail
shopt -s inherit_errexit
command -v jq >/dev/null || {
  echo "Instale o jq (brew install jq / apt install jq)"; exit 1; }

BASE=${BASE:-http://localhost:8090}
SUFIXO=$(date +%s)

post() { # post <caminho> <json>
  curl -sS --fail-with-body -X POST "$BASE$1" \
    -H 'Content-Type: application/json' -d "$2" \
    || { echo; echo "FALHOU: POST $1" >&2; exit 1; }
}
acao() { # acao <idProposta> <acao>
  curl -sS --fail-with-body -X POST "$BASE/propostas/$1/$2" -o /dev/null \
    || { echo "FALHOU: POST /propostas/$1/$2" >&2; exit 1; }
}
usuario() { # usuario <nome> <prefixoEmail>
  post /usuarios "$(jq -nc --arg n "$1" --arg e "$2_$SUFIXO@trokr.dev" \
    '{nome: $n, email: $e}')" | jq -r .id
}
item() { # item <usuarioId> <titulo> <categoria> <campo> <valorJson>
  post /itens "$(jq -nc --argjson u "$1" --arg t "$2" --arg c "$3" \
    --arg k "$4" --argjson v "$5" \
    '{titulo: $t, descricao: "Item de teste", usuarioId: $u,
      categoria: $c} + {($k): $v}')" | jq -r .id
}
negociacao() { # negociacao <autorRaiz> <itemRaiz> <autorContra> <itemContra>
  local raiz contra
  raiz=$(post /propostas "{\"usuarioId\":$1,\"itemId\":$2}" | jq -r .id)
  acao "$raiz" enviar-homologacao
  acao "$raiz" aprovar
  contra=$(post "/propostas/$raiz/contrapropostas" \
    "{\"usuarioId\":$3,\"itemId\":$4}" | jq -r .id)
  acao "$contra" enviar
  acao "$contra" aceitar
  acao "$raiz" aceitar-contraproposta
  echo "$raiz"
}

DIEGO=$(usuario "Diego Ramos" diego)
ELISA=$(usuario "Elisa Prado" elisa)
FABIO=$(usuario "Fabio Nunes" fabio)

ITEM_D=$(item "$DIEGO" "Fone de ouvido" PRODUTO pesoGramas 250)
ITEM_E=$(item "$ELISA" "Revisão de currículo" SERVICO duracaoMinutos 90)
ITEM_F=$(item "$FABIO" "Oficina de cerâmica" EXPERIENCIA nivel '"INICIANTE"')

N1=$(negociacao "$DIEGO" "$ITEM_D" "$ELISA" "$ITEM_E")   # Corrida 1
N2=$(negociacao "$DIEGO" "$ITEM_D" "$ELISA" "$ITEM_E")   # Corrida 2
N3=$(negociacao "$FABIO" "$ITEM_F" "$DIEGO" "$ITEM_D")   # Corrida 2
N4=$(negociacao "$ELISA" "$ITEM_E" "$FABIO" "$ITEM_F")   # Bloco 9
N5=$(negociacao "$ELISA" "$ITEM_E" "$FABIO" "$ITEM_F")   # Bloco 9
N6=$(negociacao "$ELISA" "$ITEM_E" "$FABIO" "$ITEM_F")   # Bloco 9
N7=$(negociacao "$ELISA" "$ITEM_E" "$FABIO" "$ITEM_F")   # Bloco 9

echo "Usuários: DIEGO=$DIEGO ELISA=$ELISA FABIO=$FABIO"
echo "Itens:    D=$ITEM_D E=$ITEM_E F=$ITEM_F"
echo "Propostas raiz prontas para finalizar:"
echo " N1=$N1 N2=$N2 N3=$N3 N4=$N4 N5=$N5 N6=$N6 N7=$N7"

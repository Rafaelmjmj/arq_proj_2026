#!/usr/bin/env bash
# Ferramenta de aula — dispara duas finalizações ao mesmo tempo.
# uso: ./corrida.sh <idA> [idB]
#   ./corrida.sh 7     -> finaliza a MESMA proposta duas vezes (Corrida 1)
#   ./corrida.sh 7 9   -> finaliza duas propostas diferentes ao mesmo tempo (Corrida 2)
set -u
if [ $# -lt 1 ]; then echo "uso: $0 <idA> [idB]"; exit 1; fi
A=$1
B=${2:-$1}
BASE=${BASE:-"http://localhost:8090/propostas"}

curl -s -o /dev/null -w "req 1 (proposta $A): %{http_code}\n" -X PATCH "$BASE/$A/finalizar" &
curl -s -o /dev/null -w "req 2 (proposta $B): %{http_code}\n" -X PATCH "$BASE/$B/finalizar" &
wait

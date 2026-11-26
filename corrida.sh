#!/usr/bin/env bash
# Dispara duas finalizações ao mesmo tempo.
#    ./corrida.sh 7    -> a MESMA proposta duas vezes (Corrida 1)
#    ./corrida.sh 7 9  -> duas propostas diferentes (Corrida 2)
# Script do professor (Apêndice A do Roteiro de Testes da Entrega Final).
set -u
[ $# -ge 1 ] || { echo "uso: $0 <idA> [idB]"; exit 1; }
A=$1; B=${2:-$1}
BASE=${BASE:-http://localhost:8090}
curl -s -o /dev/null -w "req 1 (proposta $A): %{http_code}\n" \
  -X PATCH "$BASE/propostas/$A/finalizar" &
curl -s -o /dev/null -w "req 2 (proposta $B): %{http_code}\n" \
  -X PATCH "$BASE/propostas/$B/finalizar" &
wait

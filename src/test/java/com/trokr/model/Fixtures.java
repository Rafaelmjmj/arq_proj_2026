package com.trokr.model;

/** Monta objetos de domínio à mão para testes unitários — sem Spring, sem banco. */
public final class Fixtures {

    private Fixtures() {
    }

    public static Usuario usuario(long id, String nome) {
        Usuario usuario = new Usuario(nome, nome.toLowerCase() + "@trokr.dev", "Formosa");
        usuario.setId(id);
        return usuario;
    }

    /**
     * Aula 12 — Item é abstrato: cada categoria tem a sua subclasse, montada
     * pelo builder. Num fixture de teste o switch é aceitável; no código de
     * produção quem escolhe a subclasse é o CatalogoDeFabricas.
     */
    public static Item item(long id, String titulo, CategoriaItem categoria, Usuario dono) {
        String descricao = "descrição de " + titulo;
        return switch (categoria) {
            case PRODUTO -> ItemProduto.builder().id(id).titulo(titulo).descricao(descricao)
                    .usuarioProprietario(dono).pesoGramas(500).build();
            case SERVICO -> ItemServico.builder().id(id).titulo(titulo).descricao(descricao)
                    .usuarioProprietario(dono).duracaoMinutos(60).build();
            case EXPERIENCIA -> ItemExperiencia.builder().id(id).titulo(titulo).descricao(descricao)
                    .usuarioProprietario(dono).nivel(NivelExperiencia.INICIANTE).build();
        };
    }

    /** Proposta raiz já levada até Ativa (Rascunho → Homologação → Ativa). */
    public static Proposta propostaAtiva(Usuario dono, Item item) {
        Proposta proposta = new Proposta("Troco " + item.getTitulo(), "Proposta de teste", dono, item);
        proposta.enviarParaHomologacao();
        proposta.aprovar();
        return proposta;
    }

    /** Contraproposta já enviada (Em Análise). */
    public static Proposta contrapropostaEmAnalise(Proposta raiz, Usuario autor, Item item) {
        Proposta contra = Proposta.criarContraProposta("Ofereço " + item.getTitulo(), autor, item, raiz);
        contra.enviarParaAnalise();
        return contra;
    }
}

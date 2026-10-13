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

    public static Item item(long id, String titulo, CategoriaItem categoria, Usuario dono) {
        Item item = new Item(titulo, "descrição de " + titulo, categoria, dono);
        item.setId(id);
        return item;
    }

    /** Proposta raiz já levada até Ativa (Rascunho → Homologação → Ativa). */
    public static Proposta propostaAtiva(Usuario dono, Item item) {
        Proposta proposta = new Proposta("Troco " + item.getTitulo(), "Proposta de teste", dono, item);
        proposta.submeter();
        proposta.aprovar();
        return proposta;
    }

    /** Contraproposta já enviada (Em Análise). */
    public static Proposta contrapropostaEmAnalise(Proposta raiz, Usuario autor, Item item) {
        Proposta contra = Proposta.criarContraProposta("Ofereço " + item.getTitulo(), autor, item, raiz);
        contra.submeter();
        return contra;
    }
}

package __PACKAGE__.dao;

import __PACKAGE__.beans.Entidade;

public final class Opcao implements Entidade {
    public final int id;
    public final String nome;

    public Opcao(int id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    @Override
    public Integer getId() { return id; }

    @Override
    public String toString() {
        return nome;
    }
}

package __PACKAGE__;

public final class Opcao {
    public final int id;
    public final String nome;

    public Opcao(int id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    @Override
    public String toString() {
        return nome;
    }
}

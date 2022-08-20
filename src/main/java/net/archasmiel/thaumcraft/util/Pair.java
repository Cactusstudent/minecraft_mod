package net.archasmiel.thaumcraft.util;

public class Pair<A, B> {

    private A left;
    private B right;



    public Pair(A left, B right) {
        this.left = left;
        this.right = right;
    }



    public A getLeft() {
        return left;
    }

    public void setLeft(A left) {
        this.left = left;
    }

    public B getRight() {
        return right;
    }

    public void setRight(B right) {
        this.right = right;
    }
}

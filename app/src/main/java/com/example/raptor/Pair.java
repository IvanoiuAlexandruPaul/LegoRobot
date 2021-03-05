package com.example.raptor;

public class Pair<A, B> {
    private A x;
    private B y;

    Pair(A x, B y) {
        this.x = x;
        this.y = y;
    }

    B getY() {
        return y;
    }

    A getX() {
        return x;
    }

    void setX(A x) {
        this.x = x;
    }

    void setY(B y) {
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Pair that = (Pair) o;
        if (x != that.x || y != that.y) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }

}
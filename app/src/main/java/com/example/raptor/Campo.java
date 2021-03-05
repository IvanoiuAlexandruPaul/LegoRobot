package com.example.raptor;

import java.util.LinkedList;


public class Campo {

    public static class Cell {
        int x;
        int y;
        int dist;    //distance
        Cell prev;  //parent cell in the path

        Cell(int x, int y, int dist, Cell prev) {
            this.x = x;
            this.y = y;
            this.dist = dist;
            this.prev = prev;
        }

        @Override
        public String toString() {
            return "(" + x + "," + y + ")";
        }
    }

    public enum StatoCasella {
        VISITATA,       // Stato cella visitata
        VUOTA,          // Stato cella vuota
        INIZIO,         // Stato cella di inzio
        PALLINA,        // Stato cella che conteneva pallina
        ULTIMA,         // Stato cella ultima pallina presa
        ERRATA
    }

    StatoCasella[][] campo;                   // Campo da gioco
    static int N, M;                          // Dimensioni matrice NxM
    Pair<Integer, Integer> initPosition;      // Posizione di inzio

    Campo(int N, int M, int initX, int initY) {
        this.N = N;
        this.M = M;
        this.initPosition = new Pair<>(initX, initY);
        campo = new StatoCasella[this.N][this.M];

        // Inizializzo il campo
        for (int n = 0; n < N; n++) {
            for (int m = 0; m < M; m++) {
                setVuota(m, n);
            }
        }
        campo[initPosition.getY()][initPosition.getX()] = StatoCasella.INIZIO;
    }

    public static LinkedList<Pair<Integer, Integer>> shortestPath(StatoCasella[][] campo, Pair<Integer, Integer> start, Pair<Integer, Integer> end) {
        if (campo[start.getY()][start.getX()] == StatoCasella.VUOTA || campo[end.getY()][end.getX()] == StatoCasella.VUOTA)
            return null;

        int m = M;
        int n = N;
        Cell[][] cells = new Cell[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (campo[i][j] != StatoCasella.VUOTA) {
                    cells[i][j] = new Cell(j, i, Integer.MAX_VALUE, null);
                }
            }
        }

        LinkedList<Cell> queue = new LinkedList<>();
        Cell src = cells[start.getY()][start.getX()];
        src.dist = 0;
        queue.add(src);
        Cell dest = null;
        Cell p;
        while ((p = queue.poll()) != null) {
            //find destination
            System.out.println("" + p.x + ", " + p.y);
            System.out.println("-" + end.getX() + ", " + end.getY());
            if (p.x == end.getX() && p.y == end.getY()) {
                dest = p;
                break;
            }

            // moving up
            visit(cells, queue, p.x - 1, p.y, p);

            // moving down
            visit(cells, queue, p.x + 1, p.y, p);

            // moving left
            visit(cells, queue, p.x, p.y - 1, p);

            //moving right
            visit(cells, queue, p.x, p.y + 1, p);
        }

        if (dest == null) {
            return null;
        } else {
            LinkedList<Pair<Integer, Integer>> path = new LinkedList<>();
            p = dest;
            do {
                path.addFirst(new Pair<>(p.x, p.y));
            } while ((p = p.prev) != null);
            return path;
        }
    }

    public static void visit(Cell[][] cells, LinkedList<Cell> queue, int x, int y, Cell parent) {
        if (x < 0 || x >= M || y < 0 || y >= N || cells[y][x] == null) {
            return;
        }

        int dist = parent.dist + 1;
        Cell p = cells[y][x];
        if (dist < p.dist) {
            p.dist = dist;
            p.prev = parent;
            queue.add(p);
        }
    }

    public Pair<Integer, Integer> convertiCoordinata(Pair<Integer, Integer> c) {
        int x = c.getX();
        int y = c.getY();

        y = (N - 1) - y;
        return new Pair<>(x, y);
    }


    public Pair<Integer, Integer> getInitPosition() {
        return initPosition;
    }

    public Pair<Integer, Integer> getDimensions() {
        return new Pair<>(M, N);
    }

    // Ritorna una copia del campo
    public StatoCasella[][] getCampo() {
        StatoCasella[][] newCampo = new Campo.StatoCasella[N][M];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                newCampo[i][j] = campo[i][j];
            }
        }
        return newCampo;
    }

    public Pair<Integer, Integer> getUltima() {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                if (campo[i][j] == StatoCasella.ULTIMA)
                    return new Pair<>(i, j);
            }
        }
        return null;
    }

    public StatoCasella getStato(int x, int y) {
        if (x >= 0 && x < getDimensions().getX() && y >= 0 && y < getDimensions().getY())
            return campo[y][x];
        else
            return StatoCasella.ERRATA;
    }

    void setVisita(int x, int y) {
        campo[y][x] = StatoCasella.VISITATA;
    }

    void setUltima(int x, int y) {
        campo[y][x] = StatoCasella.ULTIMA;
    }

    void setVuota(int x, int y) {
        campo[y][x] = StatoCasella.VUOTA;
    }

    void setInizio(int x, int y) {
        campo[y][x] = StatoCasella.INIZIO;
    }

    void setPallina(int x, int y) {
        campo[y][x] = StatoCasella.PALLINA;
    }


}
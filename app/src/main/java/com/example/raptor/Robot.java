package com.example.raptor;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import it.unive.dais.legodroid.lib.EV3;
import it.unive.dais.legodroid.lib.comm.BluetoothConnection;
import it.unive.dais.legodroid.lib.plugs.GyroSensor;
import it.unive.dais.legodroid.lib.plugs.TachoMotor;
import it.unive.dais.legodroid.lib.plugs.UltrasonicSensor;
import it.unive.dais.legodroid.lib.util.Prelude;

public class Robot {

    public static class BallDetector extends AsyncTask<Void, Void, Void> {

        private boolean detect = false;
        private boolean running = true;
        private Robot r;

        private BallDetector(Robot r) {
            this.r = r;
        }

        public boolean getDetect() {
            return detect;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            while (running) {
                try {

                    if (r.rilevaPalla()) {
                        detect = true;
                        running = false;
                    }
                    if (isCancelled())
                        running = false;
                } catch (IOException | ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    private static final String TAG = Prelude.ReTAG("Robot");

    enum Orientamento {NORD, EST, SUD, OVEST}

    private Orientamento orientamento;                      //Orientamento del Robot
    private TachoMotor motorCingoloSx;                      // Motore del cingolo sinistro
    private TachoMotor motorCingoloDx;                      // Motore del cingolo destro
    private TachoMotor motorGancio;                         // Motore del gancio
    private UltrasonicSensor ultraSensor;                   // Sensore ad ultrasuoni
    private GyroSensor gyroSensor;                          // Giroscopio
    private Pair<Integer, Integer> currentPos;              // Coordinate attuali del robot nel campo
    private Pair<Integer, Integer> initialPos;              // Coordinate iniziali del robot nel campo
    private Pair<Integer, Integer> dimensioni;              // Dimensioni della matrice
    private Campo campo;                                    // Campo di gioco
    private int valueAngle; // Variabile che mi dice l'angolazione giusta ogni volta
    private static final int DESTRA = 0;
    private static final int SINISTRA = 1;
    private static final int DRITTO = 2;
    private static final int DIR180 = 3;
    LinkedList<Pair<Integer, Integer>> pairPositions = new LinkedList<>();
    ArrayList<Pair<Integer, Integer>> l = new ArrayList<>();

    public Robot(TachoMotor motorCingoloSx, TachoMotor motorCingoloDx, TachoMotor motorGancio, UltrasonicSensor ultraSensor, GyroSensor gyroSensor, Campo campo) {
        this.motorCingoloSx = motorCingoloSx;
        this.motorCingoloDx = motorCingoloDx;
        this.motorGancio = motorGancio;
        this.ultraSensor = ultraSensor;
        this.gyroSensor = gyroSensor;
        this.campo = campo;
        initialPos = new Pair<>(campo.getInitPosition().getX(), campo.getInitPosition().getY());
        currentPos = new Pair<>(campo.getInitPosition().getX(), campo.getInitPosition().getY());
        dimensioni = campo.getDimensions();

        // Se il campo è di dimensioni 1X1(una casella)
        if (dimensioni.getY() == 1 && dimensioni.getX() == 1) {
            orientamento = Orientamento.NORD;
        }
        // Se il campo è di dimensione 1XM
        if (dimensioni.getX() == 1 && dimensioni.getY() > 1) {
            if (initialPos.getY() == 0) {
                orientamento = Orientamento.SUD;
            } else {
                orientamento = Orientamento.NORD;
            }
        }
        //Se il campo è di dimensione Nx1
        if (dimensioni.getY() == 1 && dimensioni.getX() > 1) {
            if (initialPos.getX() == dimensioni.getX() - 1) {
                orientamento = Orientamento.EST;
            } else {
                orientamento = Orientamento.OVEST;
            }
        }

        // Se la initialPos inziale è nell'intervallo [1, dim-1]
        if (initialPos.getY() < dimensioni.getY() - 1 && initialPos.getY() > 0) {
            // Lato sinistro
            if (initialPos.getX() == 0) {
                orientamento = Orientamento.EST;
            }
            // Lato destro
            if (initialPos.getX() == dimensioni.getX() - 1) {
                orientamento = Orientamento.OVEST;
            }
        }
        // Se la initialPos inziale è nell'intervallo [1, dim-1]
        if (initialPos.getX() < dimensioni.getX() - 1 && initialPos.getX() > 0) {
            // Lato superiore
            if (initialPos.getY() == 0) {
                orientamento = Orientamento.SUD;
            }
            // Lato inferiore
            if (initialPos.getY() == dimensioni.getY() - 1) {
                orientamento = Orientamento.NORD;
            }
        }
        // Angolo superiore sinistra
        if (initialPos.getY() == 0 && initialPos.getX() == 0) {
            orientamento = Orientamento.SUD;
        }
        // Angolo inferiore sinsitra
        if (initialPos.getY() == dimensioni.getY() - 1 && initialPos.getX() == 0) {
            orientamento = Orientamento.EST;
        }
        //Angolo superiore destra
        if (initialPos.getY() == 0 && initialPos.getX() == dimensioni.getX() - 1) {
            orientamento = Orientamento.OVEST;
        }
        // Angolo inferiore destra
        if (initialPos.getY() == dimensioni.getY() - 1 && initialPos.getX() == dimensioni.getX() - 1) {
            orientamento = Orientamento.NORD;
        }

    }

    public boolean avanzaCasella_checking(int numeroPalle, Orientamento startOrientamento) throws InterruptedException, ExecutionException, IOException, TimeoutException {
        BallDetector ballDetector = new BallDetector(this);
        ballDetector.execute();
        avanzaUnaCasella(getAngle());
        ballDetector.cancel(true);
        if (ballDetector.getDetect()) {
            numeroPalle--;
            Pair<Integer, Integer> attPos = new Pair<>(getCurrentPos().getX(), getCurrentPos().getY());
            l.add(attPos);
            gestisciMina(attPos, startOrientamento, getOrientamento(), numeroPalle);
            return true;
        }
        return false;
    }


    public boolean safeZone() throws InterruptedException, IOException {
        Thread.sleep(500);
        startMotors();
        setPowerMotors(50, 50, 1000);
        Thread.sleep(1500);
        stopMotors(1000);

        rilasciaPalla();

        Thread.sleep(500);
        startMotors();
        setPowerMotors(-50, -50, 1000);
        Thread.sleep(1500);
        stopMotors(1000);

        return true;

    }

    public void avanza(int powerDX, int powerSX, int step) throws IOException, InterruptedException {
        Thread.sleep(100);
        startMotors();
        setPowerMotors(powerSX, powerDX, step);
        Thread.sleep(step);
        stopMotors(1000);
        Thread.sleep(100);
    }

    public void avanzaUnaCasella(float angle) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        final String TAG = Prelude.ReTAG("avanzaUnaCasella");
        Log.d(TAG, "Avanzo di una casella. Posizione attuale: " + getCurrentPos().toString());
        Thread.sleep(500);
        startMotors();
        setPowerMotors(40, 40, 3400);
        Thread.sleep(3400);
        stopMotors(1000);
        if (orientamento == Orientamento.NORD) {
            if (currentPos.getY() - 1 >= 0) {
                if (campo.getStato(getCurrentPos().getX(), getCurrentPos().getY()) != Campo.StatoCasella.PALLINA)
                    campo.setVisita(currentPos.getX(), currentPos.getY() - 1);
                currentPos.setY(currentPos.getY() - 1);
                Log.d(TAG, "Posizione dopo avanzamento: " + getCurrentPos().toString());
            }
        } else if (orientamento == Orientamento.EST) {
            if (currentPos.getX() + 1 < campo.getDimensions().getX()) {
                if (campo.getStato(getCurrentPos().getX(), getCurrentPos().getY()) != Campo.StatoCasella.PALLINA)
                    campo.setVisita(currentPos.getX() + 1, currentPos.getY());
                currentPos.setX(currentPos.getX() + 1);
                Log.d(TAG, "Posizione dopo avanzamento: " + getCurrentPos().toString());
            }
        } else if (orientamento == Orientamento.SUD) {
            if (currentPos.getY() + 1 < campo.getDimensions().getY()) {
                if (campo.getStato(getCurrentPos().getX(), getCurrentPos().getY()) != Campo.StatoCasella.PALLINA)
                    campo.setVisita(currentPos.getX(), currentPos.getY() + 1);
                currentPos.setY(currentPos.getY() + 1);
                Log.d(TAG, "Posizione dopo avanzamento: " + getCurrentPos().toString());
            }
        } else if (orientamento == Orientamento.OVEST) {
            if (currentPos.getX() - 1 >= 0) {
                if (campo.getStato(getCurrentPos().getX(), getCurrentPos().getY()) != Campo.StatoCasella.PALLINA)
                    campo.setVisita(currentPos.getX() - 1, currentPos.getY());
                currentPos.setX(currentPos.getX() - 1);
                Log.d(TAG, "Posizione dopo avanzamento: " + getCurrentPos().toString());
            }
        }

        correggiDirezione(DRITTO, angle);
    }

    public void gestisciMina(Pair<Integer, Integer> lastPos, Orientamento startOrientamento, Orientamento lastOrientamento, int numeroPalle) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        final String TAG = Prelude.ReTAG("gestisciMina");
        stopMotors(1000);
        Log.d(TAG, "Aggancio palla");
        agganciaPalla();
        campo.setPallina(getCurrentPos().getX(), getCurrentPos().getY());
        goTo(getCurrentPos(), getInitialPos(), null);
        Orientamento supp;
        if (startOrientamento == Orientamento.NORD)
            supp = Orientamento.SUD;
        else if (startOrientamento == Orientamento.EST)
            supp = Orientamento.OVEST;
        else if (startOrientamento == Orientamento.SUD)
            supp = Orientamento.NORD;
        else
            supp = Orientamento.EST;
        while (getOrientamento() != supp)
            giraRobotDestra(getAngle());

        Log.d(TAG, "Avanzo verso la safe zone");
        safeZone();

        if (numeroPalle > 0) {
            goTo(getCurrentPos(), lastPos, null);
            Log.d(TAG, "Torno all'ultima posizione");
            while (getOrientamento() != lastOrientamento)
                giraRobotDestra(getAngle());
        }

    }

    synchronized public boolean rilevaPalla() throws IOException, ExecutionException, InterruptedException {
        if (ultraSensor.getDistance().get() <= 7)
            return true;
        else
            return false;

    }

    public void agganciaPalla() throws IOException, InterruptedException {
        startMotorGancio();
        setPowerGancio();
        Thread.sleep(1700);
        stopMotorsGancio();
    }

    public void rilasciaPalla() throws IOException {
        motorGancio.setStepPower(-100, 0, 1000, 0, true);
    }


    // Metodo per il movimento del robot da un punto ad un altro, utilizzando
    // il percorso minimo.
    // Si sposterà per le celle gia visitate.

    public void goTo(Pair<Integer, Integer> start, Pair<Integer, Integer> end, LinkedList<Pair<Integer, Integer>> listCoordinate) throws InterruptedException, ExecutionException, IOException, TimeoutException {
        final String TAG = Prelude.ReTAG("goTo");
        LinkedList<Pair<Integer, Integer>> minPath;
        if (listCoordinate == null)
            minPath = listCoordinate;
        else
            minPath = Campo.shortestPath(campo.getCampo(), start, end);

        //Log.d(TAG, "Size percorso minimo da " + start.toString() + " a " + end.toString() + ": " + minPath.size() + " caselle");
        int index = 0;
        while (index + 1 < minPath.size() && !getCurrentPos().equals(end)) {
            // Destra
            while (index + 1 < minPath.size() && minPath.get(index).getX() < minPath.get(index + 1).getX()) {
                if (getOrientamento() == Orientamento.OVEST)
                    giraRobot180((int) getAngle());
                if (getOrientamento() == Orientamento.SUD)
                    giraRobotSinistra((int) getAngle());
                if (getOrientamento() == Orientamento.NORD)
                    giraRobotDestra((int) getAngle());
                avanzaUnaCasella(getAngle());
                index++;
            }
            // Sinstra
            while (index + 1 < minPath.size() && minPath.get(index).getX() > minPath.get(index + 1).getX()) {
                if (getOrientamento() == Orientamento.EST)
                    giraRobot180((int) getAngle());
                if (getOrientamento() == Orientamento.NORD)
                    giraRobotSinistra((int) getAngle());
                if (getOrientamento() == Orientamento.SUD)
                    giraRobotDestra((int) getAngle());
                avanzaUnaCasella(getAngle());
                index++;
            }
            // Giù
            while (index + 1 < minPath.size() && minPath.get(index).getY() < minPath.get(index + 1).getY()) {
                if (getOrientamento() == Orientamento.NORD)
                    giraRobot180((int) getAngle());
                if (getOrientamento() == Orientamento.OVEST)
                    giraRobotSinistra((int) getAngle());
                if (getOrientamento() == Orientamento.EST)
                    giraRobotDestra((int) getAngle());
                avanzaUnaCasella(getAngle());
                index++;
            }

            // Su
            while (index + 1 < minPath.size() && minPath.get(index).getY() > minPath.get(index + 1).getY()) {
                if (getOrientamento() == Orientamento.SUD)
                    giraRobot180((int) getAngle());
                if (getOrientamento() == Orientamento.EST)
                    giraRobotSinistra((int) getAngle());
                if (getOrientamento() == Orientamento.OVEST)
                    giraRobotDestra((int) getAngle());
                avanzaUnaCasella(getAngle());
                index++;
            }

        }
    }

    public boolean avanzaCasella2(Pair<Integer, Integer> end) throws InterruptedException, ExecutionException, IOException, TimeoutException {
        BallDetector ballDetector = new BallDetector(this);
        ballDetector.execute();
        avanzaUnaCasella(getAngle());
        ballDetector.cancel(true);
        if (ballDetector.getDetect() && !getCurrentPos().equals(end)) {
            campo.setVuota(currentPos.getX(), currentPos.getY());
            if (getOrientamento() == Orientamento.NORD) {
                currentPos.setY(currentPos.getY() + 1);
            } else if (getOrientamento() == Orientamento.EST) {
                currentPos.setX(currentPos.getX() - 1);
            } else if (getOrientamento() == Orientamento.SUD) {
                currentPos.setY(currentPos.getY() - 1);
            } else if (getOrientamento() == Orientamento.OVEST) {
                currentPos.setX(getCurrentPos().getX() + 1);
            }
            startMotors();
            setPowerMotors(-25, -25, 1000);
            Thread.sleep(1500);
            stopMotors(100);
            return false;
        } else if (ballDetector.getDetect() && getCurrentPos().equals(end)) {
            startMotorGancio();
            setPowerGancio();
            Thread.sleep(1000);
            stopMotorsGancio();
            goTo(null, null, pairPositions);
            pairPositions = new LinkedList<>();
        } else {
            Pair<Integer, Integer> supaux = new Pair<>(getCurrentPos().getX(), getCurrentPos().getY());
            for (Pair<Integer, Integer> k : pairPositions) {
                if (!k.equals(supaux))
                    pairPositions.add(supaux);
            }
        }

        return false;
    }

    public boolean goTo_checking(Pair<Integer, Integer> start, Pair<Integer, Integer> end) throws InterruptedException, ExecutionException, IOException, TimeoutException {
        final String TAG = Prelude.ReTAG("goTo");
        LinkedList<Pair<Integer, Integer>> minPath = Campo.shortestPath(campo.getCampo(), start, end);

        Log.d(TAG, "Size percorso minimo da " + start.toString() + " a " + end.toString() + ": " + minPath.size() + " caselle");
        int index = 0;
        boolean stop = false;
        while (index + 1 < minPath.size() && !getCurrentPos().equals(end)) {
            // Destra
            while (index + 1 < minPath.size() && minPath.get(index).getX() < minPath.get(index + 1).getX()) {
                if (getOrientamento() == Orientamento.OVEST)
                    giraRobot180((int) getAngle());
                if (getOrientamento() == Orientamento.SUD)
                    giraRobotSinistra((int) getAngle());
                if (getOrientamento() == Orientamento.NORD)
                    giraRobotDestra((int) getAngle());
                if (avanzaCasella2(end)) {
                    stop = true;
                    break;
                }
                index++;
            }
            if (stop)
                break;
            // Sinstra
            while (index + 1 < minPath.size() && minPath.get(index).getX() > minPath.get(index + 1).getX()) {
                if (getOrientamento() == Orientamento.EST)
                    giraRobot180((int) getAngle());
                if (getOrientamento() == Orientamento.NORD)
                    giraRobotSinistra((int) getAngle());
                if (getOrientamento() == Orientamento.SUD)
                    giraRobotDestra((int) getAngle());
                if (avanzaCasella2(end)) {
                    stop = true;
                    break;
                }
                index++;
            }
            if (stop)
                break;
            // Giù
            while (index + 1 < minPath.size() && minPath.get(index).getY() < minPath.get(index + 1).getY()) {
                if (getOrientamento() == Orientamento.NORD)
                    giraRobot180((int) getAngle());
                if (getOrientamento() == Orientamento.OVEST)
                    giraRobotSinistra((int) getAngle());
                if (getOrientamento() == Orientamento.EST)
                    giraRobotDestra((int) getAngle());
                if (avanzaCasella2(end)) {
                    stop = true;
                    break;
                }
                index++;
            }
            if (stop)
                break;
            // Su
            while (index + 1 < minPath.size() && minPath.get(index).getY() > minPath.get(index + 1).getY()) {
                if (getOrientamento() == Orientamento.SUD)
                    giraRobot180((int) getAngle());
                if (getOrientamento() == Orientamento.EST)
                    giraRobotSinistra((int) getAngle());
                if (getOrientamento() == Orientamento.OVEST)
                    giraRobotDestra((int) getAngle());
                if (avanzaCasella2(end)) {
                    stop = true;
                    break;
                }
                index++;
            }
            if (stop)
                break;
        }
        if (stop) {
            return false;
        }
        return true;
    }

    private void startMotors() throws IOException {
        motorCingoloSx.start();
        motorCingoloDx.start();
    }

    private void startMotorGancio() throws IOException {
        motorGancio.start();
    }

    public void stopMotors(int step) throws IOException {
        motorCingoloSx.setStepPower(0, 0, step, 0, true);
        motorCingoloDx.setStepPower(0, 0, step, 0, true);
        motorCingoloSx.brake();
        motorCingoloDx.brake();
    }

    private void stopMotorsGancio() throws IOException {
        motorGancio.setStepPower(0, 0, 1000, 0, true);
        motorGancio.brake();
    }

    private void setPowerMotors(int powerSx, int powerDx, int step) throws IOException {
        motorCingoloSx.setStepPower(powerSx, 0, step, 0, true);
        motorCingoloDx.setStepPower(powerDx, 0, step, 0, true);
    }

    private void setPowerGancio() throws IOException {
        motorGancio.setStepPower(100, 0, 1000, 0, true);
    }


    public void correggiDirezione(int direzione, float angle) throws InterruptedException, IOException, ExecutionException, TimeoutException {
        final String TAG = Prelude.ReTAG("correggiDirezione");
        Thread.sleep(100);

        float gradiPrec = angle;
        float gradiAttuali = getAngle();

        Log.d(TAG, "Correzione direzione...");
        if (direzione == DESTRA) {
            Log.d(TAG, "Gradi inizio: " + gradiPrec + ", Gradi attuali: " + gradiAttuali + ", Gradi corretti: " + (gradiPrec + 90.0));
            while (gradiAttuali < gradiPrec + 89.0 || gradiAttuali > gradiPrec + 91.0) {
                // Se l'angolo attuale è minore rispetto all'angolo precedente sommato a 90°
                // correggo girando 100 mills verso destra
                if (gradiAttuali < gradiPrec + 90.0) {
                    startMotors();
                    setPowerMotors(50, -50, 100);
                    Thread.sleep(100);
                    stopMotors(100);
                    Log.d(TAG, "Corretto a destra");
                }
                // Se l'angolo attuale è maggiore rispetto all'angolo precedente sommato a 90°
                // correggo girando 100 mills verso sinistra
                if (gradiAttuali > gradiPrec + 90.0) {
                    startMotors();
                    setPowerMotors(-50, 50, 100);
                    Thread.sleep(100);
                    stopMotors(100);
                    Log.d(TAG, "Corretto a sinistra");
                }
                Thread.sleep(100);

                gradiAttuali = getAngle();
                Log.d(TAG, "Gradi dopo correzione: " + gradiAttuali);
            }
        }
        if (direzione == SINISTRA) {
            Log.d(TAG, "Gradi inizio: " + gradiPrec + ", Gradi attuali: " + gradiAttuali + ", Gradi corretti: " + (gradiPrec - 90.0));
            while (gradiAttuali > gradiPrec - 89.0 || gradiAttuali < gradiPrec - 91.0) {
                // Se l'angolo attuale è minore rispetto all'angolo precedente sommato a 90°
                // correggo girando 100 mills verso destra
                if (gradiAttuali < gradiPrec - 90.0) {
                    startMotors();
                    setPowerMotors(50, -50, 100);
                    Thread.sleep(100);
                    stopMotors(100);
                    Log.d(TAG, "Corretto a destra");
                }
                // Se l'angolo attuale è maggiore rispetto all'angolo precedente sommato a 90°
                // correggo girando 100 mills verso sinistra
                if (gradiAttuali > gradiPrec - 90.0) {
                    startMotors();
                    setPowerMotors(-15, 15, 100);
                    Thread.sleep(100);
                    stopMotors(100);
                    Log.d(TAG, "Corretto a sinistra");
                }
                Thread.sleep(100);

                gradiAttuali = getAngle();
                Log.d(TAG, "Gradi dopo correzione: " + gradiAttuali);
            }
        }
        if (direzione == DRITTO) {
            Log.d(TAG, "Gradi inizio: " + gradiPrec + ", gradi attuali: " + gradiAttuali + ", Gradi corretti: " + (gradiPrec));
            while (gradiAttuali > gradiPrec + 1 || gradiAttuali < gradiPrec - 1) {
                // Se l'angolo attuale è minore rispetto all'angolo precedente sommato a 90°
                // correggo girando 100 mills verso destra
                if (gradiAttuali < gradiPrec - 1) {
                    startMotors();
                    setPowerMotors(50, -50, 100);
                    Thread.sleep(100);
                    stopMotors(100);
                    Log.d(TAG, "Corretto a destra");
                }
                // Se l'angolo attuale è maggiore rispetto all'angolo precedente sommato a 90°
                // correggo girando 100 mills verso sinistra
                if (gradiAttuali > gradiPrec + 1) {
                    startMotors();
                    setPowerMotors(-50, 50, 100);
                    Thread.sleep(100);
                    stopMotors(100);
                    Log.d(TAG, "Corretto a sinistra");
                }
                Thread.sleep(100);

                gradiAttuali = getAngle();
                Log.d(TAG, "Gradi dopo correzione: " + gradiAttuali);
            }
        }
        if (direzione == DIR180) {
            Log.d(TAG, "Gradi inizio: " + gradiPrec + ", Gradi attuali: " + gradiAttuali + ", Gradi corretti: " + (gradiPrec + 180.0));

            while (gradiAttuali < gradiPrec + 179.0 || gradiAttuali > gradiPrec + 181.0) {
                // Se l'angolo attuale è minore rispetto all'angolo precedente sommato a 90°
                // correggo girando 100 mills verso destra
                if (gradiAttuali < gradiPrec + 180.0) {
                    startMotors();
                    setPowerMotors(50, -50, 100);
                    Thread.sleep(100);
                    stopMotors(100);
                    Log.d(TAG, "Corretto a destra");
                }
                // Se l'angolo attuale è maggiore rispetto all'angolo precedente sommato a 90°
                // correggo girando 100 mills verso sinistra
                if (gradiAttuali > gradiPrec + 180.0) {
                    startMotors();
                    setPowerMotors(-50, 50, 100);
                    Thread.sleep(100);
                    stopMotors(100);
                    Log.d(TAG, "Corretto a sinistra");
                }
                Thread.sleep(100);

                gradiAttuali = getAngle();
                Log.d(TAG, "Gradi dopo correzione: " + gradiAttuali);
            }
        }
    }

    public void giraRobotDestra(float angle) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        final String TAG = Prelude.ReTAG("giraRobotDestra");
        Log.d(TAG, "Giro a destra. Orientamento attuale: " + getOrientamento());
        // Se è orientato a NORD e gira a destra, il nuovo orientamento sarà EST
        if (orientamento == Orientamento.NORD) {
            startMotors();
            setPowerMotors(30, -30, 1600);
            orientamento = Orientamento.EST;
            motorCingoloDx.waitUntilReady();
            motorCingoloSx.waitUntilReady();
            Thread.sleep(1600);
            stopMotors(1000);

        }
        // Se è orientato a EST e gira a destra, il nuovo orientamento sarà SUD
        else if (orientamento == Orientamento.EST) {
            startMotors();
            setPowerMotors(30, -30, 1600);
            orientamento = Orientamento.SUD;
            motorCingoloDx.waitUntilReady();
            motorCingoloSx.waitUntilReady();
            Thread.sleep(1600);
            stopMotors(1000);
        }
        // Se è orientato a OVEST e gira a destra, il nuovo orientamento sarà NORD
        else if (orientamento == Orientamento.OVEST) {
            startMotors();
            setPowerMotors(30, -30, 1600);
            orientamento = Orientamento.NORD;
            motorCingoloDx.waitUntilReady();
            motorCingoloSx.waitUntilReady();
            Thread.sleep(1600);
            stopMotors(1000);
        }
        // Se è orientato a SUD e gira a destra, il nuovo orientamento sarà OVEST
        else if (orientamento == Orientamento.SUD) {
            startMotors();
            setPowerMotors(30, -30, 1600);
            orientamento = Orientamento.OVEST;
            motorCingoloDx.waitUntilReady();
            motorCingoloSx.waitUntilReady();
            Thread.sleep(1600);
            stopMotors(1000);
        }
        Log.d(TAG, "Girato a destra. Orientamento aggiornato: " + getOrientamento());
        correggiDirezione(DESTRA, angle);
    }

    public void giraRobotSinistra(float angle) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        final String TAG = Prelude.ReTAG("giraRobotSinistra");
        Log.d(TAG, "Giro a sinistra. Orientamento attuale: " + getOrientamento());
        // Se è orientato a NORD e gira a sinistra, il nuovo orientamento sarà OVEST
        if (orientamento == Orientamento.NORD) {
            startMotors();
            setPowerMotors(-50, 50, 1300);
            orientamento = Orientamento.OVEST;
            motorCingoloDx.waitUntilReady();
            motorCingoloSx.waitUntilReady();
            Thread.sleep(1300);
            stopMotors(1000);
        }
        // Se è orientato a OVEST e gira a sinistra, il nuovo orientamento sarà SUD
        else if (orientamento == Orientamento.OVEST) {
            startMotors();
            setPowerMotors(-50, 50, 1300);
            orientamento = Orientamento.SUD;
            motorCingoloDx.waitUntilReady();
            motorCingoloSx.waitUntilReady();
            Thread.sleep(1300);
            stopMotors(1000);
        }
        // Se è orientato a SUD e gira a sinistra, il nuovo orientamento sarà EST
        else if (orientamento == Orientamento.SUD) {
            startMotors();
            setPowerMotors(-50, 50, 1300);
            orientamento = Orientamento.EST;
            motorCingoloDx.waitUntilReady();
            motorCingoloSx.waitUntilReady();
            Thread.sleep(1300);
            stopMotors(1000);
        }
        // Se è orientato a EST e gira a sinistra, il nuovo orientamento sarà NORD
        else if (orientamento == Orientamento.EST) {
            startMotors();
            setPowerMotors(-50, 50, 1300);
            orientamento = Orientamento.NORD;
            motorCingoloDx.waitUntilReady();
            motorCingoloSx.waitUntilReady();
            Thread.sleep(1300);
            stopMotors(1000);
        }
        Log.d(TAG, "Girato a sinistra. Orientamento aggiornato: " + getOrientamento());
        correggiDirezione(SINISTRA, angle);
    }

    public void giraRobot180(float angle) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        final String TAG = Prelude.ReTAG("giraRobot180");
        Log.d(TAG, "Giro a 180. Orientamento attuale: " + getOrientamento());
        // Se è orientato a NORD e gira di 180 gradi, il nuovo orientamento sarà SUD
        if (orientamento == Orientamento.NORD) {
            startMotors();
            setPowerMotors(40, -40, 2950);
            orientamento = Orientamento.SUD;
            motorCingoloDx.waitUntilReady();
            motorCingoloSx.waitUntilReady();
            Thread.sleep(2950);
            stopMotors(1000);
        }
        // Se è orientato a OVEST e gira di 180 gradi, il nuovo orientamento sarà EST
        else if (orientamento == Orientamento.OVEST) {
            startMotors();
            setPowerMotors(40, -40, 2950);
            orientamento = Orientamento.EST;
            motorCingoloDx.waitUntilReady();
            motorCingoloSx.waitUntilReady();
            Thread.sleep(2950);
            stopMotors(1000);
        }
        // Se è orientato a SUD e gira di 180 gradi, il nuovo orientamento sarà NORD
        else if (orientamento == Orientamento.SUD) {
            startMotors();
            setPowerMotors(40, -40, 2950);
            orientamento = Orientamento.NORD;
            motorCingoloDx.waitUntilReady();
            motorCingoloSx.waitUntilReady();
            Thread.sleep(2950);
            stopMotors(1000);
        }
        // Se è orientato a EST e gira di 180 gradi, il nuovo orientamento sarà OVEST
        else if (orientamento == Orientamento.EST) {
            startMotors();
            setPowerMotors(40, -40, 2950);
            orientamento = Orientamento.OVEST;
            motorCingoloDx.waitUntilReady();
            motorCingoloSx.waitUntilReady();
            Thread.sleep(2950);
            stopMotors(1000);
        }
        Log.d(TAG, "Girato 180. Orientamento aggiornato: " + getOrientamento());
        correggiDirezione(DIR180, angle);
    }

    public Orientamento getOrientamento() {
        return orientamento;
    }

    public Pair<Integer, Integer> getInitialPos() {
        return initialPos;
    }

    public Pair<Integer, Integer> getCurrentPos() {
        return currentPos;
    }

    public float getTickMotorSx() throws IOException, ExecutionException, InterruptedException {
        return motorCingoloSx.getPosition().get();
    }

    public float getTickMotorDx() throws IOException, ExecutionException, InterruptedException {
        return motorCingoloDx.getPosition().get();
    }

    synchronized public float getAngle() throws IOException, ExecutionException, InterruptedException {
        return gyroSensor.getAngle().get();
    }


}

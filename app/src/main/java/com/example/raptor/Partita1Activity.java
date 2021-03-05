package com.example.raptor;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import it.unive.dais.legodroid.lib.EV3;
import it.unive.dais.legodroid.lib.GenEV3;
import it.unive.dais.legodroid.lib.comm.BluetoothConnection;
import it.unive.dais.legodroid.lib.plugs.GyroSensor;
import it.unive.dais.legodroid.lib.plugs.TachoMotor;
import it.unive.dais.legodroid.lib.plugs.UltrasonicSensor;
import it.unive.dais.legodroid.lib.util.Prelude;


public class Partita1Activity extends AppCompatActivity {


    private static final String TAG = Prelude.ReTAG("MainActivity");

    public static Campo campo;      // Istanza del campo di gioco
    private Robot robot;            // Istanza robot

    private TachoMotor cingoloSx;           // Motore del cingolo sinistro
    private TachoMotor cingoloDx;           // Motore del cingolo destro
    private TachoMotor motorGancio;         // Motore del gancio
    private UltrasonicSensor ultraSensor;   // Sensore ad ultrasuoni
    private GyroSensor gyroSensor;          // Giroscopio

    int initX;                   // Coordinata x inizale, dov'è posizionato il robot
    int initY;                   // Coordinata y inizale, dov'è posizionato il robot
    static int dimCampoX;        // Numero di colonne della matrice
    static int dimCampoY;        // Numero di righe della matrice
    int numeroPalle;             // Numero di palline presenti nel campo


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_partita1);

        Intent i = getIntent();
        initX = i.getIntExtra("valoreDellaCoordinataX_int", 0);
        initY = i.getIntExtra("valoreDellaCoordinataY_int", 0);
        dimCampoX = i.getIntExtra("valoreDimX_int", 0);
        dimCampoY = i.getIntExtra("valoreDimY_int", 0);
        numeroPalle = i.getIntExtra("valoreNumeroPalline_int", 0);

        campo = new Campo(dimCampoY, dimCampoX, initX, initY);

        try {

            BluetoothConnection.BluetoothChannel conn = new BluetoothConnection("EV3").connect(); // replace with your own brick name
            EV3 ev3 = new EV3(conn);
            ev3.run(this::legoMain);
        } catch (IOException | GenEV3.AlreadyRunningException e) {
            Log.e(TAG, "fatal error: cannot connect to EV3");
            e.printStackTrace();
        }

        PixelGridView pixelGrid = new PixelGridView(this, campo);
        pixelGrid.setNumColumns(dimCampoX);
        pixelGrid.setNumRows(dimCampoY);
        setContentView(pixelGrid);
    }

    // Programma principale che EV3 eseguirà
    private void legoMain(EV3.Api api) throws IOException {
        final String TAG = Prelude.ReTAG("legoMain");

        // Inzializzo i motori in base alla relative porte
        cingoloSx = api.getTachoMotor(EV3.OutputPort.A);
        cingoloDx = api.getTachoMotor(EV3.OutputPort.B);
        motorGancio = api.getTachoMotor(EV3.OutputPort.C);
        ultraSensor = api.getUltrasonicSensor(EV3.InputPort._2);
        gyroSensor = api.getGyroSensor(EV3.InputPort._4);

        // Istanza oggetto robot, che useremo per far eseguire della azioni al robot
        robot = new Robot(cingoloSx, cingoloDx, motorGancio, ultraSensor, gyroSensor, campo);

        try {
            // Indici che puntano ai limiti del campo, verranno modificati
            // per navigare in sottocampi
            int pLimitY = 0,
                    rLimitY = Campo.N - 1,
                    pLimitX = 0,
                    rLimitX = Campo.M - 1;

            // Flag che mi segnala se il robot ha visitato tutto il campo.
            // Utile quando viene dato in input un numero di palline, ma in campo non
            // ce ne sono
            boolean campoVisTutto = false;

            // Orientamento del robot nella casella inziale, serve per quando userò
            // il goTo e mi servirà capire come orientare il robot verso la9
            // safe zone.
            Robot.Orientamento startOrientamento = robot.getOrientamento();

            // Posizione iniziale che si aggiornerà per ogni sottocampo
            Pair<Integer, Integer> initPos = new Pair<>(initX, initY);

            // Ciclo che gestisce il campo incrementando o decrementando gli indici del campo e
            // verificando l'orientamento del robot nei vari casi speciali, uscirà dal ciclo quando
            // il campo è stato tutto visitato, gli indici limite si sovrappongono o le palle
            // sono state tutte prese.
            while (numeroPalle > 0 && pLimitY <= rLimitY && pLimitX <= rLimitX && !campoVisTutto) {

                // La prima mossa del robot è sempre girarsi a sinistra, per logica di spirale pensata
                robot.giraRobotSinistra(robot.getAngle());

                float angoloIniz = robot.getAngle();

                // Orientamento del robot alla posizione inziale di un sottocampo, utile per il goTo
                // in quanto mi serve per ricordare in che orinetamento ho lasciato la casella e al
                // ritorno dalla safezone posizionarmi giusto e continuare la spirale.
                Robot.Orientamento initOrientamento = robot.getOrientamento();

                // Ciclo per la gestione del movimento a spirale
                do {

                    // Se il robot si trova in un angolo (che non è la poszione inziale) dovrà
                    // girare a destra. Se i limiti x o y sono uaguali è un caso speciale e dovrà
                    // fare due giri a destra.
                    if (isAngle(robot.getCurrentPos(), pLimitY, rLimitY, pLimitX, rLimitX) && !robot.getCurrentPos().equals(initPos)) {
                        robot.avanza(10, 10, 1000);
                        robot.giraRobotDestra(robot.getAngle());
                        if (pLimitX == rLimitX || pLimitY == rLimitY)
                            robot.giraRobotDestra(robot.getAngle());
                    }

                    // Avanza di una casella controllando se incontra palle. Gestito da un thread,
                    // se viene rilevata una palla il robot la prende e la porta nella safezone
                    // e dopo ,se non era l'ultima palla, ritorna all'ultima cella lasciata.
                    // startOrientamento è l'orientamento della cella iniziale(quella dove c'è la safezone)
                    if (robot.avanzaCasella_checking(numeroPalle, startOrientamento))
                        numeroPalle--;

                    // Se il campo (o il sottocampo) in cui mi trovo è 1XM o MX1
                    // e la pos in cui mi trovo non è un angolo ed è la pos iniziale
                    // e l'orientamento non è corretto andrò avanti perchè non tutte
                    // le celle sono state visitate.
                    if ((pLimitX == rLimitX || pLimitY == rLimitY)
                            && !isAngle(robot.getCurrentPos(), pLimitY, rLimitY, pLimitX, rLimitX)
                            && robot.getCurrentPos().equals(initPos)
                            && !initOrientamento.equals(robot.getOrientamento())
                            && numeroPalle > 0) {
                        if (robot.avanzaCasella_checking(numeroPalle, startOrientamento))
                            numeroPalle--;
                    }

                } while (!robot.getCurrentPos().equals(initPos) && numeroPalle > 0);

                // Se i puntatori ai margini si scavalcano allora ho visitato tutto il campo
                if (pLimitX + 1 > rLimitX - 1 || pLimitY + 1 > rLimitY - 1) {
                    campoVisTutto = true;
                } else if (numeroPalle > 0) {

                    // Fuori dal ciclo sono posizionato nella casella di partenza
                    // e mi posiziono nella prossima
                    robot.giraRobotDestra(robot.getAngle());
                    robot.avanzaUnaCasella(robot.getAngle());

                    // Se la posizione iniziale è un angolo, dovrò fare dei movimenti
                    // aggiuntivi
                    if (isAngle(initPos, pLimitY, rLimitY, pLimitX, rLimitX)) {
                        robot.giraRobotDestra(robot.getAngle());
                        // L'avanzamento in questo caso non è "sicuro" in quanto mi sposto verso
                        // una cella non visitata.
                        if (robot.avanzaCasella_checking(numeroPalle, startOrientamento))
                            numeroPalle--;
                    }
                    if (numeroPalle <= 0)
                        break;

                    // Setto la nuova poszione di inizio del sottocampo
                    initPos = new Pair<>(robot.getCurrentPos().getX(), robot.getCurrentPos().getY());

                    // Aggiorno i limiti del campo
                    pLimitX++;
                    rLimitX--;
                    pLimitY++;
                    rLimitY--;

                    // Se i puntatori ai margini si scavalcano allora ho visitato tutto il campo
                    if (pLimitX == rLimitX && pLimitY == rLimitY) {
                        campoVisTutto = true;
                    } else {
                        // Sistemo l'orientamneto del robot nel caso abbia un orientameto errato
                        // per l'angolo in cui si trova
                        if (isAngle(initPos, pLimitY, rLimitY, pLimitX, rLimitX)) {

                            // Se il campo è 1XM
                            if (pLimitY == rLimitY) {
                                if (initPos.equals(new Pair<>(pLimitX, pLimitY))) {
                                    while (robot.getOrientamento() != Robot.Orientamento.SUD)
                                        robot.giraRobotDestra(robot.getAngle());
                                }
                                if (initPos.equals(new Pair<>(pLimitX, rLimitY))) {
                                    while (robot.getOrientamento() != Robot.Orientamento.NORD)
                                        robot.giraRobotDestra(robot.getAngle());
                                }
                            }
                            // Se il campo è NX1
                            else if (pLimitX == rLimitX) {
                                if (initPos.equals(new Pair<>(pLimitX, pLimitY))) {
                                    while (robot.getOrientamento() != Robot.Orientamento.OVEST)
                                        robot.giraRobotDestra(robot.getAngle());
                                }
                                if (initPos.equals(new Pair<>(pLimitX, rLimitY))) {
                                    while (robot.getOrientamento() != Robot.Orientamento.EST)
                                        robot.giraRobotDestra(robot.getAngle());
                                }
                            }
                            // Se il campo è NXM
                            else {
                                // Angolo in alto a sinistra
                                if ((initPos.equals(new Pair<>(pLimitX, pLimitY)) && robot.getOrientamento() != Robot.Orientamento.SUD)
                                        // Angolo in alto a destra
                                        || (initPos.equals(new Pair<>(rLimitX, pLimitY)) && robot.getOrientamento() != Robot.Orientamento.OVEST)
                                        // Angolo in basso a sinistra
                                        || (initPos.equals(new Pair<>(pLimitX, rLimitY)) && robot.getOrientamento() != Robot.Orientamento.EST)
                                        // Angolo in basso a destra
                                        || (initPos.equals(new Pair<>(rLimitX, rLimitY)) && robot.getOrientamento() != Robot.Orientamento.NORD))
                                    robot.giraRobotDestra(robot.getAngle());
                            }
                        }
                    }
                }
            }
            // Quando esce dal ciclo è perchè sono finite le palline o il campo è stato interamnete visitato
            // nel caso sia uscito perchè tutto il campo è stato visitato allora sarà necessario riportarlo
            // alla casella iniziale.
            if (campoVisTutto && !(robot.getCurrentPos().equals(robot.getInitialPos())))
                robot.goTo(robot.getCurrentPos(), robot.getInitialPos(), null);

        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            Log.e(TAG, "Errore durante l'esecuzione di legoMain");
            e.printStackTrace();
        }
    }

    public static boolean isAngle(Pair<Integer, Integer> pos, int pLimitY, int rLimitY, int pLimitX, int rLimitX) {
        if (((pos.getX() == pLimitX || pos.getX() == rLimitX) && pos.getY() == pLimitY)
                || ((pos.getX() == pLimitX || pos.getX() == rLimitX) && pos.getY() == rLimitY)) {
            return true;
        }
        return false;
    }

    public class PixelGridView extends View {
        private int numColumns, numRows;
        private int cellWidth, cellHeight;
        private Paint blackPaint = new Paint();
        private boolean[][] cellChecked;
        private Campo campo;
        private int dimCampoX, dimCampoY;

        public PixelGridView(Context context, Campo campo) {
            this(context, null, campo);
        }

        public PixelGridView(Context context, AttributeSet attrs, Campo campo) {
            super(context, attrs);
            this.campo = campo;
            setNumRows(this.campo.getDimensions().getY());
            dimCampoY = getNumRows();
            setNumColumns(this.campo.getDimensions().getX());
            dimCampoX = getNumColumns();
            blackPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        }

        public void setNumColumns(int numColumns) {
            this.numColumns = numColumns;
            calculateDimensions();
        }

        public int getNumColumns() {
            return numColumns;
        }

        public void setNumRows(int numRows) {
            this.numRows = numRows;
            calculateDimensions();
        }

        public int getNumRows() {
            return numRows;
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            calculateDimensions();
        }

        private void calculateDimensions() {
            if (numColumns < 1 || numRows < 1) {
                return;
            }

            cellWidth = getWidth() / numColumns;
            cellHeight = getHeight() / numRows;

            cellChecked = new boolean[numRows][numColumns];

            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(Color.WHITE);

            if (numColumns == 0 || numRows == 0) {
                return;
            }

            int width = getWidth();
            int height = getHeight();


            for (Pair<Integer, Integer> coordinata : robot.l) {
                canvas.drawRect(coordinata.getX() * cellWidth, coordinata.getY() * cellHeight,
                        (coordinata.getX() + 1) * cellWidth, (coordinata.getY() + 1) * cellHeight,
                        blackPaint);
            }

            for (int i = 1; i < numColumns; i++) {
                canvas.drawLine(i * cellWidth, 0, i * cellWidth, height, blackPaint);
            }

            for (int i = 1; i < numRows; i++) {
                canvas.drawLine(0, i * cellHeight, width, i * cellHeight, blackPaint);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            invalidate();
            return true;
        }
    }

}








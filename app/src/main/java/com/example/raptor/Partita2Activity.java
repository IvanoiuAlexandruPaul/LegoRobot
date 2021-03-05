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


public class Partita2Activity extends AppCompatActivity {


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
        setContentView(R.layout.activity_partita2);

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


            for (int x = 0; x < dimCampoX; x++) {
                for (int y = 0; y < dimCampoX; y++) {
                    campo.setVisita(x, y);
                }
            }
            ArrayList<Pair<Integer, Integer>> coordinate = new ArrayList<>();

            coordinate.add(new Pair<>(0, 0));

            Pair<Integer, Integer> initPos = new Pair<>(campo.getDimensions().getX(), campo.getDimensions().getY());

            for (Pair<Integer, Integer> coord : coordinate) {
                while (!robot.goTo_checking(robot.getCurrentPos(), coord)) ;


            }


            for (int x = 0; x < dimCampoX; x++) {
                for (int y = 0; y < dimCampoX; y++) {
                    campo.setVisita(x, y);
                }
            }
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            Log.e(TAG, "Errore durante l'esecuzione di legoMain");
            e.printStackTrace();
        }
    }

    public boolean verificaCoordinata(Pair<Integer, Integer> c) {
        if (c.getX() >= campo.getDimensions().getX() || c.getY() >= campo.getDimensions().getY()) {
            return false;
        }
        return true;
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








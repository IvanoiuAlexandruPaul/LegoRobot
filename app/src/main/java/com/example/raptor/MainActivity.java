package com.example.raptor;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    int valoreDellaCoordinataX_int;
    int valoreDellaCoordinataY_int;
    int valoreDimX_int;
    int valoreDimY_int;
    int valoreNumeroPalline_int;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button Partita1 = (Button) findViewById(R.id.Partita1);
        Button Partita2 = (Button) findViewById(R.id.Partita2);
        Button Partita3 = (Button) findViewById(R.id.Partita3);
        final EditText X = (EditText) findViewById(R.id.X);
        final EditText Y = (EditText) findViewById(R.id.Y);
        final EditText DimX = (EditText) findViewById(R.id.DimX);
        final EditText DimY = (EditText) findViewById(R.id.DimY);
        final EditText NrPalline = (EditText) findViewById(R.id.NrPalline);
        Partita1.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, Partita1Activity.class);

                String valoreDellaCoordinataX = X.getText().toString();       //this will get a string
                valoreDellaCoordinataX_int = Integer.parseInt(valoreDellaCoordinataX);

                String valoreDellaCoordinataY = Y.getText().toString();       //this will get a string
                valoreDellaCoordinataY_int = Integer.parseInt(valoreDellaCoordinataY);

                String valoreDimX = DimX.getText().toString();       //this will get a string
                valoreDimX_int = Integer.parseInt(valoreDimX);

                String valoreDimY = DimY.getText().toString();       //this will get a string
                valoreDimY_int = Integer.parseInt(valoreDimY);

                String valoreNumeroPalline = NrPalline.getText().toString();       //this will get a string
                valoreNumeroPalline_int = Integer.parseInt(valoreNumeroPalline);

                i.putExtra("valoreDellaCoordinataX_int", valoreDellaCoordinataX_int);
                i.putExtra("valoreDellaCoordinataY_int", valoreDellaCoordinataY_int);
                i.putExtra("valoreDimX_int", valoreDimX_int);
                i.putExtra("valoreDimY_int", valoreDimY_int);
                i.putExtra("valoreNumeroPalline_int", valoreNumeroPalline_int);

                startActivity(i);
            }
        });

        Partita2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, Partita1Activity.class);

                String valoreDellaCoordinataX = X.getText().toString();       //this will get a string
                valoreDellaCoordinataX_int = Integer.parseInt(valoreDellaCoordinataX);

                String valoreDellaCoordinataY = Y.getText().toString();       //this will get a string
                valoreDellaCoordinataY_int = Integer.parseInt(valoreDellaCoordinataY);

                String valoreDimX = DimX.getText().toString();       //this will get a string
                valoreDimX_int = Integer.parseInt(valoreDimX);

                String valoreDimY = DimY.getText().toString();       //this will get a string
                valoreDimY_int = Integer.parseInt(valoreDimY);

                String valoreNumeroPalline = NrPalline.getText().toString();       //this will get a string
                valoreNumeroPalline_int = Integer.parseInt(valoreNumeroPalline);

                i.putExtra("valoreDellaCoordinataX_int", valoreDellaCoordinataX_int);
                i.putExtra("valoreDellaCoordinataY_int", valoreDellaCoordinataY_int);
                i.putExtra("valoreDimX_int", valoreDimX_int);
                i.putExtra("valoreDimY_int", valoreDimY_int);
                i.putExtra("valoreNumeroPalline_int", valoreNumeroPalline_int);

                startActivity(i);
            }
        });

        Partita3.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, Partita3Activity.class);
                startActivity(i);
            }
        });


    }

}

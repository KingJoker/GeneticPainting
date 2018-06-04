package geneticPainting;


import javafx.scene.paint.Paint;
import javafx.scene.shape.Polygon;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static geneticPainting.Main.*;

public class PolygonDNA {
    BitSet dna;
    static int highestOneBitHeight;
    static int highestOneBitWidth;
    static int dnasize;
    Polygon polygon;
    Object dnaLock = new Object();

    static final double MUTATION_CHANCE = .01;
    static final double MUTATION_RATE = .1;

    static{
        highestOneBitHeight = BigInteger.valueOf(HEIGHT).bitLength();
        highestOneBitWidth = BigInteger.valueOf(WIDTH).bitLength();
        dnasize = highestOneBitWidth * 3 + highestOneBitHeight * 3 + 4 * 8;
    }

    public PolygonDNA(){
        polygon = new Polygon();
        dna = new BitSet(dnasize);
        synchronized (dnaLock){
            randomDNA();
            generatePolygon();
        }
    }                                                                                        



    public PolygonDNA(BitSet dna){
        synchronized (dnaLock) {
            this.dna = (BitSet) dna.clone();
            generatePolygon();
        }
    }
    public void randomDNA(){
        int index = 0;
        //points
        for(int i = 0; i < 3; i++){
            for(int j = 0; j < highestOneBitWidth; j++){
                dna.set(index++, ThreadLocalRandom.current().nextBoolean());
            }
        }

        for(int i = 0; i < 3; i++){
            for(int j = 0; j < highestOneBitHeight; j++){
                dna.set(index++, ThreadLocalRandom.current().nextBoolean());
            }
        }
        //color
        for(int j = 0; j < 4; j++){ // R, G, B, A
            for(int i = 0; i < 8; i++){ // 0-255
                dna.set(index++,ThreadLocalRandom.current().nextBoolean());
            }
        }
    }

    public Polygon getPolygon() {
        return polygon;
    }

    public Polygon generatePolygon(){
        Polygon poly = new Polygon();
        int split = (highestOneBitHeight + highestOneBitWidth)*3;

        Double[] points = new Double[6];
        for(int i = 0; i < 3; i++){
            long[] widthBits = dna.get(highestOneBitWidth * i, highestOneBitWidth * (i + 1)).toLongArray();
            try {
                points[i] = (widthBits.length > 0?widthBits[0]:0) * WIDTH / (double)(1 << highestOneBitWidth);
            }
            catch(ArrayIndexOutOfBoundsException e){
                points[i] = 0.0;
                System.out.println(dna);
                e.printStackTrace();
            }
        }
        int endX = highestOneBitWidth*3;
        for(int i = 0; i < 3; i++){
            long[] heightBits = dna.get(endX + highestOneBitHeight * i, endX + highestOneBitHeight * (i + 1)).toLongArray();
            try {
                points[i + 3] = (heightBits.length > 0?heightBits[0]:0) * HEIGHT / (double)(1<<highestOneBitHeight);
            }
            catch(ArrayIndexOutOfBoundsException e){
                points[i+3] = 0.0;
                System.out.println(dna);
                e.printStackTrace();
            }
        }

        poly.getPoints().setAll(points);

        byte[] colorBits = dna.get(split,split+32).toByteArray();
        int r = colorBits[0]+128;
        int g = colorBits[1]+128;
        int b = colorBits.length >= 3?colorBits[2]+128:0;
        double a = colorBits.length >= 4?(colorBits[3]+128) / 255.0 : 0.0;
        poly.setFill(Paint.valueOf(String.format("rgba(%d,%d,%d,%f)",r,g,b,a)));
        polygon = poly;
        return polygon;
    }
    public void flipBits(Set<Integer> flipList){

        synchronized (dnaLock) {
            for (int i : flipList) {
                dna.flip(i);
            }
            generatePolygon();
        }
    }
    public void mutate(){
        List<Integer> flipList = new ArrayList<>();
        for(int i = 0; i< MUTATION_AMOUNT;  i++){//dnasize * MUTATION_AMOUNT; i++){
            flipList.add(ThreadLocalRandom.current().nextInt(dna.size()));
        }
        for (int i = 0; i < flipList.size(); i++) {
            dna.flip(flipList.get(i));
        }
        generatePolygon();
    }

    public PolygonDNA clone(){
        return new PolygonDNA(dna);
    }
}

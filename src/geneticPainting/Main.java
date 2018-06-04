package geneticPainting;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Main extends Application {
    //public static ThreadLocalRandom random;

    public static final int WIDTH = 100;
    public static final int HEIGHT = 100;
    public static final int APP_WIDTH = 1000;
    public static final int APP_HEIGHT= 800;


    static final int POPULATION_SIZE = 50;
    static final double SELECTION_CUTOFF = .1;
    static final double MUTATION_CHANCE = 1;
    static final double SHUFFLE_CHANCE = .1;
    static final double MUTATION_RATE = .1;
    static final int NUM_GENES = 200;

    static final double MUTATION_AMOUNT = NUM_GENES * .175;

    public static final String IMAGE_URL = "file:C:\\Users\\David\\OneDrive\\Workspace\\GeneticPainting\\src\\geneticPainting\\squares100.png";
    static{
        //random = new ThreadLocalRandom();
    }

    /*@Override
    public void init() throws Exception {
        super.init();
        Painting paint2 = new Painting();
        System.out.println("fitness: " +paint2.fitness(new Image("file:geneticPainting/squares100.png")));
    }*/

    @Override
    public void start(Stage primaryStage) throws Exception{
        //Parent root = new Parent() {}FXMLLoader.load(getClass().getResource("sample.fxml"));
        //Painting paint = new Painting();
        //Painting paint2 = new Painting();
        //paint2.setVisible(false);
        //StackPane stack = new StackPane();
        //Button button = new Button();
        //button.setOnMouseClicked(x -> paint2.fitness(new Image("file:C:\\Users\\David\\OneDrive\\Workspace\\GeneticPainting\\src\\geneticPainting\\squares100.png")));

        //stack.setOnMouseClicked((x) -> paint.fitness(new Image("file:C:\\Users\\David\\OneDrive\\Workspace\\GeneticPainting\\src\\geneticPainting\\squares100.png")));
       // stack.getChildren().add(paint2);
        //stack.getChildren().add(paint);
        //stack.getChildren().add(button);

        //System.out.println("fitness: " +paint.fitness(new Image("file:geneticPainting/squares100.png")));
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "100");
        System.out.println(System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism"));
        primaryStage.setTitle("Genetic Painting");
        primaryStage.setScene(new Scene(new Driver(IMAGE_URL),APP_WIDTH,APP_HEIGHT));
        primaryStage.show();
        
    }


    public static void main(String[] args) {
        launch(args);
        System.out.println("hello");
    }
}

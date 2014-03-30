package test.herp.derp;
/*
 * Program:   tiny_gp.java
 *
 * Author:    Riccardo Poli (email: rpoli@essex.ac.uk)
 * Author:    Bradford W. Bazemore (email: bb03822@georgiasouthern.edu)
 *
 */

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Random;
import java.util.StringTokenizer;

class tiny_gp {
    public static final double
            PMUT_PER_NODE = 0.05,//the probability of mutation in any given node from the mutate function
            CROSSOVER_PROB = 0.9;//probability value of the crossover function
    static final int //this is the set of state value for the different operators
            ADD = 110,
            SUB = 111,
            MUL = 112,
            DIV = 113,
            FSET_START = ADD,
            FSET_END = DIV;
    static final int
            MAX_LEN = 10000,//this is the max length for ?
            POPSIZE = 100000,//size of stack ?
            DEPTH = 5,//the evolution depth of the tree to be searched
            GENERATIONS = 50,//the max number of generations to allow before stopping the evolution
            TSIZE = 2;
    static char[] buffer = new char[MAX_LEN];
    static Random rd = new Random();//random number generator for use though out the program
    static double[] x = new double[FSET_START];
    static double minrandom, maxrandom;//range for random variables
    static char[] program;//this is the character array for the input data
    static int PC;
    static int varnumber, fitnesscases, randomnumber;
    static double fbestpop = 0.0, favgpop = 0.0;
    static long seed;//seed for the random number generator
    static double avg_len;
    static double[][] targets;//this double matrix of targets
    double[] fitness;//stores the ratio that the program close to the "goal"
    char[][] pop;//stack

    public tiny_gp(String fname, long s) {//this is the constructor method for the tiny_gp class
        fitness = new double[POPSIZE];//sets up the fitness array to be size of nodes
        seed = s;
        if (seed >= 0)//if seed is given
            rd.setSeed(seed);
        setup_fitness(fname);
        for (int i = 0; i < FSET_START; i++)
            x[i] = (maxrandom - minrandom) * rd.nextDouble() + minrandom;
        pop = create_random_pop(POPSIZE, DEPTH, fitness);
    }

    public static void main(String[] args) {//driver of the program
        String fname = "problem.dat";
        long s = -1;

        if (args.length == 2) {
            s = Integer.valueOf(args[0]).intValue();//set a seed
            fname = args[1];
        }
        if (args.length == 1) {
            fname = args[0];
        }

        tiny_gp gp = new tiny_gp(fname, s);
        gp.evolve();
    }

    double run() { //interpreter of the program created by the system
        char primitive = program[PC++];
        if (primitive < FSET_START)
            return (x[primitive]);
        switch (primitive) {
            case ADD:
                return (run() + run());
            case SUB:
                return (run() - run());
            case MUL:
                return (run() * run());
            case DIV: {
                double num = run(), den = run();
                if (Math.abs(den) <= 0.001)
                    return (num);
                else
                    return (num / den);
            }
        }
        return (0.0); // should never get here
    }

    int traverse(char[] buffer, int buffercount) {//this method traverses the tree of nodes
        if (buffer[buffercount] < FSET_START)
            return (++buffercount);

        switch (buffer[buffercount]) {
            case ADD:
            case SUB:
            case MUL:
            case DIV:
                return (traverse(buffer, traverse(buffer, ++buffercount)));
        }
        return (0); // should never get here
    }

    void setup_fitness(String fname) {//this is the init function for the fitness system, called only once
        //this gets the data from the file
        try {
            int i, j;
            String line;

            BufferedReader in =
                    new BufferedReader(
                            new
                                    FileReader(fname)
                    );
            line = in.readLine();
            StringTokenizer tokens = new StringTokenizer(line);
            varnumber = Integer.parseInt(tokens.nextToken().trim());//this is the set up values
            if(varnumber<1){System.err.println("ERROR: must be one or more variables");System.exit(0);}
            randomnumber = Integer.parseInt(tokens.nextToken().trim());
            if (varnumber + randomnumber >= FSET_START){System.err.println("ERROR: too many variables and constants, should only be 5");}
            minrandom = Double.parseDouble(tokens.nextToken().trim());
            maxrandom = Double.parseDouble(tokens.nextToken().trim());
            fitnesscases = Integer.parseInt(tokens.nextToken().trim());
            targets = new double[fitnesscases][varnumber + 1];

            for (i = 0; i < fitnesscases; i++) {
                line = in.readLine();
                if(line == null){System.err.println("ERROR: To many cases"); System.exit(0);}
                tokens = new StringTokenizer(line);
                for (j = 0; j <= varnumber; j++) {
                    targets[i][j] = Double.parseDouble(tokens.nextToken().trim());
                }
            }
            in.close();
        } catch (FileNotFoundException e) {
            System.err.println("ERROR: Please provide a data file");
            System.exit(0);
        } catch (Exception e) {
            System.err.println("ERROR: Incorrect data format");
            System.out.print("Please place file as .data, also make sure it is in the format of");
            System.out.print("The first line should be as follows...");
            System.out.print("The second line should be as follows...");
            System.out.print("X1 X2 X3 ... Xn(inputs) Y(output)");
            System.exit(0);
        }
    }

    double fitness_function(char[] Prog) {//find the fitness of some generated program
        int i;
        //int len;
        double result, fit = 0.0;

        //len = traverse(Prog, 0);
        for (i = 0; i < fitnesscases; i++) {
            System.arraycopy(targets[i], 0, x, 0, varnumber);
            program = Prog;
            PC = 0;//set program counter to 0
            result = run();
            fit += Math.abs(result - targets[i][varnumber]);
        }
        return (-fit);
    }

    int grow(char[] buffer, int pos, int max, int depth) {//allow the evolving tree to grow
        char prim = (char) rd.nextInt(2);
        int one_child;

        if (pos >= max)
            return (-1);

        if (pos == 0)
            prim = 1;

        if (prim == 0 || depth == 0) {
            prim = (char) rd.nextInt(varnumber + randomnumber);
            buffer[pos] = prim;
            return (pos + 1);
        } else {
            prim = (char) (rd.nextInt(FSET_END - FSET_START + 1) + FSET_START);
            switch (prim) {
                case ADD:
                case SUB:
                case MUL:
                case DIV:
                    buffer[pos] = prim;
                    one_child = grow(buffer, pos + 1, max, depth - 1);
                    if (one_child < 0)
                        return (-1);
                    return (grow(buffer, one_child, max, depth - 1));
            }
        }
        return (0); // should never get here
    }

    int print_indiv(char[] buffer, int buffercounter) {//prints the trees
        int a1 = 0, a2;
        if (buffer[buffercounter] < FSET_START) {
            if (buffer[buffercounter] < varnumber)
                System.out.print("X" + (buffer[buffercounter] + 1) + " ");
            else
                System.out.print(x[buffer[buffercounter]]);
            return (++buffercounter);
        }
        switch (buffer[buffercounter]) {//breaks down the parts of the trees
            case ADD:
                System.out.print("(");
                a1 = print_indiv(buffer, ++buffercounter);
                System.out.print(" + ");
                break;
            case SUB:
                System.out.print("(");
                a1 = print_indiv(buffer, ++buffercounter);
                System.out.print(" - ");
                break;
            case MUL:
                System.out.print("(");
                a1 = print_indiv(buffer, ++buffercounter);
                System.out.print(" * ");
                break;
            case DIV:
                System.out.print("(");
                a1 = print_indiv(buffer, ++buffercounter);
                System.out.print(" / ");
                break;
        }
        a2 = print_indiv(buffer, a1);
        System.out.print(")");
        return (a2);
    }

    char[] create_random_indiv(int depth) {//this creates a random cell in the tree at some depth
        char[] ind;
        int len;

        len = grow(buffer, 0, MAX_LEN, depth);

        while (len < 0)
            len = grow(buffer, 0, MAX_LEN, depth);

        ind = new char[len];

        System.arraycopy(buffer, 0, ind, 0, len);
        return (ind);
    }

    char[][] create_random_pop(int n, int depth, double[] fitness) {//creates a random... well idk what to say yet
        char[][] pop = new char[n][];
        int i;

        for (i = 0; i < n; i++) {
            pop[i] = create_random_indiv(depth);
            fitness[i] = fitness_function(pop[i]);
        }
        return (pop);
    }

    void stats(double[] fitness, char[][] pop, int gen) {//creates information for the use to keep track of the progress
        int i, best = rd.nextInt(POPSIZE);
        int node_count = 0;
        fbestpop = fitness[best];
        favgpop = 0.0;

        for (i = 0; i < POPSIZE; i++) {
            node_count += traverse(pop[i], 0);
            favgpop += fitness[i];
            if (fitness[i] > fbestpop) {
                best = i;
                fbestpop = fitness[i];
            }
        }
        avg_len = (double) node_count / POPSIZE;
        favgpop /= POPSIZE;
        System.out.print("Generation=" + gen + " Avg Fitness=" + (-favgpop) +
                " Best Fitness=" + (-fbestpop) + " Avg Size=" + avg_len +
                "\nBest Individual: ");
        print_indiv(pop[best], 0);
        System.out.print("\n");
        System.out.flush();
    }

    int tournament(double[] fitness, int tsize) {//this is a system based on the tournament model this is the function
        //that conducts the testing based on the fitness of the new programs
        int best = rd.nextInt(POPSIZE), i, competitor;
        double fbest = -1.0e34;//init value that is very small

        for (i = 0; i < tsize; i++) {//linear search
            competitor = rd.nextInt(POPSIZE);//select a random competitor
            if (fitness[competitor] > fbest) {
                fbest = fitness[competitor];
                best = competitor;
            }
        }
        return (best);
    }

    int negative_tournament(double[] fitness, int tsize) {//same as tournament but trying to find worst
        int worst = rd.nextInt(POPSIZE), i, competitor;
        double fworst = 1e34;

        for (i = 0; i < tsize; i++) {
            competitor = rd.nextInt(POPSIZE);
            if (fitness[competitor] < fworst) {
                fworst = fitness[competitor];
                worst = competitor;
            }
        }
        return (worst);
    }

    char[] crossover(char[] parent1, char[] parent2) {//this is the point of "matting"
        int xo1start, xo1end, xo2start, xo2end;
        char[] offspring;
        int len1 = traverse(parent1, 0);
        int len2 = traverse(parent2, 0);
        int lenoff;

        xo1start = rd.nextInt(len1);
        xo1end = traverse(parent1, xo1start);

        xo2start = rd.nextInt(len2);
        xo2end = traverse(parent2, xo2start);

        lenoff = xo1start + (xo2end - xo2start) + (len1 - xo1end);

        offspring = new char[lenoff];

        System.arraycopy(parent1, 0, offspring, 0, xo1start);
        System.arraycopy(parent2, xo2start, offspring, xo1start,
                (xo2end - xo2start));
        System.arraycopy(parent1, xo1end, offspring,
                xo1start + (xo2end - xo2start),
                (len1 - xo1end));

        return (offspring);
    }

    char[] mutation(char[] parent, double pmut) {//this is the point of changing the genetic code
        int len = traverse(parent, 0), i;
        int mutsite;
        char[] parentcopy = new char[len];

        System.arraycopy(parent, 0, parentcopy, 0, len);
        for (i = 0; i < len; i++) {
            if (rd.nextDouble() < pmut) {
                mutsite = i;
                if (parentcopy[mutsite] < FSET_START)
                    parentcopy[mutsite] = (char) rd.nextInt(varnumber + randomnumber);
                else
                    switch (parentcopy[mutsite]) {
                        case ADD:
                        case SUB:
                        case MUL:
                        case DIV:
                            parentcopy[mutsite] =
                                    (char) (rd.nextInt(FSET_END - FSET_START + 1)
                                            + FSET_START);
                    }
            }
        }
        return (parentcopy);
    }

    void print_parms() {//initial data the prints when the program starts
        System.out.print("-- TINY GP (Java version) --\n");
        System.out.print("SEED=" + seed + "\nMAX_LEN=" + MAX_LEN +
                "\nPOPSIZE=" + POPSIZE + "\nDEPTH=" + DEPTH +
                "\nCROSSOVER_PROB=" + CROSSOVER_PROB +
                "\nPMUT_PER_NODE=" + PMUT_PER_NODE +
                "\nMIN_RANDOM=" + minrandom +
                "\nMAX_RANDOM=" + maxrandom +
                "\nGENERATIONS=" + GENERATIONS +
                "\nTSIZE=" + TSIZE +
                "\n----------------------------------\n");
    }

    void evolve() {//this is the main point of the program
        //it will return when the program can not be solved or is solved
        int gen, indivs, offspring, parent1, parent2, parent;
        double newfit;
        char[] newind;
        print_parms();
        stats(fitness, pop, 0);
        for (gen = 1; gen < GENERATIONS; gen++) {//runs until all generations
            if (fbestpop > -1e-5) {//5 decimal places
                System.out.print("PROBLEM SOLVED\n");
                System.exit(0);//exit, problem solved to some precision
            }
            for (indivs = 0; indivs < POPSIZE; indivs++) {//evolution
                if (rd.nextDouble() < CROSSOVER_PROB) {//probability of conducting a matting
                    parent1 = tournament(fitness, TSIZE);//find a parent
                    parent2 = tournament(fitness, TSIZE);
                    newind = crossover(pop[parent1], pop[parent2]);//cross the parents
                } else {
                    parent = tournament(fitness, TSIZE);
                    newind = mutation(pop[parent], PMUT_PER_NODE);
                }
                newfit = fitness_function(newind);
                offspring = negative_tournament(fitness, TSIZE);
                pop[offspring] = newind;//add new node to population
                fitness[offspring] = newfit;//add the fitness of said not to population
            }
            stats(fitness, pop, gen);
        }
        System.out.print("PROBLEM *NOT* SOLVED\n");
        System.exit(1);
    }
}

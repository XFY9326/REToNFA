import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws NormalFormException {
        long lastMillis;
        Scanner scanner = new Scanner(System.in);
        System.out.print("Regular Expression: ");
        String input = scanner.nextLine();

        System.out.println("Your Input: " + input);

        System.out.println("\nStarting Analyse Regular Expression ...");
        lastMillis = System.currentTimeMillis();
        GrammarNode grammarNode = new GrammarNode(input, true);
        System.out.println("Analyse Finished! (In " + (System.currentTimeMillis() - lastMillis) + " Millis)");

        System.out.println("\nStarting Building NFA Map ...");
        lastMillis = System.currentTimeMillis();
        NFAMap.Builder builder = new NFAMap.Builder();
        NFAMap nfaMap = builder.setNode(grammarNode).build();
        System.out.println("Building Finished! (In " + (System.currentTimeMillis() - lastMillis) + " Millis)");

        System.out.println("\nGenerating NFA Map ...");
        lastMillis = System.currentTimeMillis();
        nfaMap.outputAsHtml();
        System.out.println("NFA Map Was Generated Successfully! (In " + (System.currentTimeMillis() - lastMillis) + " Millis)");
    }
}

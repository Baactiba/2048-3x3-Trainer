import java.util.*;
import java.io.*;
public class MoveGetTest
{
    public static void main (String[] args) throws IOException
    {
        go(3, new int[]{2, 1, 3, 1, 3});
    }
    public static void go(int center, int[] pdf)
    {
        Input.startOutput();
        System.out.println("256 128 32: " + Input.getMoveFromTables(new int[] {8, 7, pdf[4], 5, center, pdf[3], pdf[0], pdf[1], pdf[2]}));
        System.out.println("512 128 32: " + Input.getMoveFromTables(new int[] {9, 7, pdf[4], 5, center, pdf[3], pdf[0], pdf[1], pdf[2]}));
        System.out.println("512 256 32: " + Input.getMoveFromTables(new int[] {9, 8, pdf[4], 5, center, pdf[3], pdf[0], pdf[1], pdf[2]}));
        System.out.println("1024 128 32: " + Input.getMoveFromTables(new int[] {8, 7, pdf[4], 5, center, pdf[3], pdf[0], pdf[1], pdf[2]}));
        System.out.println("1024 256 32: " + Input.getMoveFromTables(new int[] {8, 7, pdf[4], 5, center, pdf[3], pdf[0], pdf[1], pdf[2]}));
        System.out.println("\n256 128 64: " + Input.getMoveFromTables(new int[] {8, 7, pdf[4], 6, center, pdf[3], pdf[0], pdf[1], pdf[2]}));
        System.out.println("512 128 64: " + Input.getMoveFromTables(new int[] {9, 7, pdf[4], 6, center, pdf[3], pdf[0], pdf[1], pdf[2]}));
        System.out.println("512 256 64: " + Input.getMoveFromTables(new int[] {9, 8, pdf[4], 6, center, pdf[3], pdf[0], pdf[1], pdf[2]}));
        System.out.println("1024 128 64: " + Input.getMoveFromTables(new int[] {10, 7, pdf[4], 6, center, pdf[3], pdf[0], pdf[1], pdf[2]}));
        System.out.println("1024 256 64: " + Input.getMoveFromTables(new int[] {10, 8, pdf[4], 6, center, pdf[3], pdf[0], pdf[1], pdf[2]}));
        System.out.println("\n512 256 128: " + Input.getMoveFromTables(new int[] {9, 8, pdf[4], 7, center, pdf[3], pdf[0], pdf[1], pdf[2]}));
        System.out.println("1024 256 128: " + Input.getMoveFromTables(new int[] {10, 8, pdf[4], 7, center, pdf[3], pdf[0], pdf[1], pdf[2]}));
    }
}
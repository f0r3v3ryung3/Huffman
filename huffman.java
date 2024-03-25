package Huffman;

import java.io.*;
import java.util.TreeMap;
import java.util.TreeSet;

public class huffman {
    public static void main(String[] args) throws Exception {
//        Huffman huff = new Huffman("input.dat");
//        huff.archive(new FileOutputStream("output.ha"));
        deHuffman deHuff = new deHuffman("input.ha", "output.dat");
    }
}
 
class deHuffman {
    BIS in;
    BOS out;
    Tree tree;
    int lastBites;
    deHuffman (String inName, String outName) throws IOException {
        long length = new File(inName).length();
        in = new BIS(new FileInputStream(inName));
        for (int i = 0; i < 16; i ++) in.readBit();
        lastBites = (8 - ((in.readBit() << 2) + (in.readBit() << 1) + in.readBit())) % 8;
        tree = new Tree(in);
        out = new BOS(new FileOutputStream(outName));
        Node node = tree.start;
        if (tree.takeCodes().size() == 1) {
            for (long i = 0; i < length * 8 - 19 - lastBites - tree.size; i++) {
                write(tree.start.symbol.first(), 8, out);
            }
        }
        else {
            for (long i = 0; i < length * 8 - 19 - lastBites - tree.size; i++) {
                if (in.readBit() == 0) node = node.left;
                else node = node.right;
                if (node.left == null) {
                    write(node.symbol.first(), 8, out);
                    node = tree.start;
                }
            }
        }
        in.close();
        out.close();
    }

    public static void write(int count, int k, BOS out) throws IOException {
        //System.out.println(count + " " + k);
        for (int i = k - 1; i >= 0; i--) {
            out.write((count >> i) & 1);
        }
    }
}

class Huffman {
    TreeMap<Character, String> codes;
    String inName;
    Tree tree;

    Huffman (String inName) throws IOException {
        this.inName = inName;
        FileInputStream in = new FileInputStream(inName);
        TreeMap<Integer, Integer> kek = new TreeMap<>();

        int c;
        while ((c = in.read()) != -1) {
            if (!kek.containsKey(c)) kek.put(c, 1);
            else kek.put(c, kek.get(c) + 1);
        }
        in.close();
        TreeSet<Node> nodes = new TreeSet<>();
        for (int i: kek.keySet()) {
            char[] k = {(char) i};
            nodes.add(new Node(kek.get(i), k));
        }
        tree = new Tree(nodes);
        codes = tree.takeCodes();
    }

    int lastBits() throws IOException {
        long nBits = 0;
        FileInputStream in = new FileInputStream(inName);
        int c;
        while ((c = in.read()) != -1) {
            nBits += codes.get((char) c).length();
        }
        in.close();
        nBits += tree.nBit(tree.start);
        nBits += 19;
        return (int) nBits % 8;
    }

    void archive(FileOutputStream output) throws IOException {
        BOS out = new BOS(output);
        write('H', 8, out);
        write('A', 8, out);
        write(lastBits(), 3, out);
        tree.write(tree.start, out);

        FileInputStream in = new FileInputStream(inName);
        int c;
        while ((c = in.read()) != -1) {
            write(codes.get((char) c), out);
        }
        in.close();
        out.close();
    }

    public static void write(int count, int k, BOS out) throws IOException {
        //System.out.println(count + " " + k);
        for (int i = k - 1; i >= 0; i--) {
            out.write((count >> i) & 1);
        }
    }

    public static void write(String kek, BOS out) throws IOException {
        for (int i = 0; i < kek.length(); i++) {
            if (kek.charAt(i) == '1') out.write(1);
            else out.write(0);
        }
    }
}

class Node implements Comparable<Node>{
    Node left, right;
    int freq;
    TreeSet<Character> symbol = new TreeSet<>();

    Node(int freq, char[] symbols) {
        for (char i: symbols) symbol.add(i);
        this.freq = freq;
    }

    Node(int freq, TreeSet<Character> symbols, Node left, Node right) {
        this.symbol.addAll(symbols);
        this.freq = freq;
        this.left = left;
        this.right = right;
    }

    public int compareTo(Node other) {
        if (freq - other.freq == 0) return symbol.first() - other.symbol.first();
        return freq - other.freq;
    }
}

class Tree {
    Node start;
    int size = 0;

    Tree(BIS in) throws IOException {
        start = makeTree(in);
    }

    Node makeTree(BIS in) throws IOException {
        if (in.readBit() == 0) {
            size += 9;
            return new Node(0, new char[] {nextChar(in)});
        }
        size += 1;
        return new Node(0, new TreeSet<>(), makeTree(in), makeTree(in));
    }

    char nextChar(BIS in) throws IOException {
        int res = 0;
        for (int i = 0; i < 8; i++) {
            res <<= 1;
            res += in.readBit();
        }
        return (char) res;
    }

    Tree(TreeSet<Node> nodes) {
        while (nodes.size() > 1) {

            Node left, right;
            left = min(nodes); nodes.remove(left);
            right = min(nodes); nodes.remove(right);

            TreeSet<Character> symbolNew = new TreeSet<>();
            symbolNew.addAll(left.symbol);
            symbolNew.addAll(right.symbol);

            nodes.add(new Node(left.freq + right.freq, symbolNew, left, right));
        }
        start = nodes.first();
    }

    TreeMap<Character, String> takeCodes () {
        TreeMap<Character, String> dict = new TreeMap<>();
        if (start.right == null && start.left == null) {
            dict.put(start.symbol.first(), "0");
            return dict;
        }
        return generateCOde(start, dict, "");
    }

    TreeMap<Character, String> generateCOde(Node node, TreeMap<Character, String> dict, String code) {
        if (node.left == null) dict.put(node.symbol.first(), code);
        else {
            dict = generateCOde(node.left, dict, code + "0");
            dict = generateCOde(node.right, dict, code + "1");
        }
        return dict;
    }

    Node min(TreeSet<Node> nodes) {
        Node k = nodes.first();
        for (Node i: nodes) {
            if (i.freq < k.freq) k = i;
        }
        return k;
    }

    void print () {
        print(start, 0);
    }

    void print (Node node, int level) {
        if (node == null) return;
        print(node.left, level + 1);
        for (int i = 0; i < level; i++) {
            System.out.print("\t");
        }
        System.out.println(node.symbol);
        print(node.right, level + 1);
    }

    int nBit (Node node) {
        int k = 0;
        if (node.left != null && node.right != null) {
            k += nBit(node.left);
            k += nBit(node.right);
            return k + 1;
        }
        return 9;
    }

    void write(Node node, BOS out) throws IOException {
        if (node.left != null && node.right != null) {
            out.write(1);
            write(node.left, out);
            write(node.right, out);
        } else {
            out.write(0);
            write(node.symbol.first(), 8, out);
        }

    }

    public static void write(int count, int k, BOS out) throws IOException {
        //System.out.println(count + " " + k);
        for (int i = k - 1; i >= 0; i--) {
            out.write((count >> i) & 1);
        }
    }
}

class BIS extends InputStream {
    InputStream in;
    byte curByte;
    BIS(InputStream in) throws IOException {
        this.in = in;
        curByte = (byte) in.read();
    }
    int curN = 8;
    public int readBit() throws IOException {
        if (curN != 0) {
            curN--;
            return (curByte >> (curN)) & 1;
        } else {
            int k = in.read();
            if (k != -1) {
                curByte = (byte) k;
                curN = 8;
                curN--;
                return (curByte >> (curN)) & 1;
            } else return -1;
        }
    }
    public int read() throws IOException {
        return in.read();
    }
}

class BOS extends OutputStream {
    OutputStream out;
    BOS(OutputStream out) {this.out = out;}
    byte curByte = 0;
    int curN = 8;
    public void write(int k) throws IOException {
        if (curN != 0) {
            curN--;
            curByte |= k << (curN);
        } else {
//            for (int i = 7; i > -1; i--) System.out.print((curByte >> i) & 1);
//            System.out.println();
//            System.out.println(curByte);
//            System.out.println();
            out.write(curByte);
//            if (new File("output.txt").length() < 10) {
//                int c = 0;
//                FileInputStream in = new FileInputStream("output.txt");
//                while ((c = in.read()) != -1) {
//                    for (int i = 7; i > -1; i--) System.out.print((c >> i) & 1);
//                }
//                System.out.println();
//            }
            curByte = 0;
            curN = 8;
            curN--;
            curByte |= k << (curN);
        }
    }
    public void close() throws IOException {
        out.write(curByte);
//        int c = 0;
//        FileInputStream in = new FileInputStream("output.txt");
//        while ((c = in.read()) != -1) {
//            for (int i = 7; i > -1; i--) System.out.print((c >> i) & 1);
//        }
//        System.out.println();
    }
}
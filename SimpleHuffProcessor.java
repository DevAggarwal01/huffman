/*  Student information for assignment:
 *
 *  On <MY|OUR> honor, <NAME1> and <NAME2), this programming assignment is <MY|OUR> own work
 *  and <I|WE> have not provided this code to any other student.
 *
 *  Number of slip days used:
 *
 *  Student 1 (Student whose Canvas account is being used)
 *  UTEID:
 *  email address:
 *  Grader name:
 *
 *  Student 2
 *  UTEID:
 *  email address:
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class SimpleHuffProcessor implements IHuffProcessor {

    private IHuffViewer myViewer;
    private HashMap<Integer, String> encodings;

    /**
     * Preprocess data so that compression is possible ---
     * count characters/create tree/store state so that
     * a subsequent call to compress will work. The InputStream
     * is <em>not</em> a BitInputStream, so wrap it int one as needed.
     * 
     * @param in           is the stream which could be subsequently compressed
     * @param headerFormat a constant from IHuffProcessor that determines what kind
     *                     of
     *                     header to use, standard count format, standard tree
     *                     format, or
     *                     possibly some format added in the future.
     * @return number of bits saved by compression or some other measure
     *         Note, to determine the number of
     *         bits saved, the number of bits written includes
     *         ALL bits that will be written including the
     *         magic number, the header format number, the header to
     *         reproduce the tree, AND the actual data.
     * @throws IOException if an error occurs while reading from the input file.
     */
    public int preprocessCompress(InputStream in, int headerFormat) throws IOException {
        BitInputStream input = new BitInputStream(in);
        int originalSize = 0;
        int[] freq = new int[ALPH_SIZE];

        boolean done = false;
        while (!done) {
            int bit = input.readBits(BITS_PER_WORD);
            if (bit == -1) {
                done = true;
                // find remaining number of bits stored in file and add to originalSize
                for (int i = 0; i < BITS_PER_WORD; i++) {
                    if (input.readBits(1) != -1) {
                        originalSize++;
                    }
                }
            } else {
                freq[bit]++;
                originalSize += BITS_PER_WORD;
            }
        }

        // make a tree node for every value with a frequency > 0 and add it to the
        // priority queue
        PriorityQueue pQueue = new PriorityQueue();
        for (int i = 0; i < freq.length; i++) {
            // the index i is the integer equivalent of the value
            if (freq[i] != 0) {
                TreeNode node = new TreeNode(i, freq[i]);
                HuffTree tree = new HuffTree(node);
                pQueue.enqueue(tree);
            }
        }
        // PEOF value
        pQueue.enqueue(new HuffTree(new TreeNode(PSEUDO_EOF, 1)));
        // combining all trees into 1 tree
        while (pQueue.size() > 1) {
            pQueue.enqueue(new HuffTree(pQueue.dequeue(), pQueue.dequeue()));
        }
        encodings = new HashMap<>();
        // adds value-encoding pair to encodings map
        encode(pQueue.dequeue(), "");

        // bits per int (header constant, magic #, all values)
        int compressedSize = BITS_PER_INT * 2 + BITS_PER_INT * encodings.keySet().size();

        // += frequency of each encoding * the length of each encoding
        for(Integer key : encodings.keySet()) {
            if(key == PSEUDO_EOF) {
                compressedSize += encodings.get(key).length();
            } else {
                compressedSize += encodings.get(key).length() * freq[key];
            }
        }
        
        int difference = originalSize - compressedSize;

        input.close();

        // return number of bits saved by compression
        return difference;
    }

    private void encode(HuffTree tree, String code) {
        if (tree != null) {
            if (tree.root != null) {
                encodings.put(tree.root.getValue(), code);
            }
            encode(tree.left, code + "0");
            encode(tree.right, code + "1");
        }
    }

    /**
     * Compresses input to output, where the same InputStream has
     * previously been pre-processed via <code>preprocessCompress</code>
     * storing state used by this call.
     * <br>
     * pre: <code>preprocessCompress</code> must be called before this method
     * 
     * @param in    is the stream being compressed (NOT a BitInputStream)
     * @param out   is bound to a file/stream to which bits are written
     *              for the compressed file (not a BitOutputStream)
     * @param force if this is true create the output file even if it is larger than
     *              the input file.
     *              If this is false do not create the output file if it is larger
     *              than the input file.
     * @return the number of bits written.
     * @throws IOException if an error occurs while reading from the input file or
     *                     writing to the output file.
     */
    public int compress(InputStream in, OutputStream out, boolean force) throws IOException {
        throw new IOException("compress is not implemented");
        // return 0;
    }

    /**
     * Uncompress a previously compressed stream in, writing the
     * uncompressed bits/data to out.
     * 
     * @param in  is the previously compressed data (not a BitInputStream)
     * @param out is the uncompressed file/stream
     * @return the number of bits written to the uncompressed file/stream
     * @throws IOException if an error occurs while reading from the input file or
     *                     writing to the output file.
     */
    public int uncompress(InputStream in, OutputStream out) throws IOException {
        throw new IOException("uncompress not implemented");
        // return 0;
    }

    public void setViewer(IHuffViewer viewer) {
        myViewer = viewer;
    }

    private void showString(String s) {
        if (myViewer != null) {
            myViewer.update(s);
        }
    }

    // TODO should this be a inner class
    public class PriorityQueue {
        // front of queue is back of arraylist
        private ArrayList<HuffTree> pQueue;

        public PriorityQueue() {
            pQueue = new ArrayList<>();
        }

        public void enqueue(HuffTree tree) {
            int insert = 0;
            while (insert <= size() - 1 && pQueue.get(insert).compareTo(tree) > 0) {
                insert++;
            }
            pQueue.add(insert, tree);
        }

        public HuffTree dequeue() {
            if (size() <= 0) {
                throw new IllegalArgumentException("dequeue. failed preconditions.");
            }
            return pQueue.remove(size() - 1);
        }

        public HuffTree top() {
            if (size() <= 0) {
                throw new IllegalArgumentException("top. failed preconditions.");
            }
            return pQueue.get(size() - 1);
        }

        public int size() {
            return pQueue.size();
        }
    }

    // TODO should this be an innner class
    public class HuffTree implements Comparable<HuffTree> {
        private TreeNode root;
        private HuffTree left;
        private HuffTree right;
        private final int freq;

        public HuffTree(TreeNode node) {
            if (root == null) {
                throw new IllegalArgumentException("HuffTree constructor. failed preconditions.");
            }
            root = node;
            freq = node.getFrequency();
        }

        public HuffTree(HuffTree left, HuffTree right) {
            this.left = left;
            this.right = right;
            freq = left.getFreq() + right.getFreq();
        }

        @Override
        public int compareTo(HuffTree other) {
            return freq - other.getFreq();
        }

        public int getFreq() {
            return freq;
        }

        public HuffTree getLeft() {
            return left;
        }

        public HuffTree getRight() {
            return right;
        }

        public TreeNode getRoot() {
            return root;
        }

    }
}

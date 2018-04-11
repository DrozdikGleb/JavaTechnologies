package ru.ifmo.rain.drozdov.walk;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;

import static java.nio.file.Paths.get;

/**
 * Created by Gleb on 11.02.2018
 */
public class Walk {
    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            return;
        }
        try (BufferedWriter bw = Files.newBufferedWriter(get(args[1]))) {
            try (BufferedReader br = Files.newBufferedReader(get(args[0]))) {
                String currentLine;
                while ((currentLine = br.readLine()) != null) {
                    traverseDirectory(currentLine, bw);
                }
            } catch (IOException e) {
                System.err.println("I/O occurs opening input file " + e.getMessage());
            } catch (SecurityException e) {
                System.err.println("Can't read input file " + e.getMessage());
            } catch (InvalidPathException e) {
                System.err.println("Can't reach file by this path " + e.getMessage());
            }

        } catch (IOException e) {
            System.err.println("I/O occurs opening output file " + e.getMessage());
        } catch (SecurityException e) {
            System.err.println("Can't write in output file " + e.getMessage());
        } catch (InvalidPathException e) {
            System.err.println("Full path to output file can't be converted into a real path " + e.getMessage());
        }
    }

    private static void traverseDirectory(String stringPath, BufferedWriter out) {
        if (stringPath == null) {
            return;
        }
        File directory = new File(stringPath);
        if (directory.isDirectory()) {
            File[] listOfFiles = directory.listFiles();
            if (listOfFiles == null) {
                traverseFile(stringPath, out);
                return;
            }
            for (File currentFile : listOfFiles) {
                if (currentFile.isDirectory()) {
                    traverseDirectory(currentFile.toString(), out);
                } else {
                    traverseFile(currentFile.toString(), out);
                }
            }
        } else {
            traverseFile(stringPath, out);
        }
    }

    private static void traverseFile(String stringPath, BufferedWriter out) {
        try (FileInputStream in = new FileInputStream(stringPath)) {
            int hash = 0x811c9dc5;
            byte[] bytes = new byte[1024];
            int length;
            while ((length = in.read(bytes)) >= 0) {
                hash = hash(bytes, length, hash);
            }
            out.write(String.format("%08x %s %n", hash, stringPath));
        } catch (SecurityException e) {
            writeError("No access to read this file " + e.getMessage(), out, stringPath);
        } catch (FileNotFoundException e) {
            writeError("No file with such data " + e.getMessage(), out, stringPath);
        } catch (UnsupportedOperationException e) {
            writeError("Unsupported operation " + e.getMessage(), out, stringPath);
        } catch (IOException e) {
            writeError("IOException occured " + e.getMessage(), out, stringPath);
        }
    }

    private static void writeError(String message, BufferedWriter bw, String newLine) {
        System.err.println(message);
        try {
            bw.write(String.format("%s %s %n", "00000000", newLine));
        } catch (IOException e) {
            System.err.println("Can't write into output file " + e.getMessage());
        }
    }

    private static int hash(byte[] bytes, int length, int curHash) {
        for (int i = 0; i < length; i++) {
            curHash = (curHash * 0x01000193) ^ (bytes[i] & 0xff);
        }
        return curHash;
    }
}

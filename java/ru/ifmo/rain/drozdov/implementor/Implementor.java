package ru.ifmo.rain.drozdov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * This class provides implementations of interfaces {@link Impler} and {@link JarImpler}
 *
 * @author Drozdov Gleb
 * @see info.kgeorgiy.java.advanced.implementor.Impler
 * @see info.kgeorgiy.java.advanced.implementor.JarImpler
 */
public class Implementor implements Impler, JarImpler {
    /**
     * String constant for TAB
     */
    private static final String TAB = "    ";
    /**
     * String constant for double TAB
     */
    private static final String TAB_TAB = "        ";

    /**
     * StringBuilder variable for storing class data
     *
     * @see StringBuilder
     */
    private StringBuilder stringBuilder;

    /**
     * default constructor for
     */
    public Implementor() {

    }

    /**
     * Generate interface specified by provided <tt>token</tt> and put it to <tt>root</tt> directory
     *
     * @param token class that we want to create implementation for.
     * @param root  root directory, where we put our class file.
     * @throws ImplerException if problems with implementation will occur
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (token == null || root == null || !token.isInterface()) {
            throw new ImplerException("token and root can't be null");
        }
        if (!token.isInterface()) {
            throw new ImplerException("token can't be interface");
        }
        stringBuilder = new StringBuilder();
        try (PrintWriter printWriter = new PrintWriter(new File(makeFile(token, root)))) {
            if (token.getPackage() != null) {
                printWriter.printf("%s;%n%n", escape(token.getPackage().toString()));
            }
            writeClass(token);
            writeMethods(token);
            printWriter.println(escape(stringBuilder.toString()));
        } catch (IOException e) {
            throw new ImplerException("Can't write class");
        }
    }

    /**
     * Write symbols in UTF-8 if char value less than 127, otherwise in UNICODE
     *
     * @param stringToEscape string that we want to convert
     * @return string in correct representation
     */
    private String escape(String stringToEscape) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < stringToEscape.length(); index++) {
            char currentChar = stringToEscape.charAt(index);
            if (currentChar <= 127) {
                builder.append(currentChar);
            } else {
                builder.append(String.format("\\u%04x", (int) currentChar));
            }
        }
        return builder.toString();
    }

    /**
     * Write class name with modifiers to {@link PrintWriter} object
     *
     * @param token type token to create implementation for
     * @see PrintWriter
     * @see Class
     */
    private void writeClass(Class<?> token) {
        String className = token.getSimpleName() + "Impl";
        String modifier = Modifier.toString(token.getModifiers() & ~Modifier.INTERFACE & ~Modifier.ABSTRACT);
        stringBuilder.append(String.format("%s class %s implements %s { %n",
                modifier, className, token.getCanonicalName()));
    }

    /**
     * Write methods of giving class to {@link PrintWriter}
     *
     * @param token type token to create implementation for
     * @see PrintWriter
     * @see Class
     */
    private void writeMethods(Class<?> token) {
        ArrayList<Method> methodArrayList = new ArrayList<>();
        methodArrayList.addAll(Arrays.asList(token.getMethods()));
        for (Method curMethod : methodArrayList) {
            writeAnnotation(curMethod.getAnnotations());
            writeMethod(curMethod);
            stringBuilder.append(String.format("%s}%n", TAB));
        }
        stringBuilder.append("}");
    }

    /**
     * Write annotaions to {@link PrintWriter} object
     *
     * @param annotations annotations of method
     * @see Annotation
     * @see PrintWriter
     */
    private void writeAnnotation(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            stringBuilder.append(annotation.toString());
        }
    }

    /**
     * Write exceptions of current method to {@link PrintWriter} object
     *
     * @param curMethod currentMethod
     * @see Method
     * @see PrintWriter
     */
    private void writeExceptions(Method curMethod) {
        Class<?> exceptions[] = curMethod.getExceptionTypes();
        for (int i = 0; i < exceptions.length; i++) {
            stringBuilder.append(String.format("%s%s", (i == 0 ? " throws " : ", "), exceptions[i].getCanonicalName()));
        }
        stringBuilder.append(" {");
    }

    /**
     * Write method of current class
     *
     * @param curMethod current method
     * @see Method
     * @see PrintWriter
     */
    private void writeMethod(Method curMethod) {
        String modifier = Modifier.toString(curMethod.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.TRANSIENT);
        stringBuilder.append(String.format("%s %s %s %s(",
                TAB, modifier, curMethod.getReturnType().getCanonicalName(), curMethod.getName()));
        Parameter[] parameters = curMethod.getParameters();
        for (int j = 0; j < parameters.length; j++) {
            stringBuilder.append(String.format("%s%s %s", (j == 0 ? "" : ", "),
                    parameters[j].getType().getCanonicalName(), parameters[j].getName()));
        }
        stringBuilder.append(")");
        writeExceptions(curMethod);
        stringBuilder.append(String.format("%s return %s; %n",
                TAB_TAB, (curMethod.getReturnType().equals(void.class) ? "" : getReturnValue(curMethod))));
    }

    /**
     * Write string representing of default value of value with given <tt>method</tt>
     * Return empty string for {@link Void}, <tt>false</tt> for {@link Boolean},
     * <tt>0</tt> for primitive types and <tt>null</tt> for others.
     *
     * @param method type method to return default value for
     * @return default value of method depending on giving type
     */
    private String getReturnValue(Method method) {
        if (method.getReturnType().equals(void.class)) {
            return "";
        } else if (method.getReturnType().equals(boolean.class)) {
            return "false";
        } else if (method.getReturnType().isPrimitive()) {
            return "0";
        } else {
            return "null";
        }
    }

    /**
     * Create file with extension <tt>.java</tt> to root directory
     *
     * @param token type token to create implementation for
     * @param root  directory, where we want to put java file
     * @return path to java file
     * @throws IOException if problems with creating directory or file occurs
     * @see Path
     */
    private String makeFile(Class<?> token, Path root) throws IOException {
        String fileName = token.getSimpleName() + "Impl" + ".java";
        if (token.getPackage() != null) {
            root = root.resolve(token.getPackage().getName().replace('.', File.separatorChar));
        }
        Files.createDirectories(root);
        root = root.resolve(fileName);
        Files.createFile(root);
        return root.toString();
    }

    /**
     * Produces <tt>.jar</tt> file implementing class or interface specified by provided <tt>token</tt>.
     * <p>
     * Generated class full name should be same as full name of the type token with <tt>Impl</tt> suffix
     * added.
     *
     * @param token   type token to create implementation for.
     * @param jarFile target <tt>.jar</tt> file.
     * @throws ImplerException if problems with jar implementation will occur
     * @see Path
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        Path tempDir = Paths.get(System.getProperty("user.dir")).resolve("TempRoot");
        implement(token, tempDir);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Compiler not found");
        }
        String[] args = {getPath(token, tempDir, "Impl.java").toString(), "-cp", System.getProperty("java.class.path"), "-encoding", "CP1251"};
        int exitCode = compiler.run(null, null, null, args);
        if (exitCode != 0) {
            throw new ImplerException("Problems with compiling files");
        }
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        byte[] buffer = new byte[1024];
        int bytesRead;
        try (JarOutputStream jarWriter = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            jarWriter.putNextEntry(new ZipEntry(token.getName().replace('.', '/') + "Impl.class"));
            FileInputStream file = new FileInputStream(getPath(token, tempDir, "Impl.class").toString());
            while ((bytesRead = file.read(buffer)) != -1) {
                jarWriter.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new ImplerException("Can't create jar file", e);
        }
    }

    /**
     * Return representation of the <tt>path</tt>  in the correct form.
     *
     * @param token     type token to create implementation for
     * @param tempDir   directory, where we want to put our files
     * @param extension extension
     * @return path depending giving parameters
     */
    private Path getPath(Class<?> token, Path tempDir, String extension) {
        Path path = tempDir.resolve(token.getPackage().getName().replace('.', File.separatorChar));
        path = path.resolve(token.getSimpleName() + extension);
        return path;
    }

    /**
     * This function is used to choose which way of implementation to execute.
     * 2 arguments: <tt>className rootPath</tt> - runs {@link #implement(Class, Path)} with given arguments
     * 3 arguments: <tt>-jar className jarPath</tt> - runs {@link #implementJar(Class, Path)} with two second arguments
     * If arguments are incorrect or an error occurs during implementation returns message with information about error
     *
     * @param args arguments for running an application
     */
    public static void main(String[] args) {
        if (args.length < 2 || args[0] == null || args[1] == null) {
            System.err.println("You typed invalid command");
        }
        Implementor implementor = new Implementor();
        if (args.length >= 3) {
            if (args[0].equals("-jar")) {
                try {
                    implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
                } catch (ClassNotFoundException | ImplerException e) {
                    System.err.println(e.getMessage());
                }
            }
        } else {
            try {
                implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
            } catch (ImplerException | ClassNotFoundException e) {
                System.err.println(e.getMessage());
            }
        }
    }
}




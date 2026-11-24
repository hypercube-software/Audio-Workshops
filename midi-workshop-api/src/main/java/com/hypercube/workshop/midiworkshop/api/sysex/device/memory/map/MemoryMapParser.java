package com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.DeviceMemory;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.antlr.DeviceMemoryBuilder;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.antlr.MemoryMapDslLexer;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.antlr.MemoryMapDslParser;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Parse a memory map file "*.mmap" describing the memory layout of a MIDI device.
 *
 * <p>The syntax is described by {@code MemoryMapDsl.g4} (ANTLR). This class only
 * wires the lexer, the parser and the model builder together.
 *
 * <p>It is important to understand that the device memory is not contiguous:
 * a {@link DeviceMemory} contains multiple {@link MemoryMap}.
 */
public final class MemoryMapParser {

    private MemoryMapParser() {
    }

    public static DeviceMemory load(File file) {
        if (!file.exists()) {
            throw new MidiError("File not found:" + file);
        }
        try {
            CharStream input = CharStreams.fromPath(file.toPath(), StandardCharsets.UTF_8);
            MemoryMapDslLexer lexer = new MemoryMapDslLexer(input);
            lexer.removeErrorListeners();
            lexer.addErrorListener(new ThrowingErrorListener(file));

            MemoryMapDslParser parser = new MemoryMapDslParser(new CommonTokenStream(lexer));
            parser.removeErrorListeners();
            parser.addErrorListener(new ThrowingErrorListener(file));

            var tree = parser.file();
            var builder = new DeviceMemoryBuilder(file);
            builder.visit(tree);
            return builder.build();
        } catch (IOException e) {
            throw new MidiError("Unable to parse " + file.getAbsolutePath(), e);
        }
    }

    /**
     * Fail fast on the first syntax error, with a location usable in an IDE or a CI log.
     */
    private static final class ThrowingErrorListener extends BaseErrorListener {
        private final File file;

        private ThrowingErrorListener(File file) {
            this.file = file;
        }

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String msg, RecognitionException e) {
            throw new MidiError("%s:%d:%d %s".formatted(file.getName(), line, charPositionInLine + 1, msg), e);
        }
    }
}

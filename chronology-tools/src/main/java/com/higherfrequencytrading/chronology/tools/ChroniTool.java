package com.higherfrequencytrading.chronology.tools;

import com.higherfrequencytrading.chronology.ChronologyLogEvent;
import com.higherfrequencytrading.chronology.ChronologyLogProcessor;
import com.higherfrequencytrading.chronology.ChronologyLogReader;
import com.higherfrequencytrading.chronology.slf4j.ChronicleLoggingConfig;
import net.openhft.chronicle.Chronicle;
import net.openhft.chronicle.ExcerptTailer;
import net.openhft.lang.io.Bytes;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class ChroniTool {

    public static final DateFormat DF = new SimpleDateFormat(ChronicleLoggingConfig.DEFAULT_DATE_FORMAT);

    // *************************************************************************
    //
    // *************************************************************************

    public static abstract class BinaryProcessor implements ChronologyLogReader, ChronologyLogProcessor {
        @Override
        public void read(final Bytes bytes) {
            ChronologyLogEvent evt = new ChronologyLogEvent();
            evt.readMarshallable(bytes);

            process(evt);
        }

        @Override
        public void process(final String message) {
            throw new UnsupportedOperationException();
        }
    }

    public static abstract class TextProcessor implements ChronologyLogReader, ChronologyLogProcessor {
        @Override
        public void read(final Bytes bytes) {
            process(bytes.readLine());
        }

        @Override
        public void process(final ChronologyLogEvent event) {
            throw new UnsupportedOperationException();
        }
    }

    // *************************************************************************
    //
    // *************************************************************************

    public static final ChronologyLogReader READER_BINARY = new BinaryProcessor() {
        @Override
        public void process(final ChronologyLogEvent event) {
            System.out.println(asString(event));
        }
    };

    public static final ChronologyLogReader READER_TEXT = new TextProcessor() {
        @Override
        public void process(String message) {
            System.out.println(message);
        }
    };

    // *************************************************************************
    //
    // *************************************************************************

    public static ChronologyLogReader binaryReader(final ChronologyLogProcessor processor) {
        return new BinaryProcessor() {
            @Override
            public void process(final ChronologyLogEvent event) {
                processor.process(event);
            }
        };
    }

    public static ChronologyLogReader textReader(final ChronologyLogProcessor processor) {
        return new TextProcessor() {
            @Override
            public void process(String message) {
                processor.process(message);
            }
        };
    }

    public static String asString(final ChronologyLogEvent event) {
        if(!event.hasArguments()) {
            return String.format("%s|%s|%s|%s|%s\n",
                DF.format(event.getTimeStamp()),
                event.getLevel().levelStr,
                event.getThreadName(),
                event.getLoggerName(),
                event.getMessage());
        } else {
            return "";
        }
    }

    // *************************************************************************
    //
    // *************************************************************************

    public static void process(
            @NotNull final Chronicle chronicle,
            @NotNull final ChronologyLogReader reader,
            boolean waitForData,
            boolean fromEnd) throws IOException {

        ExcerptTailer tailer = null;

        try {
            tailer = fromEnd
                ? chronicle.createTailer().toEnd()
                : chronicle.createTailer();

            while (true) {
                if (tailer.nextIndex()) {
                    reader.read(tailer);
                    tailer.finish();
                } else {
                    if (waitForData) {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            // Ignore
                        }
                    } else {
                        break;
                    }
                }
            }
        } finally {
            tailer.close();
            chronicle.close();
        }
    }
}

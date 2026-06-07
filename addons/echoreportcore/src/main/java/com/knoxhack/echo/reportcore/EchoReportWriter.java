package com.knoxhack.echo.reportcore;

import java.io.IOException;
import java.nio.file.Path;

public interface EchoReportWriter {
    String writeToString(EchoReportEnvelope envelope);

    void write(Path path, EchoReportEnvelope envelope) throws IOException;
}

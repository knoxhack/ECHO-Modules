package com.knoxhack.echo.reportcore;

import java.io.IOException;
import java.nio.file.Path;

public final class EchoPackProfileReportWriter {
    private final EchoReportWriter writer;

    public EchoPackProfileReportWriter(EchoReportWriter writer) {
        this.writer = writer == null ? new EchoJsonReportWriter() : writer;
    }

    public String writeToString(EchoPackProfileReport report) {
        return writer.writeToString(report.envelope());
    }

    public void write(Path workspaceRoot, EchoPackProfileReport report) throws IOException {
        writer.write(EchoReportPaths.resolve(workspaceRoot, EchoReportKind.PACK_PROFILE), report.envelope());
    }
}

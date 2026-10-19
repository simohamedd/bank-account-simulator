package com.simo.bank;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

/** Writes an account statement as CSV that opens directly in Excel or Google Sheets. */
public final class StatementExporter {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private StatementExporter() {
    }

    public static String toCsv(Account account) {
        StringBuilder sb = new StringBuilder("date,type,description,money_in,money_out,balance\n");
        for (Transaction t : account.history()) {
            boolean in = switch (t.type()) {
                case DEPOSIT, TRANSFER_IN, INTEREST -> true;
                default -> false;
            };
            sb.append(t.at().format(TIME)).append(',')
              .append(t.type()).append(',')
              .append(quote(t.description())).append(',')
              .append(in ? t.amount().toPlainString() : "").append(',')
              .append(in ? "" : t.amount().toPlainString()).append(',')
              .append(t.balanceAfter().toPlainString()).append('\n');
        }
        return sb.toString();
    }

    public static Path write(Account account, Path folder) throws IOException {
        Path file = folder.resolve("statement-" + account.number() + ".csv");
        Files.writeString(file, toCsv(account), StandardCharsets.UTF_8);
        return file;
    }

    static String quote(String s) {
        return s.contains(",") || s.contains("\"") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }
}
